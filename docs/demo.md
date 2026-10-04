# Portfolio Demo

**Demo level:** 2 — Portfolio simulation (bundled sample data, client-side).

`lab/demos/personalized-recommendation-engine.js` ports all three signals
and the hybrid combiner to client-side JavaScript over the exact same
bundled catalog and purchase history (160 customers, 1,102 purchases). A
recruiter picks a customer from a dropdown, sees their purchase history,
and gets the identical ranked, explained recommendations this README
documents — including the verified CUST0009 example. No backend call.

The real Spring Boot API (`mvn spring-boot:run`) exposes the full
`/api/customers/{id}/recommendations` surface for any customer in the dataset.
