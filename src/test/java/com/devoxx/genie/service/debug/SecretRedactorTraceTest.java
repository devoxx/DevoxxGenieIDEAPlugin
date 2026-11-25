package com.devoxx.genie.service.debug;

import com.devoxx.genie.service.debug.SecretRedactor.TraceRedaction;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixture tokens are split into concatenated pieces ("sk_" + "live_…") so the source never
 * contains a string that GitHub push protection or other secret scanners would flag. They are
 * still compile-time constants, so they work inside annotations.
 */
class SecretRedactorTraceTest {

    private static String redact(String text) {
        return SecretRedactor.redactForTrace(text, TraceRedactionOptions.SECRETS_ONLY).text();
    }

    @Test
    void nullAndEmpty_areReturnedUnchanged() {
        assertThat(SecretRedactor.redactForTrace(null, TraceRedactionOptions.SECRETS_ONLY).text()).isNull();
        assertThat(SecretRedactor.redactForTrace("", TraceRedactionOptions.SECRETS_ONLY).text()).isEmpty();
    }

    @Test
    void plainText_isUnchanged_andCountIsZero() {
        TraceRedaction r = SecretRedactor.redactForTrace("Read src/Main.java and fix the failing test",
                TraceRedactionOptions.SECRETS_ONLY);
        assertThat(r.text()).isEqualTo("Read src/Main.java and fix the failing test");
        assertThat(r.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "token hf" + "_abcdefghijklmnopqrstuvwxyz0123456789 end | HF_TOKEN",
            "gh" + "p_abcdefghijklmnopqrstuvwxyz0123456789AB | GITHUB_TOKEN",
            "github" + "_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz0123456789 | GITHUB_PAT",
            "sk-" + "ant-api03-abcdefghijklmnopqrstuvwxyz | ANTHROPIC_KEY",
            "sk-" + "proj-abcdefghijklmnopqrstuvwxyz1234 | OPENAI_KEY",
            "sk_" + "live_" + "abcdefghijklmnopqrstuvwx | STRIPE_KEY",
            "gs" + "k_abcdefghijklmnopqrstuvwxyz | GROQ_KEY",
            "AKIA" + "ABCDEFGHIJKLMNOP | AWS_ACCESS_KEY",
            "AIza" + "SyA-abcdefghijklmnopqrstuvwxyz01234 | GOOGLE_API_KEY",
            "xo" + "xb-1234567890-abcdefghij | SLACK_TOKEN",
            "eyJhbGciOiJIUzI1NiJ9" + ".eyJzdWIiOiIxMjM0NTY3ODkwIn0.abcdefghijklmnopqrstu | JWT",
            "Authorization: Bearer abcdefgh12345678                                    | BEARER_TOKEN",
            "password=hunter2hunter2                                                   | SECRET",
            "api_key: 'abcdef123456'                                                   | SECRET",
            "{\"client_secret\": \"abcdefghijkl\"}                                     | SECRET",
    })
    void knownSecretShapes_areReplacedWithLabelledPlaceholder(String input, String label) {
        String redacted = redact(input);
        assertThat(redacted).contains("[REDACTED_" + label + "]");
    }

    @Test
    void anthropicKey_isLabelledAnthropic_notOpenAi() {
        assertThat(redact("sk-" + "ant-api03-abcdefghijklmnopqrstuvwxyz"))
                .isEqualTo("[REDACTED_ANTHROPIC_KEY]");
    }

    @Test
    void wholeSecretIsRemoved_noPrefixOrSuffixKept() {
        String secret = "gh" + "p_abcdefghijklmnopqrstuvwxyz0123456789AB";
        String redacted = redact("export GH=" + secret);
        assertThat(redacted).isEqualTo("export GH=[REDACTED_GITHUB_TOKEN]");
        assertThat(redacted).doesNotContain("ghp_").doesNotContain("89AB");
    }

