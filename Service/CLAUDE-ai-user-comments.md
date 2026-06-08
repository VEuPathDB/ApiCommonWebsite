# Plan: AI-Assisted Gene-Publication Summary — Back-End Service (Java)

## Context

VEuPathDB is adding a new kind of user comment: an **AI-assisted gene-publication summary**. A user supplies a gene (via URL `stableId`) and a publication (a PubMed ID or an uploaded PDF). A new back-end service:

1. Resolves the publication text (PMC BioC API for PubMed; for uploads, the FE has already extracted text client-side with MuPDF.js and sends it in the POST body — the PDF itself never reaches the server).
2. Looks up gene synonyms from VEuPathDB and verifies the gene is mentioned in the text.
3. Runs an LLM to generate a gene-function summary. (A second-pass validation LLM was considered but **dropped on 2026-06-05** — the Python authors found it didn't materially improve results and wasn't worth the tokens.)
4. Persists the LLM output to a **`comment_ai_run` cache row** (keyed by the content-digest `job_id`). A `user_comment` row + `comment_ai_provenance` record are created **only when the user reviews and publishes** the result via a dedicated publish endpoint — the pipeline itself never creates a comment.

> **Design pivot (2026-06-02): review-on-approval.** Earlier drafts created a `comments` row (in `review_level='unreviewed'` state) the moment a run completed, forcing every consumer of the `comments` table to LEFT JOIN the sidecar and exclude unreviewed AI rows. We have **dropped that**: a completed run produces only the `comment_ai_run` cache row, and a `comments` row springs into existence **only on user approval**. Abandoned reviews simply leave a `comment_ai_run` row behind (still useful for analysis); nothing records "user X ran Y but didn't publish" in either the database or client storage. The FE uses an "are you sure you want to leave?" navigation guard to minimise abandonment. This removes the need to touch any consumer SQL, and lets `review_level` collapse to a single `is_edited` boolean.

The pipeline already exists in Python (`VPDB_AI_gene_paper_summary/`). For this v1 we are **porting it to Java** rather than wrapping it — the user has chosen this trade-off explicitly. The companion front-end plan (`CLAUDE-plan-ai-user-comments-front-end.md`) defines the REST contract and is the source of truth for the wire shape; this plan implements the back-end side of that contract.

## Architecture overview

A new JAX-RS resource `AiGenePublicationCommentService` lives in `ApiCommonWebsite/Service`, registered in `ApiWebServiceApplication.getClasses()` alongside `UserCommentsService`. It exposes **four endpoints** (matching the FE contract), runs jobs asynchronously on a bounded thread pool, tracks job state in an in-memory map with TTL eviction, and on success writes a `comment_ai_run` cache row. A separate **publish endpoint** is what calls `getCommentFactory().createComment(...)` (plus a `comment_ai_provenance` insert) — and only when the user approves the result.

**Submit lifecycle.** From the FE's perspective the generate phase is **two HTTP interactions** (our POST, then polled GETs), followed by a third (the publish POST) once the user approves. For the upload path, the FE does the PDF heavy lifting locally: it extracts text using **MuPDF.js** (WASM, lazy-loaded only when the user picks the upload tab) and computes a SHA-256 of the PDF bytes, then sends `{ paper_text, pdf_content_sha256, ... }` in the POST body — the PDF itself never leaves the user's machine. Inside our POST handler there are **two phases**: (a) a *synchronous prelude* (target <2s) that resolves gene synonyms in-process, computes a content-digest `jobId`, looks up `comment_ai_run` and the in-memory registry, and either returns a cache-hit response (the cached `ai_output` + `sibling_summary` — **no comment is created here**) or attaches the caller to an in-flight job, then (b) the *asynchronous pipeline* that runs only on a true miss. The terminal result carries the AI output keyed by `job_id`; the comment row is materialised later by the publish endpoint.

**Identifiers.** The `jobId` is a hex SHA-256 over `(geneId, sorted-resolved-synonyms, source-key, modelName, promptVersion, optionsCanonicalJson)`, where the source-key is the PubMed id (PubMed path) or the FE-supplied `pdf_content_sha256` (upload path) and `optionsCanonicalJson` is the request's `options` object rendered as canonical JSON (Jackson with `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY`, no whitespace, missing/null fields normalised to defaults). `promptVersion` is a manually-bumped constant for the `getGeneSummary` prompt — devs bump it when they edit the prompt files, the same way they'd bump a schema version. Hashing the whole `options` blob means a future option that changes LLM output (e.g. `generate_product_descriptions`) is automatically covered without anyone having to remember to update the digest formula. (With the review-on-approval pivot, `options` no longer contains any per-submitter flags — `create_user_comment` has been removed, since the pipeline never creates a comment — so every option left in the blob genuinely affects the LLM output, and there are no longer "wasted" cache misses on submitter-specific options.) The same `jobId` keys the in-memory registry and the `comment_ai_run` cache row, so double-tap submits and cross-user resubmissions naturally dedupe.

**Pool exhaustion** returns `503 Service Unavailable` with a `Retry-After` header (no queueing). The FE renders a friendly toast and offers retry.

The Anthropic client setup mirrors `ClaudeSummarizer` (`AnthropicOkHttpClientAsync.builder().apiKey(...)`), but we do not reuse `Summarizer` itself or its disk cache. Prompts and JSON schemas are ported from Python `pipeline/prompts.py` into `.txt` and `.json` resource files; placeholder substitution uses simple `String.replace("[GENE]", ...)` matching the Python convention.

```
                                          (upload path only)
                                ┌──────────────────────────────┐
                                │ Front-end / browser          │
                                │  MuPDF.js (WASM, lazy-loaded)│
                                │   PDF → paper_text           │
                                │       + pdf_content_sha256   │
                                └──────────────┬───────────────┘
                                               │ JSON in POST body
                                               ▼
┌─────────────────────────────────────────────────────────────────┐
│ AiGenePublicationCommentService (JAX-RS, JSON-only)             │
│  POST   /user-comments/ai-gene-publication          → status    │
│  GET    /user-comments/ai-gene-publication/{id}     → status    │
│  DELETE /user-comments/ai-gene-publication/{id}     → cancel    │
│  POST   /user-comments/ai-gene-publication/{id}/publish         │
│              → create comment + provenance (user approval)      │
└─────────┬───────────────────────────────────────────────────────┘
          │ POST → sync prelude (target <2s)
          ▼
┌─────────────────────────────────────────────────────────────────┐
│ Sync prelude                                                    │
│  0a validate request                                            │
│  0b resolve gene synonyms (WDK RecordClass, in-process)         │
│  0c compute jobId = sha256(gene, synonyms, source-key,          │
│                            model, promptVersion, options)       │
│       source-key = pubmed_id | pdf_content_sha256               │
│  0d SELECT comment_ai_run by jobId  ──hit──► cache-hit response │
│  0e registry lookup by jobId        ──hit──► attach as follower │
│  0f miss → spawn pipeline on bounded executor (503 if full)     │
└─────────┬───────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────┐
│ JobRegistry  (ConcurrentHashMap<jobId, JobState>)│
│  JobState: stage, message, future, followers[], result │
│  - submit() / attach() / get() / cancel()       │
│  - scheduled 10-min eviction (terminal jobs)    │
└─────────┬───────────────────────────────────────┘
          │ stage callbacks update JobState
          ▼
┌─────────────────────────────────────────────────────────────────┐
│ AiGenePublicationPipeline (async)                               │
│  ① resolveArticleText (PMC BioC fetch for pubmed; pass-through  │
│                        for upload — text is in the POST body)   │
│  ② scanGeneMentions  (regex matcher ported from helpers.py)      │
│  ③ generateSummary   (Anthropic call — getGeneSummary prompt)    │
│  ④ flattenToComment  (structured JSON → headline + content)     │
│  ⑤ persist: INSERT INTO comment_ai_run  (cache row only —       │
│             NO comment is created by the pipeline)              │
└─────────────────────────────────────────────────────────────────┘

   ── later, on user approval (separate request thread) ──
┌─────────────────────────────────────────────────────────────────┐
│ POST …/{job_id}/publish  (PublishHandler)                       │
│  • SELECT comment_ai_run by job_id (404 if absent/uncacheable)  │
│  • CommentFactory.createComment(headline, content, provenance)  │
│      within one tx also INSERT comment_ai_provenance            │
│      (run_job_id, is_edited = submitted ≠ cached AI original)   │
│  • return { comment_id }                                        │
└─────────────────────────────────────────────────────────────────┘
```

## REST contract — Java implementation of the FE contract

Matches `CLAUDE-plan-ai-user-comments-front-end.md` §"Backend contract" with the additions noted in **Out of scope / coordination items** below.

**POST body** (JSON-only — for the upload path the FE has already extracted the text client-side with MuPDF.js and computed the PDF content hash; the PDF itself is never uploaded):

