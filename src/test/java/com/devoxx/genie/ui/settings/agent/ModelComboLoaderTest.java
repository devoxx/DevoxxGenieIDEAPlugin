package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.ui.ComboBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ModelComboLoaderTest {

    /** Executor that only runs tasks when the test says so. */
    private static final class ManualExecutor implements Executor {
        final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runAll() {
            Runnable r;
            while ((r = tasks.poll()) != null) {
                r.run();
            }
        }
    }

    private ComboBox<LanguageModel> combo;
    private ManualExecutor background;
    private ManualExecutor edt;
    private Function<ModelProvider, List<LanguageModel>> fetcher;
    private final AtomicReference<Thread> fetchThread = new AtomicReference<>();
    private ModelComboLoader loader;

    private static LanguageModel model(ModelProvider p, String name) {
        return LanguageModel.builder().provider(p).modelName(name).displayName(name).build();
    }

    @BeforeEach
    void setUp() {
        combo = new ComboBox<>();
        background = new ManualExecutor();
        edt = new ManualExecutor();
        fetcher = p -> {
            fetchThread.set(Thread.currentThread());
            return p == ModelProvider.OpenRouter
                    ? List.of(model(p, "b"), model(p, "a"))
                    : List.of(model(p, "x"), model(p, "y"));
        };
        loader = new ModelComboLoader(combo, background, edt, fetcher);
    }

    @Test
    void nonRouter_loadsSynchronously_asToday() {
        loader.load(ModelProvider.Ollama, "y");

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(combo.isEnabled()).isTrue();
        assertThat(loader.effectiveModelName()).isEqualTo("y");
        assertThat(background.tasks).isEmpty();
    }

    @Test
    void router_doesNotFetchOnCallingThread() {
        loader.load(ModelProvider.OpenRouter, "a");

        assertThat(fetchThread.get()).isNull();
        assertThat(combo.getItemCount()).isZero();
        assertThat(background.tasks).hasSize(1);

        background.runAll();
        assertThat(fetchThread.get()).isNotNull();
    }

    @Test
    void genericLocalProvider_loadsOffTheCallingThread() {
        loader.load(ModelProvider.RapidMLX, "y");

        assertThat(fetchThread.get()).isNull();
        assertThat(combo.getItemCount()).isZero();
        assertThat(background.tasks).hasSize(1);
        assertThat(loader.effectiveModelName()).isEqualTo("y");

        background.runAll();
        edt.runAll();
        assertThat(fetchThread.get()).isNotNull();
        assertThat(((LanguageModel) combo.getSelectedItem()).getModelName()).isEqualTo("y");
    }

    @Test
    void savedModel_selectedAfterAsyncLoad() {
        loader.load(ModelProvider.OpenRouter, "b");
        background.runAll();
        edt.runAll();

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(combo.isEnabled()).isTrue();
        assertThat(((LanguageModel) combo.getSelectedItem()).getModelName()).isEqualTo("b");
        assertThat(loader.effectiveModelName()).isEqualTo("b");
    }

    @Test
    void lateResult_forPreviousProvider_discarded() {
        loader.load(ModelProvider.OpenRouter, "a");
        background.runAll();
        loader.load(ModelProvider.Ollama, null);
        edt.runAll();

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(((LanguageModel) combo.getItemAt(0)).getProvider()).isEqualTo(ModelProvider.Ollama);
    }

    @Test
    void lateResult_afterReloadOfSameRouter_onlyLatestApplies() {
        loader.load(ModelProvider.OpenRouter, "a");
        loader.load(ModelProvider.OpenRouter, "b");
        background.runAll();
        edt.runAll();

        assertThat(combo.getItemCount()).isEqualTo(2);
        assertThat(loader.effectiveModelName()).isEqualTo("b");
    }

    @Test
    void effectiveModelName_duringLoad_returnsSavedModel() {
        loader.load(ModelProvider.OpenRouter, "saved/model");

        assertThat(combo.getSelectedItem()).isNull();
        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
    }

    @Test
    void failedLoad_keepsSavedModelName_andComboUsable() {
        loader = new ModelComboLoader(combo, background, edt, p -> {
            throw new IllegalStateException("boom");
        });
        loader.load(ModelProvider.OpenRouter, "saved/model");
        background.runAll();
        edt.runAll();

        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
        assertThat(combo.isEnabled()).isTrue();
    }

    @Test
    void emptyLoad_keepsSavedModelName() {
        loader = new ModelComboLoader(combo, background, edt, p -> List.of());
        loader.load(ModelProvider.OpenRouter, "saved/model");
        background.runAll();
        edt.runAll();

        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
        assertThat(combo.isEnabled()).isTrue();
    }

    @Test
    void userChangesProvider_clearsPendingModel() {
        loader.load(ModelProvider.OpenRouter, "saved/model");
        loader.load(ModelProvider.Ollama, null);

        assertThat(loader.effectiveModelName()).isNotEqualTo("saved/model");

        loader.load(ModelProvider.OpenRouter, null);
        assertThat(loader.effectiveModelName()).isEmpty();
    }

    @Test
    void pendingModel_clearedByNoProviderAndByEmptyNonRouterLoad() {
        loader.load(ModelProvider.OpenRouter, "saved/model");
        loader.load(null, null);
        assertThat(loader.effectiveModelName()).isEmpty();

        loader = new ModelComboLoader(combo, background, edt,
                p -> p == ModelProvider.Ollama ? List.of() : fetcher.apply(p));
        loader.load(ModelProvider.OpenRouter, "saved/model");
        loader.load(ModelProvider.Ollama, null);
        assertThat(loader.effectiveModelName()).isEmpty();
    }

    @Test
    void noProvider_clearsAndDisables() {
        loader.load(ModelProvider.Ollama, "x");
        loader.load(null, null);

        assertThat(combo.getItemCount()).isZero();
        assertThat(combo.isEnabled()).isFalse();
        assertThat(loader.effectiveModelName()).isEmpty();
    }

    @Test
    void lateRouterResult_afterSwitchToNoProvider_isDiscarded() {
        loader.load(ModelProvider.OpenRouter, "a");
        background.runAll();
        loader.load(null, null);
        edt.runAll();

        assertThat(combo.getItemCount()).isZero();
        assertThat(combo.isEnabled()).isFalse();
    }

    @Test
    void sameRouterReload_newestResultWins() {
        List<List<LanguageModel>> results = List.of(
                List.of(model(ModelProvider.OpenRouter, "old")),
                List.of(model(ModelProvider.OpenRouter, "new")));
        int[] call = {0};
        loader = new ModelComboLoader(combo, background, edt, p -> results.get(call[0]++));

        loader.load(ModelProvider.OpenRouter, null);
        loader.load(ModelProvider.OpenRouter, null);
        background.runAll();
        edt.runAll();

        assertThat(combo.getItemCount()).isEqualTo(1);
        assertThat(combo.getItemAt(0).getModelName()).isEqualTo("new");
    }

    @Test
    void retrySameRouterAfterFailedLoad_keepsSavedModelName() {
        loader = new ModelComboLoader(combo, background, edt, p -> {
            throw new IllegalStateException("boom");
        });
        loader.load(ModelProvider.OpenRouter, "saved/model");
        background.runAll();
        edt.runAll();
        loader.load(ModelProvider.OpenRouter, null);

        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
    }

    @Test
    void errorInFetch_stillReEnablesCombo() {
        loader = new ModelComboLoader(combo, background, edt, p -> {
            throw new NoClassDefFoundError("x");
        });
        loader.load(ModelProvider.OpenRouter, "saved/model");
        background.runAll();
        edt.runAll();

        assertThat(combo.isEnabled()).isTrue();
        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
    }

    @Test
    void sortFailure_isHandledLikeFailedLoad() {
        class Unsortable extends LanguageModel {
            @Override
            public int compareTo(LanguageModel other) {
                throw new NumberFormatException("overflow");
            }
        }
        loader = new ModelComboLoader(combo, background, edt, p -> List.of(new Unsortable(), new Unsortable()));
        loader.load(ModelProvider.OpenRouter, "saved/model");
        background.runAll();
        edt.runAll();

        assertThat(combo.isEnabled()).isTrue();
        assertThat(combo.getItemCount()).isZero();
        assertThat(loader.effectiveModelName()).isEqualTo("saved/model");
    }

    @Test
    void resultArrivingAfterDispose_isIgnored() {
        loader.load(ModelProvider.OpenRouter, "a");
        background.runAll();
        loader.dispose();
        edt.runAll();

        assertThat(combo.getItemCount()).isZero();
    }

    @Test
    void userSelectionDuringLoad_isNotOverwrittenByLateResult() {
        loader.load(ModelProvider.OpenRouter, "a");
        LanguageModel chosen = model(ModelProvider.OpenRouter, "b");
        combo.addItem(chosen);
        combo.setSelectedItem(chosen);
        background.runAll();
        edt.runAll();

        assertThat(loader.effectiveModelName()).isEqualTo("b");
    }
}
