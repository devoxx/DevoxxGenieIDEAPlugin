package com.devoxx.genie.service.projectscanner;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Handles token calculations and truncation.
 */
@Slf4j
public class TokenCalculator {
    private final Encoding encoding;

    // Constructor injection
    public TokenCalculator(Encoding encoding) {
        this.encoding = encoding;
    }

    // Default constructor for production code
    public TokenCalculator() {
        this(Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE));
    }

    /**
     * Calculates token count for any text.
     * 
     * @param text The text to count tokens for
     * @return The token count
     */
    public int calculateTokens(@NotNull String text) {
        // Make sure we're getting the accurate token count
        log.info("Calculating tokens for text of length: " + text.length());
        int tokenCount = encoding.countTokensOrdinary(text);
        log.info("Token count calculated: " + tokenCount);
        return tokenCount;
    }

    public String truncateToTokens(@NotNull String text, int maxTokens, boolean isTokenCalculation) {
        IntArrayList tokens = encoding.encodeOrdinary(text);
        if (tokens.size() <= maxTokens) {
            return text;
        }
        IntArrayList truncatedTokens = new IntArrayList(maxTokens);
        for (int i = 0; i < maxTokens; i++) {
            truncatedTokens.add(tokens.get(i));
        }
        String truncatedContent = encoding.decode(truncatedTokens);
        return isTokenCalculation ? truncatedContent :
                truncatedContent + "\n--- Project context truncated due to token limit ---\n";
    }
}
