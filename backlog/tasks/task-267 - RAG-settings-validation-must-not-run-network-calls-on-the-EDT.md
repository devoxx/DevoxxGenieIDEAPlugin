---
id: TASK-267
title: RAG settings validation must not run network calls on the EDT
status: Done
assignee:
  - '@claude'
created_date: '2026-11-03T13:25:00Z'
updated_date: '2026-11-04T18:38:00Z'
labels:
  - bug
  - RAG
  - threading
milestone: m-0
dependencies: []
references:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
modified_files:
  - src/main/java/com/devoxx/genie/ui/settings/rag/RagValidationRunner.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsHandler.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/RAGSettingsComponent.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/table/ButtonEditor.java
  - src/main/java/com/devoxx/genie/service/rag/RagValidatorService.java
  - src/main/java/com/devoxx/genie/service/rag/validator/OllamaValidator.java
  - src/main/java/com/devoxx/genie/ui/settings/rag/ActionButtonState.java
  - src/test/java/com/devoxx/genie/ui/settings/rag/RagValidationRunnerTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/RagValidationThreadingTest.java
  - >-
    src/test/java/com/devoxx/genie/ui/settings/rag/table/CollectionsLoadThreadingTest.java
  - >-
    src/test/java/com/devoxx/genie/service/rag/validator/OllamaValidatorTest.java
priority: medium
type: bug
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Opening or interacting with the RAG settings panel runs `RagValidatorService.validate()` inside `invokeLater` (`RAGSettingsHandler.performValidation()` and `RAGSettingsComponent.updateActionButtonState()`). The validators make network and Docker calls (Docker daemon, ChromaDB, Ollama `/api/tags`), so they run on the EDT. If one of those services is slow or unreachable, the Settings dialog and the whole IDE freeze until the call times out. This will get worse once embedding endpoints can be remote (TASK-265).

Constraint: the Settings dialog is modal, so results posted back to the UI must use `ModalityState.any()` or they won't appear until the dialog closes.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Opening the RAG settings panel does not run any validator on the EDT
- [x] #2 With Ollama or ChromaDB unreachable (e.g. connection hanging until timeout), the Settings dialog stays responsive while validation runs
- [x] #3 Validation results still render while the modal Settings dialog is open
- [x] #4 A test (or reproducible manual check documented in the task notes) demonstrates validation no longer executes on the EDT
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-04: also covers the new EmbeddingProviderValidator (TASK-265), which makes a blocking HTTP call with a 2 second timeout. Validation runs up to three times per refresh on the EDT, so a dead remote URL can freeze the RAG settings panel for several seconds. This is the remaining likely cause of the panel feeling unresponsive after opening or Apply.

2026-11-04 implementation. Each refresh ran validation three times on the EDT, and opening the panel started two refreshes. RagValidationRunner now runs one validation on a pooled thread, with at most one run at a time and one follow-up if requests arrive during a run. The single result updates the status rows and Start Indexing through invokeLater with ModalityState.any(). Also fixed, beyond the original description: ButtonEditor.safeLoadCollections called ChromaDB on the EDT (list plus a count per collection), and OllamaValidator had no read timeout, so a silent server would hang forever. RagValidatorService.validate() now calls assertIsNonDispatchThread(). The action button was never added to any panel, so it was removed with the maintainer's approval, along with handleValidationAction and ActionButtonState.

Verified: 4002 tests, 0 failures, 22 skipped; verifyPlugin Compatible with IU-253 and IU-261. AC #2 and #3 still need the manual check below.

Manual check: run `python3 -c "import socket,time; s=socket.socket(); s.bind(('127.0.0.1',11999)); s.listen(); time.sleep(600)"`, set the Ollama URL in the LLM settings to http://127.0.0.1:11999, and open Tools > DevoxxGenie > RAG. Expected: the dialog opens at once and stays responsive (scroll, type in the Test query field), the section shows 'Checking required RAG services…', and rows appear while the dialog is open, once the checks time out. That can take around 35 seconds: 5 for OllamaValidator, then up to 30 for EmbeddingModelValidator's HTTP read timeout. Restore the Ollama URL afterwards.

Follow-up, not fixed here: deleting a collection in ButtonEditor.getCellEditorValue still calls ChromaDB and Docker on the EDT. Docker can take 30-45 seconds to time out; the dialog stays responsive, but the section shows no result until then.

Correction to the note above: validate() guards with SwingUtilities.isEventDispatchThread() and throws IllegalStateException, not assertIsNonDispatchThread(), which the plugin verifier reports as experimental API. Re-verified: 4002 tests, 0 failures, 22 skipped; verifyPlugin Compatible with IU-253 and IU-261, with no experimental API usages.

2026-11-04: the maintainer checked the fix manually in a running IDE and reported that it looked good, which covers AC #2 and #3.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
RAG settings validation no longer runs on the EDT. `RagValidationRunner` runs one validation at a time on a pooled thread, with one follow-up run if requests arrive during a run, and the single result updates the status rows and Start Indexing through `ModalityState.any()`. Each refresh now validates once instead of three times.

Also fixed: `ButtonEditor.safeLoadCollections` no longer calls ChromaDB on the EDT, and `OllamaValidator` has a read timeout so a silent server can't hang it. `RagValidatorService.validate()` throws if called on the EDT. The unreachable action button was removed, which also settles TASK-268.

Verified: 4002 tests, 0 failures, 22 skipped; verifyPlugin Compatible with IU-253 and IU-261; checked manually in a running IDE by the maintainer.

Known gaps: status rows appear only once every check has finished, a refresh in progress can rebuild the rows and remove a ChromaDB pull's inline progress, and deleting a collection still calls ChromaDB and Docker on the EDT.

PR: https://github.com/devoxx/DevoxxGenieIDEAPlugin/pull/1382
<!-- SECTION:FINAL_SUMMARY:END -->
