package dev.langchain4j.mcp.client.transport.http;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class McpRequestLoggingInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(McpRequestLoggingInterceptor.class);
    private static final Logger trafficLog = LoggerFactory.getLogger("MCP");
    private final Consumer<String> trafficConsumer;

    McpRequestLoggingInterceptor(Consumer<String> trafficConsumer) {
        this.trafficConsumer = trafficConsumer;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        this.log(request);
        return chain.proceed(request);
    }

    private void log(Request request) {
        try {
            trafficLog.debug(
                    "Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}",
                    request.method(),
                    request.url(),
                    getHeaders(request.headers()),
                    getBody(request));
            if (trafficConsumer != null) {
                trafficConsumer.accept("> " + getBody(request));
            }
        } catch (Exception e) {
            log.warn("Error while logging request: {}", e.getMessage());
        }
    }

    private static String getBody(Request request) {
        try {
            Buffer buffer = new Buffer();
            if (request.body() == null) {
                return "";
            }
            request.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (Exception e) {
            log.warn("Exception while getting body", e);
            return "Exception while getting body: " + e.getMessage();
        }
    }

    static String getHeaders(Headers headers) {
        return StreamSupport.stream(headers.spliterator(), false)
                .map(header -> {
                    String headerKey = header.component1();
                    String headerValue = header.component2();
                    return String.format("[%s: %s]", headerKey, headerValue);
                })
                .collect(Collectors.joining(", "));
    }
}
