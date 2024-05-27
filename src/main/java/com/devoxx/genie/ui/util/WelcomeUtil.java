package com.devoxx.genie.ui.util;

import java.util.ResourceBundle;

public class WelcomeUtil {

    public static String getWelcomeText(ResourceBundle resourceBundle) {
        return """
            <html>
            <head>
                <style type="text/css">
                    body {
                        font-family: 'Source Code Pro', monospace; font-size: 14pt;
                        margin: 5px;
                    }
                </style>
            </head>
            <body>
                <h2>%s</h2>
                <p>%s</p>
                <p>%s</p>
                <h2>New features 🚀</h2>
                Enable these new features in the settings page.<br>
                <ul>
                     <li><strong>Streaming responses (beta)</strong>: See each token as it's received from the LLM in real-time.</li>
                     <BR>
                     <li><strong>Abstract Syntax Tree (AST) context</strong>: Automatically include parent class and class/field references in the prompt for better code analysis. Ensure the LLM has a large enough context window.</li>
                     <BR>
                     <li><strong>Chat Memory Size</strong>: Set the size of your chat memory, by default its set to a total of 10 messages (system + user msgs).</li>
                </ul>
                <p>%s</p>
                <ul>
                    <li>%s</li>
                    <li>%s</li>
                    <li>%s</li>
                    <li>%s</li>
                </ul>
                <p>%s</p>
                <p>%s</p>
            </body>
            </html>
            """.formatted(
            resourceBundle.getString("welcome.title"),
            resourceBundle.getString("welcome.description"),
            resourceBundle.getString("welcome.instructions"),
            resourceBundle.getString("welcome.commands"),
            resourceBundle.getString("command.test"),
            resourceBundle.getString("command.review"),
            resourceBundle.getString("command.explain"),
            resourceBundle.getString("command.custom"),
            resourceBundle.getString("welcome.tip"),
            resourceBundle.getString("welcome.enjoy")
        );
    }
}
