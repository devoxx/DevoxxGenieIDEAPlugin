---
sidebar_position: 4
title: Custom Providers
description: Connect DevoxxGenie to any OpenAI-compatible LLM service including self-hosted models, DeepSeek R1, Grok, JLama, and enterprise AI platforms.
keywords: [devoxxgenie, custom providers, openai compatible, self-hosted llm, deepseek, grok, jlama, enterprise ai, custom endpoint]
image: /img/devoxxgenie-social-card.jpg
---

# Custom Providers

DevoxxGenie offers flexible integration with custom LLM providers through its "Custom OpenAI" provider option. This allows you to connect to any service that implements the OpenAI-compatible API format.

## Overview

The Custom OpenAI provider feature allows you to:

1. Connect to self-hosted or third-party LLM services
2. Use private or specialized LLM deployments
3. Integrate with enterprise LLM platforms
4. Use new providers that aren't directly supported yet

This feature is particularly useful for:
- Enterprise environments with custom AI deployments
- Research teams using specialized models
- Users who want to connect to newer or niche providers
- Organizations with specific security or compliance requirements

## Setting Up Custom Providers

### Basic Configuration

To configure a custom provider:

1. Open DevoxxGenie settings (`Settings` → `Tools` → `DevoxxGenie`)
2. Navigate to the "LLM Providers" tab
3. Select "Custom OpenAI" as the provider
4. Configure the required settings:
   - **URL**: The base endpoint for the API (e.g., `http://localhost:8000/v1` or `https://custom-provider.example.com/api`)
   - **Model**: The model identifier expected by the provider
   - **API Key**: Authentication key (if required)

![Custom OpenAI Settings](/img/custom-openai-settings.png)

### Model Parameters

You can configure standard LLM parameters:

- **Temperature**: Controls randomness (0.0-2.0)
- **Top P**: Controls diversity via nucleus sampling (0.0-1.0)
- **Max Tokens**: Maximum length of the response
- **Timeout**: Request timeout in seconds

## Compatible Provider Types

### Self-Hosted OpenAI-Compatible Servers

Many self-hosted inference servers implement the OpenAI API format:

#### LocalAI