```json
{
  "gene_id": "string",
  "document_type": "pubmed | upload",
  "pubmed_id": "string (iff document_type=pubmed)",
  "paper_text": "string (iff document_type=upload) — extracted by MuPDF.js",
  "pdf_content_sha256": "string (iff document_type=upload) — hex SHA-256 of the PDF bytes, computed via Web Crypto",
  "external_url": "string (optional, upload provenance)",
  "external_title": "string (optional, upload provenance)",
  "options": {
    "generate_product_description": false
  }
}
```

Snake-case keys match the existing comments-service JSON convention. No `user_id` — WDK identifies the caller. Typical `paper_text` size: 30–200 KB; well under standard servlet POST limits. (`create_user_comment` was removed in the review-on-approval pivot: the generate POST never creates a comment, so the flag gated nothing.)

**POST response** uses the same union as the GET status response (i.e. POST may return a terminal state directly on a cache hit). **No terminal carries a `comment_id`** — a comment only exists after the publish step:

- `running { job_id, stage }` — fresh job spawned, or attached to an in-flight job with the same `job_id`.
- `success { job_id, ai_output, sibling_summary }` — cache hit on `comment_ai_run`; the cached AI output is returned for the FE to render its review form.
- `mentioned-in-passing { job_id, synonyms_checked, sibling_summary }` — cache hit.
- `gene-not-mentioned { job_id, synonyms_checked, sibling_summary }` — cache hit.
- `text-unavailable { reason }` — never cached; the fetch will run on every retry.

`sibling_summary { reviewed: N, edited: N, latest_at: ISO-8601 | null }` is anonymous aggregate data over `comment_ai_provenance` rows pointing at the same `run_job_id`. Because provenance rows now exist **only for published comments**, the counts are: `reviewed` = published-without-edits (`is_edited=false`), `edited` = published-with-edits (`is_edited=true`); there is no longer an `unreviewed` count (unreviewed runs leave no provenance row). It exists so the FE can render a banner ("this combo has been published by N others; you can still review and publish under your name") without revealing the other users. `latest_at` is the most recent provenance `created_at`.

**Terminal status union returned from GET on a completed job:**
- `success { job_id, ai_output, sibling_summary }`
- `gene-not-mentioned { job_id, synonyms_checked, sibling_summary }`
- `mentioned-in-passing { job_id, synonyms_checked, sibling_summary }`
- `text-unavailable { reason }`
- `internal-error { error }`
- `cancelled`

`gene-not-mentioned` comes from the deterministic regex scan; `mentioned-in-passing` comes from the LLM. None of these create a comment — the FE renders a review form (empty body for the two "not really about this gene" outcomes) and the comment is created on publish.

**The publish endpoint** — `POST /user-comments/ai-gene-publication/{job_id}/publish`:

```json
{ "headline": "string", "content": "string" }
```

- Looks up `comment_ai_run` by `{job_id}` (the path id). **404** if absent — only a *cacheable* terminal run (`success` / `mentioned-in-passing` / `gene-not-mentioned`) can be published; `text-unavailable` / `internal-error` never wrote a run row. **400** if `headline`/`content` are empty.
- In one transaction: `createComment(...)` for the authenticated user (provenance — pubmed id / external url+title / gene — read from the run row, not the body), then `INSERT comment_ai_provenance(comment_id, run_job_id, is_edited, created_at)` where `is_edited = (headline ≠ run.ai_headline OR content ≠ run.ai_content)`. For `mentioned-in-passing` / `gene-not-mentioned` the run's `ai_headline`/`ai_content` are null, so any user-written body is `is_edited = true`.
- Returns `{ comment_id }` (201). The FE then leaves the AI flow (the comment is now a normal published comment).

**Stage names emitted in `running` state** (subset of FE-defined stages, only those v1 ships):
- `queued` → `fetching-article` → `scanning-gene-mentions` → `generating-summary` → `persisting`

`persisting` (writing the `comment_ai_run` cache row) now always runs for a cacheable outcome — it is no longer gated on `create_user_comment`. Gene-synonym resolution happens in the sync prelude before any `running` state is published, so it isn't an emitted stage. No `generating-product-description` in v1 (product descriptions deferred — see "Out of scope" below).

**Polling cadence**: server tolerates ~1/s polls; no rate limiting beyond standard `AbstractWdkService` behaviour. **Registry TTL: 10 min** after terminal state (≥5 min recommended by FE; we round up). `comment_ai_run` rows are permanent — they are the source of truth for cache hits after the registry entry has been evicted.

## Pipeline stage details

### Stage 0: synchronous prelude (inside POST handler)

Runs entirely on the request thread, target <2 s end-to-end. Performs no LLM calls and no external HTTP except (for PubMed) nothing yet — the article fetch happens in stage ①.

| Step | Action |
|------|--------|
| 0a | Validate request shape. For uploads, require both `paper_text` (non-empty) and `pdf_content_sha256` (64-char hex). |
| 0b | Resolve `gene_id` + aliases via `wdkModel.getRecordClass("GeneRecordClasses.GeneRecordClass")`. 404 if the stableId is unknown. |
| 0c | Compute `job_id = sha256(geneId ‖ sortedSynonyms ‖ sourceKey ‖ modelName ‖ promptVersion ‖ optionsCanonicalJson)` where `sourceKey = pubmed_id` (PubMed path) or `pdf_content_sha256` (upload path, supplied by the FE), and `optionsCanonicalJson` is the `options` object serialised with Jackson `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` and default-normalisation. |
| 0d | `SELECT * FROM comment_ai_run WHERE job_id = ?`. **Hit** → aggregate `sibling_summary` from `comment_ai_provenance` and return the terminal cache-hit response (cached `ai_output` + `sibling_summary`). **No comment is created here** — the FE renders its review form from the cached output and the comment is materialised only by the publish endpoint. |
| 0e | Registry lookup by `job_id`. **Hit** → register the caller as a follower of the in-flight job and return its current `running` state. |
| 0f | Miss → spawn the async pipeline on the bounded executor and return `running { job_id, stage: "queued" }`. **Pool full → 503 + `Retry-After`**. |

### Stages 1–6: async pipeline

| # | Stage | Implementation |
|---|-------|---------------|
| ① | `fetching-article` | If `document_type=pubmed`: GET `https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/{pmid}` via Java `HttpClient`, parse with Jackson, keep passages where `infons.section_type ∈ {FIG, TABLE, RESULTS, CONCL, DISCUSSION, SUPPL}`, concatenate `text` fields. Non-JSON / 404 / non-OA paper → terminal `text-unavailable` (not persisted to the cache). If `document_type=upload`: nothing to fetch — the FE-supplied `paper_text` is used directly. The stage still emits a `fetching-article` progress event for symmetry, but completes immediately. |
| ② | `scanning-gene-mentions` | Port `_count_substrings` from `PubGene_back_end/helpers.py` (regex matcher: handles `Nd6` ↔ `Nd-6`, `PF3D7_1133400` ↔ `PF3D7-1133400`, case-insensitive, non-alphanumeric boundaries). If the gene id and *all* aliases score 0 hits → terminal `gene-not-mentioned` with `synonyms_checked` populated (this outcome **is** persisted to `comment_ai_run`). Otherwise pass the top-3-by-frequency aliases into the prompt. |
| ③ | `generating-summary` | Anthropic API call. System prompt + user prompts loaded from `resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}`. Placeholder substitution: `[PAPER_TEXT]`, `[GENE]`, `[N_QUOTES]`, `[JSON_SCHEMA]`. Prefill assistant turn with `{` (matches Python). Strip markdown fences from response, parse JSON. On the response's `only_in_passing=true` → short-circuit to terminal `mentioned-in-passing` (persisted to `comment_ai_run`). Up to `max_retry=3` retries via formatter LLM on malformed JSON (port `extract_json` retry loop). |
| ④ | flatten | Compute `ai_output.headline = ShortSummary` (plain text). `ai_output.content` = bullet-flattened plain text that is also valid markdown — `- ` bullets with indented `Evidence:` / `>` quote lines, an optional "Additional inferences" section, and an "Aliases mentioned in paper:" line. No HTML; no markdown editor library is in the FE monorepo, so we stay plain-text-compatible while future-proofing for a markdown renderer on the show page (deferred). |
| ⑤ | `persisting` | `INSERT INTO usercomments.comment_ai_run` — one row keyed by `job_id` (idempotent; survives retries cleanly). **That is the pipeline's only write.** No `comments` or `comment_ai_provenance` rows are created here — there are no per-follower comments. Every follower polling this `job_id` simply receives the same terminal `ai_output` on their next GET; each then independently reviews and (optionally) publishes via the publish endpoint. |

