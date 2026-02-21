package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
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
 * Extracts YAML frontmatter and markdown body, including section-aware parsing
 * for acceptance criteria, definition of done, implementation plan/notes, and final summary.
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

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "^##\\s+(.+)$",
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

        parseFrontmatter(frontmatter, builder);
        parseSections(body, builder);

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
                currentList = parseFieldValue(currentKey, value, builder);
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
            case "milestone" -> builder.milestone(value);
            case "created_date", "createddate", "created_at", "createdat" -> builder.createdAt(value);
            case "updated_date", "updateddate", "updated_at", "updatedat" -> builder.updatedAt(value);
            case "parent_task_id", "parenttaskid", "parent" -> builder.parentTaskId(value);
            case "ordinal" -> {
                try { builder.ordinal(Integer.parseInt(value)); } catch (NumberFormatException ignored) { }
            }
            default -> log.trace("Ignoring unknown frontmatter field: {}", key);
        }
    }

    /**
     * Parse a frontmatter field value, applying it to the builder and returning the new currentList state.
     * Returns a new list if the value is empty (list field start), or null for scalar/explicit-empty fields.
     */
    private static @Nullable List<String> parseFieldValue(@NotNull String key, @NotNull String value, TaskSpec.TaskSpecBuilder builder) {
        if (value.isEmpty()) {
            // This is likely a list field — next lines will be "- item"
            return new ArrayList<>();
        } else if ("[]".equals(value)) {
            // Explicit empty array
            applyListField(key, new ArrayList<>(), builder);
            return null;
        } else {
            applyScalarField(key, stripQuotes(value), builder);
            return null;
        }
    }

    private static void applyListField(@NotNull String key, @NotNull List<String> values, TaskSpec.TaskSpecBuilder builder) {
        switch (key.toLowerCase()) {
            case "assignee", "assignees" -> builder.assignees(new ArrayList<>(values));
            case "labels", "label" -> builder.labels(new ArrayList<>(values));
            case "dependencies", "dependency" -> builder.dependencies(new ArrayList<>(values));
            case "references", "reference" -> builder.references(new ArrayList<>(values));
            case "documentation", "docs" -> builder.documentation(new ArrayList<>(values));
            default -> log.trace("Ignoring unknown list field: {}", key);
        }
    }

    /**
     * Parse the markdown body into sections. Extracts:
     * - Description (text before first ## header or under ## Description)
     * - Acceptance Criteria (checkboxes under ## Acceptance Criteria)
     * - Definition of Done (checkboxes under ## Definition of Done)
     * - Implementation Plan (content under ## Implementation Plan)
     * - Implementation Notes (content under ## Implementation Notes / ## Notes)
     * - Final Summary (content under ## Final Summary)
     */
    private static void parseSections(@NotNull String body, TaskSpec.TaskSpecBuilder builder) {
        // Split body into sections
        Matcher sectionMatcher = SECTION_PATTERN.matcher(body);
        List<int[]> sectionStarts = new ArrayList<>();
        List<String> sectionNames = new ArrayList<>();

        while (sectionMatcher.find()) {
            sectionStarts.add(new int[]{sectionMatcher.start(), sectionMatcher.end()});
            sectionNames.add(sectionMatcher.group(1).trim().toLowerCase());
        }

        // If no sections found, the entire body is the description
        // Only parse acceptance criteria (not DoD) to avoid duplicating checkboxes
        if (sectionStarts.isEmpty()) {
            builder.description(body);
            parseAcceptanceCriteria(body, builder);
            return;
        }

        // Text before first section header is the description
        String preSection = body.substring(0, sectionStarts.get(0)[0]).trim();
        StringBuilder descriptionBuilder = new StringBuilder();
        if (!preSection.isEmpty()) {
            descriptionBuilder.append(preSection);
        }

        // Process each section
        for (int i = 0; i < sectionNames.size(); i++) {
            int contentStart = sectionStarts.get(i)[1];
            int contentEnd = (i + 1 < sectionStarts.size()) ? sectionStarts.get(i + 1)[0] : body.length();
            String sectionContent = body.substring(contentStart, contentEnd).trim();
            String sectionName = sectionNames.get(i);

            switch (sectionName) {
                case "description" -> descriptionBuilder.append(descriptionBuilder.length() > 0 ? "\n\n" : "").append(sectionContent);
                case "acceptance criteria" -> parseAcceptanceCriteria(sectionContent, builder);
                case "definition of done", "dod" -> parseDefinitionOfDone(sectionContent, builder);
                case "implementation plan", "plan" -> builder.implementationPlan(sectionContent);
                case "implementation notes", "notes" -> builder.implementationNotes(sectionContent);
                case "final summary", "summary" -> builder.finalSummary(sectionContent);
                default -> {
                    // Unknown sections become part of description
                    descriptionBuilder.append(descriptionBuilder.length() > 0 ? "\n\n" : "")
                            .append("## ").append(sectionNames.get(i)).append("\n").append(sectionContent);
                }
            }
        }

        String description = descriptionBuilder.toString().trim();
        if (!description.isEmpty()) {
            builder.description(description);
        }
    }

    private static void parseAcceptanceCriteria(@NotNull String content, TaskSpec.TaskSpecBuilder builder) {
        List<AcceptanceCriterion> criteria = new ArrayList<>();
        Matcher matcher = CHECKBOX_PATTERN.matcher(content);
        int index = 0;

        while (matcher.find()) {
            boolean checked = !" ".equals(matcher.group(1));
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

    private static void parseDefinitionOfDone(@NotNull String content, TaskSpec.TaskSpecBuilder builder) {
        List<DefinitionOfDoneItem> items = new ArrayList<>();
        Matcher matcher = CHECKBOX_PATTERN.matcher(content);
        int index = 0;

        while (matcher.find()) {
            boolean checked = !" ".equals(matcher.group(1));
            String text = matcher.group(2).trim();
            items.add(DefinitionOfDoneItem.builder()
                    .index(index++)
                    .text(text)
                    .checked(checked)
                    .build());
        }

        if (!items.isEmpty()) {
            builder.definitionOfDone(items);
        }
    }

    static @NotNull String stripQuotes(@NotNull String value) {
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
