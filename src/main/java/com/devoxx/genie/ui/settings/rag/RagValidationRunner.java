package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.validator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs RAG validation on a background executor and reports back on the UI executor. At most one
 * run happens at a time; requests made during a run cause one more run afterwards, because
 * whatever prompted them may have changed the answer.
 */
@Slf4j
final class RagValidationRunner {

    private final Supplier<ValidationResult> validation;
    private final Executor background;
    private final Executor ui;
    private final Consumer<ValidationResult> onResult;
    private final Runnable onFailure;

    private boolean running;
    private boolean runAgain;

    RagValidationRunner(@NotNull Supplier<ValidationResult> validation,
                        @NotNull Executor background,
                        @NotNull Executor ui,
                        @NotNull Consumer<ValidationResult> onResult,
                        @NotNull Runnable onFailure) {
        this.validation = validation;
        this.background = background;
        this.ui = ui;
        this.onResult = onResult;
        this.onFailure = onFailure;
    }

    void request() {
        synchronized (this) {
            if (running) {
                runAgain = true;
                return;
            }
            running = true;
        }
        background.execute(this::run);
    }

    private void run() {
        try {
            ValidationResult result = validation.get();
            ui.execute(() -> onResult.accept(result));
        } catch (RuntimeException e) {
            log.warn("RAG validation failed", e);
            ui.execute(onFailure);
        } finally {
            finishRun();
        }
    }

    private void finishRun() {
        synchronized (this) {
            if (!runAgain) {
                running = false;
                return;
            }
            runAgain = false;
        }
        background.execute(this::run);
    }
}
