---
sidebar_position: 3
title: Cloud LLM Providers
description: Guide to all cloud-based LLM providers supported by DevoxxGenie, including setup instructions and configuration.
keywords: [devoxxgenie, openai, anthropic, google, gemini, grok, mistral, groq, deepseek, kimi, moonshot, glm, zhipu, cloudflare, ai gateway, cloud llm]
image: /img/devoxxgenie-social-card.jpg
---

# Cloud LLM Providers

DevoxxGenie supports a wide range of cloud-based LLM providers, giving you access to powerful state-of-the-art models without requiring high-end local hardware.

:::tip
For all cloud providers, available models are **automatically fetched** from the provider's API when you select the provider and enter your API key. You don't need to manually enter model names.
:::

## Supported Cloud Providers

In the plugin settings, direct providers are on the **Cloud Direct** tab and routers are on the **Routers** tab.

**Cloud Direct**

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
11. [NVIDIA](#nvidia)
12. [Azure OpenAI](#azure-openai)
13. [Amazon Bedrock](#amazon-bedrock)

**Routers** (see [Routers](#routers))

1. [OpenRouter](#openrouter)
2. [Requesty](#requesty)
3. [DemonRoute](#demonroute)
4. [Cloudflare AI Gateway](#cloudflare-ai-gateway)

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

## Routers

:::warning Routers forward your prompts and code to other providers
Anything you send, including project files added to context, may pass through the router and whichever provider it selects. Each has its own logging, retention and training policies, and a provider may be in another country. DevoxxGenie doesn't verify these services or their privacy claims, and listing one here isn't an endorsement. Check the router's terms before enabling it, and avoid sending confidential code or secrets unless your organisation allows it.
:::

OpenRouter, Requesty, DemonRoute and Cloudflare AI Gateway are configured on the **Routers** tab in the DevoxxGenie settings. Each router is disabled until you tick its checkbox and enter an API key.

## Requesty

[Requesty](https://requesty.ai/) is an OpenAI-compatible router that offers models from several vendors through one API key.

DevoxxGenie talks to `https://router.requesty.ai/v1` and builds the model list from the Requesty catalog (`/v1/models/managed` and `/v1/models`), keeping chat models only. Context window and prices come from that catalog.

### Setup

1. Create an account at [Requesty](https://app.requesty.ai/)
2. Generate an API key at [app.requesty.ai/api-keys](https://app.requesty.ai/api-keys)
3. In DevoxxGenie settings, open the **Routers** tab, enable "Requesty" and paste your API key
4. Select a model from the list

### Configuration

- **API Key**: Your Requesty API key
- **Model**: Model ids are either a Requesty managed policy such as `claude-sonnet-4-5`, or the `vendor/model` form such as `openai/gpt-4o-mini` or `anthropic/claude-sonnet-4-5`
- **Parameters**: Temperature, Top P

:::note EU region
Requesty also runs an EU endpoint at `https://router.eu.requesty.ai/v1`. DevoxxGenie currently uses the global endpoint; see the [Requesty docs](https://docs.requesty.ai) for region details.
:::

:::note Testing status
Requesty support was contributed by Thibault Jaigu (#1303). It hasn't been tested against the live service by the maintainers.
:::

*Check the [Requesty model list](https://requesty.ai/models) for current rates.*

## DemonRoute

[DemonRoute](https://demonroute.com) is an OpenAI-compatible router. DevoxxGenie talks to `https://api.demonroute.com/v1` and reads the model list from its `/models` endpoint.

### Setup

1. Create an account and an API key at [demonroute.com](https://demonroute.com)
2. In DevoxxGenie settings, open the **Routers** tab, enable "DemonRoute" and paste your API key
3. Select a model from the list

### Configuration

- **API Key**: Your DemonRoute API key
- **Model**: Select from the models the endpoint reports
- **Parameters**: Temperature, Top P, and the max output tokens from your chat model settings

The `/models` endpoint is assumed to return the usual OpenAI-style list without prices or context windows, so DevoxxGenie assumes a 128K context window and shows no cost estimate.

:::note Testing status
DemonRoute support was added for requests #1340 and #1378. It hasn't been tested against the live service.
:::

## NVIDIA

DevoxxGenie includes a dedicated **NVIDIA** provider for NIM-hosted models. It uses the OpenAI-compatible endpoint `https://integrate.api.nvidia.com/v1` for both streaming and non-streaming chat.

### Setup

1. Obtain an API key from [NVIDIA Build](https://build.nvidia.com/).
2. Open the main **Settings → Tools → DevoxxGenie** page and find the cloud-provider settings.
3. Enable **NVIDIA API Key**, enter the key, and Apply.
4. Select **NVIDIA** in the chat provider dropdown and choose a model.

The plugin probes NVIDIA's `/models` endpoint to populate the model selector. A successful catalogue is cached until **Refresh models**; if discovery fails or returns no models, it falls back to the plugin's curated model list. Model capabilities and access depend on the selected model and your provider account.

You can [favorite a model](../features/chat-interface.md#favorite-models) for quick access across providers. [Show Thinking](../features/chat-interface.md#show-thinking) only displays reasoning when the model/provider returns it.

## Cloudflare AI Gateway

[Cloudflare AI Gateway](https://www.cloudflare.com/products/ai-gateway/) is not a model provider itself — it sits **in front of** your existing providers (OpenAI, Anthropic, Google, Workers AI, and [dozens more](https://developers.cloudflare.com/ai-gateway/)) and gives you a single endpoint with caching, rate limiting, spend controls, and per-request analytics.

DevoxxGenie talks to the gateway's OpenAI-compatible `/compat` endpoint, so every model you've wired up behind the gateway becomes available in the plugin.

### Setup

1. Create (or reuse) a gateway in the [Cloudflare AI Gateway dashboard](https://developers.cloudflare.com/ai-gateway/get-started/). A gateway named `default` is created automatically on first authenticated request.
2. Store the provider API keys you want to use in the Cloudflare dashboard (**BYOK** — Bring Your Own Keys).
3. Generate a Cloudflare API token at [dash.cloudflare.com/profile/api-tokens](https://dash.cloudflare.com/profile/api-tokens).
4. In DevoxxGenie settings, select "Cloudflare" as the provider.
5. Enter your API Key, Account ID, and Gateway Name.
6. Select your preferred model from the auto-populated list.

### Configuration

- **Cloudflare API Key**: Your Cloudflare API token, sent as `Authorization: Bearer`
- **Cloudflare Account ID**: Your account identifier
- **Cloudflare Gateway Name**: `default`, or the name of a gateway you created
- **Model**: Select from the auto-populated list, or enable the model name override to type a model id directly
- **Parameters**: Temperature, Top P, Maximum tokens

The base URL is assembled for you from the account id and gateway name — you never type it:

```
https://gateway.ai.cloudflare.com/v1/<account-id>/<gateway>/compat
```

:::note Your provider keys stay in Cloudflare
This provider uses Cloudflare's **single-token (BYOK)** model. You give DevoxxGenie only your Cloudflare API token; the downstream provider keys (OpenAI, Anthropic, ...) live in your Cloudflare dashboard, where the gateway injects them. You never paste an OpenAI or Anthropic key into DevoxxGenie for this provider.
:::

:::tip Model name override
Model discovery uses a fast best-effort probe of the gateway's `/models` endpoint. If your gateway doesn't expose that endpoint, or you already know the exact model id you want, enable the **model name override** and type it in — this skips discovery entirely.
:::

### Model naming

The `/compat` endpoint addresses models as `provider/model` — the provider prefix tells the gateway where to route the request:

```
openai/gpt-4o
anthropic/claude-4-5-sonnet
workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast
```

A bare model id (e.g. `kimi-k2.6`) cannot be routed and fails with `400 AiGatewayError` code 2005/2008. **Workers AI** ids copied from the Cloudflare dashboard start with `@cf/` — DevoxxGenie adds the missing `workers-ai/` prefix for those automatically, so `@cf/openai/gpt-oss-20b` is sent as `workers-ai/@cf/openai/gpt-oss-20b`. For every other provider, include the prefix yourself when using the model name override.

### Advantages

- **One key, every provider** — no per-provider setup inside the plugin
- **Caching** of identical requests, so repeated prompts don't cost you twice
- **Rate limiting and spend controls** you own, rather than the provider's
- **Analytics and logs** for every request (latency, tokens, cost) in the Cloudflare dashboard
- **Centralized key management** — provider keys live in Cloudflare, not scattered across tools

:::note Context window and cost estimates
Cloudflare's `/compat/models` endpoint doesn't report context length or pricing, so DevoxxGenie assumes a 128K context window and shows no cost estimate for gateway models. Use the Cloudflare dashboard for authoritative per-request cost data.
:::

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
3. On the main **Settings → Tools → DevoxxGenie** page, enable **AWS Bedrock**.
4. Choose **Authentication Method** and enter the matching credentials below, plus the **AWS region**.
5. Apply, then select **Bedrock** and a model in the chat window.

### Configuration

- **Authentication Method**:
  - **Access Key / Secret Key** (default): enter **AWS Access Key ID** and **AWS Secret Access Key**.
  - **AWS Profile**: enter **AWS Profile Name**; the SDK resolves credentials for that named profile.
  - **Bearer Token**: enter **AWS Bearer Token**. This mode uses token authentication rather than the access-key credentials provider.
- Only fields for the selected authentication method are shown. Changing the mode does not turn a bearer token into an access key; populate the matching field.
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
- **Many providers behind one key, with caching and analytics**: Cloudflare AI Gateway

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
