---
sidebar_position: 4
title: DEVOXXGENIE.md Configuration
description: Generate and configure a DEVOXXGENIE.md file to provide project-specific context to the LLM, including build commands, code style, and project structure.
keywords: [devoxxgenie, devoxxgenie.md, project context, system prompt, project instructions, llm context, intellij plugin]
image: /img/devoxxgenie-social-card.jpg
---

# DEVOXXGENIE.md Configuration

Starting from v0.5.0, DevoxxGenie introduces the ability to generate and use a **DEVOXXGENIE.md** file in your project. This file contains project-specific information that gets incorporated into the system prompt, helping the LLM better understand your project and provide more relevant responses.

## What is DEVOXXGENIE.md?

DEVOXXGENIE.md is a Markdown file that contains structured information about your project, including:

- Project guidelines
- Build commands
- Code style preferences
- Dependencies
- Project structure

By incorporating this information into the system prompt, the LLM gains a deeper understanding of your specific project, allowing it to provide more contextually relevant and accurate assistance.

## Generating DEVOXXGENIE.md

There are two ways to generate the DEVOXXGENIE.md file:

### Method 1: From Settings

1. Open IntelliJ IDEA and go to Settings
2. Navigate to Tools > DevoxxGenie > Prompts
3. Click the "Generate DEVOXXGENIE.md" button
4. The file will be created in your project root directory

![Generate DEVOXXGENIE.md from Settings](/img/devoxxgenie-md-settings.png)

### Method 2: Using the /init Command

1. Open the DevoxxGenie chat window
2. Type `/init` in the prompt input field
3. Press Enter
4. DevoxxGenie will generate the file and confirm its creation

## Customizing DEVOXXGENIE.md

After generation, you should customize the DEVOXXGENIE.md file with specific details about your project:

1. Open the DEVOXXGENIE.md file in your editor
2. Modify the sections to accurately reflect your project:
   - Update build commands
   - Add code style guidelines
   - List important dependencies
   - Add other project-specific information

Example of a customized DEVOXXGENIE.md file:

```markdown
# DEVOXXGENIE.md

## Project Guidelines

This project follows clean code principles and uses the MVC architecture pattern.

### Build Commands

- **Build:** `./gradlew build`
- **Test:** `./gradlew test`
- **Run:** `./gradlew bootRun`

### Code Style

- Follow Google Java Style Guide
- Maximum line length: 120 characters
- Use 4 spaces for indentation
- Always use braces for control statements

### Dependencies

The project uses the following main dependencies:

- **Spring Boot** - Web application framework
- **Hibernate** - ORM for database access
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework for tests

### Project Structure

The project follows a standard Spring Boot structure:
- `src/main/java` - Java source files
- `src/main/resources` - Configuration files
- `src/test` - Test files
- Controllers are in the `controller` package
- Service interfaces and implementations are in the `service` package
- Database entities are in the `model` package
```

## How DEVOXXGENIE.md Works

When you start a conversation with DevoxxGenie, the content of DEVOXXGENIE.md is automatically included in the system prompt that sets the context for the LLM. This allows the LLM to:

1. Understand your project structure and organization
2. Know your coding standards and practices
3. Be aware of your build system and commands
4. Reference the correct dependencies and frameworks

This context helps the LLM provide more tailored and helpful responses that align with your project's specific needs and patterns.

## Best Practices for DEVOXXGENIE.md

For the most effective results:

1. **Keep it concise**: Focus on the most important information that will help the LLM understand your project
2. **Be specific**: Include project-specific details rather than generic information
3. **Update regularly**: Keep the file updated as your project evolves
4. **Include architecture insights**: Document key architectural decisions and patterns
5. **Highlight conventions**: Note any naming conventions or structural patterns the LLM should follow

## Using DEVOXXGENIE.md with Different LLM Providers

DEVOXXGENIE.md works with all supported LLM providers in DevoxxGenie, but the effectiveness may vary:

- **Cloud providers** like OpenAI, Anthropic, and Google typically handle the additional context very well
- **Local models** may have more limited context windows, so a more concise DEVOXXGENIE.md might be preferred

## Troubleshooting

If you encounter issues with DEVOXXGENIE.md:

- **File not being used**: Verify that the file is in the root of your project
- **Too much information**: If responses seem generic, your DEVOXXGENIE.md might be too verbose - try making it more focused
- **File not updating**: If changes to DEVOXXGENIE.md don't seem to be reflected in responses, try restarting a new conversation

## Future Enhancements

The DevoxxGenie team is working on enhancing the DEVOXXGENIE.md functionality:

- **Section templates**: More specialized sections for different types of projects
- **Auto-discovery**: Automatic updates based on project changes
- **Interactive editor**: A specialized editor for managing the DEVOXXGENIE.md file
