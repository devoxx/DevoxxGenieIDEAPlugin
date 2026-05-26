package com.devoxx.genie.service.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkQualityFilterTest {

    // ---- Empty / blank ----------------------------------------------------------------------

    @Test
    void isLowContent_nullOrBlank_returnsTrue() {
        assertThat(ChunkQualityFilter.isLowContent(null)).isTrue();
        assertThat(ChunkQualityFilter.isLowContent("")).isTrue();
        assertThat(ChunkQualityFilter.isLowContent("   \t\n  ")).isTrue();
    }

    // ---- Padded markdown table headers (the actual failure mode from agenticengineeringworkshop)

    @Test
    void isLowContent_paddedTableHeader_isFiltered() {
        // Real example from the failing query — header row padded out to ~250 chars
        String chunk = "| Field                      | Description                                                                                                                                                                                                                                                                                |";
        assertThat(ChunkQualityFilter.isLowContent(chunk)).isTrue();
    }

    @Test
    void isLowContent_minimalTableSeparator_isFiltered() {
        assertThat(ChunkQualityFilter.isLowContent("| Field | Description |")).isTrue();
    }

    @Test
    void isLowContent_repeatedSameWord_isFiltered() {
        // Repetition doesn't add distinct tokens; should be treated as noise even at length
        assertThat(ChunkQualityFilter.isLowContent("Field Field Field Field Field")).isTrue();
    }

    @Test
    void isLowContent_caseFolded_repeatedSameWord_isFiltered() {
        // "Field" / "field" should fold to one token, not two
        assertThat(ChunkQualityFilter.isLowContent("Field field Field field Field")).isTrue();
    }

    // ---- Real prose / code (must pass through) ---------------------------------------------

    @Test
    void isLowContent_shortMcpSentence_passes() {
        // Even a single sentence about MCP should be kept — distinct tokens: MCP, the, Model,
        // Context, Protocol = 5 (note: "is" is < 3 chars and excluded)
        assertThat(ChunkQualityFilter.isLowContent("MCP is the Model Context Protocol")).isFalse();
    }

    @Test
    void isLowContent_codeSnippet_passes() {
        String code = """
                public static void main(String[] args) {
                    System.out.println("hello world");
                }
                """;
        assertThat(ChunkQualityFilter.isLowContent(code)).isFalse();
    }

    @Test
    void isLowContent_richTableRow_passes() {
        // A row that actually carries information (multiple unique field values) should pass
        String chunk = "| PreToolUse | hookSpecificOutput | permissionDecision (allow/deny/ask/defer), permissionDecisionReason |";
        assertThat(ChunkQualityFilter.isLowContent(chunk)).isFalse();
    }

    // ---- Token-counting helper -------------------------------------------------------------

    @Test
    void countDistinctMeaningfulTokens_ignoresShortAndFoldsCase() {
        // "is" (2 chars) excluded; "MCP" (3) included; case-folded so duplicates collapse
        assertThat(ChunkQualityFilter.countDistinctMeaningfulTokens("MCP is mcp Mcp"))
                .isEqualTo(1);
    }

    @Test
    void countDistinctMeaningfulTokens_splitsOnPunctuationAndWhitespace() {
        assertThat(ChunkQualityFilter.countDistinctMeaningfulTokens("alpha,beta;gamma|delta"))
                .isEqualTo(4);
    }
}
