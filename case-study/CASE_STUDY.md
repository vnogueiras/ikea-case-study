# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. 
The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. 
Think about what are important considerations for this, what are previous experiences that you have you could 
related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

- **Cost Categories:**
Define first the cost categories (labor, inventory, transport etc.), which costs are direct and which are allocated. 
Allocation rules should be documented and versioned, so a report can always say which rule 
produced it.
- **Shared costs:**
One warehouse supplies different stores, one truck can deliver to several stores on the same route. How can we split the 
costs? By volume shipped or number of orders, for example?
- **Replacing Warehouses:**
Relating costs with warehouse businessUnitCode will mix the costs of an archived warehouse and an active one if they have 
the same businessUnitCode. Costs should point to specific warehouse by id.
- **Auditability:**
In order to explain the costs, corrections should be recorded as adjustments and not only overwrite previous values, so 
totals can always be rebuilt and audited.

**Related experience**

At Mambu (core banking) I worked with financial data where every transaction is an immutable, traceable entry and 
balances have to reconcile with other systems. The same principles apply here: append-only records, clear ownership of 
each number, and reconciliation with the finance system. At BlackRock, in fraud prevention, a big part of the work was 
linking data from different sources to the right entity and keeping a clear audit trail, which is the same matching 
problem as linking costs from payroll, carriers and ERP to the right warehouse or store.

**Questions**

1. What level of detail is needed: per warehouse/store per month, or down to order or product?
2. How does Finance allocate shared costs today, and is that the same across countries and business units?
3. How should costs be split when a warehouse is replaced in the middle of a period?
4. Is real-time tracking needed, or is a daily/monthly close enough? How are late invoices and corrections handled?
5. Which currencies, tax and audit requirements apply, and how long must the data be kept?
6. Who may see which costs? Labor costs can include sensitive salary data.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. 
The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. 
How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

**Possible strategies and expected outcomes**

- **Warehouse network and capacity.** Use the data the system already has (location limits, warehouse capacity vs. stock)
to find under-used warehouses and merge or replace them, as in the replace operation. *Outcome:* lower rent, energy and 
overhead per unit stored.
- **Transport.** Better route planning, fuller trucks, combined deliveries to stores close to each other, and supplying 
each store from the nearest warehouse with stock. *Outcome:* lower cost per delivery and fewer kilometres (also lower CO2).
- **Inventory levels.** Keep stock closer to real demand per store and season; move slow-moving items to cheaper, central 
storage. *Outcome:* less obsolete stock, more free capacity.
- **Labor planning.** Plan shifts on forecast volume instead of fixed schedules. *Outcome:* fewer overtime and idle 
hours, more orders per hour.
- **Automation and process.** Automate repetitive manual steps (data entry, reconciliation, reporting) and, where 
volume justifies it, warehouse automation. *Outcome:* fewer errors and lower cost per order, with a higher upfront investment.

**How I would identify, prioritize and implement**

1. **Identify with data, not guesses.** Start from the cost tracking in Scenario 1: cost per order, per pallet, 
per store delivery and per warehouse, compared across warehouses. Large differences between similar warehouses show 
where to look first.
2. **Set the service guardrails first.** Agree the service KPIs that must not get worse (on-time delivery to stores, 
stock availability, order accuracy, delivery lead time) and track them next to the cost KPIs. A saving that lowers 
availability in stores is not a saving.
3. **Prioritize by impact vs. effort and risk.** Quick wins with low risk first (contracts, truck fill rate, shift 
planning), larger changes (network changes, automation) later with a business case.
4. **Implement in small steps.** Pilot in one warehouse or region, compare against a similar warehouse without the 
change, then roll out. Every change should be easy to stop if service KPIs drop.
5. **Measure and review.** Dashboards with cost and service KPIs together, reviewed regularly, so savings are proven 
and not only estimated.

**Related experience**

In my previous roles, performance and cost improvements always started with measuring before changing anything: 
profiling and monitoring first, then fixing the biggest bottleneck, and rolling out behind feature flags or in 
stages so it could be reverted. The same approach fits here: baseline, pilot, compare, roll out. In CI/CD and cloud 
work I also saw how automating repetitive manual steps reduced both cost and errors, which is the same idea as automating 
manual fulfilment processes.

