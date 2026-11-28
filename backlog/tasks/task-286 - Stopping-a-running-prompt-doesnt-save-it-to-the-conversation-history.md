---
id: TASK-286
title: Stopping a running prompt doesn't save it to the conversation history
status: In Progress
assignee: []
created_date: '2026-11-28T08:49:00Z'
updated_date: '2026-11-28T08:55:00Z'
labels:
  - chat
  - bug
  - history
  - traces
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/StreamingPromptStrategy.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
modified_files:
  - >-
    src/main/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandler.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/response/streaming/StreamingResponseHandlerTest.java
  - >-
    src/main/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategy.java
  - >-
    src/test/java/com/devoxx/genie/service/prompt/strategy/NonStreamingPromptStrategyTest.java
priority: high
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When the user stops a running prompt, the conversation history is not updated: the prompt and the partial answer the user saw are never saved. The agent trace buffered for that turn (TASK-279) is never written either, so View Agent Run and the Hugging Face upload can't show it.

Cause: saving is triggered only by the CONVERSATION_TOPIC event (ChatService.saveConversation, which also persists the buffered trace turn). StreamingResponseHandler publishes it from onCompleteResponse, and from persistPartialResponseOnError after an error, but stop() doesn't publish it. Both other paths skip a stopped run. NonStreamingPromptStrategy.cancel() doesn't publish it either.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Stopping a streaming prompt saves the turn to the conversation history: the prompt and the answer text streamed so far (empty if none)
- [x] #2 The save happens exactly once per stopped run: a late completion or error after stop() doesn't save it again
- [ ] #3 The agent trace recorded before the stop is persisted with the conversation
- [x] #4 Saving after a stop doesn't write to the database on the EDT
- [x] #5 Stopping a non-streaming prompt also saves the prompt to the history
- [x] #6 Tests written first: stop saves once with the partial text, stop with no text still saves the prompt, completion/error after stop don't save again
- [ ] #7 Verified manually: a stopped agent prompt appears in the conversation history, and View Agent Run shows its steps up to the stop
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Fix 2026-11-28:
- StreamingResponseHandler.stop() now ends with persistStoppedRun(). It sets the answer streamed so far (possibly empty) as the AI message and publishes CONVERSATION_TOPIC on a pooled thread, since stop() runs on the EDT and saving writes SQLite. It runs after the chat-memory cleanup, which only removes a partial message while none is set. Exactly once: onCompleteResponse and persistPartialResponseOnError already skip a stopped run, a second stop() is a no-op, and a run that already completed (isCompleted) isn't saved again.
- NonStreamingPromptStrategy: the cancellation branch on the prompt pool thread saves the prompt with an empty answer (saveStoppedPrompt) before cancelling the result task. It excludes the success path, so a run is saved once.
- The agent trace of a stopped turn is saved by the same event (ChatService.saveConversation persists the buffered turn). AC #3 is left for the manual check.
- Tests written first: 4 in StreamingResponseHandlerTest (stop saves once with the partial text, stop with no text still saves, late completion/error after stop don't save again, the save runs on a pooled thread) and 1 in NonStreamingPromptStrategyTest. Full ./gradlew test --rerun passes (JDK 21).
<!-- SECTION:NOTES:END -->
