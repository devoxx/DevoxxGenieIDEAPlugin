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
 * Provides built-in IDE tools for agentic interactions:
 * read_file, write_file, edit_file, list_files, search_files, run_command.
 */
public class BuiltInToolProvider implements ToolProvider {

    private final Map<ToolSpecification, ToolExecutor> tools;

    public BuiltInToolProvider(@NotNull Project project) {
        tools = new LinkedHashMap<>();

        // read_file
        tools.put(
                ToolSpecification.builder()
                        .name("read_file")
                        .description("Read the contents of a file in the project")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .required("path")
                                .build())
                        .build(),
                new ReadFileToolExecutor(project)
        );

        // write_file
        tools.put(
                ToolSpecification.builder()
                        .name("write_file")
                        .description("Write content to a file in the project. Creates the file and parent directories if they don't exist.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .addStringProperty("content", "The content to write to the file")
                                .required("path", "content")
                                .build())
                        .build(),
                new WriteFileToolExecutor(project)
        );

        // edit_file
        tools.put(
                ToolSpecification.builder()
                        .name("edit_file")
                        .description("Edit a file by replacing an exact string match with new content. " +
                                "The file must already exist. If the old_string appears multiple times, " +
                                "either provide more context to make it unique, or set replace_all to true.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("path", "File path relative to project root")
                                .addStringProperty("old_string", "The exact text to find in the file")
                                .addStringProperty("new_string", "The replacement text")
                                .addBooleanProperty("replace_all",
                                        "Whether to replace all occurrences (default: false)")
                                .required("path", "old_string", "new_string")
                                .build())
                        .build(),
                new EditFileToolExecutor(project)
        );

        // list_files
        tools.put(
                ToolSpecification.builder()
                        .name("list_files")
                        .description("List files and directories in the project. Skips common build/VCS directories.")
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
                        .description("Search for a regex pattern in project files. Returns matching lines with file paths and line numbers.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("pattern", "Regex pattern to search for")
                                .addStringProperty("path", "Directory path to search in (defaults to project root)")
                                .addStringProperty("file_pattern", "Glob pattern to filter files (e.g. '*.java')")
                                .required("pattern")
                                .build())
                        .build(),
                new SearchFilesToolExecutor(project)
        );

        // run_command
        tools.put(
                ToolSpecification.builder()
                        .name("run_command")
                        .description("Execute a terminal command in the project directory. Has a 30-second timeout.")
                        .parameters(JsonObjectSchema.builder()
                                .addStringProperty("command", "The command to execute")
                                .addStringProperty("working_dir", "Working directory relative to project root (defaults to project root)")
                                .required("command")
                                .build())
                        .build(),
                new RunCommandToolExecutor(project)
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
