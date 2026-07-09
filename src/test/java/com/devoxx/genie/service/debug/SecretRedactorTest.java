package com.devoxx.genie.service.debug;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretRedactorTest {

    @Test
    void redact_nullOrEmpty_returnsAsIs() {
        assertThat(SecretRedactor.redact(null)).isNull();
        assertThat(SecretRedactor.redact("")).isEmpty();
    }

    @Test
    void redact_plainTextWithoutSecrets_isUnchanged() {
        String text = "{\"role\":\"user\",\"content\":\"Explain this Java method\"}";
        assertThat(SecretRedactor.redact(text)).isEqualTo(text);
    }

    @Test
    void redact_bearerToken_masksTokenKeepsPrefix() {
        String text = "Authorization header: Bearer sk-abcdef1234567890ABCDEF";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).contains("Bearer ");
        assertThat(redacted).doesNotContain("sk-abcdef1234567890ABCDEF");
        assertThat(redacted).contains("****");
    }

    @Test
    void redact_openAiStyleKey_isMasked() {
        String text = "key=sk-proj-abcdefghijklmnopqrstuvwxyz1234567890";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).doesNotContain("sk-proj-abcdefghijklmnopqrstuvwxyz1234567890");
        assertThat(redacted).contains("sk-p").contains("7890");
    }

    @Test
    void redact_awsAccessKey_isMasked() {
        String text = "aws_access_key_id = AKIAABCDEFGHIJKLMNOP";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).doesNotContain("AKIAABCDEFGHIJKLMNOP");
    }

    @Test
    void redact_jsonSecretField_masksValueOnly() {
        String text = "{\"apiKey\":\"abcdefghijklmnop\",\"model\":\"gpt-4o\"}";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).contains("\"apiKey\":\"abcd****mnop\"");
        assertThat(redacted).contains("\"model\":\"gpt-4o\"");
    }

    @Test
    void redact_shortSecret_masksCompletely() {
        String text = "\"password\":\"ab12\"";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).contains("****");
        assertThat(redacted).doesNotContain("ab12");
    }

    @Test
    void redact_secretContainingDollarSign_doesNotThrowIllegalGroupReference() {
        // Matcher.replaceAll(Function) feeds the produced string through appendReplacement,
        // which interprets '$' as a group reference unless quoted. A secret like "pa$$word..."
        // used to throw IllegalArgumentException, silently dropping the log entry.
        String text = "{\"password\":\"pa$$word12345\"}";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).isEqualTo("{\"password\":\"pa$$****2345\"}");
    }

    @Test
    void redact_secretContainingBackslash_isMaskedWithoutCorruption() {
        // Unquoted '\' in the replacement is treated as an escape by appendReplacement and
        // used to be silently swallowed from the masked output.
        String text = "{\"apiKey\":\"a\\bcdefghijklmn\"}";
        String redacted = SecretRedactor.redact(text);
        assertThat(redacted).isEqualTo("{\"apiKey\":\"a\\bc****klmn\"}");
    }
}
