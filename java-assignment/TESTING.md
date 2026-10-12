# Testing

## Test levels

| Level | Naming | Runs with | What it covers |
|---|---|---|---|
| Unit | `*Test`, plain JUnit 5 + Mockito | `./mvnw test` | Use cases and services in isolation, with mocked ports/repositories. Every business rule with positive, negative and edge cases. |
| Component | `*Test` with `@QuarkusTest` | `./mvnw test` | REST endpoints in the running app with a Postgres started by Dev Services: status codes, error bodies, transactions. External systems are mocked with `@InjectMock`. |
| Integration | `*IT` with `@QuarkusIntegrationTest` | `./mvnw verify` | The packaged jar against a real Postgres, end-to-end flows only. |

`@QuarkusTest` needs Docker running. The integration tests need Postgres on `localhost:15432`:

```sh
docker run -it --rm --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
./mvnw verify            # unit + component + integration tests + coverage check
./mvnw verify -DskipUTs  # integration tests only
```

## Coverage

JaCoCo measures the unit and component tests (`quarkus-jacoco` for `@QuarkusTest`). The report is written to `target/site/jacoco/index.html`.
`./mvnw verify` fails if any package is below **80% line coverage**. Code generated from the OpenAPI spec is excluded.

## What is tested

Positive = expected result, negative = rejected with the right status and nothing changed.

| Area | Positive | Negative / error |
|---|---|---|
| Location (`LocationGatewayTest`) | Existing identifier is resolved | Unknown, null or blank identifier returns `null` |
| Warehouse create (`CreateWarehouseUseCaseTest`, `WarehouseResourceTest`) | Valid warehouse is created; capacity that fills the location exactly is accepted | Existing business unit code, unknown location, location at max warehouses, capacity above location limit, stock above capacity, missing fields → 400 |
| Warehouse replace (`ReplaceWarehouseUseCaseTest`, `WarehouseResourceTest`) | Old warehouse archived and new one created with the same code; replaced warehouse not counted for location limits | Capacity can't hold current stock, stock doesn't match, invalid or full location → 400 and nothing archived; unknown code → 404 |
| Warehouse archive / get (`ArchiveWarehouseUseCaseTest`, `WarehouseResourceTest`) | Archived warehouse disappears from list and get; code can be reused | Already archived, unknown or non-numeric id → 404 |
| Store (`StoreResourceTest`, `LegacyStoreManagerGatewayTest`) | CRUD works; legacy system is called only **after** the commit, with the saved store | Commit fails (duplicate name) → legacy system never called; id on create / missing name → 422; unknown id → 404; legacy failure is logged, not thrown |
| Product (`ProductResourceTest`) | CRUD works | id on create / missing name → 422; unknown id → 404 |
| Fulfilment (`FulfilmentServiceTest`, `FulfilmentResourceTest`) | Assign, list with filters, remove; a warehouse/product already used doesn't count again at the limit | 3rd warehouse per product per store, 4th warehouse per store, 6th product per warehouse → 400; duplicate → 409; unknown store, product or (archived) warehouse → 404; missing fields → 400 |
| Error mapper (`GlobalExceptionMapperTest`) | Status taken from `WebApplicationException` | Other exceptions → 500; no message → no `error` field |
| Health (`HealthEndpointTest`, `LocationDataHealthCheckTest`) | Ready (database + location data) and live are UP | Location check is DOWN without locations |

The integration tests (`WarehouseOperationsIT`, `StoreEndpointIT`, `ProductEndpointIT`, `FulfilmentEndpointIT`, `HealthEndpointIT`, `WarehouseEndpointIT`) repeat the main flows and a few rejections against the packaged app.

## Conventions

- Test names describe the behaviour: `shouldRejectStockThatDoesNotMatch`, `createShouldReturn400WhenBusinessRulesFail`.
- Tests create their own data with unique names, so they don't depend on each other or on the run order.
- Failure tests also check that nothing was saved or changed.
