---
sidebar_position: 4
title: Quick Start with Cloud LLMs
description: Get started with DevoxxGenie using cloud LLM providers like OpenAI, Anthropic Claude, Google Gemini, Mistral, and more in under 2 minutes.
keywords: [devoxxgenie, quick start, cloud llm, openai, anthropic, claude, gemini, mistral, groq, api key, setup]
image: /img/devoxxgenie-social-card.jpg
---

# Quick Start with Cloud LLMs

This guide will help you get started with DevoxxGenie using cloud-based LLM providers. Cloud providers offer more powerful models without requiring local computational resources, though they do require API keys and typically have associated costs.

## Prerequisites

Before starting, ensure you have:
- DevoxxGenie plugin installed in IntelliJ IDEA
- API key(s) for your chosen cloud LLM provider(s)
- Internet connection for accessing cloud services

## Supported Cloud LLM Providers

DevoxxGenie supports several cloud LLM providers:

- [OpenAI](https://openai.com) (ChatGPT, GPT-4, etc.)
- [Anthropic](https://www.anthropic.com/) (Claude models)
- [Mistral](https://mistral.ai/)
- [Groq](https://groq.com/)
- [Google](https://aistudio.google.com/app/apikey) (Gemini models)
- [DeepInfra](https://deepinfra.com/dash/deployments)
- [DeepSeek](https://www.deepseek.com/)
- [Kimi](https://platform.moonshot.ai/)
- [GLM](https://open.bigmodel.cn/) (Zhipu AI)
- [OpenRouter](https://www.openrouter.ai/)
- [Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service)
- [Amazon Bedrock](https://aws.amazon.com/bedrock)

## Setting Up with OpenAI

Here's how to set up DevoxxGenie with OpenAI:

### 1. Get an OpenAI API Key

1. Create or log in to your [OpenAI account](https://platform.openai.com/)
2. Navigate to the API section
3. Create a new API key or use an existing one
4. Copy the API key for use in DevoxxGenie

### 2. Configure DevoxxGenie

1. In IntelliJ IDEA, click on the DevoxxGenie icon in the right toolbar
2. Click the settings (gear) icon in the DevoxxGenie window
3. Select "OpenAI" as your LLM provider
4. Paste your API key in the API Key field
5. Select a model from the dropdown (e.g., gpt-4, gpt-3.5-turbo)
6. Click "Apply" to save your settings

![OpenAI Setup](../../static/img/openai-setup.png)

### 3. Start Using DevoxxGenie

Now you can start prompting DevoxxGenie with OpenAI. Try:
- Selecting a code snippet and asking for an explanation
- Requesting code generation for a specific task
- Using the "Add Project" feature to include project context in your prompts

## Setting Up with Anthropic (Claude)

### 1. Get an Anthropic API Key

1. Create or log in to your [Anthropic account](https://console.anthropic.com/)
2. Generate an API key from your account settings
3. Copy the API key

### 2. Configure DevoxxGenie

1. Open DevoxxGenie settings
2. Select "Anthropic" as your LLM provider
3. Paste your API key
4. Select a Claude model from the dropdown
5. Click "Apply" to save

### 3. Start Using Anthropic with DevoxxGenie

Claude models excel at understanding and generating code with nuanced explanations.

## Setting Up with Other Cloud Providers

The process is similar for other cloud providers:

1. Obtain an API key from your chosen provider's website
2. Open DevoxxGenie settings
3. Select the provider from the dropdown
4. Enter your API key
5. Select a model
6. Configure any provider-specific settings
7. Click "Apply" to save

## Cost Management

Cloud LLM providers charge based on token usage. DevoxxGenie helps manage costs with:

1. **Token Calculator**: Calculate the cost of prompts before sending
2. **Token Usage Display**: See token usage for each request
3. **Context Settings**: Control the amount of context included in prompts

To configure cost settings:

1. Go to DevoxxGenie settings
2. Navigate to "Token Cost & Context Window"
3. Review and adjust settings as needed

![Token Cost Settings](../../static/img/token-cost-settings.png)

## Security Considerations

When using cloud providers, consider:

- **API Key Security**: Your API keys are stored in the IDE's secure storage
- **Data Privacy**: Consider what code and data you're sending to cloud providers
- **Usage Monitoring**: Regularly check your usage on the provider's website to manage costs

## Troubleshooting Cloud Provider Issues

### API Key Problems

If you're having issues with your API key:

1. Verify the key is correct and hasn't expired
2. Check that you have billing set up with the provider (if required)
3. Ensure you have the appropriate permissions for the models you're trying to use

### Network Issues

If you're having connection problems:

1. Check your internet connection
2. Verify that your network allows connections to the API endpoints
3. Check if you need to configure a proxy in IntelliJ IDEA settings

### Next Steps

Now that you've set up DevoxxGenie with a cloud LLM provider, you can:

- [Learn about different features](../features/overview.md)
- [Configure prompt settings](../configuration/prompts.md)
- [Try advanced features like RAG](/docs/features/rag)
- [Use web search with your queries](../features/web-search.md)
