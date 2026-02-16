package com.devoxx.genie.service.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MCPTrafficLoggerTest {

    private List<String> capturedMessages;
    private MCPTrafficLogger logger;

    @BeforeEach
    void setUp() {
        capturedMessages = new ArrayList<>();
        logger = new MCPTrafficLogger(capturedMessages::add);
    }

    @Test
    void getName_returnsMCP() {
        assertThat(logger.getName()).isEqualTo("MCP");
    }

    @Test
    void infoWithRequestFormat_forwardsWithOutgoingPrefix() {
        logger.info("Request: {}", "{\"method\":\"tools/list\"}");

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0)).startsWith("> ");
        assertThat(capturedMessages.get(0)).contains("{\"method\":\"tools/list\"}");
    }

    @Test
    void infoWithOutgoingPrefix_forwardsWithOutgoingPrefix() {
        logger.info("> {}", "{\"id\":1}");

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0)).startsWith("> ");
        assertThat(capturedMessages.get(0)).contains("{\"id\":1}");
    }

    @Test
    void infoWithIncomingPrefix_forwardsWithIncomingPrefix() {
        logger.info("< {}", "{\"result\":{}}");

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0)).startsWith("< ");
        assertThat(capturedMessages.get(0)).contains("{\"result\":{}}");
    }

    @Test
    void infoWithUnknownFormat_doesNotForward() {
        logger.info("Some other log: {}", "value");

        assertThat(capturedMessages).isEmpty();
    }

    @Test
    void debugWithRequestFormat_forwardsWithOutgoingPrefix() {
        logger.debug("Request: {}", "{\"method\":\"tools/call\"}");

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0)).startsWith("> ");
    }

    @Test
    void debugWithIncomingPrefix_forwardsWithIncomingPrefix() {
        logger.debug("< {}", "{\"data\":\"response\"}");

        assertThat(capturedMessages).hasSize(1);
        assertThat(capturedMessages.get(0)).startsWith("< ");
    }

    @Test
    void debugWithUnknownFormat_doesNotForward() {
        logger.debug("Some debug: {}", "value");

        assertThat(capturedMessages).isEmpty();
    }

    @Test
    void nullConsumer_doesNotThrow() {
        MCPTrafficLogger loggerNoConsumer = new MCPTrafficLogger(null);

        // These should not throw
        loggerNoConsumer.info("Request: {}", "body");
        loggerNoConsumer.debug("< {}", "data");
        loggerNoConsumer.info("> {}", "data");
    }

    @Test
    void delegationMethods_doNotThrow() {
        // Verify the pure delegation methods work without error
        logger.trace("trace msg");
        logger.trace("trace {} {}", "a", "b");
        logger.trace("trace {}", (Object) "a");
        logger.trace("trace {} {} {}", "a", "b", "c");
        logger.warn("warn msg");
        logger.warn("warn {}", (Object) "a");
        logger.warn("warn {} {}", "a", "b");
        logger.error("error msg");
        logger.error("error {}", (Object) "a");
        logger.error("error {} {}", "a", "b");
        logger.info("plain info");
        logger.info("info {} {}", "a", "b");
        logger.debug("plain debug");
        logger.debug("debug {} {}", "a", "b");

        // Only info/debug with single arg + traffic format should be captured
        // "plain info", "plain debug", and other non-traffic patterns should not be captured
    }

    @Test
    void isTraceEnabled_delegates() {
        // Just verify it doesn't throw and returns a boolean
        logger.isTraceEnabled();
    }

    @Test
    void isDebugEnabled_delegates() {
        logger.isDebugEnabled();
    }

    @Test
    void isInfoEnabled_delegates() {
        logger.isInfoEnabled();
    }

    @Test
    void isWarnEnabled_delegates() {
        logger.isWarnEnabled();
    }

    @Test
    void isErrorEnabled_delegates() {
        logger.isErrorEnabled();
    }
}
