package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.Command;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
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
 *
 * <p>The most important scenario is the real upgrade path: a user with a settings file
 * persisted by the pre-rename code (i.e. {@code <option name="customPrompts">} containing
 * {@code <CustomPrompt .../>} child elements) opens the new plugin version. The XML must
 * deserialize into the new {@link Command} class (thanks to the {@code @Tag("CustomPrompt")}\n * alias) and {@link DevoxxGenieStateService#loadState} must promote those values into the\n * {@code commands} field.</p>
 */
class DevoxxGenieStateServiceMigrationTest {

    /**
     * Full XML round-trip test using a hand-crafted legacy XML payload representative of
     * what the previous plugin version wrote to disk. Asserts the entries survive deserialization
     * AND the {@code loadState} lifecycle.
     */
    @Test
    void legacyXmlWithCustomPromptItemsMigratesIntoCommands() throws Exception {
        // This is the XML shape IntelliJ's XmlSerializer produced for the old
        // List<CustomPrompt> customPrompts field. Items use the @Tag("CustomPrompt") name now
        // declared on Command, so the deserializer maps them to Command instances.
        String legacyXml = """
                <DevoxxGenieStateService>
                  <option name="customPrompts">
                    <list>
                      <CustomPrompt>
                        <option name="name" value="explainit" />
                        <option name="prompt" value="Explain this prompt" />
                      </CustomPrompt>
                      <CustomPrompt>
                        <option name="name" value="review" />
                        <option name="prompt" value="Review this code" />
                      </CustomPrompt>
                    </list>
                  </option>
                </DevoxxGenieStateService>
                """;

        Element root = JDOMUtil.load(legacyXml);
        DevoxxGenieStateService deserialized = XmlSerializer.deserialize(root, DevoxxGenieStateService.class);

        // The deserializer should have populated the legacy field via its public setter.
        Object legacyField = readField(deserialized, "customPrompts");
        assertThat(legacyField)
                .as("legacy customPrompts field should be populated after deserialization")
                .isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Command> legacyList = (List<Command>) legacyField;
        assertThat(legacyList)
                .as("legacy XML <CustomPrompt> items should map to Command instances via @Tag alias")
                .hasSize(2);
        assertThat(legacyList).extracting(Command::getName).containsExactly("explainit", "review");
        assertThat(legacyList).extracting(Command::getPrompt)
                .containsExactly("Explain this prompt", "Review this code");

        // Now exercise the full loadState lifecycle, which is what the platform actually invokes.
        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(deserialized);

        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("explainit", "review");
        assertThat(target.getCommands())
                .extracting(Command::getPrompt)
                .contains("Explain this prompt", "Review this code");

        // And the legacy field should be cleared so it does not get re-serialized.
        assertThat(readField(target, "customPrompts")).isNull();
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
        source.setCommands(new ArrayList<>(List.of(
                new Command("ralph-runners", "You are a product manager. Task: $ARGUMENT")
        )));

        // Serialize to XML.
        Element xml = XmlSerializer.serialize(source);
        String xmlString = JDOMUtil.writeElement(xml);

        // Should serialize the field under the new name. The CustomPrompt @Tag alias is on the
        // item class so each list element appears as <CustomPrompt>, but the field wrapper
        // is named "commands".
        assertThat(xmlString).contains("name=\"commands\"");

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
    void legacyXmlAlongsideNewCommandsPrefersLegacyAndClearsIt() throws Exception {
        // Pathological case: persisted state has both old-name and new-name lists. This
        // cannot happen via a normal upgrade (pre-#1040 XML only ever has customPrompts,
        // and once migrated the legacy field is cleared) but we still need a deterministic
        // resolution. We prefer the legacy field because its presence is the unambiguous
        // signal of an upgrade in progress; on next save only the new commands element will
        // be emitted because the legacy field has been cleared.
        String mixedXml = """
                <DevoxxGenieStateService>
                  <option name="commands">
                    <list>
                      <CustomPrompt>
                        <option name="name" value="new-cmd" />
                        <option name="prompt" value="from new field" />
                      </CustomPrompt>
                    </list>
                  </option>
                  <option name="customPrompts">
                    <list>
                      <CustomPrompt>
                        <option name="name" value="legacy-cmd" />
                        <option name="prompt" value="from legacy field" />
                      </CustomPrompt>
                    </list>
                  </option>
                </DevoxxGenieStateService>
                """;

        DevoxxGenieStateService deserialized =
                XmlSerializer.deserialize(JDOMUtil.load(mixedXml), DevoxxGenieStateService.class);

        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(deserialized);

        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("legacy-cmd")
                .doesNotContain("new-cmd");
        assertThat(readField(target, "customPrompts")).isNull();
    }

    /**
     * Sanity check: a state with no legacy data at all (only new {@code commands}) round-trips
     * cleanly without losing items.
     */
    @Test
    void newOnlyStateRoundTripsWithoutLoss() throws Exception {
        DevoxxGenieStateService source = new DevoxxGenieStateService();
        source.setCommands(new ArrayList<>(Arrays.asList(
                new Command("alpha", "first"),
                new Command("beta", "second")
        )));

        Element xml = XmlSerializer.serialize(source);
        DevoxxGenieStateService restored = XmlSerializer.deserialize(xml, DevoxxGenieStateService.class);

        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(restored);

        assertThat(target.getCommands())
                .extracting(Command::getName)
                .contains("alpha", "beta");
    }

    /**
     * After migration, the next save must NOT re-emit a {@code <option name="customPrompts"/>}
     * element. Otherwise stale legacy data could resurface across plugin versions.
     */
    @Test
    void afterMigrationXmlNoLongerContainsCustomPromptsElement() throws Exception {
        String legacyXml = """
                <DevoxxGenieStateService>
                  <option name="customPrompts">
                    <list>
                      <CustomPrompt>
                        <option name="name" value="legacy" />
                        <option name="prompt" value="legacy prompt" />
                      </CustomPrompt>
                    </list>
                  </option>
                </DevoxxGenieStateService>
                """;

        DevoxxGenieStateService deserialized =
                XmlSerializer.deserialize(JDOMUtil.load(legacyXml), DevoxxGenieStateService.class);
        DevoxxGenieStateService target = new DevoxxGenieStateService();
        target.loadState(deserialized);

        // Re-serialize after migration using the platform's default-skipping filter,
        // which is what IntelliJ uses for actual settings persistence.
        Element reSerialized = XmlSerializer.serialize(target, new SkipDefaultValuesSerializationFilters());
        String xmlAfterMigration = JDOMUtil.writeElement(reSerialized);

        assertThat(xmlAfterMigration)
                .as("legacy customPrompts element must not survive migration")
                .doesNotContain("name=\"customPrompts\"");
        assertThat(xmlAfterMigration).contains("name=\"commands\"");
    }

    // --- helpers ---------------------------------------------------------

    private static Object readField(DevoxxGenieStateService target, String fieldName) throws Exception {
        Field f = DevoxxGenieStateService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }
}
