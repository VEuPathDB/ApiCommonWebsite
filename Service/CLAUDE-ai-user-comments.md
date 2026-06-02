# Plan: AI-Assisted Gene-Publication Summary — Back-End Service (Java)

## Context

VEuPathDB is adding a new kind of user comment: an **AI-assisted gene-publication summary**. A user supplies a gene (via URL `stableId`) and a publication (a PubMed ID or an uploaded PDF). A new back-end service:

1. Resolves the publication text (PMC BioC API for PubMed; for uploads, the FE has already extracted text client-side with MuPDF.js and sends it in the POST body — the PDF itself never reaches the server).
2. Looks up gene synonyms from VEuPathDB and verifies the gene is mentioned in the text.
3. Runs an LLM to generate a gene-function summary, with an optional validation pass.
4. Persists a `user_comment` row carrying an `aiProvenance` record so the FE can show a review-and-publish form.

The pipeline already exists in Python (`VPDB_AI_gene_paper_summary/`). For this v1 we are **porting it to Java** rather than wrapping it — the user has chosen this trade-off explicitly. The companion front-end plan (`CLAUDE-plan-ai-user-comments-front-end.md`) defines the REST contract and is the source of truth for the wire shape; this plan implements the back-end side of that contract.

## Architecture overview

A new JAX-RS resource `AiGenePublicationCommentService` lives in `ApiCommonWebsite/Service`, registered in `ApiWebServiceApplication.getClasses()` alongside `UserCommentsService`. It exposes three endpoints (matching the FE contract), runs jobs asynchronously on a bounded thread pool, tracks job state in an in-memory map with TTL eviction, and on success calls the existing `getCommentFactory().createComment(...)` to persist the result.

**Submit lifecycle.** From the FE's perspective the whole flow is **two HTTP interactions** in both cases (our POST, then polled GETs). For the upload path, the FE does the PDF heavy lifting locally: it extracts text using **MuPDF.js** (WASM, lazy-loaded only when the user picks the upload tab) and computes a SHA-256 of the PDF bytes, then sends `{ paper_text, pdf_content_sha256, ... }` in the POST body — the PDF itself never leaves the user's machine. Inside our POST handler there are **two phases**: (a) a *synchronous prelude* (target <2s) that resolves gene synonyms in-process, computes a content-digest `jobId`, looks up `comment_ai_run` and the in-memory registry, and either returns a cache-hit response or attaches the caller to an in-flight job, then (b) the *asynchronous pipeline* that runs only on a true miss.

