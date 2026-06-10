package com.devoxx.genie.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

public final class ReadAccess {

    private ReadAccess() {
    }

    public static <T> T compute(Computable<T> computable) {
        return ApplicationManager.getApplication().runReadAction(computable);
    }

    public static void run(Runnable runnable) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }
}
