package com.devoxx.genie.ui.settings.gitmerge;

public enum GitDiffMode {
    DISABLED("Disabled", "", ""),
    SIMPLE_DIFF("Simple Git Diff", "/images/simple_diff.jpg",
        """
        <html>
        <head>
            <style type="text/css">
               body {
                  font-family: 'Source Code Pro', monospace;
                  zoom: %s;
              }
              h2 {
                margin-bottom: 5px;
              }
              p {
                margin: 0;
              }
              ul {
                margin-bottom: 5px;
              }
              li {
                margin-bottom: 5px;
              }
            </style>
        </head>
        <body>
        <h3>Two-panel side-by-side comparison</h2>
        <p>Shows direct comparison between original and suggested changes</p>
        <UL>
            <LI>Left panel: Original file labeled "Original content"</LI>
            <LI>Right panel: Modified version labeled "LLM suggested"</LI>
        </UL>
        </body></html>
    """);

    private final String displayName;
    private final String iconPath;
    private final String description;

    GitDiffMode(String displayName, String iconPath, String description) {
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getDescription() {
        return description;
    }
}
