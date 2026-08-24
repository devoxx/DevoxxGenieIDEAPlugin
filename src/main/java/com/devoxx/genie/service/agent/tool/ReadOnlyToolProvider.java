package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides read-only IDE tools for sub-agent exploration.
 * Only includes read_file, list_files, and search_files — no write or command tools.
 *
 * <p>Descriptions come from {@link BuiltInToolDescriptions} so sub-agents see the same
 * (possibly user-overridden) text as the main agent.
 */
public class ReadOnlyToolProvider implements ToolProvider {

    private final Map<ToolSpecification, ToolExecutor> tools;

    public ReadOnlyToolProvider(@NotNull Project project) {
        tools = new LinkedHashMap<>();

        // read_file
        tools.put(
                ToolSpecification.builder()
                        .name("read_file")
                        .description(BuiltInToolDescriptions.effective("read_file"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .required("path")
                                .build())
                        .build(),
                new ReadFileToolExecutor(project)
        );

        // list_files
        tools.put(
                ToolSpecification.builder()
                        .name("list_files")
                        .description(BuiltInToolDescriptions.effective("list_files"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "Directory path relative to project root (defaults to root)")
                                .addBooleanProperty("recursive", "Whether to list files recursively (default: false)")
                                .build())
                        .build(),
                new ListFilesToolExecutor(project)
        );

        // search_files
        tools.put(
                ToolSpecification.builder()
                        .name("search_files")
                        .description(BuiltInToolDescriptions.effective("search_files"))
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("pattern", "Regex pattern to search for")
                                .addStringProperty("path", "Directory path to search in (defaults to project root)")
                                .addStringProperty("file_pattern", "Glob pattern to filter files (e.g. '*.java')")
                                .required("pattern")
                                .build())
                        .build(),
                new SearchFilesToolExecutor(project)
        );
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (Map.Entry<ToolSpecification, ToolExecutor> entry : tools.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
