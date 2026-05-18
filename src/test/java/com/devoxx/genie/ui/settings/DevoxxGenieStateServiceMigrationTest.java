package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.Command;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML state migration tests for issue #1040: legacy {@code customPrompts} state should be
 * migrated to the renamed {@code commands} field on {@link DevoxxGenieStateService#loadState}.
 */
class DevoxxGenieStateServiceMigrationTest {

    @Test
    void legacyCustomPromptsAreMigratedToCommandsOnLoadState() throws Exception {
        // Simulate the state object IntelliJ would build from legacy persisted XML:
        // a DevoxxGenieStateService with the (now-deprecated) customPrompts field populated
        // and the new commands list empty.
        DevoxxGenieStateService persistedLegacyState = new DevoxxGenieStateService();
        setCommandsList(persistedLegacyState, "commands", new ArrayList<>());
        setCommandsList(persistedLegacyState, "customPrompts", new ArrayList<>(Arrays.asList(
                new Command("explainit", "Explain this prompt"),
                new Command("review", "Review this code")
        )));

        // Act: replay the loadState lifecycle on a fresh in-memory instance.
        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(persistedLegacyState);

        // The new field should contain the migrated values.
        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("explainit", "review");
        assertThat(target.getCommands())
                .extracting(Command::getPrompt)
                .contains("Explain this prompt", "Review this code");

        // And the legacy field should be cleared so it does not get re-serialised.
        Object legacyValue = readField(target, "customPrompts");
        assertThat(legacyValue).isNull();
    }

    @Test
    void freshStateUsesDefaultCommandsWhenNoLegacyData() {
        DevoxxGenieStateService blankState = new DevoxxGenieStateService();
        // No customPrompts, no commands -> initialize defaults from default prompts.

        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(blankState);

        // Default prompts should be populated (REVIEW_COMMAND etc.).
        assertThat(target.getCommands()).isNotEmpty();
        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("explain", "review", "test");
    }

    @Test
    void commandsAreSerializedAsCommandsAndRoundTrip() {
        DevoxxGenieStateService source = new DevoxxGenieStateService();
        source.setCommands(new ArrayList<>(Arrays.asList(
                new Command("ralph-runners", "You are a product manager. Task: $ARGUMENT")
        )));

        // Serialize to XML.
        Element xml = XmlSerializer.serialize(source);
        String xmlString = JDOMUtil.writeElement(xml);

        // Should serialize the field under the new name and not the legacy name.
        assertThat(xmlString).contains("name=\"commands\"");
        assertThat(xmlString).doesNotContain("name=\"customPrompts\"");

        // Deserialize via XmlSerializer to confirm round-trip.
        DevoxxGenieStateService restored = XmlSerializer.deserialize(xml, DevoxxGenieStateService.class);
        assertThat(restored.getCommands()).hasSize(1);
        assertThat(restored.getCommands().get(0).getName()).isEqualTo("ralph-runners");

        // Apply loadState lifecycle and assert the value is preserved.
        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(restored);
        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("ralph-runners");
    }

    @Test
    void legacyDataDoesNotOverrideExistingCommands() throws Exception {
        // If a state somehow has both legacy and new entries, the new commands must win.
        DevoxxGenieStateService mixedState = new DevoxxGenieStateService();
        setCommandsList(mixedState, "commands", new ArrayList<>(List.of(
                new Command("new-cmd", "from new field")
        )));
        setCommandsList(mixedState, "customPrompts", new ArrayList<>(List.of(
                new Command("legacy-cmd", "from legacy field")
        )));

        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(mixedState);

        assertThat(target.getCommands()).hasSize(1);
        assertThat(target.getCommands().get(0).getName()).isEqualTo("new-cmd");
        assertThat(readField(target, "customPrompts")).isNull();
    }

    // --- helpers ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static void setCommandsList(DevoxxGenieStateService target, String fieldName, List<Command> value)
            throws Exception {
        Field f = DevoxxGenieStateService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object readField(DevoxxGenieStateService target, String fieldName) throws Exception {
        Field f = DevoxxGenieStateService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }
}
