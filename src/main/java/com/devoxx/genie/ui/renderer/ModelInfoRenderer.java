package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ModelInfoRenderer extends JPanel implements ListCellRenderer<LanguageModel> {

    private final JLabel nameLabel = new JBLabel();
    private final JLabel tokenLabel = new JBLabel();

    public ModelInfoRenderer() {
        setLayout(new BorderLayout());
        add(nameLabel, BorderLayout.WEST);
        add(tokenLabel, BorderLayout.EAST);
        setBorder(JBUI.Borders.empty(2));

        tokenLabel.setFont(JBUI.Fonts.smallFont());
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends LanguageModel> list,
                                                  LanguageModel languageModel,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (languageModel == null) {
            nameLabel.setText("");
            tokenLabel.setText("");
        } else {
            nameLabel.setText(languageModel.getDisplayName());
            String tokenString = formatTokenCount(languageModel.getMaxTokens());
            var cost = (languageModel.getCostPer1MTokensInput() / 1_000_000) * languageModel.getMaxTokens();
            if (cost <= 0.0) {
                tokenLabel.setText(tokenString);
            } else {
                tokenLabel.setText(String.format("%s @ %s USD", tokenString, new DecimalFormat("#.##").format(cost)));
            }
        }

        tokenLabel.setForeground(JBColor.GRAY);

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);

        return this;
    }

    private @NotNull String formatTokenCount(int tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%dM tokens", tokens / 1_000_000);
        } else if (tokens >= 1_000) {
            return String.format("%dK tokens", tokens / 1_000);
        } else {
            return String.format("%d tokens", tokens);
        }
    }
}
