package com.devoxx.genie.service.rag;

/**
 * Decides whether a freshly-split chunk carries enough information to be worth embedding and
 * storing. The bar is intentionally low: we want to drop chunks that are dominated by
 * structural noise (markdown table separator rows, padding-only header lines, ASCII-art
 * dividers) while keeping every chunk that contains real prose or code — even short ones.
 *
 * <p>Heuristic: count distinct alphanumeric tokens of length &ge; 3. Tokens this short still
 * carry meaning ({@code MCP}, {@code LSP}, {@code git}), so the threshold can't be raised
 * further without losing legitimate content. Below the threshold the chunk is treated as
 * noise — its embedding would carry little query-discriminating signal and would mostly
 * inflate the noise floor of similarity scores.
 *
 * <p>This is index-time gating only; nothing downstream re-reads the dropped chunks.
 */
public final class ChunkQualityFilter {

    /**
     * Minimum number of distinct alphanumeric tokens (length &ge; 3) required for a chunk to
     * be considered informative enough to embed. Calibrated against the failure mode observed
     * in {@code agenticengineeringworkshop}: padded markdown table headers like
     * {@code "| Field | Description |"} contain ≤2 unique tokens — well under the threshold —
     * while even one prose sentence ("MCP is the Model Context Protocol") has 5+.
     */
    static final int MIN_DISTINCT_TOKENS = 5;

    private ChunkQualityFilter() {} // utility

    /**
     * @return {@code true} if {@code chunk} is likely noise and should not be indexed.
     */
    public static boolean isLowContent(String chunk) {
        if (chunk == null || chunk.isBlank()) return true;
        return countDistinctMeaningfulTokens(chunk) < MIN_DISTINCT_TOKENS;
    }

    /**
     * Count distinct lowercased alphanumeric runs of length &ge; 3. Whitespace and punctuation
     * are token boundaries; case is folded so {@code "Field"} and {@code "field"} don't both
     * count toward the threshold. Visible for tests.
     */
    static int countDistinctMeaningfulTokens(String chunk) {
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        int n = chunk.length();
        int start = -1;
        for (int i = 0; i < n; i++) {
            char c = chunk.charAt(i);
            boolean alnum = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            if (alnum) {
                if (start < 0) start = i;
            } else if (start >= 0) {
                if (i - start >= 3) seen.add(chunk.substring(start, i).toLowerCase());
                start = -1;
            }
        }
        if (start >= 0 && n - start >= 3) seen.add(chunk.substring(start).toLowerCase());
        return seen.size();
    }
}