[LocalAI](https://github.com/localai/localai) is a drop-in replacement for OpenAI's API that runs on your hardware:

```bash
# Example setup
docker run -p 8080:8080 localai/localai
```

Configuration for DevoxxGenie:
- URL: `http://localhost:8080/v1`
- Model: Name of your loaded model (e.g., `ggml-gpt4all-j`)

#### vLLM

[vLLM](https://github.com/vllm-project/vllm) is a high-throughput and memory-efficient inference engine:

```bash
# Example setup
python -m vllm.entrypoints.openai.api_server --model meta-llama/Llama-2-7b-chat-hf
```

Configuration for DevoxxGenie:
- URL: `http://localhost:8000/v1`
- Model: The model name (e.g., `meta-llama/Llama-2-7b-chat-hf`)

#### Text Generation WebUI

[Text Generation WebUI](https://github.com/oobabooga/text-generation-webui) is a popular interface for running LLMs:

```bash
# Enable the OpenAI API extension in the WebUI
python server.py --extensions openai
```

Configuration for DevoxxGenie:
- URL: `http://localhost:5000/v1`
- Model: The model name as shown in the WebUI

#### LMStudio

[LMStudio](https://lmstudio.ai/) provides an OpenAI-compatible server:

1. Launch LMStudio
2. Load your model
3. Start the local server

Configuration for DevoxxGenie:
- URL: `http://localhost:1234/v1`
- Model: The model name as configured in LMStudio

### Java-Based LLM Servers

Several Java-based LLM servers provide OpenAI-compatible APIs:

#### JLama

[JLama](https://github.com/tjake/Jlama) is a pure Java implementation of LLM inference:

```bash
# Start JLama with OpenAI-compatible API
java -jar jlama.jar --openai-api
```

Configuration for DevoxxGenie:
- URL: `http://localhost:8080/v1`
- Model: The model name as configured in JLama

#### Llama3.java

[Llama3.java](https://github.com/stephanj/Llama3JavaChatCompletionService) is a Spring Boot wrapper for Llama 3 models:

```bash
# Start the service
java -jar llama3java.jar
```

Configuration for DevoxxGenie:
- URL: `http://localhost:8080/v1`
- Model: The model name (e.g., `llama3`)

### Enterprise AI Platforms

Many enterprise AI platforms provide OpenAI-compatible endpoints:

#### HuggingFace Inference Endpoints

If you deploy models on [HuggingFace Inference Endpoints](https://huggingface.co/inference-endpoints), they can expose OpenAI-compatible APIs:

Configuration for DevoxxGenie:
- URL: Your HuggingFace endpoint URL
- API Key: Your HuggingFace API key
- Model: The deployed model name

#### SageMaker Endpoints

Custom Amazon SageMaker deployments with OpenAI-compatible APIs:

Configuration for DevoxxGenie:
- URL: Your SageMaker endpoint URL
- API Key: Your AWS credentials (handled separately)
- Model: The model name expected by your endpoint

#### Private AI Deployments

For private enterprise AI deployments:

Configuration for DevoxxGenie:
- URL: Your internal API endpoint
- API Key: Your internal authentication token
- Model: The internal model identifier

## Recent Cloud Providers with Custom Integration

Several newer cloud providers may be used via the Custom OpenAI integration. Note that DeepSeek, Grok, and Kimi are now available as native providers in DevoxxGenie. Use Custom OpenAI only if you need to connect to alternative endpoints.

### DeepSeek R1 (via Nvidia/Chutes)

To connect to DeepSeek R1 model via Nvidia or Chutes (when not using the native DeepSeek provider):

Configuration for DevoxxGenie:
- URL: `https://chutes-deepseek-ai-deepseek-r1.chutes.ai/v1/` (Chutes) or `https://integrate.api.nvidia.com/v1` (Nvidia)
- API Key: Your DeepSeek API key
- Model: `deepseek-r1`

### Grok

To connect to Grok (when not using the native Grok provider):

Configuration for DevoxxGenie:
- URL: `https://console.x.ai/` (Check the latest URL)
- API Key: Your Grok API key
- Model: The Grok model name

### Kimi (Moonshot AI)

To connect to Kimi via Custom OpenAI (when not using the native Kimi provider):

Configuration for DevoxxGenie:
- URL: `https://api.moonshot.ai/v1`
- API Key: Your Kimi API key from [platform.moonshot.ai](https://platform.moonshot.ai/)
- Model: `moonshot-v1-8k`, `moonshot-v1-32k`, `moonshot-v1-128k`, or `kimi-k2-turbo-preview`

## OpenAI API Compatibility

When using custom providers, it's important to understand OpenAI API compatibility:

### Core Endpoints

Most providers implement these essential endpoints:

- `/chat/completions`: For chat-based interactions (most commonly used)
- `/completions`: For completion-based interactions
- `/models`: To list available models

### Request Format

The standard request format follows this pattern:

```json
{
  "model": "model-name",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Hello, can you help me with Java code?"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000
}
```

### Response Format

The standard response format follows this pattern:

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677858242,
  "model": "model-name",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Hello! I'd be happy to help with your Java code..."
      },
      "finish_reason": "stop",
      "index": 0
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 120,
    "total_tokens": 170
  }
}
```

### Compatibility Variations

Different providers may have varying levels of compatibility:

- **Full compatibility**: Implements all endpoints and parameters
- **Basic compatibility**: Implements only the essential endpoints
- **Partial compatibility**: May have limitations or differences in parameters

## Best Practices

### Testing New Providers

Before using a custom provider extensively:

1. Test with simple queries to verify basic functionality
2. Check response formats match what DevoxxGenie expects
3. Test streaming if you intend to use it
4. Validate token counting accuracy

### Security Considerations

When using custom providers:

1. Verify the trustworthiness of the provider
2. Use HTTPS for remote endpoints
3. Use API keys with minimal necessary permissions
4. Be careful with sensitive code and data

### Performance Optimization

To optimize performance with custom providers:

1. Choose appropriate timeout settings
2. Check latency and adjust expectations
3. Use appropriate model parameters (temperature, etc.)
4. Consider local endpoints for lower latency

## Troubleshooting

### Common Issues

#### Connection Errors

If you're unable to connect to the provider:
- Verify the URL is correct and accessible
- Check that the service is running
- Test the endpoint with a tool like curl or Postman
- Check for firewall or network restrictions

```bash
# Example curl command to test an endpoint
curl -X POST "http://localhost:8000/v1/chat/completions" \
  -H "Content-Type: application/json" \
  -d '{"model":"test-model","messages":[{"role":"user","content":"Hello"}]}'
```

#### Authentication Issues

If authentication fails:
- Verify the API key is correct
- Ensure the key has the necessary permissions
- Check if the provider requires additional headers
- Verify the authentication method (some may use Bearer tokens)

#### Response Format Problems

If responses aren't being processed correctly:
- Check if the provider fully implements the OpenAI schema
- Look for any non-standard fields or formats
- Test with simpler requests to isolate issues
- Check provider documentation for any format differences

#### Model Not Found

If you get "model not found" errors:
- Verify the model name is exactly as expected by the provider
- Check if the model needs to be loaded or started first
- Ensure the model is available in your provider's deployment

## Example Configurations

Here are complete configuration examples for common custom provider setups:

### LocalAI with Llama 3

```
URL: http://localhost:8080/v1
Model: llama3:8b
API Key: (leave empty)
Temperature: 0.7
Max Tokens: 2000
Timeout: 300
```

### JLama Server

```
URL: http://localhost:8080/v1
Model: jlama-model
API Key: (leave empty)
Temperature: 0.8
Max Tokens: 1000
Timeout: 120
```

### Enterprise Deployment

```
URL: https://ai-api.enterprise.com/openai/v1
Model: enterprise-code-llm
API Key: your-api-key-here
Temperature: 0.5
Max Tokens: 4000
Timeout: 60
```

## Future Directions

The landscape of custom LLM providers is rapidly evolving:

1. **More standardization**: Increasing adoption of OpenAI-compatible APIs
2. **Better performance**: Improved inference servers with lower latency
3. **Specialized providers**: More code-focused and domain-specific models
4. **Enterprise solutions**: More managed and private AI deployments

DevoxxGenie will continue to enhance its custom provider support to accommodate these developments.
