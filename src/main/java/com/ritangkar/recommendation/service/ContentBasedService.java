package com.ritangkar.recommendation.service;

import com.ritangkar.recommendation.model.Product;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * For each candidate product the customer hasn't bought, finds the
 * customer's <b>single most similar</b> already-purchased product and uses
 * that similarity as the score — remembering which purchase drove it, so
 * the recommendation can say "because you bought X."
 */
@Service
public class ContentBasedService {

    private static final double CATEGORY_WEIGHT = 0.40;
    private static final double BRAND_WEIGHT = 0.20;
    private static final double USE_CASE_WEIGHT = 0.25;
    private static final double PRICE_BUCKET_WEIGHT = 0.15;

    private final RecommendationDataRepository repository;

    public ContentBasedService(RecommendationDataRepository repository) {
        this.repository = repository;
    }

    public record ScoredCandidate(double score, String drivingProductId) {
    }

    public Map<String, ScoredCandidate> scoreForCustomer(String customerId) {
        List<String> purchasedIds = repository.purchasedProductIds(customerId);
        Set<String> purchasedSet = new HashSet<>(purchasedIds);
        List<Product> purchased = purchasedIds.stream()
                .map(id -> repository.findProductById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        Map<String, ScoredCandidate> result = new LinkedHashMap<>();
        for (Product candidate : repository.findAllProducts()) {
            if (purchasedSet.contains(candidate.id())) continue;

            double bestSimilarity = 0;
            String bestDriver = null;
            for (Product owned : purchased) {
                double sim = similarity(owned, candidate);
                if (sim > bestSimilarity) {
                    bestSimilarity = sim;
                    bestDriver = owned.id();
                }
            }
            if (bestSimilarity > 0) {
                result.put(candidate.id(), new ScoredCandidate(bestSimilarity, bestDriver));
            }
        }
        return result;
    }

    private double similarity(Product a, Product b) {
        double score = 0.0;
        if (a.category().equals(b.category())) score += CATEGORY_WEIGHT;
        if (a.brand().equals(b.brand())) score += BRAND_WEIGHT;
        if (a.useCase().equals(b.useCase())) score += USE_CASE_WEIGHT;
        if (priceBucket(a.price()) == priceBucket(b.price())) score += PRICE_BUCKET_WEIGHT;
        return score;
    }

    private int priceBucket(double price) {
        if (price < 1000) return 0;
        if (price < 3000) return 1;
        if (price < 6000) return 2;
        if (price < 10000) return 3;
        return 4;
    }
}
