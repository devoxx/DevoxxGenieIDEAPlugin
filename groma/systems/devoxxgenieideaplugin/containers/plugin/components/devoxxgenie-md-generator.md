---
type: C4 Component
title: DEVOXXGENIE.md generator
status: stable
groma:
  id: devoxxgenie-md-generator
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/DevoxxGenieGenerator.java
      symbol: DevoxxGenieGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/cpp/CppProjectScannerExtension.java
      symbol: CppProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/go/GoProjectScannerExtension.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/java/JavaProjectScannerExtension.java
      symbol: JavaProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/javascript/JavaScriptProjectScannerExtension.java
      symbol: JavaScriptProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/kotlin/KotlinProjectScannerExtension.java
      symbol: KotlinProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/php/PhpProjectScannerExtension.java
      symbol: PhpProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/python/PythonProjectScannerExtension.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/languages/rust/RustProjectScannerExtension.java
      symbol: RustProjectScannerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/ProjectAnalyzer.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/ProjectAnalyzerExtension.java
      symbol: ProjectAnalyzerExtension
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/tools/GlobTool.java
      symbol: GlobTool
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/util/CachedProjectScanner.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analyzer/util/GitignoreParser.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/BuildCommandsGenerator.java
      symbol: BuildCommandsGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/ContentBuilder.java
      symbol: ContentBuilder
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/ContentGenerator.java
      symbol: ContentGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/impl/GradleCommandsGenerator.java
      symbol: GradleCommandsGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/impl/JavaStyleGenerator.java
      symbol: JavaStyleGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/impl/RustCommandsGenerator.java
      symbol: RustCommandsGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/impl/RustStyleGenerator.java
      symbol: RustStyleGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/LanguageStyleGenerator.java
      symbol: LanguageStyleGenerator
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/content/PromptBuilder.java
      symbol: PromptBuilder
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/file/FileManager.java
      symbol: FileManager
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/generator/tree/ProjectTreeGenerator.java
      symbol: ProjectTreeGenerator
  group: Prompt pipeline
description: Analyses the project and writes DEVOXXGENIE.md
---

Detects languages and build tools through per-language analyzer extensions (an extension point other plugins can implement), then generates build commands, code style notes and a project tree into DEVOXXGENIE.md, which is added to every system prompt.
