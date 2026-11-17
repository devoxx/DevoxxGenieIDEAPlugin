package com.devoxx.genie.service.chromadb;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Derives the ChromaDB collection name for a project.
 *
 * <p>ChromaDB requires a name of 3-63 characters that starts and ends with an alphanumeric
 * character and otherwise contains only alphanumerics, {@code _}, {@code -} or {@code .}
 * (no {@code ..}, not an IPv4 address).
 *
 * <p>A name that already meets those rules is kept exactly as earlier releases built it, so
 * existing indexes are still found. Any other name (non-ASCII, too short, too long, or with a
 * leading/trailing separator, see #1421) never had a working index; it becomes its usable ASCII
 * part plus a short hash of the original project name, so distinct projects don't share an index.
 */
public final class ChromaCollectionNames {

    private static final int MAX_LENGTH = 63;
    private static final int HASH_LENGTH = 8;
    private static final String FALLBACK_PREFIX = "project";
    private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]");

    private ChromaCollectionNames() {
    }

    public static @NotNull String forProject(@NotNull String projectName) {
        String legacy = projectName.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (VALID.matcher(legacy).matches()) {
            return legacy;
        }
        String base = trimDashes(legacy.replaceAll("-{2,}", "-"));
        int maxBase = MAX_LENGTH - HASH_LENGTH - 1;
        if (base.length() > maxBase) {
            base = trimDashes(base.substring(0, maxBase));
        }
        if (base.isEmpty()) {
            base = FALLBACK_PREFIX;
        }
        return base + "-" + hash(projectName);
    }

    private static String trimDashes(String s) {
        return s.replaceAll("^-+|-+$", "");
    }

    private static String hash(String projectName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(projectName.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
