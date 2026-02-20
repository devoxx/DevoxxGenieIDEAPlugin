package com.devoxx.genie.ui.settings.appearance;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

import static com.devoxx.genie.ui.topic.AppTopics.APPEARANCE_SETTINGS_TOPIC;

/**
 * Settings component for controlling the appearance of the DevoxxGenie chat interface.
 */
public class AppearanceSettingsComponent extends AbstractSettingsComponent {

    public static final String SETTINGS_COMPONENTS_ARE_BEING_INITIALIZED = "Settings components are being initialized...";

    private final JSpinner lineHeightSpinner;
    private final JBIntSpinner messagePaddingSpinner;
    private final JBIntSpinner messageMarginSpinner;
    private final JBIntSpinner borderWidthSpinner;
    private final JBTextField userMessageBorderColor;
    private final JBTextField assistantMessageBorderColor;
    private final JBTextField userMessageBackgroundColor;
    private final JBTextField assistantMessageBackgroundColor;
    private final JBTextField userMessageTextColor;
    private final JBTextField assistantMessageTextColor;
    private final JCheckBox useCustomFontSize;
    private final JBIntSpinner customFontSizeSpinner;
    private final JCheckBox useCustomCodeFontSize;
    private final JBIntSpinner customCodeFontSizeSpinner;
    private final JCheckBox useRoundedCorners;
    private final JBIntSpinner cornerRadiusSpinner;
    private final JCheckBox useCustomColors;
    private final JButton resetButton;

    // Removed theme preview components

