# ClothFlow — Master Continuation & Debugging Context

You are continuing my **ClothFlow** project. Treat the following as the authoritative project context.

## 1. Project Goal

ClothFlow is a serious production-style clothing-store backend being built for senior-level learning and interview preparation.

The goal is to learn and implement:

* Java 21
* Spring Boot
* Microservices
* PostgreSQL
* REST APIs
* Kafka
* Transactional Outbox
* Inbox/idempotency
* Saga
* Distributed transactions
* Concurrency
* Database locking
* Resilience/retries
* Observability
* Docker
* AWS
* Terraform
* ECS/Fargate
* ALB
* RDS
* MSK/Kafka
* CloudWatch
* Security and IAM
* Production deployment patterns

Architecture must remain **microservices from the beginning**. Do not suggest converting to a monolith or rebuilding completed services unnecessarily.

---

# 2. Current Microservices

Implementation order:

Product
→ Inventory
→ Order
→ Payment
→ Kafka/Outbox/Inbox hardening
→ cancellation/refund Saga
→ Shipping/Notification
→ Gateway
→ AWS hardening/observability/scaling

Current services:

* Product Service — 8081 local
* Inventory Service — 8082
* Order Service — 8083
* Payment Service — 8084

Databases:

* Product: `clothflow`
* Inventory: `clothflow_inventory`
* Order: `clothflow_order`
* Payment: `clothflow_payment`

Technology:

* Java 21
* Spring Boot 4.1.1
* PostgreSQL 16.x
* Flyway 12.4.0
* Kafka / Spring Kafka
* Docker
* Terraform

---

# 3. Local AWS Environment

I use:

* WSL/Bash
* FLOCI AWS emulator
* FLOCI version 2.0.1
* AWS CLI profile: `clothflow`
* Region: `us-east-1`
* Local ECR registry: `localhost:5100`

AWS CLI commands should use:

```bash
--profile clothflow
--region us-east-1
--endpoint-url http://localhost:4566
```

FLOCI Docker network:

```text
floci_default
```

Important FLOCI configuration:

```yaml
FLOCI_SERVICES_ECR_URI_STYLE: path
```

Therefore the local ECR URI format must be discovered rather than assumed.

Current path-style example:

```text
localhost:5100/000000000000/us-east-1/clothflow/payment-service:1.0.3
```

Do NOT blindly switch back to:

```text
000000000000.dkr.ecr.us-east-1.localhost:5100/...
```

---

# 4. Deployment Contract

Every service follows:

```text
Source Code
→ Maven Build
→ Docker Image
→ Local Registry
→ FLOCI ECR
→ ECS Task Definition
→ ECS/Fargate
→ ALB
→ HTTP API
```

Terraform is the source of truth for:

* ECR
* ECS
* ALB
* CloudWatch
* VPC
* Security Groups
* RDS
* Kafka/MSK
* IAM

Do not manually modify running ECS infrastructure unless explicitly debugging something temporarily.

Image tags are immutable and follow:

```text
MAJOR.MINOR.PATCH
```

Never reuse an existing immutable image tag.

Always verify both:

1. Docker Registry
2. ECR API

because:

```text
Registry = image physically exists
ECR API = FLOCI recognizes image
ECS = task can pull/run image
```

---

# 5. Current Deployment Status

The following are already working:

* Product ECS
* Inventory ECS
* Order ECS
* Payment ECS
* RDS
* Kafka/MSK
* ECR
* ALB
* CloudWatch
* Terraform
* Docker/FLOCI networking

Current deployed image versions include:

```text
Product     1.0.0
Inventory   1.0.0
Payment     1.0.3
Order       1.0.4
```

Payment and Order ECS startup issues were previously caused by using the wrong ECR URI style.

The issue was fixed by using FLOCI's path-style ECR URI.

Do not reopen that investigation unless new evidence indicates the problem has returned.

---

# 6. Important Networking Rule

Inside Docker/FLOCI network:

```text
floci
floci-ecr-registry
floci-msk-...
floci-rds-...
```

are reachable using Docker DNS.

For example:

```text
Order container
    ↓
floci-msk-5f9b53:9092
```

