package com.devoxx.genie.error;

import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

public class ErrorHandler {

    private static final Logger LOG = Logger.getInstance(ErrorHandler.class);

    public enum ErrorType {
        TIMEOUT,
        PROVIDER_UNAVAILABLE,
        CANCELLED,
        UNKNOWN
    }

    public static void handleError(Project project, Throwable error) {
        ErrorType errorType = categorizeError(error);
        String message = getErrorMessage(errorType, error);
        NotificationUtil.sendNotification(project, message);

        // Log the full stack trace
        LOG.error("Detailed error: ", error);
    }

    private static ErrorType categorizeError(Throwable error) {
        if (error instanceof TimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (error instanceof CancellationException) {
            return ErrorType.CANCELLED;
        } else if (error.getMessage() != null && error.getMessage().contains("provider not available")) {
            return ErrorType.PROVIDER_UNAVAILABLE;
        } else {
            return ErrorType.UNKNOWN;
        }
    }

    private static @NotNull String getErrorMessage(@NotNull ErrorType errorType, Throwable error) {
        StringBuilder message = new StringBuilder();
        switch (errorType) {
            case TIMEOUT:
                message.append("Timeout occurred. Please increase the timeout setting.");
                break;
            case PROVIDER_UNAVAILABLE:
                message.append("LLM provider not available. Please select another provider or make sure it's running.");
                break;
            case CANCELLED:
                message.append("Operation cancelled.");
                break;
            case UNKNOWN:
            default:
                message.append("An unexpected error occurred: ")
                    .append(error.getClass().getSimpleName())
                    .append(" - ")
                    .append(error.getMessage());

                // Add cause if available
                if (error.getCause() != null) {
                    message.append("\nCaused by: ")
                        .append(error.getCause().getClass().getSimpleName())
                        .append(" - ")
                        .append(error.getCause().getMessage());
                }

                message.append("\nPlease check the IDE log for more details.");
        }
        return message.toString();
    }
}
