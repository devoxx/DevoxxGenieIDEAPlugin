# Devoxx Genie 

<img height="128" src="src/main/resources/META-INF/pluginIcon.svg" width="128"/>

Devoxx Genie is a fully Java-based LLM plugin for IntelliJ IDEA, designed to integrate with local LLM providers such as [Ollama](https://ollama.com/), [LMStudio](https://lmstudio.ai/), and [GPT4All](https://gpt4all.io/index.html). This plugin aids in reviewing, testing, and explaining your project code, enhancing your development workflow.

https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/7c7e8744-4bcf-4730-89d5-bb79b7ad1a53


### Key Features:

- **100% Java**: An IDEA plugin using local LLM models using Java and [Langchain4J](https://github.com/langchain4j/langchain4j)
- **Explain**: Explain code using local LLM's.
- **Review**: Review and improve your code using local LLM's.
- **Test**: Generate unit tests for your code using local LLM's.

### Installation:

- **From IntelliJ IDEA**: Go to `Settings` -> `Plugins` -> `Marketplace` -> Enter 'Devoxx' to find plugin (pending approval) OR Install plugin from Disk
- **From Source Code**: Clone the repository, build the plugin using `./gradlew buildPlugin`, and install the plugin from the `build/distributions` directory and select file 'DevoxxGenie-0.0.3.zip'

 
### LLM Settings
In the IDEA settings you can modify the REST endpoints and the LLM parameters.  Make sure to press enter and apply to save your changes.

<img width="1194" alt="DevoxxSettings" src="https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/3f79f716-1647-49be-a155-8563ba340629">

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
