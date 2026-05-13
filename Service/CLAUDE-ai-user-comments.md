# AI Gene Summary Service — Planning Context

## Purpose
Implement a new AI-powered WDK service: given a gene ID and a PubMed ID, generate a headline and gene function summary, then write the result as a WDK user comment record in the database.

## Why this module
`ApiCommonWebsite/Service` is the correct home because:
- It already owns the user comment infrastructure (`UserCommentsService`, `AttachmentsService`, `AbstractUserCommentService`, `getCommentFactory().createComment(...)`).
- It has its own RAML (`doc/raml/apicommonwebsite.raml`) where the new endpoint should be documented.
- New services are registered by `.add()`-ing them to `ApiWebServiceApplication.getClasses()` — the established pattern here.
- It is the genomics-site-specific layer (above `EbrcWebsiteCommon` and `WDK`), appropriate for `Api*`-only features.
- The existing AI pipeline (`ClaudeSummarizer` etc.) lives next door in `ApiCommonWebsite/Model`, accessible as a dependency.

## Existing patterns to follow

### Service registration
`src/main/java/org/apidb/apicommon/service/ApiWebServiceApplication.java`
- Add `.add(AiUserCommentService.class)` (or similar name) alongside the other ApiCommon-specific services.

### User comment creation
`src/main/java/org/apidb/apicommon/service/services/comments/UserCommentsService.java`
- `getCommentFactory().createComment(CommentRequest body, User user)` — use this to write the generated comment.
- `CommentRequest` includes `target` (type + stable ID), `pubMedIds`, comment content, etc.

### Existing AI code (in ApiCommonWebsite/Model)
`src/main/java/org/apidb/apicommon/model/report/ai/expression/ClaudeSummarizer.java`
- Uses `AnthropicOkHttpClientAsync` with `CLAUDE_API_KEY` WDK property.
- The new service should NOT reuse the custom expression-summarisation cache.
- It should reuse the Anthropic client setup pattern (API key from `wdkModel.getProperties()`).

### WDK genomics queries
- `getWdkModel()` is available in all WDK services — use it to query gene names and synonyms via the standard WDK record/question machinery.

## Suggested implementation location
```
src/main/java/org/apidb/apicommon/service/services/ai/
    AiUserCommentService.java   ← new JAX-RS @Path resource
```

## RAML
Add a new top-level resource to `doc/raml/apicommonwebsite.raml`, e.g.:
```raml
/ai/gene-summary:
  post:
    description: Generate an AI gene function summary and post it as a user comment.
    body:
      application/json:
        ...
    responses:
      200: ...
```

## What is NOT yet decided
- Exact request/response schema (gene ID + PubMed ID in; headline + summary out, plus created comment ID?)
- Authentication / authorisation requirements (run as a system user? logged-in user?)
- Whether the service is synchronous or async (Claude API calls can be slow)
- Which Claude model and prompt strategy to use
- Error handling for invalid gene IDs or PubMed IDs
