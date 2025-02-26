package com.devoxx.genie.util;

import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpClientProviderTest {

    @Mock
    private Interceptor.Chain mockChain;

    @Mock
    private Request mockRequest;

    @Mock
    private Response mockResponse;

    @Mock
    private Response.Builder mockResponseBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetClient_ReturnsSameInstance() {
        // Get client twice
        OkHttpClient client1 = HttpClientProvider.getClient();
        OkHttpClient client2 = HttpClientProvider.getClient();

        // Verify both references point to the same instance
        assertSame(client1, client2, "HttpClientProvider should return the same OkHttpClient instance");
    }

    @Test
    void testGetClient_HasCorrectTimeouts() {
        OkHttpClient client = HttpClientProvider.getClient();

        assertEquals(10, client.connectTimeoutMillis() / 1000,
                "Connect timeout should be 10 seconds");
        assertEquals(30, client.readTimeoutMillis() / 1000,
                "Read timeout should be 30 seconds");
        assertEquals(30, client.writeTimeoutMillis() / 1000,
                "Write timeout should be 30 seconds");
    }

    @Test
    void testGetClient_HasConnectionPool() {
        OkHttpClient client = HttpClientProvider.getClient();

        ConnectionPool pool = client.connectionPool();
        assertNotNull(pool, "Client should have a connection pool");

        // Cannot directly test max idle connections and keep alive in unit tests
        // because those fields are not exposed, but we can verify the pool exists
    }

    @Test
    void testGetClient_HasRetryInterceptor() {
        OkHttpClient client = HttpClientProvider.getClient();

        boolean hasRetryInterceptor = client.interceptors().stream()
                .anyMatch(interceptor -> interceptor.getClass().getSimpleName().equals("RetryInterceptor"));

        assertTrue(hasRetryInterceptor, "Client should have a RetryInterceptor");
    }

    @Test
    void testRetryInterceptor_SuccessfulFirstAttempt() throws IOException {
        // Create test instance of RetryInterceptor
        Interceptor retryInterceptor = new HttpClientProvider.RetryInterceptor(3);

        // Setup mock behavior for successful response
        when(mockChain.request()).thenReturn(mockRequest);
        when(mockChain.proceed(mockRequest)).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);

        // Execute the interceptor
        Response result = retryInterceptor.intercept(mockChain);

        // Verify
        assertSame(mockResponse, result, "Should return the successful response");
        verify(mockChain, times(1)).proceed(mockRequest);
        verify(mockResponse, never()).close();
    }

    @Test
    void testRetryInterceptor_RetryAfterFailedResponse() throws IOException {
        // Create test instance of RetryInterceptor
        Interceptor retryInterceptor = new HttpClientProvider.RetryInterceptor(3);

        // Setup mock behavior for unsuccessful then successful response
        when(mockChain.request()).thenReturn(mockRequest);

        // First response is unsuccessful
        Response unsuccessfulResponse = mock(Response.class);
        when(unsuccessfulResponse.isSuccessful()).thenReturn(false);

        // Second response is successful
        when(mockChain.proceed(mockRequest))
                .thenReturn(unsuccessfulResponse)
                .thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);

        // Execute the interceptor
        Response result = retryInterceptor.intercept(mockChain);

        // Verify
        assertSame(mockResponse, result, "Should return the successful response after retry");
        verify(mockChain, times(2)).proceed(mockRequest);
        verify(unsuccessfulResponse, times(1)).close();
    }

    @Test
    void testRetryInterceptor_HandlesExceptions() throws IOException {
        // Create test instance of RetryInterceptor
        Interceptor retryInterceptor = new HttpClientProvider.RetryInterceptor(2);

        // Setup mock behavior
        when(mockChain.request()).thenReturn(mockRequest);
        when(mockChain.proceed(mockRequest))
                .thenThrow(new IOException("Network error"))
                .thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);

        // Execute the interceptor
        Response result = retryInterceptor.intercept(mockChain);

        // Verify
        assertSame(mockResponse, result, "Should return the successful response after exception");
        verify(mockChain, times(2)).proceed(mockRequest);
    }

    @Test
    void testRetryInterceptor_ThrowsAfterMaxExceptions() throws IOException {
        // Create test instance of RetryInterceptor
        Interceptor retryInterceptor = new HttpClientProvider.RetryInterceptor(2);

        // Setup mock behavior
        when(mockChain.request()).thenReturn(mockRequest);
        try {
            when(mockChain.proceed(mockRequest))
                    .thenThrow(new IOException("First error"))
                    .thenThrow(new IOException("Second error"));

            // Execute the interceptor
            retryInterceptor.intercept(mockChain);
            fail("Should have thrown IOException after max retries");
        } catch (IOException e) {
            // Expected exception
            verify(mockChain, times(2)).proceed(mockRequest);
        }
    }

    @Test
    void testRetryInterceptor_HandlesInterruption() {
        // Create test instance of RetryInterceptor with mocked Thread
        Interceptor retryInterceptor = new HttpClientProvider.RetryInterceptor(3);

        // Setup mock behavior
        when(mockChain.request()).thenReturn(mockRequest);
        try {
            when(mockChain.proceed(mockRequest))
                    .thenAnswer(invocation -> {
                        // Simulate Thread.sleep being interrupted
                        Thread.currentThread().interrupt();
                        Response response = mock(Response.class);
                        when(response.isSuccessful()).thenReturn(false);
                        return response;
                    });

            // Execute the interceptor
            retryInterceptor.intercept(mockChain);
            fail("Should have thrown IOException after interruption");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Retry interrupted"),
                    "Exception should indicate retry was interrupted");
            assertTrue(Thread.interrupted(), "Interrupted flag should be cleared");
        }
    }
}
