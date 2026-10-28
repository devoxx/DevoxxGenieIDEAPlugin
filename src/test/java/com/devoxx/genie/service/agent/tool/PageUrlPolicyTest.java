package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageUrlPolicyTest {

    private final PageUrlPolicy publicWeb = PageUrlPolicy.publicWeb();

    @Test
    void urlLongerThanTwoThousandCharactersIsRejected() {
        var url = "https://8.8.8.8/" + "a".repeat(2000);

        assertThatThrownBy(() -> this.publicWeb.fetchableUri(url)).hasMessageContaining("longer than 2000");
    }

    @Test
    void nonWebSchemeIsRejected() {
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("ftp://example.com/file"))
                .hasMessage("URL must start with http:// or https://");
    }

    @Test
    void urlWithCredentialsIsRejected() {
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("https://user:secret@example.com/"))
                .hasMessageContaining("user name or password");
    }

    @Test
    void hostWithoutADotIsRejected() {
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("http://localhost:8080/admin"))
                .hasMessageContaining("not a public domain name");
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("http://intranet/wiki"))
                .hasMessageContaining("not a public domain name");
    }

    @Test
    void loopbackLinkLocalAndUnspecifiedAddressesAreRejected() {
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("http://127.0.0.1/"))
                .hasMessageContaining("local address");
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("http://169.254.169.254/latest/meta-data/"))
                .hasMessageContaining("local address");
        assertThatThrownBy(() -> this.publicWeb.fetchableUri("http://0.0.0.0/"))
                .hasMessageContaining("local address");
    }

    @Test
    void plainHttpIsUpgradedToHttpsWhenNoPortIsGiven() {
        assertThat(this.publicWeb.fetchableUri("http://8.8.8.8/docs?q=1"))
                .isEqualTo(URI.create("https://8.8.8.8/docs?q=1"));
        assertThat(this.publicWeb.fetchableUri("http://8.8.8.8:8080/docs"))
                .isEqualTo(URI.create("http://8.8.8.8:8080/docs"));
    }

    @Test
    void policyForTestsAllowsLocalHostsAndKeepsHttp() {
        var permissive = new PageUrlPolicy(false, true);

        assertThat(permissive.fetchableUri("http://localhost:8080/page"))
                .isEqualTo(URI.create("http://localhost:8080/page"));
    }

    @Test
    void sameHostIgnoresTheWwwPrefixAndCase() {
        assertThat(PageUrlPolicy.isSameHost(URI.create("https://www.Example.com/a"), URI.create("https://example.com/b")))
                .isTrue();
        assertThat(PageUrlPolicy.isSameHost(URI.create("https://example.com/a"), URI.create("https://other.com/a")))
                .isFalse();
    }
}
