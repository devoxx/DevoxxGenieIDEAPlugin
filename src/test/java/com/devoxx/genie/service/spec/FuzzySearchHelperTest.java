package com.devoxx.genie.service.spec;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FuzzySearchHelperTest {

    // ── Exact match ───────────────────────────────────────────────────────

    @Nested
    class ExactMatch {

        @Test
        void exactSubstring_returnsMaxScore() {
            assertThat(FuzzySearchHelper.score("authentication", "Implement user authentication flow"))
                    .isEqualTo(1.0);
        }

        @Test
        void caseInsensitive_exactMatch() {
            assertThat(FuzzySearchHelper.score("AUTH", "user authentication system"))
                    .isEqualTo(1.0);
        }

        @Test
        void singleCharacterQuery_exactMatch() {
            assertThat(FuzzySearchHelper.score("a", "abc"))
                    .isEqualTo(1.0);
        }

        @Test
        void fullStringMatch() {
            assertThat(FuzzySearchHelper.score("hello world", "hello world"))
                    .isEqualTo(1.0);
        }
    }

    // ── Typo tolerance ────────────────────────────────────────────────────

    @Nested
    class TypoTolerance {

        @Test
        void singleCharacterTypo_shouldMatch() {
            double score = FuzzySearchHelper.score("authentcation", "authentication system");
            assertThat(score).isGreaterThan(0.0);
            assertThat(FuzzySearchHelper.matches("authentcation", "authentication system")).isTrue();
        }

        @Test
        void transposedCharacters_shouldMatch() {
            double score = FuzzySearchHelper.score("authetincation", "authentication system");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        void missingCharacter_shouldMatch() {
            // "architectre" missing a 'u'
            double score = FuzzySearchHelper.score("architectre", "architecture overview");
            assertThat(score).isGreaterThan(0.0);
            assertThat(FuzzySearchHelper.matches("architectre", "architecture overview")).isTrue();
        }

        @Test
        void extraCharacter_shouldMatch() {
            double score = FuzzySearchHelper.score("authenticcation", "authentication");
            assertThat(score).isGreaterThan(0.0);
        }
    }

    // ── Partial word matching ─────────────────────────────────────────────

    @Nested
    class PartialWordMatching {

        @Test
        void prefixMatch_shouldScore() {
            double score = FuzzySearchHelper.score("auth", "authentication module");
            assertThat(score).isGreaterThan(0.0);
            // "auth" is a substring, so exact contains should give 1.0
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        void partialWordInMultiWordQuery() {
            // "impl auth" -> "impl" matches "implement", "auth" matches "authentication"
            double score = FuzzySearchHelper.score("impl auth", "Implement authentication");
            assertThat(score).isGreaterThan(0.0);
        }
    }

    // ── Token matching ────────────────────────────────────────────────────

    @Nested
    class TokenMatching {

        @Test
        void allTokensPresent_highScore() {
            double score = FuzzySearchHelper.score("user login", "Implement user login feature");
            assertThat(score).isGreaterThan(0.5);
        }

        @Test
        void tokensInDifferentOrder_shouldMatch() {
            double score = FuzzySearchHelper.score("login user", "Implement user login feature");
            assertThat(score).isGreaterThan(0.5);
        }

        @Test
        void someTokensMissing_lowerScore() {
            double scoreAll = FuzzySearchHelper.score("user login", "Implement user login feature");
            double scorePartial = FuzzySearchHelper.score("user missing", "Implement user login feature");
            assertThat(scoreAll).isGreaterThan(scorePartial);
        }

        @Test
        void singleTokenMatch_shouldWork() {
            double score = FuzzySearchHelper.score("login", "Implement login feature");
            assertThat(score).isEqualTo(1.0); // exact substring
        }
    }

    // ── Subsequence matching ──────────────────────────────────────────────

    @Nested
    class SubsequenceMatching {

        @Test
        void abbreviation_matchesSubsequence() {
            // "flt" as subsequence of "filter"
            double score = FuzzySearchHelper.score("flt", "Apply filter to results");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        void nonMatchingSubsequence_zeroScore() {
            // "zyx" has no subsequence in "abc"
            double score = FuzzySearchHelper.subsequenceMatchScore("zyx", "abc");
            assertThat(score).isEqualTo(0.0);
        }
    }

    // ── Null / empty handling ─────────────────────────────────────────────

    @Nested
    class NullAndEmptyHandling {

        @Test
        void nullQuery_returnsZero() {
            assertThat(FuzzySearchHelper.score(null, "some text")).isEqualTo(0.0);
        }

        @Test
        void nullText_returnsZero() {
            assertThat(FuzzySearchHelper.score("query", null)).isEqualTo(0.0);
        }

        @Test
        void emptyQuery_returnsZero() {
            assertThat(FuzzySearchHelper.score("", "some text")).isEqualTo(0.0);
        }

        @Test
        void emptyText_returnsZero() {
            assertThat(FuzzySearchHelper.score("query", "")).isEqualTo(0.0);
        }

        @Test
        void bothNull_returnsZero() {
            assertThat(FuzzySearchHelper.score(null, null)).isEqualTo(0.0);
        }

        @Test
        void nullQuery_doesNotMatch() {
            assertThat(FuzzySearchHelper.matches(null, "text")).isFalse();
        }
    }

    // ── matches() threshold ───────────────────────────────────────────────

    @Nested
    class MatchesThreshold {

        @Test
        void exactSubstring_matchesDefaultThreshold() {
            assertThat(FuzzySearchHelper.matches("task", "This is a task")).isTrue();
        }

        @Test
        void completelyUnrelated_doesNotMatch() {
            assertThat(FuzzySearchHelper.matches("xyzzy", "Implement authentication")).isFalse();
        }

        @Test
        void customThreshold_stricter() {
            // Something that might pass 0.3 but not 0.95
            String query = "authentcation"; // typo
            String text = "authentication system";
            assertThat(FuzzySearchHelper.matches(query, text, 0.3)).isTrue();
            assertThat(FuzzySearchHelper.matches(query, text, 0.95)).isFalse();
        }
    }

    // ── scoreMultiField ───────────────────────────────────────────────────

    @Nested
    class MultiFieldScoring {

        @Test
        void returnsHighestScoreAcrossFields() {
            double score = FuzzySearchHelper.scoreMultiField("authentication",
                    "Login feature", // no match
                    "Implement authentication flow" // exact match
            );
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        void allFieldsNull_returnsZero() {
            double score = FuzzySearchHelper.scoreMultiField("query", null, null, null);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void mixedNullAndValid_returnsValidScore() {
            double score = FuzzySearchHelper.scoreMultiField("test",
                    null,
                    "Running unit tests",
                    null
            );
            assertThat(score).isEqualTo(1.0); // "test" is substring of "tests"
        }

        @Test
        void noFieldMatches_returnsZero() {
            double score = FuzzySearchHelper.scoreMultiField("xyzzy",
                    "Implement login",
                    "Add user registration"
            );
            assertThat(score).isEqualTo(0.0);
        }
    }

    // ── Relevance ranking ─────────────────────────────────────────────────

    @Nested
    class RelevanceRanking {

        @Test
        void exactMatch_scoresHigherThanFuzzy() {
            double exact = FuzzySearchHelper.score("authentication", "authentication module");
            double fuzzy = FuzzySearchHelper.score("authentcation", "authentication module");
            assertThat(exact).isGreaterThan(fuzzy);
        }

        @Test
        void titleMatch_scoresHigherThanDescriptionTypo() {
            // Exact match in one field should beat fuzzy match in another
            double exactTitle = FuzzySearchHelper.scoreMultiField("login",
                    "User login page",
                    "Some unrelated description"
            );
            double fuzzyDesc = FuzzySearchHelper.scoreMultiField("logn",
                    "Unrelated title",
                    "User login feature"
            );
            assertThat(exactTitle).isGreaterThan(fuzzyDesc);
        }

        @Test
        void completeTokenMatch_scoresHigherThanPartialToken() {
            double complete = FuzzySearchHelper.score("search feature", "Add search feature to app");
            double partial = FuzzySearchHelper.score("sear feat", "Add search feature to app");
            assertThat(complete).isGreaterThan(partial);
        }
    }

    // ── Internal scoring methods ──────────────────────────────────────────

    @Nested
    class InternalScoring {

        @Test
        void tokenMatchScore_allTokensPresent() {
            double score = FuzzySearchHelper.tokenMatchScore("add search", "add search functionality");
            assertThat(score).isGreaterThan(0.5);
        }

        @Test
        void tokenMatchScore_noTokensPresent() {
            double score = FuzzySearchHelper.tokenMatchScore("xyz abc", "hello world");
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void tokenMatchScore_emptyQuery() {
            double score = FuzzySearchHelper.tokenMatchScore("", "some text");
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void subsequenceMatchScore_fullSequence() {
            double score = FuzzySearchHelper.subsequenceMatchScore("abc", "aXbYcZ");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        void subsequenceMatchScore_noSequence() {
            double score = FuzzySearchHelper.subsequenceMatchScore("cba", "abc");
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void trigramSimilarityScore_similarStrings() {
            double score = FuzzySearchHelper.trigramSimilarityScore("authentication", "authenticaton");
            assertThat(score).isGreaterThan(0.3);
        }

        @Test
        void trigramSimilarityScore_differentStrings() {
            double score = FuzzySearchHelper.trigramSimilarityScore("xyz", "abc");
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void trigramSimilarityScore_tooShort() {
            double score = FuzzySearchHelper.trigramSimilarityScore("ab", "abcd");
            assertThat(score).isEqualTo(0.0); // "ab" has no trigrams
        }

        @Test
        void editDistanceScore_identicalWords() {
            double score = FuzzySearchHelper.editDistanceScore("hello", "hello");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        void editDistanceScore_oneCharDifference() {
            double score = FuzzySearchHelper.editDistanceScore("hello", "hallo");
            assertThat(score).isGreaterThan(0.0);
        }

        @Test
        void editDistanceScore_veryDifferent() {
            double score = FuzzySearchHelper.editDistanceScore("hello", "xyzzy");
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        void editDistanceScore_veryDifferentLengths() {
            double score = FuzzySearchHelper.editDistanceScore("hi", "authentication");
            assertThat(score).isEqualTo(0.0);
        }
    }

    // ── Real-world backlog search scenarios ────────────────────────────────

    @Nested
    class RealWorldScenarios {

        @Test
        void searchByTaskId() {
            assertThat(FuzzySearchHelper.matches("TASK-1", "TASK-1")).isTrue();
            assertThat(FuzzySearchHelper.matches("task-1", "TASK-1")).isTrue();
        }

        @Test
        void searchByPartialTitle() {
            assertThat(FuzzySearchHelper.matches("auth", "Implement user authentication")).isTrue();
        }

        @Test
        void searchWithTypoInTaskTitle() {
            assertThat(FuzzySearchHelper.matches("implemnt", "Implement login feature")).isTrue();
        }

        @Test
        void searchAcrossMultipleWords() {
            assertThat(FuzzySearchHelper.matches("add search", "Add core search functionality")).isTrue();
        }

        @Test
        void searchWordsReversed() {
            assertThat(FuzzySearchHelper.matches("search add", "Add core search functionality")).isTrue();
        }

        @Test
        void searchUnrelatedTerm_doesNotMatch() {
            assertThat(FuzzySearchHelper.matches("database migration", "Implement user login feature")).isFalse();
        }

        @Test
        void searchWithAcceptanceCriteriaContent() {
            String description = "Tests pass\nDocumentation updated\nNo regressions introduced";
            assertThat(FuzzySearchHelper.matches("regression", description)).isTrue();
        }

        @Test
        void searchLabels() {
            assertThat(FuzzySearchHelper.matches("bug", "bug")).isTrue();
            assertThat(FuzzySearchHelper.matches("featre", "feature")).isTrue(); // typo
        }

        @Test
        void searchPriority() {
            assertThat(FuzzySearchHelper.matches("high", "high priority task")).isTrue();
        }
    }
}
