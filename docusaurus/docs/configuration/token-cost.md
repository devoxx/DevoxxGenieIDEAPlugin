---
sidebar_position: 3
---

# Token Cost & Context Window

DevoxxGenie provides tools to help you manage token usage, control costs, and optimize the context window size when working with cloud LLM providers. This page explains how to configure and use these features.

## Understanding Tokens and Costs

### What are Tokens?

Tokens are the basic units that LLMs process. In simple terms:
- A token is roughly 4 characters or 3/4 of a word in English
- Code typically uses more tokens than natural language
- Different languages may have different tokenization patterns

### Why Token Management Matters

- **Cost Control**: Cloud LLM providers charge based on token usage
- **Context Window**: LLMs have limits on how many tokens they can process at once
- **Performance**: Optimizing token usage can lead to better, more focused responses

## Token Cost Configuration

Access token cost settings through:
1. Settings → Tools → DevoxxGenie → Token Cost & Context Window

![Token Cost Settings](/img/token-cost-settings.png)

### Configuring Provider Costs

For each LLM provider, you can configure:

1. **Input Cost**: The cost per 1,000 tokens for input (prompts)
2. **Output Cost**: The cost per 1,000 tokens for output (responses)
3. **Context Window**: The maximum number of tokens the model can process

Default values are pre-configured, but you can adjust them if the provider changes their pricing or if you have special rates.

### Provider-Specific Settings

Different providers have different pricing models:

- **OpenAI**: Different rates for different models (GPT-3.5, GPT-4, etc.)
- **Anthropic**: Pricing varies by Claude model
- **Google**: Different rates for different Gemini models
- **Others**: Each provider has their own pricing structure

## Context Window Management

The context window is the amount of text (in tokens) that the LLM can "see" at once.

### Window Size by Provider

Each LLM has a maximum context window size:

| Provider | Model | Context Window |
|----------|-------|---------------|
| OpenAI | GPT-4o | 128K tokens |
| OpenAI | GPT-3.5 Turbo | 16K tokens |
| Anthropic | Claude 3.5 Sonnet | 200K tokens |
| Google | Gemini 1.5 Pro | 1M tokens |
| Ollama | Llama 3 | Varies by version |

### Optimizing Context Window Usage

DevoxxGenie provides several tools to help you optimize context window usage:

1. **Token Usage Bar**: Visual indicator of how much of the context window is being used
2. **Token Calculator**: Calculate tokens before sending prompts
3. **Project Scanner Settings**: Control how much code is included in prompts

## Token Calculation Features

### Token Calculator

To calculate tokens for a directory or file:

1. Right-click on a directory in the project view
2. Select "Calc Tokens for Directory"
3. View the token count, estimated cost, and available models

![Token Calculator](/img/token-calculator.png)

### Calculating Tokens for Current Prompt

The DevoxxGenie interface displays:

1. **Current Token Count**: The number of tokens in your prompt
2. **Estimated Cost**: The approximate cost of the request
3. **Available Models**: Which models can handle the token count

### Token Usage in Responses

After receiving a response, DevoxxGenie shows:

1. **Input Tokens**: How many tokens were in your prompt
2. **Output Tokens**: How many tokens were in the LLM's response
3. **Total Cost**: The estimated cost of the exchange

## Smart Model Selection

DevoxxGenie helps you choose the right model for your task:

- Models in the dropdown are filtered by context window size
- Models that can't handle your prompt are disabled
- Cost information is shown for each model

### Cost-Saving Strategies

To minimize token usage and costs:

1. **Be Specific**: Craft focused prompts that are direct about what you need
2. **Limit Context**: Only include relevant code in your prompts
3. **Use RAG**: Instead of including entire files, use RAG to retrieve only relevant sections
4. **Clean Code**: Remove unnecessary comments and unused imports before including code
5. **Use Local Models**: For non-critical tasks, consider using local models (free)

## Project Scanning and Token Management

When scanning your project for context:

1. **Selective Inclusion**: Choose specific directories or files to include
2. **Exclusion Patterns**: Configure patterns to exclude (e.g., test files, generated code)
3. **JavaDoc Removal**: Option to strip JavaDocs to reduce token count
4. **Size Limits**: Set maximum file sizes to include

### "Add Project" Feature

The "Add Project to Context" feature considers token limits:

1. Analyses project size in tokens
2. Warns if the project exceeds the model's context window
3. Suggests appropriate models for your project size
4. Gives token count and cost estimates

## Best Practices

1. **Start Small**: Begin with smaller contexts and add more as needed
2. **Monitor Costs**: Regularly check token usage to avoid unexpected bills
3. **Try Different Models**: Smaller models are often adequate and much cheaper
4. **Balance Quality and Cost**: Higher-tier models cost more but may provide better results
5. **Set Budgets**: Consider setting monthly budgets for API usage

## Troubleshooting

- **Token Count Discrepancies**: If provider-reported tokens differ from DevoxxGenie estimates, adjust the calculation settings
- **Context Window Errors**: If you receive errors about exceeding context length, reduce the amount of included code or switch to a model with a larger context window
- **High Costs**: If costs are higher than expected, review your usage patterns and consider using more efficient prompts or local models
