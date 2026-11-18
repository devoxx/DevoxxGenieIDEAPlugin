---
sidebar_position: 5
title: Web Search Integration - DevoxxGenie for IntelliJ IDEA
description: Ground LLM answers in live web results in DevoxxGenie. Choose a web search provider (Tavily, Keenable or Google Custom Search) for /search and the agent's web_search tool.
keywords: [devoxxgenie, web search, web search provider, tavily, google custom search, keenable, parallel, /search, agent mode, web_search tool]
image: /img/devoxxgenie-social-card.jpg
---

# Web Search

DevoxxGenie can ground answers in live web results, so the LLM can use information that is newer than its training data.

## Overview

Web search:

1. Sends your search query to the web search provider you selected
2. Retrieves the most relevant results (title, URL and snippet)
3. Hands those results to the LLM, which answers from them
4. Helps with anything that changed after the model's knowledge cutoff

It is particularly useful for:
- Technical questions about recent framework versions
- Documentation lookups for APIs and libraries
- Current best practices and patterns
- Specific error messages

There are two ways to use it:

| Where | How |
|-------|-----|
| **`/search` command** | Type `/search <question>` in the prompt. DevoxxGenie searches, then answers from the results. |
| **Agent Mode `web_search` tool** | The agent decides by itself when to search, and gets the raw results (title, URL, snippet) to reason over. |

Both use **the same provider and the same credentials**, chosen once in **Settings → DevoxxGenie → Web search**.

## Choosing a Provider

Web search uses **one provider at a time**. Pick it in the **Provider** dropdown:

