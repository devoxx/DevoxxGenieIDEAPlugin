# Release Workflow
1. ASK the user for the target version number - do not assume
2. Update version in: build.gradle.kts, plugin.xml, application.properties, and any other version files
3. Skip local config files (e.g., local.properties)
4. Update plugin change-notes in plugin.xml with new version section
5. If CHANGELOG.md exists at the project root, add a new version section at the top with categorized entries (Added, Changed, Fixed, Documentation, Dependencies) based on merged PRs/commits since the last release
6. Run `./gradlew build` to verify
7. Commit with message: "chore: bump version to vX.Y.Z"
8. Push to current branch
