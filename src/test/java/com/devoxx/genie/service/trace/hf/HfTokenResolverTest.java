package com.devoxx.genie.service.trace.hf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HfTokenResolverTest {

    @TempDir
    Path home;

    private final Map<String, String> env = new HashMap<>();

    private HfTokenResolver resolver(String stored) {
        return new HfTokenResolver(() -> stored, env::get, home.toString());
    }

    @Test
    void storedToken_winsOverEnvironmentAndFiles() {
        env.put("HF_TOKEN", "env-token");
        HfTokenResolver.ResolvedToken t = resolver("ide-token").resolve();
        assertThat(t).isNotNull();
        assertThat(t.token()).isEqualTo("ide-token");
        assertThat(t.source()).isEqualTo(HfTokenResolver.Source.IDE);
    }

    @Test
    void environment_isUsedWhenNothingStored() {
        env.put("HUGGING_FACE_HUB_TOKEN", " legacy-env \n");
        HfTokenResolver.ResolvedToken t = resolver("").resolve();
        assertThat(t.token()).isEqualTo("legacy-env");
        assertThat(t.source()).isEqualTo(HfTokenResolver.Source.ENVIRONMENT);
    }

    @Test
    void hfCliTokenFile_inDefaultCache_isUsedLast() throws IOException {
        Path file = home.resolve(".cache/huggingface/token");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "cli-token\n");
        HfTokenResolver.ResolvedToken t = resolver(null).resolve();
        assertThat(t.token()).isEqualTo("cli-token");
        assertThat(t.source()).isEqualTo(HfTokenResolver.Source.HF_CLI);
    }

    @Test
    void hfHomeAndTokenPath_areHonoured() throws IOException {
        Path hfHome = home.resolve("hf");
        Files.createDirectories(hfHome);
        Files.writeString(hfHome.resolve("token"), "from-hf-home");
        env.put("HF_HOME", hfHome.toString());
        assertThat(resolver(null).resolve().token()).isEqualTo("from-hf-home");

        Path explicit = home.resolve("explicit-token");
        Files.writeString(explicit, "from-token-path");
        env.put("HF_TOKEN_PATH", explicit.toString());
        assertThat(resolver(null).resolve().token()).isEqualTo("from-token-path");
    }

    @Test
    void nothingAnywhere_returnsNull() {
        assertThat(resolver(null).resolve()).isNull();
    }

    @Test
    void failingCredentialStore_fallsThrough() {
        env.put("HF_TOKEN", "env-token");
        HfTokenResolver r = new HfTokenResolver(() -> { throw new IllegalStateException("keychain"); }, env::get, home.toString());
        assertThat(r.resolve().token()).isEqualTo("env-token");
    }
}
