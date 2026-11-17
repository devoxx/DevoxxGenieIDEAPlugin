package com.devoxx.genie.service.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The batch runner records unfinished tasks with a targeted text edit so that
 * frontmatter fields and markup this plugin does not model (e.g. Backlog.md's
 * {@code type}, {@code modified_files} and SECTION markers) survive (#1424 review).
 */
class SpecTaskFileEditorTest {

    private static final String NOTE = "Batch run stopped 2026-11-17: agent action limit reached (25).";

    private static final String BACKLOG_TASK = """
            ---
            id: TASK-7
            title: Some task
            status: To Do
            type: feature
            assignee: []
            created_date: '2026-11-10 09:00'
            labels:
              - runner
            modified_files:
              - src/main/java/Foo.java
              - src/test/java/FooTest.java
            dependencies: []
            ---

            ## Description

            <!-- SECTION:DESCRIPTION:BEGIN -->
            Do the thing.
            <!-- SECTION:DESCRIPTION:END -->

            ## Implementation Notes

            <!-- SECTION:NOTES:BEGIN -->
            Started on the parser.
            <!-- SECTION:NOTES:END -->

            ## Final Summary

            <!-- SECTION:FINAL_SUMMARY:BEGIN -->
            <!-- SECTION:FINAL_SUMMARY:END -->
            """;

    @Test
    void changesOnlyStatusAndNotes_preservingUnmodelledFieldsAndMarkers() {
        String out = SpecTaskFileEditor.setStatusAndAppendNote(BACKLOG_TASK, "In Progress", NOTE);

        String expected = BACKLOG_TASK
                .replace("status: To Do", "status: In Progress")
                .replace("Started on the parser.\n<!-- SECTION:NOTES:END -->",
                        "Started on the parser.\n\n" + NOTE + "\n<!-- SECTION:NOTES:END -->");
        assertThat(out).isEqualTo(expected);
        assertThat(out).contains("type: feature", "modified_files:", "  - src/test/java/FooTest.java",
                "<!-- SECTION:FINAL_SUMMARY:BEGIN -->");
    }

    @Test
    void emptyNotesSection_insertsNoteWithoutLeadingBlankLine() {
        String in = BACKLOG_TASK.replace("Started on the parser.\n", "");
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).contains("<!-- SECTION:NOTES:BEGIN -->\n" + NOTE + "\n<!-- SECTION:NOTES:END -->");
    }

    @Test
    void notesHeadingWithoutMarkers_appendsAtEndOfThatSection() {
        String in = """
                ---
                id: TASK-8
                status: In Progress
                ---

                ## Implementation Notes

                First note.

                ## Final Summary

                Nothing yet.
                """;
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).isEqualTo(in.replace("First note.\n", "First note.\n\n" + NOTE + "\n"));
    }

    @Test
    void noNotesSection_appendsOneUsingTheFilesMarkerStyle() {
        String in = BACKLOG_TASK.substring(0, BACKLOG_TASK.indexOf("## Implementation Notes"));
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).endsWith("<!-- SECTION:DESCRIPTION:END -->\n\n## Implementation Notes\n\n"
                + "<!-- SECTION:NOTES:BEGIN -->\n" + NOTE + "\n<!-- SECTION:NOTES:END -->\n");
    }

    @Test
    void plainFileWithoutNotes_appendsPlainSection() {
        String in = "---\nid: TASK-9\nstatus: To Do\n---\n\nJust a description.\n";
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).isEqualTo("---\nid: TASK-9\nstatus: In Progress\n---\n\nJust a description.\n\n"
                + "## Implementation Notes\n\n" + NOTE + "\n");
    }

    @Test
    void missingStatusLine_isAddedToFrontmatter() {
        String in = "---\nid: TASK-10\ntitle: x\n---\n\nBody\n";
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).startsWith("---\nid: TASK-10\ntitle: x\nstatus: In Progress\n---\n");
    }

    @Test
    void statusInBodyIsNotTouched_andQuotedStatusIsReplaced() {
        String in = "---\nid: TASK-11\nstatus: 'To Do'\n---\n\nstatus: To Do appears in the body\n";
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out).contains("---\nid: TASK-11\nstatus: In Progress\n---")
                .contains("status: To Do appears in the body");
    }

    @Test
    void crlfLineEndings_arePreserved() {
        String in = BACKLOG_TASK.replace("\n", "\r\n");
        String out = SpecTaskFileEditor.setStatusAndAppendNote(in, "In Progress", NOTE);

        assertThat(out.replace("\r\n", "")).doesNotContain("\n");
        assertThat(out).contains("status: In Progress\r\n", NOTE + "\r\n<!-- SECTION:NOTES:END -->");
    }
}
