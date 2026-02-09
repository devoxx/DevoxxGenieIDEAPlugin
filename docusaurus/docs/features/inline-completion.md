---
sidebar_position: 10
title: Inline Code Completion
description: Setup and configuration guide for DevoxxGenie's inline code completion feature using Fill-in-the-Middle (FIM) models via Ollama or LM Studio.
keywords: [inline completion, code completion, intellij, fim, fill-in-the-middle, ollama, lmstudio, starcoder, qwen, deepseek-coder]
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Inline Code Completion

DevoxxGenie provides AI-powered inline code completion that suggests code as you type, appearing as ghost text directly in your editor. This feature uses Fill-in-the-Middle (FIM) models to intelligently complete code based on the context both before and after your cursor position.

## What is Inline Completion?

Unlike traditional code completion that only looks at the code before your cursor, DevoxxGenie's inline completion uses **Fill-in-the-Middle (FIM)** technology:

- **Prefix context**: Code before your cursor (up to 4096 characters)
- **Suffix context**: Code after your cursor (up to 1024 characters)
- **Smart completion**: The model generates code that fits naturally between the prefix and suffix

This results in more contextually relevant suggestions that understand the broader structure of your code.

## Requirements

### IntelliJ IDEA Version

Inline completion requires **IntelliJ IDEA 2024.3+**. The feature uses the new debounced inline completion API available in these versions.

### Supported Providers

Inline completion is supported via two local LLM providers:

