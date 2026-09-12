Absolutely. Since **Product Service is effectively frozen** and we don't want to introduce unnecessary changes, here's the equivalent **Product Service Contract** in the same format as your Inventory contract.

# ClothFlow — Product Service Contract

## Service

**Name:** `product-service`

**Port:** `8081`

**Database:** `clothflow`

**Status:** COMPLETE ✅

### Technology

* Java 21
* Spring Boot 4.1.1
* PostgreSQL 16
* Flyway 12.4.0
* Spring Data JPA
* Spring Validation
* Spring Actuator
* Micrometer

---

# 1. Product Service Responsibility

Product Service manages the **product catalog**.

It provides:

* Product creation
* Product lookup
* Product search
* Product update
* Product deletion
* SKU management
* Product catalog lifecycle

Product Service **does not manage inventory**.

Inventory-related fields such as:

```text
quantity
reservedQuantity
availableQuantity
```

belong exclusively to Inventory Service.

---

# 2. Product Data

Each product record contains:

```text
id
name
description
price
sku
version
deleted
createdAt
updatedAt
```

Product identity is represented using:

```text
UUID
```

SKU is unique across the product catalog.

---

# 3. REST API

Base path:

```text
/api/v1/products
```

---

## Create Product

```http
POST /api/v1/products
```

Request:

```json
{
  "name": "Red Cotton Kurti",
  "description": "Women's cotton kurti",
  "price": 899.00,
  "sku": "KURTI-RED-M"
}
```

Response:

