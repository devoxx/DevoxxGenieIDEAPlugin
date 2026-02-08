# Release Workflow
1. ASK the user for the target version number - do not assume
2. Update version in: build.gradle.kts, plugin.xml, gradle.properties, and any other version files
3. Skip local config files (e.g., local.properties)
4. Update CHANGELOG.md with recent PRs/commits since last release
5. Update plugin history if applicable
6. Run `./gradlew build` to verify
7. Commit with message: "chore: bump version to vX.Y.Z"
8. Push to current branch
