package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class ModelInfoRenderer extends JPanel implements ListCellRenderer<LanguageModel> {

    private final JLabel nameLabel = new JBLabel();
    private final JLabel tokenLabel = new JBLabel();

    private final Map<Integer, String> tokenValues = new HashMap<>();

    public ModelInfoRenderer() {
        setLayout(new BorderLayout());
        add(nameLabel, BorderLayout.WEST);
        add(tokenLabel, BorderLayout.EAST);
        setBorder(JBUI.Borders.empty(2));

        tokenLabel.setFont(JBUI.Fonts.smallFont());

        tokenValues.put(1_000_000, "1M");
        tokenValues.put(200_000, "200K");
        tokenValues.put(100_000, "100K");
        tokenValues.put(64_000, "64K");
        tokenValues.put(32_000, "32K");
        tokenValues.put(8_000, "8K");
        tokenValues.put(4_096, "4K");
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
            String tokenString = tokenValues.getOrDefault(languageModel.getMaxTokens(), "" + languageModel.getMaxTokens());
            var cost = (languageModel.getCostPer1MTokensInput() / 1_000_000) * languageModel.getMaxTokens();
            if (cost <= 0.0) {
                tokenLabel.setText(tokenString + " tokens");
            } else {
                tokenLabel.setText(tokenString + " tokens @ " + NumberFormat.getInstance().format(cost) + " USD");
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
}