    public AppearanceSettingsComponent() {
        panel.setLayout(new BorderLayout());

        // Initialize components with values from state service
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        
        // Initialize spacing controls
        SpinnerNumberModel lineHeightModel = new SpinnerNumberModel(
                state.getLineHeight(),         // initial value
                Double.valueOf(0.1),           // minimum value
                Double.valueOf(3.0),           // maximum value
                Double.valueOf(0.1)            // step size
        );
        lineHeightSpinner = new JSpinner(lineHeightModel);

        messagePaddingSpinner = new JBIntSpinner(state.getMessagePadding(), 0, 50, 1);
        messageMarginSpinner = new JBIntSpinner(state.getMessageMargin(), 0, 50, 1);
        borderWidthSpinner = new JBIntSpinner(state.getBorderWidth(), 0, 10, 1);
        cornerRadiusSpinner = new JBIntSpinner(state.getCornerRadius(), 0, 20, 1);
        
        // Initialize color controls
        userMessageBorderColor = new JBTextField(state.getUserMessageBorderColor());
        assistantMessageBorderColor = new JBTextField(state.getAssistantMessageBorderColor());
        userMessageBackgroundColor = new JBTextField(state.getUserMessageBackgroundColor());
        assistantMessageBackgroundColor = new JBTextField(state.getAssistantMessageBackgroundColor());
        userMessageTextColor = new JBTextField(state.getUserMessageTextColor());
        assistantMessageTextColor = new JBTextField(state.getAssistantMessageTextColor());
        
        // Initialize font controls
        useCustomFontSize = new JCheckBox("Use custom font size", state.getUseCustomFontSize());
        customFontSizeSpinner = new JBIntSpinner(state.getCustomFontSize(), 8, 24, 1);
        useCustomCodeFontSize = new JCheckBox("Use custom code font size", state.getUseCustomCodeFontSize());
        customCodeFontSizeSpinner = new JBIntSpinner(state.getCustomCodeFontSize(), 8, 24, 1);
        
        // Initialize other controls
        useRoundedCorners = new JCheckBox("Use rounded corners", state.getUseRoundedCorners());
        useCustomColors = new JCheckBox("Use custom colors", state.getUseCustomColors());
        
        // Preview panels removed
        
        // Enable/disable controls based on checkbox states
        customFontSizeSpinner.setEnabled(useCustomFontSize.isSelected());
        customCodeFontSizeSpinner.setEnabled(useCustomCodeFontSize.isSelected());
        cornerRadiusSpinner.setEnabled(useRoundedCorners.isSelected());

        // Create tabbed pane for different categories
        JTabbedPane tabbedPane = new JBTabbedPane();
        
        // Create panels for each tab
        JPanel colorsPanel = createColorsPanel();
        JPanel spacingPanel = createSpacingPanel();
        JPanel fontsPanel = createFontsPanel();
        
        // Add tabs - Colors is now the first tab 
        tabbedPane.addTab("Colors", colorsPanel);
        tabbedPane.addTab("Spacing", spacingPanel);
        tabbedPane.addTab("Fonts", fontsPanel);
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        // Create bottom panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        resetButton = new JButton("Reset to Defaults");

        buttonPanel.add(resetButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    // General panel removed as its settings were moved to Spacing panel
    
    private @NotNull JPanel createColorsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Ensure color fields are initialized
        if (userMessageBorderColor != null && assistantMessageBorderColor != null &&
            userMessageBackgroundColor != null && assistantMessageBackgroundColor != null) {
            
            addSection(panel, gbc, "Theme Settings");
            
            // Add the "Use custom colors" toggle checkbox at the top
            addSettingRow(panel, gbc, "Override theme colors:", useCustomColors);
            
            // Add a separator
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            JSeparator separator = new JSeparator();
            separator.setPreferredSize(new Dimension(separator.getPreferredSize().width, 10));
            panel.add(separator, gbc);
            gbc.gridwidth = 1;
            gbc.gridy++;
            
            addSection(panel, gbc, "Message Colors");
            
            // Group user message colors
            addSettingRow(panel, gbc, "User message text color:", createColorPickerField(userMessageTextColor));
            addSettingRow(panel, gbc, "User message background color:", createColorPickerField(userMessageBackgroundColor));
            addSettingRow(panel, gbc, "User message border color:", createColorPickerField(userMessageBorderColor));
            
            // Group assistant message colors
            addSettingRow(panel, gbc, "Assistant message text color:", createColorPickerField(assistantMessageTextColor));
            addSettingRow(panel, gbc, "Assistant message background color:", createColorPickerField(assistantMessageBackgroundColor));
            addSettingRow(panel, gbc, "Assistant message border color:", createColorPickerField(assistantMessageBorderColor));

            // Add empty panel to push everything to the top
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(new JPanel(), gbc);
        } else {
            // Add a placeholder if components aren't initialized
            JLabel placeholderLabel = new JLabel(SETTINGS_COMPONENTS_ARE_BEING_INITIALIZED);
            panel.add(placeholderLabel, new GridBagConstraints());
        }
        
        return panel;
    }
    
    private @NotNull JPanel createSpacingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Ensure spacing controls are initialized
        if (lineHeightSpinner != null && messagePaddingSpinner != null && 
            messageMarginSpinner != null && borderWidthSpinner != null) {
            
            addSection(panel, gbc, "Spacing Settings");
            
            addSettingRow(panel, gbc, "Line height multiplier:", lineHeightSpinner);
            addSettingRow(panel, gbc, "Message internal padding (px):", messagePaddingSpinner);
            addSettingRow(panel, gbc, "Message external margin (px):", messageMarginSpinner);
            addSettingRow(panel, gbc, "Border width (px):", borderWidthSpinner);
            
            // Add rounded corners settings previously in General tab
            addSettingRow(panel, gbc, "Use rounded corners:", useRoundedCorners);
            addSettingRow(panel, gbc, "Corner radius (px):", cornerRadiusSpinner);
            
            // Add empty panel to push everything to the top
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(new JPanel(), gbc);
        } else {
            // Add a placeholder if components aren't initialized
            JLabel placeholderLabel = new JLabel(SETTINGS_COMPONENTS_ARE_BEING_INITIALIZED);
            panel.add(placeholderLabel, new GridBagConstraints());
        }
        
        return panel;
    }
    
    private @NotNull JPanel createFontsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Ensure font controls are initialized
        if (useCustomFontSize != null && customFontSizeSpinner != null &&
            useCustomCodeFontSize != null && customCodeFontSizeSpinner != null) {
            
            addSection(panel, gbc, "Font Settings");
            
            addSettingRow(panel, gbc, "Override editor font size:", useCustomFontSize);
            addSettingRow(panel, gbc, "Custom font size (px):", customFontSizeSpinner);
            addSettingRow(panel, gbc, "Override code font size:", useCustomCodeFontSize);
            addSettingRow(panel, gbc, "Custom code font size (px):", customCodeFontSizeSpinner);
            
            // Add empty panel to push everything to the top
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy++;
            panel.add(new JPanel(), gbc);
        } else {
            // Add a placeholder if components aren't initialized
            JLabel placeholderLabel = new JLabel(SETTINGS_COMPONENTS_ARE_BEING_INITIALIZED);
            panel.add(placeholderLabel, new GridBagConstraints());
        }
        
