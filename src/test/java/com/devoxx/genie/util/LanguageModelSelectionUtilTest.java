package com.devoxx.genie.util;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageModelSelectionUtilTest {

    private static LanguageModel model(String name, int inputMaxTokens) {
        return LanguageModel.builder()
                .provider(ModelProvider.CustomOpenAI)
                .modelName(name)
                .displayName(name)
                .inputMaxTokens(inputMaxTokens)
                .build();
    }

    @Test
    void reselectKeepsTheUserModelAndPicksUpTheRefreshedContextWindow() {
        // Regression: after the Custom OpenAI context window changed, the settings-changed handler
        // re-selected the *previously selected* LanguageModel instance. LanguageModel is a Lombok
        // @Data value type whose equals() covers inputMaxTokens, so the stale instance no longer
        // equals its refreshed counterpart and the selection silently jumped to the first model.
        LanguageModel stale = model("model-b", 4096);

        JComboBox<LanguageModel> comboBox = new JComboBox<>();
        comboBox.addItem(model("model-a", 262_000));
        comboBox.addItem(model("model-b", 262_000));

        assertThat(LanguageModelSelectionUtil.reselectByModelName(comboBox, stale)).isTrue();

        LanguageModel selected = (LanguageModel) comboBox.getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("model-b");
        assertThat(selected.getInputMaxTokens()).isEqualTo(262_000);
    }

    @Test
    void plainSetSelectedItemSilentlyDropsTheStaleSelection() {
        // Documents the Swing behaviour the fix works around: a non-editable JComboBox rejects a
        // setSelectedItem() argument that is absent from its model, so the stale instance is
        // discarded and the auto-selected first model stays selected.
        LanguageModel stale = model("model-b", 4096);

        JComboBox<LanguageModel> comboBox = new JComboBox<>();
        comboBox.addItem(model("model-a", 262_000));
        comboBox.addItem(model("model-b", 262_000));
        comboBox.setSelectedItem(stale);

        LanguageModel selected = (LanguageModel) comboBox.getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("model-a");
    }

    @Test
    void reselectReportsNoMatchAndLeavesSelectionWhenTheNameIsAbsent() {
        // Custom OpenAI endpoints do not always enumerate the configured model. The util reports
        // the miss so the caller can re-add the model rather than silently swapping it.
        LanguageModel previous = model("private-model", 262_000);

        JComboBox<LanguageModel> comboBox = new JComboBox<>();
        comboBox.addItem(model("model-a", 262_000));

        assertThat(LanguageModelSelectionUtil.reselectByModelName(comboBox, previous)).isFalse();

        LanguageModel selected = (LanguageModel) comboBox.getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("model-a");
    }

    @Test
    void reselectReportsNoMatchWhenPreviousIsNull() {
        JComboBox<LanguageModel> comboBox = new JComboBox<>();
        comboBox.addItem(model("model-a", 262_000));

        assertThat(LanguageModelSelectionUtil.reselectByModelName(comboBox, null)).isFalse();

        LanguageModel selected = (LanguageModel) comboBox.getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected.getModelName()).isEqualTo("model-a");
    }
}
