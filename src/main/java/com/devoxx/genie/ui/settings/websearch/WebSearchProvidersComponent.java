package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchCredentialField;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.service.websearch.WebSearchProviderDefinition;
import com.devoxx.genie.service.websearch.WebSearchProviderRegistry;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.common.SearchTestPanel;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBColor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Settings → Web search. The provider dropdown and each provider's credential rows are generated
 * from {@link WebSearchProviderRegistry}, so a new provider needs no change here.
 *
 * <p>Except for the master switch (see {@link WebSearchProvidersConfigurable}), nothing here writes
 * to settings until Apply: Test search uses the unsaved form via {@link #formConfig()}.
 */
@Getter
public class WebSearchProvidersComponent extends AbstractSettingsComponent {

    /** One dropdown entry; {@code provider == null} is "None". */
    public record ProviderChoice(@Nullable WebSearchProvider provider, @NotNull String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    static final ProviderChoice NONE = new ProviderChoice(null, "None");

    private final JCheckBox enableWebSearchCheckbox =
            new JCheckBox("", Boolean.TRUE.equals(stateService.getIsWebSearchEnabled()));

    private final ComboBox<ProviderChoice> providerComboBox = new ComboBox<>(choices());

    /** Shown only while a deprecated provider (Google) is selected (TASK-277.4). */
    private final JBLabel deprecationWarning = new JBLabel();

    /** One password field per credential of every provider, keyed by where it is stored. */
    private final Map<CredentialKey, JPasswordField> credentialFields = new EnumMap<>(CredentialKey.class);

    /** The rows (labels, fields, hint) that belong to each provider; only the selected provider's are visible. */
    private final Map<WebSearchProvider, List<JComponent>> providerRows = new EnumMap<>(WebSearchProvider.class);

    private final JBIntSpinner maxSearchResults = new JBIntSpinner(new UINumericRange(
            stateService.getMaxSearchResults() == null ? WebSearchConfig.DEFAULT_MAX_RESULTS
                    : stateService.getMaxSearchResults(), 1, 10));

    /** Test search over the unsaved form (TASK-277.7: shared with the RAG settings' test). */
    private final SearchTestPanel<WebSearchTestSearch.Hit> testSearch =
            new SearchTestPanel<>("Test search", new WebSearchTestSearch(this::formConfig));

    public WebSearchProvidersComponent() {
        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            for (WebSearchCredentialField field : definition.fields()) {
                credentialFields.put(field.key(), new JPasswordField(stateService.getCredential(field.key())));
            }
        }
        setSelectedProvider(stateService.getSelectedWebSearchProvider());
        updateProviderRowsVisibility();
        addListeners();
    }

    private static ProviderChoice[] choices() {
        List<ProviderChoice> choices = new ArrayList<>();
        choices.add(NONE);
        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            String label = definition.deprecation() == null
                    ? definition.displayName() : definition.displayName() + " (deprecated)";
            choices.add(new ProviderChoice(definition.provider(), label));
        }
        return choices.toArray(ProviderChoice[]::new);
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/web-search";
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        addSection(panel, gbc, "Web Search Providers");

        gbc.gridy++;
        JBLabel infoLabel = new JBLabel("<html><body style='width: 100%;'>" +
                "Ground answers in live web results. Choose one provider; it is used by /search, " +
                "the agent's web_search tool and the test below." +
                "</body></html>");
        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        gbc.gridy++;
        addSettingRow(panel, gbc, "Enable feature", enableWebSearchCheckbox);
        addSettingRow(panel, gbc, "Provider", providerComboBox);

        deprecationWarning.setForeground(JBColor.RED);
        deprecationWarning.setIcon(AllIcons.General.Warning);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(deprecationWarning, gbc);
        gbc.gridy++;

        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            addProviderRows(gbc, definition);
        }
        updateProviderRowsVisibility();

        addSettingRow(panel, gbc, "Max search results", maxSearchResults);

        addTestSearchSection(gbc);

        return panel;
    }

    private void addProviderRows(@NotNull GridBagConstraints gbc, @NotNull WebSearchProviderDefinition definition) {
        List<JComponent> rows = new ArrayList<>();

        JBLabel hint = new JBLabel("<html><body style='width: 100%;'>" + definition.hint() + "</body></html>");
        hint.setForeground(UIUtil.getContextHelpForeground());
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(hint, gbc);
        gbc.gridy++;
        rows.add(hint);

        for (WebSearchCredentialField field : definition.fields()) {
            JLabel label = new JLabel(definition.displayName() + " " + field.label());
            JComponent input = createTextWithPasswordButton(credentialFields.get(field.key()), field.helpUrl());
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.weightx = 0;
            panel.add(label, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(input, gbc);
            gbc.weightx = 0;
            gbc.gridy++;
            rows.add(label);
            rows.add(input);
        }
        providerRows.put(definition.provider(), rows);
    }

    private void addTestSearchSection(GridBagConstraints gbc) {
        addSection(panel, gbc, "Test web search");
        addSettingRow(panel, gbc, "Test query", testSearch.getQueryRow());
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(testSearch, gbc);
        gbc.gridy++;
    }

    /** The test query field (used to prefill the docs screenshot). */
    public @NotNull JBTextField getTestSearchQuery() {
        return testSearch.getQueryField();
    }

    // --- form state ------------------------------------------------------------------------------

    public @Nullable WebSearchProvider getSelectedProvider() {
        Object selected = providerComboBox.getSelectedItem();
        return selected instanceof ProviderChoice choice ? choice.provider() : null;
    }

    public void setSelectedProvider(@Nullable WebSearchProvider provider) {
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            if (providerComboBox.getItemAt(i).provider() == provider) {
                providerComboBox.setSelectedIndex(i);
                return;
            }
        }
        providerComboBox.setSelectedItem(NONE);
    }

    public @NotNull JPasswordField getCredentialField(@NotNull CredentialKey key) {
        JPasswordField field = credentialFields.get(key);
        if (field == null) {
            throw new IllegalArgumentException("No web search credential field for " + key);
        }
        return field;
    }

    /** The settings exactly as currently typed in the form (not yet applied). */
    public @NotNull WebSearchConfig formConfig() {
        WebSearchProvider provider = getSelectedProvider();
        Map<CredentialKey, String> credentials = new EnumMap<>(CredentialKey.class);
        if (provider != null) {
            for (WebSearchCredentialField field : WebSearchService.definition(provider).fields()) {
                credentials.put(field.key(), new String(getCredentialField(field.key()).getPassword()));
            }
        }
        return new WebSearchConfig(provider, credentials, maxSearchResults.getNumber());
    }

    /** True when the provider's rows are currently shown (for tests and the visibility rule). */
    boolean isShowingRowsOf(@NotNull WebSearchProvider provider) {
        List<JComponent> rows = providerRows.get(provider);
        return rows != null && !rows.isEmpty() && rows.get(0).isVisible();
    }

    private void updateProviderRowsVisibility() {
        WebSearchProvider selected = getSelectedProvider();
        providerRows.forEach((provider, rows) -> rows.forEach(row -> row.setVisible(provider == selected)));
        String warning = selected == null ? null : WebSearchService.deprecationWarning(selected);
        deprecationWarning.setText(warning == null ? "" : "<html><body style='width: 100%;'>" + warning + "</body></html>");
        deprecationWarning.setVisible(warning != null);
        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void addListeners() {
        // The master switch is mirrored into settings immediately so the chat's Web switch follows it
        // (WebSearchProvidersConfigurable compares against a snapshot for that reason). The provider
        // choice is kept when the feature is switched off, so switching it back on restores it.
        enableWebSearchCheckbox.addItemListener(e ->
                stateService.setIsWebSearchEnabled(e.getStateChange() == ItemEvent.SELECTED));

        providerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateProviderRowsVisibility();
            }
        });
    }
}
