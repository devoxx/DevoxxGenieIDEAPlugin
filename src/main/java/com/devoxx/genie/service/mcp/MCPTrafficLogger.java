package com.devoxx.genie.service.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.function.Consumer;

/**
 * A delegating SLF4J Logger that wraps the real "MCP" logger and also
 * forwards traffic-related log calls to a {@link Consumer}.
 * <p>
 * {@link dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport}
 * is a pre-compiled class from the langchain4j-mcp jar. It accepts a custom
 * {@code Logger} via its builder ({@code .logger(Logger)}). Internally it logs
 * outgoing requests as {@code trafficLog.info("Request: {}", body)} and
 * incoming responses as {@code trafficLog.info("< {}", data)}.
 * <p>
 * This wrapper delegates every call to the real logger unchanged, and
 * additionally calls the {@code trafficConsumer} for {@code info(String, Object)}
 * and {@code debug(String, Object)} invocations so that traffic lines reach the
 * MCP Log Panel.
 */
public class MCPTrafficLogger implements Logger {

    private final Logger delegate;
    private final Consumer<String> trafficConsumer;

    public MCPTrafficLogger(Consumer<String> trafficConsumer) {
        this.delegate = LoggerFactory.getLogger("MCP");
        this.trafficConsumer = trafficConsumer;
    }

    // ── traffic-intercepting overrides ──────────────────────────────────

