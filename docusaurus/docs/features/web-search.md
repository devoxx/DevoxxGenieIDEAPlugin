---
sidebar_position: 5
title: Web Search Integration - DevoxxGenie for IntelliJ IDEA
description: Augment LLM responses with real-time web search results using Google Custom Search or Tavily integration in DevoxxGenie.
keywords: [devoxxgenie, web search, google custom search, tavily, search augmented, real-time search, llm web access, agent mode, web_search tool]
image: /img/devoxxgenie-social-card.jpg
---

# Web Search

DevoxxGenie includes web search integration that allows LLMs to access up-to-date information from the internet to provide more accurate and informed responses.

## Overview

The web search feature:

1. Performs real-time internet searches based on your queries
2. Retrieves relevant information from search results
3. Incorporates this information into the LLM's response
4. Helps overcome the LLM's knowledge cutoff limitations

This feature is particularly valuable for:
- Technical questions about recent framework versions
- Documentation lookups for APIs and libraries
- Current best practices and patterns
- Bug fixes for specific error messages

## Supported Search Providers

DevoxxGenie supports two web search providers:

### Google Custom Search

- Uses Google's Custom Search API
- Provides comprehensive web search results
- Requires a Google API key and Custom Search Engine ID
- May have usage limits based on your API plan

### Tavily

- AI-native search API designed for LLM integration
- Specialized for developer-focused searches
- Requires a Tavily API key
- Offers various search types (standard, comprehensive, code)

## Enabling Web Search

### Configuration

To set up web search:

1. Open DevoxxGenie settings
2. Navigate to "Web Search"
3. Enable web search
4. Configure your preferred search provider

![Web Search Settings](/img/web-search-settings.png)

#### Google Custom Search Setup

