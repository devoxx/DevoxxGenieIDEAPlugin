package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.model.agent.AgentDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps {@link AgentDefinition}s to/from the Genie-format agent spec YAML used by the
 * DockerAgents POC (TASK-247), so personas edited in the IDE run unchanged in a
 * DockerAgents deployment and vice versa:
 *
 * <pre>
 * models:
 *   default: { provider: ollama, model: qwen3:... }
 * agents:
 *   root:
 *     model: default
 *     description: ...
 *     instruction: |
 *       ...
 *     toolsets:
 *       - type: filesystem
 *         readonly: true
 * </pre>
 *
 * The agent's name is not part of the Genie spec (it is the filename); callers pass it
 * explicitly on import. Unknown providers/toolsets degrade to warnings, never failures.
 */
public final class GenieAgentSpecMapper {

    /** Genie provider token → DevoxxGenie ModelProvider display name. */
    private static final Map<String, String> PROVIDER_TO_MODEL_PROVIDER = Map.ofEntries(
            Map.entry("ollama", "Ollama"),
            Map.entry("lmstudio", "LMStudio"),
            Map.entry("llamacpp", "LLaMA"),
            Map.entry("gpt4all", "GPT4All"),
            Map.entry("jan", "Jan"),
            Map.entry("exo", "Exo"),
            Map.entry("customopenai", "CustomOpenAI"),
            Map.entry("anthropic", "Anthropic"),
            Map.entry("openai", "OpenAI"),
            Map.entry("google", "Google"),
            Map.entry("gemini", "Google"),
            Map.entry("mistral", "Mistral"),
            Map.entry("groq", "Groq"),
            Map.entry("deepseek", "DeepSeek"),
            Map.entry("openrouter", "OpenRouter"),
            Map.entry("kimi", "Kimi"));

    /** DevoxxGenie ModelProvider display name → Genie provider token (export). */
    private static final Map<String, String> MODEL_PROVIDER_TO_PROVIDER = Map.ofEntries(
            Map.entry("Ollama", "ollama"),
            Map.entry("LMStudio", "lmstudio"),
            Map.entry("LLaMA", "llamacpp"),
            Map.entry("GPT4All", "gpt4all"),
            Map.entry("Jan", "jan"),
            Map.entry("Exo", "exo"),
            Map.entry("CustomOpenAI", "customopenai"),
            Map.entry("Anthropic", "anthropic"),
            Map.entry("OpenAI", "openai"),
            Map.entry("Google", "google"),
            Map.entry("Mistral", "mistral"),
            Map.entry("Groq", "groq"),
            Map.entry("DeepSeek", "deepseek"),
            Map.entry("OpenRouter", "openrouter"),
            Map.entry("Kimi", "kimi"));

    private GenieAgentSpecMapper() {
    }

    /** Import outcome: the mapped definition plus non-fatal mapping warnings. */
    public record ImportResult(@NotNull AgentDefinition definition, @NotNull List<String> warnings) {
    }

