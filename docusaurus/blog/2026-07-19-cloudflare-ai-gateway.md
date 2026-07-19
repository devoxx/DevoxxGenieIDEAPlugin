---
slug: cloudflare-ai-gateway
title: "One Key, Every Provider: Cloudflare AI Gateway Comes to DevoxxGenie"
authors: [stephanj]
tags: [cloudflare, ai gateway, llm, cloud providers, byok, openai compatible, intellij idea, open source]
date: 2026-07-19
description: DevoxxGenie now speaks to Cloudflare AI Gateway as a first-class provider. One Cloudflare token, your account ID, and a gateway name unlock every model you've wired up behind the gateway - with auto-discovery, a manual override, and clear error messages when something's off.
keywords: [devoxxgenie, cloudflare, cloudflare ai gateway, byok, openai compatible, compat endpoint, workers ai, intellij plugin, code assistant, llm gateway]
image: /img/cloudflare-chat.png
---

# One Key, Every Provider: Cloudflare AI Gateway Comes to DevoxxGenie

[Cloudflare AI Gateway](https://www.cloudflare.com/products/ai-gateway/) sits in front of your LLM providers and gives you one place to route, cache, rate-limit, and observe every request - across OpenAI, Anthropic, Google, Workers AI, and more. Starting with the next DevoxxGenie release, you can point the plugin straight at your gateway and use **any** model behind it, without leaving your IDE.

No per-provider setup inside the plugin. One Cloudflare token, your account ID, a gateway name - and you're talking to whatever you've configured behind the gateway.

<!-- truncate -->

![A working Cloudflare AI Gateway conversation in DevoxxGenie](/img/cloudflare-chat.png)

## Why route through a gateway?

Talking to providers directly is fine until you want to *see* what's happening. Cloudflare AI Gateway adds a control plane on top of the models you already use:

- **One endpoint for many providers** - OpenAI, Anthropic, Google, Groq, Workers AI, and [dozens more](https://developers.cloudflare.com/ai-gateway/), all reachable through a single OpenAI-compatible URL.
- **Caching** of identical requests, so repeated prompts don't cost you twice.
- **Rate limiting** and spend controls you own, not the provider's.
- **Analytics and logs** for every request - latency, tokens, cost - in the Cloudflare dashboard.
- **Bring Your Own Keys (BYOK)** - your provider keys live in Cloudflare, not scattered across tools.

DevoxxGenie now plugs into all of that as a first-class provider.

## How authentication works

DevoxxGenie uses Cloudflare's **single-token (BYOK)** model. You give the plugin **one** thing - your Cloudflare API token - and it's sent as `Authorization: Bearer`. The downstream provider keys (your OpenAI key, Anthropic key, and so on) stay **stored in your Cloudflare dashboard**, where the gateway injects them for you.

That means you never paste an OpenAI or Anthropic key into DevoxxGenie for this provider - Cloudflare holds them. One token in the plugin, every provider behind the gateway.

## Getting started takes under 2 minutes

![Configuring the Cloudflare provider in DevoxxGenie settings](/img/cloudflare-settings.png)

1. Create (or reuse) a gateway in the [Cloudflare AI Gateway dashboard](https://developers.cloudflare.com/ai-gateway/get-started/). A gateway named `default` is created automatically on first use.
2. In the Cloudflare dashboard, store the provider API keys you want to use (BYOK).
3. Generate a Cloudflare API token at [dash.cloudflare.com/profile/api-tokens](https://dash.cloudflare.com/profile/api-tokens).
4. In DevoxxGenie, open **Settings → LLM Providers**, enable **Cloudflare**, and fill in:
   - **Cloudflare API Key** - your Cloudflare API token
   - **Cloudflare Account ID** - your account identifier
   - **Cloudflare Gateway Name** - `default`, or the name of a gateway you created
5. Pick a model from the dropdown and start chatting.

Under the hood DevoxxGenie assembles the OpenAI-compatible base URL for you:

```
https://gateway.ai.cloudflare.com/v1/<account-id>/<gateway>/compat
```

You never type that URL - the three fields build it. The plugin then reuses the same battle-tested OpenAI-compatible client path as our other cloud providers.

## Models are auto-discovered

DevoxxGenie asks your gateway's `/compat/models` endpoint what's available and populates the dropdown for you - so you always pick from models that actually exist on **your** gateway. Cloudflare uses a `provider/model` naming scheme, so you'll see IDs like:

- `openai/gpt-4o-mini`
- `anthropic/claude-4-5-sonnet`
- `workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast`
- `moonshot/moonshotai/kimi-k2.6`

Want to pin one exact model instead? Enable the **Cloudflare Model** override and type the exact `provider/model` name - the plugin then uses it verbatim and skips discovery. Handy for scripted setups or when you only ever use one model.

## Clear errors when something's off

Gateways can be picky - a model that isn't configured, a provider key you haven't stored yet. Instead of dumping Cloudflare's raw JSON error at you, DevoxxGenie translates it into something you can act on:

> Cloudflare AI Gateway couldn't run model `azure-openai/kimi-k2.6` (error 2005: Failed to get response from provider). This usually means the model isn't available on your gateway, or its provider isn't configured. Pick a model from the dropdown (auto-discovered from your gateway), or add that provider's API key in your Cloudflare AI Gateway dashboard.

No stack-trace archaeology - just what went wrong and what to do next.

## A quick tip on Workers AI

If you want to try the gateway *before* wiring up any BYOK keys, reach for a **Workers AI** model like `workers-ai/@cf/meta/llama-3.3-70b-instruct-fp8-fast`. Those are Cloudflare's own hosted models and bill to your account directly - a great way to confirm everything is connected end-to-end.

## Learn more

- [Cloudflare AI Gateway - product overview](https://www.cloudflare.com/products/ai-gateway/)
- [AI Gateway - get started](https://developers.cloudflare.com/ai-gateway/get-started/)
- [OpenAI-compatible (`/compat`) chat completions](https://developers.cloudflare.com/ai-gateway/usage/chat-completion/)

One token, your whole gateway, right inside IntelliJ IDEA. Enjoy! ⚡
