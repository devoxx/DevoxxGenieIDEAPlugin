package com.devoxx.genie.service.trace;

import com.devoxx.genie.service.trace.TruffleHogScanner.FindingStatus;
import com.devoxx.genie.service.trace.TruffleHogScanner.ParsedLine;
import com.devoxx.genie.service.trace.TruffleHogScanner.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TruffleHogScannerTest {

    @TempDir
    Path tempDir;

    // ---------------------------------------------------------------------------------------
    // Output parsing (port of trufflehog.ts parseLine)
    // ---------------------------------------------------------------------------------------

    @Test
    void parseLine_finding_extractsDetectorFileRawAndStatus() {
        ParsedLine parsed = TruffleHogScanner.parseLine("""
                {"DetectorName":"Github","Verified":false,"Raw":"ghp_secretsecretsecret",
                 "SourceMetadata":{"Data":{"Filesystem":{"file":"/tmp/x/e0-content.txt","line":3}}}}"""
                .replace("\n", ""));

        assertThat(parsed).isNotNull();
        assertThat(parsed.file()).isEqualTo("/tmp/x/e0-content.txt");
        assertThat(parsed.finding().detector()).isEqualTo("Github");
        assertThat(parsed.finding().raw()).isEqualTo("ghp_secretsecretsecret");
        assertThat(parsed.finding().status()).isEqualTo(FindingStatus.UNVERIFIED);
        assertThat(parsed.finding().masked()).isEqualTo("ghp_…et (22 chars)");
    }

    @Test
    void parseLine_verifiedAndUnknownStatuses() {
        String file = "\"SourceMetadata\":{\"Data\":{\"Filesystem\":{\"file\":\"f\"}}}";
        assertThat(TruffleHogScanner.parseLine("{\"DetectorName\":\"A\",\"Verified\":true,\"Raw\":\"r\"," + file + "}")
                .finding().status()).isEqualTo(FindingStatus.VERIFIED);
        assertThat(TruffleHogScanner.parseLine("{\"DetectorName\":\"A\",\"VerificationError\":\"timeout\",\"Raw\":\"r\"," + file + "}")
                .finding().status()).isEqualTo(FindingStatus.UNKNOWN);
        assertThat(TruffleHogScanner.parseLine("{\"DetectorName\":\"A\",\"ExtraData\":{\"verification_error\":\"x\"},\"Raw\":\"r\"," + file + "}")
                .finding().status()).isEqualTo(FindingStatus.UNKNOWN);
    }

    @Test
    void parseLine_ignoresLogLinesAndNonFilesystemFindings() {
        assertThat(TruffleHogScanner.parseLine("{\"level\":\"info-0\",\"msg\":\"running source\"}")).isNull();
        assertThat(TruffleHogScanner.parseLine("🐷🔑🐷  TruffleHog. Unearth your secrets. 🐷🔑🐷")).isNull();
        assertThat(TruffleHogScanner.parseLine("{not json")).isNull();
        assertThat(TruffleHogScanner.parseLine("{\"DetectorName\":\"A\",\"SourceMetadata\":{\"Data\":{\"Git\":{}}}}")).isNull();
        assertThat(TruffleHogScanner.parseLine(null)).isNull();
    }

    @Test
    void finding_toString_neverContainsTheRawSecret() {
        TruffleHogScanner.Finding f = new TruffleHogScanner.Finding("Github", FindingStatus.VERIFIED,
                "ghp_verysecretvalue", TruffleHogScanner.mask("ghp_verysecretvalue"));
        assertThat(f.toString()).doesNotContain("ghp_verysecretvalue");
    }

    @Test
    void mask_shortSecrets_areFullyStarred() {
        assertThat(TruffleHogScanner.mask("abc")).isEqualTo("***");
    }

    @Test
    void placeholder_normalisesDetectorName() {
        assertThat(TruffleHogScanner.placeholder("AWS-Session key")).isEqualTo("[REDACTED_TRUFFLEHOG_AWS_SESSION_KEY]");
    }

    // ---------------------------------------------------------------------------------------
    // Scanning with a fake trufflehog binary
    // ---------------------------------------------------------------------------------------

    /**
     * Fake trufflehog: {@code --version} succeeds; {@code filesystem DIR ...} reports every
     * FAKESECRET_* token found in DIR's files as a finding, records its arguments, and exits
     * with $FAKE_EXIT (written into the script).
     */
    private Path fakeTruffleHog(int exitCode, int sleepSeconds) throws IOException {
        Path script = tempDir.resolve("trufflehog-fake-" + exitCode + "-" + sleepSeconds);
        Path argsFile = tempDir.resolve("args.txt");
        Files.writeString(script, """
                #!/bin/sh
                if [ "$1" = "--version" ]; then echo "trufflehog 3.0.0-fake"; exit 0; fi
                echo "$@" > "%s"
                sleep %d
                dir="$2"
                echo '{"level":"info-0","msg":"running source"}'
                for f in "$dir"/*; do
                  for raw in $(grep -o 'FAKESECRET_[A-Z0-9]*' "$f" | sort -u); do
                    printf '{"DetectorName":"FakeDetector","Verified":false,"Raw":"%%s","SourceMetadata":{"Data":{"Filesystem":{"file":"%%s","line":1}}}}\\n' "$raw" "$f"
                  done
                done
                exit %d
                """.formatted(argsFile, sleepSeconds, exitCode));
        script.toFile().setExecutable(true);
        return script;
    }

    private static List<TraceEvent> turnWith(String toolResult, String toolArgs, String reasoning) {
        return List.of(
                TraceEvent.user("deploy it", null, 1),
                TraceEvent.assistant("", reasoning,
                        List.of(new TraceEvent.ToolCall("c1", "run_command", toolArgs)), "m", null, 2),
                TraceEvent.tool("c1", "run_command", toolResult, false, null, 3),
                TraceEvent.assistant("Done.", null, List.of(), "m", null, 4));
    }

    private long scanDirsLeft() throws IOException {
        try (Stream<Path> s = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return s.filter(p -> p.getFileName().toString().startsWith("devoxxgenie-trace-scan-")).count();
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_replacesFindingsInEveryField_andDeletesTempFiles() throws IOException {
        long before = scanDirsLeft();
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(30));
        List<TraceEvent> events = turnWith("token=FAKESECRET_ONE ok", "{\"cmd\":\"echo FAKESECRET_TWO\"}",
                "remember FAKESECRET_ONE");

        ScanResult result = scanner.scan(events, fakeTruffleHog(0, 0).toString(), false);

        assertThat(result).isInstanceOf(ScanResult.Completed.class);
        ScanResult.Completed completed = (ScanResult.Completed) result;
        assertThat(completed.allRemoved()).isTrue();
        assertThat(completed.findings()).hasSize(3);
        assertThat(completed.events().get(2).content()).isEqualTo("token=[REDACTED_TRUFFLEHOG_FAKEDETECTOR] ok");
        assertThat(completed.events().get(1).toolCalls().get(0).arguments())
                .isEqualTo("{\"cmd\":\"echo [REDACTED_TRUFFLEHOG_FAKEDETECTOR]\"}");
        assertThat(completed.events().get(1).reasoning()).isEqualTo("remember [REDACTED_TRUFFLEHOG_FAKEDETECTOR]");
        assertThat(completed.events().get(0)).isEqualTo(events.get(0));
        assertThat(scanDirsLeft()).isEqualTo(before);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_passesNoVerificationUnlessVerifyIsOn() throws IOException {
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(30));
        Path fake = fakeTruffleHog(0, 0);

        scanner.scan(turnWith("x", "{}", null), fake.toString(), false);
        String args = Files.readString(tempDir.resolve("args.txt"));
        assertThat(args).startsWith("filesystem ").contains("--json").contains("--no-update")
                .contains("--results=verified,unknown,unverified").contains("--no-verification");

        scanner.scan(turnWith("x", "{}", null), fake.toString(), true);
        assertThat(Files.readString(tempDir.resolve("args.txt"))).doesNotContain("--no-verification");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_cleanTurn_hasNoFindings() throws IOException {
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(30));
        List<TraceEvent> events = turnWith("all good", "{}", null);

        ScanResult.Completed completed = (ScanResult.Completed) scanner.scan(events, fakeTruffleHog(0, 0).toString(), false);

        assertThat(completed.findings()).isEmpty();
        assertThat(completed.events()).isEqualTo(events);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_exitCode183_isSuccess() throws IOException {
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(30));
        ScanResult result = scanner.scan(turnWith("FAKESECRET_X", "{}", null), fakeTruffleHog(183, 0).toString(), false);
        assertThat(result).isInstanceOf(ScanResult.Completed.class);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_otherNonZeroExit_isAFailedScan() throws IOException {
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(30));
        ScanResult result = scanner.scan(turnWith("x", "{}", null), fakeTruffleHog(2, 0).toString(), false);
        assertThat(result).isInstanceOf(ScanResult.Failed.class);
        assertThat(((ScanResult.Failed) result).reason()).contains("code 2");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void scan_timeout_isAFailedScan_andCleansUp() throws IOException {
        long before = scanDirsLeft();
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofMillis(300));
        ScanResult result = scanner.scan(turnWith("x", "{}", null), fakeTruffleHog(0, 5).toString(), false);
        assertThat(result).isInstanceOf(ScanResult.Failed.class);
        assertThat(((ScanResult.Failed) result).reason()).contains("timed out");
        assertThat(scanDirsLeft()).isEqualTo(before);
    }

    @Test
    void scan_missingBinary_isNotFound() {
        TruffleHogScanner scanner = new TruffleHogScanner(Duration.ofSeconds(5));
        String missing = tempDir.resolve("no-such-trufflehog").toString();

        assertThat(scanner.isAvailable(missing)).isFalse();
        assertThat(scanner.scan(turnWith("x", "{}", null), missing, false)).isInstanceOf(ScanResult.NotFound.class);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void isAvailable_trueForWorkingBinary() throws IOException {
        assertThat(new TruffleHogScanner().isAvailable(fakeTruffleHog(0, 0).toString())).isTrue();
    }

    @Test
    void applyFindings_whenSecretIsNotInTheField_reportsNotAllRemoved() {
        List<TraceEvent> events = turnWith("nothing here", "{}", null);
        TruffleHogScanner.FieldRef ref = new TruffleHogScanner.FieldRef(2, TruffleHogScanner.Field.CONTENT, -1);
        TruffleHogScanner.Finding unknownRaw = new TruffleHogScanner.Finding("X", FindingStatus.UNVERIFIED, null, "[REDACTED]");

        ScanResult.Completed completed = TruffleHogScanner.applyFindings(events, Map.of(ref, List.of(unknownRaw)));

        assertThat(completed.allRemoved()).isFalse();
    }

    @Test
    void applyFindings_secretWithRegexCharacters_isReplacedLiterally() {
        List<TraceEvent> events = turnWith("pw: a$b\\c(d)*", "{}", null);
        TruffleHogScanner.FieldRef ref = new TruffleHogScanner.FieldRef(2, TruffleHogScanner.Field.CONTENT, -1);
        TruffleHogScanner.Finding f = new TruffleHogScanner.Finding("Generic", FindingStatus.UNVERIFIED, "a$b\\c(d)*", "***");

        ScanResult.Completed completed = TruffleHogScanner.applyFindings(events, Map.of(ref, List.of(f)));

        assertThat(completed.allRemoved()).isTrue();
        assertThat(completed.events().get(2).content()).isEqualTo("pw: [REDACTED_TRUFFLEHOG_GENERIC]");
    }
}
