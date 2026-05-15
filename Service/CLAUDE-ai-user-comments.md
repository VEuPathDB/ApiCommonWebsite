# Plan: AI-Assisted Gene-Publication Summary — Back-End Service (Java)

## Context

VEuPathDB is adding a new kind of user comment: an **AI-assisted gene-publication summary**. A user supplies a gene (via URL `stableId`) and a publication (a PubMed ID or an uploaded PDF). A new back-end service:

1. Resolves the publication text (PMC BioC API for PubMed, local PDF extraction for uploads).
2. Looks up gene synonyms from VEuPathDB and verifies the gene is mentioned in the text.
3. Runs an LLM to generate a gene-function summary, with an optional validation pass.
4. Persists a `user_comment` row carrying an `aiProvenance` record so the FE can show a review-and-publish form.

The pipeline already exists in Python (`VPDB_AI_gene_paper_summary/`). For this v1 we are **porting it to Java** rather than wrapping it — the user has chosen this trade-off explicitly. The companion front-end plan (`CLAUDE-plan-ai-user-comments-front-end.md`) defines the REST contract and is the source of truth for the wire shape; this plan implements the back-end side of that contract.

## Architecture overview

A new JAX-RS resource `AiGenePublicationCommentService` lives in `ApiCommonWebsite/Service`, registered in `ApiWebServiceApplication.getClasses()` alongside `UserCommentsService`. It exposes three endpoints (matching the FE contract), runs jobs asynchronously on a bounded thread pool, tracks job state in an in-memory map with TTL eviction, and on success calls the existing `getCommentFactory().createComment(...)` to persist the result.

The Anthropic client setup mirrors `ClaudeSummarizer` (`AnthropicOkHttpClientAsync.builder().apiKey(...)`), but we do not reuse `Summarizer` itself or its disk cache. Prompts and JSON schemas are ported from Python `pipeline/prompts.py` into `.txt` and `.json` resource files; placeholder substitution uses simple `String.replace("[GENE]", ...)` matching the Python convention.

```
┌─────────────────────────────────────────────────────────────────┐
│ AiGenePublicationCommentService (JAX-RS)                        │
│  POST  /user-comments/ai-gene-publication       → 202 {jobId}    │
│  GET   /user-comments/ai-gene-publication/{id}  → status         │
│  DELETE /user-comments/ai-gene-publication/{id} → cancel         │
└─────────┬───────────────────────────────────────────────────────┘
          │ submit                                  
          ▼                                         
┌─────────────────────────────────────────────────┐
│ JobRegistry  (ConcurrentHashMap<UUID, JobState>)│
│  - submit()  → spawn pipeline on ExecutorService│
│  - get()                                        │
│  - cancel()                                     │
│  - scheduled eviction (ScheduledExecutorService)│
└─────────┬───────────────────────────────────────┘
          │ stage callbacks update JobState
          ▼
┌─────────────────────────────────────────────────────────────────┐
│ AiGenePublicationPipeline                                       │
│  ① fetchArticleText  (PMC BioC fetcher  | PDFBox extractor)      │
│  ② fetchGeneSynonyms (WDK RecordClass — gene + Alias table)      │
│  ③ scanGeneMentions  (regex matcher ported from helpers.py)      │
│  ④ generateSummary   (Anthropic call — getGeneSummary prompt)    │
│  ⑤ validateSummary   (Anthropic call — verifyGeneSummary prompt) │
│  ⑥ flattenToComment  (structured JSON → headline + content)     │
│  ⑦ persistComment    (CommentFactory.createComment + sidecar)   │
└─────────────────────────────────────────────────────────────────┘
```

## REST contract — Java implementation of the FE contract

Matches `CLAUDE-plan-ai-user-comments-front-end.md` §"Backend contract" exactly, with one addition:

