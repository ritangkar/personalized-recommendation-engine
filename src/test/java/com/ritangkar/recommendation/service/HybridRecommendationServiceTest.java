package com.ritangkar.recommendation.service;

import com.ritangkar.recommendation.model.Recommendation;
import com.ritangkar.recommendation.repository.RecommendationDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * CUST0009's recommendations were independently computed with a Python
 * reference implementation of the same popularity/content/collaborative/
 * hybrid pipeline, against this exact bundled dataset, before this test
 * was written — see docs/design-decisions.md.
 */
class HybridRecommendationServiceTest {

    private final RecommendationDataRepository repository = new RecommendationDataRepository();
    private final PopularityService popularityService = new PopularityService(repository);
    private final ContentBasedService contentBasedService = new ContentBasedService(repository);
    private final CollaborativeFilteringService collaborativeFilteringService = new CollaborativeFilteringService(repository);
    private final HybridRecommendationService service = new HybridRecommendationService(
            repository, popularityService, contentBasedService, collaborativeFilteringService);

    @Test
    void neverRecommendsAProductTheCustomerAlreadyBought() {
        List<String> purchased = repository.purchasedProductIds("CUST0009");
        List<Recommendation> recs = service.recommend("CUST0009", 10);

        assertThat(recs).noneMatch(r -> purchased.contains(r.productId()));
    }

    @Test
    void topRecommendationMatchesTheIndependentlyVerifiedReference() {
        List<Recommendation> recs = service.recommend("CUST0009", 5);

        assertThat(recs).hasSize(5);
        Recommendation top = recs.get(0);
        assertThat(top.productId()).isEqualTo("PR0086");
        assertThat(top.signal()).isEqualTo("CONTENT");
        assertThat(top.score()).isCloseTo(0.947, within(0.01));
        assertThat(top.explanation()).contains("ThermoLayer Running Jacket");
    }

    @Test
    void secondRecommendationIsCollaborativelyDriven() {
        List<Recommendation> recs = service.recommend("CUST0009", 5);

        Recommendation second = recs.get(1);
        assertThat(second.productId()).isEqualTo("PR0032");
        assertThat(second.signal()).isEqualTo("COLLABORATIVE");
        assertThat(second.explanation()).contains("PacePro Heart Rate Monitor");
    }

    @Test
    void recommendationsAreSortedByScoreDescending() {
        List<Recommendation> recs = service.recommend("CUST0009", 10);
        for (int i = 0; i < recs.size() - 1; i++) {
            assertThat(recs.get(i).score()).isGreaterThanOrEqualTo(recs.get(i + 1).score());
        }
    }

    @Test
    void everyExplanationIsConsistentWithItsOwnSignal() {
        List<Recommendation> recs = service.recommend("CUST0009", 10);
        for (Recommendation r : recs) {
            switch (r.signal()) {
                case "CONTENT" -> assertThat(r.explanation()).contains("previously purchased");
                case "COLLABORATIVE" -> assertThat(r.explanation()).contains("also bought");
                case "POPULARITY" -> assertThat(r.explanation()).contains("popular");
                default -> throw new AssertionError("Unexpected signal: " + r.signal());
            }
        }
    }

    @Test
    void aCustomerWithNoPurchaseHistoryFallsBackToPopularity() {
        List<Recommendation> recs = service.recommend("CUST9999-UNKNOWN", 5);

        assertThat(recs).isNotEmpty();
        assertThat(recs).allMatch(r -> r.signal().equals("POPULARITY"));
    }

    @Test
    void limitIsRespected() {
        assertThat(service.recommend("CUST0009", 3)).hasSizeLessThanOrEqualTo(3);
    }
}
