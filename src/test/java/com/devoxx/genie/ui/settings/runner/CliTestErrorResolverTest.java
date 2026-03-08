package com.devoxx.genie.ui.settings.runner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CliTestErrorResolverTest {

    @Test
    void sanitizeOutputLine_stripsAnsiEscapes() {
        String input = "\u001B[31mError: authentication failed\u001B[0m";
        assertThat(CliTestErrorResolver.sanitizeOutputLine(input))
                .isEqualTo("Error: authentication failed");
    }

    @Test
    void sanitizeOutputLine_stripsControlCharacters() {
        String input = "Error\u0007: bad \u0008input\u001F here";
        assertThat(CliTestErrorResolver.sanitizeOutputLine(input))
                .isEqualTo("Error: bad input here");
    }

    @Test
    void sanitizeOutputLine_returnsEmptyForBlankInput() {
        assertThat(CliTestErrorResolver.sanitizeOutputLine(null)).isEmpty();
        assertThat(CliTestErrorResolver.sanitizeOutputLine("")).isEmpty();
        assertThat(CliTestErrorResolver.sanitizeOutputLine("   ")).isEmpty();
    }

    @Test
    void sanitizeFullOutput_joinsMultipleLinesWithPipe() {
        String input = "line one\nline two\n\nline three";
        assertThat(CliTestErrorResolver.sanitizeFullOutput(input))
                .isEqualTo("line one | line two | line three");
    }

    @Test
    void sanitizeFullOutput_handlesNullAndBlank() {
        assertThat(CliTestErrorResolver.sanitizeFullOutput(null)).isEmpty();
        assertThat(CliTestErrorResolver.sanitizeFullOutput("")).isEmpty();
        assertThat(CliTestErrorResolver.sanitizeFullOutput("  \n  \n  ")).isEmpty();
    }

    @Test
    void resolve_detectsAuthenticationError() {
        String result = CliTestErrorResolver.resolve(1, "", "Error: authentication failed");
        assertThat(result).startsWith("Authentication failed.");
        assertThat(result).contains("authentication failed");
    }

    @Test
    void resolve_detectsTokenError() {
        String result = CliTestErrorResolver.resolve(1, "", "invalid token provided");
        assertThat(result).startsWith("Authentication failed.");
    }

    @Test
    void resolve_detects401Error() {
        String result = CliTestErrorResolver.resolve(1, "", "HTTP 401 Unauthorized");
        assertThat(result).startsWith("Authentication failed.");
    }

    @Test
    void resolve_detectsPermissionDenied() {
        String result = CliTestErrorResolver.resolve(1, "", "permission denied: /usr/bin/copilot");
        assertThat(result).startsWith("Permission denied.");
    }

    @Test
    void resolve_fallsBackToStdoutWhenStderrEmpty() {
        String result = CliTestErrorResolver.resolve(1, "some stdout error", "");
        assertThat(result).isEqualTo("some stdout error");
    }

    @Test
    void resolve_showsExitCodeWhenNoOutput() {
        String result = CliTestErrorResolver.resolve(42, "", "");
        assertThat(result).isEqualTo("Exit code 42");
    }

    @Test
    void resolve_passesPlainErrorThrough() {
        String result = CliTestErrorResolver.resolve(1, "", "unknown error occurred");
        assertThat(result).isEqualTo("unknown error occurred");
    }

    @Test
    void sanitizeOutputLine_stripsOscSequences() {
        // OSC sequence: \x1B]0;title\x07
        String input = "\u001B]0;Terminal Title\u0007some text";
        assertThat(CliTestErrorResolver.sanitizeOutputLine(input))
                .isEqualTo("some text");
    }

    @Test
    void resolve_handlesAnsiInErrorOutput() {
        String stderr = "\u001B[1m\u001B[31mError\u001B[0m: \u001B[33mlogin\u001B[0m required";
        String result = CliTestErrorResolver.resolve(1, "", stderr);
        assertThat(result).startsWith("Authentication failed.");
        assertThat(result).contains("login required");
        assertThat(result).doesNotContain("\u001B");
    }
}