> **Validation stage removed (2026-06-05):** an earlier draft had a stage ④ `validating` (an Anthropic call using a `verifyGeneSummary` prompt) gated on `options.validate`. The Python authors reported the second pass didn't materially improve results and wasn't worth the tokens, so it's dropped — there is no `validate` option, no `validating` stage, and no `validation-error` terminal. The pipeline runs a single LLM stage (`generating-summary`). When product descriptions are added later, they derive from this first-pass summary.

### Publish step (separate from the pipeline)

Triggered by the FE's `POST /user-comments/ai-gene-publication/{job_id}/publish` when the user approves. Runs on the request thread (no executor). `SELECT comment_ai_run by job_id` (404 if absent) → `getCommentFactory().createComment(commentRequest, user)` building the request's provenance from the run row, and within the **same transaction** `INSERT INTO usercomments.comment_ai_provenance(comment_id, run_job_id, is_edited, created_at)` with `is_edited = submitted text ≠ cached AI original`. Returns `{ comment_id }`. This is the *only* place a `comments` row is created in the whole flow.

**Terminal outcomes that are persisted to `comment_ai_run`** (and therefore short-circuit future submits, and are publishable): `success`, `mentioned-in-passing`, `gene-not-mentioned`. **Not persisted** (retries are free; not publishable): `text-unavailable`, `internal-error`, `cancelled`. See "Out of scope / coordination items" for an optional `text-unavailable` observability log.

## Job state mechanism

In-memory `JobRegistry`:
- `ConcurrentHashMap<String, JobState>` keyed by the hex SHA-256 `job_id` (not a UUID). `JobState` carries the current `{stage, message, updated_at}`, the immutable submission summary, the running `Future`, a **follower list** `List<Long>` (user ids — used only for dedupe/observability, since followers no longer get per-user comments), and (when terminal) the result. *(The earlier per-follower `comment_id` map / `recordComment` / `getCommentId` on `JobState` have been **removed** — comments are created by the publish endpoint, not the pipeline.)*
- Submit handler runs the sync prelude (stage 0); on a registry hit it appends the caller's user id to the follower list and returns the existing job's state, on a miss it spawns the pipeline on a fixed-size `ExecutorService` (cap: **8 threads**, sized for slow LLM calls). If the pool rejects the submission → `503 Service Unavailable` with `Retry-After`.
- Each pipeline stage calls back into the registry to update `JobState.progress` with `{ stage, message, updated_at }`.
- `ScheduledExecutorService` runs every 60 s and evicts entries whose terminal-state age exceeds 10 min. `comment_ai_run` rows are permanent and still satisfy late cache hits.
- DELETE: marks the job cancelled, calls `.cancel(true)` on the `Future`, and cancels any in-flight Anthropic HTTP call via the OkHttp client. Next poll from any follower returns `type: 'cancelled'`. Per-follower opt-out (one follower wants to detach without killing the job for others) is deferred — see "Out of scope".
- Server restart = all in-flight jobs lost. Acceptable for v1: jobs are seconds-to-minutes, and the `comments`/`comment_ai_provenance` rows are the durable artefacts once `persisting` has run. Lost mid-flight jobs surface to the FE as `not-found` (404) on the next poll, which already triggers a friendly "job expired, please submit again" UX.

## Database schema — two sidecar tables

Match the existing convention (Categories, References, Attachments all use sidecars off `comments`). The shared LLM output cache lives in `comment_ai_run`, keyed by the content-digest `job_id`. A `comment_ai_provenance` row exists **only for a published comment** (created by the publish endpoint, never by the pipeline), keyed by `comment_id` with a FK to the run row; it records `is_edited` rather than a multi-valued review level, because by construction every such row is already reviewed-and-approved. The current (possibly edited) headline/content live in `usercomments.comments` itself; the immutable AI original lives on the run row.

**Dev database**: PostgreSQL — `userdb_devn` on `ares13.penn.apidb.org:5432` (confirmed via LDAP lookup of `userDb_ldapCommonName: userdb_devn` on `ds.apidb.org`).

**Action**: Ask Mustafa or Steve to create the tables in `userdb_devn`.

**Schema persistence**: `VEuPathDB/ApiCommonModel` → `Model/lib/sql/` (follow the `migration_comment_b*.sql` pattern; the previous draft pointed at `ApiCommonData/Load/lib/sql/comments/psql/createCommentTables.sql`, which is the wrong repo).

**Production roll-out**: Bob needs to understand how this works.

**Oracle back-port**: probably not needed (all active sites appear to be on PostgreSQL) — confirm before closing.

```sql
CREATE TABLE usercomments.comment_ai_run
(
  job_id                       VARCHAR(64)  NOT NULL,   -- hex SHA-256
  model_name                   VARCHAR(64)  NOT NULL,   -- e.g. 'claude-sonnet-4-6'
  prompt_version               VARCHAR(32)  NOT NULL,   -- manually-bumped constant per prompt stage
  source_kind                  VARCHAR(16)  NOT NULL,   -- 'pubmed' | 'upload'
  pubmed_id                    VARCHAR(32),             -- iff source_kind='pubmed'
  external_url                 TEXT,                    -- iff source_kind='upload', optional
  external_title               VARCHAR(4000),           -- iff source_kind='upload', optional
  pdf_content_sha256           VARCHAR(64),             -- iff source_kind='upload'
  gene_id                      VARCHAR(128) NOT NULL,
  synonyms_used                TEXT[]       NOT NULL,   -- canonicalised, sorted, also baked into job_id
  options_json                 TEXT         NOT NULL,   -- canonical JSON of the request's `options` object, also baked into job_id
  terminal_status              VARCHAR(32)  NOT NULL,   -- 'success' | 'mentioned-in-passing' | 'gene-not-mentioned'
  is_only_mentioned_in_passing BOOLEAN      NOT NULL,
  ai_headline                  VARCHAR(2000),           -- null iff terminal_status != 'success'
  ai_content                   TEXT,                    -- null iff terminal_status != 'success'
  completed_at                 TIMESTAMP    NOT NULL,
  CONSTRAINT comment_ai_run_pkey PRIMARY KEY (job_id)
);

CREATE TABLE usercomments.comment_ai_provenance
(
  comment_id    BIGINT       NOT NULL,
  run_job_id    VARCHAR(64)  NOT NULL,
  is_edited     BOOLEAN      NOT NULL,   -- true iff the published text differs from the AI original
  created_at    TIMESTAMP    NOT NULL,   -- when the user approved/published (row only exists post-approval)
  CONSTRAINT comment_ai_provenance_pkey PRIMARY KEY (comment_id),
  CONSTRAINT comment_ai_prov_comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES usercomments.comments (comment_id),
  CONSTRAINT comment_ai_prov_run_fkey FOREIGN KEY (run_job_id)
      REFERENCES usercomments.comment_ai_run (job_id)
);

CREATE INDEX comment_ai_provenance_run_idx ON usercomments.comment_ai_provenance (run_job_id);

GRANT insert, update, delete on usercomments.comment_ai_run        to COMM_WDK_W;
GRANT insert, update, delete on usercomments.comment_ai_provenance to COMM_WDK_W;
GRANT select                  on usercomments.comment_ai_run        to GUS_R;
GRANT select                  on usercomments.comment_ai_provenance to GUS_R;
```

The current (possibly edited) headline/content live in `usercomments.comments`. The immutable AI original lives in `comment_ai_run.ai_headline` / `ai_content`. `is_edited` is a convenience flag the publish endpoint sets by comparing the user's submitted text to that original — cheaper for the FE/consumers than re-deriving the diff across two tables on every read. (A full diff is still reconstructable from the two tables if ever needed.)

Style follows the existing comment-sidecar conventions: `BIGINT` for the comment id, `VARCHAR`/`TEXT` for strings, grants to `COMM_WDK_W`/`GUS_R`, table names under `usercomments.comment_*`. `ai_headline` sized to match `comments.headline VARCHAR(2000)`.

## Java implementation: file layout

### New code (this plan)

