package com.devoxx.genie.ui.panel;

import com.devoxx.genie.chatmodel.ChatModelFactory;
import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIContextWindow;
import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAICost;
import com.devoxx.genie.chatmodel.local.exo.ExoChatModelFactory;
import com.devoxx.genie.util.LanguageModelSelectionUtil;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.Persona;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.models.ModelConfigService;
import com.devoxx.genie.ui.listener.LLMSettingsChangeListener;
import com.devoxx.genie.ui.listener.PersonaChangeListener;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.renderer.ModelProviderRenderer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.DevoxxGenieFontsUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.devoxx.genie.ui.component.FilteringComboBox;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;

@Slf4j
public class LlmProviderPanel extends JBPanel<LlmProviderPanel> implements LLMSettingsChangeListener, PersonaChangeListener {

    private final transient Project project;
    // Composite key for per-tab provider/model persistence (projectHash or projectHash-tabId)
    private final String stateKey;

    @Getter
    private final JPanel contentPanel = new JPanel();
    @Getter
    private final ComboBox<ModelProvider> modelProviderComboBox = new ComboBox<>() {
        @Override
        public void setSelectedItem(Object item) {
            // The null row is a menu action, never an execution provider. Keep the
            // actual provider selected so existing submission listeners remain valid.
            if (item == null && isInitializationComplete && !isUpdatingModelNames
                    && getItemCount() > 0 && getItemAt(0) == null) {
                SwingUtilities.invokeLater(LlmProviderPanel.this::showFavorites);
                return;
            }
            super.setSelectedItem(item);
        }
    };
    @Getter
    private final FilteringComboBox<LanguageModel> modelNameComboBox = new FilteringComboBox<>(
            model -> model.getDisplayName() != null ? model.getDisplayName() : model.getModelName(),
            model -> (model.getDisplayName() == null ? "" : model.getDisplayName()) + " "
                    + (model.getModelName() == null ? "" : model.getModelName()));

    @Getter
    private final ComboBox<Persona> personaComboBox = new ComboBox<>();
    private final JPanel personaPanel = new JPanel(new BorderLayout());
    private boolean isUpdatingPersonas = false;

    private JButton refreshButton;
    private final JButton favoriteButton = new JButton("☆");

    private String lastSelectedProvider = null;
    private String lastSelectedLanguageModel = null;

    private boolean isInitializationComplete = false;
    private boolean isUpdatingModelNames = false;

    // Monotonically increasing token per updateModelNamesComboBox call. Model fetches run on
    // background threads and may complete out of order (an instant cloud list vs. a slow local
    // HTTP probe); only the response belonging to the LATEST request may touch the combo.
    private final AtomicInteger modelComboGeneration = new AtomicInteger();

    /**
     * @return {@code true} while the model name combo is being repopulated programmatically
     * (provider switch, settings refresh, restore). Listeners that distinguish user actions
     * from programmatic updates can read this to suppress side-effects (e.g. analytics).
     */
    public boolean isUpdatingModelNames() {
        return isUpdatingModelNames;
    }

    public LlmProviderPanel(@NotNull Project project) {
        this(project, null);
    }

    /**
     * The conversation panel constructor.
     *
     * @param project the project instance
     * @param tabId   the tab identifier for per-tab persistence (null for legacy behavior)
     */
    public LlmProviderPanel(@NotNull Project project, String tabId) {
        super(new BorderLayout());
        this.project = project;
        this.stateKey = tabId != null
                ? project.getLocationHash() + "-" + tabId
                : project.getLocationHash();

        // Set consistent renderers and fonts for both combo boxes
        ModelProviderRenderer providerRenderer = new ModelProviderRenderer();
        modelProviderComboBox.setRenderer((list, provider, index, selected, focused) -> {
            if (provider != null || index < 0) {
                return providerRenderer.getListCellRendererComponent(list, provider, index, selected, focused);
            }
            JLabel label = new JLabel("★ Favorites");
            label.setOpaque(true);
            label.setFont(DevoxxGenieFontsUtil.getDropdownFont());
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        modelNameComboBox.setRenderer(new ModelInfoRenderer());
        
        // Set the font for the combo boxes themselves
        modelProviderComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());

        addModelProvidersToComboBox();

        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));