**Terminal status union (FE-contract additions in bold):**
- `success { aiOutput, commentId? }`
- `gene-not-mentioned { synonymsChecked }`
- **`mentioned-in-passing { synonymsChecked }`** — new for v1
- `text-unavailable { reason }`
- `validation-error { errors }`
- `internal-error { error }`
- `cancelled`

The FE plan must be updated to handle `mentioned-in-passing` (a coordination item — flagged below).

**Stage names emitted in `running` state** (subset of FE-defined stages, only those v1 ships):
- `queued` → `fetching-article` → `fetching-gene-synonyms` → `scanning-gene-mentions` → `generating-summary` → `validating` (iff `options.validate`) → `persisting` (iff `options.createUserComment`)

No `generating-product-description` in v1 (product descriptions deferred — see "Out of scope" below).

**Polling cadence**: server tolerates ~1/s polls; no rate limiting beyond standard `AbstractWdkService` behaviour. Job TTL: **10 min** after terminal state (≥5 min recommended by FE; we round up).

## Pipeline stage details

| # | Stage | Implementation |
|---|-------|---------------|
| ① | `fetching-article` | If `source=pubmed`: GET `https://www.ncbi.nlm.nih.gov/research/bionlp/RESTful/pmcoa.cgi/BioC_json/{pmid}` via Java `HttpClient`, parse with Jackson, keep passages where `infons.section_type ∈ {FIG, TABLE, RESULTS, CONCL, DISCUSSION, SUPPL}`, concatenate `text` fields. Non-JSON / 404 / non-OA paper → `text-unavailable`. If `source=upload`: stream the multipart `pdf` part into Apache PDFBox (`PDDocument.load` + `PDFTextStripper`) for text extraction. **PDF is held in memory/tempdir only — never persisted.** |
| ② | `fetching-gene-synonyms` | Use `wdkModel.getRecordClass("GeneRecordClasses.GeneRecordClass")`, fetch `RecordInstance` for the stableId, read the `Alias` table. Drop aliases equal to the gene id. (Python calls the same VPDB HTTP endpoint we *are*; we short-circuit by querying WDK in-process.) `host_db` derivation: not needed — we use `wdkModel.getProjectId()` if the prompt template needs it for display, but the WDK query is project-scoped automatically. |
| ③ | `scanning-gene-mentions` | Port `_count_substrings` from `PubGene_back_end/helpers.py` (regex matcher: handles `Nd6` ↔ `Nd-6`, `PF3D7_1133400` ↔ `PF3D7-1133400`, case-insensitive, non-alphanumeric boundaries). If the gene id and *all* aliases score 0 hits → terminal `gene-not-mentioned` with `synonymsChecked` populated. Otherwise pass the top-3-by-frequency aliases into the prompt. |
| ④ | `generating-summary` | Anthropic API call. System prompt + user prompts loaded from `resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}`. Placeholder substitution: `[PAPER_TEXT]`, `[GENE]`, `[N_QUOTES]`, `[JSON_SCHEMA]`. Prefill assistant turn with `{` (matches Python). Strip markdown fences from response, parse JSON. On the response's `only_in_passing=true` → short-circuit to terminal `mentioned-in-passing`. Up to `max_retry=3` retries via formatter LLM on malformed JSON (port `extract_json` retry loop). |
| ⑤ | `validating` (iff `options.validate`) | Anthropic call using `verifyGeneSummary` prompt. Input: original summary JSON + paper text. Output: verified/corrected summary in the same shape. If validation finds blocking issues we can't recover from, terminal `validation-error` with the validator's notes. |
| ⑥ | flatten | Compute `aiOutput.headline = ShortSummary` (plain text). `aiOutput.content` = bullet-flattened plain text that is also valid markdown — `- ` bullets with indented `Evidence:` / `>` quote lines and an "Aliases mentioned in paper:" header. No HTML; no markdown editor library is in the FE monorepo, so we stay plain-text-compatible while future-proofing for a markdown renderer on the show page (deferred). |
| ⑦ | `persisting` (iff `options.createUserComment`) | Call `getCommentFactory().createComment(commentRequest, user)` where `commentRequest` has `headline`, `content`, and the gene's stableId set as the `target`. Then write the sidecar `user_comment_ai_provenance` row with `review_level='unreviewed'`, `source_kind`, `pubmed_id` or `external_url`/`external_title`, `original_headline`, `original_content`. Return `commentId` in the terminal `success`. |

