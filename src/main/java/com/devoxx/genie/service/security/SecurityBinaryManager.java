package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.intellij.openapi.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Resolves scanner binaries: user override → bundled resource → download (trivy only).
 * Extracts to ~/.devoxxgenie/security-tools/{tool}/{platform}/.
 */
@Slf4j
public class SecurityBinaryManager {

    private static final String TOOLS_DIR = ".devoxxgenie/security-tools";
    private static final String CLASSPATH_SEP = "/";

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
        String bundledPath = CLASSPATH_SEP + String.join(CLASSPATH_SEP, "security", type.getId(), "dist", platform, type.getId());
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
        return findOnPath(type.getId());
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
