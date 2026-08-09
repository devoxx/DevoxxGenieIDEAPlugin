package com.devoxx.genie.ui.listener;

/**
 * Notifies UI components that the persona settings (list, visibility or default persona)
 * changed in the Prompts settings panel.
 */
public interface PersonaChangeListener {
    void onPersonasChanged();
}
