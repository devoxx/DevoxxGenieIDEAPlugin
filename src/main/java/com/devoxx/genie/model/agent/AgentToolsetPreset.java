package com.devoxx.genie.model.agent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Named toolset presets an {@link AgentDefinition} composes its tool allowlist from —
 * the DevoxxGenie analog of the Genie YAML {@code toolsets} block (shell/filesystem/fetch),
 * mapped onto the built-in agent tool names.
 */
public enum AgentToolsetPreset {

    /** Read-only project access. */
    FILESYSTEM_RO("filesystem-ro", List.of("read_file", "list_files", "search_files")),

    /** Read/write project access (superset of {@link #FILESYSTEM_RO}). */
    FILESYSTEM("filesystem", List.of("read_file", "list_files", "search_files", "write_file", "edit_file")),

    /** Command and test execution. */
    SHELL("shell", List.of("run_command", "run_tests")),

    /** Web access. */
    FETCH("fetch", List.of("fetch_page", "web_search")),

    /** Structural code analysis: PSI tools + semantic search (registered only when enabled globally). */
    ANALYSIS("analysis", List.of(
            "find_symbols", "document_symbols", "find_references", "find_definition",
            "find_implementations", "find_callees", "trace_call_chains",
            "calculate_complexity", "find_dead_code", "semantic_search"));

    /** Tools stripped from any preset when an agent is marked read-only. */
    private static final Set<String> WRITE_TOOLS = Set.of("write_file", "edit_file", "run_command", "run_tests");

    private final String key;
    private final List<String> tools;

    AgentToolsetPreset(String key, List<String> tools) {
        this.key = key;
        this.tools = tools;
    }

    public String getKey() {
        return key;
    }

    public List<String> getTools() {
        return tools;
    }

    @Nullable
    public static AgentToolsetPreset fromKey(@Nullable String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(preset -> preset.key.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves preset keys to the union of their concrete tool names, preserving preset
     * order. Unknown keys are skipped. When {@code readOnly} is true, write/run tools are
     * stripped from the result (the structural equivalent of the Genie YAML
     * {@code readonly: true} toolset flag).
     */
    public static @NotNull Set<String> resolveTools(@Nullable Collection<String> presetKeys, boolean readOnly) {
        Set<String> resolved = new LinkedHashSet<>();
        if (presetKeys == null) {
            return resolved;
        }
        for (String key : presetKeys) {
            AgentToolsetPreset preset = fromKey(key);
            if (preset != null) {
                resolved.addAll(preset.tools);
            }
        }
        if (readOnly) {
            resolved.removeAll(WRITE_TOOLS);
        }
        return resolved;
    }
}