| Action | Path |
| ------ | ---- |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationCommentService.java` — JAX-RS resource (POST / GET / DELETE / **POST {id}/publish**), `@Consumes(APPLICATION_JSON)` end-to-end |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobRegistry.java` — in-memory job store, follower list, bounded executor, scheduled eviction |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobState.java` + `JobStatus.java` — value types matching FE contract |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationPipeline.java` — orchestrator: runs stages ①–⑥, emits progress callbacks (⑥ writes only `comment_ai_run`) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/PublishHandler.java` — backs `POST {id}/publish`: load run row, `createComment` + `comment_ai_provenance` insert in one tx, compute `is_edited` |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/SyncPrelude.java` — gene-synonym resolution, PDF content hash, digest, cache/registry lookup, follower attach |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/article/PmcBiocFetcher.java` |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneSynonymService.java` (WDK `RecordClass` lookup) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneMentionScanner.java` (regex matcher port) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/llm/AnthropicJsonClient.java` (thin wrapper: load prompt files, substitute placeholders, parse JSON with retry) |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/CommentAiRun.java` — POJO for the cache row |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/AiProvenance.java` — POJO for the per-comment provenance row (`run_job_id`, `is_edited`, `created_at`) |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/InsertCommentAiRunQuery.java` — follows the `InsertCategoryQuery` / `InsertAttachmentQuery` pattern |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/InsertCommentAiProvenanceQuery.java` |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/GetCommentAiRunQuery.java` — cache lookup by `job_id` plus sibling-summary aggregate |
| create | `ApiCommonWebsite/Service/src/main/resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/post-request.json` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/status-response.json` |
| create | DB migration script under `ApiCommonModel/Model/lib/sql/` (follow the `migration_comment_b*.sql` pattern) for both new tables |
| modify | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/ApiWebServiceApplication.java` — `.add(AiGenePublicationCommentService.class)` |
| modify | `ApiCommonWebsite/Service/doc/raml/apicommonwebsite.raml` — document the four new endpoints (incl. `{id}/publish`) |
| modify | `ApiCommonWebsite/Service/pom.xml` — no new PDF dependency needed (text extraction happens client-side in MuPDF.js, the server never touches a PDF). `anthropic-java` is already present in the Model pom; no SDK addition needed there. |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/CommentFactory.java` (or its EbrcWebsiteCommon parent) — extend the `createComment` transaction (currently `con.setAutoCommit(false)` → `con.commit()` at `CommentFactory.java:196,234`) to also insert `comment_ai_provenance` when an `AiProvenance` is supplied on the request. This is now driven by the **publish endpoint** (user approval), not the pipeline. Extend `GetCommentQuery` to LEFT JOIN the provenance + run rows so a published AI comment exposes its `aiProvenance` (source, `is_edited`, and the AI originals from the run row). **No consumer-SQL exclusion needed** — unreviewed runs never create `comments` rows. **Coordination item with EbrcWebsiteCommon.** |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/CommentRequest.java` and `Comment.java` — add `aiProvenance` field (optional). |

### Implementation order

**First deliverable — scaffolding only.** All new classes (resource, registry, pipeline, sync prelude, fetchers, scanners, LLM client, POJOs, queries) created with JAX-RS annotations, method signatures, and no-op bodies that throw `UnsupportedOperationException` (or return `501 Not Implemented` for the endpoints). Service registered in `ApiWebServiceApplication`. SQL migration script committed. Module builds clean; the endpoints return 501. **Pause here for user review of the shape before any pipeline logic lands.**

Subsequent deliverables, in order:

1. Sync prelude (gene/synonym resolution, digest, `comment_ai_run` lookup, registry attach). Cache-hit returns cached `ai_output` + `sibling_summary` (no comment created).
2. PMC BioC article fetcher (pubmed path); upload path just reads `paper_text` from the request.
3. Gene-mention regex scanner.
4. Anthropic call + JSON retry loop for `generating-summary`.
5. Flatten the summary JSON → `ai_output` (headline + content). _(Was "validation pass"; the verifyGeneSummary stage was dropped 2026-06-05.)_
6. Persist stage ⑤ — `INSERT comment_ai_run` only (the pipeline creates no comment).
7. **Publish endpoint** (`POST {id}/publish`) — `createComment` + `comment_ai_provenance` insert in one tx, `is_edited` computed vs the run's AI original. Depends on the `CommentFactory`/`CommentRequest` `aiProvenance` extension. **Blocked on DB tables for live test.**
8. DELETE / cancel wiring.
9. Registry eviction scheduler.

*(Deliverable numbering shifted: the old D6 "6b loop over followers" is gone; comment creation moved to the new publish deliverable. `create_user_comment` is removed from `AiGenePublicationRequest.Options`, the canonical-options JSON, and `JobDigest`. The vestigial per-follower `comment_id` map on `JobState` is removed.)*

### Reused without modification

- **Anthropic client setup pattern** — `AnthropicOkHttpClientAsync.builder().apiKey(wdkModel.getProperties().get("CLAUDE_API_KEY")).maxRetries(32)...` (lifted verbatim from `ClaudeSummarizer` at `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/report/ai/expression/ClaudeSummarizer.java`).
- **`fetchUser()` / `getRequestingUser()`** from `AbstractWdkService` / `AbstractUserCommentService` — authenticates the caller; throws 401 for guests, which is the FE-required `requiresLogin: true` behaviour.
- **`getCommentFactory().createComment(commentRequest, user)`** from the existing `UserCommentsService` flow (line 91 of that file) — does the bulk of the persistence; its transaction scope (`CommentFactory.java:196,234`) is what we extend with the provenance insert.
- **DELETE-user-comment endpoint** at `DELETE /user-comments/{id}` (`UserCommentsService.deleteComment`, line 164) is already implemented. (No longer used for an AI "delete draft" — there is no draft comment now; abandoning a review creates nothing to delete. It remains available for deleting a *published* AI comment like any other.)

## Prompts: porting strategy

Source: `VPDB_AI_gene_paper_summary/pipeline/prompts.py` at https://github.com/PubLLicanProject/VPDB_AI_gene_paper_summary/tree/main (`global_prompts_and_schema` dict, 1436 lines covering five stages — we port only `getGeneSummary`; the `verifyGeneSummary` validation pass was dropped 2026-06-05). 

Approach:
- One directory per stage under `src/main/resources/ai/prompts/<stage>/`.
- `system.txt` — system-prompt text with `[PLACEHOLDER]` markers.
- `user.txt` — newline-separated user-message turns (one paragraph per turn, blank line separating). The Anthropic Java SDK supports multi-turn user messages via `addUserMessage` per line.
- `schema.json` — JSON-schema dict (validation only used as prompt guidance, matching Python — not enforced server-side).
- A small `PromptLoader` utility reads the files once at startup, caches them, and exposes `render(stage, Map<String,String> replacements)` that does naive `String.replace` for each placeholder.

This keeps the diff against `prompts.py` legible — when the Python team iterates, manual sync remains feasible. Drift risk acknowledged.

## Out of scope / coordination items

These are flagged for the user to decide or coordinate, not implemented in this plan:

1. **FE contract amendments** (cross-reference): the FE plan (`CLAUDE-plan-ai-user-comments-front-end.md`) is kept in sync with this one — both were revised together for the review-on-approval pivot (no inline comment / no `comment_id` on terminals; review keyed by `job_id`; the publish endpoint; `sibling_summary { reviewed, edited, latest_at }`).
2. **EbrcWebsiteCommon coordination**: extending `CommentRequest`/`Comment`/`CommentFactory` to carry `aiProvenance` and read/write the two new sidecar tables — now invoked from the **publish endpoint** rather than the pipeline. May span repos.
3. **DB migration script placement / review**: scripts live in `ApiCommonModel/Model/lib/sql/` (corrected location) — DBA review and migration scripting follows existing project conventions.
4. **Product descriptions** (Python stages 3–5): explicitly deferred. When added, they get `options.generate_product_description=true`, a new `generating-product-description` stage, and a separate persistence target (TBD). PDs derive from the **first-pass `getGeneSummary` output** (the `verifyGeneSummary` validation pass was dropped 2026-06-05; per the Python authors, generating PDs from the first-pass summary is fine).
5. **Prompt drift**: the Python pipeline is still being iterated (test sets, model comparison runs ongoing). Ported Java prompts will drift unless re-synced. A v2 option is to read prompts from a shared file/repo, or to revisit the integration choice. Cache invalidation on prompt edits is **manual** — devs bump `promptVersion` (the per-stage constant baked into `job_id`) when they touch the prompt files. Convention enforced by code review.
6. **No daily cost cap in v1**: unlike `AiExpressionSummary`'s `DailyCostMonitor`, this service does not currently rate-limit by daily spend. If volume warrants, plug into the existing `DailyCostMonitor` pattern as a follow-up.
7. **`/user-comments/show` modernisation, gene-page provenance column, de-duplication** — all deferred per the FE plan.
8. **Optional `comment_ai_text_unavailable_log` table** (append-only, no read path) — would let us measure how often `text-unavailable` outcomes occur and decide later whether short-lived caching is worth it. Not added in this plan — decision pending.
9. **Per-follower DELETE semantics**: today a DELETE cancels the underlying job for *all* attached followers. A future addition could let one follower detach without killing the job for others; not in v1. **Accepted by the user (2026-06-08):** within a single job's processing window the overwhelmingly likely "follower" case is *the same user* resubmitting the identical job (double-tap / refresh), where cancel-all is exactly right; two *different* users hitting the identical (gene, publication, options) digest concurrently is very unlikely. So the simplification is fine for v1.
11. **Publish-comment `ORGANISM` (post-impl TODO, user-requested):** the D7 publish endpoint currently creates the comment with a null `ORGANISM` (it's optional for comment creation and isn't on the run row). Follow-up: resolve the organism from the gene record (`GeneRecordClasses.GeneRecordClass`, as the prelude already resolves the gene) and set it in `AiGenePublicationCommentService.buildPublishComment`. Marked with a `TODO(post-impl)` in code.
10. **FE coordination — client-side PDF extraction**: the FE plan needs (a) MuPDF.js (WASM, lazy-loaded only on the upload tab — see <https://mupdf.readthedocs.io/en/1.27.2/guide/using-with-javascript.html>), (b) Web Crypto SHA-256 of the PDF bytes, (c) POST body shape carrying `paper_text` + `pdf_content_sha256`, (d) a graceful client-side error UI for the case where MuPDF.js fails to parse a particular PDF (since that no longer surfaces as a server-side `text-unavailable`). The text-extraction quality matches the Python pipeline (MuPDF.js and PyMuPDF share the MuPDF engine), so prompts that have been tuned against the Python output should transfer cleanly.

## Verification

1. **Module builds clean** in order: `install/`, `WDK/`, `ApiCommonWebsite/Model`, `ApiCommonWebsite/Service` (`mvn clean install -DskipTests`).
2. **Service registers**: `GET /user-comments/ai-gene-publication/<bogus-digest>` returns 404 (not a JAX-RS 405/wrong-route error).
3. **Auth gate**: same call from a guest session returns 401.
4. **PubMed happy path**: POST with a known OA PMID + gene that's clearly discussed → POST returns `running { job_id, stage }` within ~2s, polling shows stages advance, terminal `success` returns `ai_output` (no `comment_id`). `comment_ai_run` row exists keyed by the digest; **no `comments` / `comment_ai_provenance` row yet**.
5. **Publish (review-on-approval)**: after #4, `POST …/{job_id}/publish { headline, content }` → 201 `{ comment_id }`. `comments` row + `comment_ai_provenance` row now exist; `is_edited=false` when the body matches `comment_ai_run.ai_headline`/`ai_content`, `true` when edited. `GET /user-comments/{comment_id}` returns the full comment with `aiProvenance` populated.
6. **PDF (upload) happy path**: FE extracts text and computes the PDF SHA-256 client-side with MuPDF.js + Web Crypto, then POSTs `{ document_type: "upload", paper_text, pdf_content_sha256, gene_id, external_url?, external_title? }`. `comment_ai_run.pdf_content_sha256` matches the value the FE supplied; otherwise as #4 (+ publish as #5).
7. **Gene not in paper**: POST a PMID that doesn't mention the gene → terminal `gene-not-mentioned` with `synonyms_checked` populated. `comment_ai_run` row written (terminal_status='gene-not-mentioned'); **no comment row** until the user publishes (the publish body is then entirely user-written → `is_edited=true`).
8. **Mentioned only in passing**: POST a PMID where the gene appears trivially → terminal `mentioned-in-passing`. `comment_ai_run` row written; no comment row until publish.
9. **Text unavailable**: POST a non-OA / restricted PMID → terminal `text-unavailable` with `reason` populated, **no rows** in `comment_ai_run`, `comments`, or `comment_ai_provenance`. Retrying re-runs the fetch. A publish attempt against this `job_id` returns **404**.
10. **Cache hit across users**: user A completes a run and publishes; user B POSTs the same `gene_id` + `pubmed_id` → POST returns within ~2s with `success { ai_output, sibling_summary: { reviewed: 1, edited: 0, … } }` (reflecting A's published comment), **no `comment_id`**. When B publishes, a fresh `comments` + `comment_ai_provenance` row exists for B, referencing the same `run_job_id` as A's.
11. **In-flight attach**: user A submits a job; before it completes, user B POSTs the same digest → both poll the same `job_id` and both receive the terminal `ai_output`. No comment rows are created by attaching; each user publishes independently.
12. **Pool exhaustion**: saturate the 8-thread executor; ninth submit returns `503 Service Unavailable` with `Retry-After`.
13. **Resume**: submit a job, then `GET` with the same `job_id` after a tab refresh → returns the live state (or the terminal `ai_output`) without resubmitting. (Server restart between submit and resume returns 404 — FE handles this.)
14. **Cancel**: submit a job, immediately DELETE → polling returns `type: 'cancelled'`, the executor thread terminates promptly, no `comment_ai_run` or `comment` row is created.
15. **Registry TTL eviction**: submit, let it complete, wait >10 min, GET → live registry returns 404 but a fresh POST (or a publish) with the same digest still resolves via the permanent `comment_ai_run` row.
16. **Regression**: existing `/user-comments` endpoints (POST/GET/DELETE/attachments) still work unchanged; **no consumer of the `comments` table needs an unreviewed-AI exclusion**, since unreviewed runs never create `comments` rows.

_(A "validation off" verification item was removed with the validation stage on 2026-06-05.)_

---

# Implementation progress & session handoff

_Last updated: 2026-06-05. We are executing this plan via the `superpowers:executing-plans` workflow (one reviewable deliverable at a time, TDD for pure logic). Branch: `feature-ai-user-comments`._

## Deliverable status

| # | Deliverable | Status |
|---|-------------|--------|
| 0 | Scaffolding (all classes, 501/no-op, builds clean) | ✅ **Done & committed** |
| 1 | Sync prelude (validate, synonyms, digest, cache lookup, registry attach, spawn/503, POST+GET wiring) | ✅ **Done** — full Service build green, **21 unit tests pass** |
| 2 | PMC BioC article fetcher (`fetching-article`) | ✅ **Done** — full Service build green, **31 unit tests pass** (+10 new); live HTTP path smoke-tested then throwaway IT removed |
| 3 | Gene-mention regex scanner (`scanning-gene-mentions`) | ✅ **Done** — full Service build green, **53 unit tests pass** (+20 new) |
| 4 | Anthropic call + JSON retry (`generating-summary`) | ✅ **Done** — full Service build green, **74 unit tests pass** (+21 new); real Anthropic path compiles but **NOT live-tested** (needs `CLAUDE_API_KEY` + running WdkModel) |
| 5 | ~~Validation pass (`verifyGeneSummary`)~~ → **Flatten to comment** (`ai_output` headline+content) | ✅ **Done** — validation step removed per Python authors (2026-06-05); flatten implemented. Full Service build green, **81 unit tests pass** (+7 new) |
| 6 | Persist (`comment_ai_run` **only** — pipeline creates no comment) | ✅ **Done (code)** — full Service build green, **86 ai-package tests pass** (+6 new); SQL binding (incl. `synonyms_used TEXT[]`) **NOT live-tested** (Steve created the tables; live smoke test deferred to dev-server deploy) |
| 7 | **Publish endpoint** (`POST {id}/publish` → `comment` + `comment_ai_provenance`, `is_edited`) | ✅ **Done (code)** — full Service build green, **91 ai-package tests pass** (+5 new); SQL `INSERT`/tx **NOT live-tested** (deferred to dev-server deploy). **Remaining sub-piece → D7b:** the anonymous `sibling_summary` aggregate (counts over `comment_ai_provenance`) in the cache-hit/terminal responses. |
| 7b | **`sibling_summary` aggregate** — counts over `comment_ai_provenance` (`reviewed`/`edited`/`latest_at`) wired into the cache-hit + live terminal responses | ✅ **Done (code)** — full Service build green, **93 ai-package tests pass** (+2 new); aggregate SQL **NOT live-tested** (dev-server deploy) |
| 8 | DELETE / cancel wiring | ✅ **Done** — full Service build green, **96 ai-package tests pass** (+3 new) |
| 9 | Registry eviction scheduler | ⬜ Pending (`_evictor` field present, unused) |

> **⚠ Plan pivot (2026-06-02) — review-on-approval.** After D2, the team simplified the model: a completed run now writes **only** the `comment_ai_run` cache row; the `comments` + `comment_ai_provenance` rows are created **only when the user approves & publishes**, via a new `POST …/{job_id}/publish` endpoint. Consequences already folded into the plan above and the items below:
> - Terminals no longer carry `comment_id`; cache-hit returns cached `ai_output` + `sibling_summary` only.
> - `comment_ai_provenance.review_level`/`reviewed_at` → **`is_edited` BOOLEAN + `created_at`**; `sibling_summary` is now `{ reviewed, edited, latest_at }` (no `unreviewed`).
> - **Code changes applied (2026-06-02), build green, 31 tests pass**: removed `create_user_comment` from `AiGenePublicationRequest.Options`, the `post-request.json` schema, and `JobDigestTest` (canonical-options JSON now `{generate_product_description,validate}`); made `AiGenePublicationPipeline.run()` call `persist()` unconditionally; deleted the vestigial `JobState._commentIdsByUser` / `recordComment` / `getCommentId` (+ unused imports); reshaped `AiProvenance` POJO (`isEdited`/`createdAt`, dropped the `ReviewLevel` enum) and `InsertCommentAiProvenanceQuery` SQL (`is_edited`, `created_at`); updated `migration_comment_b70.sql` DDL. **Still owed when the publish deliverable lands**: D1's `cacheHitJson` `sibling_summary` aggregate; the publish endpoint + `CommentFactory`/`CommentRequest` aiProvenance wiring.

> **⚠ Plan change (2026-06-05) — validation step removed.** The Python authors reported the `verifyGeneSummary` second pass didn't materially improve results and was a waste of tokens in production. D5 therefore collapses from "validation + flatten" to **flatten-only**, and the pipeline runs a single LLM stage. **Code changes applied (2026-06-05), build green, 81 tests pass**: removed `validate` from `AiGenePublicationRequest.Options` (canonical-options JSON now `{generate_product_description}` — `JobDigestTest` updated); removed `JobStatus.VALIDATION_ERROR`, `JobState.Stage.VALIDATING`, the `run()` validate block, `validateSummary()`, and `_validatedJson`; renumbered pipeline stage markers (flatten ⑤→④, persist ⑥→⑤); deleted the `verifyGeneSummary` prompt resource dir; dropped `validate`/`validating`/`validation-error`/`errors` from `post-request.json` + `status-response.json`. PD generation (deferred) will derive from the first-pass summary. **FE plan intentionally NOT updated** — the user will reconcile it separately (it still references `options.validate`, the `validating` stage, and a `validation-error` outcome).

## Decisions locked (with rationale)

- **LLM model = `claude-sonnet-4-20250514`** — matches the Python pipeline the prompts were tuned against. Held in `AiSummaryConfig.MODEL_NAME`. Baked into the `jobId` digest.
- **`PROMPT_VERSION = "1"`** (`AiSummaryConfig`) — manually bumped when prompt files change; baked into digest. Now covers the single `getGeneSummary` prompt (the verify prompt was dropped 2026-06-05).
- **Comment schema = `usercomments`** — confirmed via conifer `commentconfig_commentSchema: usercomments` (→ `CommentConfig.commentSchema`, what `CommentFactory.getCommentSchema()` returns). The plan's original DDL was right; an earlier `userlogins5` worry came from the stale `migration_comment_b21.sql`. **Migration file is now `ApiCommonModel/Model/lib/sql/migration_comment_b70.sql`** (renamed by user). Tables requested from Steve for `userdb_devn` — **no ETA**.
- **Test harness = JUnit 4 added to the Service module** (it had none; `ApiCommonWebsite` had zero unit tests). Tests live in `Service/src/test/java/...ai/`.
- **`GeneSynonymService` uses `wdkModel.getSystemUser()`** — gene aliases are public, non-user-owned data.
- **`JobRegistry` = process-wide singleton** (`JobRegistry.instance()`); package-private `(ExecutorService, ScheduledExecutorService)` constructor for tests; 8-thread pool; `putIfAbsent` dedupe; on `RejectedExecutionException` it removes the entry and rethrows (caller → 503).

## Classes added beyond the original file-layout table

- `services/ai/AiGenePublicationRequest.java` — POST-body DTO (Jackson, snake_case).
- `services/ai/JobDigest.java` — pure: `canonicalOptionsJson(Options)` + `compute(...)` SHA-256. **8 tests.**
- `services/ai/AiSummaryConfig.java` — `MODEL_NAME`, `PROMPT_VERSION` constants.
- `services/ai/JobSubmission.java` — immutable resolved-inputs carrier built by the prelude, read by the pipeline.
- `SyncPrelude.PreludeResult` (nested) — `CACHE_HIT | RUNNING` outcome the resource maps to a response.
- `services/ai/TerminalResult.java` (D2) — value type for the terminal payload published on `JobState` (rendered by the resource's `jobStateJson`). D2 covers `textUnavailable(reason)` + `internalError(error)`; success / gene-not-mentioned / sibling_summary factories land in D4–6.

**`JobState` was refactored** from a per-follower `Submitter`-with-full-request to: one shared `JobSubmission` + `List<Long> followerUserIds` + a `userId→commentId` map (filled at persist). `JobState.getJobId()` delegates to the submission. Three state tiers: immutable submission (shared) · follower ids (per-user) · volatile stage/result (published; transient stage outputs live as pipeline instance fields). **Post-pivot (done 2026-06-02):** the `userId→commentId` map / `recordComment` / `getCommentId` were **removed** (the pipeline no longer creates comments); `followerUserIds` stays for dedupe/observability only.

## Implemented this far (what's real vs stub)

**Implemented + unit-tested (pure):** `SyncPrelude.validate` (7 tests), `JobDigest` (8 tests), `JobRegistry.submit/attach/get` (6 tests), `PmcBiocFetcher.parseBiocJson` (7 tests, D2), `AiGenePublicationPipeline.fetchArticle` upload/pubmed/text-unavailable (3 tests, D2), `GeneMentionScanner.countSubstrings`/`namesMentioned` (20 tests, D3), `AiGenePublicationPipeline.scanGeneMentions` gene-found/gene-not-mentioned (2 tests, D3), `PromptLoader` (6 tests, D4), `AnthropicJsonClient.extractJson`/retry loop (8 tests, D4), `AiGenePublicationPipeline.geneForPrompt`/`generateSummary` (6 tests, D4).
**Implemented, compiles, live-smoke-tested:** `PmcBiocFetcher.fetch` (D2 — real BioC HTTP path verified against PMID 17299597 (27 KB) and a bogus PMID → `text-unavailable`; throwaway IT then removed).
**Implemented, compiles, NOT live-tested** (need running WdkModel / DB tables): `GeneSynonymService.resolve` (reads gene `Alias` table), `GetCommentAiRunQuery.parseResults` + `CommentFactory.findAiRun`, `SyncPrelude.handleSubmit/resolveSynonyms/computeJobId`, `AiGenePublicationCommentService` POST (auth, spawn/attach/cache, 503) + GET (registry→cache→404), `AiGenePublicationPipeline.run()` orchestration (terminal short-circuit after each stage; top-level `Throwable` → `internal-error`).
**Implemented, compiles, NOT live-tested (need `CLAUDE_API_KEY` + network):** `AnthropicJsonClient` real `LlmCompleter` (the actual Anthropic call) — only the parse/retry orchestration is unit-tested via an injected completer.
**Implemented + unit-tested (pure, D5):** `AiGenePublicationPipeline.flattenHeadline`/`flattenContent` (6 tests) + `flattenToComment` wiring (1 test).
**Still stubs:** none of the AI flow's endpoints — POST/GET/DELETE/publish are all wired. _(`validateSummary` was removed, not stubbed — see the 2026-06-05 plan change. D6: `persist`/`InsertCommentAiRunQuery`; D7: publish + `InsertCommentAiProvenanceQuery`; D7b: `sibling_summary`; D8: `JobRegistry.cancel` + DELETE.)_ Remaining work is D9 (registry eviction scheduler — `_evictor` injected but no sweep scheduled yet).

## Interim caveats (by design, not bugs)

- **`run()` is now fully wired through persist (D6).** It calls stages in order, skips ahead once a stage marks the job terminal, then calls `persist()` for **every** cacheable terminal, and catches any `Throwable` → terminal `internal-error`. A live job reaches `fetching-article` → `scanning-gene-mentions` → `generating-summary` → `flatten` (④, D5) → `persist` (⑤, D6), which writes the `comment_ai_run` row and marks terminal `success` (publishing `ai_output`). (Terminates earlier as `gene-not-mentioned` (deterministic) / `mentioned-in-passing` (LLM `only_in_passing=true`) — **both now also write a `comment_ai_run` row** in `persist`; a non-OA pubmed id terminates as `text-unavailable`, which is **not** persisted.) NB: the real Anthropic call and the SQL `INSERT` are both wired but unexercised by tests — the first live run on the dev server is itself a smoke test of `buildCompleter` + `persistAiRun`.
- **Cache-hit + live terminal responses now carry `sibling_summary`** (D7b): the three publishable terminals (`success`/`mentioned-in-passing`/`gene-not-mentioned`) include `{reviewed, edited, latest_at}` aggregated over `comment_ai_provenance` for the run job id. Post-pivot the cache-hit creates no comment (no `comment_id`).

## Build / test notes (important for next session)

- Build chain order: `install/` → `WDK/` → `ApiCommonWebsite/Model` → `ApiCommonWebsite/Service`. **`ai-wdk` IS `project_home`** (CLAUDE.md paths say `project_home/…`).
- **Service resolves Model from `.m2`**, so after changing Model run `cd ApiCommonWebsite/Model && mvn install -DskipTests` before building/testing Service, or Service won't see new Model methods (e.g. `findAiRun`).
- Run one test class: `cd ApiCommonWebsite/Service && mvn -Dtest=JobDigestTest test`. All AI tests: `-Dtest=JobDigestTest,SyncPreludeTest,JobRegistryTest`.
- Non-fatal POM warnings (`version is missing` for `javax.servlet:servlet-api`, `classloader-leak-prevention-servlet`) appear but the build SUCCEEDS — ignore.
- `mvn test`/`mvn compile` from `ApiCommonWebsite/Service` is the quick loop.

## Reference facts already gathered for upcoming deliverables

Source of truth = Python `VPDB_AI_gene_paper_summary/` (at `../VPDB_AI_gene_paper_summary`), esp. `PubGene_back_end/helpers.py` and `pipeline/prompts.py`.

- **D2 (BioC fetch): ✅ DONE.** `GET https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/{pmid}` via Java `HttpClient` (180 s read timeout, `Accept: application/json`); keep passages where `infons.section_type ∈ {FIG, TABLE, RESULTS, CONCL, DISCUSS, SUPPL}`; concat non-empty `text` with `\n` (`PmcBiocFetcher.parseBiocJson`, pure + unit-tested). Non-2xx / non-`application/json` content-type / malformed JSON / **empty parse result** → `TextUnavailableException` → terminal `text-unavailable` (not cached). Upload path: `fetchArticle()` uses `submission.getUploadedPaperText()` directly. **⚠ Deviation from Python (user-approved):** Python `PUBMED_SECTIONS` uses `DISCUSSION`, but the live PMC BioC vocabulary emits `DISCUSS` — so Python silently dropped *all* discussion sections. The Java port uses `DISCUSS` (verified empirically against PMID 17299597). **Report this bug to the Python team** (prompts were tuned on output that excluded discussion text, so re-tuning may be warranted). Also: treating an empty parse as `text-unavailable` (rather than Python's empty-string pass-through) avoids caching a false `gene-not-mentioned` for a paper we couldn't extract.
- **D3 (`_count_substrings`, helpers.py:264): ✅ DONE.** `GeneMentionScanner.countSubstrings` ports the matcher: if alias matches `^[A-Za-z]+[0-9]+$` → inner regex `Pattern.quote(letters) + "-?" + Pattern.quote(digits)` (so `Nd6`↔`Nd-6`); else collapse each run of `_`/`-`/space to `[-_\s]+` and `Pattern.quote` the rest; wrap as `(?<![A-Za-z0-9]) inner (?![A-Za-z0-9])`, `CASE_INSENSITIVE`; count `Matcher.find()` hits. `GeneMentionScanner.namesMentioned` ports `aliases_mentioned_in_paper`: gene_id first iff mentioned (tries `_`→`-` variant), then aliases with count `>0` sorted **count desc then name asc**, capped at top-3 **then** de-duped case-insensitively against the gene id (matching Python's order — no backfill of a 4th alias into a gap left by a case-collision dupe; covered by a dedicated test). `AiGenePublicationPipeline.scanGeneMentions` stores that list (prompt input for D4) or, if empty, marks terminal `gene-not-mentioned` with `synonyms_checked = [geneId, …synonyms]` (rendered by `TerminalResult.geneNotMentioned`; `sibling_summary` aggregate still TODO for D6/D7). **20 unit tests** (12 `countSubstrings`, 8 `namesMentioned`) + 2 pipeline-stage tests. **Note:** Python only tries the `_`→`-` fallback for the gene_id, not for aliases — Java matches this.
- **D4 (LLM): ✅ DONE.** Prompts ported into `resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}` from `pipeline/prompts.py`. `PromptLoader` (classpath load + cache, split `user.txt` into turns by blank line **before** substitution, naive `[PH]` `String.replace`; **6 tests**). `AnthropicJsonClient` (`implements JsonPromptClient`): `extractJson` (fence-strip + parse, null on fail), formatter-retry loop (`MAX_RETRY=3`, port of STEP_1 loop — formatter system prompt copied verbatim incl. the "respong" typo), and a real `LlmCompleter` built via `AnthropicOkHttpClientAsync.builder().apiKey(CLAUDE_API_KEY).maxRetries(32).checkJacksonVersionCompatibility(false)`, `max_tokens=20000`, `temperature=0`, system + one `addUserMessage` per turn + `addAssistantMessage("{")` prefill, blocking `.join()`, prefill prepended to the result (**8 tests** via injected completer; the real network path is **NOT live-tested**). Pipeline `generateSummary`: builds `[N_QUOTES]=2 / [GENE]=geneForPrompt(geneId,namesMentioned) / [PAPER_TEXT]=articleText / [JSON_SCHEMA]=schema.json`, calls the client, stores `_summaryJson`, and on `only_in_passing=true` → terminal `mentioned-in-passing` (persisted; `synonyms_checked=[geneId,…synonyms]`, `TerminalResult.mentionedInPassing`). `geneForPrompt` ported from `gene_for_prompt` (**3 tests**); generateSummary (**3 tests**). **⚠ Faithful-port quirks reproduced in `system.txt` (report upstream):** Python string-concat typos `"The geneor one of"`, `"measurements   - Be"` (missing `\n`), `"7. FORMAT COMPLIANCEStructure"` (missing space) — kept verbatim so output matches what prompts were tuned on. The formatter system prompt's "respong with the corrected JSON" typo is likewise verbatim.
- **D5 (flatten): ✅ DONE.** Validation pass **removed** (see the 2026-06-05 plan change) — D5 is flatten-only. `AiGenePublicationPipeline.flattenHeadline` returns `ShortSummary` (empty if absent); `flattenContent` ports the structure of Python `build_extended_summary_html` to **plain-text markdown** (no HTML): `- ` gene-summary bullets each with an indented `Evidence: <location>` line (omitted when blank) and `  > <quote>` lines, then an optional `Additional inferences:` section, then an `Aliases mentioned in paper: a, b` line; sections joined by a blank line, absent/empty sections omitted. `flattenToComment` stores `_aiHeadline`/`_aiContent` (consumed by D6 persist + the success terminal). **7 tests** (6 pure render + 1 wiring). **Elaboration beyond the plan's literal stage-④ text:** the plan listed only bullets/Evidence/quotes + aliases header; I also render `AdditionalInferences` (it's part of the summary and Python's renderer includes it). Plan stage table updated to match.
- **D7 (publish endpoint): ✅ DONE (code; live-test deferred).** `POST /user-comments/ai-gene-publication/{job_id}/publish`, body `{headline, content}` only (a new `PublishRequest` DTO). Flow: `fetchUser` (401 guests) → 400 if headline/content blank → `findAiRun(jobId)` (404 if no run row — covers the never-persisted `text-unavailable`/`internal-error` outcomes) → `buildPublishComment` → `CommentFactory.createComment` → **201** `{comment_id}`. `buildPublishComment` (pure, **5 tests**) maps the run row → `CommentRequest`: gene `Target{type=GeneSynonymService.GENE_URL_SEGMENT("gene"), id=run.geneId}`, headline/content from the body, and an `AiProvenance{runJobId, createdAt, isEdited}`. **`is_edited` = submitted text ≠ the run's `ai_headline`/`ai_content`** (via `Objects.equals`) — so when the AI original is null (`gene-not-mentioned`/`mentioned-in-passing`) any written body is correctly `edited=true`. `createComment` now inserts the `comment_ai_provenance` row (sets `commentId` on the provenance, runs `InsertCommentAiProvenanceQuery`) **inside the existing comment transaction**, just before `con.commit()`; `InsertCommentAiProvenanceQuery.getArguments` binds `(comment_id BIGINT, run_job_id VARCHAR, is_edited BOOLEAN, created_at TIMESTAMP)`. Added `_aiProvenance` to `CommentRequest`. **91 ai-package tests pass (+5).** **SQL/tx NOT live-tested** — deferred to dev-server deploy (consistent with D6). **No other repos touched** (the whole comment subsystem lives in `ApiCommonWebsite/Model`). **⚠ TODO(post-impl, user-requested):** resolve & set the comment `ORGANISM` from the gene record (`GeneRecordClasses.GeneRecordClass`) rather than leaving it null — organism is optional for comment creation so v1 omits it; marked in code at `buildPublishComment`. **Split out → D7b:** the `sibling_summary` aggregate.
- **D8 (DELETE / cancel): ✅ DONE.** `DELETE /user-comments/ai-gene-publication/{job_id}` → `fetchUser` (401 guests) → `JobRegistry.cancel(jobId)` → **204**. `cancel` is a no-op for an unknown/evicted/already-terminal job (idempotent); for a live job it **marks `CANCELLED` first, then interrupts the future** (`future.cancel(true)`). `JobState.markTerminal` is now **synchronized + first-terminal-wins** (returns boolean) — this is what makes cancellation stick: the interrupted pipeline thread's `catch(Throwable) → internal-error` (D6 `run()`) is ignored because the job is already terminal. A cancelled job is **not** persisted (D6 `persist()` skips non-publishable terminals). v1 cancels for **all** followers (no per-follower detach — see out-of-scope #9). **3 new tests** (cancel marks running job CANCELLED + cancels the future; unknown-job no-op; a finished job is not overwritten by a late cancel); **96 ai-package tests pass.** **⚠ Limitation:** `future.cancel(true)` interrupts the worker thread, but the Anthropic `AnthropicOkHttpClientAsync` `.join()` (D4) may not be promptly interruptible — an in-flight LLM HTTP call can keep running in the background until it returns; the job is already `cancelled` and its result is discarded (and not cached). Acceptable for v1; revisit if cancelled-but-still-billing LLM calls become a concern.
- **D7b (`sibling_summary`): ✅ DONE (code; live-test deferred).** New `SiblingSummary` POJO (`reviewed`/`edited`/`latestAt`) + `GetSiblingSummaryQuery` (`COUNT(CASE WHEN NOT is_edited …)` / `COUNT(CASE WHEN is_edited …)` / `MAX(created_at)` `WHERE run_job_id=?`; portable `CASE` not PG-only `FILTER`; always returns a row → `(0,0,null)` when no siblings) + `CommentFactory.getSiblingSummary(runJobId)`. Added `JobStatus.isPublishable()` (the three persisted/publishable terminals). Service rendering methods (`preludeJson`/`jobStateJson`/`cacheHitJson`) became instance + `throws WdkModelException`; they now attach `sibling_summary` to the **cache-hit** response (always — cache rows are publishable) and the **live terminal** response (publishable terminals only), via a pure static `siblingSummaryJson` (`{reviewed, edited, latest_at}`; `latest_at` = ISO-8601 UTC `Instant.toString()` or JSON null). **+2 pure tests** (count/ISO mapping; null `latest_at` → JSON null); **93 ai-package tests pass.** Aggregate SQL **NOT live-tested** (dev-server deploy).
- **D6 (persist): ✅ DONE (code; live-test deferred).** Stage ⑤. `AiGenePublicationPipeline.persist()` writes one `comment_ai_run` row for each **cacheable** terminal (`success` / `gene-not-mentioned` / `mentioned-in-passing`) via the new `CommentFactory.persistAiRun(CommentAiRun)`, and on the success path advances stage to `persisting` then marks terminal `success` with the flattened `ai_output` (`TerminalResult.success(headline,content)` — same wire shape as `cacheHitJson`). Non-cacheable terminals (`text-unavailable`/`internal-error`/`cancelled`) write nothing. `buildRun` maps the immutable `JobSubmission` + flattened fields onto the row. `InsertCommentAiRunQuery` now binds all 16 columns; **`synonyms_used TEXT[]` is bound as a real PG array** via `con.createArrayOf("text", …)` — done by overriding `run(Connection)` to capture the connection before `getArguments()` (the `ArgumentBatch` seam has no connection of its own). Test seam: a narrow `AiGenePublicationPipeline.AiRunStore` functional interface (default impl = `CommentFactoryManager.getCommentFactory(projectId)::persistAiRun`), injected in tests to capture the row with no DB. **+6 pipeline tests** (success persists row + publishes `ai_output`; gene-not-mentioned & mentioned-in-passing persist; text-unavailable & persist-failure don't); **86 ai-package tests pass**. **SQL `INSERT` path NOT live-tested** — Steve created the tables in `userdb_devn`; live smoke deferred to dev-server deploy (consistent with the other DB-touching code). **⚠ Latent bug fixed by D6:** the old `run()` returned *before* `persist()` on the `gene-not-mentioned`/`mentioned-in-passing` short-circuits, so those "persisted" terminals (per the schema's intent) were never actually written; the restructured `run()` (skip-ahead instead of early-return, single tail `persist()`) closes that gap.
- **Gene aliases (D1, done):** gene record `Alias` table — `queryRef="GeneTables.Alias"`, column `alias` (also `id_type`, `db_name`), `displayName="Names, Previous Identifiers, and Aliases"`. Read via `RecordInstance.getTableValue("Alias")`, exclude the queried id, sort/dedupe. Parity with Python `get_vpdb_alias`.

## Port quirks & deviations from the Python pipeline

Single registry of every place the Java port knowingly differs from `VPDB_AI_gene_paper_summary/`, so nothing is lost between deliverables. Full per-deliverable detail lives in the "Reference facts" D2/D3/D4 bullets above; this is the index. Three kinds: **(A)** upstream Python bugs reproduced verbatim (the prompts were tuned on the buggy output, so "fixing" them here would diverge from what was validated — these are the ones to **report to the Python team**); **(B)** deliberate Java deviations; **(C)** subtle faithful-match decisions worth remembering.

### A. Upstream Python bugs — reproduced verbatim, ☐ NOT yet reported to the Python team
- ☐ **`DISCUSSION` vs `DISCUSS`** (D2, `PmcBiocFetcher`): Python `PUBMED_SECTIONS` keeps `DISCUSSION`, but the live PMC BioC vocabulary emits `DISCUSS`, so Python silently drops *all* discussion passages. Java uses `DISCUSS` (verified vs PMID 17299597). Prompts were tuned on output that excluded discussion text → re-tuning may be warranted.
- ☐ **`getGeneSummary` `system.txt` string-concat typos** (D4): `"The geneor one of"` (missing space), `"measurements   - Be especially"` (missing `\n` between bullets), `"7. FORMAT COMPLIANCEStructure"` (missing space). Kept verbatim.
- ☐ **Formatter-prompt typo** (D4): `"respong with the corrected JSON ONLY and nothing else."` — verbatim from the STEP_1 retry-loop system prompt.

### B. Deliberate Java deviations (intentional — not bugs to report)
- **`verifyGeneSummary` validation pass not ported** (D5, 2026-06-05): the Python pipeline has an optional second-pass `verifyGeneSummary` (gated on `validate`), but the Python authors found it didn't improve results enough to justify the tokens, so the Java port omits it entirely (no `validate` option, no `validating` stage, no `validation-error`). PD generation (deferred) will derive from the first-pass summary. NB: in Python the verify output never fed the displayed summary anyway — only the (deferred, arguably-broken) PD path.
- **`AdditionalInferences` rendered in the flattened comment** (D5): the plan's stage-④ text listed only bullets/Evidence/quotes + an aliases header, but the flatten also emits an "Additional inferences" section (it's part of the summary and Python's `build_extended_summary_html` includes it). Plan stage table updated to match.
- **Empty BioC parse → `text-unavailable`** (D2): Python returns an empty string (which would then cache a false `gene-not-mentioned`); Java treats an empty extraction as `text-unavailable` (not cached), so a paper we couldn't extract stays retryable.
- **No Anthropic prompt caching** (D4): Python `call_prompt` tags the first (paper-text) user turn with `cache_control: ephemeral` to cache across multiple genes of the same paper. Our per-job flow is one gene per paper, so that cache would never hit — the Java `LlmCompleter` omits `cache_control`. **Revisit if batch / multi-gene-per-paper submission is ever added.**
- **`[JSON_SCHEMA]` substitution** (D4): Python injects `json.dumps(schema_dict, indent=2)`; Java injects the raw `schema.json` file text. Equivalent JSON, different incidental whitespace — no behavioural effect.
- **Trailing whitespace not reproduced** (D4): a couple of trailing spaces in the Python prompt string literals (`"…paper text: "`, `"…bullet points "`) are dropped in the `.txt` files. Cosmetic.

### C. Subtle faithful-match decisions (matched Python on purpose — don't "fix")
- **Top-3 cap THEN dedup** (D3, `namesMentioned`): cap mentioned aliases at 3 *before* de-duping the case-collision gene-id dupe — Python does not backfill a 4th alias into the gap. Locked by a dedicated test.
- **`_`→`-` fallback for the gene id only** (D3): Python tries the hyphen variant for the gene id but *not* for aliases — Java matches.

## Open coordination items (unchanged from "Out of scope")

- DB tables in `userdb_devn` (Steve) — blocks D6 live test + all cache-hit/persist live verification.
- EbrcWebsiteCommon: `CommentFactory.createComment` transaction extension for `comment_ai_provenance` + `CommentRequest`/`Comment` `aiProvenance` field (D6). Note: `CommentFactory` is local to `ApiCommonWebsite/Model` (not Ebrc) — confirmed.
- RAML doc (`Service/doc/raml/apicommonwebsite.raml`) for the 3 endpoints — not yet written.
- **Report the upstream Python bugs** (the `DISCUSS` section-type bug + the `getGeneSummary`/formatter prompt typos) to the Python team — see "Port quirks & deviations" §A; none reported yet.
