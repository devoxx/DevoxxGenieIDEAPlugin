# DEVOXXGENIE.md

## Project Guidelines

### Build Commands

- **Build:** `./gradlew build`
- **Test:** `./gradlew test`
- **Single Test:** `./gradlew test --tests ClassName.methodName`

### Code Style

### Dependencies

The project uses the following main dependencies:

- **LangChain4j** - Java library for LLM applications
- **JUnit** - Testing framework
- **Mockito** - Mocking framework for tests
- **Retrofit** - HTTP client for API calls

See build.gradle.kts or pom.xml for the complete dependency list.



### Project Tree

```
DevoxxGenieIDEAPlugin/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── devoxx/
│   │   │           └── genie/
│   │   │               ├── action/
│   │   │               │   ├── AddDirectoryAction.java
│   │   │               │   ├── AddFileAction.java
│   │   │               │   ├── AddSnippetAction.java
│   │   │               │   ├── CalcTokensForDirectoryAction.java
│   │   │               │   └── ExcludeDirectoryAction.java
│   │   │               ├── chatmodel/
│   │   │               │   ├── ChatModelFactory.java
│   │   │               │   ├── ChatModelFactoryProvider.java
│   │   │               │   └── ChatModelProvider.java
│   │   │               ├── controller/
│   │   │               │   ├── ActionButtonsPanelController.java
│   │   │               │   ├── ProjectContextController.java
│   │   │               │   ├── PromptExecutionController.java
│   │   │               │   └── TokenCalculationController.java
│   │   │               ├── error/
│   │   │               │   └── ErrorHandler.java
│   │   │               ├── model/
│   │   │               │   ├── ChatContextParameters.java
│   │   │               │   ├── ChatModel.java
│   │   │               │   ├── Constant.java
│   │   │               │   ├── GenericOpenAIProvider.java
│   │   │               │   └── ScanContentResult.java
│   │   │               ├── service/
│   │   │               │   ├── ChatMemoryService.java
│   │   │               │   ├── ChatPromptExecutor.java
│   │   │               │   ├── ChatService.java
│   │   │               │   ├── FileListManager.java
│   │   │               │   ├── FileListObserver.java
│   │   │               │   ├── LLMModelRegistryService.java
│   │   │               │   ├── LLMProviderService.java
│   │   │               │   ├── MessageCreationService.java
│   │   │               │   ├── NoOpProgressIndicator.java
│   │   │               │   ├── NonStreamingPromptExecutor.java
│   │   │               │   ├── PostStartupActivity.java
│   │   │               │   ├── ProjectContentService.java
│   │   │               │   ├── PromptExecutionService.java
│   │   │               │   ├── PropertiesService.java
│   │   │               │   └── TokenCalculationService.java
│   │   │               ├── ui/
│   │   │               │   ├── ConversationStarter.java
│   │   │               │   ├── DevoxxGenieToolWindowContent.java
│   │   │               │   ├── DevoxxGenieToolWindowFactory.java
│   │   │               │   └── EditorFileButtonManager.java
│   │   │               └── util/
│   │   │                   ├── ChatMessageContextUtil.java
│   │   │                   ├── ClipboardUtil.java
│   │   │                   ├── DefaultLLMSettingsUtil.java
│   │   │                   ├── DockerUtil.java
│   │   │                   ├── FileUtil.java
│   │   │                   ├── HttpClientProvider.java
│   │   │                   ├── HttpUtil.java
│   │   │                   ├── ImageUtil.java
│   │   │                   ├── LocalDateTimeConverter.java
│   │   │                   └── MessageBusUtil.java
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── clion-features.xml
│   │       │   ├── goland-features.xml
│   │       │   ├── java-features.xml
│   │       │   ├── kotlin-features.xml
│   │       │   ├── php-features.xml
│   │       │   ├── plugin.xml
│   │       │   ├── pluginIcon.svg
│   │       │   ├── python-features.xml
│   │       │   ├── rust-features.xml
│   │       │   └── webstorm-features.xml
│   │       ├── icons/
│   │       │   ├── addNewFile.svg
│   │       │   ├── addNewFile_dark.svg
│   │       │   ├── arrowExpand.svg
│   │       │   ├── arrowExpand_dark.svg
│   │       │   ├── arrowExpanded.svg
│   │       │   ├── arrowExpanded_dark.svg
│   │       │   ├── calculator.svg
│   │       │   ├── calculator_dark.svg
│   │       │   ├── class.svg
│   │       │   ├── clock.svg
│   │       │   ├── clock_dark.svg
│   │       │   ├── closeSmall_dark.svg
│   │       │   ├── codeSnippet.svg
│   │       │   ├── codeSnippet_dark.svg
│   │       │   ├── cog.svg
│   │       │   ├── cog_dark.svg
│   │       │   ├── copy.svg
│   │       │   ├── copy_dark.svg
│   │       │   ├── delete.svg
│   │       │   ├── delete_dark.svg
│   │       │   ├── devoxxHead.png
│   │       │   ├── devoxxHead_dark.png
│   │       │   ├── enum.svg
│   │       │   ├── event.svg
│   │       │   ├── event_dark.svg
│   │       │   ├── google-small.svg
│   │       │   ├── image.svg
│   │       │   ├── image_dark.svg
│   │       │   ├── insertCode.svg
│   │       │   ├── insertCode_dark.svg
│   │       │   ├── inspectionsEye.svg
│   │       │   ├── inspectionsEye_dark.svg
│   │       │   ├── interface.svg
│   │       │   ├── paperPlane.svg
│   │       │   ├── paperPlane_dark.svg
│   │       │   ├── pluginIcon.svg
│   │       │   ├── pluginIcon_dark.svg
│   │       │   ├── plus.svg
│   │       │   ├── plus_dark.svg
│   │       │   ├── record.svg
│   │       │   ├── record_dark.svg
│   │       │   ├── refresh.svg
│   │       │   ├── refresh_dark.svg
│   │       │   ├── stop.svg
│   │       │   ├── trash.svg
│   │       │   ├── trash_dark.svg
│   │       │   ├── web.svg
│   │       │   └── web_dark.svg
│   │       ├── images/
│   │       │   ├── diff_merge.jpg
│   │       │   └── simple_diff.jpg
│   │       ├── application.properties
│   │       ├── logback.xml
│   │       ├── messages.properties
│   │       └── messages_fr_FR.properties
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── devoxx/
│       │           └── genie/
│       │               ├── chatmodel/
│       │               │   └── AbstractLightPlatformTestCase.java
│       │               ├── service/
│       │               │   ├── ChatPromptExecutorIT.java
│       │               │   ├── FileListManagerTest.java
│       │               │   ├── MessageCreationServiceTest.java
│       │               │   └── PromptExecutionServiceIT.java
│       │               ├── ui/
│       │               │   └── ... (1 more items)
│       │               └── util/
│       │                   └── HttpClientProviderTest.java
│       └── resources/
│           └── test-log.properties
└── filesystem-tool-fix.md

```
