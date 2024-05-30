## Devoxx Genie 

<img height="128" src="src/main/resources/icons/pluginIcon.svg" width="128"/>

Devoxx Genie is a fully Java-based LLM plugin for IntelliJ IDEA, designed to integrate with local LLM providers such as [Ollama](https://ollama.com/), [LMStudio](https://lmstudio.ai/), [GPT4All](https://gpt4all.io/index.html) and [Jan](https://jan.ai) but also cloud based LLM's such as [OpenAI](https://openai.com), [Anthropic](https://www.anthropic.com/), [Mistral](https://mistral.ai/), [Groq](https://groq.com/), [Gemini](https://aistudio.google.com/app/apikey). 

We now also support LLM-driven web search with Google and Tavily.

This plugin aids in reviewing, testing, and explaining your project code, enhancing your development workflow.

[<img width="200" alt="Marketplace" src="https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/1c24d692-37ea-445d-8015-2c25f63e2f90">](https://plugins.jetbrains.com/plugin/24169-devoxxgenie)

https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/a4a4b095-63ab-41cd-add9-c0bca852c21c

### Key Features:

- **100% Java**: An IDEA plugin using local and cloud based LLM models. Fully developed in Java using [Langchain4J](https://github.com/langchain4j/langchain4j)
- **Explain**: Explain code using local and cloud-based LLM's.
- **Review**: Review and improve your code using local and cloud-based LLM's.
- **Test**: Generate unit tests for your code using local and cloud-based LLM's.
- **Code Higlighting**: Supports highlighting of code blocks.
- **Chat conversations**: Supports chat conversations with configurable memory size.
- **Add files & code snippets to context**: You can add open files to the chat window context for producing better answers or code snippets if you want to have a super focused window
- **LLM enabled Web Search**: With support for Google and Tavily 

See new features in action @ [https://www.youtube.com/watch?v=7IJrKIS1eN8 ](https://www.youtube.com/watch?v=E9PcKBSMv8U)

![GenieExample](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/5064cef3-e7f8-4ab8-9485-2dbd0a7788df)

We now support also streaming responses which you can enable in the Settings page 🤩 🚀

https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/8081d4f2-c5c4-4283-af1d-19061b7ae7bf

### Installation:

- **From IntelliJ IDEA**: Go to `Settings` -> `Plugins` -> `Marketplace` -> Enter 'Devoxx' to find [plugin](https://plugins.jetbrains.com/plugin/24169-devoxxgenie) OR Install plugin from Disk
- **From Source Code**: Clone the repository, build the plugin using `./gradlew buildPlugin`, and install the plugin from the `build/distributions` directory and select file 'DevoxxGenie-X.Y.Z.zip'

 
### LLM Settings
In the IDEA settings you can modify the REST endpoints and the LLM parameters.  Make sure to press enter and apply to save your changes.

We now also support Cloud based LLMs, you can paste the API keys on the Settings page. 

<img width="1196" alt="DevoxxGenieSettings" src="https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/266780ce-e640-4815-b6fc-7b2a3f86292a">

### Requirements:

- **IntelliJ** minimum version is 2023.3.4
- **Java** minimum version is JDK 17

### Build

Gradle IntelliJ Plugin prepares a ZIP archive when running the buildPlugin task.  
You'll find it in the build/distributions/ directory

```shell
./gradlew buildPlugin 
```

### Publish plugin

It is recommended to use the publishPlugin task for releasing the plugin

```shell
./gradlew publishPlugin
```


### Usage:
1) Select an LLM provider from the DevoxxGenie panel (right corner)
2) Select some code 
4) Enter shortcode command review, explain, generate unit tests of the selected code or enter a custom prompt.

Enjoy! 