    @Override
    public void info(String format, Object arg) {
        delegate.info(format, arg);
        if (trafficConsumer != null) {
            forwardToConsumer(format, arg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
        if (trafficConsumer != null) {
            forwardToConsumer(format, arg);
        }
    }

    /**
     * Extract direction prefix from the format pattern and forward the
     * formatted message to the traffic consumer.
     * <p>
     * Expected patterns from StreamableHttpMcpTransport:
     * <ul>
     *   <li>{@code "Request: {}"} → outgoing, prefix with {@code "> "}</li>
     *   <li>{@code "> {}"} → outgoing, already prefixed</li>
     *   <li>{@code "< {}"} → incoming, already prefixed</li>
     * </ul>
     */
    private void forwardToConsumer(String format, Object arg) {
        String message = String.valueOf(arg);
        if (format.startsWith("> ") || format.startsWith("Request")) {
            trafficConsumer.accept("> " + message);
        } else if (format.startsWith("< ")) {
            trafficConsumer.accept("< " + message);
        }
    }

    // ── pure delegation ─────────────────────────────────────────────────

    @Override public String getName() { return delegate.getName(); }

    @Override public boolean isTraceEnabled() { return delegate.isTraceEnabled(); }
    @Override public boolean isTraceEnabled(Marker marker) { return delegate.isTraceEnabled(marker); }
    @Override public void trace(String msg) { delegate.trace(msg); }
    @Override public void trace(String format, Object arg) { delegate.trace(format, arg); }
    @Override public void trace(String format, Object arg1, Object arg2) { delegate.trace(format, arg1, arg2); }
    @Override public void trace(String format, Object... arguments) { delegate.trace(format, arguments); }
    @Override public void trace(String msg, Throwable t) { delegate.trace(msg, t); }
    @Override public void trace(Marker marker, String msg) { delegate.trace(marker, msg); }
    @Override public void trace(Marker marker, String format, Object arg) { delegate.trace(marker, format, arg); }
    @Override public void trace(Marker marker, String format, Object arg1, Object arg2) { delegate.trace(marker, format, arg1, arg2); }
    @Override public void trace(Marker marker, String format, Object... arguments) { delegate.trace(marker, format, arguments); }
    @Override public void trace(Marker marker, String msg, Throwable t) { delegate.trace(marker, msg, t); }

    @Override public boolean isDebugEnabled() { return delegate.isDebugEnabled(); }
    @Override public boolean isDebugEnabled(Marker marker) { return delegate.isDebugEnabled(marker); }
    @Override public void debug(String msg) { delegate.debug(msg); }
    @Override public void debug(String format, Object arg1, Object arg2) { delegate.debug(format, arg1, arg2); }
    @Override public void debug(String format, Object... arguments) { delegate.debug(format, arguments); }
    @Override public void debug(String msg, Throwable t) { delegate.debug(msg, t); }
    @Override public void debug(Marker marker, String msg) { delegate.debug(marker, msg); }
    @Override public void debug(Marker marker, String format, Object arg) { delegate.debug(marker, format, arg); }
    @Override public void debug(Marker marker, String format, Object arg1, Object arg2) { delegate.debug(marker, format, arg1, arg2); }
    @Override public void debug(Marker marker, String format, Object... arguments) { delegate.debug(marker, format, arguments); }
    @Override public void debug(Marker marker, String msg, Throwable t) { delegate.debug(marker, msg, t); }

    @Override public boolean isInfoEnabled() { return delegate.isInfoEnabled(); }
    @Override public boolean isInfoEnabled(Marker marker) { return delegate.isInfoEnabled(marker); }
    @Override public void info(String msg) { delegate.info(msg); }
    @Override public void info(String format, Object arg1, Object arg2) { delegate.info(format, arg1, arg2); }
    @Override public void info(String format, Object... arguments) { delegate.info(format, arguments); }
    @Override public void info(String msg, Throwable t) { delegate.info(msg, t); }
    @Override public void info(Marker marker, String msg) { delegate.info(marker, msg); }
    @Override public void info(Marker marker, String format, Object arg) { delegate.info(marker, format, arg); }
    @Override public void info(Marker marker, String format, Object arg1, Object arg2) { delegate.info(marker, format, arg1, arg2); }
    @Override public void info(Marker marker, String format, Object... arguments) { delegate.info(marker, format, arguments); }
    @Override public void info(Marker marker, String msg, Throwable t) { delegate.info(marker, msg, t); }

    @Override public boolean isWarnEnabled() { return delegate.isWarnEnabled(); }
    @Override public boolean isWarnEnabled(Marker marker) { return delegate.isWarnEnabled(marker); }
    @Override public void warn(String msg) { delegate.warn(msg); }
    @Override public void warn(String format, Object arg) { delegate.warn(format, arg); }
    @Override public void warn(String format, Object arg1, Object arg2) { delegate.warn(format, arg1, arg2); }
    @Override public void warn(String format, Object... arguments) { delegate.warn(format, arguments); }
    @Override public void warn(String msg, Throwable t) { delegate.warn(msg, t); }
    @Override public void warn(Marker marker, String msg) { delegate.warn(marker, msg); }
    @Override public void warn(Marker marker, String format, Object arg) { delegate.warn(marker, format, arg); }
    @Override public void warn(Marker marker, String format, Object arg1, Object arg2) { delegate.warn(marker, format, arg1, arg2); }
    @Override public void warn(Marker marker, String format, Object... arguments) { delegate.warn(marker, format, arguments); }
    @Override public void warn(Marker marker, String msg, Throwable t) { delegate.warn(marker, msg, t); }

    @Override public boolean isErrorEnabled() { return delegate.isErrorEnabled(); }
    @Override public boolean isErrorEnabled(Marker marker) { return delegate.isErrorEnabled(marker); }
    @Override public void error(String msg) { delegate.error(msg); }
    @Override public void error(String format, Object arg) { delegate.error(format, arg); }
    @Override public void error(String format, Object arg1, Object arg2) { delegate.error(format, arg1, arg2); }
    @Override public void error(String format, Object... arguments) { delegate.error(format, arguments); }
    @Override public void error(String msg, Throwable t) { delegate.error(msg, t); }
    @Override public void error(Marker marker, String msg) { delegate.error(marker, msg); }
    @Override public void error(Marker marker, String format, Object arg) { delegate.error(marker, format, arg); }
    @Override public void error(Marker marker, String format, Object arg1, Object arg2) { delegate.error(marker, format, arg1, arg2); }
    @Override public void error(Marker marker, String format, Object... arguments) { delegate.error(marker, format, arguments); }
    @Override public void error(Marker marker, String msg, Throwable t) { delegate.error(marker, msg, t); }
}
