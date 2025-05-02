package com.devoxx.genie.service.projectscanner;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenCalculatorTest {

    private Encoding mockEncoding;

    @BeforeEach
    void setUp() {
        // Reset the mock before each test
        mockEncoding = mock(Encoding.class);
    }

    /**
     * Helper method to create a fresh TokenCalculator instance for each test
     * This helps avoid issues with static state between tests
     */
    private TokenCalculator createCalculatorWithMockedEncoding(@NotNull MockedStatic<Encodings> encodingsMock) {
        var mockRegistry = mock(com.knuddels.jtokkit.api.EncodingRegistry.class);
        encodingsMock.when(Encodings::newDefaultEncodingRegistry).thenReturn(mockRegistry);
        when(mockRegistry.getEncoding(EncodingType.CL100K_BASE)).thenReturn(mockEncoding);

        // Create a new instance with our mocked dependencies
        return new TokenCalculator();
    }

    @Test
    void testCalculateTokens() {
        // Setup mocks
        Encoding mockEncoding = mock(Encoding.class);
        when(mockEncoding.countTokensOrdinary(anyString())).thenReturn(5);

        // Inject mock into TokenCalculator
        TokenCalculator calculator = new TokenCalculator(mockEncoding);

        // Define test data and execute
        String text = "This is a test string";
        int result = calculator.calculateTokens(text);

        // Verify
        assertEquals(5, result);
        verify(mockEncoding).countTokensOrdinary(text);
    }

    @Test
    void testTruncateToTokens_NoTruncationNeeded() {
        try (MockedStatic<Encodings> encodingsMock = mockStatic(Encodings.class)) {
            // Setup mocks and calculator
            TokenCalculator calculator = createCalculatorWithMockedEncoding(encodingsMock);

            // Define test data
            String text = "This is a short string";
            IntArrayList tokens = new IntArrayList(5);
            for (int i = 0; i < 5; i++) {
                tokens.add(i + 1); // Add some dummy token IDs
            }

            when(mockEncoding.encodeOrdinary(text)).thenReturn(tokens);

            // Execute the method - maxTokens more than actual tokens
            String result = calculator.truncateToTokens(text, 10, false);

            // Verify
            assertEquals(text, result); // Should return the original text
            verify(mockEncoding).encodeOrdinary(text);
            verify(mockEncoding, never()).decode(any(IntArrayList.class));
        }
    }

    @Test
    void testTruncateToTokens_TruncationNeeded() {
        try (MockedStatic<Encodings> encodingsMock = mockStatic(Encodings.class)) {
            // Setup mocks and calculator
            TokenCalculator calculator = createCalculatorWithMockedEncoding(encodingsMock);

            // Define test data
            String text = "This is a longer string that needs truncation";
            String truncatedText = "This is a longer string";

            // Create tokens for the original text
            IntArrayList tokens = new IntArrayList(10);
            for (int i = 0; i < 10; i++) {
                tokens.add(i + 1); // Add some dummy token IDs
            }

            when(mockEncoding.encodeOrdinary(text)).thenReturn(tokens);

            // Mock the decode method to return our truncated text
            when(mockEncoding.decode(any(IntArrayList.class))).thenReturn(truncatedText);

            // Execute the method - truncate to 5 tokens
            String result = calculator.truncateToTokens(text, 5, false);

            // Verify
            assertEquals(truncatedText + "\n--- Project context truncated due to token limit ---\n", result);
            verify(mockEncoding).encodeOrdinary(text);
            verify(mockEncoding).decode(any(IntArrayList.class));
        }
    }

    @Test
    void testTruncateToTokens_TruncationNeededWithTokenCalculation() {
        try (MockedStatic<Encodings> encodingsMock = mockStatic(Encodings.class)) {
            // Setup mocks and calculator
            TokenCalculator calculator = createCalculatorWithMockedEncoding(encodingsMock);

            // Define test data
            String text = "This is a longer string that needs truncation";
            String truncatedText = "This is a longer string";

            // Create tokens for the original text
            IntArrayList tokens = new IntArrayList(10);
            for (int i = 0; i < 10; i++) {
                tokens.add(i + 1); // Add some dummy token IDs
            }

            when(mockEncoding.encodeOrdinary(text)).thenReturn(tokens);

            // Mock the decode method to return our truncated text
            when(mockEncoding.decode(any(IntArrayList.class))).thenReturn(truncatedText);

            // Execute the method - truncate to 5 tokens with isTokenCalculation=true
            String result = calculator.truncateToTokens(text, 5, true);

            // Verify - should not include the truncation message
            assertEquals(truncatedText, result);
            verify(mockEncoding).encodeOrdinary(text);
            verify(mockEncoding).decode(any(IntArrayList.class));
        }
    }

    @Test
    void testTokenLengthCalculationAccuracy() {
        // For tests that use the real implementation, create a separate instance
        // to avoid interference with mocked tests
        TokenCalculator calculator = new TokenCalculator();

        // Simple strings with predictable token counts
        assertEquals(1, calculator.calculateTokens("Hello"));
        assertEquals(2, calculator.calculateTokens("Hello world"));

        // Generate a longer text to test larger token counts
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is sentence ").append(i).append(". ");
        }

        // Verify the token count is in a reasonable range
        int tokenCount = calculator.calculateTokens(longText.toString());
        assertTrue(tokenCount > 300); // Rough estimate based on tokenization rules
    }

    @Test
    void testTruncationBoundaryConsistency() {
        // Use a dedicated instance for this test
        TokenCalculator calculator = new TokenCalculator();

        // Create a text with clear token boundaries (each word is likely one token)
        String text = "one two three four five six seven eight nine ten";

        // Truncate to different token limits
        String truncated3 = calculator.truncateToTokens(text, 3, true);
        String truncated5 = calculator.truncateToTokens(text, 5, true);

        // Verify truncated text ends at word boundaries
        assertTrue(truncated3.endsWith("three"));
        assertTrue(truncated5.endsWith("five"));

        // Calculate tokens in truncated text to verify it matches the limit
        assertEquals(3, calculator.calculateTokens(truncated3));
        assertEquals(5, calculator.calculateTokens(truncated5));
    }
}