**Questions**

1. What is the main goal: lower total cost, lower cost per order, or freeing budget for growth? Is there a target?
2. Which service KPIs define "quality" for the business, and what are their minimum levels?
3. Which costs are the biggest today (labor, transport, inventory), and which are actually controllable?
4. Do we already have reliable cost and service data per warehouse and store, or does that need to be built first (Scenario 1)?
5. Is there budget for investment (e.g. automation), and what payback period is acceptable?

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely 
cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company 
would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

**Why the integration matters and the benefits**

- **One source of truth.** Without integration, operations and finance work with different numbers and spend time 
reconciling spreadsheets. With it, the costs in the Cost Control Tool are the same costs that end up in the books.
- **Timely decisions.** Operations can see cost deviations during the month (e.g. a warehouse spending more on overtime) 
instead of after the month-end close, when it's too late to act.
- **Fewer manual errors and less work.** No re-typing of invoices or exports between systems, and a faster month-end close.
- **Better budgeting and forecasting.** Actual costs flow straight into the comparison with budgets and forecasts.
- **Compliance and audit.** Every cost can be traced from the report back to the original invoice or payroll entry.

**How I would ensure seamless integration and data synchronization**

- **Clear ownership of data.** Decide per data type which system is the master. For example, the financial system owns 
the chart of accounts, cost centres and booked amounts; the Cost Control Tool owns the operational data (warehouses, 
stores, Business Unit Codes) and the allocation. Map Business Unit Codes to the finance cost centres explicitly.
- **Event-driven, near real-time sync.** Publish changes as events (e.g. "warehouse replaced", "cost recorded") through 
a message broker or the systems' APIs instead of nightly file exports. Only send data that is committed, the same principle 
as the Store / legacy system task in the code assignment, and use an outbox or retry mechanism so no event is lost if the 
other system is down.
- **Idempotent and ordered processing.** Every message has a unique id, so a retried message is not booked twice. 
Corrections are new entries (reversal + new booking), never silent updates.
- **Reconciliation.** Real-time sync is not enough on its own: a scheduled job compares totals per period, cost center
and category between both systems and raises an alert on differences. This catches what the events missed.
- **Monitoring and error handling.** Dashboards and alerts for failed or delayed messages, a dead-letter queue for 
messages that can't be processed, and a way to replay them.
- **Security.** Financial data needs authentication between systems, encryption, least-privilege access and an audit log 
of who changed what.
- **Gradual rollout.** Run the new integration in parallel with the current process for one or two periods and compare 
results before switching over.

**Questions**

1. Which financial systems are involved (ERP, payroll, accounts payable, carrier billing), and what interfaces do they 
offer: APIs, events, or only file exports?
2. What does "real-time" mean for the business: seconds, minutes, or is daily enough for most reports?
3. Which system is the source of truth for each data type, and how do Business Unit Codes map to cost centers?
4. Is the data flow one-way (finance > tool) or two-way (the tool also sends allocations back to finance)?
5. How are corrections, late invoices and period closing handled in the financial system? Can closed periods still change?
6. What are the audit, security and data retention requirements?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. 
The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into 
account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

**Why budgeting and forecasting matter**

- **Fulfilment costs move with volume and season.** Peaks (back to school, holidays, sales campaigns) need extra staff, 
trucks and storage. Without a forecast, the company either pays for capacity it doesn't use or misses deliveries when it 
runs short.
- **Resources need time to arrange.** Hiring temporary staff, booking carriers or renting extra space has lead time. 
A forecast lets operations plan these instead of paying premium prices at the last minute.
- **Control during the year.** A budget is the baseline to compare actual costs against. Large deviations show early 
where something is going wrong (or a forecast assumption was wrong).
- **Investment decisions.** Opening, closing or replacing a warehouse or investing in automation needs a 
forecast of future volumes and costs to build the business case.

**What I would take into account designing the system**

