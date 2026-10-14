---
type: C4 Component
title: Shared utilities
status: stable
groma:
  id: shared-utilities
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/error/ErrorHandler.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/Constant.java
      symbol: Constant
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/exception/ModelNotActiveException.java
      symbol: ModelNotActiveException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/exception/ProviderUnavailableException.java
      symbol: ProviderUnavailableException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/exception/UnsuccessfulRequestException.java
      symbol: UnsuccessfulRequestException
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/NoOpProgressIndicator.java
      symbol: NoOpProgressIndicator
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/topic/AppTopics.java
      symbol: AppTopics
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/ClipboardUtil.java
      symbol: ClipboardUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/FileUtil.java
      symbol: FileUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/HttpClientProvider.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/HttpUtil.java
      symbol: HttpUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/ImageUtil.java
      symbol: ImageUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/LocalDateTimeConverter.java
      symbol: LocalDateTimeConverter
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/MessageBusUtil.java
      symbol: MessageBusUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/ReadAccess.java
      symbol: ReadAccess
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/SafeJacksonJsonCodecFactory.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/TemplateVariableEscaper.java
      symbol: TemplateVariableEscaper
    - scanner: java
      file: src/main/java/com/devoxx/genie/util/ThreadUtils.java
      symbol: ThreadUtils
  group: Settings and platform
description: Cross-cutting helpers, message-bus topics and common exceptions
---

Message-bus topic definitions, HTTP client provider, JSON codec, file, image, clipboard, threading and read-access helpers, error handling and provider exceptions shared by most components.
