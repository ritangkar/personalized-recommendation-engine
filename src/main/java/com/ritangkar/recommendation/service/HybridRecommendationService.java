package com.ritangkar.recommendation.service;

import com.ritangkar.recommendation.model.Product;
import com.ritangkar.recommendation.model.Recommendation;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Normalizes each of the three signals to [0, 1] (relative to the
 * customer's own candidate set, so scores are comparable regardless of
 * catalog size) and combines them with fixed weights: popularity 0.2,
 * content-based 0.4, collaborative 0.4. Whichever signal contributed the
 * most to a candidate's final score names itself in the explanation — the
 * explanation is generated from the same numbers that produced the score,
 * not written separately.
 *
 * A customer with no purchase history yet (or an unknown id) naturally
 * falls back to pure popularity, since content and collaborative scores
 * are empty for them — no special-casing needed.
 */
@Service
public class HybridRecommendationService {

    private static final double POPULARITY_WEIGHT = 0.2;
    private static final double CONTENT_WEIGHT = 0.4;
    private static final double COLLABORATIVE_WEIGHT = 0.4;

    private final RecommendationDataRepository repository;
    private final PopularityService popularityService;
    private final ContentBasedService contentBasedService;
    private final CollaborativeFilteringService collaborativeFilteringService;

    public HybridRecommendationService(RecommendationDataRepository repository,
                                        PopularityService popularityService,
                                        ContentBasedService contentBasedService,
                                        CollaborativeFilteringService collaborativeFilteringService) {
        this.repository = repository;
        this.popularityService = popularityService;
        this.contentBasedService = contentBasedService;
        this.collaborativeFilteringService = collaborativeFilteringService;
    }

    public List<Recommendation> recommend(String customerId, int topN) {
        Set<String> purchased = new HashSet<>(repository.purchasedProductIds(customerId));

        Map<String, Integer> popularityRaw = popularityService.globalPurchaseCounts();
        Map<String, Double> popularityCandidates = new HashMap<>();
        for (Product p : repository.findAllProducts()) {
            if (purchased.contains(p.id())) continue;
            popularityCandidates.put(p.id(), popularityRaw.getOrDefault(p.id(), 0).doubleValue());
        }

        Map<String, ContentBasedService.ScoredCandidate> content = contentBasedService.scoreForCustomer(customerId);
        Map<String, CollaborativeFilteringService.ScoredCandidate> collaborative =
                collaborativeFilteringService.scoreForCustomer(customerId);

        Map<String, Double> popN = normalize(popularityCandidates);
        Map<String, Double> contentN = normalize(mapValues(content, ContentBasedService.ScoredCandidate::score));
        Map<String, Double> collabN = normalize(mapValues(collaborative, CollaborativeFilteringService.ScoredCandidate::score));

        Set<String> allCandidateIds = new LinkedHashSet<>();
        allCandidateIds.addAll(popN.keySet());
        allCandidateIds.addAll(contentN.keySet());
        allCandidateIds.addAll(collabN.keySet());

        List<Recommendation> results = new ArrayList<>();
        for (String candidateId : allCandidateIds) {
            double popScore = popN.getOrDefault(candidateId, 0.0) * POPULARITY_WEIGHT;
            double contentScore = contentN.getOrDefault(candidateId, 0.0) * CONTENT_WEIGHT;
            double collabScore = collabN.getOrDefault(candidateId, 0.0) * COLLABORATIVE_WEIGHT;
            double total = popScore + contentScore + collabScore;

            String topSignal = topSignal(popScore, contentScore, collabScore);
            Product product = repository.findProductById(candidateId).orElse(null);
            if (product == null) continue;

            String explanation = buildExplanation(topSignal, product,
                    topSignal.equals("CONTENT") ? content.get(candidateId) : null,
                    topSignal.equals("COLLABORATIVE") ? collaborative.get(candidateId) : null);

            results.add(new Recommendation(product.id(), product.name(), product.color(), round3(total), topSignal, explanation));
        }

        results.sort(Comparator.comparingDouble(Recommendation::score).reversed());
        return results.size() > topN ? results.subList(0, topN) : results;
    }

    private String topSignal(double pop, double content, double collab) {
        if (content >= pop && content >= collab) return "CONTENT";
        if (collab >= pop && collab >= content) return "COLLABORATIVE";
        return "POPULARITY";
    }

    private String buildExplanation(String signal, Product product,
                                     ContentBasedService.ScoredCandidate contentMatch,
                                     CollaborativeFilteringService.ScoredCandidate collabMatch) {
        return switch (signal) {
            case "CONTENT" -> {
                Product driver = contentMatch != null ? repository.findProductById(contentMatch.drivingProductId()).orElse(null) : null;
                yield driver != null
                        ? "Recommended because you previously purchased " + driver.name() + " (" + driver.color() + ")."
                        : "Recommended based on similarity to your past purchases.";
            }
            case "COLLABORATIVE" -> {
                Product driver = collabMatch != null ? repository.findProductById(collabMatch.drivingProductId()).orElse(null) : null;
                yield driver != null
                        ? "Customers who bought " + driver.name() + " (" + driver.color() + ") also bought this."
                        : "Recommended based on what similar customers bought.";
            }
            default -> "A popular choice among all customers.";
        };
    }

    private <K, V> Map<K, Double> mapValues(Map<K, V> source, java.util.function.ToDoubleFunction<V> extractor) {
        Map<K, Double> result = new HashMap<>();
        source.forEach((k, v) -> result.put(k, extractor.applyAsDouble(v)));
        return result;
    }

    private Map<String, Double> normalize(Map<String, Double> scores) {
        if (scores.isEmpty()) return Map.of();
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max == 0) return scores;
        Map<String, Double> normalized = new HashMap<>();
        scores.forEach((k, v) -> normalized.put(k, v / max));
        return normalized;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