    @Test
    void keyValueRule_keepsTheKeyName() {
        assertThat(redact("password = s3cretValue!")).isEqualTo("password = [REDACTED_SECRET]");
    }

    @Test
    void keyValueRule_doesNotReRedactAPlaceholder() {
        String redacted = redact("api_key=sk-" + "proj-abcdefghijklmnopqrstuvwxyz1234");
        assertThat(redacted).isEqualTo("api_key=[REDACTED_OPENAI_KEY]");
    }

    @Test
    void privateKeyBlock_isRemovedEntirely() {
        String pem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEow\nabc\n-----END RSA PRIVATE KEY-----";
        assertThat(redact("key:\n" + pem + "\ndone")).isEqualTo("key:\n[REDACTED_PRIVATE_KEY]\ndone");
    }

    @Test
    void dollarAndBackslashInSecret_doNotBreakReplacement() {
        String redacted = redact("password=ab$1cd\\9efgh and more");
        assertThat(redacted).isEqualTo("password=[REDACTED_SECRET] and more");
    }

    @Test
    void count_reportsEveryReplacement() {
        TraceRedaction r = SecretRedactor.redactForTrace(
                "AKIA" + "ABCDEFGHIJKLMNOP and AKIA" + "QRSTUVWXYZ012345", TraceRedactionOptions.SECRETS_ONLY);
        assertThat(r.count()).isEqualTo(2);
    }

    @Test
    void homeDirectory_isReplacedWithTilde_beforeUsername() {
        TraceRedactionOptions opts = TraceRedactionOptions.of("/home/alice", "alice", List.of());
        String redacted = SecretRedactor.redactForTrace(
                "cat /home/alice/project/a.txt as alice", opts).text();
        assertThat(redacted).isEqualTo("cat ~/project/a.txt as <user>");
    }

    @Test
    void username_isOnlyReplacedAsAWholeWord() {
        TraceRedactionOptions opts = TraceRedactionOptions.of(null, "sam", List.of());
        assertThat(SecretRedactor.redactForTrace("sample by sam", opts).text()).isEqualTo("sample by <user>");
    }

    @Test
    void shortUsernameOrHome_isIgnored() {
        TraceRedactionOptions opts = TraceRedactionOptions.of("/a", "al", List.of());
        assertThat(SecretRedactor.redactForTrace("/a/b al", opts).text()).isEqualTo("/a/b al");
    }

    @Test
    void windowsHome_matchesBackslashForwardSlashAndJsonEscapedForms() {
        TraceRedactionOptions opts = TraceRedactionOptions.of("C:\\Users\\bob", null, List.of());
        assertThat(SecretRedactor.redactForTrace("C:\\Users\\bob\\x", opts).text()).isEqualTo("~\\x");
        assertThat(SecretRedactor.redactForTrace("C:/Users/bob/x", opts).text()).isEqualTo("~/x");
        assertThat(SecretRedactor.redactForTrace("{\"p\":\"C:\\\\Users\\\\bob\\\\x\"}", opts).text())
                .isEqualTo("{\"p\":\"~\\\\x\"}");
    }

    @Test
    void customPatterns_areApplied_andInvalidOnesSkipped() {
        TraceRedactionOptions opts = TraceRedactionOptions.of(null, null, List.of("ACME-\\d{4}", "([unclosed", " "));
        TraceRedaction r = SecretRedactor.redactForTrace("ticket ACME-1234 and ACME-99", opts);
        assertThat(r.text()).isEqualTo("ticket [REDACTED] and ACME-99");
        assertThat(r.count()).isEqualTo(1);
    }

    @Test
    void activityLogRedact_isUnchanged_byTraceRules() {
        // The existing Activity Log masking keeps 4+4 characters; trace redaction must not alter it.
        assertThat(SecretRedactor.redact("key=sk-" + "proj-abcdefghijklmnopqrstuvwxyz1234567890"))
                .contains("sk-p").contains("7890");
    }
}
