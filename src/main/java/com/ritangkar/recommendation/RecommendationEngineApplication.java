package com.ritangkar.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Personalized Recommendation Engine.
 *
 * Three independent signals — popularity, content-based similarity, and
 * item-based collaborative filtering — normalized and combined into one
 * hybrid score, with every recommendation traceable to the specific
 * signal (and, where applicable, the specific prior purchase) that drove
 * it. No external ML platform; see docs/design-decisions.md.
 */
@SpringBootApplication
public class RecommendationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationEngineApplication.class, args);
    }
}