**Identifiers.** The `jobId` is a hex SHA-256 over `(geneId, sorted-resolved-synonyms, source-key, modelName, promptVersion, optionsCanonicalJson)`, where the source-key is the PubMed id (PubMed path) or the FE-supplied `pdf_content_sha256` (upload path) and `optionsCanonicalJson` is the request's `options` object rendered as canonical JSON (Jackson with `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY`, no whitespace, missing/null fields normalised to defaults). `promptVersion` is a manually-bumped constant per prompt stage (one per `getGeneSummary` / `verifyGeneSummary`) — devs bump it when they edit the prompt files, the same way they'd bump a schema version. Hashing the whole `options` blob means a future option that changes LLM output (e.g. `generate_product_descriptions`) is automatically covered without anyone having to remember to update the digest formula. The minor cost is wasted cache misses when two submits differ only on per-submitter options like `create_user_comment` — negligible in practice since that one is `true` almost always. The same `jobId` keys the in-memory registry and the `comment_ai_run` cache row, so double-tap submits and cross-user resubmissions naturally dedupe.

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
│  POST  /user-comments/ai-gene-publication       → status/job_id │
│  GET   /user-comments/ai-gene-publication/{id}  → status        │
│  DELETE /user-comments/ai-gene-publication/{id} → cancel        │
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
│  ④ validateSummary   (Anthropic call — verifyGeneSummary prompt) │
│  ⑤ flattenToComment  (structured JSON → headline + content)     │
│  ⑥ persist 6a: INSERT INTO comment_ai_run                       │
│            6b: per follower → CommentFactory.createComment      │
│                + INSERT INTO comment_ai_provenance              │
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
    "validate": true,
    "generate_product_description": false,
    "create_user_comment": true
  }
}
```

Snake-case keys match the existing comments-service JSON convention. No `user_id` — WDK identifies the caller. Typical `paper_text` size: 30–200 KB; well under standard servlet POST limits.

**POST response** uses the same union as the GET status response (i.e. POST may return a terminal state directly on a cache hit):

- `running { job_id, stage }` — fresh job spawned, or attached to an in-flight job with the same `job_id`.
- `success { job_id, ai_output, sibling_summary, comment_id }` — cache hit on `comment_ai_run`; submitter's own comment is created in-line and `comment_id` is returned.
- `mentioned-in-passing { job_id, synonyms_checked, sibling_summary, comment_id }` — cache hit; submitter's comment created.
- `gene-not-mentioned { job_id, synonyms_checked, sibling_summary, comment_id }` — cache hit; submitter's comment created.
- `text-unavailable { reason }` — never cached; the fetch will run on every retry.

`sibling_summary { unreviewed: N, reviewed: N, edited: N, latest_reviewed_at: ISO-8601 | null }` is anonymous aggregate data over `comment_ai_provenance` rows pointing at the same `run_job_id`. It exists so the FE can render a banner ("this combo was processed before by N others; you can still review and publish under your name") without revealing the other users.

**Terminal status union returned from GET on a completed job** (FE-contract additions in bold):
- `success { ai_output, comment_id, sibling_summary }`
- `gene-not-mentioned { synonyms_checked, sibling_summary }`
- **`mentioned-in-passing { synonyms_checked, sibling_summary }`** — new for v1
- `text-unavailable { reason }`
- `validation-error { errors }`
- `internal-error { error }`
- `cancelled`

`gene-not-mentioned` comes from the deterministic regex scan; `mentioned-in-passing` comes from the LLM.

**Stage names emitted in `running` state** (subset of FE-defined stages, only those v1 ships):
- `queued` → `fetching-article` → `scanning-gene-mentions` → `generating-summary` → `validating` (iff `options.validate`) → `persisting` (iff `options.create_user_comment`)

Gene-synonym resolution happens in the sync prelude before any `running` state is published, so it isn't an emitted stage. No `generating-product-description` in v1 (product descriptions deferred — see "Out of scope" below).

**Polling cadence**: server tolerates ~1/s polls; no rate limiting beyond standard `AbstractWdkService` behaviour. **Registry TTL: 10 min** after terminal state (≥5 min recommended by FE; we round up). `comment_ai_run` rows are permanent — they are the source of truth for cache hits after the registry entry has been evicted.

## Pipeline stage details

### Stage 0: synchronous prelude (inside POST handler)

Runs entirely on the request thread, target <2 s end-to-end. Performs no LLM calls and no external HTTP except (for PubMed) nothing yet — the article fetch happens in stage ①.

| Step | Action |
|------|--------|
| 0a | Validate request shape. For uploads, require both `paper_text` (non-empty) and `pdf_content_sha256` (64-char hex). |
| 0b | Resolve `gene_id` + aliases via `wdkModel.getRecordClass("GeneRecordClasses.GeneRecordClass")`. 404 if the stableId is unknown. |
| 0c | Compute `job_id = sha256(geneId ‖ sortedSynonyms ‖ sourceKey ‖ modelName ‖ promptVersion ‖ optionsCanonicalJson)` where `sourceKey = pubmed_id` (PubMed path) or `pdf_content_sha256` (upload path, supplied by the FE), and `optionsCanonicalJson` is the `options` object serialised with Jackson `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY` and default-normalisation. |
| 0d | `SELECT * FROM comment_ai_run WHERE job_id = ?`. **Hit** → aggregate `sibling_summary` from `comment_ai_provenance`, create the submitter's `comments` + `comment_ai_provenance` rows inline (per `options.create_user_comment`), return the terminal cache-hit response with `comment_id` and `sibling_summary`. |
| 0e | Registry lookup by `job_id`. **Hit** → register the caller as a follower of the in-flight job and return its current `running` state. |
| 0f | Miss → spawn the async pipeline on the bounded executor and return `running { job_id, stage: "queued" }`. **Pool full → 503 + `Retry-After`**. |

### Stages 1–6: async pipeline

| # | Stage | Implementation |
|---|-------|---------------|
| ① | `fetching-article` | If `document_type=pubmed`: GET `https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/{pmid}` via Java `HttpClient`, parse with Jackson, keep passages where `infons.section_type ∈ {FIG, TABLE, RESULTS, CONCL, DISCUSSION, SUPPL}`, concatenate `text` fields. Non-JSON / 404 / non-OA paper → terminal `text-unavailable` (not persisted to the cache). If `document_type=upload`: nothing to fetch — the FE-supplied `paper_text` is used directly. The stage still emits a `fetching-article` progress event for symmetry, but completes immediately. |
| ② | `scanning-gene-mentions` | Port `_count_substrings` from `PubGene_back_end/helpers.py` (regex matcher: handles `Nd6` ↔ `Nd-6`, `PF3D7_1133400` ↔ `PF3D7-1133400`, case-insensitive, non-alphanumeric boundaries). If the gene id and *all* aliases score 0 hits → terminal `gene-not-mentioned` with `synonyms_checked` populated (this outcome **is** persisted to `comment_ai_run`). Otherwise pass the top-3-by-frequency aliases into the prompt. |
| ③ | `generating-summary` | Anthropic API call. System prompt + user prompts loaded from `resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}`. Placeholder substitution: `[PAPER_TEXT]`, `[GENE]`, `[N_QUOTES]`, `[JSON_SCHEMA]`. Prefill assistant turn with `{` (matches Python). Strip markdown fences from response, parse JSON. On the response's `only_in_passing=true` → short-circuit to terminal `mentioned-in-passing` (persisted to `comment_ai_run`). Up to `max_retry=3` retries via formatter LLM on malformed JSON (port `extract_json` retry loop). |
| ④ | `validating` (iff `options.validate`) | Anthropic call using `verifyGeneSummary` prompt. Input: original summary JSON + paper text. Output: verified/corrected summary in the same shape. If validation finds blocking issues we can't recover from, terminal `validation-error` with the validator's notes (not persisted to the cache). |
| ⑤ | flatten | Compute `ai_output.headline = ShortSummary` (plain text). `ai_output.content` = bullet-flattened plain text that is also valid markdown — `- ` bullets with indented `Evidence:` / `>` quote lines and an "Aliases mentioned in paper:" header. No HTML; no markdown editor library is in the FE monorepo, so we stay plain-text-compatible while future-proofing for a markdown renderer on the show page (deferred). |
| ⑥ | `persisting` | **6a.** `INSERT INTO usercomments.comment_ai_run` (one row keyed by `job_id`; idempotent — survives retries cleanly). **6b.** For each follower in the registry (per the attach behaviour above), call `getCommentFactory().createComment(commentRequest, user)` and within its transaction also `INSERT INTO usercomments.comment_ai_provenance(comment_id, run_job_id, review_level='unreviewed')`. Both writes per-follower are atomic; the loop runs sequentially. Return each follower's `comment_id` in the terminal `success` they receive on their next GET. |

**Terminal outcomes that are persisted to `comment_ai_run`** (and therefore short-circuit future submits): `success`, `mentioned-in-passing`, `gene-not-mentioned`. **Not persisted** (retries are free): `text-unavailable`, `validation-error`, `internal-error`, `cancelled`. See "Out of scope / coordination items" for an optional `text-unavailable` observability log.

## Job state mechanism

In-memory `JobRegistry`:
- `ConcurrentHashMap<String, JobState>` keyed by the hex SHA-256 `job_id` (not a UUID). `JobState` carries the current `{stage, message, updated_at}`, the immutable submission summary, the running `Future`, a **follower list** `List<Submitter>` (each entry holds the userId plus the `CommentRequest` fields needed at persist time), and (when terminal) the result.
- Submit handler runs the sync prelude (stage 0); on a registry hit it appends to the follower list and returns the existing job's state, on a miss it spawns the pipeline on a fixed-size `ExecutorService` (cap: **8 threads**, sized for slow LLM calls). If the pool rejects the submission → `503 Service Unavailable` with `Retry-After`.
- Each pipeline stage calls back into the registry to update `JobState.progress` with `{ stage, message, updated_at }`.
- `ScheduledExecutorService` runs every 60 s and evicts entries whose terminal-state age exceeds 10 min. `comment_ai_run` rows are permanent and still satisfy late cache hits.
- DELETE: marks the job cancelled, calls `.cancel(true)` on the `Future`, and cancels any in-flight Anthropic HTTP call via the OkHttp client. Next poll from any follower returns `type: 'cancelled'`. Per-follower opt-out (one follower wants to detach without killing the job for others) is deferred — see "Out of scope".
- Server restart = all in-flight jobs lost. Acceptable for v1: jobs are seconds-to-minutes, and the `comments`/`comment_ai_provenance` rows are the durable artefacts once `persisting` has run. Lost mid-flight jobs surface to the FE as `not-found` (404) on the next poll, which already triggers a friendly "job expired, please submit again" UX.

## Database schema — two sidecar tables

Match the existing convention (Categories, References, Attachments all use sidecars off `comments`). The shared LLM output cache lives in `comment_ai_run`, keyed by the content-digest `job_id`. The per-user review state lives in `comment_ai_provenance`, keyed by `comment_id` with a FK to the run row. The current (possibly edited) headline/content live in `usercomments.comments` itself; the immutable AI original lives on the run row.

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
  review_level  VARCHAR(16)  NOT NULL,   -- 'unreviewed' | 'reviewed' | 'edited'
  reviewed_at   TIMESTAMP,               -- set when review_level transitions away from 'unreviewed'
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

The current (possibly edited) headline/content live in `usercomments.comments`. The immutable AI original lives in `comment_ai_run.ai_headline` / `ai_content`. A diff between them reconstructs what the user edited; the FE doesn't need separate `edited_*` columns on the provenance row.

Style follows the existing comment-sidecar conventions: `BIGINT` for the comment id, `VARCHAR`/`TEXT` for strings, grants to `COMM_WDK_W`/`GUS_R`, table names under `usercomments.comment_*`. `ai_headline` sized to match `comments.headline VARCHAR(2000)`.

## Java implementation: file layout

### New code (this plan)

| Action | Path |
| ------ | ---- |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationCommentService.java` — JAX-RS resource (POST/GET/DELETE), `@Consumes(APPLICATION_JSON)` end-to-end |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobRegistry.java` — in-memory job store, follower list, bounded executor, scheduled eviction |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobState.java` + `JobStatus.java` — value types matching FE contract |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationPipeline.java` — orchestrator: runs stages ①–⑥, emits progress callbacks |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/SyncPrelude.java` — gene-synonym resolution, PDF content hash, digest, cache/registry lookup, follower attach |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/article/PmcBiocFetcher.java` |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneSynonymService.java` (WDK `RecordClass` lookup) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneMentionScanner.java` (regex matcher port) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/llm/AnthropicJsonClient.java` (thin wrapper: load prompt files, substitute placeholders, parse JSON with retry) |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/CommentAiRun.java` — POJO for the cache row |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/AiProvenance.java` — POJO for the per-comment provenance row |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/InsertCommentAiRunQuery.java` — follows the `InsertCategoryQuery` / `InsertAttachmentQuery` pattern |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/InsertCommentAiProvenanceQuery.java` |
| create | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/repo/GetCommentAiRunQuery.java` — cache lookup by `job_id` plus sibling-summary aggregate |
| create | `ApiCommonWebsite/Service/src/main/resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}` |
| create | `ApiCommonWebsite/Service/src/main/resources/ai/prompts/verifyGeneSummary/{system.txt,user.txt,schema.json}` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/post-request.json` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/status-response.json` |
| create | DB migration script under `ApiCommonModel/Model/lib/sql/` (follow the `migration_comment_b*.sql` pattern) for both new tables |
| modify | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/ApiWebServiceApplication.java` — `.add(AiGenePublicationCommentService.class)` |
| modify | `ApiCommonWebsite/Service/doc/raml/apicommonwebsite.raml` — document the three new endpoints |
| modify | `ApiCommonWebsite/Service/pom.xml` — no new PDF dependency needed (text extraction happens client-side in MuPDF.js, the server never touches a PDF). `anthropic-java` is already present in the Model pom; no SDK addition needed there. |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/CommentFactory.java` (or its EbrcWebsiteCommon parent) — extend the `createComment` transaction (currently `con.setAutoCommit(false)` → `con.commit()` at `CommentFactory.java:196,234`) to also insert `comment_ai_provenance` when an `AiProvenance` is supplied on the request. Extend `GetCommentQuery` to LEFT JOIN the provenance + run rows. **Coordination item with EbrcWebsiteCommon.** |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/CommentRequest.java` and `Comment.java` — add `aiProvenance` field (optional). |

