# Personalized Recommendation Engine

A compact hybrid recommendation engine — popularity, content-based
similarity, and item-based collaborative filtering, combined and always
**explained** — over synthetic purchase history. Every recommendation can
say, specifically, why it's there.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

160 synthetic customers, 1,102 purchases across a 124-product catalog.
Ask for one customer's recommendations; get back a ranked list where each
item names which signal drove it and, where applicable, exactly which
prior purchase caused the recommendation.

## 2. Problem

"Recommended for you" is easy to fake with a popularity list. The
harder, more valuable thing is a recommendation that can say *why* —
"because you bought X" only means something if that's genuinely the
purchase that produced the highest similarity score, not a plausible-sounding guess.

## 3. Solution

Three from-scratch signals — global popularity, content-based similarity
(best-matching prior purchase), and item-based collaborative filtering
(co-purchase patterns across all customers) — normalized per-customer and
combined with fixed weights. The explanation is generated from the exact
same data that produced the score, so it can never drift out of sync.

## 4. Key Features

- **Popularity**: simple, honest global purchase counts
- **Content-based**: category/brand/use-case/price-bucket similarity,
  tracking the single best-matching prior purchase
- **Collaborative filtering**: a real item-based co-purchase matrix built
  from all 1,102 purchases
- **Hybrid scoring**: per-customer-normalized weighted combination (0.2 /
  0.4 / 0.4)
- **Every recommendation is explained** — "Recommended because you
  previously purchased X" or "Customers who bought X also bought this,"
  generated from the winning signal, never hand-written separately
- Graceful fallback to popularity for a customer with no purchase history

## 5. Architecture

```
customerId
  ├─ PopularityService        (global purchase counts)
  ├─ ContentBasedService      (best-match similarity)
  └─ CollaborativeFilteringService (co-purchase matrix)
        │
        ▼  normalize per-customer → weighted sum → explain from winning signal
        ▼
Ranked, explained Recommendation list
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

- No recommendation/ML platform — three inspectable signals, combined
  deterministically (see docs/design-decisions.md for why)
- The co-purchase matrix is built once at startup from the full 1,102-row
  purchase history, not recomputed per request

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md) — including a
real display-name collision the shared product catalog turned up (not a
data-integrity bug — product ids are always unique) and why per-customer
signal normalization was chosen over a global maximum.

## 8. Sample Input

```
GET /api/customers/CUST0009/recommendations?limit=5
```

## 9. Sample Output

Real output for CUST0009, who purchased two long-distance running shoes, a
training jacket and a heart-rate monitor:

```json
[
  {
    "productId": "PR0086", "productName": "ThermoLayer Compression Tights", "productColor": "White",
    "score": 0.947, "signal": "CONTENT",
    "explanation": "Recommended because you previously purchased ThermoLayer Running Jacket (Red)."
  },
  {
    "productId": "PR0032", "productName": "CoreFit Heart Rate Monitor", "productColor": "White",
    "score": 0.818, "signal": "COLLABORATIVE",
    "explanation": "Customers who bought PacePro Heart Rate Monitor (Black) also bought this."
  }
]
```

*(3 more recommendations follow — see the test suite for the full, verified list.)*

## 10. How to Run

```bash
git clone <your-repo-url>
cd personalized-recommendation-engine
mvn spring-boot:run
```

API available at `http://localhost:8090`.

## 11. How to Test

```bash
mvn test
```

Covers the exact CUST0009 scenario above (top-2 recommendations, their
signals and explanations), plus general invariants: never recommending an
already-purchased product, descending score order, explanation-signal
consistency, and the no-history fallback to popularity.

## 12. API Documentation

| Method | Path | Description |
|---|---|---|
| GET | `/api/customers/{id}/recommendations` | `limit` (default 5, max 50) |
| GET | `/api/customers/{id}/purchases` | That customer's purchase history |
| GET | `/api/customers` | All customer ids in the dataset |

## 13. Portfolio Demo

The Commerce Engineering Lab demo (`lab/demos/personalized-recommendation-engine.js`)
ports all three signals to client-side JavaScript over the same bundled
data. See [`docs/demo.md`](docs/demo.md).

## 14. Limitations

- Synthetic purchase history (persona-driven, not real behavioral data)
- Similarity weights (category 0.4 / brand 0.2 / use-case 0.25 / price-bucket
  0.15) are hand-picked and illustrative, not learned
- No temporal decay — a purchase from a year ago weighs the same as one
  from yesterday

## 15. Future Enhancements

- Weight recent purchases more heavily than old ones
- A/B-test-style evaluation harness comparing signal weight combinations
- Session-based (not just purchase-history-based) signals

## 16. License

MIT — see [`LICENSE`](LICENSE).
