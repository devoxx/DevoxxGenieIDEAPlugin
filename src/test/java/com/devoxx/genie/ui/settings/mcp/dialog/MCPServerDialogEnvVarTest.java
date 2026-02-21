package com.devoxx.genie.ui.settings.mcp.dialog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MCPServerDialog.EnvVarSensitivityChecker — the pure sensitivity-check
 * logic extracted from the EnvVarDialog constructor to reduce cognitive complexity.
 */
class MCPServerDialogEnvVarTest {

    private MCPServerDialog.EnvVarSensitivityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new MCPServerDialog.EnvVarSensitivityChecker();
    }

    @Nested
    class IsSensitive {

        @Test
        void nullKeyReturnsFalse() {
            assertThat(checker.isSensitive(null)).isFalse();
        }

        @Test
        void emptyKeyReturnsFalse() {
            assertThat(checker.isSensitive("")).isFalse();
        }

        @Test
        void plainKeyReturnsFalse() {
            assertThat(checker.isSensitive("DATABASE_HOST")).isFalse();
        }

        @Test
        void keyContainingPasswordIsDetected() {
            assertThat(checker.isSensitive("DB_PASSWORD")).isTrue();
        }

        @Test
        void keyContainingSecretIsDetected() {
            assertThat(checker.isSensitive("CLIENT_SECRET")).isTrue();
        }

        @Test
        void keyContainingTokenIsDetected() {
            assertThat(checker.isSensitive("ACCESS_TOKEN")).isTrue();
        }

        @Test
        void keyContainingApiIsDetected() {
            assertThat(checker.isSensitive("OPENAI_API_KEY")).isTrue();
        }

        @Test
        void keyContainingAuthIsDetected() {
            assertThat(checker.isSensitive("AUTH_HEADER")).isTrue();
        }

        @Test
        void keyContainingCredentialIsDetected() {
            assertThat(checker.isSensitive("AWS_CREDENTIAL")).isTrue();
        }

        @Test
        void keyContainingPwdIsDetected() {
            assertThat(checker.isSensitive("USER_PWD")).isTrue();
        }

        @Test
        void keyContainingPassIsDetected() {
            assertThat(checker.isSensitive("PASS_PHRASE")).isTrue();
        }

        @Test
        void detectionIsCaseInsensitive() {
            assertThat(checker.isSensitive("My_Secret_Value")).isTrue();
            assertThat(checker.isSensitive("MY_SECRET_VALUE")).isTrue();
            assertThat(checker.isSensitive("my_secret_value")).isTrue();
        }

        @Test
        void unrelatedKeywordNotMatched() {
            assertThat(checker.isSensitive("TIMEOUT")).isFalse();
            assertThat(checker.isSensitive("RETRY_COUNT")).isFalse();
            assertThat(checker.isSensitive("BASE_URL")).isFalse();
        }
    }
}
