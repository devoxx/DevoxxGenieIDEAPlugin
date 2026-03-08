---
id: TASK-200
title: OpenAI GPT-5 does not support top_p parameter
status: To Do
assignee: []
created_date: '2026-03-08 11:29'
labels:
  - bug
  - openai
  - gpt-5
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/chatmodel/cloud/openai/OpenAiChatModelFactory.java
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When using OpenAI GPT-5, the plugin sends the `top_p` parameter which is not supported by this model, causing a runtime error.

**Error:**
```
ExecutionException - Error occurred while processing chat message
Caused by: CompletionException - com.devoxx.genie.service.prompt.error.ModelException: Provider unavailable:
{
  "error": {
    "message": "Unsupported parameter: 'top_p' is not supported with this model.",
    "type": "invalid_request_error",
    "param": "top_p",
    "code": "unsupported_parameter"
  }
}
```

**Root Cause:** The OpenAI chat model factory sends `top_p` as a parameter when building the chat model request, but GPT-5 does not accept it.

**Suggested Fix:** Conditionally omit `top_p` (and potentially `top_k`) for models that don't support these parameters. This could be handled in the `OpenAiChatModelFactory` by checking the model name or by catching the error and retrying without the unsupported parameter.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GPT-5 model works without top_p error
- [ ] #2 Other OpenAI models that support top_p continue to work as before
- [ ] #3 No regression in other cloud providers
<!-- AC:END -->
