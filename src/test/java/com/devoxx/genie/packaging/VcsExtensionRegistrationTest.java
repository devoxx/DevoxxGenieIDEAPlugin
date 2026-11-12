package com.devoxx.genie.packaging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IntelliJ IDEA 2026.3 moved the VCS check-in API (CheckinHandlerFactory, CheckinProjectPanel) out of the core
 * platform into the bundled "Version Control" plugin. Its classes are only on DevoxxGenie's classpath through a
 * dependency on the com.intellij.modules.vcs module, so the check-in extension must be registered in an optional
 * config file that depends on it. The tests run on an older platform where the API is still in the core.
 */
class VcsExtensionRegistrationTest {

    private static final Path META_INF = Paths.get("").toAbsolutePath().resolve("src/main/resources/META-INF");

    @Test
    void checkinHandlerFactory_isRegisteredOnlyThroughTheOptionalVcsDependency() throws IOException {
        String pluginXml = Files.readString(META_INF.resolve("plugin.xml"));
        String vcsFeatures = Files.readString(META_INF.resolve("vcs-features.xml"));

        assertThat(pluginXml)
                .contains("<depends optional=\"true\" config-file=\"vcs-features.xml\">com.intellij.modules.vcs</depends>")
                .doesNotContain("<checkinHandlerFactory");
        assertThat(vcsFeatures).contains(
                "<checkinHandlerFactory implementation=\"com.devoxx.genie.service.automation.listeners.VcsCheckinHandlerFactory\"/>");
    }
}