    /**
     * Parses a Genie-format YAML spec into an {@link AgentDefinition}.
     *
     * @param name the agent name (Genie specs carry the name in the filename)
     * @throws IllegalArgumentException when the YAML is unparsable or has no
     *         {@code agents.root.instruction}
     */
    @SuppressWarnings("unchecked")
    public static @NotNull ImportResult fromYaml(@NotNull String name, @NotNull String yaml) {
        List<String> warnings = new ArrayList<>();
        Object parsed;
        try {
            parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        } catch (Exception e) {
            throw new IllegalArgumentException("Not a valid YAML document: " + e.getMessage());
        }
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Not a Genie agent spec: top-level YAML mapping expected.");
        }
        Map<String, Object> spec = (Map<String, Object>) parsed;
        Map<String, Object> root = child(child(spec, "agents"), "root");
        if (root == null) {
            throw new IllegalArgumentException("Not a Genie agent spec: missing 'agents.root'.");
        }
        String instruction = asString(root.get("instruction"));
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("Not a Genie agent spec: missing 'agents.root.instruction'.");
        }

        // Model binding: agents.root.model names an entry in the top-level models{} map.
        String modelProvider = "";
        String modelName = "";
        Map<String, Object> models = child(spec, "models");
        String modelKey = asString(root.getOrDefault("model", "default"));
        if (models != null && modelKey != null && models.get(modelKey) instanceof Map<?, ?> modelDef) {
            String provider = asString(((Map<String, Object>) modelDef).get("provider"));
            String model = asString(((Map<String, Object>) modelDef).get("model"));
            if (provider != null && !provider.isBlank()) {
                String mapped = PROVIDER_TO_MODEL_PROVIDER.get(provider.trim().toLowerCase(Locale.ROOT));
                if (mapped != null) {
                    modelProvider = mapped;
                    modelName = model != null ? model : "";
                } else {
                    warnings.add("Unknown provider '" + provider + "' — the agent will inherit the "
                            + "conversation model instead. Bind a provider manually if needed.");
                }
            }
        }

        // Toolsets → presets. readOnly = true only when nothing writable was granted.
        List<String> presets = new ArrayList<>();
        boolean grantsWrites = false;
        Object toolsets = root.get("toolsets");
        if (toolsets instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> toolset)) {
                    continue;
                }
                String type = asString(((Map<String, Object>) toolset).get("type"));
                boolean readonly = Boolean.TRUE.equals(((Map<String, Object>) toolset).get("readonly"));
                if (type == null) {
                    continue;
                }
                switch (type.trim().toLowerCase(Locale.ROOT)) {
                    case "filesystem" -> {
                        presets.add(readonly ? "filesystem-ro" : "filesystem");
                        grantsWrites |= !readonly;
                    }
                    case "shell" -> {
                        presets.add("shell");
                        grantsWrites = true;
                    }
                    case "fetch" -> presets.add("fetch");
                    case "analysis" -> presets.add("analysis"); // DevoxxGenie extension
                    case "todo", "mcp" -> warnings.add("Toolset '" + type
                            + "' has no DevoxxGenie equivalent and was skipped.");
                    default -> warnings.add("Unknown toolset '" + type + "' was skipped.");
                }
            }
        }

        AgentDefinition definition = AgentDefinition.builder()
                .name(name.trim().toLowerCase(Locale.ROOT))
                .description(nullToEmpty(asString(root.get("description"))).replace("\n", " ").trim())
                .instruction(instruction)
                .modelProvider(modelProvider)
                .modelName(modelName)
                .toolsetPresets(presets)
                .readOnly(!grantsWrites)
                .build();
        return new ImportResult(definition, warnings);
    }

    /** Serializes a definition back to the Genie spec shape (round-trip stable). */
    public static @NotNull String toYaml(@NotNull AgentDefinition definition) {
        Map<String, Object> spec = new LinkedHashMap<>();

        if (definition.getModelProvider() != null && !definition.getModelProvider().isBlank()) {
            String provider = MODEL_PROVIDER_TO_PROVIDER.getOrDefault(
                    definition.getModelProvider(),
                    definition.getModelProvider().toLowerCase(Locale.ROOT));
            Map<String, Object> modelDef = new LinkedHashMap<>();
            modelDef.put("provider", provider);
            if (definition.getModelName() != null && !definition.getModelName().isBlank()) {
                modelDef.put("model", definition.getModelName());
            }
            spec.put("models", Map.of("default", modelDef));
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("model", "default");
        if (definition.getDescription() != null && !definition.getDescription().isBlank()) {
            root.put("description", definition.getDescription());
        }
        root.put("instruction", definition.getInstruction());

        List<Map<String, Object>> toolsets = new ArrayList<>();
        if (definition.getToolsetPresets() != null) {
            for (String preset : definition.getToolsetPresets()) {
                switch (preset) {
                    case "filesystem-ro" -> {
                        Map<String, Object> ts = new LinkedHashMap<>();
                        ts.put("type", "filesystem");
                        ts.put("readonly", true);
                        toolsets.add(ts);
                    }
                    case "filesystem" -> toolsets.add(typeOnly("filesystem"));
                    case "shell" -> toolsets.add(typeOnly("shell"));
                    case "fetch" -> toolsets.add(typeOnly("fetch"));
                    case "analysis" -> toolsets.add(typeOnly("analysis"));
                    default -> { /* unknown preset keys are not exported */ }
                }
            }
        }
        if (!toolsets.isEmpty()) {
            root.put("toolsets", toolsets);
        }

        spec.put("agents", Map.of("root", root));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setPrettyFlow(true);
        return new Yaml(options).dump(spec);
    }

    private static Map<String, Object> typeOnly(String type) {
        Map<String, Object> ts = new LinkedHashMap<>();
        ts.put("type", type);
        return ts;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> child(@Nullable Map<String, Object> map, @NotNull String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value != null ? value : "";
    }
}
