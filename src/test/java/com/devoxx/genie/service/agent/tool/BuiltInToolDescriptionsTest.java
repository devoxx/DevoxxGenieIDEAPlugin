package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.service.agent.tool.psi.PsiToolCatalog;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuiltInToolDescriptionsTest {

    @Mock
    private DevoxxGenieStateService stateService;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private Map<String, String> overrides;

    @BeforeEach
    void setUp() {
        overrides = new HashMap<>();
        stateServiceMock = mockStatic(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getToolDescriptionOverrides()).thenReturn(overrides);
    }

    @AfterEach
    void tearDown() {
        stateServiceMock.close();
    }

    // --- Defaults ---

    @Test
    void defaultOf_knownTool_returnsShippedDescription() {
        assertThat(BuiltInToolDescriptions.defaultOf("read_file"))
                .isEqualTo("Read the contents of a file in the project");
    }

    @Test
    void defaultOf_unknownTool_returnsNull() {
        assertThat(BuiltInToolDescriptions.defaultOf("some_mcp_tool")).isNull();
    }

    @Test
    void everyToolHasANonBlankDefault() {
        assertThat(BuiltInToolDescriptions.toolNames()).isNotEmpty();
        for (String name : BuiltInToolDescriptions.toolNames()) {
            assertThat(BuiltInToolDescriptions.defaultOf(name))
                    .as("default description for %s", name)
                    .isNotNull()
                    .isNotBlank();
        }
    }

    /**
     * Every PSI tool exposed as a settings checkbox must also be description-editable, otherwise
     * the settings panel would show an edit affordance with no default text behind it.
     */
    @Test
    void allPsiToolsAreOverridable() {
        assertThat(BuiltInToolDescriptions.toolNames())
                .containsAll(PsiToolCatalog.toolNames());
    }

    // --- Override applied ---

    @Test
    void effective_withOverride_returnsOverride() {
        overrides.put("edit_file", "DISABLED. Use run_command with sed/patch for all edits.");

        assertThat(BuiltInToolDescriptions.effective("edit_file"))
                .isEqualTo("DISABLED. Use run_command with sed/patch for all edits.");
        assertThat(BuiltInToolDescriptions.isOverridden("edit_file")).isTrue();
    }

    @Test
    void effective_withOverride_leavesOtherToolsOnTheirDefault() {
        overrides.put("edit_file", "custom");

        assertThat(BuiltInToolDescriptions.effective("run_command"))
                .isEqualTo(BuiltInToolDescriptions.defaultOf("run_command"));
        assertThat(BuiltInToolDescriptions.isOverridden("run_command")).isFalse();
    }

    @Test
    void effective_overrideIsTrimmed() {
        overrides.put("read_file", "  padded override  ");

        assertThat(BuiltInToolDescriptions.effective("read_file")).isEqualTo("padded override");
    }

    // --- Reset to default ---

    @Test
    void effective_afterOverrideRemoved_returnsDefault() {
        overrides.put("read_file", "custom");
        assertThat(BuiltInToolDescriptions.effective("read_file")).isEqualTo("custom");

        overrides.remove("read_file");

        assertThat(BuiltInToolDescriptions.effective("read_file"))
                .isEqualTo(BuiltInToolDescriptions.defaultOf("read_file"));
        assertThat(BuiltInToolDescriptions.isOverridden("read_file")).isFalse();
    }

    @Test
    void effective_blankOverride_isTreatedAsNoOverride() {
        overrides.put("read_file", "   ");

        assertThat(BuiltInToolDescriptions.effective("read_file"))
                .isEqualTo(BuiltInToolDescriptions.defaultOf("read_file"));
        assertThat(BuiltInToolDescriptions.isOverridden("read_file")).isFalse();
    }

    // --- Unknown / non-built-in tools ---

    @Test
    void effective_unknownTool_returnsNullAndIgnoresStoredOverride() {
        // A stale override for a renamed/removed tool, or one aimed at an MCP tool.
        overrides.put("some_mcp_tool", "hijacked description");

        assertThat(BuiltInToolDescriptions.effective("some_mcp_tool")).isNull();
        assertThat(BuiltInToolDescriptions.overrideOf("some_mcp_tool")).isNull();
        assertThat(BuiltInToolDescriptions.isOverridden("some_mcp_tool")).isFalse();
    }

    // --- Robustness ---

    @Test
    void effective_nullOverrideMap_returnsDefault() {
        when(stateService.getToolDescriptionOverrides()).thenReturn(null);

        assertThat(BuiltInToolDescriptions.effective("read_file"))
                .isEqualTo(BuiltInToolDescriptions.defaultOf("read_file"));
    }

    @Test
    void effective_settingsUnavailable_returnsDefault() {
        stateServiceMock.when(DevoxxGenieStateService::getInstance)
                .thenThrow(new IllegalStateException("no application"));

        assertThat(BuiltInToolDescriptions.effective("read_file"))
                .isEqualTo(BuiltInToolDescriptions.defaultOf("read_file"));
    }

    @Test
    void toolNames_isImmutable() {
        assertThat(BuiltInToolDescriptions.toolNames())
                .isUnmodifiable();
    }
}
