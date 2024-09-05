package com.devoxx.genie.util;

import org.jetbrains.annotations.NotNull;

public class HttpUtil {

    public static String ensureEndsWithSlash(@NotNull String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
