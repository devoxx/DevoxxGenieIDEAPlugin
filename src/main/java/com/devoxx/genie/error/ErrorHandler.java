package com.devoxx.genie.error;

import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

public class ErrorHandler {

    public enum ErrorType {
        TIMEOUT,
        PROVIDER_UNAVAILABLE,
        CANCELLED,
        UNKNOWN
    }

    public static void handleError(Project project, Throwable error) {
        ErrorType errorType = categorizeError(error);
        String message = getErrorMessage(errorType);
        NotificationUtil.sendNotification(project, message);
    }

    private static ErrorType categorizeError(Throwable error) {
        if (error instanceof TimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (error instanceof CancellationException) {
            return ErrorType.CANCELLED;
        } else if (error.getMessage().contains("provider not available")) {
            return ErrorType.PROVIDER_UNAVAILABLE;
        } else {
            return ErrorType.UNKNOWN;
        }
    }

    private static String getErrorMessage(ErrorType errorType) {
        switch (errorType) {
            case TIMEOUT:
                return "Timeout occurred. Please increase the timeout setting.";
            case PROVIDER_UNAVAILABLE:
                return "LLM provider not available. Please select another provider or make sure it's running.";
            case CANCELLED:
                return "Operation cancelled.";
            case UNKNOWN:
            default:
                return "An unexpected error occurred. Please try again or check the logs.";
        }
    }
}
