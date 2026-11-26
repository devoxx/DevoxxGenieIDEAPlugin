package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.service.credentials.CredentialService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Finds the Hugging Face token for trace uploads (task-280), in order:
 * <ol>
 *   <li>the token saved in the IDE's credential store ({@link CredentialKey#HUGGING_FACE_TOKEN});</li>
 *   <li>the {@code HF_TOKEN} or {@code HUGGING_FACE_HUB_TOKEN} environment variable;</li>
 *   <li>the token file written by {@code hf auth login}: {@code $HF_TOKEN_PATH}, {@code $HF_HOME/token},
 *       or {@code ~/.cache/huggingface/token} (honouring {@code $XDG_CACHE_HOME}).</li>
 * </ol>
 * Mirrors {@code readCachedToken()} in copilot-traces-hf's {@code hub.ts}.
 */
public final class HfTokenResolver {

    /** Where the token came from, for the setup dialog ("using the token from hf auth login"). */
    public enum Source { IDE, ENVIRONMENT, HF_CLI }

    public record ResolvedToken(@NotNull String token, @NotNull Source source) {}

    private final Supplier<String> stored;
    private final Function<String, String> env;
    private final String userHome;

    HfTokenResolver(@NotNull Supplier<String> stored, @NotNull Function<String, String> env, @NotNull String userHome) {
        this.stored = stored;
        this.env = env;
        this.userHome = userHome;
    }

    public static @NotNull HfTokenResolver system() {
        return new HfTokenResolver(
                () -> CredentialService.getInstance().getCredential(CredentialKey.HUGGING_FACE_TOKEN),
                System::getenv,
                System.getProperty("user.home"));
    }

    public @Nullable ResolvedToken resolve() {
        String ide = trimToNull(safe(stored));
        if (ide != null) {
            return new ResolvedToken(ide, Source.IDE);
        }
        for (String var : List.of("HF_TOKEN", "HUGGING_FACE_HUB_TOKEN")) {
            String value = trimToNull(env.apply(var));
            if (value != null) {
                return new ResolvedToken(value, Source.ENVIRONMENT);
            }
        }
        for (Path file : tokenFiles()) {
            try {
                if (Files.isRegularFile(file)) {
                    String value = trimToNull(Files.readString(file, StandardCharsets.UTF_8));
                    if (value != null) {
                        return new ResolvedToken(value, Source.HF_CLI);
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // unreadable token file: try the next location
            }
        }
        return null;
    }

    List<Path> tokenFiles() {
        List<Path> files = new ArrayList<>();
        String tokenPath = trimToNull(env.apply("HF_TOKEN_PATH"));
        if (tokenPath != null) {
            files.add(Path.of(tokenPath));
        }
        String hfHome = trimToNull(env.apply("HF_HOME"));
        if (hfHome != null) {
            files.add(Path.of(hfHome, "token"));
        }
        String cache = trimToNull(env.apply("XDG_CACHE_HOME"));
        files.add(Path.of(cache != null ? cache : Path.of(userHome, ".cache").toString(), "huggingface", "token"));
        return files;
    }

    private static @Nullable String safe(Supplier<String> s) {
        try {
            return s.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String trimToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
