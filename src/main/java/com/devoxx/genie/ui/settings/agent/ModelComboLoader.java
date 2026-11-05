package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.cloud.router.RouterRegistry;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Fills a model combo box for a provider. Generic routers fetch their catalogue over the network,
 * so those are loaded on a pooled thread and applied on the EDT; every other provider keeps the
 * synchronous path. The settings dialog is modal, hence {@code executeOnPooledThread} +
 * {@code ModalityState.any()} rather than {@code Task.Backgroundable}.
 * <p>
 * While a router load is pending the combo is empty and disabled, and {@link #effectiveModelName()}
 * keeps reporting the saved model so Apply/isModified never see an empty selection. All methods
 * except the background fetch run on the EDT; a generation counter discards stale results.
 */
final class ModelComboLoader {

    private static final Logger LOG = Logger.getInstance(ModelComboLoader.class);

    private final ComboBox<LanguageModel> combo;
    private final Executor background;
    private final Executor edt;
    private final Function<ModelProvider, List<LanguageModel>> fetcher;

    private int generation;
    private boolean disposed;
    private @Nullable String pendingModel;

    ModelComboLoader(@NotNull ComboBox<LanguageModel> combo) {
        this(combo,
                task -> ApplicationManager.getApplication().executeOnPooledThread(task),
                task -> ApplicationManager.getApplication().invokeLater(task, ModalityState.any()));
    }

    ModelComboLoader(@NotNull ComboBox<LanguageModel> combo, @NotNull Executor background, @NotNull Executor edt) {
        this(combo, background, edt, ModelComboLoader::fetchFromFactory);
    }

    ModelComboLoader(@NotNull ComboBox<LanguageModel> combo,
                     @NotNull Executor background,
                     @NotNull Executor edt,
                     @NotNull Function<ModelProvider, List<LanguageModel>> fetcher) {
        this.combo = combo;
        this.background = background;
        this.edt = edt;
        this.fetcher = fetcher;
    }

    private static List<LanguageModel> fetchFromFactory(ModelProvider provider) {
        return ChatModelFactoryProvider.getFactoryByProvider(provider.getName())
                .map(factory -> (List<LanguageModel>) new ArrayList<>(factory.getModels()))
                .orElseGet(List::of);
    }

    /** EDT only. Replaces the combo contents; {@code modelToSelect} is the saved model, if any. */
    void load(@Nullable ModelProvider provider, @Nullable String modelToSelect) {
        int gen = ++generation;
        pendingModel = null;
        combo.removeAllItems();

        if (provider == null) {
            combo.setEnabled(false);
            return;
        }

        if (!RouterRegistry.isGeneric(provider)) {
            combo.setEnabled(true);
            populate(fetcher.apply(provider), modelToSelect);
            return;
        }

        pendingModel = modelToSelect == null || modelToSelect.isEmpty() ? null : modelToSelect;
        combo.setEnabled(false);
        background.execute(() -> {
            List<LanguageModel> models;
            try {
                models = fetcher.apply(provider);
            } catch (Exception e) {
                LOG.warn("Could not load models for " + provider.getName() + ": " + e.getMessage());
                models = List.of();
            }
            List<LanguageModel> result = models;
            edt.execute(() -> finish(gen, result));
        });
    }

    private void finish(int gen, List<LanguageModel> models) {
        if (disposed || gen != generation) {
            return;
        }
        combo.setEnabled(true);
        if (combo.getSelectedItem() != null) {
            return; // the user already chose a model; do not override it
        }
        populate(models, pendingModel);
    }

    private void populate(List<LanguageModel> models, @Nullable String modelToSelect) {
        if (models == null || models.isEmpty()) {
            return; // keep the saved name pending
        }
        List<LanguageModel> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.naturalOrder());
        sorted.forEach(combo::addItem);
        if (modelToSelect != null && !modelToSelect.isEmpty()) {
            for (LanguageModel m : sorted) {
                if (m != null && modelToSelect.equals(m.getModelName())) {
                    combo.setSelectedItem(m);
                    break;
                }
            }
        }
        pendingModel = null;
    }

    /** The selected model name, else the saved name still waiting for a load; "" when neither. */
    String effectiveModelName() {
        LanguageModel selected = (LanguageModel) combo.getSelectedItem();
        if (selected != null && selected.getModelName() != null) {
            return selected.getModelName();
        }
        return pendingModel != null ? pendingModel : "";
    }

    /** EDT only. Results arriving after this are ignored. */
    void dispose() {
        disposed = true;
        generation++;
    }
}
