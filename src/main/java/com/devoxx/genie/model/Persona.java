package com.devoxx.genie.model;

import com.intellij.util.xmlb.annotations.Tag;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A persona is a named system prompt (e.g. "Developer", "Reviewer", "Architect") that the
 * user can select from a dropdown in the tool window when the "Show personas" setting is
 * enabled. The selected persona's prompt replaces the default system prompt for new
 * conversations.
 */
@Tag("Persona")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Persona {
    private String name;
    private String prompt;

    @Override
    public String toString() {
        return name;
    }
}
