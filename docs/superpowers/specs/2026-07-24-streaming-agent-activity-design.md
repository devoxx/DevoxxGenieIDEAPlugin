# Streaming Agent Activity Design

## Goal
Show agent tool requests and results in the matching conversation while a response streams, just as they are shown when streaming is disabled.

## Context
The Activity Log receives matching `AGENT` events for streaming runs, but the Compose conversation timeline does not display them. Streaming response text is explicitly applied on the IntelliJ EDT, while `ConversationPanel` forwards activity messages synchronously from the publishing thread to the Compose view model. This mixes concurrent UI-state updates and allows streamed response rendering to replace an activity update.

## Design
`ConversationPanel` will marshal each accepted activity message onto the IntelliJ EDT before calling `ConversationViewController.onActivityMessage`. It will keep its existing project-location filter. The EDT then serializes activity changes with streaming partial/final response updates, loading-indicator changes, and the Compose runtime.

The change intentionally applies to all activity sources delivered to the conversation panel. `ConversationViewModel` remains responsible for excluding `RAW` messages, so raw request and response payloads remain visible only in the Activity Log tool window.

## Alternatives Considered
1. Marshal activity delivery in `ConversationPanel` (chosen): a narrow, UI-bound ordering fix at the message-bus-to-Compose boundary.
2. Marshal every `ComposeConversationViewController` method: broader than required and risks changing unrelated callers.
3. Buffer activity inside streaming handlers: requires correlating events to requests and would weaken live progress updates.

## Testing
Add a regression test that verifies activity events are dispatched through the EDT boundary and retain the request/result lifecycle while a streaming response update is queued. Keep existing view-model tests for non-streaming behavior and RAW-message exclusion.
