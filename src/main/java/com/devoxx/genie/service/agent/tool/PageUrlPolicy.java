package com.devoxx.genie.service.agent.tool;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

final class PageUrlPolicy {

    private static final int MAX_URL_LENGTH = 2000;

    private final boolean upgradesToHttps;

    private final boolean allowsLocalHosts;

    PageUrlPolicy(boolean upgradesToHttps, boolean allowsLocalHosts) {
        this.upgradesToHttps = upgradesToHttps;
        this.allowsLocalHosts = allowsLocalHosts;
    }

    static @NotNull PageUrlPolicy publicWeb() {
        return new PageUrlPolicy(true, false);
    }

    @NotNull URI fetchableUri(@NotNull String url) {
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("URL is longer than " + MAX_URL_LENGTH + " characters.");
        }
        if (!hasWebScheme(url)) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
        var uri = parse(url);
        var host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host: " + url);
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("URLs containing a user name or password are not fetched.");
        }
        if (!this.allowsLocalHosts) {
            rejectLocalHost(host);
        }
        return this.upgradesToHttps ? upgradedToHttps(uri) : uri;
    }

    static boolean isSameHost(@NotNull URI first, @NotNull URI second) {
        return hostWithoutWww(first).equals(hostWithoutWww(second));
    }

    private static boolean hasWebScheme(@NotNull String url) {
        return url.regionMatches(true, 0, "http://", 0, 7) || url.regionMatches(true, 0, "https://", 0, 8);
    }

    private static @NotNull URI parse(@NotNull String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    private static void rejectLocalHost(@NotNull String host) {
        if (!host.contains(".")) {
            throw new IllegalArgumentException("Host '" + host + "' is not a public domain name.");
        }
        for (InetAddress address : addressesOf(host)) {
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("Host '" + host + "' resolves to a local address ("
                        + address.getHostAddress() + "), which fetch_page does not access.");
            }
        }
    }

    private static InetAddress @NotNull [] addressesOf(@NotNull String host) {
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException unknown) {
            throw new IllegalArgumentException("Unknown host: " + host);
        }
    }

    private static @NotNull URI upgradedToHttps(@NotNull URI uri) {
        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getPort() != -1) {
            return uri;
        }
        var url = uri.toString();
        return URI.create("https" + url.substring(url.indexOf(':')));
    }

    private static @NotNull String hostWithoutWww(@NotNull URI uri) {
        var host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
