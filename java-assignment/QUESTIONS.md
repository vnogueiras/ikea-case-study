# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
I would refactor towards the repository approach for Store, so the code base uses one consistent pattern. But I would not 
change anything on the Warehouse side, as it makes sense where there are business rules (Warehouse has many
validations).
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
API-first (Warehouse):
+ The YAML is the contract; it can be shared and reviewed before any code exists,
  and clients can be generated from it.
+ The compiler enforces that the implementation matches the spec.
- Less flexible: e.g. the generated interface returns a Warehouse, so I could not
  return 201 on create as the spec says without changing the contract.
- Generated names (createANewWarehouseUnit) and an extra build step.

Code-first (Product, Store):
+ Fast and flexible, full control over status codes and responses.
- No contract: the API is whatever the code does, documentation can drift,
  and here the endpoints return JPA entities directly, which couples the API to the DB.
  
While code-first is more flexible, I would choose API-first if the API is going to be shared with other teams or systems. 
For internal APIs, code-first is enough
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would focus on the business rules first, because that's where bugs are expensive.
To keep it effective over time:
- CI runs build, unit tests, integration tests and coverage as separate jobs on
  every push and pull request, so it is clear what failed.
- JaCoCo fails the build below 80% line coverage per package (generated code
  excluded), so coverage can't silently drop.
- Coverage is a minimum, not the goal: every new rule or bug fix comes with a
  test for that specific case, including the negative path.
```