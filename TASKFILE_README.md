# Changelog Automation with Taskfile

This project uses [Task](https://taskfile.dev/) to automate changelog generation for releases.

## Prerequisites

1. **Install Task**: 
   ```bash
   # macOS
   brew install go-task/tap/go-task
   
   # Or download from https://taskfile.dev/installation/
   ```

2. **Install dependencies**:
   ```bash
   # GitHub CLI
   brew install gh
   
   # jq for JSON processing
   brew install jq
   
   # python3 (usually pre-installed on macOS)
   python3 --version
   ```

3. **Authenticate with GitHub**:
   ```bash
   gh auth login
   ```

## Usage

### Generate Changelog for New Release

```bash
# Generate changelog for version 0.8.0
task generate-changelog VERSION=0.8.0

# Generate changelog with more PRs (default is 10)
task generate-changelog VERSION=0.8.0 LIMIT=20
```

This will:
1. Fetch the last 10 (or specified number) of merged PRs
2. Categorize them into Added/Fixed/Dependencies sections
3. Update `CHANGELOG.md` with the new version
4. Update `src/main/resources/META-INF/plugin.xml` change-notes section
5. Extract unique contributors

### Preview Changes

```bash
# See what changes would be included without making modifications
task preview-changes VERSION=0.8.0
```

### Clean Up

```bash
# Remove temporary files
task clean-temp
```

### Help

```bash
# Show all available tasks and examples
task help
# or just
task
```

## What Gets Updated

1. **CHANGELOG.md**: Adds a new version section with:
   - Date-stamped version header
   - Categorized changes (Added, Fixed, Dependencies)
   - PR numbers and links
   - List of contributors

2. **plugin.xml**: Updates the `<change-notes>` section with:
   - HTML-formatted list of key changes
   - Focuses on user-facing features and fixes
   - Limits dependency updates to avoid clutter

## Customization

The Taskfile can be customized by editing `Taskfile.yml`:

- Modify PR categorization rules in the `jq` filters
- Change the number of dependency updates shown in plugin.xml
- Adjust the changelog format
- Add additional tasks for related release processes

## Manual Review

After running the automation:

1. **Review CHANGELOG.md** - Ensure categorization is correct and edit descriptions if needed
2. **Review plugin.xml** - Make sure change-notes are user-friendly and properly formatted
3. **Commit changes** - The automation doesn't commit, allowing you to review first

## Troubleshooting

- **"gh not found"**: Install GitHub CLI with `brew install gh`
- **"jq not found"**: Install jq with `brew install jq`
- **"No PRs found"**: Check if you're authenticated with `gh auth status`
- **Permission issues**: Ensure you have read access to the repository