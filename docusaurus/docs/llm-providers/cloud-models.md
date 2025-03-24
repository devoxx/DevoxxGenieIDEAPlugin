---
sidebar_position: 3
---

# Cloud LLM Providers

DevoxxGenie supports a wide range of cloud-based LLM providers, giving you access to powerful state-of-the-art models without requiring high-end local hardware.

## Supported Cloud Providers

DevoxxGenie integrates with these cloud LLM providers:

1. [OpenAI](#openai)
2. [Anthropic](#anthropic)
3. [Google](#google)
4. [Mistral](#mistral)
5. [Groq](#groq)
6. [DeepInfra](#deepinfra)
7. [DeepSeek](#deepseek)
8. [OpenRouter](#openrouter)
9. [Azure OpenAI](#azure-openai)
10. [Amazon Bedrock](#amazon-bedrock)

## OpenAI

[OpenAI](https://openai.com) provides some of the most widely used LLMs, including the GPT family of models.

### Available Models

OpenAI offers several models with different capabilities and price points:

- **GPT-4o**: Latest flagship model with multimodal capabilities
- **GPT-4o mini**: More efficient version of GPT-4o
- **GPT-4 Turbo**: Advanced model with a large context window
- **GPT-3.5 Turbo**: Balanced model for most use cases
- **O1**: Specialized models designed for efficiency and reasoning

### Setup

1. Create an account at [OpenAI](https://platform.openai.com/)
2. Generate an API key in your account dashboard
3. In DevoxxGenie settings, select "OpenAI" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

In DevoxxGenie settings, you can configure:

- **API Key**: Your OpenAI API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P
  - Maximum tokens

### Pricing

OpenAI uses a token-based pricing model:

| Model | Input Cost (per 1M tokens) | Output Cost (per 1M tokens) |
|-------|----------------------------|------------------------------|
| GPT-4o | $5 | $15 |
| GPT-4o mini | $2 | $6 |
| GPT-3.5 Turbo | $0.50 | $1.50 |

*Prices may change; check [OpenAI's pricing page](https://openai.com/pricing) for current rates.*

### Advantages

- State-of-the-art models
- Excellent code understanding
- Multimodal capabilities in newer models
- Reliable API with good documentation

## Anthropic

[Anthropic](https://anthropic.com/) provides the Claude family of models, known for their helpfulness, harmlessness, and honesty.

### Available Models

Anthropic offers several Claude models:

- **Claude 3.5 Sonnet**: Latest high-performance model
- **Claude 3.5 Haiku**: Faster, more efficient model
- **Claude 3 Opus**: Most powerful model for complex tasks
- **Claude 3 Sonnet**: Balanced performance and speed
- **Claude 3 Haiku**: Fast, efficient model

### Setup

1. Create an account at [Anthropic](https://console.anthropic.com/)
2. Generate an API key in your account dashboard
3. In DevoxxGenie settings, select "Anthropic" as the provider
4. Paste your API key
5. Select your preferred Claude model

### Configuration

In the settings, you can configure:

- **API Key**: Your Anthropic API key
- **Model**: Select from available Claude models
- **Parameters**:
  - Temperature
  - Top P
  - Maximum tokens

### Pricing

Anthropic uses a token-based pricing model:

| Model | Input Cost (per 1M tokens) | Output Cost (per 1M tokens) |
|-------|----------------------------|------------------------------|
| Claude 3.5 Sonnet | $3 | $15 |
| Claude 3.5 Haiku | $0.25 | $1.25 |
| Claude 3 Opus | $15 | $75 |
| Claude 3 Sonnet | $3 | $15 |
| Claude 3 Haiku | $0.25 | $1.25 |

*Prices may change; check [Anthropic's pricing page](https://www.anthropic.com/pricing) for current rates.*

### Advantages

- Excellent reasoning abilities
- Very large context windows (up to 200K tokens)
- Strong performance on code tasks
- Clear, nuanced responses

## Google

Google provides the Gemini family of models through the Google AI Studio.

### Available Models

Google offers several Gemini models:

- **Gemini 1.5 Pro**: Advanced model with 1M token context window
- **Gemini 1.5 Flash**: Faster, more efficient model
- **Gemini 1.0 Pro**: Earlier version of the Pro model
- **Gemini Pro Vision**: Vision-capable model

### Setup

1. Create an account at [Google AI Studio](https://aistudio.google.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "Google" as the provider
4. Paste your API key
5. Select your preferred Gemini model

### Configuration

In the settings, you can configure:

- **API Key**: Your Google API key
- **Model**: Select from available Gemini models
- **Parameters**:
  - Temperature
  - Top P
  - Maximum output tokens

### Pricing

Google uses a token-based pricing model:

| Model | Input Cost (per 1M tokens) | Output Cost (per 1M tokens) |
|-------|----------------------------|------------------------------|
| Gemini 1.5 Pro | $7 | $21 |
| Gemini 1.5 Flash | $0.35 | $1.05 |
| Gemini 1.0 Pro | $3.50 | $10.50 |

*Prices may change; check [Google's pricing page](https://ai.google.dev/pricing) for current rates.*

### Advantages

- Extremely large context window (1M tokens)
- Strong multimodal capabilities
- Good performance on code tasks
- Integration with Google ecosystem

## Mistral

[Mistral AI](https://mistral.ai/) offers efficient, powerful models with competitive performance.

### Available Models

Mistral provides several model families:

- **Mistral Large**: Most powerful model
- **Mistral Medium**: Balanced performance
- **Mistral Small**: Efficient, fast model
- **Mistral Embed**: Embedding model

### Setup

1. Create an account at [Mistral AI](https://console.mistral.ai/)
2. Generate an API key
3. In DevoxxGenie settings, select "Mistral" as the provider
4. Paste your API key
5. Select your preferred Mistral model

### Configuration

In the settings, you can configure:

- **API Key**: Your Mistral API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P

### Pricing

Mistral uses a token-based pricing model:

| Model | Input Cost (per 1M tokens) | Output Cost (per 1M tokens) |
|-------|----------------------------|------------------------------|
| Mistral Large | $8 | $24 |
| Mistral Medium | $2.70 | $8.10 |
| Mistral Small | $1 | $3 |

*Prices may change; check [Mistral's pricing page](https://mistral.ai/pricing/) for current rates.*

### Advantages

- Efficient models with competitive performance
- European-based company (GDPR compliance)
- Strong open-source foundation
- Good price-to-performance ratio

## Groq

[Groq](https://groq.com/) is known for extremely fast inference speeds, delivering responses with minimal latency.

### Available Models

Groq offers optimized versions of several open models:

- **Llama 3 70B**: High-performance model
- **Llama 3 8B**: Efficient model
- **Mixtral 8x7B**: Mixture-of-experts model
- **Gemma 7B**: Google's efficient model

### Setup

1. Create an account at [Groq](https://console.groq.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "Groq" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

In the settings, you can configure:

- **API Key**: Your Groq API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P

### Pricing

Groq uses a token-based pricing model:

| Model | Input Cost (per 1M tokens) | Output Cost (per 1M tokens) |
|-------|----------------------------|------------------------------|
| Llama 3 70B | $0.70 | $1.40 |
| Mixtral 8x7B | $0.27 | $0.27 |

*Prices may change; check [Groq's pricing page](https://console.groq.com/pricing) for current rates.*

### Advantages

- Extremely fast inference speeds
- Competitive pricing
- Good selection of optimized models
- Strong performance on code tasks

## DeepInfra

[DeepInfra](https://deepinfra.com/) provides a platform for running various open-source models with optimized inference.

### Available Models

DeepInfra offers many models, including:

- **Llama 3**: Various sizes of Meta's Llama 3
- **Mistral**: Mistral's open models
- **CodeLlama**: Code-specialized models
- **And many more**

### Setup

1. Create an account at [DeepInfra](https://deepinfra.com/)
2. Generate an API key
3. In DevoxxGenie settings, select "DeepInfra" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

In the settings, you can configure:

- **API Key**: Your DeepInfra API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P
  - Max tokens

### Pricing

DeepInfra uses a token-based pricing model that varies by model.

*Check [DeepInfra's pricing page](https://deepinfra.com/pricing) for current rates.*

### Advantages

- Access to many open-source models
- Competitive pricing
- Good selection of code-specialized models
- Flexible deployment options

## DeepSeek

[DeepSeek](https://www.deepseek.com/) specializes in models with strong coding capabilities.

### Available Models

DeepSeek offers several models:

- **DeepSeek-Coder**: Specialized for coding tasks
- **DeepSeek-V2**: General-purpose model
- **DeepSeek-R1**: Advanced reasoning model

### Setup

1. Create an account at DeepSeek
2. Generate an API key
3. In DevoxxGenie settings, select "DeepSeek" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

In the settings, you can configure:

- **API Key**: Your DeepSeek API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P

### Advantages

- Excellent code generation capabilities
- Models specialized for developer workflows
- Strong understanding of programming concepts
- Good documentation quality

## OpenRouter

[OpenRouter](https://openrouter.ai/) is a unified API that provides access to many different models from various providers.

### Available Models

OpenRouter gives access to models from:

- OpenAI
- Anthropic
- Meta
- Mistral
- And many others

### Setup

1. Create an account at [OpenRouter](https://openrouter.ai/)
2. Generate an API key
3. In DevoxxGenie settings, select "OpenRouter" as the provider
4. Paste your API key
5. Select your preferred model

### Configuration

In the settings, you can configure:

- **API Key**: Your OpenRouter API key
- **Model**: Select from available models
- **Parameters**:
  - Temperature
  - Top P

### Pricing

OpenRouter passes through the costs from the underlying providers, typically with a small markup.

*Check [OpenRouter's pricing page](https://openrouter.ai/pricing) for current rates.*

### Advantages

- Single API for many different models
- Fallback options if a provider is unavailable
- Easy model comparisons
- Pay-as-you-go pricing

## Azure OpenAI

[Azure OpenAI Service](https://azure.microsoft.com/en-us/products/ai-services/openai-service) provides OpenAI models integrated with Microsoft Azure.

### Available Models

Azure OpenAI offers the same models as OpenAI:

- GPT-4
- GPT-3.5 Turbo
- And others (depending on availability in your region)

### Setup

1. Create an Azure account
2. Set up Azure OpenAI Service
3. Create a deployment and get your API details
4. In DevoxxGenie settings, select "Azure OpenAI" as the provider
5. Enter your:
   - API Key
   - Endpoint URL
   - Deployment name
   - API version

### Configuration

In the settings, you can configure:

- **API Key**: Your Azure OpenAI key
- **Endpoint**: Your Azure OpenAI endpoint
- **Deployment**: Your specific model deployment
- **API Version**: The Azure OpenAI API version
- **Parameters**:
  - Temperature
  - Top P

### Advantages

- Enterprise compliance and security
- Service level agreements (SLAs)
- Regional availability options
- Integration with other Azure services
- Potentially better rate limits for enterprise users

## Amazon Bedrock

[Amazon Bedrock](https://aws.amazon.com/bedrock) provides access to foundation models from various providers through AWS.

### Available Models

Amazon Bedrock includes models from:

- Anthropic (Claude)
- AI21 Labs (Jurassic)
- Cohere (Command)
- Meta (Llama)
- Amazon's own models (Titan)

### Setup

1. Create an AWS account
2. Set up Amazon Bedrock
3. Configure access permissions
4. In DevoxxGenie settings, select "Amazon Bedrock" as the provider
5. Enter your:
   - Access Key
   - Secret Key
   - Region
   - Model ID

### Configuration

In the settings, you can configure:

- **AWS Credentials**: Your access and secret keys
- **Region**: AWS region for Bedrock
- **Model ID**: Specific model identifier
- **Parameters**:
  - Temperature
  - Top P

### Advantages

- Enterprise-grade security and compliance
- Integration with AWS ecosystem
- Choice of multiple foundation models
- Consistent API across providers
- SLAs for enterprise workloads

## Choosing a Cloud Provider

When selecting a cloud provider, consider these factors:

### Performance Needs

- **Complex reasoning**: Anthropic Claude 3 Opus, OpenAI GPT-4
- **Code generation**: DeepSeek Coder, OpenAI GPT-4, Anthropic Claude
- **Speed priority**: Groq, OpenAI GPT-3.5 Turbo
- **Context size**: Google Gemini 1.5 Pro (1M tokens), Anthropic Claude (200K tokens)

### Cost Considerations

- **Budget-friendly**: Mistral Small, Groq
- **Pay-per-use**: All providers use token-based pricing
- **Enterprise**: Azure OpenAI, Amazon Bedrock for enterprise agreements

### Privacy and Compliance

- **Data residency**: Azure OpenAI offers regional deployment
- **GDPR focus**: Mistral (European company)
- **Enterprise controls**: Azure OpenAI, Amazon Bedrock

### Integration

- **AWS ecosystem**: Amazon Bedrock
- **Azure ecosystem**: Azure OpenAI
- **Google ecosystem**: Google Gemini

## Best Practices

### API Key Security

- Never share your API keys
- DevoxxGenie stores keys securely in IntelliJ's credential store
- Regularly rotate keys for better security
- Use environment-specific keys (dev/prod)

### Cost Management

- Monitor token usage through provider dashboards
- Use token calculation features in DevoxxGenie
- Set usage limits on your provider accounts
- Consider using RAG to reduce context size

### Performance Optimization

- Choose the right model for your task
- Use efficient prompts to reduce token usage
- Leverage streaming for better user experience
- Balance context window size with cost

### Fallback Strategies

- Configure multiple providers
- Use OpenRouter for automatic fallbacks
- Have local models as backup options
- Implement retry logic for temporary issues

## Troubleshooting

### Common Issues

#### Authentication Errors

- Verify API key is correctly entered
- Check for whitespace in copied keys
- Ensure your account has active billing
- Verify API key permissions

#### Rate Limiting

- Check your provider's rate limits
- Implement backoff and retry logic
- Consider upgrading your account tier
- Distribute requests across multiple models

#### High Latency

- Try a different provider or model
- Use streaming responses
- Optimize prompt length
- Check for provider outages

#### Cost Overruns

- Set budget alerts on your provider account
- Monitor token usage in DevoxxGenie
- Use smaller context windows when possible
- Switch to more cost-effective models for routine tasks

## Future Trends

The cloud LLM landscape is rapidly evolving:

- **Specialized models**: More code-focused models
- **Multimodal capabilities**: Increased support for images and other media
- **Lower costs**: More competition driving down prices
- **Higher performance**: Models continue to improve rapidly
- **Agentic features**: Models that can use tools and APIs

Keep your DevoxxGenie plugin updated to access new providers and models as they become available.
