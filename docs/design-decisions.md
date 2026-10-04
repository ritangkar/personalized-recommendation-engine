# Design Decisions

---

**Decision:** Three simple, from-scratch signals (popularity, content-based
similarity, item-based collaborative filtering) combined by a fixed
weighted sum, rather than a matrix-factorization or embedding-based
recommender.

**Why:** The master brief explicitly asks to "avoid building a giant ML
platform." Each signal here is a few dozen lines of Java, fully
inspectable, and — critically — each one can name a *specific reason* for
a recommendation. A latent-factor model can be more accurate at scale but
produces recommendations that are much harder to explain in one sentence,
which is the actual deliverable this project is demonstrating.

**Trade-off:** Less predictive power than a real production recommender
trained on much larger interaction data; the similarity function
(category/brand/useCase/price-bucket) is a hand-picked, illustrative
weighting, not learned from data.

---

**Decision:** Reuse the 124-product catalog from the Smart Product Search
Engine project (Batch 2) rather than generating a fresh one.

**Why:** A shared catalog across projects that touch commerce data is more
representative of a real platform than each project inventing its own
product universe, and it meant this project's purchase-history generator
could focus entirely on realistic persona-driven buying patterns.

**A real thing this surfaced, not smoothed over:** that catalog's product
*names* aren't guaranteed unique across color/price variants (e.g. two
different `HydroFlow Running Backpack` SKUs with different ids and
colors) — a display-name collision, not a data-integrity bug, since
product **ids** are always unique and the recommendation logic operates on
ids throughout. It does mean a name alone isn't always enough to tell two
recommended products apart at a glance, which is why every API response
and the documented example include color alongside name.

**Trade-off:** None functional — purely a display clarity note.

---

**Decision:** Normalize each signal (popularity, content, collaborative)
to [0, 1] **per customer**, relative to that customer's own candidate
scores, before combining — not against a single global maximum shared by
every customer.

**Why:** A customer with a small, focused purchase history might have
every content-similarity candidate score fairly high (everything looks
similar to a narrow history); a customer with a broad, eclectic history
might never produce a high content score for anything. Normalizing against
a shared global maximum would let one customer's signal strength "leak"
into how another customer's recommendations get weighted. Per-customer
normalization keeps every customer's hybrid score internally consistent,
even if it means raw scores aren't directly comparable across customers.

**Trade-off:** A weak signal for one customer can rank equally with a
strong signal for another, since both get rescaled to the same [0,1]
range — acceptable here since recommendations are always shown per-customer,
never compared across customers.