- **[Ollama](/docs/llm-providers/local-models#ollama)** - Open-source, easy to use, wide model selection
- **[LM Studio](/docs/llm-providers/local-models#lmstudio)** - Desktop app with GUI, OpenAI-compatible API

Choose the provider that best fits your workflow. Both require FIM-capable models.

## Setup

### 1. Choose and Configure Your Provider

<Tabs>
<TabItem value="ollama" label="Ollama">

1. Install Ollama from [ollama.com](https://ollama.com)
2. Start Ollama (runs in background)
3. Ensure Ollama URL is configured in **Settings** > **DevoxxGenie** > **LLM Providers** (default: `http://localhost:11434`)

</TabItem>
<TabItem value="lmstudio" label="LM Studio">

1. Install LM Studio from [lmstudio.ai](https://lmstudio.ai)
2. Launch LM Studio and download a FIM-capable model (see [recommended models](#recommended-fim-models))
3. Start the local server in LM Studio (toggle in the UI)
4. Ensure LM Studio URL is configured in **Settings** > **DevoxxGenie** > **LLM Providers** (default: `http://localhost:1234/v1`)

:::tip
LM Studio must have the **Local Server** running for inline completion to work. Check the server status in the LM Studio UI.
:::

</TabItem>
</Tabs>

### 2. Install a FIM Model

<Tabs>
<TabItem value="ollama" label="Ollama Models">

Pull one of the recommended FIM models:

```bash
# Recommended: Lightweight and fast
ollama pull starcoder2:3b

# Alternative: Better quality, slightly slower
ollama pull qwen2.5-coder:7b

# Alternative: DeepSeek Coder (base version for FIM)
ollama pull deepseek-coder:6.7b-base
```

</TabItem>
<TabItem value="lmstudio" label="LM Studio Models">

In LM Studio:

1. Go to the **Discover** tab
2. Search for FIM-capable models:
   - `starcoder2-3b` (lightweight, fast)
   - `qwen2.5-coder-7b` (balanced quality/speed)
   - `deepseek-coder-6.7b-base` (code-optimized)
3. Download your chosen model
4. Start the **Local Server** with the model loaded

:::note
LM Studio uses HuggingFace model names (e.g., `starcoder2-3b` instead of `starcoder2:3b`).
:::

</TabItem>
</Tabs>

### 3. Enable Inline Completion

1. Open **Settings** > **Tools** > **DevoxxGenie** > **Completion**
2. Select your provider from the **"Fill-in-the-Middle Provider"** dropdown:
   - **None** - Disable inline completion
   - **Ollama** - Use Ollama for completions
   - **LM Studio** - Use LM Studio for completions
3. Select your FIM model from the dropdown (click **Refresh Models** if empty)
4. Adjust performance settings if needed (see [configuration](#configuration))

### 4. Start Coding

Once enabled, suggestions will appear automatically as you type. The ghost text appears in gray after your cursor.

## Using Inline Completion

### Accepting Suggestions

| Action | Shortcut | Description |
|--------|----------|-------------|
| Accept full suggestion | `Tab` | Insert the entire completion |
| Accept next word | `Ctrl+Right Arrow` (Windows/Linux) or `Option+Right Arrow` (Mac) | Insert only the next word |
| Accept next line | `Ctrl+Enter` | Insert only the current line |
| Dismiss | `Escape` | Hide the suggestion |

### When Suggestions Appear

Suggestions appear automatically when:
- You're typing in a writable code editor
- The file type is supported (not binary)
- The file is under 500KB
- No code completion popup is currently visible

Suggestions are **not** shown when:
- The feature is disabled in settings (Provider set to "None")
- No FIM model is configured
- The editor is in viewer mode
- A lookup/completion popup is active

## Configuration

Configure inline completion in **Settings** > **Tools** > **DevoxxGenie** > **Completion**:

| Setting | Description | Default | Range |
|---------|-------------|---------|-------|
| **Provider** | FIM provider: None, Ollama, or LM Studio | None | - |
| **Model name** | The FIM model to use | - | Available models |
| **Max tokens** | Maximum tokens to generate | 64 | 16-256 |
| **Timeout (ms)** | Request timeout in milliseconds | 5000 | 1000-30000 |
| **Debounce delay (ms)** | Delay after typing before requesting | 300 | 100-2000 |

:::info
Provider URLs are configured in the main **DevoxxGenie LLM Providers** settings. Inline completion uses the same endpoints as the chat interface.
:::

### Tuning Recommendations

**For faster suggestions:**
- Reduce **Debounce delay** to 100-200ms
- Reduce **Max tokens** to 32-48
- Use a smaller model like `starcoder2:3b`

**For better quality suggestions:**
- Increase **Max tokens** to 128-256
- Use a larger model like `qwen2.5-coder:7b`
- Increase **Timeout** if using a slower model

**For slower machines:**
- Use `starcoder2:3b` (3 billion parameters)
- Increase **Debounce delay** to 500-1000ms
- Set **Timeout** to 10000ms or higher

## Recommended FIM Models

Not all models support FIM. You need models specifically trained for Fill-in-the-Middle completion:

### Lightweight (Fast, Good for Everyday Coding)

| Model | Size | Best For | Ollama | LM Studio |
|-------|------|----------|--------|-----------|
| `starcoder2:3b` / `starcoder2-3b` | 3B | Fast suggestions, general coding | ✅ | ✅ |
| `qwen2.5-coder:1.5b` / `qwen2.5-coder-1.5b` | 1.5B | Very fast, lightweight tasks | ✅ | ✅ |

### Balanced (Quality vs Speed)

| Model | Size | Best For | Ollama | LM Studio |
|-------|------|----------|--------|-----------|
| `qwen2.5-coder:7b` / `qwen2.5-coder-7b` | 7B | Good balance of quality and speed | ✅ | ✅ |
| `deepseek-coder:6.7b-base` / `deepseek-coder-6.7b-base` | 6.7B | Code-specific training | ✅ | ✅ |

### Higher Quality (Slower but Better)

| Model | Size | Best For | Ollama | LM Studio |
|-------|------|----------|--------|-----------|
| `qwen2.5-coder:14b` / `qwen2.5-coder-14b` | 14B | Complex code, larger context | ✅ | ✅ |

:::tip
Start with `starcoder2:3b` for the best initial experience. It's fast enough for real-time suggestions while providing good completion quality.
:::

## Provider Comparison

| Feature | Ollama | LM Studio |
|---------|--------|-----------|
| Setup Complexity | Simple CLI | Desktop GUI |
| Model Management | Command line | Visual interface |
| Resource Usage | Lower overhead | Higher (GUI) |
| Best For | Developers comfortable with CLI | Users preferring GUI |
| FIM Support | Native `/api/generate` with suffix | OpenAI-compatible `/v1/completions` |

## How It Works

```
Your Code:
----------------------------------------
public void calculateTotal() {
    double sum = 0;
    for (Item item : items) {
        sum += █
    }
    return sum;
}
----------------------------------------
         ↑
    Cursor position

Prefix sent to model:
  "public void calculateTotal() {\n    double sum = 0;\n    for (Item item : items) {\n        sum += "

Suffix sent to model:
  "\n    }\n    return sum;\n}"

Generated completion:
  "item.getPrice() * item.getQuantity();"
```

The model sees both the code before and after your cursor to generate contextually appropriate completions.

## Troubleshooting

### No Suggestions Appearing

1. **Verify the feature is enabled**: Check Settings > DevoxxGenie > Completion (Provider should not be "None")
2. **Check your provider is running**:
   - Ollama: Run `ollama list` in terminal
   - LM Studio: Check that the Local Server is started in the UI
3. **Check your model**: Ensure you've selected a FIM-capable model
4. **Verify the URL**: Check that the provider URL is correct in LLM Providers settings
5. **Check IntelliJ version**: Requires 2024.3 or later

### Slow Suggestions

1. **Use a smaller model**: Try `starcoder2:3b` instead of larger models
2. **Reduce max tokens**: Lower to 32-48 for faster generation
3. **Increase debounce delay**: Set to 500-1000ms to reduce request frequency
4. **Check system resources**: Ensure your machine has enough RAM/CPU

### Low Quality Suggestions

1. **Use a larger model**: Try `qwen2.5-coder:7b` or `deepseek-coder:6.7b-base`
2. **Increase max tokens**: Allow the model to generate more context
3. **Ensure FIM model**: Regular chat models don't work well for inline completion

### Provider-Specific Issues

**Ollama:**
- Verify the model name is correct in settings
- Check that Ollama is accessible at the configured URL
- Try pulling the model again: `ollama pull starcoder2:3b`

**LM Studio:**
- Ensure the Local Server is running (check the toggle in LM Studio UI)
- Verify the model is loaded in LM Studio
- Check that the URL ends with `/v1` (e.g., `http://localhost:1234/v1`)
- Try reloading the model in LM Studio

## Best Practices

1. **Start small**: Begin with `starcoder2:3b` and upgrade if you need better quality
2. **Tune debounce delay**: Find a balance between responsiveness and system load
3. **Use appropriate context**: The model works best when there's clear surrounding code
4. **Accept word-by-word**: Use partial acceptance (Ctrl+Right Arrow) for long suggestions
5. **Disable when not needed**: Set Provider to "None" when doing non-coding tasks
6. **Keep models loaded**: For LM Studio, keep the model loaded for faster first suggestions

## Future Enhancements

Planned improvements to inline completion include:

- Support for additional providers
- Multi-line completion improvements
- Context-aware language detection
- Integration with project-specific patterns
