package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.rag.validator.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RagValidationRunnerTest {

    private final Deque<Runnable> background = new ArrayDeque<>();
    private final Deque<Runnable> ui = new ArrayDeque<>();
    private final List<ValidationResult> results = new ArrayList<>();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger validations = new AtomicInteger();

    private final RagValidationRunner runner = new RagValidationRunner(
            () -> new ValidationResult(validations.incrementAndGet() > 0, List.of()),
            background::add, ui::add, results::add, failures::incrementAndGet);

    @Test
    void validationRunsOnTheBackgroundExecutorAndTheResultOnTheUiExecutor() {
        runner.request();

        assertThat(validations).hasValue(0);

        background.remove().run();

        assertThat(validations).hasValue(1);
        assertThat(results).isEmpty();

        ui.remove().run();

        assertThat(results).hasSize(1);
    }

    @Test
    void requestsDuringARunCauseExactlyOneMoreRun() {
        runner.request();
        runner.request();
        runner.request();

        assertThat(background).hasSize(1);

        background.remove().run();

        assertThat(background).hasSize(1);

        background.remove().run();

        assertThat(background).isEmpty();
        assertThat(validations).hasValue(2);
    }

    @Test
    void aRequestAfterARunFinishedStartsANewRun() {
        runner.request();
        background.remove().run();

        runner.request();

        assertThat(background).hasSize(1);
    }

    @Test
    void aFailedRunReportsTheFailureAndLaterRequestsStillWork() {
        RagValidationRunner failing = new RagValidationRunner(
                () -> {
                    throw new IllegalStateException("boom");
                },
                background::add, ui::add, results::add, failures::incrementAndGet);

        failing.request();
        background.remove().run();
        ui.remove().run();

        assertThat(failures).hasValue(1);
        assertThat(results).isEmpty();

        failing.request();

        assertThat(background).hasSize(1);
    }
}
