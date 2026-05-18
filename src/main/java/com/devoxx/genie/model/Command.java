package com.devoxx.genie.model;

import com.intellij.util.xmlb.annotations.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user-defined command callable from the chat input with the {@code /} prefix.
 *
 * <p>Renamed from {@code CustomPrompt} as part of issue #1040 to distinguish user-defined
 * <em>commands</em> (chat prompt shortcuts) from langchain4j-managed <em>skills</em>.</p>
 *
 * <p>The XML tag is pinned to {@code CustomPrompt} so that pre-#1040 persisted settings
 * (which used {@code <CustomPrompt .../>} item tags inside the {@code customPrompts}
 * collection) still deserialize into this class on upgrade. Without the alias, IntelliJ's
 * {@code XmlSerializer} matches collection items by the class's simple name, causing every
 * user-defined command to be silently dropped after the rename.</p>
 */
@Tag("CustomPrompt")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Command {
    private String name;
    private String prompt;
}
