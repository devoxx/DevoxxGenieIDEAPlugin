---
id: TASK-231
title: Fix ChromaDB v2 API migration and add e2e tests for ChromaDB interactions
status: In Progress
assignee: []
created_date: '2026-06-09 11:27'
labels:
  - bug
  - rag
  - chromadb
  - testing
dependencies: []
references:
  - 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1085'
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDBService.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaEmbeddingService.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDBManager.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/validator/ChromeDBValidatorTest.java
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Context

GitHub issue #1085 reported that the RAG feature fails with ChromaDB 0.6+ because the plugin was using the deprecated v1 REST API (`/api/v1/`). ChromaDB 0.6.x deprecated v1 in favour of v2; ChromaDB 1.x removes it entirely.

## Root Cause

Two components used the v1 API:

1. **`ChromaDBService.java`** — Retrofit interface with hardcoded `/api/v1/collections` paths used by `ChromaDBManager` for list, count, and delete operations shown in the RAG settings UI.
2. **`ChromaEmbeddingService.java`** — `ChromaEmbeddingStore.builder()` defaulted to `ChromaApiVersion.V1` (langchain4j default). Langchain4J 1.7.0-beta13+ added `ChromaApiVersion.V2`.

## Fix Applied (branch: fix/issue-1085-chromadb-v2-api)

- `ChromaDBService.java`: Updated all three endpoints to `/api/v2/tenants/default_tenant/databases/default_database/collections/...`
- `ChromaEmbeddingService.java`: Added `.apiVersion(ChromaApiVersion.V2).tenantName("default_tenant").databaseName("default_database")` to the store builder

`default_tenant` and `default_database` are ChromaDB's server-side defaults, ensuring existing indexed data is preserved.

## Remaining Work

Write e2e / integration tests covering the ChromaDB REST interactions to prevent regressions when ChromaDB or langchain4j are upgraded again.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 ChromaDBService Retrofit interface uses /api/v2/ paths for all three operations (list, count, delete)
- [ ] #2 ChromaEmbeddingService uses ChromaApiVersion.V2 with default_tenant and default_database
- [ ] #3 No v1 API calls remain in the codebase (grep check)
- [ ] #4 Integration/e2e tests cover ChromaDBManager.listCollections() against a real or WireMock ChromaDB v2 endpoint
- [ ] #5 Integration/e2e tests cover ChromaDBManager.countDocuments() against the v2 count endpoint
- [ ] #6 Integration/e2e tests cover ChromaDBManager.deleteCollection() against the v2 delete endpoint
- [ ] #7 Integration/e2e tests cover ChromaEmbeddingService.init() and a round-trip embed+search using the v2 store
- [ ] #8 All existing ChromaDB-related tests continue to pass
- [ ] #9 Build compiles cleanly with no new errors
<!-- AC:END -->