## Job state mechanism

In-memory `JobRegistry`:
- `ConcurrentHashMap<UUID, JobState>` where `JobState` carries the current stage/message, the immutable submission, the running `Future`, and (when terminal) the result.
- Submit handler returns `{ jobId }` synchronously after spawning the pipeline on a fixed-size `ExecutorService` (cap: **8 threads**, sized for slow LLM calls).
- Each pipeline stage calls back into the registry to update `JobState.progress` with `{ stage, message, updatedAt }`.
- `ScheduledExecutorService` runs every 60 s and evicts entries whose terminal-state age exceeds 10 min.
- DELETE: marks the job cancelled, calls `.cancel(true)` on the `Future`, and cancels any in-flight Anthropic HTTP call via the OkHttp client. Next poll returns `type: 'cancelled'`.
- Server restart = all in-flight jobs lost. Acceptable for v1: jobs are seconds-to-minutes, and the `user_comment` row is the durable artefact once `persisting` has run. Lost mid-flight jobs surface to the FE as `not-found` (404) on the next poll, which already triggers a friendly "job expired, please submit again" UX.

## Database schema — sidecar table

Match the existing convention (Categories, References, Attachments all use sidecars off `comments`).

**Dev database**: PostgreSQL — `userdb_devn` on `ares13.penn.apidb.org:5432` (confirmed via LDAP lookup of `userDb_ldapCommonName: userdb_devn` on `ds.apidb.org`).

**Action**: Ask Mustafa to create the table in `userdb_devn`.

**Schema persistence**: `VEuPathDB/ApiCommonData` -> `Load/lib/sql/comments/psql/createCommentTables.sql`

**Production roll-out**: Bob needs to understand how this works.

**Oracle back-port**: probably not needed (all active sites appear to be on PostgreSQL) — confirm before closing.

```sql
CREATE TABLE usercomments.comment_ai_provenance
(
  comment_id        BIGINT        NOT NULL,
  review_level      VARCHAR(16)   NOT NULL,   -- 'unreviewed' | 'reviewed' | 'edited'
  source_kind       VARCHAR(16)   NOT NULL,   -- 'pubmed' | 'upload'
  pubmed_id         VARCHAR(32),              -- iff source_kind='pubmed'
  external_url      TEXT,                     -- iff source_kind='upload', optional
  external_title    VARCHAR(4000),            -- iff source_kind='upload', optional
  original_headline VARCHAR(2000) NOT NULL,
  original_content  TEXT          NOT NULL,
  CONSTRAINT comment_ai_provenance_pkey PRIMARY KEY (comment_id),
  CONSTRAINT comment_ai_prov_comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES usercomments.comments (comment_id)
);

GRANT insert, update, delete on usercomments.comment_ai_provenance to COMM_WDK_W;
GRANT select on usercomments.comment_ai_provenance to GUS_R;
```

Style follows `createCommentTables.sql`: `BIGINT` for IDs, `VARCHAR`, `TEXT` for unbounded strings, grants to `COMM_WDK_W`/`GUS_R`. Table name follows the `comment_*` convention. `original_headline` sized to match `comments.headline VARCHAR(2000)`. FK references `usercomments.comments (comment_id)`.

## Java implementation: file layout

### New code (this plan)

