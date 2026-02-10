---
sidebar_position: 6
title: Drag & Drop Images
description: Drag and drop images into DevoxxGenie to use multimodal LLMs like Gemini, Claude, and GPT-4V for visual code analysis, UI review, and diagram interpretation.
keywords: [devoxxgenie, drag and drop, images, multimodal, vision, gemini, claude, gpt-4v, llava, screenshot]
image: /img/devoxxgenie-social-card.jpg
---

# Drag & Drop Images

Starting with version 0.4.10, DevoxxGenie supports drag and drop images for multimodal LLMs, allowing you to include visual context in your conversations with the AI.

## Overview

The drag and drop images feature allows you to:

1. Drag images directly into the DevoxxGenie chat window
2. Include screenshots of UI, diagrams, or code
3. Ask questions about visual elements
4. Get more contextually relevant answers based on both text and images

This feature works with multimodal LLMs that can process both text and images, such as:

- Google Gemini
- Anthropic Claude (3 series and newer)
- OpenAI GPT-4V and newer
- Local models like LLaVA (via Ollama)

## Using Image Drag & Drop

### Adding Images to Your Prompts

To include an image in your conversation:

1. Drag an image file from your file explorer and drop it into the DevoxxGenie input field
2. Or take a screenshot and paste it directly into the input field (Ctrl+V or Cmd+V)
3. Type your question or prompt about the image
4. Submit the prompt

![Drag and Drop Image](/img/dnd-images.png)

### Supported Image Types

DevoxxGenie supports common image formats:

- PNG (.png)
- JPEG/JPG (.jpg, .jpeg)
- GIF (.gif, non-animated)
- WebP (.webp)
- BMP (.bmp)

### Image Size Considerations

When using images with LLMs, consider:

- **Resolution**: Higher resolution images provide more detail but use more tokens
- **File size**: Larger files may take longer to process
- **Cropping**: Crop images to focus on relevant areas
- **Compression**: Some compression is automatically applied

## Use Cases for Images

### Code Screenshots

Use image drag and drop to:

1. Share code from outside your project
2. Show code from other applications or websites
3. Ask about code that isn't text-selectable

Example prompts:
- "What does this code do? How can I improve it?"
- "Is there a bug in this code? How would you fix it?"
- "How would you refactor this to be more efficient?"

### UI and Design

Get feedback on user interfaces:

1. Drag in screenshots of your application UI
2. Ask for design recommendations
3. Identify UI/UX issues

Example prompts:
- "How can this UI be improved for better usability?"
- "Does this design follow best practices?"
- "How would you implement this UI in Java Swing?"

### Diagrams and Architecture

Discuss system architecture and diagrams:

1. Include architecture diagrams
2. Show database schemas
3. Share UML diagrams

Example prompts:
- "Explain what this architecture diagram represents"
- "How would you implement this class diagram in Java?"
- "Suggest improvements to this database schema"

### Error Messages and Logs

Get help with errors:

1. Share screenshots of error messages
2. Include log output
3. Show stack traces

Example prompts:
- "What's causing this error and how can I fix it?"
- "Help me understand this stack trace"
- "How should I debug this issue?"

## Combining Images with Code

For the best results, combine images with text and code:

1. Drag and drop an image
2. Include relevant code snippets in your prompt
3. Ask specific questions that relate both

Example combined approach:
```
Here's my current implementation:

[Image of UI]

And here's the related code:

```java
public void createUI() {
    JPanel panel = new JPanel();
    // ...
}
```

How can I improve this to better follow Material Design guidelines?
```

## Multimodal Model Support

Different LLM providers have varying levels of image support:

### Cloud Providers

- **OpenAI**: GPT-4V and GPT-4o support images
- **Anthropic**: Claude 3 Opus, Sonnet, and Haiku support images
- **Google**: All Gemini models support images

### Local Providers

- **Ollama**: Supports image input with models like LLaVA
- **Other local providers**: Image support varies by implementation

## Image Privacy Considerations

When using images with LLMs, consider:

1. **Cloud processing**: Images sent to cloud providers are processed on their servers
2. **Data retention**: Check provider policies regarding image data retention
3. **Sensitive information**: Avoid sharing images with sensitive or confidential information
4. **Local alternatives**: For maximum privacy, use local multimodal models like LLaVA through Ollama

## Best Practices

For the best results with image drag and drop:

1. **Be specific**: Ask clear questions about the image
2. **Crop appropriately**: Include only the relevant parts of the image
3. **Provide context**: Explain what the image shows and what you're looking for
4. **Use high contrast**: Ensure text in images is easily readable
5. **Combine with code**: When relevant, include both images and code snippets
6. **Test different models**: Different multimodal models have varying capabilities with images

## Troubleshooting

If you encounter issues with image drag and drop:

### Image Not Displaying in Input

- Verify the image format is supported
- Check that the file isn't too large
- Try copying and pasting instead of dragging and dropping

### Model Not Responding to Image

- Confirm you're using a multimodal-capable model
- Try a different provider or model
- Ensure your provider API key has access to multimodal models

### Poor Response Quality

- Improve image clarity and contrast
- Be more specific in your prompt
- Try breaking complex images into simpler ones
- Add more textual context about what you're asking

## Future Enhancements

The DevoxxGenie team is working on several enhancements to the image feature:

1. **Support for more image formats**
2. **Better image compression and optimization**
3. **Image annotation tools**
4. **Multi-image support in a single prompt**

Stay updated with the latest releases to access these improvements as they become available.
