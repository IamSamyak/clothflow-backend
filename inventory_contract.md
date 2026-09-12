# ClothFlow — Inventory Service Contract

## Service

**Name:** `inventory-service`
**Port:** `8082`
**Database:** `clothflow_inventory`

**Status:** COMPLETE ✅

### Technology

* Java 21
* Spring Boot 4.1.1
* PostgreSQL 16
* Flyway 12.4.0
* Spring Data JPA
* Spring Kafka
* Micrometer
* Actuator
* Prometheus

---

# 1. Inventory Service Responsibility

Inventory Service manages inventory for products.

It provides:

* Inventory creation
* Inventory lookup
* Stock addition
* Stock removal
* Stock reservation
* Reservation commitment
* Reservation release

---

# 2. Inventory Data

Each inventory record contains:

```text
id
productId
quantity
reservedQuantity
availableQuantity
version
createdAt
updatedAt
```

Available quantity is calculated as:

```text
availableQuantity = quantity - reservedQuantity
```

Inventory maintains the following invariants:

```text
quantity >= 0
reservedQuantity >= 0
reservedQuantity <= quantity
```

---

# 3. REST API

Base path:

```text
/api/v1/inventory
```

## Create Inventory

```http
POST /api/v1/inventory
```

Request:

```json
{
  "productId": "UUID",
  "quantity": 100
}
```

Response:

```json
{
  "id": "UUID",
  "productId": "UUID",
  "quantity": 100,
  "reservedQuantity": 0,
  "availableQuantity": 100,
  "version": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## Get Inventory

```http
GET /api/v1/inventory/{productId}
```

Returns the current inventory state for the product.

---

## Add Stock

```http
POST /api/v1/inventory/{productId}/stock/add
```

Request:

```json
{
  "operationId": "UUID",
  "quantity": 20
}
```

Adds the specified quantity to inventory.

---

## Remove Stock

```http
POST /api/v1/inventory/{productId}/stock/remove
```

Request:

```json
{
  "operationId": "UUID",
  "quantity": 20
}
```

Removes the specified quantity from available inventory.

If insufficient stock exists, the operation fails.

---

## Reserve Stock

```http
POST /api/v1/inventory/{productId}/reservations
```

Request:

```json
{
  "reservationId": "UUID",
  "quantity": 2
}
```

Creates an inventory reservation.

A successful reservation:

```text
quantity           unchanged
reservedQuantity   + requested quantity
availableQuantity  - requested quantity
```

---

## Commit Reservation

```http
POST /api/v1/inventory/{productId}/reservations/commit
```

Request:

```json
{
  "reservationId": "UUID",
  "quantity": 2
}
```

Commits an active reservation.

A successful commit:

```text
quantity           - reservation quantity
reservedQuantity   - reservation quantity
```

---

## Release Reservation

```http
POST /api/v1/inventory/{productId}/reservations/release
```

Request:

```json
{
  "reservationId": "UUID",
  "quantity": 2
}
```

Releases an active reservation.

A successful release:

```text
quantity           unchanged
reservedQuantity   - reservation quantity
availableQuantity  + reservation quantity
```

---

# 4. Reservation States

Reservations have three states:

```text
ACTIVE
RELEASED
COMMITTED
```

Supported lifecycle:

```text
ACTIVE
   │
   ├──→ COMMITTED
   │
   └──→ RELEASED
```

Repeated commit of an already committed reservation is idempotent.

Repeated release of an already released reservation is idempotent.

Invalid state transitions are rejected.

---

# 5. Idempotency

Stock operations use:

```text
operationId
```

Reservations use:

```text
reservationId
```

Both identifiers are unique.

Repeated requests using the same identifier do not perform the business operation twice.

---

# 6. Concurrency Guarantees

Inventory uses database-level concurrency control.

Critical inventory updates use pessimistic row locking.

Inventory also maintains an optimistic locking version:

```text
version
```

The service is designed to prevent concurrent operations from incorrectly overselling inventory.

---

# 7. Error Responses

### Inventory not found

```text
404 NOT FOUND
```

### Insufficient stock

```text
409 CONFLICT
```

### Invalid inventory operation

```text
409 CONFLICT
```

### Concurrency conflict

```text
409 CONFLICT
```

### Validation error

```text
400 BAD REQUEST
```

---

# 8. Events

Inventory Service produces inventory-related events through its transactional Outbox.

Defined event types:

```text
INVENTORY_RESERVED
INVENTORY_RELEASED
INVENTORY_COMMITTED
STOCK_ADDED
STOCK_REMOVED
```

---

# 9. Inventory Reserved Event

Event model:

```text
InventoryReservedEvent
```

Payload:

```json
{
  "eventId": "UUID",
  "reservationId": "UUID",
  "productId": "UUID",
  "quantity": 2,
  "occurredAt": "..."
}
```

Event type:

```text
INVENTORY_RESERVED
```

---

# 10. Event Delivery

Inventory events are published using a Transactional Outbox.

Outbox processing supports:

```text
PENDING
PROCESSING
PUBLISHED
FAILED
```

The publisher supports:

* Retry
* Exponential backoff
* Jitter
* Maximum retry attempts
* Failed-event state
* Stale processing recovery

Event delivery should be considered **at-least-once**.

Therefore, duplicate event delivery is possible.

---

# 11. Database Ownership

Inventory Service owns:

```text
clothflow_inventory
```

Inventory database tables include:

```text
inventory
inventory_reservation
inventory_stock_operation
outbox_event
```

These tables are internal to Inventory Service.

---

# 12. Observability

Inventory Service exposes:

```text
/actuator/health
/actuator/prometheus
```

Custom business metrics include:

```text
inventory.reservations.created
inventory.reservations.released
inventory.reservations.committed
inventory.stock.insufficient
inventory.stock.adjustments
```

---

# 13. Service Status

Inventory Service is complete and tested.

Current status:

```text
Inventory Service
       │
       ├── REST APIs          ✅
       ├── Stock management   ✅
       ├── Reservations       ✅
       ├── Idempotency        ✅
       ├── Concurrency        ✅
       ├── Transactions       ✅
       ├── Outbox             ✅
       ├── Kafka events       ✅
       ├── Retry/recovery     ✅
       ├── Observability      ✅
       └── Tests              ✅
```

**All Inventory Service tests pass.**