Kafka is NOT exposed to the WSL host through localhost.

Likewise, from a Docker container:

```text
http://floci
```

is the internal FLOCI router/ALB-style entry point.

The external ALB hostname is intended for host/client access.

---

# 7. Completed Product Service

Product Service is working.

It owns catalog information.

Order Service communicates with Product Service over HTTP and snapshots:

* productId
* SKU
* productName
* unitPrice

Do not introduce cross-database foreign keys between services.

---

# 8. Completed Inventory Service

Inventory Service is production-style enough to continue from.

It implements:

* quantity
* reservedQuantity
* availableQuantity
* pessimistic locking
* optimistic version
* reservations
* reservation lifecycle
* idempotency
* stock operation idempotency
* transactional outbox
* retries
* stale processing recovery
* metrics/actuator

Invariant:

```text
quantity >= 0
reservedQuantity >= 0
reservedQuantity <= quantity
availableQuantity = quantity - reservedQuantity
```

Reservation lifecycle:

```text
ACTIVE
  ↓
COMMITTED / RELEASED
```

Do not rebuild Inventory Service.

---

# 9. Completed Order Service

Order Service implements:

```text
PENDING
INVENTORY_RESERVED
PAYMENT_PENDING
CONFIRMED
PROCESSING
SHIPPED
DELIVERED
CANCELLED
INVENTORY_FAILED
PAYMENT_FAILED
```

It has:

* Order
* OrderItem
* OrderStatusHistory
* OrderInventoryReservation
* ProcessedEvent

It communicates with:

```text
Product → HTTP
Inventory → HTTP
Payment → HTTP
Kafka → asynchronous payment events
```

`spring.jpa.open-in-view=false` is intentional.

Do not solve lazy-loading issues by changing everything to EAGER.

Transaction boundaries are intentionally separated using services such as:

* OrderCreationPersistenceService
* OrderStatePersistenceService
* OrderReadService

because Spring `@Transactional` self-invocation previously caused problems.

---

# 10. Order + Payment Workflow

Current intended workflow:

```text
Create Order
    ↓
PENDING
    ↓
Reserve Inventory
    ↓
INVENTORY_RESERVED
    ↓
Payment
    ↓
PAYMENT_PENDING
    ↓
Payment Service
    ↓
PAYMENT_SUCCEEDED event
    ↓
Kafka
    ↓
Order Service
    ↓
CONFIRMED
```

Failure:

```text
Payment failed
    ↓
PAYMENT_FAILED
    ↓
release inventory
```

Successful payment must eventually result in:

```text
Inventory reservation committed
Order CONFIRMED
```

---

# 11. Payment Service

Payment Service has:

* payment state machine
* payment gateway abstraction
* fake deterministic gateway
* idempotency key
* PostgreSQL
* Flyway
* Kafka dependency
* transactional outbox

Payment states:

```text
INITIATED
PENDING
SUCCEEDED
FAILED
CANCELLED
REFUND_PENDING
REFUNDED
```

Payment API requires:

```text
Idempotency-Key
```

Payment gateway is currently fake.

Important limitation:

The fake gateway currently executes inside the DB transaction. This is acceptable for the learning baseline but should eventually be redesigned around asynchronous payment intent/callback concepts.

---

# 12. Kafka

Kafka topic:

```text
clothflow.payment.events
```

Current local Kafka container:

```text
floci-msk-5f9b53
```

Internal broker:

```text
floci-msk-5f9b53:9092
```

Topic currently has 3 partitions.

Spring Kafka is configured with:

```yaml
enable-auto-commit: false
```

and manual acknowledgement.

Order consumer receives:

```text
PaymentSucceededEvent
```

with:

```text
eventId
paymentId
orderId
customerId
amount
currency
paymentReference
paymentMethod
occurredAt
```

---

# 13. Inbox / Idempotency

Order Service has:

```text
processed_event
```

with:

```text
event_id PRIMARY KEY
event_type
aggregate_id
processed_at
```

Processing uses PostgreSQL:

```sql
ON CONFLICT (event_id)
DO NOTHING
```

