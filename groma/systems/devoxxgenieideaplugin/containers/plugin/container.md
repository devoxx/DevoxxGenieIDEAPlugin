---
type: C4 Container
title: DevoxxGenie IntelliJ plugin
status: stable
groma:
  id: plugin
  parent: devoxxgenieideaplugin
  technology: Java 17+, Kotlin, IntelliJ Platform SDK, langchain4j, Compose for Desktop, Gradle
description: The plugin that runs inside the developer's IDE
---

A JetBrains plugin (declared in `src/main/resources/META-INF/plugin.xml`) loaded into the IDE JVM. It contributes the DevoxxGenie and activity-log tool windows, editor and project-view actions, settings pages, startup activities and project/application services.

All LLM traffic goes through langchain4j. UI work runs on the EDT; prompts, indexing and external processes run on pooled threads. The main chat output is rendered with Compose for Desktop from Kotlin sources under `src/main/kotlin`, which no configured Groma scanner reads, so those files have no component on this map (see Conversation view). Conversations are persisted in a local SQLite database, settings in IntelliJ persistent state, secrets in PasswordSafe.