```json
{
  "id": "UUID",
  "name": "Red Cotton Kurti",
  "description": "Women's cotton kurti",
  "price": 899.00,
  "sku": "KURTI-RED-M",
  "version": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

# 4. Get Product

```http
GET /api/v1/products/{productId}
```

Returns the product if it exists and has not been soft-deleted.

Example response:

```json
{
  "id": "e197ce1d-60f0-4efc-a9dd-b9241dce1e40",
  "name": "Red Cotton Kurti",
  "description": "Women's cotton kurti",
  "price": 899.00,
  "sku": "KURTI-RED-M",
  "version": 0,
  "createdAt": "2026-09-06T11:49:27.82052264",
  "updatedAt": "2026-09-06T11:49:27.82052264"
}
```

---

# 5. Search Products

```http
GET /api/v1/products
```

Supported filters:

```text
name
minPrice
maxPrice
```

Pagination is supported:

```text
page
size
sort
```

Example:

```http
GET /api/v1/products?name=kurti&minPrice=500&maxPrice=1500&page=0&size=20
```

The service limits page size to prevent excessively large queries.

Maximum page size:

```text
100
```

Response:

```json
{
  "content": [
    {
      "id": "UUID",
      "name": "Red Cotton Kurti",
      "description": "Women's cotton kurti",
      "price": 899.00,
      "sku": "KURTI-RED-M",
      "version": 0,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

# 6. Update Product

```http
PUT /api/v1/products/{productId}
```

Request:

```json
{
  "name": "Red Cotton Kurti",
  "description": "Premium women's cotton kurti",
  "price": 949.00,
  "sku": "KURTI-RED-M",
  "version": 0
}
```

The client must provide the product's current:

```text
version
```

The service uses optimistic locking to prevent lost updates.

Successful update:

```text
version → version + 1
```

For example:

```text
version = 0
        ↓
update
        ↓
version = 1
```

---

# 7. Optimistic Locking

Product Service uses:

```java
@Version
private Long version;
```

This protects product updates from concurrent modifications.

Example:

```text
Request A reads version 5
Request B reads version 5

Request A updates
version → 6

Request B attempts update using version 5
        ↓
CONFLICT
```

The second update is rejected instead of silently overwriting the first update.

---

# 8. SKU Uniqueness

Every product must have a unique SKU.

Example:

```text
KURTI-RED-M
```

cannot belong to two products.

Duplicate SKU results in:

```text
409 CONFLICT
```

This is protected at two levels:

```text
Application validation
        +
Database unique constraint
```

The database remains the final protection against race conditions.

---

# 9. Product Deletion

Product deletion uses **soft delete**.

```http
DELETE /api/v1/products/{productId}?version=1
```

The product is not physically removed from the database.

Instead:

```text
deleted = true
```

Deleted products are excluded from normal product lookup and search operations.

This preserves historical product references required by other services.

For example:

```text
Order
  ↓
order_item
  ↓
productId + SKU + productName + unitPrice
```

An old order remains valid even if the product is later removed from the catalog.

---

# 10. Product Snapshot Responsibility

Product Service owns the current catalog information.

When Order Service creates an order, it retrieves product information from Product Service.

Example:

```text
Order Service
      │
      │ GET /api/v1/products/{productId}
      ↓
Product Service
      │
      ↓
Product DB
```

Order Service then snapshots:

```text
productId
sku
productName
unitPrice
```

into its own `order_items` table.

Product Service does **not** access Order DB.

Order Service does **not** access Product DB directly.

---

# 11. Database Ownership

Product Service exclusively owns:

```text
clothflow
```

Its primary table:

```text
products
```

Product Service owns the schema and migrations for this database.

Other services must not directly query:

```text
products
```

Instead they communicate through:

```text
Product Service REST API
```

and, later where appropriate, asynchronous events.

---

# 12. Database Constraints

The Product database enforces important invariants.

### Required fields

```text
name       NOT NULL
price      NOT NULL
sku        NOT NULL
version    NOT NULL
deleted    NOT NULL
created_at NOT NULL
updated_at NOT NULL
```

### Price

```text
price >= 0.01
```

### SKU

```text
UNIQUE
```

These constraints provide a second layer of protection in addition to application validation.

---

# 13. Validation

Create and update requests validate:

```text
name
description
price
sku
version
```

Examples:

```text
name → required
name → maximum 255 characters

price → required
price → minimum 0.01

sku → required
sku → maximum 100 characters

version → required for updates
```

Invalid requests return:

```text
400 BAD REQUEST
```

---

# 14. Error Responses

### Product not found

```text
404 NOT FOUND
```

### Duplicate SKU

```text
409 CONFLICT
```

### Optimistic locking conflict

```text
409 CONFLICT
```

### Database constraint violation

```text
409 CONFLICT
```

### Validation error

```text
400 BAD REQUEST
```

### Invalid request

```text
400 BAD REQUEST
```

Errors are returned through a consistent API error structure containing information such as:

```text
timestamp
status
error
message
path
```

---

# 15. Transactions

Product write operations execute inside transactional boundaries.

Examples:

```text
Create Product
      ↓
Validate
      ↓
Persist Product
      ↓
Commit transaction
```

Update:

```text
Read Product
      ↓
Validate version
      ↓
Modify Product
      ↓
Persist
      ↓
Commit transaction
```

This ensures product modifications are atomic.

---

# 16. Product Lifecycle

Current lifecycle is represented through the soft-delete flag:

```text
ACTIVE
  │
  │ DELETE
  ↓
DELETED
```

Deleted products cannot normally be retrieved or searched.

---

# 17. Pagination & Query Protection

Product search supports pagination.

The service validates requested sorting and limits page size.

Maximum:

```text
size = 100
```

This prevents clients from requesting unbounded result sets.

Indexes support common catalog queries such as:

```text
name
deleted/active state
createdAt
```

---

# 18. Concurrency Guarantees

Product updates use:

```text
Optimistic Locking
```

SKU uniqueness uses:

```text
Database Unique Constraint
```

Therefore:

```text
Concurrent product updates
        ↓
@Version
        ↓
Conflict detected
```

and:

```text
Concurrent SKU creation
        ↓
Database UNIQUE constraint
        ↓
Duplicate rejected
```

---

# 19. Observability

Product Service exposes:

```text
/actuator/health
/actuator/info
```

The service is designed to integrate with the broader ClothFlow observability stack.

Later:

```text
Product Service
      ↓
Micrometer
      ↓
Prometheus
      ↓
Grafana
```

---

# 20. Service-to-Service Contract

Product Service is consumed by other services without sharing its database.

For example, Order Service uses:

```http
GET /api/v1/products/{productId}
```

to retrieve:

```text
productId
name
description
price
sku
version
createdAt
updatedAt
```

Product Service does **not** expose inventory information.

---

# 21. Architecture Boundary

The ownership boundary is:

```text
                 Product Service
                       │
                       ▼
                 ┌───────────┐
                 │ Product DB│
                 └───────────┘
                       │
          ┌────────────┴────────────┐
          │                         │
     Catalog Data              Product Identity
          │                         │
 name / SKU / price             productId
 description
```

Inventory is separate:

```text
Product Service              Inventory Service
      │                             │
      ▼                             ▼
 Product DB                   Inventory DB
      │                             │
 name                          quantity
 SKU                           reservedQuantity
 price                         availableQuantity
```

There is **no shared database**.

---

# 22. Product Service Status

```text
Product Service

       │
       ├── REST APIs             ✅
       ├── Product CRUD          ✅
       ├── Product Search        ✅
       ├── Pagination            ✅
       ├── Validation            ✅
       ├── SKU uniqueness        ✅
       ├── Soft delete           ✅
       ├── Optimistic locking    ✅
       ├── Transactions          ✅
       ├── DB constraints        ✅
       ├── Error handling        ✅
       ├── Database ownership    ✅
       └── Tests                 ✅
```

### Current decision

**Product Service = LOCKED ✅**

We don't need to make it perfect before moving forward. Any non-breaking improvements can be handled later during hardening.

Our next implementation target should therefore be:

```text
                 ORDER SERVICE
                      │
                      │ REST
                      ▼
                PRODUCT SERVICE
                    :8081
                      │
                      ▼
                  Product DB
```

Then we move to:

```text
Order
  ↓
Product lookup
  ↓
Create order + snapshot product
  ↓
Inventory reservation
```

That keeps us aligned with the original **Product → Inventory → Order** architecture while now building the actual **Order orchestration** layer.
