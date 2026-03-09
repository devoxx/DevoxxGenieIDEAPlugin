---
sidebar_position: 8
title: Troubleshooting - DevoxxGenie
description: Solutions to common issues when using DevoxxGenie, including GPU rendering problems, connection issues, and LLM configuration errors.
keywords: [devoxxgenie troubleshooting, gpu rendering error, skiko error, unsatisfiedlinkerror, chat ui not loading, windows gpu fix]
image: /img/devoxxgenie-social-card.jpg
---

# Troubleshooting

This guide helps you resolve common issues when using DevoxxGenie.

## Chat UI Not Loading / GPU Rendering Error

### Problem
On certain Windows systems with specific GPU driver configurations, the DevoxxGenie chat UI may fail to load with an error related to Skiko/Direct3D:

```
UnsatisfiedLinkError: Failed to load Direct3D native library
```

This occurs because the graphics rendering engine (Skiko) cannot initialize hardware-accelerated rendering on some GPU/driver combinations.

### Automatic Fix (v0.8.0+)

Starting with version 0.8.0, DevoxxGenie **automatically handles this issue**:

1. First, it attempts hardware rendering (Direct3D/OpenGL)
2. If that fails, it automatically retries with software rendering
3. The chat UI should load normally without any action required

You'll see a log message indicating the fallback was used:
```
Successfully initialized Compose with software rendering fallback
```

### Manual Workarounds

If you still experience issues, or want to avoid the automatic retry delay, use one of these methods:

#### Option 1: Force Software Rendering in Settings (Recommended)

1. Open **Settings** (or **Preferences** on macOS)
2. Navigate to **Tools** > **DevoxxGenie** > **Appearance**
3. Scroll to **Rendering Settings**
4. Check **"Force software rendering (fixes GPU issues on Windows)"**
5. Click **Apply** and **OK**
6. **Restart IntelliJ IDEA**

This permanently enables software rendering and prevents the error from occurring.

#### Option 2: VM Options (Alternative)

Add the following system property to your IDE's VM options:

1. Go to **Help** > **Edit Custom VM Options...**
2. Add this line:
   ```
   -Dskiko.renderApi=SOFTWARE
   ```
3. Save the file
4. **Restart IntelliJ IDEA**

#### Option 3: Update GPU Drivers

In some cases, updating your GPU drivers resolves the compatibility issue:

- **NVIDIA**: [Download from nvidia.com](https://www.nvidia.com/drivers)
- **AMD**: [Download from amd.com](https://www.amd.com/support)
- **Intel**: [Download from intel.com](https://www.intel.com/content/www/us/en/support/detect.html)

After updating drivers, restart IntelliJ IDEA and try DevoxxGenie again.

### Still Not Working?

If none of the above solutions work:

1. Check the **IntelliJ IDEA log files** for detailed error messages:
   - **Help** > **Show Log in Explorer/Finder**
   - Look for errors related to `Skiko`, `Direct3D`, or `UnsatisfiedLinkError`

2. Report the issue on [GitHub Issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) with:
   - Your operating system and version
   - GPU model and driver version
   - IntelliJ IDEA version
   - The relevant error log excerpt

## Ollama Connection Issues

### Problem
DevoxxGenie cannot connect to Ollama or models don't appear in the dropdown.

### Solutions

1. **Verify Ollama is running**:
   ```bash
   curl http://localhost:11434/api/tags
   ```
   If this fails, start Ollama: `ollama serve`

2. **Check the base URL in settings**:
   - Default should be: `http://localhost:11434`
   - If using Docker or remote Ollama, adjust accordingly

3. **Firewall/Antivirus**: Ensure port 11434 is not blocked

4. **Model not showing**: Click **Refresh Models** in the provider dropdown

See the full [Ollama setup guide](use-ollama-in-intellij.md) for more details.

## API Key Errors

### Problem
Getting "Invalid API key" or "Authentication failed" errors with cloud providers.

### Solutions

1. **Verify the API key is correct**:
   - Open DevoxxGenie settings
   - Navigate to **LLM Providers**
   - Select the provider and check the API key field
   - Re-enter the key if unsure

2. **Check key permissions**: Some providers require specific permissions or the key may be expired

3. **Verify provider status**: Check if the LLM service is experiencing outages

## Slow Responses with Local Models

### Problem
Local LLM responses are very slow or timeout.

### Solutions

1. **Use a smaller model**: Try `llama3.2:3b` or `qwen2.5-coder:1.5b` instead of larger models

2. **Check system resources**:
   - Monitor CPU/GPU usage during generation
   - Ensure sufficient RAM is available
   - Close unnecessary applications

3. **Enable GPU acceleration for Ollama**:
   ```bash
   ollama run llama3.2 --gpu
   ```

See [Ollama performance tips](use-ollama-in-intellij.md#performance-tips) for more optimization suggestions.

## Plugin Not Visible After Installation

### Problem
The DevoxxGenie icon doesn't appear in the IDE after installation.

### Solutions

1. **Restart IntelliJ IDEA** — required after plugin installation

2. **Check if the plugin is enabled**:
   - **Settings** > **Plugins** > **Installed**
   - Look for DevoxxGenie and ensure it's checked

3. **Invalidate caches**:
   - **File** > **Invalidate Caches / Restart...**
   - Select **Invalidate and Restart**

4. **Verify IDE version**: DevoxxGenie requires IntelliJ IDEA 2023.3.4 or later

## Getting Help

If your issue isn't covered here:

1. Check the [FAQ](faq.md) for common questions
2. Search [GitHub Issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) for similar problems
3. Join [GitHub Discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions) for community support
4. Create a new issue with:
   - Clear description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, IDE version, plugin version)
   - Relevant log excerpts