- **Built on actual cost data.** Forecasts are only as good as the history behind them, so the system should use the 
same cost data and Business Unit Codes as cost tracking and the actuals from the financial integration.
- **Driver-based forecasting.** Forecast the drivers (orders, volume shipped, number of stores served) 
and multiply by the cost per unit, instead of just extrapolating last year's totals. That makes the forecast explainable 
("costs rise 8% because volume rises 10%") and easy to adjust when plans change.
- **Seasonality and events.** Account for seasonal patterns, campaigns, new store openings and warehouse changes, 
not only trends.
- **Budget vs. forecast vs. actual.** Keep the approved budget fixed, update the forecast regularly (rolling forecast,
e.g. monthly), and always show both next to actuals with the variance.
- **Right level of detail.** Per warehouse/store, cost category and month is usually enough for planning; 
more detail adds noise and effort.
- **Accuracy tracking.** Measure how close previous forecasts were to actuals and use that to improve the model. 
Start simple (history + drivers) and only add statistical or ML models where they prove more accurate.
- **Alerts.** Notify the owner when actual costs deviate from budget beyond an agreed threshold, so they can act during 
the period.

**Questions**

1. What is the planning cycle today (yearly budget, quarterly or monthly reforecast), and which tools are used? 
Probably spreadsheets?
2. Who creates, reviews and approves budgets, and at what level (warehouse, region, country)?
3. How much historical data is available, and how reliable is it?
4. Which demand forecasts already exist (sales, store orders) that we can use as input?
5. How far ahead should forecasts go, and how accurate do they need to be in order to be useful?
6. Which events must be planned explicitly: new stores, warehouse replacements, campaigns, contract changes?
7. What should happen when actuals go over budget: only alerts, or approval needed for extra spending?

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the 
Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how 
this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

**Cost control aspects of replacing a Warehouse**

- **Business case before the replacement.** A replacement is an investment: the new warehouse should cost less per unit, 
or handle more volume for a similar cost. The old warehouse's cost history is the baseline that justifies the decision 
and sets the target for the new one.
- **One-off transition costs vs. running costs.** Moving stock, a period where both warehouses are paid for (double rent, 
staff), setup, IT and training are one-off costs. They need their own budget and must be tracked separately, so they don't 
make the new warehouse look expensive in its first months, or hide that it really is.
- **Continuity of service and stock.** The replace rules in the code assignment (the new warehouse must hold the same 
stock and have capacity for it) prevent stock loss during the switch. Lost or damaged stock is a hidden cost of a 
replacement, so this also matters financially.
- **Clean cut-over date.** Costs before the switch belong to the old warehouse, costs after it to the new one. 
Late invoices for the old warehouse (energy, carriers) still arrive after it's archived and must be booked to it, 
not to the new one.

**Why preserving cost history is important**

- **Comparison and learning.** Only with the history can we answer "is the new warehouse actually cheaper per order 
than the old one?" and use the old warehouse's patterns (seasonality, peaks) to budget the new one.
- **Continuity of the business unit.** The Business Unit Code is reused, so reports per business unit show a continuous 
history over the years, while each warehouse keeps its own costs. In the implementation, the replaced warehouse is 
archived (`archivedAt` set) and not deleted, and the new one is a separate record with its own id. Costs should be 
linked to the specific warehouse record (and period), not only to the Business Unit Code, so both views are possible.
- **Audit and compliance.** Financial records must be kept for years; deleting or overwriting the old warehouse would 
break the audit trail and the link to its invoices.
- **Late and corrected costs.** Costs for the old warehouse keep arriving after the replacement, so it must still exist 
as a target for them.

**How this relates to keeping the new Warehouse within budget**

- The budget for the new warehouse is built from the old warehouse's actual costs, adjusted with the expected changes 
(capacity, location, automation, rent).
- From day one, actual costs of the new warehouse are compared with that budget and with the old warehouse's cost 
per unit for the same period last year, with alerts on deviations.
- A ramp-up period with its own expectations: efficiency is usually lower in the first months, and that should be planned, 
not discovered.
- A review after e.g. 6 and 12 months: did the replacement deliver the expected savings? That feedback improves the next 
replacement decision.

**Questions**

1. What is the expected benefit of the replacement (lower cost, more capacity, better location), and how will success be 
measured?
2. Is there a transition period where both warehouses operate, and how should costs be split during it?
3. How long must the archived warehouse's cost history be kept, and who needs access to it?
4. What happens with contracts linked to the old warehouse (lease, carriers, suppliers)? Are there exit costs?

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
