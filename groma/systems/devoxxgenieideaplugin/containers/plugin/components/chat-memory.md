---
type: C4 Component
title: Chat memory
status: stable
groma:
  id: chat-memory
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryManager.java
      symbol: ChatMemoryManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/memory/ChatMemoryService.java
      symbol: ChatMemoryService
  group: Prompt pipeline
  technology: langchain4j ChatMemory
description: Per-conversation message window sent to the LLM
---

Keeps a bounded langchain4j chat memory per project/tab so follow-up prompts and agent round trips carry earlier turns.