        JPanel providerPanel = createProviderPanel();
        toolPanel.add(providerPanel);
        toolPanel.add(Box.createVerticalStrut(5));

        JPanel modelPanel = new JPanel(new BorderLayout());
        modelPanel.add(modelNameComboBox, BorderLayout.CENTER);
        favoriteButton.setToolTipText("Add selected model to favorites");
        favoriteButton.setEnabled(false);
        favoriteButton.addActionListener(e -> toggleFavorite());
        modelPanel.add(favoriteButton, BorderLayout.EAST);
        toolPanel.add(modelPanel);

        // Persona dropdown — only visible when "Show personas" is enabled in Prompts settings
        personaComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        personaPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        personaPanel.add(personaComboBox, BorderLayout.CENTER);
        toolPanel.add(personaPanel);
        populatePersonaComboBox();
        personaComboBox.addActionListener(this::handlePersonaSelectionChange);

        Dimension comboBoxSize = new Dimension(Integer.MAX_VALUE, modelProviderComboBox.getPreferredSize().height);
        modelProviderComboBox.setMaximumSize(comboBoxSize);
        modelNameComboBox.setMaximumSize(comboBoxSize);
        personaComboBox.setMaximumSize(comboBoxSize);
        personaPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                modelProviderComboBox.getPreferredSize().height + 5));

        lastSelectedProvider = DevoxxGenieStateService.getInstance().getSelectedProvider(stateKey);
        lastSelectedLanguageModel = DevoxxGenieStateService.getInstance().getSelectedLanguageModel(stateKey);

        // restoreLastSelectedProvider() triggers asynchronous model loading and restores the
        // previously selected language model via its completion callback once the combo is
        // populated (see updateModelNamesComboBox), so no separate restore call is needed here.
        restoreLastSelectedProvider();

        modelProviderComboBox.addActionListener(this::handleModelProviderSelectionChange);
        modelNameComboBox.addActionListener(this::handleModelNameSelectionChange);

        add(toolPanel, BorderLayout.CENTER);
        isInitializationComplete = true;
    }

    /**
     * Create the LLM provider panel.
     *
     * @return the provider panel
     */
    private @NotNull JPanel createProviderPanel() {
        refreshButton = createActionButton(RefreshIcon, "Refresh models", e-> refreshModels());
        JPanel providerPanel = new JPanel(new BorderLayout());
        providerPanel.add(modelProviderComboBox, BorderLayout.CENTER);
        providerPanel.add(refreshButton, BorderLayout.EAST);
        return providerPanel;
    }

    /**
     * Add the LLM providers to combobox.
     * Only show the enabled LLM providers.
     */
    public void addModelProvidersToComboBox() {
        LLMProviderService providerService = LLMProviderService.getInstance();
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        providerService.getAvailableModelProviders().stream()
                .filter(provider -> switch (provider) {
                    case Ollama -> stateService.isOllamaEnabled();
                    case LMStudio -> stateService.isLmStudioEnabled();
                    case GPT4All -> stateService.isGpt4AllEnabled();
                    case Jan -> stateService.isJanEnabled();
                    case Nativ -> stateService.isNativEnabled();
                    case LLaMA -> stateService.isLlamaCPPEnabled();
                    case Exo -> stateService.isExoEnabled();
                    case CustomOpenAI -> stateService.isCustomOpenAIUrlEnabled();
                    case OpenAI -> stateService.isOpenAIEnabled();
                    case Mistral -> stateService.isMistralEnabled();
                    case Anthropic -> stateService.isAnthropicEnabled();
                    case Groq -> stateService.isGroqEnabled();
                    case DeepInfra -> stateService.isDeepInfraEnabled();
                    case Google -> stateService.isGoogleEnabled();
                    case DeepSeek -> stateService.isDeepSeekEnabled();
                    case OpenRouter -> stateService.isOpenRouterEnabled();
                    case Grok -> stateService.isGrokEnabled();
                    case Kimi -> stateService.isKimiEnabled();
                    case GLM -> stateService.isGlmEnabled();
                    case Nvidia -> stateService.isNvidiaEnabled();
                    case Cloudflare -> stateService.isCloudflareEnabled();
                    case AzureOpenAI -> stateService.isAzureOpenAIEnabled();
                    case Bedrock -> stateService.isAwsEnabled();
                    case CLIRunners -> true;
                    case ACPRunners -> true;
                })
                .distinct()
                .sorted(Comparator.comparing(ModelProvider::getName))
                .forEach(modelProviderComboBox::addItem);
        updateFavoritesEntry();
    }

    private void updateFavoritesEntry() {
        boolean wasUpdating = isUpdatingModelNames;
        isUpdatingModelNames = true;
        try {
            boolean hasEntry = modelProviderComboBox.getItemCount() > 0
                    && modelProviderComboBox.getItemAt(0) == null;
            boolean hasFavorites = !DevoxxGenieStateService.getInstance().getFavoriteModels().isEmpty();
            if (hasFavorites && !hasEntry) {
                modelProviderComboBox.insertItemAt(null, 0);
            } else if (!hasFavorites && hasEntry) {
                modelProviderComboBox.removeItemAt(0);
            }
            modelProviderComboBox.setVisible(modelProviderComboBox.getItemCount() > 1);
        } finally {
            isUpdatingModelNames = wasUpdating;
        }
        revalidate();
        repaint();
    }

    private void updateFavoriteButton() {
        LanguageModel model = (LanguageModel) modelNameComboBox.getSelectedItem();
        boolean valid = model != null && model.getProvider() != null && model.getModelName() != null;
        favoriteButton.setEnabled(valid);
        boolean favorite = valid && DevoxxGenieStateService.getInstance().getFavoriteModels()
                .getOrDefault(model.getProvider().name(), Collections.emptyList()).contains(model.getModelName());
        favoriteButton.setText(favorite ? "★" : "☆");
        favoriteButton.setToolTipText(favorite ? "Remove selected model from favorites" : "Add selected model to favorites");
    }

    private void toggleFavorite() {
        LanguageModel model = (LanguageModel) modelNameComboBox.getSelectedItem();
        if (model == null || model.getProvider() == null || model.getModelName() == null) return;
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        Map<String, List<String>> favorites = state.getFavoriteModels();
        List<String> names = favorites.computeIfAbsent(model.getProvider().name(), key -> new ArrayList<>());
        if (!names.remove(model.getModelName())) names.add(model.getModelName());
        if (names.isEmpty()) favorites.remove(model.getProvider().name());
        state.setFavoriteModels(favorites);
        updateFavoritesEntry();
        updateFavoriteButton();
    }

    private boolean isProviderAvailable(ModelProvider provider) {
        for (int i = 0; i < modelProviderComboBox.getItemCount(); i++) {
            if (modelProviderComboBox.getItemAt(i) == provider) return true;
        }
        return false;
    }

    private void showFavorites() {
        if (!modelProviderComboBox.isShowing()) return;
        JPopupMenu menu = new JPopupMenu();
        DevoxxGenieStateService.getInstance().getFavoriteModels().forEach((providerName, names) -> {
            ModelProvider provider;
            try {
                provider = ModelProvider.valueOf(providerName);
            } catch (IllegalArgumentException e) {
                provider = null;
            }
            ModelProvider favoriteProvider = provider;
            List<JMenuItem> items = new ArrayList<>();
            for (String name : names) {
                JMenuItem item = new JMenuItem(providerName + " — " + name + " (unavailable)");
                item.setEnabled(false);
                items.add(item);
                menu.add(item);
            }
            if (favoriteProvider == null || !isProviderAvailable(favoriteProvider)) return;
            for (int i = 0; i < names.size(); i++) {
                items.get(i).setText(providerName + " — " + names.get(i) + " (checking…)");
            }
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                List<LanguageModel> models;
                try {
                    models = ChatModelFactoryProvider.getFactoryByProvider(favoriteProvider.name())
                            .map(ChatModelFactory::getModels).orElse(Collections.emptyList());
                } catch (Exception e) {
                    log.debug("Cannot load favorite models for {}", providerName, e);
                    models = Collections.emptyList();
                }
                List<LanguageModel> available = new ArrayList<>(models);
                ApplicationManager.getApplication().invokeLater(() -> {
                    for (int i = 0; i < names.size(); i++) {
                        String name = names.get(i);
                        JMenuItem item = items.get(i);
                        boolean found = available.stream().anyMatch(model -> name.equals(model.getModelName()));
                        item.setText(providerName + " — " + name + (found ? "" : " (unavailable)"));
                        item.setEnabled(found && isProviderAvailable(favoriteProvider));
                        item.addActionListener(e -> selectFavorite(favoriteProvider, name));
                    }
                    menu.pack();
                });
            });
        });
        if (menu.getComponentCount() == 0) {
            JMenuItem empty = new JMenuItem("No favorite models");
            empty.setEnabled(false);
            menu.add(empty);
        }
        menu.show(modelProviderComboBox, 0, modelProviderComboBox.getHeight());
    }

    private void selectFavorite(ModelProvider provider, String name) {
        if (!isProviderAvailable(provider)) return;
        boolean wasUpdating = isUpdatingModelNames;
        isUpdatingModelNames = true;
        try {
            modelProviderComboBox.setSelectedItem(provider);
        } finally {
            isUpdatingModelNames = wasUpdating;
        }
        DevoxxGenieStateService.getInstance().setSelectedProvider(stateKey, provider.getName());
        int generation = modelComboGeneration.get() + 1;
        updateModelNamesComboBox(provider.getName(), () -> {
            if (generation != modelComboGeneration.get()) return;
            for (int i = 0; i < modelNameComboBox.getItemCount(); i++) {
                if (name.equals(modelNameComboBox.getItemAt(i).getModelName())) {
                    modelNameComboBox.setSelectedIndex(i);
                    break;
                }
            }
            persistSelectedModel();
            updateFavoriteButton();
        });
    }

    /**
     * Refresh the list of local models
     */
    private void refreshModels() {
        ModelProvider selectedProvider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (selectedProvider == null) {
            return;
        }

        if (isLocalProvider(selectedProvider)) {
            refreshLocalModels(selectedProvider);
        } else {
            refreshCloudModels(selectedProvider);
        }
    }

    private static boolean isLocalProvider(ModelProvider provider) {
        return provider == ModelProvider.LMStudio ||
               provider == ModelProvider.Ollama ||
               provider == ModelProvider.Jan ||
               provider == ModelProvider.Nativ ||
               provider == ModelProvider.GPT4All ||
               provider == ModelProvider.OpenRouter ||
               provider == ModelProvider.Bedrock;
    }

    private void refreshLocalModels(ModelProvider selectedProvider) {
        ApplicationManager.getApplication().invokeLater(() -> {
            refreshButton.setEnabled(false);

            ChatModelFactory factory = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                    .orElseThrow(() -> new IllegalArgumentException("No factory for provider: " + selectedProvider));
            factory.resetModels();

            // Reload asynchronously (getModels() may hit the network) and re-enable the
            // button once the combo has been repopulated.
            updateModelNamesComboBox(selectedProvider.getName(), () -> refreshButton.setEnabled(true));
        });
    }

    private void refreshCloudModels(@NonNull ModelProvider selectedProvider) {
        refreshButton.setEnabled(false);

        // Capture the "before" model names off the EDT: getModels() may hit the network
        // (e.g. Custom OpenAI probes its /models endpoint) and must never block the UI.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Set<String> beforeNames = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                    .map(f -> f.getModels().stream()
                            .map(LanguageModel::getModelName)
                            .collect(Collectors.toSet()))
                    .orElse(Collections.emptySet());

            ModelConfigService.getInstance().forceRefresh(() -> {
                ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                        .ifPresent(ChatModelFactory::resetModels);
                // Reload the combo asynchronously; re-enable the button and report the
                // diff once the (now-cached) models have been populated.
                updateModelNamesComboBox(selectedProvider.getName(), () -> {
                    refreshButton.setEnabled(true);
                    notifyModelChanges(selectedProvider, beforeNames);
                });
            });
        });
    }

    private void notifyModelChanges(@NonNull ModelProvider selectedProvider,
                                    @NonNull Set<String> beforeNames) {
        Set<String> afterNames = ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.name())
                .map(f -> f.getModels().stream()
                        .map(LanguageModel::getModelName)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        long added = afterNames.stream().filter(n -> !beforeNames.contains(n)).count();
        long removed = beforeNames.stream().filter(n -> !afterNames.contains(n)).count();

        String message;
        if (added == 0 && removed == 0) {
            message = selectedProvider.getName() + " models are up to date (" + afterNames.size() + " models).";
        } else {
            List<String> parts = new ArrayList<>();
            if (added > 0) parts.add(added + " new");
            if (removed > 0) parts.add(removed + " removed");
            message = selectedProvider.getName() + " models refreshed: " + String.join(", ", parts) + ".";
        }
        NotificationUtil.sendNotification(project, message);
    }

    /**
     * Update the model names combobox.
     */
    public void updateModelNamesComboBox(String modelProvider) {
        updateModelNamesComboBox(modelProvider, null);
    }

    /**
     * Update the model names combobox, invoking {@code onComplete} on the EDT once the combo
     * has been repopulated.
     * <p>
     * The actual {@link ChatModelFactory#getModels()} call is dispatched to a background thread
     * because it can perform blocking network I/O (e.g. the Custom OpenAI provider probes its
     * {@code /models} endpoint). Running it on the EDT froze the IDE when the endpoint was slow
     * or unreachable. Results are applied back to the combo on the EDT.
     *
     * @param modelProvider the provider name; when {@code null} nothing happens
     * @param onComplete    optional callback run on the EDT after the combo is updated
     */
    public void updateModelNamesComboBox(String modelProvider, @Nullable Runnable onComplete) {
        if (modelProvider == null) {
            return;
        }

        int generation = modelComboGeneration.incrementAndGet();

        Optional<ChatModelFactory> factoryOpt = ChatModelFactoryProvider.getFactoryByProvider(modelProvider);
        if (factoryOpt.isEmpty()) {
            applyModelsOnEdt(Collections.emptyList(), generation, onComplete);
            return;
        }

        ChatModelFactory factory = factoryOpt.get();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<LanguageModel> models;
            try {
                models = new ArrayList<>(factory.getModels());
            } catch (Exception e) {
                log.error("Error fetching models for provider {}", modelProvider, e);
                models = Collections.emptyList();
            }
            applyModelsOnEdt(models, generation, onComplete);
        });
    }

    /**
     * Apply the fetched models to the combo on the EDT. Keeps {@link #isUpdatingModelNames}
     * {@code true} while the combo is repopulated so the selection listener can distinguish
     * programmatic updates from user actions.
     * <p>
     * A response belonging to a superseded request (stale {@code generation}) is dropped so a
     * slow fetch cannot overwrite the models of a provider selected later. Its {@code onComplete}
     * still runs so callers waiting on completion (e.g. refresh button re-enable) are not stuck.
     */
    private void applyModelsOnEdt(@NotNull List<LanguageModel> models, int generation, @Nullable Runnable onComplete) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (generation != modelComboGeneration.get()) {
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            boolean wasUpdating = isUpdatingModelNames;
            isUpdatingModelNames = true;
            try {
                modelNameComboBox.removeAllItems();
                modelNameComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());
                if (models.isEmpty()) {
                    hideModelNameComboBox();
                } else {
                    List<LanguageModel> sorted = new ArrayList<>(models);
                    sorted.sort(Comparator.naturalOrder());
                    sorted.forEach(modelNameComboBox::addItem);
                }
                modelNameComboBox.setVisible(modelNameComboBox.getItemCount() > 1);
                updateFavoriteButton();
                modelNameComboBox.setRenderer(new ModelInfoRenderer());
                modelNameComboBox.revalidate();
                modelNameComboBox.repaint();
            } catch (Exception e) {
                log.error("Error updating model names", e);
                Messages.showErrorDialog(project, "Failed to update model names: " + e.getMessage(), "Error");
            } finally {
                isUpdatingModelNames = wasUpdating;
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * Hide the model name combobox.
     */
    private void hideModelNameComboBox() {
        modelNameComboBox.setVisible(false);
    }

    /**
     * Restore the last selected provider from persistent storage
     */
    public void restoreLastSelectedProvider() {
        if (lastSelectedProvider != null) {
            for (int i = 0; i < modelProviderComboBox.getItemCount(); i++) {
                ModelProvider provider = modelProviderComboBox.getItemAt(i);
                if (provider != null && provider.getName().equals(lastSelectedProvider)) {
                    modelProviderComboBox.setSelectedIndex(i);
                    // Model loading is asynchronous, so restore the previously selected
                    // language model only once the combo has been repopulated.
                    updateModelNamesComboBox(lastSelectedProvider, this::restoreLastSelectedLanguageModel);
                    break;
                }
            }
        } else {
            setLastSelectedProvider();
        }
    }

    /**
     * Restore the last selected language model from persistent storage
     */
    public void restoreLastSelectedLanguageModel() {
        if (lastSelectedLanguageModel == null || lastSelectedLanguageModel.isBlank()) {
            return;
        }
        boolean wasUpdating = isUpdatingModelNames;
        isUpdatingModelNames = true;
        try {
            for (int i = 0; i < modelNameComboBox.getItemCount(); i++) {
                LanguageModel model = modelNameComboBox.getItemAt(i);
                if (model.getModelName().equals(lastSelectedLanguageModel)) {
                    modelNameComboBox.setSelectedIndex(i);
                    return;
                }
            }
            ModelProvider provider = (ModelProvider) modelProviderComboBox.getSelectedItem();
            if (modelNameComboBox.getItemCount() == 0 || provider == ModelProvider.CustomOpenAI) {
                // The provider returned nothing (e.g. unreachable at cold start), or it is
                // Custom OpenAI whose /models endpoint may not enumerate the chosen model.
                // Re-add the persisted model so the user's selection survives an IDE restart.
                restorePersistedModelNotInList();
            } else {
                // The provider returned a healthy model list that does not contain the persisted
                // name: the saved model belongs to another provider (stale/corrupted state, e.g.
                // an Anthropic model persisted under the Ollama key) or was removed. Fall back to
                // the first available model and persist it so the inconsistent state heals.
                fallBackToFirstAvailableModel();
            }
        } finally {
            isUpdatingModelNames = wasUpdating;
            updateFavoriteButton();
        }
    }

    /**
     * Select the first model of the freshly fetched list and persist it, replacing a persisted
     * model name that the current provider does not offer.
     */
    private void fallBackToFirstAvailableModel() {
        modelNameComboBox.setSelectedIndex(0);
        LanguageModel first = modelNameComboBox.getItemAt(0);
        lastSelectedLanguageModel = first.getModelName();
        DevoxxGenieStateService.getInstance().setSelectedLanguageModel(stateKey, first.getModelName());
    }

    /**
     * Re-add the persisted language model to the combo when the provider did not return it,
     * so the previous selection is preserved across restarts. A minimal {@link LanguageModel}
     * is synthesised from the persisted name and current provider.
     */
    private void restorePersistedModelNotInList() {
        ModelProvider provider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (provider == null) {
            return;
        }
        LanguageModel restored = synthesizePersistedModel(provider, lastSelectedLanguageModel);
        modelNameComboBox.addItem(restored);
        modelNameComboBox.setSelectedItem(restored);
        modelNameComboBox.setVisible(modelNameComboBox.getItemCount() > 1);
    }

    /**
     * Build the stand-in {@link LanguageModel} for a persisted model the provider did not return.
     *
     * <p>For Custom OpenAI the context window and costs are settings, not properties of the
     * {@code /models} response, so they are resolved here as well. Leaving the context window at 0
     * hid the conversation context indicator entirely for users whose endpoint does not enumerate
     * their configured model.</p>
     */
    static @NotNull LanguageModel synthesizePersistedModel(@NotNull ModelProvider provider,
                                                           String modelName) {
        LanguageModel.LanguageModelBuilder builder = LanguageModel.builder()
                .provider(provider)
                .modelName(modelName)
                .displayName(modelName);
        if (provider == ModelProvider.CustomOpenAI) {
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
            builder.inputCost(CustomOpenAICost.resolve(state.getCustomOpenAIInputCost()))
                    .outputCost(CustomOpenAICost.resolve(state.getCustomOpenAIOutputCost()))
                    .inputMaxTokens(CustomOpenAIContextWindow.resolve(state.getCustomOpenAIContextWindow()))
                    .apiKeyUsed(state.isCustomOpenAIApiKeyEnabled());
        }
        return builder.build();
    }

    /**
     * Re-select {@code previous} after the model combo has been repopulated (e.g. once settings
     * were applied). Matching is by model name, because the refreshed models carry the settings
     * just applied and therefore no longer {@code equals()} the instance that was selected before.
     *
     * <p>When the provider no longer offers the model it is re-added for Custom OpenAI, whose
     * {@code /models} endpoint need not enumerate the configured model; for other providers the
     * model genuinely disappeared, so the refreshed list's first entry stays selected.</p>
     */
    public void reselectModel(@Nullable LanguageModel previous) {
        if (previous == null || LanguageModelSelectionUtil.reselectByModelName(modelNameComboBox, previous)) {
            return;
        }
        ModelProvider provider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (provider == ModelProvider.CustomOpenAI && previous.getModelName() != null) {
            LanguageModel restored = synthesizePersistedModel(provider, previous.getModelName());
            modelNameComboBox.addItem(restored);
            modelNameComboBox.setSelectedItem(restored);
            modelNameComboBox.setVisible(modelNameComboBox.getItemCount() > 1);
        }
    }

    /**
     * Set the last selected LLM provider or show default.
     */
    public void setLastSelectedProvider() {
        int firstProvider = modelProviderComboBox.getItemCount() > 0
                && modelProviderComboBox.getItemAt(0) == null ? 1 : 0;
        ModelProvider modelProvider = firstProvider < modelProviderComboBox.getItemCount()
                ? modelProviderComboBox.getItemAt(firstProvider) : null;
        if (modelProvider != null) {
            DevoxxGenieStateService.getInstance().setSelectedProvider(stateKey, modelProvider.getName());
            updateModelNamesComboBox(modelProvider.getName());
        }
    }

    /**
     * (Re)populates the persona combo from settings and syncs its visibility with the
     * "Show personas" setting. The selected item is the tab's runtime selection, which
     * falls back to the configured default persona.
     */
    private void populatePersonaComboBox() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        boolean showPersonas = Boolean.TRUE.equals(state.getShowPersonas());

        isUpdatingPersonas = true;
        try {
            personaComboBox.removeAllItems();
            if (showPersonas) {
                String selectedName = state.getSelectedPersonaName(stateKey);
                Persona toSelect = null;
                for (Persona persona : state.getPersonas()) {
                    personaComboBox.addItem(persona);
                    if (persona.getName() != null && persona.getName().equalsIgnoreCase(selectedName)) {
                        toSelect = persona;
                    }
                }
                if (toSelect != null) {
                    personaComboBox.setSelectedItem(toSelect);
                }
                updatePersonaTooltip();
            }
            personaPanel.setVisible(showPersonas && personaComboBox.getItemCount() > 0);
        } finally {
            isUpdatingPersonas = false;
        }
        personaPanel.revalidate();
        personaPanel.repaint();
    }

    private void handlePersonaSelectionChange(@NotNull ActionEvent e) {
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) || isUpdatingPersonas) {
            return;
        }
        Persona selected = (Persona) personaComboBox.getSelectedItem();
        if (selected != null) {
            DevoxxGenieStateService.getInstance().setSelectedPersonaName(stateKey, selected.getName());
            updatePersonaTooltip();
        }
    }

    private void updatePersonaTooltip() {
        Persona selected = (Persona) personaComboBox.getSelectedItem();
        personaComboBox.setToolTipText(selected != null ? selected.getPrompt() : null);
    }

    @Override
    public void onPersonasChanged() {
        populatePersonaComboBox();
    }

    @Override
    public void llmSettingsChanged() {
        updateModelNamesComboBox(
                DevoxxGenieStateService.getInstance().getSelectedProvider(stateKey)
        );
    }

    /**
     * When a model is selected for the Exo provider, start preparing the instance in the background.
     */
    private void handleModelNameSelectionChange(@NotNull ActionEvent e) {
        updateFavoriteButton();
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) || !isInitializationComplete || isUpdatingModelNames) return;

        ModelProvider provider = (ModelProvider) modelProviderComboBox.getSelectedItem();
        if (provider != ModelProvider.Exo) return;

        LanguageModel selectedModel = (LanguageModel) modelNameComboBox.getSelectedItem();
        if (selectedModel != null) {
            ExoChatModelFactory.prepareInstanceAsync(selectedModel.getModelName(), project);
        }
    }

    /**
     * Process the model provider selection change.
     * Set the model provider and update the model names.
     */
    private void handleModelProviderSelectionChange(@NotNull ActionEvent e) {
        if (!e.getActionCommand().equals(Constant.COMBO_BOX_CHANGED) ||
            !isInitializationComplete ||
            isUpdatingModelNames)
            return;

        DevoxxGenieStateService stateInstance = DevoxxGenieStateService.getInstance();
        JComboBox<?> comboBox = (JComboBox<?>) e.getSource();
        ModelProvider modelProvider = (ModelProvider) comboBox.getSelectedItem();
        if (modelProvider != null) {
            stateInstance.setSelectedProvider(stateKey, modelProvider.getName());
            // Models are fetched off the EDT and applied back asynchronously (see
            // updateModelNamesComboBox), which also manages isUpdatingModelNames while it
            // repopulates the combo. This keeps the UI responsive for slow/unreachable endpoints.
            // Once the combo is repopulated, persist the resulting model together with the
            // provider: persisting only the provider would leave the previous provider's model
            // in state, which a later restart restores as a foreign model (e.g. an Anthropic
            // model under Ollama).
            updateModelNamesComboBox(modelProvider.getName(), this::persistSelectedModel);
        }
    }

    /**
     * Persist the currently selected model for this tab, or clear the persisted model when the
     * current provider has none, keeping the stored provider/model pair consistent.
     */
    private void persistSelectedModel() {
        LanguageModel selected = (LanguageModel) modelNameComboBox.getSelectedItem();
        String modelName = selected != null ? selected.getModelName() : "";
        lastSelectedLanguageModel = modelName;
        DevoxxGenieStateService.getInstance().setSelectedLanguageModel(stateKey, modelName);
    }

    /**
     * Re-populate the provider combo (e.g. after settings changed) without firing the provider
     * selection handler. Without the suppression, clearing and re-adding items auto-selects the
     * first provider, transiently persisting a wrong provider and fetching its models.
     *
     * @param providerToSelect the provider to re-select afterwards, or {@code null} to leave the
     *                         default (first) selection
     */
    public void repopulateProviders(@Nullable ModelProvider providerToSelect) {
        boolean wasUpdating = isUpdatingModelNames;
        isUpdatingModelNames = true;
        try {
            modelProviderComboBox.removeAllItems();
            modelNameComboBox.removeAllItems();
            addModelProvidersToComboBox();
            if (providerToSelect != null) {
                modelProviderComboBox.setSelectedItem(providerToSelect);
            }
        } finally {
            isUpdatingModelNames = wasUpdating;
        }
    }
}
