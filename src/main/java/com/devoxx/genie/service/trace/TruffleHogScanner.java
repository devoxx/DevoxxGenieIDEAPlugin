package com.devoxx.genie.service.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Optional secret scan of a recorded turn with TruffleHog (https://github.com/trufflesecurity/trufflehog),
 * ported from copilot-traces-hf's {@code trufflehog.ts} (task-279).
 * <p>
 * Every free-text field of the turn is written to its own file in a fresh temp directory with
 * owner-only permissions, the directory is scanned with {@code trufflehog filesystem --json}, and
 * each finding is mapped back to the exact field it came from. One file per field means no JSON
 * escaping stands between TruffleHog's {@code Raw} value and the text it has to be removed from.
 * The temp directory is always deleted.
 * <p>
 * Raw secrets are never logged: only {@link Finding#masked()} is safe to show.
 */
@Slf4j
public class TruffleHogScanner {

    /** TruffleHog's exit code with {@code --fail} and findings; treated as success. */
    static final int EXIT_FINDINGS = 183;
    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration VERSION_TIMEOUT = Duration.ofSeconds(15);
    private static final ObjectMapper JSON = new ObjectMapper();

    public enum FindingStatus { VERIFIED, UNVERIFIED, UNKNOWN }

    /** One secret TruffleHog found. {@code raw} is the matched secret: never log it. */
    public record Finding(@NotNull String detector, @NotNull FindingStatus status, @Nullable String raw,
                          @NotNull String masked) {

        @Override
        public String toString() {
            return "Finding[" + detector + ", " + status + ", " + masked + "]";
        }
    }

    /** The outcome of scanning one turn. */
    public sealed interface ScanResult {
        /** The binary could not be started. */
        record NotFound(@NotNull String binary) implements ScanResult {}

        /** The scan ran but failed (non-zero exit, timeout, I/O error). */
        record Failed(@NotNull String reason) implements ScanResult {}

        /** The scan succeeded; {@code events} have findings replaced (or are unchanged when none). */
        record Completed(@NotNull List<TraceEvent> events, @NotNull List<Finding> findings,
                         boolean allRemoved) implements ScanResult {}
    }

    private final Duration timeout;

    public TruffleHogScanner() {
        this(DEFAULT_TIMEOUT);
    }

    TruffleHogScanner(@NotNull Duration timeout) {
        this.timeout = timeout;
    }

    /** Whether {@code binary} can be started ({@code trufflehog --version} succeeds). */
    public boolean isAvailable(@NotNull String binary) {
        Process process = null;
        try {
            process = new ProcessBuilder(binary, "--version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(VERSION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** Scans a turn's (already regex-redacted) events and replaces every finding it can. */
    public @NotNull ScanResult scan(@NotNull List<TraceEvent> events, @NotNull String binary, boolean verify) {
        Path dir = null;
        try {
            dir = createPrivateTempDir();
            Map<String, FieldRef> files = writeFields(dir, events);
            Path stdout = dir.resolveSibling(dir.getFileName() + ".out");
            Path stderr = dir.resolveSibling(dir.getFileName() + ".err");
            try {
                List<String> command = new ArrayList<>(List.of(binary, "filesystem", dir.toString(), "--json",
                        "--no-update", "--no-color", "--results=verified,unknown,unverified"));
                if (!verify) {
                    command.add("--no-verification");
                }
                Process process;
                try {
                    process = new ProcessBuilder(command)
                            .redirectOutput(stdout.toFile())
                            .redirectError(stderr.toFile())
                            .start();
                } catch (IOException e) {
                    return new ScanResult.NotFound(binary);
                }
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    return new ScanResult.Failed("trufflehog timed out after " + timeout.toSeconds() + "s");
                }
                int code = process.exitValue();
                if (code != 0 && code != EXIT_FINDINGS) {
                    String err = Files.exists(stderr) ? Files.readString(stderr, StandardCharsets.UTF_8).trim() : "";
                    return new ScanResult.Failed("trufflehog exited with code " + code
                            + (err.isEmpty() ? "" : ": " + err.substring(0, Math.min(300, err.length()))));
                }
                Map<FieldRef, List<Finding>> byField = parseOutput(
                        Files.readAllLines(stdout, StandardCharsets.UTF_8), dir, files);
                return applyFindings(events, byField);
            } finally {
                Files.deleteIfExists(stdout);
                Files.deleteIfExists(stderr);
            }
        } catch (IOException e) {
            return new ScanResult.Failed("trufflehog scan I/O error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ScanResult.Failed("trufflehog scan interrupted");
        } finally {
            if (dir != null) {
                deleteRecursively(dir);
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Fields ↔ files
    // ---------------------------------------------------------------------------------------

    /** Which text of which event a scanned file holds. {@code callIndex} is set for tool arguments. */
    record FieldRef(int eventIndex, @NotNull Field field, int callIndex) {}

    enum Field { CONTENT, REASONING, TOOL_ARGUMENTS }

    static @NotNull Map<String, FieldRef> writeFields(@NotNull Path dir, @NotNull List<TraceEvent> events) throws IOException {
        Map<String, FieldRef> files = new HashMap<>();
        for (int i = 0; i < events.size(); i++) {
            TraceEvent e = events.get(i);
            writeField(dir, files, e.content(), new FieldRef(i, Field.CONTENT, -1));
            writeField(dir, files, e.reasoning(), new FieldRef(i, Field.REASONING, -1));
            for (int c = 0; c < e.toolCalls().size(); c++) {
                writeField(dir, files, e.toolCalls().get(c).arguments(), new FieldRef(i, Field.TOOL_ARGUMENTS, c));
            }
        }
        return files;
    }

    private static void writeField(Path dir, Map<String, FieldRef> files, @Nullable String text, FieldRef ref) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        String name = "e" + ref.eventIndex() + "-" + ref.field().name().toLowerCase() + (ref.callIndex() >= 0 ? "-" + ref.callIndex() : "") + ".txt";
        Path file = dir.resolve(name);
        Files.writeString(file, text, StandardCharsets.UTF_8);
        restrictToOwner(file, false);
        files.put(file.toAbsolutePath().normalize().toString(), ref);
    }

    // ---------------------------------------------------------------------------------------
    // Output parsing
    // ---------------------------------------------------------------------------------------

    /** One parsed finding line: the scanned file it was found in, and the finding. */
    record ParsedLine(@NotNull String file, @NotNull Finding finding) {}

    /**
     * Parses one line of {@code trufflehog --json} output. Returns {@code null} for log lines and
     * anything that isn't a finding with a filesystem source.
     */
    static @Nullable ParsedLine parseLine(@Nullable String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        JsonNode node;
        try {
            node = JSON.readTree(trimmed);
        } catch (IOException e) {
            return null;
        }
        JsonNode detector = node.get("DetectorName");
        if (detector == null || !detector.isTextual()) {
            return null;
        }
        JsonNode file = node.at("/SourceMetadata/Data/Filesystem/file");
        if (!file.isTextual()) {
            return null;
        }
        String raw = node.hasNonNull("Raw") && !node.get("Raw").asText().isEmpty() ? node.get("Raw").asText() : null;
        FindingStatus status;
        if (node.path("Verified").asBoolean(false)) {
            status = FindingStatus.VERIFIED;
        } else if (hasText(node, "VerificationError") || hasText(node.path("ExtraData"), "verification_error")
                || hasText(node.path("ExtraData"), "verificationError")) {
            status = FindingStatus.UNKNOWN;
        } else {
            status = FindingStatus.UNVERIFIED;
        }
        return new ParsedLine(file.asText(), new Finding(detector.asText(), status, raw, raw == null ? "[REDACTED]" : mask(raw)));
    }

    private static boolean hasText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() && !v.asText().isEmpty();
    }

    static @NotNull Map<FieldRef, List<Finding>> parseOutput(@NotNull List<String> lines, @NotNull Path dir,
                                                             @NotNull Map<String, FieldRef> files) {
        Map<FieldRef, List<Finding>> byField = new HashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String line : lines) {
            ParsedLine parsed = parseLine(line);
            if (parsed == null) {
                continue;
            }
            Path path = Path.of(parsed.file());
            if (!path.isAbsolute()) {
                path = dir.resolve(path);
            }
            FieldRef ref = files.get(path.toAbsolutePath().normalize().toString());
            if (ref == null) {
                continue;
            }
            String key = ref + "|" + parsed.finding().detector() + "|" + parsed.finding().raw();
            if (seen.add(key)) {
                byField.computeIfAbsent(ref, r -> new ArrayList<>()).add(parsed.finding());
            }
        }
        return byField;
    }

    /** Masked form of a secret, safe for logs and the UI. */
    static @NotNull String mask(@NotNull String secret) {
        if (secret.length() <= 8) {
            return "*".repeat(secret.length());
        }
        return secret.substring(0, 4) + "…" + secret.substring(secret.length() - 2) + " (" + secret.length() + " chars)";
    }

    // ---------------------------------------------------------------------------------------
    // Replacing findings
    // ---------------------------------------------------------------------------------------

    static @NotNull ScanResult.Completed applyFindings(@NotNull List<TraceEvent> events,
                                                      @NotNull Map<FieldRef, List<Finding>> byField) {
        List<Finding> all = byField.values().stream().flatMap(List::stream).toList();
        if (all.isEmpty()) {
            return new ScanResult.Completed(events, List.of(), true);
        }
        List<TraceEvent> result = new ArrayList<>(events);
        boolean allRemoved = true;
        for (Map.Entry<FieldRef, List<Finding>> entry : byField.entrySet()) {
            FieldRef ref = entry.getKey();
            TraceEvent event = result.get(ref.eventIndex());
            String text = read(event, ref);
            if (text == null) {
                allRemoved = false;
                continue;
            }
            for (Finding f : entry.getValue()) {
                if (f.raw() == null || !text.contains(f.raw())) {
                    allRemoved = false;
                    continue;
                }
                text = text.replace(f.raw(), placeholder(f.detector()));
            }
            result.set(ref.eventIndex(), write(event, ref, text));
        }
        return new ScanResult.Completed(List.copyOf(result), all, allRemoved);
    }

    static @NotNull String placeholder(@NotNull String detector) {
        String label = detector.toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        return "[REDACTED_TRUFFLEHOG_" + label + "]";
    }

    private static @Nullable String read(TraceEvent e, FieldRef ref) {
        return switch (ref.field()) {
            case CONTENT -> e.content();
            case REASONING -> e.reasoning();
            case TOOL_ARGUMENTS -> ref.callIndex() < e.toolCalls().size() ? e.toolCalls().get(ref.callIndex()).arguments() : null;
        };
    }

    private static TraceEvent write(TraceEvent e, FieldRef ref, String text) {
        return switch (ref.field()) {
            case CONTENT -> new TraceEvent(e.role(), text, e.reasoning(), e.toolCalls(), e.toolCallId(), e.toolName(),
                    e.error(), e.model(), e.subAgentId(), e.timestampMs());
            case REASONING -> new TraceEvent(e.role(), e.content(), text, e.toolCalls(), e.toolCallId(), e.toolName(),
                    e.error(), e.model(), e.subAgentId(), e.timestampMs());
            case TOOL_ARGUMENTS -> {
                List<TraceEvent.ToolCall> calls = new ArrayList<>(e.toolCalls());
                TraceEvent.ToolCall call = calls.get(ref.callIndex());
                calls.set(ref.callIndex(), new TraceEvent.ToolCall(call.id(), call.name(), text));
                yield new TraceEvent(e.role(), e.content(), e.reasoning(), calls, e.toolCallId(), e.toolName(),
                        e.error(), e.model(), e.subAgentId(), e.timestampMs());
            }
        };
    }

    // ---------------------------------------------------------------------------------------
    // Temp files
    // ---------------------------------------------------------------------------------------

    private static Path createPrivateTempDir() throws IOException {
        Path dir = Files.createTempDirectory("devoxxgenie-trace-scan-");
        restrictToOwner(dir, true);
        return dir;
    }

    private static void restrictToOwner(Path path, boolean directory) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            // Non-POSIX file system (Windows): the temp dir is already under the user's profile.
            java.io.File f = path.toFile();
            f.setReadable(false, false);
            f.setReadable(true, true);
            f.setWritable(false, false);
            f.setWritable(true, true);
        }
    }

    private static void deleteRecursively(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Could not delete trace scan temp directory {}: {}", dir, e.getMessage());
        }
    }
}
