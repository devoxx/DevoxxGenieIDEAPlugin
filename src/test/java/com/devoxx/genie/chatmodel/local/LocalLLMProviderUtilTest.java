package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.sun.net.httpserver.HttpServer;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalLLMProviderUtilTest {

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/fail", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Test
    void getBodyFromUrl_returnsBodyOn200() throws IOException {
        assertThat(LocalLLMProviderUtil.getBodyFromUrl(url("/ok"), new OkHttpClient())).isEqualTo("{\"data\":[]}");
    }

    @Test
    void getBodyFromUrl_throwsOnNon2xx() {
        assertThatThrownBy(() -> LocalLLMProviderUtil.getBodyFromUrl(url("/fail"), new OkHttpClient()))
                .isInstanceOf(UnsuccessfulRequestException.class);
    }
}
