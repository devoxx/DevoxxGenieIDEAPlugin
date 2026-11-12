---
sidebar_position: 4
sidebar_label: Routers
title: LLM Routers
description: Use LLM routers such as OpenRouter, OrcaRouter, UberLLM, Requesty, DemonRoute and Cloudflare AI Gateway with DevoxxGenie to reach models from many vendors through one API key.
keywords: [devoxxgenie, llm router, openrouter, orcarouter, uberllm, requesty, demonroute, cloudflare, ai gateway, openai-compatible, intellij plugin]
image: /img/devoxxgenie-social-card.jpg
---

# LLM Routers

Routers are OpenAI-compatible gateways that reach models from many vendors through one API key. Your prompts pass through the router on their way to another provider. They're configured on the **Routers** tab of the DevoxxGenie settings (**Settings > Tools > DevoxxGenie**).

:::warning Routers forward your prompts and code to other providers
Anything you send, including project files added to context, may pass through the router and whichever provider it selects. Each has its own logging, retention and training policies, and a provider may be in another country. DevoxxGenie doesn't verify these services or their privacy claims, and listing one here isn't an endorsement. Check the router's terms before enabling it, and avoid sending confidential code or secrets unless your organisation allows it.
:::

Supported routers:

1. [OpenRouter](#openrouter)
2. [OrcaRouter](#orcarouter)
3. [UberLLM](#uberllm)
4. [Requesty](#requesty)
5. [DemonRoute](#demonroute)
6. [Cloudflare AI Gateway](#cloudflare-ai-gateway)

## How the Routers tab works

Each router is disabled until you tick its checkbox and enter an API key.

![The Routers tab, with its privacy warning and a checkbox per router](/img/settings-routers.png)

Each router row works the same way:

- **Checkbox**: decides whether the router appears in the chat's provider list. New routers are off by default.
- **API Key**: the key button next to the field opens the router's API key page. A key is only saved on **Apply** when you changed it.
- **URL**: the base URL for the model catalog and chat requests, pre-filled with the router's default. Change it to use an alternative endpoint, such as Requesty's EU router. Leave it blank to go back to the default. Only a changed URL is stored, so a router left on the default follows later changes to it. Changing the URL refreshes that router's model list.

A disabled router, or one without a key, sends no requests. If fetching a router's model catalog fails, DevoxxGenie waits 60 seconds before trying again. DevoxxGenie sends no affiliate, referral or attribution data to any router.

Cloudflare AI Gateway has its own fields (API token, account ID and gateway name) instead of a URL; see [Cloudflare AI Gateway](#cloudflare-ai-gateway).

## OpenRouter

[OpenRouter](https://openrouter.ai/) is a unified API that provides access to many different models from various providers.

### Setup

1. Create an account at [OpenRouter](https://openrouter.ai/)
2. Generate an API key
3. In DevoxxGenie settings, open the **Routers** tab, enable "OpenRouter" and paste your API key
4. Select your preferred model from the extensive list

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

## OrcaRouter

[OrcaRouter](https://www.orcarouter.ai/) is an OpenAI-compatible router that offers models from several vendors through one API key.

DevoxxGenie talks to `https://api.orcarouter.ai/v1` and builds the model list from its `/models` endpoint, keeping chat models and OrcaRouter's routing aliases such as `orcarouter/auto`. Image, video, embedding and text-to-speech models are left out. Context window and prices come from that catalog. A model that doesn't list a context window is assumed to have 128K, and one without prices shows no price.

### Setup

1. Create an account at [OrcaRouter](https://www.orcarouter.ai/)
2. Generate an API key on the [API Keys page](https://www.orcarouter.ai/console/token) of the console. Keys start with `sk-orca-`
3. In DevoxxGenie settings, open the **Routers** tab, enable "OrcaRouter" and paste your API key
4. Select a model from the list

### Configuration

- **API Key**: Your OrcaRouter API key
- **Model**: Model ids use the `vendor/model` form, such as `anthropic/claude-fable-5`, or a routing alias such as `orcarouter/auto`
- **Parameters**: Temperature, Top P, and the max output tokens from your chat model settings

:::note Testing status
OrcaRouter support was added for request #1291. The model list was checked against the live catalog, but chat requests haven't been tested against the live service.
:::

*Check the [OrcaRouter model list](https://www.orcarouter.ai/models) for current rates.*

## UberLLM

[UberLLM](https://uberllm.dev) is an OpenAI-compatible router that offers models from several vendors through one API key.

DevoxxGenie talks to `https://api.uberllm.dev/v1` and builds the model list from its `/models` endpoint, keeping models that return text. Context window and prices come from that catalog. A model that doesn't list a context window is assumed to have 128K.

### Setup

1. Create an account and an API key at [uberllm.dev/signup](https://uberllm.dev/signup). Credit is prepaid, with no monthly fee
2. In DevoxxGenie settings, open the **Routers** tab, enable "UberLLM" and paste your API key
3. Select a model from the list

### Configuration

- **API Key**: Your UberLLM API key
- **Model**: Model ids use the `vendor/model` form, such as `deepseek/deepseek-v3.2` or `anthropic/claude-sonnet-4.5`
- **Parameters**: Temperature, Top P, and the max output tokens from your chat model settings

*Check the [UberLLM model list](https://uberllm.dev/models) for current rates.*

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
DevoxxGenie uses the global endpoint by default. To route through Requesty's EU endpoint, set the **Requesty URL** on the Routers tab to `https://router.eu.requesty.ai/v1`. See the [Requesty docs](https://docs.requesty.ai) for region details.
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

The `/models` endpoint is assumed to return the usual OpenAI-style list without prices or context windows, so DevoxxGenie assumes a 128K context window and treats the input cost as $0. The model dropdown therefore shows no price, and the project scan cost estimate reports $0.00000.

:::note Testing status
DemonRoute support was added for requests #1340 and #1378. It hasn't been tested against the live service.
:::

## Cloudflare AI Gateway

[Cloudflare AI Gateway](https://www.cloudflare.com/products/ai-gateway/) is not a model provider itself — it sits **in front of** your existing providers (OpenAI, Anthropic, Google, Workers AI, and [dozens more](https://developers.cloudflare.com/ai-gateway/)) and gives you a single endpoint with caching, rate limiting, spend controls, and per-request analytics.

DevoxxGenie talks to the gateway's OpenAI-compatible `/compat` endpoint, so every model you've wired up behind the gateway becomes available in the plugin.

### Setup

1. Create (or reuse) a gateway in the [Cloudflare AI Gateway dashboard](https://developers.cloudflare.com/ai-gateway/get-started/). A gateway named `default` is created automatically on first authenticated request.
2. Store the provider API keys you want to use in the Cloudflare dashboard (**BYOK** — Bring Your Own Keys).
3. Generate a Cloudflare API token at [dash.cloudflare.com/profile/api-tokens](https://dash.cloudflare.com/profile/api-tokens).
4. In DevoxxGenie settings, open the **Routers** tab and enable "Cloudflare".
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
