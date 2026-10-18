package com.devoxx.genie.ui.settings.websearch;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import com.intellij.ui.components.JBTextField;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

@Getter
public class WebSearchProvidersComponent extends AbstractSettingsComponent {

    @Getter
    private final JCheckBox enableWebSearchCheckbox =
            new JCheckBox("", stateService.getIsWebSearchEnabled());

    @Getter
    private final JCheckBox tavilySearchEnabledCheckBox = new JCheckBox("", stateService.isTavilySearchEnabled());

    private final JPasswordField tavilySearchApiKeyField =
            new JPasswordField(stateService.getTavilySearchKey());

    @Getter
    private final JCheckBox googleSearchEnabledCheckBox = new JCheckBox("", stateService.isGoogleSearchEnabled());
    private final JPasswordField googleSearchApiKeyField =
            new JPasswordField(stateService.getGoogleSearchKey());

    private final JPasswordField googleCSIApiKeyField =
            new JPasswordField(stateService.getGoogleCSIKey());

    private final JBIntSpinner maxSearchResults =
            new JBIntSpinner(new UINumericRange(stateService.getMaxSearchResults(), 1, 10));

    private final JBTextField testSearchQuery = new JBTextField();
    private final JButton testSearchButton = new JButton("Test search");
    private final JTextArea testSearchResults = new JTextArea(7, 40);
    private final JProgressBar testSearchProgress = new JProgressBar();

    public WebSearchProvidersComponent() {
        addListeners();
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

        // Add description
        gbc.gridy++;
        JBLabel infoLabel = new JBLabel();
        infoLabel.setText(
                "<html><body style='width: 100%;'>" +
                        "Post your prompt on the web using either Google search or Tavily search." +
                        "</body></html>");

        infoLabel.setForeground(UIUtil.getContextHelpForeground());
        infoLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(infoLabel, gbc);

        gbc.gridy++;
        addSettingRow(panel, gbc, "Enable feature", enableWebSearchCheckbox);

        addProviderSettingRow(panel, gbc, "Tavily Web Search API Key", tavilySearchEnabledCheckBox,
                createTextWithPasswordButton(tavilySearchApiKeyField, "https://app.tavily.com/home"));

        addProviderSettingRow(panel, gbc, "Google Web Search API Key", googleSearchEnabledCheckBox,
                createTextWithPasswordButton(googleSearchApiKeyField, "https://developers.google.com/custom-search/docs/paid_element#api_key"));

        addSettingRow(panel, gbc, "Google Custom Search Engine ID",
                createTextWithPasswordButton(googleCSIApiKeyField, "https://programmablesearchengine.google.com/controlpanel/create"));

        addSettingRow(panel, gbc, "Max search results", maxSearchResults);

        addTestSearchSection(gbc);

        return panel;
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

    private void runTestSearch() {
        String query = testSearchQuery.getText().trim();
        boolean tavily = tavilySearchEnabledCheckBox.isSelected();
        boolean google = googleSearchEnabledCheckBox.isSelected();
        String apiKey = new String((tavily ? tavilySearchApiKeyField : googleSearchApiKeyField).getPassword()).trim();
        String csi = new String(googleCSIApiKeyField.getPassword()).trim();
        int limit = maxSearchResults.getNumber();
        if (query.isEmpty()) {
            testSearchResults.setText("Enter a query to test web search.");
            return;
        }
        if ((!tavily && !google) || apiKey.isEmpty() || (!tavily && csi.isEmpty())) {
            testSearchResults.setText("Select a provider and enter its API key (and Search Engine ID for Google).");
            return;
        }
        testSearchButton.setEnabled(false);
        testSearchProgress.setVisible(true);
        testSearchResults.setText("Searching...");
        // Snapshot the form on the EDT so newly entered credentials can be tested before Apply.
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                WebSearchEngine engine = tavily
                        ? TavilyWebSearchEngine.builder().apiKey(apiKey).build()
                        : GoogleCustomWebSearchEngine.builder().apiKey(apiKey).csi(csi).build();
                var hits = engine.search(WebSearchRequest.builder()
                        .searchTerms(query).maxResults(limit).build()).results();
                if (hits == null || hits.isEmpty()) return "No results found.";
                StringBuilder text = new StringBuilder();
                for (WebSearchOrganicResult hit : hits.subList(0, Math.min(limit, hits.size()))) {
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
            protected void done() {
                try {
                    testSearchResults.setText(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    testSearchResults.setText("Search interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    // Provider exceptions may contain request URLs or credentials.
                    testSearchResults.setText("Search failed (" + e.getCause().getClass().getSimpleName() +
                            "). Check the API key, provider settings, and network connection.");
                } finally {
                    testSearchProgress.setVisible(false);
                    testSearchButton.setEnabled(true);
                    testSearchResults.setCaretPosition(0);
                }
            }
        }.execute();
    }

    @Override
    public void addListeners() {
        enableWebSearchCheckbox.addItemListener(e -> {
            stateService.setIsWebSearchEnabled(e.getStateChange() == ItemEvent.SELECTED);
            // Disable both providers when the feature is disabled
            if (!enableWebSearchCheckbox.isSelected()) {
                tavilySearchEnabledCheckBox.setSelected(false);
                googleSearchEnabledCheckBox.setSelected(false);
                updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
                updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
            }
        });

        tavilySearchEnabledCheckBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Disable Google Search if Tavily is selected
                googleSearchEnabledCheckBox.setSelected(false);
                stateService.setGoogleSearchEnabled(false);
                updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
            }
            stateService.setTavilySearchEnabled(tavilySearchEnabledCheckBox.isSelected());
            updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
        });

        googleSearchEnabledCheckBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Disable Tavily if Google Search is selected
                tavilySearchEnabledCheckBox.setSelected(false);
                stateService.setTavilySearchEnabled(false);
                updateUrlFieldState(tavilySearchEnabledCheckBox, tavilySearchApiKeyField);
            }
            stateService.setGoogleSearchEnabled(googleSearchEnabledCheckBox.isSelected());
            updateUrlFieldState(googleSearchEnabledCheckBox, googleSearchApiKeyField);
        });
    }

    private void updateUrlFieldState(@NotNull JCheckBox checkbox,
                                     @NotNull JComponent urlComponent) {
        urlComponent.setEnabled(checkbox.isSelected());
    }
}
