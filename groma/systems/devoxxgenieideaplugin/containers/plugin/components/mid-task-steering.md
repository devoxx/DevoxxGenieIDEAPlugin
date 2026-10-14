---
type: C4 Component
title: Mid-task steering
status: stable
groma:
  id: mid-task-steering
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/steering/PendingPromptQueue.java
      symbol: PendingPromptQueue
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/steering/SteeringMessageInjector.java
      symbol: SteeringMessageInjector
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/prompt/steering/SteeringMessageQueue.java
      symbol: SteeringMessageQueue
  group: Prompt pipeline
description: Queues user messages typed while an agent is running and injects them into the loop
---

Holds pending steering messages and, through a langchain4j `chatRequestTransformer`, appends them to the next agent round-trip request and to chat memory.