| Provider | What you need | Notes |
|----------|---------------|-------|
| **Tavily** | A Tavily API key | Search API built for LLMs. |
| **Keenable** | Nothing (an API key is optional) | Works without signing up; free searches are rate-limited per network. Add a key for higher limits. Available after v1.18.0. |
| **Google Custom Search** | A Google API key **and** a Programmable Search Engine ID | See the [note on Google](#a-note-on-google-custom-search) below. |
| **None** | — | Web search is off for `/search` and the agent tool. |

New to web search? **Keenable** needs no account; **Tavily** needs a free API key.

Only the selected provider's fields are shown. Keys you entered for other providers are kept, so you can switch back without typing them again. Keys are stored in IntelliJ's credential store (PasswordSafe), not in the settings XML.

:::info Upgrading from an earlier version
Earlier versions had a separate checkbox per provider. When you upgrade, DevoxxGenie keeps your choice: if Tavily was enabled it is selected, otherwise Google if it was enabled, otherwise **None**. Your keys are not touched.
:::

### More providers coming soon

The provider list is pluggable, and more providers are in the works. They are **not available yet**; this page will be updated when they ship.

| Provider | Status | What it will offer |
|----------|--------|--------------------|
| **Parallel** | Under discussion ([#1423](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues/1423)) | Web search via Parallel's search service |

Until then, you can already use any search service that offers an MCP server in Agent Mode, by adding it in **Settings → DevoxxGenie → MCP**. It then works as an agent tool, but not for `/search`.

### A note on Google Custom Search

Google has restricted access to its Custom Search JSON API, and new projects may no longer be able to get it. Check [Google's Custom Search JSON API overview](https://developers.google.com/custom-search/v1/overview) for the current status and any end-of-service date. If you are setting up web search for the first time, choose **Tavily** or **Keenable**. If you rely on Google today, plan to switch.

## Setting Up Web Search

1. Open **Settings → DevoxxGenie → Web search**
2. Check **Enable feature**
3. Choose a **Provider**
4. Enter that provider's credentials (see below)
5. Optionally set **Max search results** (1–10, default 5)
6. Type a query under **Test web search** and click **Test search** to check the setup. The test uses what is in the form, so you can try a key **before** you click Apply.
7. Click **Apply**

![Web Search Settings](/img/web-search-settings.png)

### Tavily Setup

1. Sign up at [app.tavily.com](https://app.tavily.com/home) and copy your API key
2. Select **Tavily** as the provider
3. Paste the key into **Tavily API key**

### Keenable Setup

Keenable works with no setup at all:

1. Select **Keenable** as the provider
2. Click **Test search** to check it, then **Apply**

Without a key, DevoxxGenie uses Keenable's free public search. It is rate-limited per network, so on a shared office or VPN connection you may hit the limit sooner. When that happens you'll see:

> Keenable's free search limit for your network was reached. Try again later, or add a Keenable API key in Settings → DevoxxGenie → Web search for a higher limit.

To raise the limit, get an API key from [keenable.ai](https://keenable.ai) and paste it into **Keenable API key (optional)**. With a key, searches go to Keenable's authenticated endpoint. If the key is wrong, DevoxxGenie tells you it was rejected; clear the field to go back to free keyless search.

Requests identify themselves as DevoxxGenie. The API key is only ever sent to Keenable, never to another site after a redirect.

### Google Custom Search Setup

1. Create a [Google Cloud project](https://console.cloud.google.com/) and enable the Custom Search API
2. Create an API key
3. Create a [Programmable Search Engine](https://programmablesearchengine.google.com/controlpanel/create) and copy its Search Engine ID
4. Select **Google Custom Search** as the provider
5. Enter both the **API key** and the **Search Engine ID**

### What happens when something is missing

DevoxxGenie checks the selected provider before it sends anything. If something is missing, no request is made and you get a message that says what to fix, for example:

> Tavily is missing its API key. Enter it in Settings → DevoxxGenie → Web search.

> Google Custom Search is missing its Search Engine ID. Enter it in Settings → DevoxxGenie → Web search.

> No web search provider is selected. Choose one in Settings → DevoxxGenie → Web search.

The same check applies to `/search`, the agent's `web_search` tool and **Test search**.

## Using Web Search

### The `/search` command

`/search` is available when **Enable feature** is on and a provider is ready:

```
/search How to implement JWT authentication in Spring Boot 3.2?
```

DevoxxGenie sends the text after `/search` to your provider, then asks the LLM to answer from the results.

### Sources

Every `/search` answer ends with a **Sources** list: the title and original URL of each result the answer was based on, as clickable links. Links open in the browser configured in your IDE.

```
Use since-build in plugin.xml to declare the earliest compatible IDE build…

Sources
1. Build number ranges  →  https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
2. plugins.jetbrains.com  →  https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
```

When a result has no title, the site's host name is shown instead. Only `http` and `https` results are listed.

If the provider finds nothing, you get a clear message instead of an answer, and no LLM call is made:

> No web results found for "…". Try different keywords, or check the provider in Settings → DevoxxGenie → Web search.

## Use Cases

### Framework and Library Documentation

```
/search What's the correct way to configure WebClient in Spring Boot 3.2?
```

### Error Resolution

```
/search Hibernate HHH90000022 legacy SequenceStyleGenerator warning: how do I fix it?
```

### Best Practices and Patterns

```
/search Current best practice for handling JWT authentication in React applications
```

### Version-Specific Questions

```
/search New features in Java 21 compared to Java 17
```

## Agent Mode: `web_search` Tool

DevoxxGenie also exposes web search as an **agent tool** when Agent Mode is active. The LLM decides by itself when a live web search is needed.

### Enabling the tool

1. Open **Settings → DevoxxGenie → Agent Mode**
2. Under **Built-in Tools**, check **web_search**
3. Select a provider and enter its credentials in **Settings → DevoxxGenie → Web search**. The agent tool uses the same provider and keys as `/search`.

If the selected provider isn't ready, the Agent Mode settings show a warning with a link to the Web search settings. At run time the tool returns a descriptive error string, such as which key is missing, and the agent carries on with other tools. There are no modal dialogs and the agent loop doesn't break.

### How it works

When the agent decides a query needs external information, it calls `web_search` directly:

```
Found 3 results for "langchain4j ChromaDB 0.6 migration":

1. LangChain4j 0.37 release notes
   URL: https://github.com/langchain4j/langchain4j/releases/tag/0.37.0
   LangChain4j 0.37 updates the ChromaDB store to API v2 (0.6.x)...

2. ...
```

The tool returns raw structured results (title, URL and snippet), so the agent can reason over sources without an extra summarisation step. Only the selected provider is ever contacted. Keys stored for other providers are not used.

When a result looks promising, the agent can follow up with the `fetch_page` tool, which fetches the full content of a page given its URL. That makes `web_search` → `fetch_page` a natural two-step pattern for reading external documentation.

Like `semantic_search`, the `web_search` tool is classified as **read-only**, so it is auto-approved when the "Auto-approve read-only tools" setting is on.

### The three-tier retrieval stack

With Agent Mode, web search becomes one layer of a complete retrieval hierarchy:

| Tool | Best for |
|------|----------|
| `semantic_search` | Conceptual queries over your indexed codebase |
| `search_files` | Exact-string or regex lookups in project files |
| `web_search` | Documentation, release notes, external references |

The LLM picks the right tier based on the query. You can enable or disable each tool independently under **Settings → Agent Mode → Built-in Tools**.

## Web Search and RAG

Web search complements the RAG feature:

- **RAG** (`semantic_search` tool): retrieves information from your indexed project codebase
- **Web search**: retrieves up-to-date information from the internet

To use both:

1. Enable RAG, web search and Agent Mode for the full tool stack
2. Ask questions that relate to both your code and external information
3. DevoxxGenie combines insights from both sources

## Best Practices

### Writing Effective Search Queries

1. **Be specific**: include version numbers, framework names and specific technologies
2. **Use technical terminology** rather than general descriptions
3. **Ask one question at a time**: multi-part queries tend to return less relevant results
4. **Mention recency** when it matters (e.g. "latest approach")

### When to Use Web Search

Web search is most valuable when:

1. You need information about recent software versions
2. You're looking for current best practices
3. You need help with specific error messages
4. The information might have changed since the LLM's training cutoff

It is less useful for:

1. Conceptual computer science questions
2. Questions about your own codebase (use RAG instead)
3. General programming patterns that haven't changed recently

## Privacy Considerations

1. **Search queries**: the text you search for is sent to the selected provider, and only to that provider
2. **API keys**: stored in IntelliJ's credential store (PasswordSafe)
3. **Data usage**: check the selected provider's privacy policy for how it handles queries
4. **Code privacy**: avoid putting sensitive code in search queries

## Troubleshooting

### Common Issues

#### `/search` says web search isn't configured

1. Check that **Enable feature** is on in **Settings → Web search**
2. Check that a **Provider** is selected (not **None**)
3. Read the message: it names the missing key or ID
4. Use **Test search** to confirm the setup before applying

#### No Search Results

If `/search` answers *No web results found*:

1. Verify the key with **Test search**
2. Make sure you haven't exceeded the provider's rate limits
3. Make your query more specific or use different keywords

#### Poor Search Results

1. Make your query more specific
2. Include version numbers and exact error messages
3. Try the other provider
4. Break complex questions into simpler ones

#### API Key Issues

1. Re-enter the key in **Settings → Web search** and run **Test search**
2. Check that the key is active in your provider account (Tavily or Google Cloud)
3. Ensure billing is set up if your provider requires it
4. Check for restrictions on the key (IP, usage limits)

## Advanced Configuration

### Search Settings

The settings page exposes **Enable feature**, the **Provider** choice, the selected provider's credentials, and **Max search results** (1–10). It does not expose search-type, locale or Safe Search controls.

### Custom Prompting

You can steer how results are used by being explicit in your prompt:

```
/search Spring Boot 3.2 security configuration changes versus Spring Boot 2.x, focusing on OAuth2 resource server setup
```

## Future Enhancements

Planned improvements to web search:

1. **More search providers**: Parallel (see [More providers coming soon](#more-providers-coming-soon))
2. **Context-aware searching**: automatically incorporating project context into searches
3. **Search result caching**: improving performance by caching common searches