        return panel;
    }
    
    // Preview panel and theme preview panel methods removed
    
    private @NotNull JComponent createColorPickerField(JBTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        
        // Create a panel for the text field and color preview
        JPanel fieldWithPreview = new JPanel(new BorderLayout(5, 0));
        
        // Create color preview panel
        JPanel colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(20, 20));
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Set initial color
        try {
            Color currentColor = Color.decode(textField.getText());
            colorPreview.setBackground(currentColor);
        } catch (NumberFormatException ex) {
            colorPreview.setBackground(Color.WHITE);
        }
        
        // Add document listener to update color preview when text changes
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateColorPreview();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateColorPreview();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateColorPreview();
            }
            
            private void updateColorPreview() {
                try {
                    Color currentColor = Color.decode(textField.getText());
                    colorPreview.setBackground(currentColor);
                } catch (NumberFormatException ex) {
                    colorPreview.setBackground(Color.WHITE);
                }
            }
        });
        
        // Add components to panels
        fieldWithPreview.add(colorPreview, BorderLayout.WEST);
        fieldWithPreview.add(textField, BorderLayout.CENTER);
        
        panel.add(fieldWithPreview, BorderLayout.CENTER);
        
        // Color picker button
        JButton colorPickerButton = new JButton("...");
        colorPickerButton.addActionListener(e -> {
            Color initialColor;
            try {
                initialColor = Color.decode(textField.getText());
            } catch (NumberFormatException ex) {
                initialColor = Color.WHITE;
            }
            
            Color selectedColor = JColorChooser.showDialog(
                panel,
                "Choose Color",
                initialColor
            );
            
            if (selectedColor != null) {
                String hexColor = String.format("#%02x%02x%02x", 
                    selectedColor.getRed(), 
                    selectedColor.getGreen(), 
                    selectedColor.getBlue()
                );
                textField.setText(hexColor);
                colorPreview.setBackground(selectedColor);
            }
        });
        
        panel.add(colorPickerButton, BorderLayout.EAST);
        return panel;
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/configuration/appearance";
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        return state.getLineHeight() != lineHeightSpinner.getValue() ||
               state.getMessagePadding() != messagePaddingSpinner.getNumber() ||
               state.getMessageMargin() != messageMarginSpinner.getNumber() ||
               state.getBorderWidth() != borderWidthSpinner.getNumber() ||
               state.getCornerRadius() != cornerRadiusSpinner.getNumber() ||
               !state.getUserMessageBorderColor().equals(userMessageBorderColor.getText()) ||
               !state.getAssistantMessageBorderColor().equals(assistantMessageBorderColor.getText()) ||
               !state.getUserMessageBackgroundColor().equals(userMessageBackgroundColor.getText()) ||
               !state.getAssistantMessageBackgroundColor().equals(assistantMessageBackgroundColor.getText()) ||
               !state.getUserMessageTextColor().equals(userMessageTextColor.getText()) ||
               !state.getAssistantMessageTextColor().equals(assistantMessageTextColor.getText()) ||
               state.getUseCustomFontSize() != useCustomFontSize.isSelected() ||
               state.getCustomFontSize() != customFontSizeSpinner.getNumber() ||
               state.getUseCustomCodeFontSize() != useCustomCodeFontSize.isSelected() ||
               state.getCustomCodeFontSize() != customCodeFontSizeSpinner.getNumber() ||
               state.getUseRoundedCorners() != useRoundedCorners.isSelected() ||
               state.getUseCustomColors() != useCustomColors.isSelected();
    }
    
    public void apply() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        
        // Apply spacing settings
        state.setLineHeight((Double) lineHeightSpinner.getValue());
        state.setMessagePadding(messagePaddingSpinner.getNumber());
        state.setMessageMargin(messageMarginSpinner.getNumber());
        state.setBorderWidth(borderWidthSpinner.getNumber());
        state.setCornerRadius(cornerRadiusSpinner.getNumber());
        
        // Apply color settings
        state.setUserMessageBorderColor(userMessageBorderColor.getText());
        state.setAssistantMessageBorderColor(assistantMessageBorderColor.getText());
        state.setUserMessageBackgroundColor(userMessageBackgroundColor.getText());
        state.setAssistantMessageBackgroundColor(assistantMessageBackgroundColor.getText());
        state.setUserMessageTextColor(userMessageTextColor.getText());
        state.setAssistantMessageTextColor(assistantMessageTextColor.getText());
        
        // Apply font settings
        state.setUseCustomFontSize(useCustomFontSize.isSelected());
        state.setCustomFontSize(customFontSizeSpinner.getNumber());
        state.setUseCustomCodeFontSize(useCustomCodeFontSize.isSelected());
        state.setCustomCodeFontSize(customCodeFontSizeSpinner.getNumber());
        
        // Apply other settings
        state.setUseRoundedCorners(useRoundedCorners.isSelected());
        state.setUseCustomColors(useCustomColors.isSelected());
        
        // Notify open windows to refresh their styling
        ApplicationManager.getApplication().getMessageBus().syncPublisher(APPEARANCE_SETTINGS_TOPIC)
            .appearanceSettingsChanged();
    }
    
    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        
        // Reset spacing controls
        lineHeightSpinner.setValue(state.getLineHeight());
        messagePaddingSpinner.setNumber(state.getMessagePadding());
        messageMarginSpinner.setNumber(state.getMessageMargin());
        borderWidthSpinner.setNumber(state.getBorderWidth());
        cornerRadiusSpinner.setNumber(state.getCornerRadius());
        
        // Reset color controls
        userMessageBorderColor.setText(state.getUserMessageBorderColor());
        assistantMessageBorderColor.setText(state.getAssistantMessageBorderColor());
        userMessageBackgroundColor.setText(state.getUserMessageBackgroundColor());
        assistantMessageBackgroundColor.setText(state.getAssistantMessageBackgroundColor());
        userMessageTextColor.setText(state.getUserMessageTextColor());
        assistantMessageTextColor.setText(state.getAssistantMessageTextColor());
        
        // Reset font controls
        useCustomFontSize.setSelected(state.getUseCustomFontSize());
        customFontSizeSpinner.setNumber(state.getCustomFontSize());
        useCustomCodeFontSize.setSelected(state.getUseCustomCodeFontSize());
        customCodeFontSizeSpinner.setNumber(state.getCustomCodeFontSize());
        
        // Reset other controls
        useRoundedCorners.setSelected(state.getUseRoundedCorners());
        useCustomColors.setSelected(state.getUseCustomColors());
        
        // Update control states
        customFontSizeSpinner.setEnabled(useCustomFontSize.isSelected());
        customCodeFontSizeSpinner.setEnabled(useCustomCodeFontSize.isSelected());
        cornerRadiusSpinner.setEnabled(useRoundedCorners.isSelected());
    }
    
    @Override
    public void addListeners() {
        // Add listeners for checkbox state changes
        useCustomFontSize.addItemListener(e -> 
            customFontSizeSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
            
        useCustomCodeFontSize.addItemListener(e -> 
            customCodeFontSizeSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
            
        useRoundedCorners.addItemListener(e -> 
            cornerRadiusSpinner.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
            
        // Add listeners for button actions
        resetButton.addActionListener(e -> {
            resetToDefaultValues();
            apply();
        });
    }
    
    private void resetToDefaultValues() {
        // Reset to default values
        lineHeightSpinner.setValue(1.6);
        messagePaddingSpinner.setNumber(10);
        messageMarginSpinner.setNumber(10);
        borderWidthSpinner.setNumber(4);
        cornerRadiusSpinner.setNumber(4);
        
        // Reset border colors to Devoxx brand colors
        userMessageBorderColor.setText("#FF5400");  // Devoxx orange
        assistantMessageBorderColor.setText("#0095C9");  // Devoxx blue
        
        // Reset background and text colors based on current theme
        if (isDarkTheme()) {
            userMessageBackgroundColor.setText("#2a2520");  // Dark theme user background
            assistantMessageBackgroundColor.setText("#1e282e");  // Dark theme assistant background
            userMessageTextColor.setText("#e0e0e0");  // Light text for dark theme
            assistantMessageTextColor.setText("#e0e0e0");  // Light text for dark theme
        } else {
            userMessageBackgroundColor.setText("#fff9f0");  // Light theme user background
            assistantMessageBackgroundColor.setText("#f0f7ff");  // Light theme assistant background
            userMessageTextColor.setText("#000000");  // Dark text for light theme
            assistantMessageTextColor.setText("#000000");  // Dark text for light theme
        }
        
        // Reset font settings
        useCustomFontSize.setSelected(false);
        customFontSizeSpinner.setNumber(14);
        useCustomCodeFontSize.setSelected(false);
        customCodeFontSizeSpinner.setNumber(14);
        
        // Reset other settings
        useRoundedCorners.setSelected(true);
        useCustomColors.setSelected(false);  // Important: default to using theme-based colors
        
        // Update control states
        customFontSizeSpinner.setEnabled(useCustomFontSize.isSelected());
        customCodeFontSizeSpinner.setEnabled(useCustomCodeFontSize.isSelected());
        cornerRadiusSpinner.setEnabled(useRoundedCorners.isSelected());
    }
    
    private boolean isDarkTheme() {
        return com.devoxx.genie.ui.util.ThemeDetector.isDarkTheme();
    }
}
