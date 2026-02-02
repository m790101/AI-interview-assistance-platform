# Input Data
Please analyze the following resume content and provide a deep audit report with reference to the [Technical Optimization Benchmarks].

## Candidate Resume
---Resume Content Start---
{resumeText}
---Resume Content End---

## Technical Optimization Benchmarks (Reference Standards)
When proposing optimization suggestions, ensure you benchmark against the following high-standard scenarios and expression logic:

### High Concurrency & Cache Optimization
| Technical Scenario | Reference Expression |
|-------------------|---------------------|
| Multi-level Caching | Redis + Caffeine two-level cache architecture to solve breakdown/penetration/avalanche, supporting 30w+ QPS |
| Atomic Operations | Redis Lua scripts to implement distributed token bucket rate limiting or atomic inventory reduction |

### Asynchronous & Performance Tuning
| Technical Scenario | Reference Expression |
|-------------------|---------------------|
| Async Orchestration | `CompletableFuture` for multi-source RPC call orchestration, reducing RT from seconds to milliseconds |
| Thread Governance | Dynamic thread pool parameter monitoring and adjustment, resolving deadlocks caused by parent-child task thread pool isolation |

### Microservices Architecture & Data Consistency
| Technical Scenario | Reference Expression |
|-------------------|---------------------|
| Data Synchronization | Canal + RabbitMQ/RocketMQ for real-time synchronization of MySQL incremental data to Elasticsearch |
| Distributed Transactions | Message queue (delayed messages) based implementation for order timeout closure or eventual data consistency |
| Gateway & Security | Spring Cloud Gateway + Spring Security OAuth2 + JWT + RBAC for dynamic permission control |

### Complex Business Modeling & Design Patterns
| Technical Scenario | Reference Expression |
|-------------------|---------------------|
| DDD (Domain-Driven Design) | Abstracting domain models, utilizing Factory, Strategy, and Template Method patterns to build business chains |
| Rule Engines | Responsibility Chain pattern for pre-validation, Combination pattern + Decision Tree to support complex business logic |
| State Management | Spring State Machine to manage complex business transitions (e.g., order status), ensuring idempotency |

### Stability & Big Data Processing
| Technical Scenario | Reference Expression |
|-------------------|---------------------|
| Full-link Governance | Sentinel for rate limiting/circuit breaking, SkyWalking for trace tracking, MAT analysis of Dumps to locate memory leaks |
| Sharding | ShardingSphere composite sharding algorithms to solve query performance bottlenecks under billion-level data volumes |
| Batch Processing | EasyExcel + MyBatis Batch + Task Table Asynchronization to optimize million-level data import/export |

## Audit Dimensions
| Dimension | Evaluation Criteria |
|-----------|-------------------|
| Technical Depth | Whether underlying principles are reflected (e.g., lock mechanisms, index optimization, concurrency models) |
| Business Value | Whether it describes how technology solves business pain points (e.g., overselling, lag, latency) |
| Quantified Results | Presence of clear performance indicators (RT, QPS, throughput, delivery cycles) |

## Output Requirements
Please output the analysis results strictly in JSON format. Output the JSON object directly without including any Markdown code block tags.