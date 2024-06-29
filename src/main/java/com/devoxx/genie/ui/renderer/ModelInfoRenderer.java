package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import java.text.DecimalFormat;
public class ModelInfoRenderer extends JPanel implements ListCellRenderer<LanguageModel> {
    private final JLabel nameLabel = new JBLabel();
    private final JLabel infoLabel = new JBLabel();

    public ModelInfoRenderer() {
        setLayout(new BorderLayout());
        add(nameLabel, BorderLayout.WEST);
        add(infoLabel, BorderLayout.EAST);
        setBorder(JBUI.Borders.empty(2));

        infoLabel.setFont(JBUI.Fonts.smallFont());
        infoLabel.setForeground(JBColor.GRAY);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends LanguageModel> list,
                                                  LanguageModel model,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (model == null) {
            nameLabel.setText("");
            infoLabel.setText("");
        } else {
            nameLabel.setText(model.getDisplayName());
            String tokenString = formatTokenCount(model.getMaxTokens());
            double cost = (model.getCostPer1MTokensInput() / 1_000_000) * model.getMaxTokens();
            if (cost <= 0.01d) {
                infoLabel.setText(String.format("%s", tokenString));
            } else {
                infoLabel.setText(String.format("%s @ %s USD", tokenString, new DecimalFormat("#.##").format(cost)));
            }
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
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
