---
sidebar_position: 1
title: LLM Providers Overview - DevoxxGenie Documentation
description: A comprehensive guide to all LLM providers supported by DevoxxGenie, including local and cloud-based options, with comparison of features and capabilities.
keywords: [devoxxgenie, intellij plugin, llm providers, openai, anthropic, local llm, cloud llm, mistral, ollama]
image: /img/devoxxgenie-social-card.jpg
---

# LLM Providers Overview

DevoxxGenie supports a wide range of Language Model (LLM) providers, both local and cloud-based. This flexibility allows you to choose the provider that best fits your needs in terms of performance, privacy, and cost.

## Supported Provider Types

DevoxxGenie integrates with two main types of LLM providers:

### Local LLM Providers

Local providers run models directly on your machine, offering:

- **Privacy**: Your code and prompts stay on your computer
- **No API costs**: Use without usage fees or API keys
- **Offline usage**: Work without internet connection
- **Customization**: Fine-tune models for your specific needs

Popular local LLM providers supported by DevoxxGenie include:
- [Ollama](local-models.md#ollama)
- [LMStudio](local-models.md#lmstudio)
- [GPT4All](local-models.md#gpt4all)
- [Llama.cpp](local-models.md#llamacpp)
- [Exo](local-models.md#exo)
- [JLama](local-models.md#jlama)
- [Jan](local-models.md#jan)

### Cloud LLM Providers

Cloud providers offer powerful models as a service:

- **Power**: Access to state-of-the-art models
- **No local resources**: Run without high-end hardware
- **Easy setup**: No model downloads or configuration
- **Regular updates**: Always use the latest models

Cloud LLM providers supported by DevoxxGenie include:
- [OpenAI](cloud-models.md#openai) (GPT-4, GPT-3.5, etc.)
- [Anthropic](cloud-models.md#anthropic) (Claude models)
- [Google](cloud-models.md#google) (Gemini models)
- [Mistral](cloud-models.md#mistral)
- [Groq](cloud-models.md#groq)
- [DeepInfra](cloud-models.md#deepinfra)
- [DeepSeek](cloud-models.md#deepseek)
- [OpenRouter](cloud-models.md#openrouter)
- [Azure OpenAI](cloud-models.md#azure-openai)
- [Amazon Bedrock](cloud-models.md#amazon-bedrock)

## Provider Selection

You can select and configure LLM providers in two ways:

1. **From the DevoxxGenie Panel**: Use the dropdown in the top-right corner of the DevoxxGenie window
2. **From Settings**: Go to Settings > Tools > DevoxxGenie

![Provider Selection](/img/provider-selection.png)

## Provider Comparison

| Provider | Privacy | Setup Complexity | Cost | Performance | Multimodal |
|----------|---------|------------------|------|-------------|------------|
| **Local Providers** |
| Ollama | High | Low | Free | Varies by hardware | Some models |
| LMStudio | High | Medium | Free | Varies by hardware | Some models |
| GPT4All | High | Low | Free | Basic | No |
| Llama.cpp | High | High | Free | Varies by hardware | Some models |
| Exo | High | Medium | Free | Good on Apple Silicon | Some models |
| JLama | High | Medium | Free | Good for Java models | No |
| Jan | High | Low | Free | Basic | No |
| **Cloud Providers** |
| OpenAI | Low | Low | Pay-per-token | Excellent | GPT-4V+ |
| Anthropic | Low | Low | Pay-per-token | Excellent | Claude 3+ |
| Google | Low | Low | Pay-per-token | Excellent | Yes |
| Mistral | Low | Low | Pay-per-token | Very Good | No |
| Groq | Low | Low | Pay-per-token | Very Good | No |
| DeepSeek | Low | Low | Pay-per-token | Good | No |
| Azure OpenAI | Medium | Medium | Pay-per-token | Excellent | Depends |
| Amazon Bedrock | Medium | High | Pay-per-token | Very Good | Some models |

## Choosing the Right Provider

When selecting an LLM provider, consider:

### For Local Providers

- **Hardware Requirements**: Local models need sufficient RAM and CPU/GPU
- **Model Size vs. Quality**: Smaller models run faster but may have lower quality
- **Specific Capabilities**: Some models are specialized for code, some for general text

### For Cloud Providers

- **Cost**: Different providers have different pricing structures
- **Quality Needs**: Some tasks require more powerful models
- **Context Window**: How much context can be included with your prompts
- **Specific Features**: Some providers offer unique capabilities

## Provider Configuration

Each provider requires specific configuration:

### Local Providers

- **Endpoint URLs**: Where to connect to the local service
- **Model Selection**: Which model to use
- **Parameter Settings**: Temperature, tokens, etc.

### Cloud Providers

- **API Keys**: Authentication keys for the service
- **Model Selection**: Which model variant to use
- **Parameter Settings**: Temperature, tokens, etc.

For detailed configuration instructions, see:
- [Local Models Configuration](local-models.md)
- [Cloud Models Configuration](cloud-models.md)
- [Custom Providers](custom-providers.md)

## Provider-Specific Features

Some features in DevoxxGenie are provider-dependent:

- **Multimodal Support**: Image support requires compatible providers like OpenAI GPT-4V, Claude 3, or Gemini
- **Window Context Size**: Varies by provider and model
- **Streaming Support**: Not all providers support token streaming
- **Cost Calculation**: Only available for cloud providers

## Troubleshooting Provider Issues

If you encounter issues with a specific provider:

1. **Check Connection**: Ensure the service is running and accessible
2. **Verify Settings**: Double-check API keys and endpoint URLs
3. **Check Logs**: Look for error messages in the IDE log
4. **Provider Status**: Check the provider's status page for outages
5. **Model Selection**: Try a different model from the same provider

For more detailed troubleshooting steps, see the specific provider pages.
