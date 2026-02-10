package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Backlog.md-compatible task spec files.
 * Extracts YAML frontmatter and markdown body, including acceptance criteria checkboxes.
 */
@Slf4j
public final class SpecFrontmatterParser {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "\\A---\\s*\\n(.*?)\\n---\\s*\\n?(.*)",
            Pattern.DOTALL
    );

    private static final Pattern CHECKBOX_PATTERN = Pattern.compile(
            "^\\s*-\\s+\\[([ xX])]\\s+(.+)$",
            Pattern.MULTILINE
    );

    private SpecFrontmatterParser() {
    }

    /**
     * Parse a task spec file content into a TaskSpec object.
     *
     * @param content  the raw file content
     * @param filePath the absolute path to the source file
     * @return the parsed TaskSpec, or null if the content cannot be parsed
     */
    public static @Nullable TaskSpec parse(@NotNull String content, @NotNull String filePath) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.matches()) {
            log.debug("No frontmatter found in file: {}", filePath);
            return null;
        }

        String frontmatter = matcher.group(1);
        String body = matcher.group(2).trim();

        TaskSpec.TaskSpecBuilder builder = TaskSpec.builder();
        builder.filePath(filePath);
        builder.description(body);

        parseFrontmatter(frontmatter, builder);
        parseAcceptanceCriteria(body, builder);

        return builder.build();
    }

    private static void parseFrontmatter(@NotNull String frontmatter, TaskSpec.TaskSpecBuilder builder) {
        String currentKey = null;
        List<String> currentList = null;

        for (String line : frontmatter.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Check if this is a list item (starts with "- ")
            if (trimmed.startsWith("- ") && currentKey != null && currentList != null) {
                String value = trimmed.substring(2).trim();
                // Remove surrounding quotes
                value = stripQuotes(value);
                currentList.add(value);
                continue;
            }

            // Check if this is a key-value pair
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                // Flush previous list if any
                if (currentKey != null && currentList != null) {
                    applyListField(currentKey, currentList, builder);
                }

                currentKey = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();

                if (value.isEmpty()) {
                    // This is likely a list field — next lines will be "- item"
                    currentList = new ArrayList<>();
                } else {
                    currentList = null;
                    value = stripQuotes(value);
                    applyScalarField(currentKey, value, builder);
                }
            }
        }

        // Flush any remaining list
        if (currentKey != null && currentList != null) {
            applyListField(currentKey, currentList, builder);
        }
    }

    private static void applyScalarField(@NotNull String key, @NotNull String value, TaskSpec.TaskSpecBuilder builder) {
        switch (key.toLowerCase()) {
            case "id" -> builder.id(value);
            case "title" -> builder.title(value);
            case "status" -> builder.status(value);
            case "priority" -> builder.priority(value);
            default -> log.trace("Ignoring unknown frontmatter field: {}", key);
        }
    }

    private static void applyListField(@NotNull String key, @NotNull List<String> values, TaskSpec.TaskSpecBuilder builder) {
        switch (key.toLowerCase()) {
            case "assignee", "assignees" -> builder.assignees(new ArrayList<>(values));
            case "labels", "label" -> builder.labels(new ArrayList<>(values));
            case "dependencies", "dependency" -> builder.dependencies(new ArrayList<>(values));
            case "definition_of_done", "definitionofdone", "dod" -> builder.definitionOfDone(new ArrayList<>(values));
            default -> log.trace("Ignoring unknown list field: {}", key);
        }
    }

    private static void parseAcceptanceCriteria(@NotNull String body, TaskSpec.TaskSpecBuilder builder) {
        List<AcceptanceCriterion> criteria = new ArrayList<>();
        Matcher matcher = CHECKBOX_PATTERN.matcher(body);
        int index = 0;

        while (matcher.find()) {
            boolean checked = !"  ".contains(matcher.group(1));
            String text = matcher.group(2).trim();
            criteria.add(AcceptanceCriterion.builder()
                    .index(index++)
                    .text(text)
                    .checked(checked)
                    .build());
        }

        if (!criteria.isEmpty()) {
            builder.acceptanceCriteria(criteria);
        }
    }

    private static @NotNull String stripQuotes(@NotNull String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
