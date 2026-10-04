package com.ritangkar.recommendation.model;

public record Product(
        String id,
        String name,
        String category,
        String brand,
        String color,
        double price,
        String useCase,
        String material,
        double rating,
        int reviewCount,
        boolean inStock,
        String description
) {
}
