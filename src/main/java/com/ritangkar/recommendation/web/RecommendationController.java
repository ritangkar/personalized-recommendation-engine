package com.ritangkar.recommendation.web;

import com.ritangkar.recommendation.model.Product;
import com.ritangkar.recommendation.model.Recommendation;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import com.ritangkar.recommendation.service.HybridRecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RecommendationController {

    private final HybridRecommendationService recommendationService;
    private final RecommendationDataRepository repository;

    public RecommendationController(HybridRecommendationService recommendationService, RecommendationDataRepository repository) {
        this.recommendationService = recommendationService;
        this.repository = repository;
    }

    /** GET /api/customers/{customerId}/recommendations?limit=5 */
    @GetMapping("/api/customers/{customerId}/recommendations")
    public List<Recommendation> recommendations(@PathVariable String customerId,
                                                  @RequestParam(defaultValue = "5") int limit) {
        return recommendationService.recommend(customerId, Math.max(1, Math.min(limit, 50)));
    }

    @GetMapping("/api/customers/{customerId}/purchases")
    public List<Product> purchases(@PathVariable String customerId) {
        return repository.purchasedProductIds(customerId).stream()
                .map(id -> repository.findProductById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @GetMapping("/api/customers")
    public List<String> customers() {
        return repository.allCustomerIds().stream().sorted().toList();
    }
}
