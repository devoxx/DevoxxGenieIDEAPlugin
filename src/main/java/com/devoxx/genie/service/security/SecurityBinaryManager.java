package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.intellij.openapi.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

/**
 * Resolves scanner binaries: user override → bundled resource → download (trivy only).
 * Extracts to ~/.devoxxgenie/security-tools/{tool}/{platform}/.
 */
@Slf4j
public class SecurityBinaryManager {

    private static final String TOOLS_DIR = ".devoxxgenie/security-tools";

    private SecurityBinaryManager() {}

    /**
     * Resolve the binary path for a scanner.
     * Priority: user override → bundled → download (trivy only).
     */
    @Nullable
    public static String resolveBinary(@NotNull ScannerType type, @NotNull String userOverride)
            throws SecurityScanException {
        // 1. User override
        if (!userOverride.isBlank()) {
            File f = new File(userOverride);
            if (f.isFile() && f.canExecute()) {
                return f.getAbsolutePath();
            }
            throw new SecurityScanException(type.getDisplayName() +
                    ": custom binary not found or not executable: " + userOverride);
        }

        // 2. Bundled resource
        String platform = detectPlatform();
        String bundledPath = "/security/" + type.getId() + "/dist/" + platform + "/" + type.getId();
        if (SystemInfo.isWindows) {
            bundledPath += ".exe";
        }

        Path extractDir = getToolDir(type, platform);
        String binaryName = type.getId() + (SystemInfo.isWindows ? ".exe" : "");
        Path extractedBinary = extractDir.resolve(binaryName);

        if (Files.isExecutable(extractedBinary)) {
            return extractedBinary.toString();
        }

        try (InputStream in = SecurityBinaryManager.class.getResourceAsStream(bundledPath)) {
            if (in != null) {
                Files.createDirectories(extractDir);
                Files.copy(in, extractedBinary, StandardCopyOption.REPLACE_EXISTING);
                if (!extractedBinary.toFile().setExecutable(true)) {
                    log.warn("Failed to set executable bit on {}", extractedBinary);
                }
                return extractedBinary.toString();
            }
        } catch (IOException e) {
            log.warn("Failed to extract bundled binary for {}", type.getId(), e);
        }

        // 3. Try system PATH
        String systemBinary = findOnPath(type.getId());
        if (systemBinary != null) {
            return systemBinary;
        }

        return null;
    }