| Action | Path |
| ------ | ---- |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationCommentService.java` — JAX-RS resource (POST/GET/DELETE) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobRegistry.java` — in-memory job store + executors |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/JobState.java` + `JobStatus.java` — value types matching FE contract |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/AiGenePublicationPipeline.java` — orchestrator: walks the seven stages, emits progress callbacks |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/article/PmcBiocFetcher.java` |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/article/PdfTextExtractor.java` (PDFBox wrapper) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneSynonymService.java` (WDK `RecordClass` lookup) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/gene/GeneMentionScanner.java` (regex matcher port) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/services/ai/llm/AnthropicJsonClient.java` (thin wrapper: load prompt files, substitute placeholders, parse JSON with retry) |
| create | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/model/comment/pojo/AiProvenance.java` (POJO mirroring FE type) |
| create | `ApiCommonWebsite/Service/src/main/resources/ai/prompts/getGeneSummary/{system.txt,user.txt,schema.json}` |
| create | `ApiCommonWebsite/Service/src/main/resources/ai/prompts/verifyGeneSummary/{system.txt,user.txt,schema.json}` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/post-request.json` |
| create | `ApiCommonWebsite/Service/src/main/resources/schema/apicomm/ai-gene-publication/status-response.json` |
| create | DB migration script (placement TBC alongside existing schema) for `user_comment_ai_provenance` |
| modify | `ApiCommonWebsite/Service/src/main/java/org/apidb/apicommon/service/ApiWebServiceApplication.java` — `.add(AiGenePublicationCommentService.class)` |
| modify | `ApiCommonWebsite/Service/doc/raml/apicommonwebsite.raml` — document the three new endpoints |
| modify | `ApiCommonWebsite/Service/pom.xml` — add Apache PDFBox dependency (e.g. `org.apache.pdfbox:pdfbox:3.0.x`) |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/CommentFactory.java` (or its EbrcWebsiteCommon parent) — extend `createComment` path to persist the sidecar row when an `AiProvenance` is present on the request; extend the `GetComment` query path to load it back. **Coordination item with EbrcWebsiteCommon.** |
| modify | `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/comment/pojo/CommentRequest.java` and `Comment.java` — add `aiProvenance` field (optional). |

### Reused without modification

- **Anthropic client setup pattern** — `AnthropicOkHttpClientAsync.builder().apiKey(wdkModel.getProperties().get("CLAUDE_API_KEY")).maxRetries(32)...` (lifted verbatim from `ClaudeSummarizer` at `ApiCommonWebsite/Model/src/main/java/org/apidb/apicommon/model/report/ai/expression/ClaudeSummarizer.java`).
- **`fetchUser()` / `getRequestingUser()`** from `AbstractWdkService` / `AbstractUserCommentService` — authenticates the caller; throws 401 for guests, which is the FE-required `requiresLogin: true` behaviour.
- **`getCommentFactory().createComment(commentRequest, user)`** from the existing `UserCommentsService` flow (line 91 of that file) — does the bulk of the persistence.
- **Multipart parsing pattern** — `@Consumes(MediaType.MULTIPART_FORM_DATA)` + `@FormDataParam` (mirror `AttachmentsService.newAttachment`).
- **DELETE-user-comment endpoint** at `DELETE /user-comments/{id}` (`UserCommentsService.deleteComment`, line 164) is already implemented and used by the FE's "Delete draft" button.

## Prompts: porting strategy

Source: `VPDB_AI_gene_paper_summary/pipeline/prompts.py` (`global_prompts_and_schema` dict, 1436 lines covering five stages — we port only `getGeneSummary` and `verifyGeneSummary`).

Approach:
- One directory per stage under `src/main/resources/ai/prompts/<stage>/`.
- `system.txt` — system-prompt text with `[PLACEHOLDER]` markers.
- `user.txt` — newline-separated user-message turns (one paragraph per turn, blank line separating). The Anthropic Java SDK supports multi-turn user messages via `addUserMessage` per line.
- `schema.json` — JSON-schema dict (validation only used as prompt guidance, matching Python — not enforced server-side).
- A small `PromptLoader` utility reads the files once at startup, caches them, and exposes `render(stage, Map<String,String> replacements)` that does naive `String.replace` for each placeholder.