### Implementation order

**First deliverable — scaffolding only.** All new classes (resource, registry, pipeline, sync prelude, fetchers, scanners, LLM client, POJOs, queries) created with JAX-RS annotations, method signatures, and no-op bodies that throw `UnsupportedOperationException` (or return `501 Not Implemented` for the endpoints). Service registered in `ApiWebServiceApplication`. SQL migration script committed. Module builds clean; the three endpoints return 501. **Pause here for user review of the shape before any pipeline logic lands.**

Subsequent deliverables, in order:

1. Sync prelude (gene/synonym resolution, digest, `comment_ai_run` lookup, registry attach).
2. PMC BioC article fetcher (pubmed path); upload path just reads `paper_text` from the request.
3. Gene-mention regex scanner.
4. Anthropic call + JSON retry loop for `generating-summary`.
5. Validation pass (`verifyGeneSummary`).
6. Persist stage 6a (`comment_ai_run`) + 6b (`comment` + `comment_ai_provenance`, looping over followers).
7. DELETE / cancel wiring.
8. Registry eviction scheduler.

### Reused without modification

- **Anthropic client setup pattern** — `AnthropicOkHttpClientAsync.builder().apiKey(wdkModel.getProperties().get("CLAUDE_API_KEY")).maxRetries(32)...` (lifted verbatim from `ClaudeSummarizer` at `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/report/ai/expression/ClaudeSummarizer.java`).
- **`fetchUser()` / `getRequestingUser()`** from `AbstractWdkService` / `AbstractUserCommentService` — authenticates the caller; throws 401 for guests, which is the FE-required `requiresLogin: true` behaviour.
- **`getCommentFactory().createComment(commentRequest, user)`** from the existing `UserCommentsService` flow (line 91 of that file) — does the bulk of the persistence; its transaction scope (`CommentFactory.java:196,234`) is what we extend with the provenance insert.
- **DELETE-user-comment endpoint** at `DELETE /user-comments/{id}` (`UserCommentsService.deleteComment`, line 164) is already implemented and used by the FE's "Delete draft" button.

