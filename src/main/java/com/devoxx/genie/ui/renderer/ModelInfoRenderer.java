package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.ui.util.WindowContextFormatterUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class ModelInfoRenderer extends JPanel implements ListCellRenderer<LanguageModel> {
    private final JLabel nameLabel = new JBLabel();
    private final JLabel infoLabel = new JBLabel();
    private final DecimalFormat decimalFormat = new DecimalFormat("#.#####");

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
            String windowContext = WindowContextFormatterUtil.format(model.getInputMaxTokens(), "tokens");
            if (model.getInputCost() > 0.0) {
                double cost;
                if (model.getProvider().getName().equalsIgnoreCase("openrouter")) {
                    cost = model.getInputCost() * model.getInputMaxTokens();
                } else {
                    cost = (model.getInputCost() / 1_000_000) * model.getInputMaxTokens();
                }
                infoLabel.setText(String.format("%s @ %s USD", windowContext, decimalFormat.format(cost)));
            } else {
                infoLabel.setText(windowContext);
            }
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        setEnabled(list.isEnabled());
        setFont(list.getFont());

        return this;
    }
}