This keeps the diff against `prompts.py` legible — when the Python team iterates, manual sync remains feasible. Drift risk acknowledged.

## Out of scope / coordination items

These are flagged for the user to decide or coordinate, not implemented in this plan:

1. **FE contract extension**: add `mentioned-in-passing` terminal status. The FE plan (`CLAUDE-plan-ai-user-comments-front-end.md`) must be amended to handle this new variant — UX TBD (probably similar to `gene-not-mentioned` but with "the gene is only mentioned in passing in this paper" copy).
2. **EbrcWebsiteCommon coordination**: extending `CommentRequest`/`Comment`/`CommentFactory` to carry `aiProvenance` and read/write the sidecar table. May span repos.
3. **DB migration script placement / review**: the user-comment schema lives outside `ApiCommonWebsite/Service` proper — DBA review and migration scripting follows existing project conventions.
4. **Product descriptions** (Python stages 3–5): explicitly deferred. When added, they get `options.generateProductDescription=true`, a new `generating-product-description` stage, and a separate persistence target (TBD).
5. **Prompt drift**: the Python pipeline is still being iterated (test sets, model comparison runs ongoing). Ported Java prompts will drift unless re-synced. A v2 option is to read prompts from a shared file/repo, or to revisit the integration choice.
6. **No daily cost cap in v1**: unlike `AiExpressionSummary`'s `DailyCostMonitor`, this service does not currently rate-limit by daily spend. If volume warrants, plug into the existing `DailyCostMonitor` pattern as a follow-up.
7. **`/user-comments/show` modernisation, gene-page provenance column, de-duplication** — all deferred per the FE plan.

## Verification

1. **Module builds clean** in order: `install/`, `WDK/`, `ApiCommonWebsite/Model`, `ApiCommonWebsite/Service` (`mvn clean install -DskipTests`).
2. **Service registers**: hitting `GET /user-comments/ai-gene-publication/nonexistent-id` returns 404 (not a JAX-RS 405/wrong-route error).
3. **Auth gate**: same call from a guest session returns 401.
4. **PubMed happy path**: POST with a known OA PMID + gene that's clearly discussed → URL gains `jobId`, polling shows stages advance, terminal `success` returns a `commentId`, and `GET /user-comments/{commentId}` returns the new row with `aiProvenance` populated (`reviewLevel='unreviewed'`, source.kind='pubmed', originals set).
5. **PDF happy path**: POST multipart with a PDF + provenance URL/title → text extraction succeeds, same downstream stages run, terminal `success` includes `aiProvenance.source = { kind:'upload', externalUrl, externalTitle }`.
6. **Gene not in paper**: POST a PMID that doesn't mention the gene → terminal `gene-not-mentioned` with `synonymsChecked` populated and **no `user_comment` row created**.
7. **Mentioned only in passing**: POST a PMID where the gene appears trivially → terminal `mentioned-in-passing` with `synonymsChecked` and **no `user_comment` row created** (the cleanest semantics; if the user wants the comment regardless, they can opt to proceed via a future FE control).
8. **Text unavailable**: POST a non-OA / restricted PMID → terminal `text-unavailable` with `reason` populated, no DB writes.
9. **Validation off**: POST with `options.validate=false` → the `validating` stage is never emitted; success still works.
10. **Resume**: submit a job, then `GET` with the same `jobId` after restart-of-the-tab simulating a refresh → returns the live state without resubmitting. (Server restart between submit and resume returns 404 — FE handles this.)
11. **Cancel**: submit a job, immediately DELETE → polling returns `type: 'cancelled'`, the executor thread terminates promptly, no `user_comment` row is created.
12. **Job TTL eviction**: submit, let it complete, wait >10 min, GET → 404.
13. **Regression**: existing `/user-comments` endpoints (POST/GET/DELETE/attachments) still work unchanged.
