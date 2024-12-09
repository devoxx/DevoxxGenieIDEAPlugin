package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.NotNull;

<<<<<<< HEAD
=======
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
>>>>>>> master
import java.util.ResourceBundle;

public class WelcomeUtil {

<<<<<<< HEAD
    private WelcomeUtil() {
    }
=======
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
>>>>>>> master

    public static @NotNull String getWelcomeText(@NotNull ResourceBundle resourceBundle, float scaleFactor) {
        return """
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
            <h2>%s</h2>
<<<<<<< HEAD
            <small>Follow us on Bluesky : <a href="https://bsky.app/profile/devoxxgenie.bsky.social">@DevoxGenie.bsky.social</a></small>
=======
            <small>Follow us on 𝕏 : <a href="https://twitter.com/DevoxxGenie">@DevoxxGenie</a></small>
>>>>>>> master
            <p>%s</p>
            <p>%s</p>

            <h2>New features 🚀</h2>
            Configure features in the settings page.<br>
            <ul>
<<<<<<< HEAD
                 <li><strong>🧐RAG Support</strong>: Retrieval-Augmented Generation (RAG) support for automatically incorporating project context into your prompts.</li>
=======
>>>>>>> master
                 <li><strong>💪🏻Git Diff</strong>: Show Git Diff dialog to commit LLM suggestions</li>
                 <li><strong>❌.gitignore</strong>: Exclude files and directories based on .gitignore file</li>
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
            <small>BTW If you like this plugin please give us a <a href="https://plugins.jetbrains.com/plugin/24169-devoxxgenie/reviews?noRedirect=true">review</a> ❤️</small>
        </body>
        </html>
        """.formatted(
            scaleFactor == 1.0f ? "normal" : scaleFactor * 100 + "%",
            resourceBundle.getString("welcome.title"),
            resourceBundle.getString("welcome.description"),
            resourceBundle.getString("welcome.instructions"),
            HelpUtil.getCustomPromptCommands(),
            resourceBundle.getString("welcome.tip"),
            resourceBundle.getString("welcome.enjoy")
    );
    }
}
