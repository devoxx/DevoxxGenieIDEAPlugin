---
sidebar_position: 3
title: Cloud LLM Providers
description: Guide to all cloud-based LLM providers supported by DevoxxGenie, including setup instructions and configuration.
keywords: [devoxxgenie, openai, anthropic, google, gemini, grok, mistral, groq, deepseek, kimi, moonshot, glm, zhipu, cloud llm]
image: /img/devoxxgenie-social-card.jpg
---

# Cloud LLM Providers

DevoxxGenie supports a wide range of cloud-based LLM providers, giving you access to powerful state-of-the-art models without requiring high-end local hardware.

:::tip
For all cloud providers, available models are **automatically fetched** from the provider's API when you select the provider and enter your API key. You don't need to manually enter model names.
:::

## Supported Cloud Providers

1. [OpenAI](#openai)
2. [Anthropic](#anthropic)
3. [Google](#google)
4. [Grok](#grok)
5. [Mistral](#mistral)
6. [Groq](#groq)
7. [DeepInfra](#deepinfra)
8. [DeepSeek](#deepseek)
9. [Kimi](#kimi)
10. [GLM](#glm)
11. [OpenRouter](#openrouter)
12. [Azure OpenAI](#azure-openai)
13. [Amazon Bedrock](#amazon-bedrock)

## OpenAI

[OpenAI](https://openai.com) provides some of the most widely used LLMs, including the GPT and O-series families.

### Setup

1. Create an account at [OpenAI](https://platform.openai.com/)
2. Generate an API key in your account dashboard
3. In DevoxxGenie settings, select "OpenAI" as the provider
4. Paste your API key
5. Select your preferred model from the auto-populated list

### Configuration

- **API Key**: Your OpenAI API key
- **Model**: Select from available models (GPT-4o, GPT-4o mini, O-series reasoning models, etc.)
- **Parameters**: Temperature, Top P, Maximum tokens

### Advantages

- State-of-the-art models with excellent code understanding
- Multimodal capabilities (GPT-4V+)
- O-series models with advanced reasoning
- Reliable API with extensive documentation

*Check [OpenAI's pricing page](https://openai.com/pricing) for current rates. DevoxxGenie's built-in token cost calculator can estimate costs before sending prompts.*

## Anthropic

[Anthropic](https://anthropic.com/) provides the Claude family of models, known for their strong reasoning and large context windows.

### Setup

1. Create an account at [Anthropic](https://console.anthropic.com/)
2. Generate an API key in your account dashboard
3. In DevoxxGenie settings, select "Anthropic" as the provider
4. Paste your API key
5. Select your preferred Claude model

### Configuration

- **API Key**: Your Anthropic API key
- **Model**: Select from available Claude models (Claude 4 family, Claude 3.5 Sonnet, etc.)
- **Parameters**: Temperature, Top P, Maximum tokens

### Advantages

- Excellent reasoning abilities
- Very large context windows (up to 200K tokens)
- Strong performance on code tasks
- Clear, nuanced responses

*Check [Anthropic's pricing page](https://www.anthropic.com/pricing) for current rates.*

## Google

Google provides the Gemini family of models through Google AI Studio.

### Setup

1. Create an account at [Google AI Studio](https://aistudio.google.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "Google" as the provider
4. Paste your API key
5. Select your preferred Gemini model

### Configuration

- **API Key**: Your Google API key
- **Model**: Select from available Gemini models (Gemini 2.x, Gemini 1.5 Pro, Flash, etc.)
- **Parameters**: Temperature, Top P, Maximum output tokens

### Advantages

- Extremely large context window (up to 1M+ tokens)
- Strong multimodal capabilities
- Good performance on code tasks
- Integration with Google ecosystem

*Check [Google's pricing page](https://ai.google.dev/pricing) for current rates.*

## Grok

[Grok](https://x.ai/) is xAI's LLM, accessible via an OpenAI-compatible API.

### Setup

1. Create an account at [xAI](https://console.x.ai/)
2. Generate an API key
3. In DevoxxGenie settings, select "Grok" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

- **API Key**: Your xAI API key
- **Model**: Select from available Grok models
- **Parameters**: Temperature, Top P, Maximum tokens

### Advantages

- Strong reasoning and code capabilities
- OpenAI-compatible API (base URL: `https://api.x.ai/v1`)
- Good performance on technical tasks

*Check [xAI's website](https://x.ai/) for current pricing.*

## Mistral

[Mistral AI](https://mistral.ai/) offers efficient, powerful models with competitive performance.

### Setup

1. Create an account at [Mistral AI](https://console.mistral.ai/)
2. Generate an API key
3. In DevoxxGenie settings, select "Mistral" as the provider
4. Paste your API key
5. Select your preferred Mistral model

### Configuration

- **API Key**: Your Mistral API key
- **Model**: Select from available models (Mistral Large, Small, etc.)
- **Parameters**: Temperature, Top P

### Advantages

- Efficient models with competitive performance
- European-based company (GDPR compliance)
- Strong open-source foundation
- Good price-to-performance ratio

*Check [Mistral's pricing page](https://mistral.ai/pricing/) for current rates.*

## Groq

[Groq](https://groq.com/) is known for extremely fast inference speeds.

### Setup

1. Create an account at [Groq](https://console.groq.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "Groq" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

- **API Key**: Your Groq API key
- **Model**: Select from available models (Llama, Mixtral, Gemma, etc.)
- **Parameters**: Temperature, Top P

### Advantages

- Extremely fast inference speeds
- Competitive pricing
- Good selection of optimized open models
- Strong performance on code tasks

*Check [Groq's pricing page](https://groq.com/pricing) for current rates.*

## DeepInfra

[DeepInfra](https://deepinfra.com/) provides a platform for running various open-source models with optimized inference.

### Setup

1. Create an account at [DeepInfra](https://deepinfra.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "DeepInfra" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

- **API Key**: Your DeepInfra API key
- **Model**: Select from available models (Llama, Mistral, CodeLlama, and many more)
- **Parameters**: Temperature, Top P, Max tokens

### Advantages

- Access to many open-source models
- Competitive pricing
- Good selection of code-specialized models

*Check [DeepInfra's pricing page](https://deepinfra.com/pricing) for current rates.*

## DeepSeek

[DeepSeek](https://www.deepseek.com/) specializes in models with strong coding and reasoning capabilities.

### Setup

1. Create an account at DeepSeek
2. Generate an API key
3. In DevoxxGenie settings, select "DeepSeek" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

- **API Key**: Your DeepSeek API key
- **Model**: Select from available models (DeepSeek-Coder, DeepSeek-V2, DeepSeek-R1, etc.)
- **Parameters**: Temperature, Top P

### Advantages

- Excellent code generation capabilities
- Advanced reasoning with R1 model
- Strong understanding of programming concepts
- Competitive pricing

## Kimi

[Kimi](https://platform.moonshot.ai/) (Moonshot AI) provides powerful language models with long context windows and strong performance on code-related tasks.

### Setup

1. Create an account at [Moonshot AI](https://platform.moonshot.ai/)
2. Generate an API key from your account dashboard
3. In DevoxxGenie settings, select "Kimi" as the provider
4. Paste your API key
5. Select your preferred Kimi model

### Configuration

- **API Key**: Your Kimi API key
- **Model**: Select from available models (Moonshot v1 8K/32K/128K, Kimi K2 Turbo Preview, etc.)
- **Parameters**: Temperature (0.0-1.0), Top P, Maximum tokens

### Advantages

- Long context windows (up to 128K tokens, 256K for K2)
- Strong code understanding and generation
- Competitive pricing
- Fast response times
- Good support for Chinese and English

*Check [Moonshot AI's pricing page](https://platform.moonshot.ai/) for current rates.*

## GLM

[GLM](https://open.bigmodel.cn/) (Zhipu AI / Z.AI) provides the ChatGLM family of models, offering strong performance on code-related tasks with competitive pricing.

### Setup

1. Create an account at [Zhipu AI (Z.AI)](https://z.ai/)
2. Generate an API key from [your account dashboard](https://z.ai/manage-apikey/apikey-list)
3. In DevoxxGenie settings, select "GLM" as the provider
4. Paste your API key
5. Select your preferred GLM model

### Configuration

- **API Key**: Your GLM API key
- **Model**: Select from available models (GLM-4.7, GLM-4.7 Flash, GLM-4.5)
- **Parameters**: Temperature, Top P, Maximum tokens

### Available Models

| Model | Context Window | Input Cost | Output Cost |
|-------|---------------|------------|-------------|
| GLM-4.7 | 200K tokens | $0.60/1M tokens | $2.20/1M tokens |
| GLM-4.7 Flash | 200K tokens | $0.06/1M tokens | $0.40/1M tokens |
| GLM-4.5 | 128K tokens | $0.35/1M tokens | $1.55/1M tokens |

### Advantages

- Large context windows (up to 200K tokens)
- Very competitive pricing, especially the Flash variant
- Strong code understanding and generation
- OpenAI-compatible API for seamless integration
- Good support for Chinese and English

*Check [Zhipu AI's pricing page](https://open.bigmodel.cn/) for current rates.*

## OpenRouter

[OpenRouter](https://openrouter.ai/) is a unified API that provides access to many different models from various providers.

### Setup

1. Create an account at [OpenRouter](https://openrouter.ai/)
2. Generate an API key
3. In DevoxxGenie settings, select "OpenRouter" as the provider
4. Paste your API key
5. Select your preferred model from the extensive list

### Configuration

- **API Key**: Your OpenRouter API key
- **Model**: Select from available models (OpenAI, Anthropic, Meta, Mistral, and many more)
- **Parameters**: Temperature, Top P

### Advantages

- Single API for many different models
- Fallback options if a provider is unavailable
- Easy model comparisons
- Pay-as-you-go pricing

*Check [OpenRouter's pricing page](https://openrouter.ai/pricing) for current rates.*

## Azure OpenAI

[Azure OpenAI Service](https://azure.microsoft.com/en-us/products/ai-services/openai-service) provides OpenAI models integrated with Microsoft Azure.

:::note
Azure OpenAI is an **optional provider** that must be manually enabled in DevoxxGenie settings due to its more complex setup requirements.
:::

### Setup

1. Create an Azure account
2. Set up Azure OpenAI Service
3. Create a deployment and get your API details
4. In DevoxxGenie settings, select "Azure OpenAI" as the provider
5. Enter your API Key, Endpoint URL, Deployment name, and API version

### Configuration

- **API Key**: Your Azure OpenAI key
- **Endpoint**: Your Azure OpenAI endpoint
- **Deployment**: Your specific model deployment
- **API Version**: The Azure OpenAI API version
- **Parameters**: Temperature, Top P

### Advantages

- Enterprise compliance and security
- Service level agreements (SLAs)
- Regional availability options
- Integration with other Azure services

## Amazon Bedrock

[Amazon Bedrock](https://aws.amazon.com/bedrock) provides access to foundation models from various providers through AWS.

:::note
Amazon Bedrock is an **optional provider** that must be manually enabled in DevoxxGenie settings due to its more complex setup requirements.
:::

### Setup

1. Create an AWS account
2. Set up Amazon Bedrock and configure access permissions
3. In DevoxxGenie settings, select "Amazon Bedrock" as the provider
4. Enter your Access Key, Secret Key, and Region

### Configuration

- **AWS Credentials**: Your access and secret keys
- **Region**: AWS region for Bedrock (supports regional inference with us/eu/apac prefixes)
- **Model ID**: Specific model identifier
- **Parameters**: Temperature, Top P

### Advantages

- Enterprise-grade security and compliance
- Integration with AWS ecosystem
- Choice of multiple foundation models (Anthropic, Meta, Cohere, Amazon Titan, etc.)
- Regional inference for data residency requirements

## Choosing a Cloud Provider

When selecting a cloud provider, consider:

- **Complex reasoning**: Anthropic Claude, OpenAI O-series, DeepSeek R1
- **Code generation**: DeepSeek, OpenAI, Anthropic Claude
- **Speed priority**: Groq, Google Gemini Flash
- **Large context**: Google Gemini (1M+ tokens), Anthropic Claude (200K tokens)
- **Budget-friendly**: Groq, Mistral Small, DeepSeek, Kimi, GLM Flash
- **Enterprise**: Azure OpenAI, Amazon Bedrock
- **European data residency**: Mistral

## Best Practices

### API Key Security

- DevoxxGenie stores keys securely in IntelliJ's credential store
- Regularly rotate keys for better security

### Cost Management

- Monitor token usage through provider dashboards
- Use the built-in token cost calculator in DevoxxGenie
- Set usage limits on your provider accounts
- Consider using RAG to reduce context size

### Performance

- Choose the right model for your task
- Use streaming for better user experience
- Balance context window size with cost
