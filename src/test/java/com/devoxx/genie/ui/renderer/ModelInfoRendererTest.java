package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

class ModelInfoRendererTest {

    private static String infoText(ModelProvider provider, double inputCost) {
        LanguageModel model = LanguageModel.builder()
                .provider(provider)
                .modelName("m")
                .displayName("m")
                .inputCost(inputCost)
                .inputMaxTokens(200_000)
                .apiKeyUsed(true)
                .build();
        ModelInfoRenderer renderer = new ModelInfoRenderer();
        Component c = renderer.getListCellRendererComponent(new JList<>(), model, 0, false, false);
        JPanel panel = (JPanel) c;
        BorderLayout layout = (BorderLayout) panel.getLayout();
        return ((JLabel) layout.getLayoutComponent(BorderLayout.EAST)).getText();
    }

    @Test
    void costIsPerMillionTokensForEveryProvider() {
        String requesty = infoText(ModelProvider.Requesty, 3.0);
        String openRouter = infoText(ModelProvider.OpenRouter, 3.0);

        assertThat(requesty).matches(".*@ 0[.,]6 USD");
        assertThat(openRouter).isEqualTo(requesty);
    }
}
