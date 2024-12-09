## Devoxx Genie 

<img height="128" src="src/main/resources/icons/pluginIcon.svg" width="128"/>

[![X](https://img.shields.io/twitter/follow/DevoxxGenie)](https://x.com/devoxxgenie)

Devoxx Genie is a fully Java-based LLM Code Assistant plugin for IntelliJ IDEA, designed to integrate with local LLM providers such as [Ollama](https://ollama.com/), [LMStudio](https://lmstudio.ai/), [GPT4All](https://gpt4all.io/index.html), [Llama.cpp](https://github.com/ggerganov/llama.cpp) and [Exo](https://github.com/exo-explore/exo) but also cloud based LLM's such as [OpenAI](https://openai.com), [Anthropic](https://www.anthropic.com/), [Mistral](https://mistral.ai/), [Groq](https://groq.com/), [Gemini](https://aistudio.google.com/app/apikey), [DeepInfra](https://deepinfra.com/dash/deployments), [DeepSeek](https://www.deepseek.com/), [OpenRouter](https://www.openrouter.ai/) and [Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service)

We now also support RAG-based prompt context based on your vectorized project files. 
In addition to Git Dif viewer and LLM-driven web search with [Google](https://developers.google.com/custom-search) and [Tavily](https://tavily.com/).

With Claude 3.5 Sonnet, DevoxxGenie isn't just another developer tool... it's a glimpse into the future of software engineering. As we eagerly await Claude 3.5 Opus, one thing is clear: we're witnessing a paradigm shift in Ai Augmented Programming (AAP) 🐒

[<img width="200" alt="Marketplace" src="https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/1c24d692-37ea-445d-8015-2c25f63e2f90">](https://plugins.jetbrains.com/plugin/24169-devoxxgenie)

### Hands-on with DevoxxGenie

[![DevoxxGenie Demo](https://devoxx.be/wp-content/uploads/2024/08/DevoxxGenieDemo.jpg)](https://www.youtube.com/live/kgtctcbA6WE?feature=shared&t=124)

### More Video Tutorials:

- [DevoxxGenie in action (Devoxx Belgium 2024)](https://www.youtube.com/watch?v=c5EyVLAXaGQ)
- [How ChatMemory works](https://www.youtube.com/watch?v=NRAe4d7n6_4)
- [Hands-on with DevoxxGenie](https://youtu.be/Rs8S4rMTR9s?feature=shared)
- [The Era of AAP: Ai Augmented Programming using only Java](https://www.youtube.com/watch?v=yvgvALVs3xo)

### Blog Posts:

- [DevoxxGenie: Your AI Assistant for IDEA](https://mydeveloperplanet.com/2024/10/08/devoxxgenie-your-ai-assistant-for-idea/)

### Key Features:

- **🧐 RAG Support (🔥 NEW)**: Retrieval-Augmented Generation (RAG) support for automatically incorporating project context into your prompts.
- **💪🏻 Git Diff/Merge** : Show Git Diff/Merge dialog to accept LLM suggestions.
- **👀 Chat History**: Your chats are stored locally, allowing you to easily restore them in the future.
- **🧠 Project Scanner**: Add source code (full project or by package) to prompt context when using Anthropic, OpenAI or Gemini.
- **💰 Token Cost Calculator**: Calculate the cost when using Cloud LLM providers.
- **🔍 Web Search** : Search the web for a given query using Google or Tavily.
- **🏎️ Streaming responses**: See each token as it's received from the LLM in real-time.
- **🧐 Abstract Syntax Tree (AST) context**: Automatically include parent class and class/field references in the prompt for better code analysis.
- **💬 Chat Memory Size**: Set the size of your chat memory, by default its set to a total of 10 messages (system + user & AI msgs).
- **☕️ 100% Java**: An IDEA plugin using local and cloud based LLM models. Fully developed in Java using [Langchain4J](https://github.com/langchain4j/langchain4j)
- **👀 Code Highlighting**: Supports highlighting of code blocks.
- **💬 Chat conversations**: Supports chat conversations with configurable memory size.
- **📁 Add files & code snippets to context**: You can add open files to the chat window context for producing better answers or code snippets if you want to have a super focused window

### Start in 5 Minutes with local LLM

- Download and start [Ollama](https://ollama.com/download)
- Open terminal and download a model using command "ollama run llama3.2"
- Start your IDEA and go to plugins > Marketplace and enter "Devoxx"
- Select "DevoxxGenie" and install plugin
- In the DevoxxGenie window select Ollama and available model
- Start prompting

### Start in 2 Minutes using Cloud LLM

- Start your IDEA and go to plugins > Marketplace and enter "Devoxx"
- Select "DevoxxGenie" and install plugin
- Click on DevoxxGenie cog (settings) icon and click on Cloud Provider link icon to create API KEY
- Paste API Key in Settings panel
- In the DevoxxGenie window select your cloud provider and model
- Start prompting

### 🔥 NEW RAG Feature 

<img width="749" alt="RAG" src="https://github.com/user-attachments/assets/ea34247a-b33d-40a2-b96a-d10de0868dfa">

Devoxx Genie now includes starting from v0.4.0 a Retrieval-Augmented Generation (RAG) feature, which enables advanced code search and retrieval capabilities. 
This feature uses a combination of natural language processing (NLP) and machine learning algorithms to analyze code snippets and identify relevant results based on their semantic meaning.

With RAG, you can:

* Search for code snippets using natural language queries
* Retrieve relevant code examples that match your query's intent
* Explore related concepts and ideas in the codebase

We currently use Ollama and Nomic Text embedding to generates vector representations of your project files.
These embedding vectors are then stored in a Chroma DB running locally within Docker. 
The vectors are used to compute similarity scores between search queries and your code all running locally.

The RAG feature is a significant enhancement to Devoxx Genie's code search capabilities, enabling developers to quickly find relevant code examples and accelerate their coding workflow.

See also [Demo](https://www.youtube.com/watch?v=VVU8x45jIt4)

### LLM Settings
In the IDEA settings you can modify the REST endpoints and the LLM parameters.  Make sure to press enter and apply to save your changes.

We now also support Cloud based LLMs, you can paste the API keys on the Settings page. 

<img width="1072" alt="Settings" src="https://github.com/user-attachments/assets/a88f1ae8-55dc-4c6b-b5eb-ec0c3d70b28f">

### Smart Model Selection and Cost Estimation
The language model dropdown is not just a list anymore, it's your compass for smart model selection.

![Models](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/1924a967-37c3-400c-bac4-fc1a678aeec5)

See available context window sizes for each cloud model
View associated costs upfront
Make data-driven decisions on which model to use for your project

### Add Project to prompt & clipboard

You can now add the full project to your prompt IF your selected cloud LLM has a big enough window context.

![AddFull](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/be014cf1-ee01-428a-bd75-55acc82627fb)

### Calc Cost

Leverage the prompt cost calculator for precise budget management. Get real-time updates on how much of the context window you're using.

![AddCalcProject](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/0c971331-40fe-47a4-8ede-f349fa40c00c)

See the input/output costs and window context per Cloud LLM.  Eventually we'll also allow you to edit these values.

![Cost](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/422fc829-fc9f-42f4-a8e5-c33ec5a239fc)

### Handling Massive Projects?
"But wait," you might say, "my project is HUGE!" 😅 

Fear not! We've got options:

1. Leverage Gemini's Massive Context: 

Gemini's colossal 1 million token window isn't just big, it's massive. We're talking about the capacity to digest approximately 30,000 lines of code in a single go. That's enough to digest most codebases whole, from the tiniest scripts to some decent projects.

But if that's not enough you have more options...

2. Smart Filtering: 

The new "Copy Project" panel lets you: 

Exclude specific directories 
Filter by file extensions
Remove JavaDocs to slim down your context

<img width="1072" alt="ScanProject" src="https://github.com/user-attachments/assets/51523394-1b36-442b-adfa-91d0c7a8182e">

3. Selective Inclusion 

Right-click to add only the most relevant parts of your project to the context.

![RightClick](https://github.com/devoxx/DevoxxGenieIDEAPlugin/assets/179457/a86c311a-4589-41f9-bb4a-c8c4f0b884ee)

## Git Diff viewer
Starting from v0.3.0, you can enable a Git diff/merge viewer to directly review and accept LLM-generated code changes without needing to copy and paste them from the LLM's response.
To activate this feature, navigate to Settings and select "LLM Git Diff Merge." You can then choose between a two-panel or three-panel Git diff view.

![git_diff](https://github.com/user-attachments/assets/114bb3cf-2824-442b-bdd3-483fb0d58983)
![diff_merge](https://github.com/user-attachments/assets/eec2d6c8-5145-4729-8962-7033807018b1)

For example, the two-panel Git diff view works seamlessly with the local Ollama LLM provider and the Llama 3.2 3B model.

https://github.com/user-attachments/assets/817159ab-586f-4d46-bd46-bc0097805aed

## The Power of Full Context: A Real-World Example
The DevoxxGenie project itself, at about 70K tokens, fits comfortably within most high-end LLM context windows. 
This allows for incredibly nuanced interactions – we're talking advanced queries and feature requests that leave tools like GitHub Copilot scratching their virtual heads!

## Support for JLama & LLama3.java
DevoxxGenie now also supports the 100% Modern Java LLM inference engines: [JLama](https://github.com/tjake/Jlama).

JLama offers a REST API compatible with the widely-used OpenAI API.

![image](https://github.com/user-attachments/assets/65096be3-2b34-4fea-8295-d63e04066390)

You can also integrate it seamlessly with [Llama3.java](https://github.com/stephanj/Llama3JavaChatCompletionService) but using the Spring Boot OpenAI API wrapper coupled with the JLama DevoxxGenie option.

## Local LLM Cluster with Exo

V0.2.7 also supports Exo, a local LLM cluster for Apple Silicon which allows you to run Llama 3.1 8b, 70b and 405b on your own Apple computers 🤩

![image](https://github.com/user-attachments/assets/a79033ff-d9dd-442d-aa92-0fc70cc37747)


### Installation:

- **From IntelliJ IDEA**: Go to `Settings` -> `Plugins` -> `Marketplace` -> Enter 'Devoxx' to find [plugin](https://plugins.jetbrains.com/plugin/24169-devoxxgenie) OR Install plugin from Disk
- **From Source Code**: Clone the repository, build the plugin using `./gradlew buildPlugin`, and install the plugin from the `build/distributions` directory and select file 'DevoxxGenie-X.Y.Z.zip'

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
