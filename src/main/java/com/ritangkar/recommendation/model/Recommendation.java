package com.ritangkar.recommendation.model;

/**
 * One recommended product. {@code signal} names which of the three scoring
 * components contributed most to {@code score} (POPULARITY, CONTENT, or
 * COLLABORATIVE); {@code explanation} is a human-readable sentence
 * generated from that same signal — never a separate, potentially
 * inconsistent description.
 */
public record Recommendation(
        String productId,
        String productName,
        String productColor,
        double score,
        String signal,
        String explanation
) {
}
