package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Registry for Agent Team definitions. Backed by {@link DevoxxGenieStateService}
 * persistence; seeds the built-in personas on first access and renders the live agent
 * catalog for the orchestrator system prompt — the in-process analog of the DockerAgents
 * delegation table generated from its {@code GET /agents} directory.
 */
@Slf4j
public final class AgentRegistry {

    /** Flat, lowercase agent names — same constraint as DockerAgents agent spec names. */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{1,31}$");

    private static final AgentRegistry INSTANCE = new AgentRegistry();

    private AgentRegistry() {
    }

    public static AgentRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns all persisted definitions, seeding the built-ins on first access.
     * Returned list is a defensive copy.
     */
    public @NotNull List<AgentDefinition> getAll() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        List<AgentDefinition> stored = state.getAgentDefinitions();
        if (stored == null || stored.isEmpty()) {
            List<AgentDefinition> seeded = new ArrayList<>();
            for (AgentDefinition def : BuiltInAgents.defaults()) {
                seeded.add(def.copy());
            }
            state.setAgentDefinitions(seeded);
            log.info("Agent registry seeded with {} built-in agents", seeded.size());
            stored = state.getAgentDefinitions();
        }
        List<AgentDefinition> copy = new ArrayList<>(stored.size());
        for (AgentDefinition def : stored) {
            copy.add(def.copy());
        }
        return copy;
    }

    /** Enabled agents only — the set the orchestrator may delegate to. */
    public @NotNull List<AgentDefinition> getEnabled() {
        return getAll().stream().filter(AgentDefinition::isEnabled).toList();
    }

    public @NotNull Optional<AgentDefinition> byName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return getAll().stream()
                .filter(def -> normalized.equals(def.getName()))
                .findFirst();
    }

    /** Comma-separated enabled agent names, for error messages and validation hints. */
    public @NotNull String availableNames() {
        return String.join(", ", getEnabled().stream().map(AgentDefinition::getName).toList());
    }

    public static boolean isValidName(@Nullable String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Validates and persists a full replacement of the agent list (settings apply()).
     * Throws {@link IllegalArgumentException} with a user-presentable message when a
     * name is invalid or duplicated, or a built-in agent was removed.
     */
    public void saveAll(@NotNull List<AgentDefinition> definitions) {
        List<String> seen = new ArrayList<>(definitions.size());
        for (AgentDefinition def : definitions) {
            if (!isValidName(def.getName())) {
                throw new IllegalArgumentException("Invalid agent name '" + def.getName()
                        + "': use 2-32 chars, lowercase letters/digits/dashes, starting with a letter.");
            }
            if (seen.contains(def.getName())) {
                throw new IllegalArgumentException("Duplicate agent name '" + def.getName() + "'.");
            }
            if (def.getInstruction() == null || def.getInstruction().isBlank()) {
                throw new IllegalArgumentException("Agent '" + def.getName() + "' has an empty persona instruction.");
            }
            seen.add(def.getName());
        }
        for (AgentDefinition shipped : BuiltInAgents.defaults()) {
            if (!seen.contains(shipped.getName())) {
                throw new IllegalArgumentException("Built-in agent '" + shipped.getName()
                        + "' cannot be deleted (disable it instead).");
            }
        }
        List<AgentDefinition> copy = new ArrayList<>(definitions.size());
        for (AgentDefinition def : definitions) {
            copy.add(def.copy());
        }
        DevoxxGenieStateService.getInstance().setAgentDefinitions(copy);
    }

    /** The shipped default for a built-in agent name, or empty for custom/unknown names. */
    public @NotNull Optional<AgentDefinition> shippedDefault(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        return BuiltInAgents.defaults().stream()
                .filter(def -> def.getName().equals(name))
                .findFirst()
                .map(AgentDefinition::copy);
    }

    /**
     * Restores a built-in agent's shipped persona/config, preserving nothing from the
     * edited version. No-op for unknown or non-built-in names.
     */
    public void resetBuiltIn(@NotNull String name) {
        BuiltInAgents.defaults().stream()
                .filter(def -> def.getName().equals(name))
                .findFirst()
                .ifPresent(shipped -> {
                    List<AgentDefinition> all = getAll();
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getName().equals(name)) {
                            all.set(i, shipped.copy());
                        }
                    }
                    DevoxxGenieStateService.getInstance().setAgentDefinitions(all);
                });
    }

    /**
     * Renders the live agent catalog as a markdown table for the orchestrator system
     * prompt. Includes only enabled agents; the model column shows the per-agent binding
     * or "conversation model" when inherited.
     */
    public @NotNull String buildCatalogPrompt() {
        List<AgentDefinition> enabled = getEnabled();
        if (enabled.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Available specialist agents (delegate with the `delegate_task` tool):\n\n");
        sb.append("| agent | capabilities | model |\n");
        sb.append("|-------|--------------|-------|\n");
        for (AgentDefinition def : enabled) {
            sb.append("| ").append(def.getName())
                    .append(" | ").append(sanitizeCell(def.getDescription()))
                    .append(" | ").append(modelLabel(def))
                    .append(" |\n");
        }
        return sb.toString();
    }

    /**
     * The full orchestrator system-prompt fragment for Agent Team mode: coordinator
     * mandate + live catalog.
     */
    public @NotNull String buildOrchestratorInstruction() {
        return BuiltInAgents.ORCHESTRATOR_INSTRUCTION + "\n" + buildCatalogPrompt();
    }

    static @NotNull String modelLabel(@NotNull AgentDefinition def) {
        if (def.getModelProvider() == null || def.getModelProvider().isBlank()) {
            return "conversation model";
        }
        String label = def.getModelProvider();
        if (def.getModelName() != null && !def.getModelName().isBlank()) {
            label += " · " + def.getModelName();
        }
        return label;
    }

    private static @NotNull String sanitizeCell(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "/").replace("\n", " ").trim();
    }
}
