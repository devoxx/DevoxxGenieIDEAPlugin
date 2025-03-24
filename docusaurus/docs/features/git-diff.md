---
sidebar_position: 5
---

# Git Diff Integration

Starting from v0.3.0, DevoxxGenie includes Git diff/merge integration that allows you to directly review and accept LLM-generated code changes without manually copying and pasting code.

## Overview

The Git diff/merge feature:

1. Shows a side-by-side comparison of your original code and the LLM's suggested changes
2. Allows you to accept all changes at once or selectively pick specific modifications
3. Integrates with IntelliJ's built-in diff viewer
4. Makes it easier to incorporate AI suggestions into your codebase

## Enabling Git Diff Integration

To activate the Git diff feature:

1. Open DevoxxGenie settings (Settings → Tools → DevoxxGenie → LLM Git Diff View)
2. Enable "LLM Git Diff Merge"
3. Choose your preferred diff view mode (Two-panel or Three-panel)
4. Click "Apply" to save your settings

![Git Diff Settings](/img/git-diff-settings.png)

Alternatively, you can toggle Git diff directly from the DevoxxGenie window using the Git diff button in the toolbar.

## Using Git Diff

### Basic Workflow

1. Select a code snippet that you want to improve or modify
2. Ask DevoxxGenie to refactor, improve, or modify the code
3. When DevoxxGenie returns code in its response, a "Show Diff" button will appear
4. Click the button to open the diff viewer
5. Review the changes and accept those you want to incorporate

![Git Diff Viewer](/img/git-diff-viewer.png)

### Diff View Modes

DevoxxGenie supports two diff view modes:

#### Two-Panel Diff View

- Shows your original code on the left
- Shows the LLM's suggested code on the right
- Easier to understand for simple changes

#### Three-Panel Diff View

- Shows your original code on the left
- Shows the result after applying changes in the middle
- Shows the LLM's suggested code on the right
- Better for complex changes with multiple modifications

### Diff Viewer Controls

The diff viewer provides several controls:

1. **Navigation arrows**: Move between changes
2. **Accept/Reject buttons**: Apply or discard specific changes
3. **Accept All**: Apply all suggested changes at once
4. **Ignore Whitespace**: Option to ignore whitespace differences
5. **Expand/Collapse**: Expand or collapse unchanged regions

## Effective Prompts for Git Diff

To get the most out of the Git diff feature, use specific prompts that lead to well-structured code changes:

### Good Prompts for Git Diff

- "Refactor this code to use streams instead of for loops"
- "Optimize this method for better performance"
- "Convert this code to follow a builder pattern"
- "Add proper exception handling to this method"

### Less Effective Prompts

- "Make this code better" (too vague)
- "Fix all issues" (not specific enough)
- "Analyze this code" (doesn't request changes)

## Best Practices

### Code Selection

1. **Select complete units**: Choose complete methods or classes when possible
2. **Include imports**: Include necessary imports in your selection
3. **Provide context**: If needed, add context in your prompt about the purpose of the code

### Reviewing Changes

1. **Check each change**: Review all modifications carefully
2. **Look for subtle issues**: Pay attention to small changes that might affect behavior
3. **Verify functionality**: Test the code after accepting changes
4. **Consider partial acceptance**: You don't have to accept all changes

### Integration with Version Control

The Git diff feature works well with your regular Git workflow:

1. Review and accept changes from DevoxxGenie
2. Test the modified code
3. Commit the changes to your repository

## Use Cases

### Code Refactoring

Use Git diff to easily incorporate AI-suggested refactorings:

1. Select code that needs refactoring
2. Ask DevoxxGenie to improve the structure or readability
3. Review the suggested refactoring in the diff viewer
4. Accept appropriate changes

### Modernizing Legacy Code

Update older code to modern standards:

1. Select legacy code
2. Ask DevoxxGenie to update it to use newer language features or patterns
3. Review changes in the diff viewer
4. Accept modernizations that maintain the original functionality

### Adding Features

Extend existing code with new features:

1. Select a class or method
2. Ask DevoxxGenie to add specific functionality
3. Review the additions in the diff viewer
4. Accept and integrate the new features

### Bug Fixing

Address issues in your code:

1. Select problematic code
2. Ask DevoxxGenie to identify and fix issues
3. Review the proposed fixes
4. Accept corrections after verifying they address the problem

## Troubleshooting

### Diff Viewer Not Appearing

If the diff viewer doesn't appear:

- Ensure the Git diff feature is enabled in settings
- Verify that the LLM actually suggested code changes
- Check that the suggested code is properly formatted in a code block

### Changes Not Applying Correctly

If accepted changes don't apply as expected:

- Make sure your original code hasn't changed since you asked for suggestions
- Check for complex changes that might require manual intervention
- Try using the three-panel diff view for better visibility

### Formatting Issues

If code formatting is inconsistent:

- Ask DevoxxGenie to follow your project's code style
- Consider running a code formatter after accepting changes
- Use the "Ignore Whitespace" option in the diff viewer

## Limitations

Be aware of these limitations when using the Git diff feature:

1. **Single file focus**: The diff viewer works best with changes to a single file
2. **Context limitations**: The LLM may suggest changes that require broader context
3. **Code validity**: Always verify that accepted changes compile and work as expected
4. **Complex refactorings**: Very complex changes might require manual intervention

## Future Enhancements

The DevoxxGenie team is working on several enhancements to the Git diff feature:

1. **Multi-file diffs**: Support for changes across multiple files
2. **Smart merge**: More intelligent merging of complex changes
3. **Conflict resolution**: Better handling of conflicts
4. **Change grouping**: Grouping related changes for easier review
