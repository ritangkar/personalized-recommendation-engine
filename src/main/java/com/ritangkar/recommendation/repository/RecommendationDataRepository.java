package com.ritangkar.recommendation.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritangkar.recommendation.model.Product;
import com.ritangkar.recommendation.model.Purchase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Repository
public class RecommendationDataRepository {

    private final List<Product> products;
    private final List<Purchase> purchases;
    private final Map<String, Product> productsById;
    private final Map<String, List<String>> purchasedProductIdsByCustomer;

    public RecommendationDataRepository() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.products = readList(mapper, "sample-data/products.json", Product[].class);
            this.purchases = readList(mapper, "sample-data/purchases.json", Purchase[].class);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled sample data", e);
        }

        Map<String, Product> byId = new LinkedHashMap<>();
        for (Product p : products) byId.put(p.id(), p);
        this.productsById = byId;

        Map<String, List<String>> byCustomer = new LinkedHashMap<>();
        for (Purchase pur : purchases) {
            byCustomer.computeIfAbsent(pur.customerId(), k -> new ArrayList<>()).add(pur.productId());
        }
        this.purchasedProductIdsByCustomer = byCustomer;
    }

    private <T> List<T> readList(ObjectMapper mapper, String classpathLocation, Class<T[]> arrayType) throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return List.of(mapper.readValue(in, arrayType));
        }
    }

    public List<Product> findAllProducts() { return products; }
    public List<Purchase> findAllPurchases() { return purchases; }
    public Optional<Product> findProductById(String id) { return Optional.ofNullable(productsById.get(id)); }

    /** Product ids this customer has purchased, in the order purchases were recorded. Empty list if unknown customer. */
    public List<String> purchasedProductIds(String customerId) {
        return purchasedProductIdsByCustomer.getOrDefault(customerId, List.of());
    }

    public Set<String> allCustomerIds() {
        return purchasedProductIdsByCustomer.keySet();
    }
}