This provides event-level idempotency.

Do not remove this simply because business-state checks also exist.

---

# 14. Transactional Outbox

Payment Service has:

```text
outbox_event
```

with:

```text
id
aggregate_type
aggregate_id
event_type
payload
status
retry_count
next_attempt_at
last_error
created_at
processed_at
```

States:

```text
PENDING
PROCESSING
PUBLISHED
FAILED
```

PostgreSQL JSONB is used for payload.

Spring Boot 4.1.1 uses Jackson 3 APIs:

```java
tools.jackson.databind.ObjectMapper
```

Do not replace these with old Jackson 2 imports unless there is a specific compatibility reason.

---

# 15. Current Next Development Task

The next planned work is:

## Payment Outbox Publisher

Goal:

```text
PENDING
   ↓
PROCESSING
   ↓
Kafka publish
   ↓
Kafka acknowledgement
   ↓
PUBLISHED
```

Failure:

```text
PROCESSING
   ↓
Kafka failure
   ↓
PENDING
   ↓
next_attempt_at
   ↓
retry
```

Also implement:

* stale PROCESSING recovery
* retry/backoff
* duplicate publishing considerations
* publisher concurrency
* safe transaction boundaries
* eventual improvement from holding DB transactions during external Kafka calls

The final design should avoid holding database locks while waiting for external network calls.

---

# 16. Debugging Rules — VERY IMPORTANT

When I report an error:

### DO NOT immediately rewrite code.

First:

1. Identify the exact failure layer.
2. Read the complete error.
3. Determine whether it is:

   * source code
   * compile
   * Spring startup
   * database
   * Flyway
   * Docker
   * networking
   * Kafka
   * ECR
   * ECS
   * ALB
   * Terraform
   * AWS/FLOCI emulator
4. Separate root cause from secondary errors.
5. Use the smallest diagnostic command that can prove/disprove the hypothesis.
6. Only change configuration/code after evidence supports it.
7. Preserve all already-working components.
8. Never restart the whole architecture unnecessarily.

When diagnosing deployment problems, follow:

```text
Application
↓
Container
↓
Docker network
↓
Registry
↓
ECR API
↓
ECS task definition
↓
ECS task
↓
ALB target
↓
HTTP API
```

Do not assume the problem is Spring Boot just because a service isn't reachable.

---

# 17. Debugging Response Format

When I give you an error, respond in this structure:

### 1. What failed

One sentence.

### 2. Evidence

Quote the important error/message.

### 3. Root cause

Explain the actual technical cause.

### 4. What is NOT broken

Explicitly protect already-working components.

### 5. One diagnostic step

Give me the exact command.

### 6. Expected result

Tell me what the output means.

### 7. Fix

Only after the evidence confirms the diagnosis.

### 8. Verification

Give me the exact command/API request to prove it works.

Do not give me 10 speculative fixes at once.

---

# 18. Commands

Whenever you give me an API request, ALWAYS provide the complete copy-paste `curl` command.

For example:

```bash
curl -i \
  -X GET \
  "http://localhost:8083/api/v1/orders/<ORDER_ID>"
```

Do not give only fragments such as:

```text
GET /api/v1/orders/{id}
```

For AWS commands always include:

```text
--profile clothflow
--region us-east-1
--endpoint-url http://localhost:4566
```

when applicable.

---

# 19. Learning Style

I am building this project specifically to understand senior-level concepts.

Do not just give me code.

For important implementation decisions, briefly explain:

* why we are doing it
* what production problem it solves
* what failure scenario it handles
* what tradeoff it introduces
* what interview/system-design concept it demonstrates

But avoid unnecessary theory when I am actively debugging.

---

# 20. Continuation Rule

I use:

```text
next
```

to move to the next implementation step.

When I say `next`:

* continue from the latest completed state
* do not restart
* do not repeat completed services
* do not redesign working architecture without evidence
* preserve existing IDs/configuration/conventions
* give me the next concrete implementation step

If something is uncertain, inspect the current evidence first rather than guessing.

## Golden Rule

**Diagnose → prove → change → verify.**

Never:

**guess → change everything → hope.**
