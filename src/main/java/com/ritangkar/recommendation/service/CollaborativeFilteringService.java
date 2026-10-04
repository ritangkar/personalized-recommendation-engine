package com.ritangkar.recommendation.service;

import com.ritangkar.recommendation.model.Product;
import com.ritangkar.recommendation.model.Purchase;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Classic item-based collaborative filtering: builds a co-purchase matrix
 * once (how often each pair of products was bought by the same customer,
 * across the whole dataset), then for a target customer sums the
 * co-occurrence between their purchases and every candidate product —
 * "customers who bought what you bought also bought this."
 */
@Service
public class CollaborativeFilteringService {

    private final RecommendationDataRepository repository;
    private final Map<String, Map<String, Integer>> coPurchaseMatrix;

    public CollaborativeFilteringService(RecommendationDataRepository repository) {
        this.repository = repository;
        this.coPurchaseMatrix = buildCoPurchaseMatrix();
    }

    public record ScoredCandidate(double score, String drivingProductId) {
    }

    private Map<String, Map<String, Integer>> buildCoPurchaseMatrix() {
        Map<String, Set<String>> productsByCustomer = new LinkedHashMap<>();
        for (Purchase p : repository.findAllPurchases()) {
            productsByCustomer.computeIfAbsent(p.customerId(), k -> new LinkedHashSet<>()).add(p.productId());
        }

        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        for (Set<String> owned : productsByCustomer.values()) {
            List<String> ownedList = new ArrayList<>(owned);
            for (int i = 0; i < ownedList.size(); i++) {
                for (int j = 0; j < ownedList.size(); j++) {
                    if (i == j) continue;
                    matrix.computeIfAbsent(ownedList.get(i), k -> new HashMap<>())
                            .merge(ownedList.get(j), 1, Integer::sum);
                }
            }
        }
        return matrix;
    }

    public Map<String, ScoredCandidate> scoreForCustomer(String customerId) {
        List<String> purchasedIds = repository.purchasedProductIds(customerId);
        Set<String> purchasedSet = new HashSet<>(purchasedIds);

        Map<String, ScoredCandidate> result = new LinkedHashMap<>();
        for (Product candidate : repository.findAllProducts()) {
            if (purchasedSet.contains(candidate.id())) continue;

            int total = 0;
            String bestDriver = null;
            int bestContribution = 0;
            for (String owned : purchasedIds) {
                int contribution = coPurchaseMatrix.getOrDefault(owned, Map.of()).getOrDefault(candidate.id(), 0);
                total += contribution;
                if (contribution > bestContribution) {
                    bestContribution = contribution;
                    bestDriver = owned;
                }
            }
            if (total > 0) {
                result.put(candidate.id(), new ScoredCandidate(total, bestDriver));
            }
        }
        return result;
    }
}
