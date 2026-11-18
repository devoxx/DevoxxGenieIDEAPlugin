package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.websearch.WebSearchConfig;
import com.devoxx.genie.service.websearch.WebSearchCredentialField;
import com.devoxx.genie.service.websearch.WebSearchProvider;
import com.devoxx.genie.service.websearch.WebSearchProviderDefinition;
import com.devoxx.genie.service.websearch.WebSearchProviderRegistry;
import com.devoxx.genie.service.websearch.WebSearchReadiness;
import com.devoxx.genie.service.websearch.WebSearchService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import dev.langchain4j.web.search.WebSearchOrganicResult;
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

    /** One password field per credential of every provider, keyed by where it is stored. */
    private final Map<CredentialKey, JPasswordField> credentialFields = new EnumMap<>(CredentialKey.class);

    /** The rows (labels, fields, hint) that belong to each provider; only the selected provider's are visible. */
    private final Map<WebSearchProvider, List<JComponent>> providerRows = new EnumMap<>(WebSearchProvider.class);

    private final JBIntSpinner maxSearchResults = new JBIntSpinner(new UINumericRange(
            stateService.getMaxSearchResults() == null ? WebSearchConfig.DEFAULT_MAX_RESULTS
                    : stateService.getMaxSearchResults(), 1, 10));

    private final JBTextField testSearchQuery = new JBTextField();
    private final JButton testSearchButton = new JButton("Test search");
    private final JTextArea testSearchResults = new JTextArea(7, 40);
    private final JProgressBar testSearchProgress = new JProgressBar();

    public WebSearchProvidersComponent() {
        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            for (WebSearchCredentialField field : definition.fields()) {
                credentialFields.put(field.key(), new JPasswordField(stateService.getCredential(field.key())));
            }
        }
        setSelectedProvider(stateService.getSelectedWebSearchProvider());
        addListeners();
    }

    private static ProviderChoice[] choices() {
        List<ProviderChoice> choices = new ArrayList<>();
        choices.add(NONE);
        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            choices.add(new ProviderChoice(definition.provider(), definition.displayName()));
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
        JPanel queryRow = new JPanel(new BorderLayout(5, 0));
        queryRow.add(testSearchQuery, BorderLayout.CENTER);
        queryRow.add(testSearchButton, BorderLayout.EAST);
        addSettingRow(panel, gbc, "Test query", queryRow);
        testSearchResults.setEditable(false);
        testSearchResults.setLineWrap(true);
        testSearchResults.setWrapStyleWord(true);
        testSearchProgress.setIndeterminate(true);
        testSearchProgress.setVisible(false);
        JPanel output = new JPanel(new BorderLayout(0, 5));
        output.add(testSearchProgress, BorderLayout.NORTH);
        output.add(new JScrollPane(testSearchResults), BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(output, gbc);
        gbc.gridy++;
        testSearchButton.addActionListener(e -> runTestSearch());
        testSearchQuery.addActionListener(e -> {
            if (testSearchButton.isEnabled()) runTestSearch();
        });
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
        panel.revalidate();
        panel.repaint();
    }

    // --- test search -----------------------------------------------------------------------------

    private void runTestSearch() {
        String query = testSearchQuery.getText().trim();
        if (query.isEmpty()) {
            testSearchResults.setText("Enter a query to test web search.");
            return;
        }
        // Snapshot the form on the EDT so newly entered credentials can be tested before Apply.
        WebSearchConfig config = formConfig();
        WebSearchReadiness readiness = WebSearchService.readiness(config);
        if (!readiness.isReady()) {
            testSearchResults.setText(readiness.message());
            return;
        }
        testSearchButton.setEnabled(false);
        testSearchProgress.setVisible(true);
        testSearchResults.setText("Searching...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return performTestSearch(config, query);
            }

            @Override
            protected void done() {
                try {
                    testSearchResults.setText(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    testSearchResults.setText("Search interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    testSearchResults.setText("Search failed. Check the provider settings and network connection.");
                } finally {
                    testSearchProgress.setVisible(false);
                    testSearchButton.setEnabled(true);
                    testSearchResults.setCaretPosition(0);
                }
            }
        }.execute();
    }

    /**
     * Runs the test search for {@code config} and renders the results as text. Never throws: failures
     * become a message. Provider exceptions may contain request URLs or credentials, so only the
     * exception type is shown.
     */
    static @NotNull String performTestSearch(@NotNull WebSearchConfig config, @NotNull String query) {
        WebSearchReadiness readiness = WebSearchService.readiness(config);
        if (!readiness.isReady()) {
            return readiness.message();
        }
        String providerName = WebSearchService.definition(config.provider()).displayName();
        List<WebSearchOrganicResult> hits;
        try {
            hits = WebSearchService.search(config, query).results();
        } catch (Exception e) {
            return "Search with " + providerName + " failed (" + e.getClass().getSimpleName() +
                    "). Check the " + providerName + " settings and network connection.";
        }
        if (hits == null || hits.isEmpty()) {
            return "No results found.";
        }
        StringBuilder text = new StringBuilder();
        for (WebSearchOrganicResult hit : hits.subList(0, Math.min(config.maxResults(), hits.size()))) {
            text.append(hit.title() == null ? "(no title)" : hit.title()).append("\n");
            if (hit.url() != null) text.append(hit.url()).append("\n");
            if (hit.snippet() != null) {
                String snippet = hit.snippet();
                text.append(snippet, 0, Math.min(snippet.length(), 1000));
                if (snippet.length() > 1000) text.append("...");
            }
            text.append("\n\n");
        }
        return text.toString();
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
