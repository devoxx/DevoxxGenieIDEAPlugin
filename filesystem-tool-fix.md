
# DevoxxGeniePlugin Filesystem Tool Fix

## Problem Identified
When running the DevoxxGeniePlugin with the prompt "list all java files in package src/main/java/com/devoxx/genie/action using filesytem tool", the following error occurs:

```
[ERROR] env: node: No such file or directory
```

The MCP filesystem tool is called using:
```
/bin/bash -c /opt/homebrew/bin/npx -y @modelcontextprotocol/server-filesystem /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin
```

But the `node` executable can't be found. This happens because the Node.js environment path is not properly set when executing the command.

## Solution Options

### Option 1: Update the MCP server configuration with explicit PATH

1. Open the MCP settings in the IDE
2. Select the filesystem MCP server
3. Edit the environment variables
4. Add a PATH variable that includes the Node.js installation directory
   - Add key: `PATH`
   - Add value: `/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin`

### Option 2: Modify the command to use the full path to Node.js

1. Open the MCP settings in the IDE
2. Select the filesystem MCP server
3. Change the command from:
   ```
   /opt/homebrew/bin/npx
   ```
   to:
   ```
   /bin/bash
   ```
4. Change the arguments from:
   ```
   -y @modelcontextprotocol/server-filesystem /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin
   ```
   to:
   ```
   -c "PATH=/opt/homebrew/bin:$PATH npx -y @modelcontextprotocol/server-filesystem /Users/stephan/IdeaProjects/DevoxxGenieIDEAPlugin"
   ```

### Option 3: Install Node.js globally

If Node.js is only installed via Homebrew and not globally accessible, consider:

1. Install Node.js with proper symlinks to ensure it's available system-wide:
   ```
   brew link --overwrite node
   ```

2. Or install via Node.js installer from https://nodejs.org/ to ensure proper system paths are configured

## Testing the Fix

After implementing one of these solutions:

1. Restart the IDE
2. Try the prompt again: "list all java files in package src/main/java/com/devoxx/genie/action using filesytem tool"

The MCP filesystem tool should now be able to find the Node.js executable and function properly.
