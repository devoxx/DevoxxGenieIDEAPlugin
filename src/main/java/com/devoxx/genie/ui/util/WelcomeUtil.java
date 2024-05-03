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
                <p>%s
                <ul>
                <li>%s</li>
                <li>%s</li>
                <li>%s</li>
                <li>%s</li>
                </ul>
                </p>
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
