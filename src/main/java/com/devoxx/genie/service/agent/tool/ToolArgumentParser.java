package com.devoxx.genie.service.agent.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for parsing JSON arguments from tool execution requests.
 */
public final class ToolArgumentParser {

    private ToolArgumentParser() {
    }

    @Nullable
    public static String getString(String arguments, String key) {
        try {
            JsonObject json = JsonParser.parseString(arguments).getAsJsonObject();
            JsonElement element = json.get(key);
            return element != null && !element.isJsonNull() ? element.getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean getBoolean(String arguments, String key, boolean defaultValue) {
        try {
            JsonObject json = JsonParser.parseString(arguments).getAsJsonObject();
            JsonElement element = json.get(key);
            return element != null && !element.isJsonNull() ? element.getAsBoolean() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(String arguments, String key, int defaultValue) {
        try {
            JsonObject json = JsonParser.parseString(arguments).getAsJsonObject();
            JsonElement element = json.get(key);
            return element != null && !element.isJsonNull() ? element.getAsInt() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @NotNull
    public static List<String> getStringArray(String arguments, String key) {
        try {
            JsonObject json = JsonParser.parseString(arguments).getAsJsonObject();
            JsonElement element = json.get(key);
            if (element == null || element.isJsonNull() || !element.isJsonArray()) {
                return Collections.emptyList();
            }
            JsonArray array = element.getAsJsonArray();
            List<String> result = new ArrayList<>(array.size());
            for (JsonElement item : array) {
                if (item != null && !item.isJsonNull()) {
                    result.add(item.getAsString());
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
