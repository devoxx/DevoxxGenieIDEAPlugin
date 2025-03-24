---
sidebar_position: 1
---

# How to Contribute

DevoxxGenie is an open-source project, and we welcome contributions from the community. Whether you're fixing bugs, improving documentation, or adding new features, your help is appreciated!

## Ways to Contribute

There are many ways to contribute to DevoxxGenie:

- **Code Contributions**: Implement new features or fix bugs
- **Documentation**: Improve or extend the documentation
- **Bug Reports**: Report issues you encounter
- **Feature Requests**: Suggest new features or improvements
- **Testing**: Test the plugin across different environments
- **Community Support**: Help answer questions from other users

## Getting Started

### Prerequisites

Before you start contributing, make sure you have:

- Basic knowledge of Java
- Understanding of IntelliJ IDEA plugin development
- Git installed
- JDK 17 or later
- IntelliJ IDEA (Community or Ultimate)

### Setting Up the Development Environment

1. **Fork the Repository**

   Fork the [DevoxxGenie repository](https://github.com/devoxx/DevoxxGenieIDEAPlugin) on GitHub.

2. **Clone Your Fork**

   ```bash
   git clone https://github.com/YOUR-USERNAME/DevoxxGenieIDEAPlugin.git
   cd DevoxxGenieIDEAPlugin
   ```

3. **Set Up the Project in IntelliJ IDEA**

   - Open IntelliJ IDEA
   - Select "Open" and navigate to the cloned repository
   - Choose "Open as Project"
   - Allow the Gradle sync to complete

4. **Verify Setup**

   Run the Gradle build task to ensure everything is set up correctly:

   ```bash
   ./gradlew build
   ```

## Development Workflow

1. **Create a Branch**

   Create a branch for your changes:

   ```bash
   git checkout -b feature/your-feature-name
   ```

   Or for bugfixes:

   ```bash
   git checkout -b fix/issue-description
   ```

2. **Make Your Changes**

   Implement your changes, following the [code style guidelines](#code-style-guidelines).

3. **Run Tests**

   Run the tests to ensure everything works as expected:

   ```bash
   ./gradlew test
   ```

4. **Build the Plugin**

   Build the plugin to verify it packages correctly:

   ```bash
   ./gradlew buildPlugin
   ```

5. **Test the Plugin**

   Run the plugin in a development instance of IntelliJ:

   ```bash
   ./gradlew runIde
   ```

6. **Commit Your Changes**

   Commit your changes with a clear and descriptive commit message:

   ```bash
   git commit -am "Add feature: description of your changes"
   ```

7. **Push Your Changes**

   Push your changes to your fork:

   ```bash
   git push origin feature/your-feature-name
   ```

8. **Create a Pull Request**

   Go to the [DevoxxGenie repository](https://github.com/devoxx/DevoxxGenieIDEAPlugin) and create a pull request from your fork.

## Code Style Guidelines

DevoxxGenie follows standard Java code style conventions:

- Use 4 spaces for indentation
- Follow Java naming conventions
- Write clear, concise comments
- Include JavaDoc for public APIs
- Maximum line length of 120 characters
- Organize imports

## Pull Request Guidelines

When submitting a pull request:

1. **Link Related Issues**: Reference any related issues in your PR description
2. **Describe Your Changes**: Provide a clear description of what your changes do
3. **Include Tests**: Add tests for new features or bug fixes
4. **Update Documentation**: Update any relevant documentation
5. **Keep PRs Focused**: Each PR should address a single concern
6. **Be Responsive**: Respond to feedback and update your PR as needed

## Finding Issues to Work On

- Check the [Issues tab](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) on GitHub
- Look for issues labeled "good first issue" for beginner-friendly tasks
- Join the [GitHub Discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions) to find areas where help is needed

## Community Guidelines

- **Be Respectful**: Treat all community members with respect
- **Be Constructive**: Provide constructive feedback and suggestions
- **Be Collaborative**: Work together to improve the project
- **Be Patient**: Not all contributors have the same expertise or availability
- **Be Inclusive**: Welcome new contributors and help them get started

## Additional Resources

- [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
- [Langchain4j Documentation](https://docs.langchain4j.dev/)

Thank you for contributing to DevoxxGenie! Your efforts help make this tool better for everyone.