    @Nullable
    private static String findOnPath(@NotNull String binaryName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) return null;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            File f = new File(dir, binaryName);
            if (f.isFile() && f.canExecute()) {
                return f.getAbsolutePath();
            }
            if (SystemInfo.isWindows) {
                File fExe = new File(dir, binaryName + ".exe");
                if (fExe.isFile() && fExe.canExecute()) {
                    return fExe.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static String downloadTrivy(@NotNull Path extractDir, @NotNull Path targetPath)
            throws SecurityScanException {
        String platform = detectPlatform();
        String os;
        String arch;
        if (platform.startsWith("mac")) {
            os = "macOS";
            arch = platform.contains("arm64") ? "ARM64" : "64bit";
        } else if (platform.startsWith("linux")) {
            os = "Linux";
            arch = platform.contains("arm64") ? "ARM64" : "64bit";
        } else {
            os = "Windows";
            arch = "64bit";
        }

        String fileName = "trivy_" + getTrivyVersion() + "_" + os + "-" + arch + ".tar.gz";
        String url = "https://github.com/aquasecurity/trivy/releases/download/v" +
                getTrivyVersion() + "/" + fileName;

        log.info("Downloading trivy from {}", url);

        try {
            Files.createDirectories(extractDir);
            Path tempFile = Files.createTempFile("trivy-download", ".tar.gz");

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Extract trivy binary from tar.gz
            extractTarGz(tempFile, extractDir, "trivy");
            Files.deleteIfExists(tempFile);

            if (Files.isRegularFile(targetPath)) {
                if (!targetPath.toFile().setExecutable(true)) {
                    log.warn("Failed to set executable bit on {}", targetPath);
                }
                return targetPath.toString();
            }

            throw new SecurityScanException("Failed to extract trivy binary from download");
        } catch (SecurityScanException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityScanException("Failed to download trivy: " + e.getMessage(), e);
        }
    }

    /**
     * Extract a named binary from a tar.gz archive using plain Java tar parsing.
     * TAR format: 512-byte header blocks followed by file data rounded to 512 bytes.
     */
    private static void extractTarGz(@NotNull Path tarGzFile, @NotNull Path targetDir,
                                       @NotNull String binaryName) throws IOException {
        try (InputStream fis = Files.newInputStream(tarGzFile);
             GZIPInputStream gzis = new GZIPInputStream(fis)) {

            byte[] header = new byte[512];
            while (true) {
                int bytesRead = readFully(gzis, header);
                if (bytesRead < 512 || isEndOfArchive(header)) break;

                String name = parseTarEntryName(header);
                long size = parseTarEntrySize(header);

                if (name.endsWith("/" + binaryName) || name.equals(binaryName)) {
                    Path outPath = targetDir.resolve(binaryName);
                    try (OutputStream out = Files.newOutputStream(outPath)) {
                        long remaining = size;
                        byte[] buf = new byte[8192];
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buf.length, remaining);
                            int read = gzis.read(buf, 0, toRead);
                            if (read <= 0) break;
                            out.write(buf, 0, read);
                            remaining -= read;
                        }
                    }
                    // Skip padding to 512-byte boundary
                    long pad = (512 - (size % 512)) % 512;
                    gzis.skipNBytes(pad);
                    return;
                }

                // Skip this entry's data + padding
                long dataBlocks = (size + 511) / 512 * 512;
                gzis.skipNBytes(dataBlocks);
            }
        }
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int read = in.read(buf, total, buf.length - total);
            if (read < 0) break;
            total += read;
        }
        return total;
    }

    private static boolean isEndOfArchive(byte[] header) {
        for (byte b : header) {
            if (b != 0) return false;
        }
        return true;
    }

    private static String parseTarEntryName(byte[] header) {
        // Name is at offset 0, max 100 bytes; prefix at offset 345, max 155 bytes (POSIX/UStar)
        String prefix = extractString(header, 345, 155);
        String name = extractString(header, 0, 100);
        return prefix.isEmpty() ? name : prefix + "/" + name;
    }

    private static long parseTarEntrySize(byte[] header) {
        // Size is at offset 124, 12 bytes, octal encoded
        return parseOctal(header, 124, 12);
    }

    private static String extractString(byte[] data, int offset, int length) {
        int end = offset;
        while (end < offset + length && end < data.length && data[end] != 0) end++;
        return new String(data, offset, end - offset, java.nio.charset.StandardCharsets.US_ASCII).trim();
    }

    private static long parseOctal(byte[] data, int offset, int length) {
        long result = 0;
        for (int i = offset; i < offset + length; i++) {
            byte b = data[i];
            if (b == 0 || b == ' ') continue;
            if (b < '0' || b > '7') break;
            result = result * 8 + (b - '0');
        }
        return result;
    }

    private static String getTrivyVersion() {
        return "0.58.2";
    }

    @NotNull
    static String detectPlatform() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean isArm = arch.contains("aarch64") || arch.contains("arm64");

        if (SystemInfo.isMac) {
            return isArm ? "mac_arm64" : "mac_x64";
        } else if (SystemInfo.isLinux) {
            return isArm ? "linux_arm64" : "linux_x64";
        } else {
            return "win_x64";
        }
    }

    @NotNull
    private static Path getToolDir(@NotNull ScannerType type, @NotNull String platform) {
        return Path.of(System.getProperty("user.home"), TOOLS_DIR, type.getId(), platform);
    }
}