## Prompts: porting strategy

Source: `VPDB_AI_gene_paper_summary/pipeline/prompts.py` at https://github.com/PubLLicanProject/VPDB_AI_gene_paper_summary/tree/main (`global_prompts_and_schema` dict, 1436 lines covering five stages — we port only `getGeneSummary` and `verifyGeneSummary`). 

Approach:
- One directory per stage under `src/main/resources/ai/prompts/<stage>/`.
- `system.txt` — system-prompt text with `[PLACEHOLDER]` markers.
- `user.txt` — newline-separated user-message turns (one paragraph per turn, blank line separating). The Anthropic Java SDK supports multi-turn user messages via `addUserMessage` per line.
- `schema.json` — JSON-schema dict (validation only used as prompt guidance, matching Python — not enforced server-side).
- A small `PromptLoader` utility reads the files once at startup, caches them, and exposes `render(stage, Map<String,String> replacements)` that does naive `String.replace` for each placeholder.

This keeps the diff against `prompts.py` legible — when the Python team iterates, manual sync remains feasible. Drift risk acknowledged.

## Out of scope / coordination items

These are flagged for the user to decide or coordinate, not implemented in this plan:

1. **FE contract amendments** (cross-reference): the FE plan (`CLAUDE-plan-ai-user-comments-front-end.md`) needs (a) `mentioned-in-passing` terminal status, (b) the anonymous `sibling_summary` field on terminal responses and the banner that consumes it, (c) the upload-via-TempFileService prerequisite step before submitting to our service.
2. **EbrcWebsiteCommon coordination**: extending `CommentRequest`/`Comment`/`CommentFactory` to carry `aiProvenance` and read/write the two new sidecar tables. May span repos.
3. **DB migration script placement / review**: scripts live in `ApiCommonModel/Model/lib/sql/` (corrected location) — DBA review and migration scripting follows existing project conventions.
4. **Product descriptions** (Python stages 3–5): explicitly deferred. When added, they get `options.generate_product_description=true`, a new `generating-product-description` stage, and a separate persistence target (TBD).
5. **Prompt drift**: the Python pipeline is still being iterated (test sets, model comparison runs ongoing). Ported Java prompts will drift unless re-synced. A v2 option is to read prompts from a shared file/repo, or to revisit the integration choice. Cache invalidation on prompt edits is **manual** — devs bump `promptVersion` (the per-stage constant baked into `job_id`) when they touch the prompt files. Convention enforced by code review.
6. **No daily cost cap in v1**: unlike `AiExpressionSummary`'s `DailyCostMonitor`, this service does not currently rate-limit by daily spend. If volume warrants, plug into the existing `DailyCostMonitor` pattern as a follow-up.
7. **`/user-comments/show` modernisation, gene-page provenance column, de-duplication** — all deferred per the FE plan.
8. **Optional `comment_ai_text_unavailable_log` table** (append-only, no read path) — would let us measure how often `text-unavailable` outcomes occur and decide later whether short-lived caching is worth it. Not added in this plan — decision pending.
9. **Per-follower DELETE semantics**: today a DELETE cancels the underlying job for *all* attached followers. A future addition could let one follower detach without killing the job for others; not in v1.
10. **FE coordination — client-side PDF extraction**: the FE plan needs (a) MuPDF.js (WASM, lazy-loaded only on the upload tab — see <https://mupdf.readthedocs.io/en/1.27.2/guide/using-with-javascript.html>), (b) Web Crypto SHA-256 of the PDF bytes, (c) POST body shape carrying `paper_text` + `pdf_content_sha256`, (d) a graceful client-side error UI for the case where MuPDF.js fails to parse a particular PDF (since that no longer surfaces as a server-side `text-unavailable`). The text-extraction quality matches the Python pipeline (MuPDF.js and PyMuPDF share the MuPDF engine), so prompts that have been tuned against the Python output should transfer cleanly.

## Verification

1. **Module builds clean** in order: `install/`, `WDK/`, `ApiCommonWebsite/Model`, `ApiCommonWebsite/Service` (`mvn clean install -DskipTests`).
2. **Service registers**: `GET /user-comments/ai-gene-publication/<bogus-digest>` returns 404 (not a JAX-RS 405/wrong-route error).
3. **Auth gate**: same call from a guest session returns 401.
4. **PubMed happy path**: POST with a known OA PMID + gene that's clearly discussed → POST returns `running { job_id, stage }` within ~2s, polling shows stages advance, terminal `success` returns `ai_output` + `comment_id`. `comment_ai_run` row exists keyed by the digest; `comment_ai_provenance` row exists for the submitter with `review_level='unreviewed'`; `GET /user-comments/{comment_id}` returns the full comment with `aiProvenance` populated.
5. **PDF (upload) happy path**: FE extracts text and computes the PDF SHA-256 client-side with MuPDF.js + Web Crypto, then POSTs `{ document_type: "upload", paper_text, pdf_content_sha256, gene_id, external_url?, external_title? }` to our service. `comment_ai_run.pdf_content_sha256` matches the value the FE supplied; otherwise as #4.
6. **Gene not in paper**: POST a PMID that doesn't mention the gene → terminal `gene-not-mentioned` with `synonyms_checked` populated. `comment_ai_run` row written (terminal_status='gene-not-mentioned'); `comment` + `comment_ai_provenance` row created for the submitter (per D5).
7. **Mentioned only in passing**: POST a PMID where the gene appears trivially → terminal `mentioned-in-passing`. `comment_ai_run` row written; submitter's `comment` + `comment_ai_provenance` created.
8. **Text unavailable**: POST a non-OA / restricted PMID → terminal `text-unavailable` with `reason` populated, **no rows** in `comment_ai_run`, `comments`, or `comment_ai_provenance`. Retrying re-runs the fetch.
9. **Validation off**: POST with `options.validate=false` → the `validating` stage is never emitted; success still works.
10. **Cache hit across users**: user A's job completes; user B POSTs the same `gene_id` + `pubmed_id` → POST returns within ~2s with `success { ai_output, sibling_summary: { unreviewed: 1, … }, comment_id }`; a fresh `comments` + `comment_ai_provenance` row exists for B, both rows reference the same `run_job_id` as A's.
11. **In-flight attach**: user A submits a job; before it completes, user B POSTs the same digest → both poll the same `job_id`. On terminal success, two `comments` + two `comment_ai_provenance` rows exist, both pointing to the single `comment_ai_run` row.
12. **Pool exhaustion**: saturate the 8-thread executor; ninth submit returns `503 Service Unavailable` with `Retry-After`.
13. **Resume**: submit a job, then `GET` with the same `job_id` after restart-of-the-tab simulating a refresh → returns the live state without resubmitting. (Server restart between submit and resume returns 404 — FE handles this.)
14. **Cancel**: submit a job, immediately DELETE → polling returns `type: 'cancelled'`, the executor thread terminates promptly, no `comment_ai_run` or `comment` row is created.
15. **Registry TTL eviction**: submit, let it complete, wait >10 min, GET → live registry returns 404 but a fresh POST with the same digest still cache-hits via `comment_ai_run`.
16. **Regression**: existing `/user-comments` endpoints (POST/GET/DELETE/attachments) still work unchanged.

---

# Implementation progress & session handoff

_Last updated: 2026-06-02. We are executing this plan via the `superpowers:executing-plans` workflow (one reviewable deliverable at a time, TDD for pure logic). Branch: `feature-ai-user-comments`._

## Deliverable status

| # | Deliverable | Status |
|---|-------------|--------|
| 0 | Scaffolding (all classes, 501/no-op, builds clean) | ✅ **Done & committed** |
| 1 | Sync prelude (validate, synonyms, digest, cache lookup, registry attach, spawn/503, POST+GET wiring) | ✅ **Done** — full Service build green, **21 unit tests pass** |
| 2 | PMC BioC article fetcher (`fetching-article`) | ✅ **Done** — full Service build green, **31 unit tests pass** (+10 new); live HTTP path smoke-tested then throwaway IT removed |
| 3 | Gene-mention regex scanner (`scanning-gene-mentions`) | ⬜ Next |
| 4 | Anthropic call + JSON retry (`generating-summary`) | ⬜ Pending |
| 5 | Validation pass (`verifyGeneSummary`) | ⬜ Pending |
| 6 | Persist (`comment_ai_run` + `comment` + `comment_ai_provenance`, loop followers) | ⬜ Pending — **blocked on DB tables** for live test |
| 7 | DELETE / cancel wiring | ⬜ Pending (endpoint still returns 501) |
| 8 | Registry eviction scheduler | ⬜ Pending (`_evictor` field present, unused) |

## Decisions locked (with rationale)

- **LLM model = `claude-sonnet-4-20250514`** — matches the Python pipeline the prompts were tuned against. Held in `AiSummaryConfig.MODEL_NAME`. Baked into the `jobId` digest.
- **`PROMPT_VERSION = "1"`** (`AiSummaryConfig`) — manually bumped when prompt files change; baked into digest. When the verify prompt lands (D5), keep it a single combined version covering both stages.
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

**`JobState` was refactored** from a per-follower `Submitter`-with-full-request to: one shared `JobSubmission` + `List<Long> followerUserIds` + a `userId→commentId` map (filled at persist). `JobState.getJobId()` delegates to the submission. Three state tiers: immutable submission (shared) · follower ids (per-user) · volatile stage/result (published; transient stage outputs live as pipeline instance fields).

## Implemented this far (what's real vs stub)

**Implemented + unit-tested (pure):** `SyncPrelude.validate` (7 tests), `JobDigest` (8 tests), `JobRegistry.submit/attach/get` (6 tests), `PmcBiocFetcher.parseBiocJson` (7 tests, D2), `AiGenePublicationPipeline.fetchArticle` upload/pubmed/text-unavailable (3 tests, D2).
**Implemented, compiles, live-smoke-tested:** `PmcBiocFetcher.fetch` (D2 — real BioC HTTP path verified against PMID 17299597 (27 KB) and a bogus PMID → `text-unavailable`; throwaway IT then removed).
**Implemented, compiles, NOT live-tested** (need running WdkModel / DB tables): `GeneSynonymService.resolve` (reads gene `Alias` table), `GetCommentAiRunQuery.parseResults` + `CommentFactory.findAiRun`, `SyncPrelude.handleSubmit/resolveSynonyms/computeJobId`, `AiGenePublicationCommentService` POST (auth, spawn/attach/cache, 503) + GET (registry→cache→404), `AiGenePublicationPipeline.run()` orchestration (terminal short-circuit after each stage; top-level `Throwable` → `internal-error`).
**Still stubs (throw `UnsupportedOperationException` / 501):** pipeline stages `scanGeneMentions` (D3) / `generateSummary` (D4) / `validateSummary` (D5) / `flattenToComment` (D5) / `persist` (D6), `GeneMentionScanner`, `AnthropicJsonClient.complete`, `InsertCommentAiRunQuery`/`InsertCommentAiProvenanceQuery` `getArguments`, DELETE endpoint.

## Interim caveats (by design, not bugs)

- **`run()` is now wired (D2).** It calls stages in order, returns early once a stage marks the job terminal, and catches any `Throwable` → terminal `internal-error`. A live job therefore now reaches `fetching-article` and then **terminates as `internal-error`** because the next stage (`scanGeneMentions`, D3) still throws `UnsupportedOperationException` — caught by `run()`. (A non-OA pubmed id terminates earlier as `text-unavailable`; an upload passes the FE text through and likewise lands at the D3 stub → `internal-error`.) This replaces the old "stays running forever" behaviour and is the expected interim state until D3+ land.
- **Cache-hit response is minimal** (`type`/`job_id`/`ai_output`) — submitter comment creation + `sibling_summary` aggregate are D6 (TODOs in `AiGenePublicationCommentService.cacheHitJson`).

## Build / test notes (important for next session)

- Build chain order: `install/` → `WDK/` → `ApiCommonWebsite/Model` → `ApiCommonWebsite/Service`. **`ai-wdk` IS `project_home`** (CLAUDE.md paths say `project_home/…`).
- **Service resolves Model from `.m2`**, so after changing Model run `cd ApiCommonWebsite/Model && mvn install -DskipTests` before building/testing Service, or Service won't see new Model methods (e.g. `findAiRun`).
- Run one test class: `cd ApiCommonWebsite/Service && mvn -Dtest=JobDigestTest test`. All AI tests: `-Dtest=JobDigestTest,SyncPreludeTest,JobRegistryTest`.
- Non-fatal POM warnings (`version is missing` for `javax.servlet:servlet-api`, `classloader-leak-prevention-servlet`) appear but the build SUCCEEDS — ignore.
- `mvn test`/`mvn compile` from `ApiCommonWebsite/Service` is the quick loop.

## Reference facts already gathered for upcoming deliverables

Source of truth = Python `VPDB_AI_gene_paper_summary/` (at `../VPDB_AI_gene_paper_summary`), esp. `PubGene_back_end/helpers.py` and `pipeline/prompts.py`.

- **D2 (BioC fetch): ✅ DONE.** `GET https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/{pmid}` via Java `HttpClient` (180 s read timeout, `Accept: application/json`); keep passages where `infons.section_type ∈ {FIG, TABLE, RESULTS, CONCL, DISCUSS, SUPPL}`; concat non-empty `text` with `\n` (`PmcBiocFetcher.parseBiocJson`, pure + unit-tested). Non-2xx / non-`application/json` content-type / malformed JSON / **empty parse result** → `TextUnavailableException` → terminal `text-unavailable` (not cached). Upload path: `fetchArticle()` uses `submission.getUploadedPaperText()` directly. **⚠ Deviation from Python (user-approved):** Python `PUBMED_SECTIONS` uses `DISCUSSION`, but the live PMC BioC vocabulary emits `DISCUSS` — so Python silently dropped *all* discussion sections. The Java port uses `DISCUSS` (verified empirically against PMID 17299597). **Report this bug to the Python team** (prompts were tuned on output that excluded discussion text, so re-tuning may be warranted). Also: treating an empty parse as `text-unavailable` (rather than Python's empty-string pass-through) avoids caching a false `gene-not-mentioned` for a paper we couldn't extract.
- **D3 (`_count_substrings`, helpers.py:264):** if alias matches `^[A-Za-z]+[0-9]+$` → inner regex `letters + "-?" + digits` (so `Nd6`↔`Nd-6`); else replace each run of `_`/`-`/space with `[-_\s]+` and escape the rest; wrap as `(?<![A-Za-z0-9]) inner (?![A-Za-z0-9])`, `CASE_INSENSITIVE`; count matches. `get_gene_synonyms_in_paper`: count each alias, keep `>0`, return **top-3 by count desc**. `gene-not-mentioned` iff gene_id AND all aliases score 0 (gene_id also tries `_`→`-` variant). This outcome **is** persisted.
- **D4 (LLM):** prompts in `resources/ai/prompts/getGeneSummary/{system,user,schema}` (currently placeholders — port real text from `pipeline/prompts.py` `global_prompts_and_schema['getGeneSummary']`). Placeholders: `[PAPER_TEXT] [GENE] [N_QUOTES] [JSON_SCHEMA]`. Prefill assistant turn with `{`. Response field `only_in_passing=true` → terminal `mentioned-in-passing` (persisted). `N_QUOTES=2`, temperature `0`, `max_tokens=20000`, `extract_json` retry max 3 via a formatter LLM call. Anthropic client (mirror `ClaudeSummarizer`): `AnthropicOkHttpClientAsync.builder().apiKey(wdkModel.getProperties().get("CLAUDE_API_KEY")).maxRetries(32).checkJacksonVersionCompatibility(false).build()`. `gene_for_prompt(geneId, names)` → `"<geneId> (also known as X or Y)"`.
- **Gene aliases (D1, done):** gene record `Alias` table — `queryRef="GeneTables.Alias"`, column `alias` (also `id_type`, `db_name`), `displayName="Names, Previous Identifiers, and Aliases"`. Read via `RecordInstance.getTableValue("Alias")`, exclude the queried id, sort/dedupe. Parity with Python `get_vpdb_alias`.

## Open coordination items (unchanged from "Out of scope")

- DB tables in `userdb_devn` (Steve) — blocks D6 live test + all cache-hit/persist live verification.
- EbrcWebsiteCommon: `CommentFactory.createComment` transaction extension for `comment_ai_provenance` + `CommentRequest`/`Comment` `aiProvenance` field (D6). Note: `CommentFactory` is local to `ApiCommonWebsite/Model` (not Ebrc) — confirmed.
- RAML doc (`Service/doc/raml/apicommonwebsite.raml`) for the 3 endpoints — not yet written.
