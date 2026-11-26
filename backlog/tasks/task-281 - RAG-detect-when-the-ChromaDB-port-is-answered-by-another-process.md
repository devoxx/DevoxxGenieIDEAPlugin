---
id: TASK-281
title: 'RAG: detect when the ChromaDB port is answered by another process'
status: To Do
assignee: []
created_date: '2026-11-26T13:37:00Z'
labels:
  - rag
  - chromadb
  - validation
dependencies: []
references:
  - src/main/java/com/devoxx/genie/service/rag/validator/ChromeDBValidator.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDBManager.java
  - src/main/java/com/devoxx/genie/service/chromadb/ChromaDockerService.java
priority: medium
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On 2026-11-26, RAG indexing failed with a raw HTML error from another server: "Error indexing project: java.lang.RuntimeException: HTTP error: … Error code: 501 Message: Unsupported method ('POST')". The same 501 page made `semantic_search` and the RAG file watcher fail.

Cause: a Python dev server (`python serve.py`) from another project was listening on IPv4 *:8000. ChromaDB's Docker container started after it, so it only got IPv6 *:8000. Java resolves `localhost` to 127.0.0.1, so the plugin talked to the Python server, while `curl localhost` (IPv6) reached ChromaDB, which made the problem confusing to diagnose.

`ChromeDBValidator` only checks Docker metadata (Docker running, image present, container running, port published). It never contacts the port, so RAG settings showed "ChromaDB v0.6.2 is running" the whole time.

Goal: before indexing and in RAG validation, check that the server on the configured port really is ChromaDB (e.g. `GET /api/v2/heartbeat` returns ChromaDB's JSON) using the same host resolution the plugin uses. If it isn't, show a clear, actionable message instead of the other server's raw HTML.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 RAG validation contacts the configured port and fails with a clear message (e.g. "Port 8000 is answered by another process, not ChromaDB. Stop that process or change the ChromaDB port in Settings → RAG.") when the server there is not ChromaDB
- [ ] #2 The check uses the same address resolution as the plugin's ChromaDB client (localhost → 127.0.0.1 on IPv4), so an IPv4/IPv6 split like the one above is detected
- [ ] #3 Start indexing, the RAG file watcher and semantic_search show that same message instead of a raw HTML error body
- [ ] #4 The check runs off the EDT and times out quickly (a few seconds) when nothing answers
- [ ] #5 Unit tests with a MockWebServer cover: a real ChromaDB heartbeat (valid), a non-Chroma server returning HTML 404/501 (invalid with the port message), and no listener (unreachable)
<!-- AC:END -->
