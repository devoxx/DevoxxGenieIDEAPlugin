package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

public class WelcomeUtil {

    public static @NotNull String getWelcomeText(@NotNull ResourceBundle resourceBundle) {
        return """
            <html>
            <head>
                <style type="text/css">
                    body {
                        font-family: 'Source Code Pro', monospace; font-size: 14pt;
                        margin: 5px;
                    }
                    ul {
                        list-style-type: none;
                    }
                    li {
                        margin-bottom: 10px;
                    }
                </style>
            </head>
            <body>
                <h2>%s</h2>
                <p><small>Follow us on 𝕏 : <a href="https://twitter.com/DevoxxGenie">@DevoxxGenie</a></small></p>
                <p>%s</p>
                <p>%s</p>
                <h2>New features 🚀</h2>
                Configure features in the settings page.<br>
                <ul>
                     <li><strong>👀Chat History</strong>: All chats are saved and can be restored or removed</li>
                     <li><strong>🧠Project Scanner</strong>: Add source code (full project or by package) to prompt context (or clipboard) when using Anthropic, OpenAI or Gemini.</li>
                     <li><strong>💰Token Cost Calculator</strong>: Calculate the cost when using Cloud LLM providers.  Input/Output token prices can be viewed in the Settings page.</li>
                     <li><strong>🔍Web Search</strong>: Search the web for a given query using Google or Tavily</li>
                     <li><strong>🏎️Streaming responses</strong>: See each token as it's received from the LLM in real-time</li>
                     <li><strong>💬Chat Memory Size</strong>: Set the size of your chat memory, by default its set to a total of 10 messages (system + user & AI msgs)</li>
                </ul>
                <h2>Utility Commands:</h2>
                You can update the prompts for each utility commands or add custom ones in the settings page.<br>
                <ul>
                    %s
                </ul>
                <p>%s</p>
                <p>%s</p>
                <p><small>BTW If you like this plugin please give us a <a href="https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true">review</a> ❤️</small></p>
            </body>
            </html>
            """.formatted(
            resourceBundle.getString("welcome.title"),
            resourceBundle.getString("welcome.description"),
            resourceBundle.getString("welcome.instructions"),
            HelpUtil.getCustomPromptCommands(),
            resourceBundle.getString("welcome.tip"),
            resourceBundle.getString("welcome.enjoy")
        );
    }
}
