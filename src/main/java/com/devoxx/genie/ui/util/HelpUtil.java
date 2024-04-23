package com.devoxx.genie.ui.util;

import java.util.ResourceBundle;

public class HelpUtil {

    private HelpUtil() {
    }

    public static String getHelpMessage(ResourceBundle resourceBundle) {
        return "<html><head><style type=\"text/css\">body { font-family: 'Source Code Pro', monospace; font-size: 14pt; margin: 5px; }</style></head><body>" +
            resourceBundle.getString("command.available") +
            "<br><ul>" +
            "<li>" + resourceBundle.getString("command.test") + "</li>" +
            "<li>" + resourceBundle.getString("command.review") + "</li>" +
            "<li>" + resourceBundle.getString("command.explain") + "</li>" +
            "<li>" + resourceBundle.getString("command.custom") + "</li>" +
            "</ul></body></html>";
    }
}