1. Create a [Google Cloud Project](https://console.cloud.google.com/)
2. Enable the Custom Search API
3. Create API credentials and get your API key
4. Create a [Custom Search Engine](https://programmablesearchengine.google.com/about/)
5. Get your Search Engine ID
6. Enter both in DevoxxGenie settings

#### Tavily Setup

1. Sign up for a [Tavily API key](https://tavily.com/)
2. Enter your API key in DevoxxGenie settings
3. Optionally configure search parameters

### Enabling for Individual Conversations

Once configured, you can toggle web search on or off for each conversation:

1. Click the web search toggle button in the DevoxxGenie toolbar
2. When enabled, a globe icon will be highlighted

## Using Web Search

### Basic Usage

To use web search:

1. Ensure web search is enabled (toggle is active)
2. Ask a question that might benefit from current information
3. DevoxxGenie will automatically perform a web search if relevant
4. Search results will be incorporated into the response

### Explicit Web Search Commands

You can explicitly request web search using commands:

```
/search How to implement JWT authentication in Spring Boot 3.2?
```

Or incorporate it into your regular prompts:

```
What are the latest best practices for Spring Boot 3.2 authentication? Please search the web for current information.
```

### Understanding Search Results

When web search is used, the response will typically:

1. Indicate that information was retrieved from web search
2. Provide attribution for sources used
3. Include timestamps or version information to indicate recency
4. Synthesize information from multiple sources when appropriate

## Use Cases

### Framework and Library Documentation

Get up-to-date information about libraries:

```
What's the correct way to configure WebClient in Spring Boot 3.2?
```

### Error Resolution

Find solutions for specific error messages:

```
I'm getting this error with Hibernate: "HHH90000022: Hibernate's legacy org.hibernate.id.enhanced.SequenceStyleGenerator sequence-based generator is used, but the database does not support sequences" How do I fix it?
```

### Best Practices and Patterns

Learn current recommended approaches:

```
What's the current best practice for handling authentication in React applications with JWT?
```

### Version-Specific Questions

Get information about specific software versions:

```
What are the new features in Java 21 compared to Java 17?
```

## Agent Mode: `web_search` Tool

In addition to the chat-mode toggle, DevoxxGenie exposes web search as a first-class **agent tool** when Agent Mode is active. This lets the LLM decide autonomously when a live web search is needed — no manual toggle required.

### Enabling the tool

1. Open **Settings → DevoxxGenie → Agent Mode**
2. Under **Built-in Tools**, check **web_search**
3. Make sure at least one web search provider is configured (Tavily or Google Custom Search) in **Settings → DevoxxGenie → Web Search** — the agent tool reuses those keys

If neither key is configured, the tool returns a descriptive error string and the agent falls back to other tools. No modal dialogs or broken agent loops.

### How it works

When the agent determines a query needs external information, it calls `web_search` directly:

```
Found 3 results for "langchain4j ChromaDB 0.6 migration":

1. LangChain4j 0.37 release notes
   URL: https://github.com/langchain4j/langchain4j/releases/tag/0.37.0
   LangChain4j 0.37 updates the ChromaDB store to API v2 (0.6.x)...

2. ...
```

The tool returns raw structured results — title, URL, and snippet — so the agent can reason over sources without an extra summarisation step. Provider selection is automatic: Tavily is preferred when its key is present; Google Custom Search is the fallback.

When a search result looks promising, the agent can follow up with the `fetch_page` tool, which fetches the full content of a webpage given a URL — making `web_search` → `fetch_page` a natural two-step pattern for retrieving and reading external documentation.

Like `semantic_search`, the `web_search` tool is classified as **read-only** and is therefore auto-approved when the "Auto-approve read-only tools" setting is on.

### The three-tier retrieval stack

With Agent Mode, web search becomes one layer of a complete retrieval hierarchy:

| Tool | Best for |
|------|----------|
| `semantic_search` | Conceptual queries over your indexed codebase |
| `search_files` | Exact-string or regex lookups in project files |
| `web_search` | Documentation, release notes, external references |

The LLM selects the right tier automatically based on the query. You can enable or disable each tool independently under **Settings → Agent Mode → Built-in Tools**.

## Web Search and RAG

Web search complements the RAG feature:

- **RAG** (`semantic_search` tool): Retrieves information from your indexed project codebase
- **Web Search**: Retrieves up-to-date information from the internet

You can use both together for the most comprehensive context:

1. Enable both RAG and web search (and Agent Mode for the full tool stack)
2. Ask questions that relate to both your code and external information
3. DevoxxGenie will combine insights from both sources

## Best Practices

### Writing Effective Search Queries

For the best web search results:

1. **Be specific**: Include version numbers, framework names, and specific technologies
2. **Use technical terminology**: Include proper technical terms rather than general descriptions
3. **Focus on one question at a time**: Complex multi-part queries may yield less relevant search results
4. **Mention recency**: If you specifically need recent information, mention it (e.g., "latest approach")

### When to Use Web Search

Web search is most valuable when:

1. You need information about recent software versions
2. You're looking for current best practices
3. You need help with specific error messages
4. The information might have changed since the LLM's training cutoff

It may be less useful for:

1. Conceptual computer science questions
2. Questions about your specific codebase (use RAG instead)
3. General programming patterns that haven't changed recently

## Privacy Considerations

When using web search:

1. **Search queries**: Your queries are sent to the search provider
2. **API keys**: Your API keys are stored securely in IntelliJ's credential store
3. **Data usage**: Check the privacy policies of Google or Tavily for their data handling practices
4. **Code privacy**: Consider not including sensitive code in searches

## Troubleshooting

### Common Issues

#### No Search Results

If web search isn't returning results:

1. Check that web search is enabled (toggle is active)
2. Verify your API keys are correct in settings
3. Ensure you haven't exceeded API rate limits
4. Make your query more specific or use different keywords

#### Poor Search Results

If search results aren't helpful:

1. Make your query more specific
2. Include version numbers and exact error messages
3. Try using the other search provider
4. Break complex questions into simpler ones

#### API Key Issues

If you encounter API key problems:

1. Verify the key in DevoxxGenie settings
2. Check that the key is active in your Google Cloud or Tavily account
3. Ensure billing is set up if required
4. Check for any restrictions on the key (IP, usage limits)

## Advanced Configuration

### Search Settings

Advanced settings available:

- **Result Count**: Number of search results to retrieve
- **Search Type**: Standard or code-focused (Tavily)
- **Locale**: Region-specific search results (Google)
- **Safe Search**: Content filtering level (Google)

### Custom Prompting

You can optimize how web search results are used by being explicit in your prompt:

```
Search for information about Spring Boot 3.2 security configurations and explain how they differ from Spring Boot 2.x. Focus on OAuth2 and resource server setup.
```

## Future Enhancements

The DevoxxGenie team is working on several enhancements to web search:

1. **More search providers**: Integration with additional search engines
2. **Context-aware searching**: Automatically incorporating project context into searches
3. **Search result caching**: Improving performance by caching common searches
4. **Visual search results**: Better handling of diagrams and images from search results
