package com.devoxx.genie.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user-defined command callable from the chat input with the {@code /} prefix.
 *
 * <p>Renamed from {@code CustomPrompt} as part of issue #1040 to distinguish user-defined
 * <em>commands</em> (chat prompt shortcuts) from langchain4j-managed <em>skills</em>.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Command {
    private String name;
    private String prompt;
}
