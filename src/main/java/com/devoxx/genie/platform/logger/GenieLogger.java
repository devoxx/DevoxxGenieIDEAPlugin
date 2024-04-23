package com.devoxx.genie.platform.logger;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class GenieLogger {

    @NotNull
    private final Logger logger;

    public GenieLogger(@NotNull Class<?> theClass) {
        super();
        this.logger = com.intellij.openapi.diagnostic.Logger.getInstance(theClass);
    }

    public void debug(@NotNull String msg) {
        this.logger.debug(msg);
    }

    public void debug(@NotNull Throwable cause) {
        this.logger.debug(cause);
    }

    public void debug(@NotNull String msg, @NotNull Throwable cause) {
        this.logger.debug(msg, cause);
    }

    public void info(@NotNull String msg) {
        this.logger.info(msg);
    }

    public void info(@NotNull Throwable cause) {
        this.logger.info(cause);
    }

    public void info(@NotNull String msg, @NotNull Throwable cause) {
        this.logger.info(msg, cause);
    }

    public void warn(@NotNull String msg) {
        this.logger.warn(msg);
    }

    public void warn(@NotNull Throwable cause) {
        this.logger.warn(cause);
    }

    public void warn(@NotNull String msg, @NotNull Throwable cause) {
        this.logger.warn(msg, cause);
    }

    public void error(@NotNull String msg) {
        this.logger.error(msg);
    }

    public void error(@NotNull Throwable cause) {
        this.logger.error(cause);
    }

    public void error(@NotNull String msg, @NotNull Throwable cause) {
        this.logger.error(msg, cause);
    }
}
