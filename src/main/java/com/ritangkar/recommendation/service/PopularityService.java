package com.ritangkar.recommendation.service;

import com.ritangkar.recommendation.model.Purchase;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PopularityService {

    private final RecommendationDataRepository repository;

    public PopularityService(RecommendationDataRepository repository) {
        this.repository = repository;
    }

    /** Raw purchase counts for every product, across all customers. */
    public Map<String, Integer> globalPurchaseCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Purchase p : repository.findAllPurchases()) {
            counts.merge(p.productId(), 1, Integer::sum);
        }
        return counts;
    }
}
