package com.devoxx.genie.util;

import com.devoxx.genie.model.LanguageModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;

/**
 * Helpers for restoring a model selection in a combo box that has just been repopulated.
 */
public final class LanguageModelSelectionUtil {

    private LanguageModelSelectionUtil() {
    }

    /**
     * Re-select {@code previous} in {@code comboBox} by matching on the model name.
     *
     * <p>{@link LanguageModel} is a value type whose {@code equals()} covers the settings-derived
     * fields (context window, costs). Once those settings change, the previously selected instance
     * no longer equals its refreshed counterpart in the repopulated list, and
     * {@code JComboBox.setSelectedItem()} rejects an item absent from its model — leaving the
     * selection on whichever model happened to be added first. Matching on the name instead keeps
     * the user's model selected while adopting the refreshed values.</p>
     *
     * @return {@code true} when a model with the same name was found and selected; {@code false}
     * when the name is absent, in which case the current selection is left untouched and the
     * caller decides how to preserve the previous model.
     */
    public static boolean reselectByModelName(@Nullable JComboBox<LanguageModel> comboBox,
                                              @Nullable LanguageModel previous) {
        if (comboBox == null || previous == null || previous.getModelName() == null) {
            return false;
        }
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            LanguageModel item = comboBox.getItemAt(i);
            if (item != null && previous.getModelName().equals(item.getModelName())) {
                comboBox.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }
}
