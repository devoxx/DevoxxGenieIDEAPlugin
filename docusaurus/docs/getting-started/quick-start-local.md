---
sidebar_position: 3
---

# Quick Start with Local LLMs

This guide will help you get started with DevoxxGenie using local LLM providers. Running LLMs locally gives you privacy and doesn't require API keys, though it does require more system resources.

## Prerequisites

Before starting, ensure you have:
- DevoxxGenie plugin installed in IntelliJ IDEA
- One of the supported local LLM providers installed on your system

## Supported Local LLM Providers

DevoxxGenie supports several local LLM providers:

- [Ollama](https://ollama.com/)
- [LMStudio](https://lmstudio.ai/)
- [GPT4All](https://gpt4all.io/index.html)
- [Llama.cpp](https://github.com/ggerganov/llama.cpp)
- [Jan](https://jan.ai/)
- Custom OpenAI-compatible providers

## Setting Up with Ollama (Recommended)

Ollama is one of the easiest ways to run LLMs locally. Here's how to set it up with DevoxxGenie:

### 1. Install Ollama

Download and install Ollama from [ollama.com/download](https://ollama.com/download) for your operating system.

### 2. Download a Model

After installing Ollama, open a terminal or command prompt and run:

```bash
ollama run llama3.2
```

This will download the Llama 3.2 model. You can also try other models like:

```bash
ollama run phi2    # Smaller, faster model
ollama run llava   # Multimodal model with image support
ollama run codellama   # Optimized for code
```

### 3. Configure DevoxxGenie

1. In IntelliJ IDEA, click on the DevoxxGenie icon in the right toolbar
2. In the DevoxxGenie window, click on the settings dropdown (top-right corner)
3. Select "Ollama" as the LLM provider
4. Select your downloaded model from the model dropdown

![Ollama Setup](../../static/img/ollama-setup.png)

### 4. Start Using DevoxxGenie

Now you can start prompting DevoxxGenie. Try:
- Selecting a code snippet and asking for an explanation
- Asking for help with a programming concept
- Requesting code generation for a specific task

## Setting Up with Other Local Providers

### LMStudio

1. Download and install [LMStudio](https://lmstudio.ai/)
2. Launch LMStudio and download a model of your choice
3. Start the local server in LMStudio
4. In DevoxxGenie settings, select "LMStudio" as the provider
5. Choose your model from the dropdown

### GPT4All

1. Download and install [GPT4All](https://gpt4all.io/)
2. Launch GPT4All and download a model
3. Start the API server in GPT4All
4. In DevoxxGenie settings, select "GPT4All" as the provider
5. Configure the model settings as needed

### Custom OpenAI-Compatible Providers

For other local providers that expose an OpenAI-compatible API:

1. In DevoxxGenie settings, select "Custom OpenAI" as the provider
2. Configure the URL endpoint (e.g., `http://localhost:8000/v1`)
3. Set the model name as required by your provider
4. Adjust other settings as needed

## Troubleshooting Local LLM Issues

### Connection Problems

If DevoxxGenie can't connect to your local LLM provider:

1. Ensure the LLM service is running
2. Check that the port is not blocked by a firewall
3. Verify the URL in DevoxxGenie settings matches your LLM provider's endpoint
4. For Ollama, check that it's running with `ollama list`

### Performance Issues

If responses are slow or the model is struggling:

1. Try a smaller model that requires fewer resources
2. Ensure your system has adequate RAM and GPU (if applicable)
3. Close other resource-intensive applications
4. Adjust the context window size in settings to be smaller

### Next Steps

Now that you've set up DevoxxGenie with a local LLM provider, you can:

- [Learn about different features](../features/overview.md)
- [Configure prompt settings](../configuration/prompts.md)
- [Try more advanced features like RAG](/docs/features/rag)
