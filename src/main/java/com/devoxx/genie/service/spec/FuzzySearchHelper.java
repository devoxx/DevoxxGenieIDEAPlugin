package com.devoxx.genie.service.spec;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Fuzzy search utility for matching queries against task titles, descriptions, and IDs.
 * Supports typo tolerance, partial word matching, out-of-order tokens, and subsequence matching.
 * Returns a relevance score (0.0 to 1.0) to enable ranked search results.
 */
public final class FuzzySearchHelper {

    private static final double DEFAULT_THRESHOLD = 0.3;

    private FuzzySearchHelper() {
    }

    /**
     * Calculates a fuzzy match score between a query and a text.
     *
     * @return score from 0.0 (no match) to 1.0 (exact substring match)
     */
    public static double score(@Nullable String query, @Nullable String text) {
        if (query == null || text == null || query.isEmpty() || text.isEmpty()) {
            return 0.0;
        }

        String lowerQuery = query.toLowerCase();
        String lowerText = text.toLowerCase();

        // Exact substring match - highest possible score
        if (lowerText.contains(lowerQuery)) {
            return 1.0;
        }

        // Compute multiple scoring strategies and return the best
        double tokenScore = tokenMatchScore(lowerQuery, lowerText);
        double subsequenceScore = subsequenceMatchScore(lowerQuery, lowerText);
        double trigramScore = trigramSimilarityScore(lowerQuery, lowerText);

        return Math.max(tokenScore, Math.max(subsequenceScore, trigramScore));
    }

    /**
     * Returns true if the query fuzzy-matches the text using the default threshold.
     */
    public static boolean matches(@Nullable String query, @Nullable String text) {
        return score(query, text) >= DEFAULT_THRESHOLD;
    }

    /**
     * Returns true if the query fuzzy-matches the text using a custom threshold.
     */
    public static boolean matches(@Nullable String query, @Nullable String text, double threshold) {
        return score(query, text) >= threshold;
    }

    /**
     * Computes the best fuzzy score for a query against multiple text fields.
     * Returns the highest score across all fields.
     */
    public static double scoreMultiField(@Nullable String query, @Nullable String @NotNull ... fields) {
        double best = 0.0;
        for (String field : fields) {
            best = Math.max(best, score(query, field));
        }
        return best;
    }

    /**
     * Token-based matching: splits query into words and checks how many appear in the text.
     * Supports partial word matching (query token is a prefix/substring of a text word).
     * Max score: 0.9 (reserved 1.0 for exact substring match).
     */
    static double tokenMatchScore(@NotNull String query, @NotNull String text) {
        String[] queryTokens = query.split("\\s+");
        if (queryTokens.length == 0) {
            return 0.0;
        }

        double totalScore = 0.0;
        int scoredTokens = 0;

        for (String token : queryTokens) {
            if (token.isEmpty()) {
                continue;
            }
            scoredTokens++;

            if (text.contains(token)) {
                // Full token found in text
                totalScore += 1.0;
            } else {
                // Check if any word in text starts with this token (prefix match)
                String[] textWords = text.split("\\s+");
                double bestWordScore = 0.0;
                for (String word : textWords) {
                    if (word.startsWith(token)) {
                        bestWordScore = Math.max(bestWordScore, 0.8);
                    } else if (word.contains(token)) {
                        bestWordScore = Math.max(bestWordScore, 0.6);
                    } else {
                        // Check edit distance for short tokens (typo tolerance)
                        double editScore = editDistanceScore(token, word);
                        bestWordScore = Math.max(bestWordScore, editScore);
                    }
                }
                totalScore += bestWordScore;
            }
        }

        if (scoredTokens == 0) {
            return 0.0;
        }

        return (totalScore / scoredTokens) * 0.9;
    }

    /**
     * Subsequence matching: checks if the characters of the query appear in order in the text.
     * Rewards consecutive character runs.
     * Max score: 0.7
     */
    static double subsequenceMatchScore(@NotNull String query, @NotNull String text) {
        int queryLen = query.length();
        int textLen = text.length();
        if (queryLen == 0 || textLen == 0) {
            return 0.0;
        }

        int qi = 0;
        int matchedConsecutive = 0;
        int totalConsecutiveBonus = 0;
        boolean lastWasMatch = false;

        for (int ti = 0; ti < textLen && qi < queryLen; ti++) {
            if (query.charAt(qi) == text.charAt(ti)) {
                qi++;
                if (lastWasMatch) {
                    matchedConsecutive++;
                    totalConsecutiveBonus += matchedConsecutive;
                } else {
                    matchedConsecutive = 0;
                }
                lastWasMatch = true;
            } else {
                lastWasMatch = false;
                matchedConsecutive = 0;
            }
        }

        // Not all query characters found in order
        if (qi < queryLen) {
            return 0.0;
        }

        // Base score: ratio of query length to text length, capped
        double baseScore = Math.min((double) queryLen / textLen, 1.0);
        // Bonus for consecutive runs
        double consecutiveRatio = (double) totalConsecutiveBonus / queryLen;

        return Math.min((baseScore * 0.5 + consecutiveRatio * 0.3), 0.7);
    }

    /**
     * Trigram (3-character sliding window) similarity.
     * Good for catching typos and character transpositions.
     * Max score: 0.7
     */
    static double trigramSimilarityScore(@NotNull String query, @NotNull String text) {
        Set<String> queryTrigrams = buildTrigrams(query);
        Set<String> textTrigrams = buildTrigrams(text);

        if (queryTrigrams.isEmpty() || textTrigrams.isEmpty()) {
            return 0.0;
        }

        long commonCount = 0;
        for (String trigram : queryTrigrams) {
            if (textTrigrams.contains(trigram)) {
                commonCount++;
            }
        }

        // Dice coefficient: 2 * |intersection| / (|A| + |B|)
        double dice = (2.0 * commonCount) / (queryTrigrams.size() + textTrigrams.size());
        return dice * 0.7;
    }

    /**
     * Edit distance score between two individual words.
     * Uses Levenshtein distance normalized to a 0-1 score.
     * Only applies when words are of similar length (to avoid matching wildly different words).
     */
    static double editDistanceScore(@NotNull String a, @NotNull String b) {
        // Only consider edit distance for reasonably similar-length words
        int lenDiff = Math.abs(a.length() - b.length());
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 0.0;
        }
        if (lenDiff > Math.max(2, maxLen / 3)) {
            return 0.0;
        }

        int distance = levenshteinDistance(a, b);
        int threshold = Math.max(1, maxLen / 3);
        if (distance > threshold) {
            return 0.0;
        }

        return (1.0 - (double) distance / maxLen) * 0.6;
    }

    private static int levenshteinDistance(@NotNull String a, @NotNull String b) {
        int lenA = a.length();
        int lenB = b.length();
        int[] prev = new int[lenB + 1];
        int[] curr = new int[lenB + 1];

        for (int j = 0; j <= lenB; j++) {
            prev[j] = j;
        }

        for (int i = 1; i <= lenA; i++) {
            curr[0] = i;
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }

        return prev[lenB];
    }

    private static @NotNull Set<String> buildTrigrams(@NotNull String s) {
        Set<String> trigrams = new HashSet<>();
        for (int i = 0; i <= s.length() - 3; i++) {
            trigrams.add(s.substring(i, i + 3));
        }
        return trigrams;
    }
}
