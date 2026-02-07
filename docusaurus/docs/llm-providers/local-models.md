---
sidebar_position: 2
title: Local LLM Providers
description: Guide to all local LLM providers supported by DevoxxGenie, including setup instructions and configuration.
keywords: [devoxxgenie, ollama, lmstudio, gpt4all, llama.cpp, jan, local llm, privacy]
---

# Local LLM Providers

DevoxxGenie supports a variety of local LLM providers, allowing you to run AI models on your own machine. This offers benefits like privacy, offline usage, and no per-token costs.

## Supported Local Providers

DevoxxGenie integrates with these local LLM providers:

1. [Ollama](#ollama)
2. [LMStudio](#lmstudio)
3. [GPT4All](#gpt4all)
4. [Llama.cpp](#llamacpp)
5. [Jan](#jan)
6. [Custom OpenAI-compatible Providers](#custom-openai-compatible-providers)

## Ollama

[Ollama](https://ollama.com/) is one of the most popular tools for running LLMs locally, offering a simple way to download and run a variety of models.

### Setup

1. Download and install Ollama from [ollama.com/download](https://ollama.com/download)
2. Open a terminal and pull a model:
   ```bash
   ollama run llama3.2
   ```
3. In DevoxxGenie settings, select "Ollama" as the provider
4. Choose your downloaded model from the dropdown

:::tip
Available models are **automatically fetched** from your running Ollama instance. Any model you've pulled with `ollama pull` will appear in the model dropdown.
:::

### Configuration

In DevoxxGenie settings, you can configure:

- **Endpoint URL**: Default is `http://localhost:11434`
- **Model**: Select from available downloaded models
- **Parameters**:
  - Temperature (creativity vs. predictability)
  - Top P (diversity of responses)
  - Context window (varies by model)

### Advantages of Ollama

- Easy setup and model management
- Wide variety of models
- Good performance on consumer hardware
- Support for multimodal models

## LMStudio

[LM Studio](https://lmstudio.ai/) is a powerful desktop application for running and fine-tuning language models.

### Setup

1. Download and install LM Studio from [lmstudio.ai](https://lmstudio.ai/)
2. Launch LM Studio and download a model
3. Start the local server in LM Studio
4. In DevoxxGenie settings, select "LMStudio" as the provider
5. Configure the endpoint (default: `http://localhost:1234/v1`)

:::tip
Available models are **automatically fetched** from the running LM Studio server.
:::

### Configuration

LM Studio allows extensive configuration:

- **Model Selection**: Choose from downloaded models
- **Inference Parameters**: Customize generation settings
- **Quantization**: Run models with reduced precision for better performance
- **Server Settings**: Configure the OpenAI-compatible API server

### Advantages of LM Studio

- Excellent UI for model management
- Advanced configuration options
- Built-in chat interface for testing
- Model comparison tools

## GPT4All

[GPT4All](https://gpt4all.io/) focuses on running lightweight models locally with minimal setup.

### Setup

1. Download and install GPT4All from [gpt4all.io](https://gpt4all.io/)
2. Launch GPT4All and download a model
3. Start the API server in GPT4All
4. In DevoxxGenie settings, select "GPT4All" as the provider

### Configuration

GPT4All provides:

- **Model Library**: Built-in access to compatible models
- **API Server**: OpenAI-compatible REST API
- **Basic Parameters**: Temperature, top_p, etc.

### Advantages of GPT4All

- Simple, user-friendly interface
- Focuses on smaller, efficient models
- Low resource requirements
- Cross-platform support

## Llamacpp

[Llama.cpp](https://github.com/ggerganov/llama.cpp) is a C/C++ implementation of the Llama model, optimized for CPU inference.

### Setup

1. Clone the [llama.cpp repository](https://github.com/ggerganov/llama.cpp)
2. Build the project following the repository instructions
3. Download model weights
4. Start the server:
   ```bash
   ./server -m /path/to/model.gguf
   ```
5. In DevoxxGenie settings, select "Llama CPP" as the provider
6. Configure the endpoint (default: `http://localhost:8080`)

### Configuration

Llama.cpp offers advanced configuration:

- **Model Quantization**: Various precision options (q4_k_m, q5_k_m, etc.)
- **Context Size**: Configurable context window
- **Thread Count**: CPU thread utilization
- **GPU Acceleration**: CUDA, Metal, and other options

### Advantages of Llama.cpp

- Highly optimized performance
- Extensive customization options
- Support for many model architectures
- Active development community

## Jan

[Jan](https://jan.ai/) is an open-source alternative to ChatGPT that runs locally on your computer.

### Setup

1. Download and install Jan from [jan.ai](https://jan.ai/)
2. Launch Jan and download models
3. In DevoxxGenie settings, select "Jan" as the provider
4. Configure any necessary connection parameters

### Configuration

Jan provides a full chat interface and API:

- **Built-in Model Library**: Easy download of various models
- **Chat Interface**: Similar to ChatGPT but local
- **API Access**: Connect other applications to your models

### Advantages of Jan

- All-in-one solution (model management + chat interface)
- User-friendly interface
- Active development with frequent updates
- Open-source community

## Custom OpenAI-compatible Providers

DevoxxGenie supports any OpenAI-compatible API server, allowing you to use many other local inference servers.

### Setup

1. Install and configure your preferred OpenAI-compatible server
2. In DevoxxGenie settings, select "Custom OpenAI" as the provider
3. Configure:
   - **URL**: Your server's endpoint
   - **Model**: The model name your server expects
   - **API Key**: If your server requires authentication

### Compatible Servers

Many local servers support the OpenAI API format:

- **LocalAI**: Lightweight API on top of llama.cpp
- **vLLM**: High-throughput serving for LLMs
- **Text Generation WebUI**: Comprehensive interface with API
- **Llama3.java**: Java implementation of Llama 3
- **And many others**

### Advantages of Custom Providers

- Flexibility to use your preferred implementation
- Support for specialized or custom models
- Integration with existing infrastructure
- Future compatibility with new providers

## Hardware Considerations

When using local LLM providers, consider your hardware:

### RAM

- Minimum: 8GB for small models (1-3B parameters)
- Recommended: 16GB+ for medium models (7-13B parameters)
- Large models (30B+): 32GB or more

### GPU

- NVIDIA GPUs: CUDA acceleration for most providers
- AMD GPUs: ROCm support in some providers
- Apple Silicon: Metal acceleration for excellent performance

### Disk Space

- Models typically require 2-10GB each
- Consider SSD storage for faster loading times

## Best Practices

### Model Selection

- **Start small**: Begin with smaller models (1-7B) and move up as needed
- **Specialized models**: Use code-specific models for programming tasks
- **Quantized models**: Lower precision models use less memory with minimal quality loss

### Provider Selection

Choose the provider that best matches your needs:

- **Ease of use**: Ollama and GPT4All are simplest to set up
- **Performance**: Llama.cpp offers the most control over optimization
- **Customization**: LM Studio and Llama.cpp provide the most options
- **All-in-one**: Jan provides model management + chat in a single app

## Troubleshooting

### Common Issues

#### Model Loading Fails

- Check you have sufficient RAM
- Verify model files are not corrupted
- Try a smaller or more quantized model

#### Slow Response Times

- Lower the context window size
- Use a smaller or more quantized model
- Enable GPU acceleration if available

#### Connection Errors

- Verify the provider service is running
- Check the endpoint URL in DevoxxGenie settings
- Ensure no firewall is blocking the connection
- Verify the correct port is configured
