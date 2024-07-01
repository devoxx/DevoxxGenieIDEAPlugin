package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
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
            return this;
        }

        nameLabel.setText(model.getDisplayName());

        if (!model.getProvider().equals(ModelProvider.Ollama) &&
            !model.getProvider().equals(ModelProvider.LMStudio) &&
            !model.getProvider().equals(ModelProvider.Jan) &&
            !model.getProvider().equals(ModelProvider.GPT4All)) {
            nameLabel.setText(model.getDisplayName());
            String windowContext = WindowContextFormatterUtil.format(model.getContextWindow(), "tokens");
            double cost = (model.getInputCost() / 1_000_000) * model.getContextWindow();
            if (cost > 0.0) {
                infoLabel.setText(String.format("%s @ %s USD", windowContext, new DecimalFormat("#.##").format(cost)));
            }
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setOpaque(true);

        return this;
    }
}
