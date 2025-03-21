package com.devoxx.genie.service.projectscanner;

import com.intellij.openapi.diagnostic.Logger;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Handles token calculations and truncation.
 */
public class TokenCalculator {
    private static final Logger LOG = Logger.getInstance(TokenCalculator.class.getName());
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
        LOG.info("Calculating tokens for text of length: " + text.length());
        int tokenCount = encoding.countTokensOrdinary(text);
        LOG.info("Token count calculated: " + tokenCount);
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
