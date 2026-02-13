package com.devoxx.genie.service.acp.protocol;

import com.devoxx.genie.service.acp.protocol.exception.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcpExceptionHierarchyTest {

    @Test
    void testAcpException_withMessage() {
        AcpException ex = new AcpException("test error");
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void testAcpException_withMessageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        AcpException ex = new AcpException("test error", cause);
        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void testAcpException_extendsException() {
        assertThat(new AcpException("test")).isInstanceOf(Exception.class);
    }

    @Test
    void testAcpConnectionException_withMessage() {
        AcpConnectionException ex = new AcpConnectionException("connection error");
        assertThat(ex.getMessage()).isEqualTo("connection error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void testAcpConnectionException_withMessageAndCause() {
        Throwable cause = new java.io.IOException("network down");
        AcpConnectionException ex = new AcpConnectionException("connection error", cause);
        assertThat(ex.getMessage()).isEqualTo("connection error");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void testAcpConnectionException_extendsAcpException() {
        assertThat(new AcpConnectionException("test")).isInstanceOf(AcpException.class);
    }

    @Test
    void testAcpSessionException_withMessage() {
        AcpSessionException ex = new AcpSessionException("session error");
        assertThat(ex.getMessage()).isEqualTo("session error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void testAcpSessionException_withMessageAndCause() {
        Throwable cause = new RuntimeException("bad state");
        AcpSessionException ex = new AcpSessionException("session error", cause);
        assertThat(ex.getMessage()).isEqualTo("session error");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void testAcpSessionException_extendsAcpException() {
        assertThat(new AcpSessionException("test")).isInstanceOf(AcpException.class);
    }

    @Test
    void testAcpTimeoutException_withMessage() {
        AcpTimeoutException ex = new AcpTimeoutException("timed out");
        assertThat(ex.getMessage()).isEqualTo("timed out");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void testAcpTimeoutException_withMessageAndCause() {
        Throwable cause = new java.util.concurrent.TimeoutException("120s elapsed");
        AcpTimeoutException ex = new AcpTimeoutException("timed out", cause);
        assertThat(ex.getMessage()).isEqualTo("timed out");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void testAcpTimeoutException_extendsAcpException() {
        assertThat(new AcpTimeoutException("test")).isInstanceOf(AcpException.class);
    }

    @Test
    void testAcpRequestException_extendsAcpException() {
        assertThat(new AcpRequestException("test")).isInstanceOf(AcpException.class);
    }

    @Test
    void testAcpRequestException_withMessage() {
        AcpRequestException ex = new AcpRequestException("request failed");
        assertThat(ex.getMessage()).isEqualTo("request failed");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void testAcpRequestException_withMessageAndCause() {
        Throwable cause = new RuntimeException("execution error");
        AcpRequestException ex = new AcpRequestException("request failed", cause);
        assertThat(ex.getMessage()).isEqualTo("request failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void testAllExceptionsAreCatchableAsAcpException() {
        // Verify the hierarchy allows catching all ACP exceptions with a single catch block
        Exception[] exceptions = {
                new AcpConnectionException("conn"),
                new AcpSessionException("session"),
                new AcpTimeoutException("timeout"),
                new AcpRequestException("request")
        };

        for (Exception ex : exceptions) {
            assertThat(ex).isInstanceOf(AcpException.class);
        }
    }
}
