# Contributing to DevoxxGenie

Thank you for your interest in contributing to DevoxxGenie! We welcome contributions from the community.

## 📚 Documentation

Before contributing, please familiarize yourself with our [comprehensive documentation](https://genie.devoxx.com).

## Getting Started

1. **Read the docs**: Check out our [architecture overview](https://genie.devoxx.com/docs/architecture) and [developer guide](https://genie.devoxx.com/docs/contributing)
2. **Set up your environment**: Follow the [development setup guide](https://genie.devoxx.com/docs/contributing#development-setup)
3. **Find an issue**: Browse [open issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) or [discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions)

## Development Workflow

### Building the Project

```bash
./gradlew buildPlugin
```

The plugin ZIP will be created in `build/distributions/`

### Running Tests

```bash
./gradlew test
```

### Running the Plugin in Development

```bash
./gradlew runIde
```

This launches IntelliJ IDEA with the plugin installed for testing.

### Code Style

- Follow existing code conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise
- See [CLAUDE.md](CLAUDE.md) for detailed architectural guidelines

## Submitting Changes

1. **Fork the repository** and create a new branch
2. **Make your changes** with clear, descriptive commits
3. **Write or update tests** for your changes
4. **Update documentation** if you're adding/changing features
5. **Submit a pull request** with a clear description

### Pull Request Guidelines

- Link to any related issues
- Describe what your changes do and why
- Include screenshots/videos for UI changes
- Ensure all tests pass
- Update the documentation if needed

## Types of Contributions

### Bug Fixes

Found a bug? Check our [troubleshooting guide](https://genie.devoxx.com/docs/troubleshooting) first, then:
1. Search existing issues to avoid duplicates
2. Create a new issue with detailed reproduction steps
3. Submit a PR with the fix

### New Features

Want to add a feature? Please:
1. Open an issue or discussion first to discuss the approach
2. Get feedback from maintainers before implementing
3. Follow the architecture patterns in existing code
4. Update documentation

### Documentation Improvements

Documentation contributions are always welcome! Visit our [docs repository](https://github.com/devoxx/DevoxxGenieIDEAPlugin/tree/master/docusaurus) or edit pages directly on [genie.devoxx.com](https://genie.devoxx.com).

### Adding LLM Providers

To add a new LLM provider:
1. Create a factory class implementing `ChatModelFactory`
2. Place it in `chatmodel/cloud/` or `chatmodel/local/`
3. Register in `ChatModelFactoryProvider`
4. Add to `ModelProvider` enum
5. See [CLAUDE.md](CLAUDE.md) for detailed instructions

## Community

- 💬 [GitHub Discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions) - Ask questions, share ideas
- 🐛 [GitHub Issues](https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues) - Report bugs, request features
- 🐦 [Twitter/X](https://x.com/devoxxgenie) - Follow for updates
- 📖 [Documentation](https://genie.devoxx.com) - Learn more about DevoxxGenie

## Code of Conduct

Be respectful, constructive, and welcoming. We're all here to build something great together.

## Questions?

- Check our [documentation](https://genie.devoxx.com)
- Ask in [GitHub Discussions](https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions)
- Open an issue for bugs

## License

By contributing to DevoxxGenie, you agree that your contributions will be licensed under the same license as the project.

---

**Ready to contribute? We can't wait to see what you build!** 🚀
