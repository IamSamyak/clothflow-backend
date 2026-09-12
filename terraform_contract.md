Yes — you want the **Terraform / Infrastructure Contract** in the same format as the Inventory Service contract, so that we have a fixed reference for what our infrastructure is supposed to provide.

Here is the contract I would use for ClothFlow.

# ClothFlow — Terraform Infrastructure Contract

## Infrastructure

**Name:** `clothflow-infrastructure`

**IaC:** Terraform

**Cloud:** AWS-compatible infrastructure via FLOCI locally

**AWS Region:** `us-east-1`

**Environment:** `local`

**Status:** INFRASTRUCTURE COMPLETE ✅

### Technology

* Terraform
* AWS VPC
* AWS ECS
* AWS ECR
* AWS ALB
* AWS RDS PostgreSQL
* AWS IAM
* AWS CloudWatch
* FLOCI 2.0.1

---

# 1. Infrastructure Responsibility

Terraform manages the infrastructure required to run ClothFlow microservices.

It provides:

* Network isolation
* Public and private subnets
* Internet connectivity
* Application Load Balancer
* ECS cluster
* ECS execution IAM role
* ECS task IAM role
* ECR repositories
* PostgreSQL RDS
* Security groups
* CloudWatch log groups
* Service-to-database network access

Terraform is responsible for **infrastructure**, not application business logic.

---

# 2. AWS Region

All resources are created in:

```text
us-east-1
```

AWS CLI profile:

```text
clothflow
```

Local AWS endpoint:

```text
http://localhost:4566
```

---

# 3. VPC

VPC:

```text
clothflow-vpc
```

CIDR:

```text
10.20.0.0/16
```

The VPC provides the isolated network boundary for ClothFlow.

---

# 4. Availability Zones

The infrastructure is designed across two Availability Zones:

```text
us-east-1a
us-east-1b
```

This provides the foundation for future high-availability deployment.

---

# 5. Subnets

The VPC contains:

### Public Subnets

Used for internet-facing infrastructure such as the ALB.

```text
clothflow-public-subnet
```

### Private Subnets

Used for application and database resources.

```text
clothflow-private-subnet
```

Application services should not be directly exposed to the public internet.

---

# 6. Network Architecture

Target architecture:

```text
                    Internet
                       │
                       ▼
              ┌─────────────────┐
              │       ALB       │
              │ Public Subnet   │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   ECS Service   │
              │ Private Subnet │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │   RDS Postgres  │
              │ Private Subnet │
              └─────────────────┘
```

---

# 7. Security Groups

Terraform manages separate network security boundaries.

### ALB Security Group

Allows:

```text
Internet
   ↓
HTTP/HTTPS
   ↓
ALB
```

### Application Security Group

Allows traffic:

```text
ALB
 ↓
ECS application
```

Application containers should not accept arbitrary internet traffic.

### Database Security Group

Allows:

```text
ECS application
       ↓
PostgreSQL :5432
       ↓
RDS
```

The database should not be publicly accessible.

---

# 8. Application Load Balancer

Terraform creates:

```text
clothflow-alb
```

The ALB is internet-facing.

Current FLOCI output:

```text
clothflow-alb-e5302c054365441a.elb.localhost.floci.io
```

The ALB acts as the entry point for ClothFlow APIs.

Future routing:

```text
/api/v1/products/*
        ↓
Product Service

/api/v1/inventory/*
        ↓
Inventory Service

/api/v1/orders/*
        ↓
Order Service
```

---

# 9. ECS Cluster

Terraform creates:

```text
clothflow-cluster
```

ECS is responsible for running containerized ClothFlow microservices.

Target architecture:

```text
ECS Cluster
│
├── Product Service
│
├── Inventory Service
│
├── Order Service
│
└── Future Services
```

Each microservice should be independently deployable.

---

# 10. ECS Task Execution Role

Terraform creates an IAM role for ECS task execution.

Responsibilities include access required for:

* Pulling container images
* Writing CloudWatch logs
* Accessing required AWS services

The application should not receive unnecessary AWS permissions.

---

# 11. ECS Task Role

Application containers receive a separate task IAM role.

This follows the principle:

```text
ECS Execution Role
        ≠
Application Task Role
```

The execution role handles infrastructure-level container operations.

The task role handles permissions required by the application itself.

---

# 12. ECR

Terraform manages private ECR repositories.

Current repository:

```text
clothflow/product-service
```

Repository URI returned by FLOCI:

```text
000000000000.dkr.ecr.us-east-1.localhost:5100/clothflow/product-service
```

### Local FLOCI path-style registry

Because:

```text
FLOCI_SERVICES_ECR_URI_STYLE=path
```

Docker images are stored through:

```text
localhost:5100/
000000000000/
us-east-1/
clothflow/
product-service
```

Current verified image:

```text
product-service:1.0.0
```

Digest:

```text
sha256:3481855e305c8b0559b760497d7626d15559802f47fcd1679c9d91337c2021a7
```

ECR verification:

```text
aws ecr describe-images
        ↓
imageDetails
        ↓
1.0.0
        ↓
SUCCESS ✅
```

---

# 13. RDS PostgreSQL

Terraform manages PostgreSQL database infrastructure.

Database engine:

```text
PostgreSQL
```

Version:

```text
PostgreSQL 16
```

Primary database:

```text
clothflow
```

Database user:

```text
clothflow_admin
```

Database port:

```text
5432
```

The database is intended to reside in the private network.

---

# 14. Database Ownership

Microservices follow database ownership principles.

Target architecture:

```text
Product Service
      ↓
Product Database

Inventory Service
      ↓
Inventory Database

Order Service
      ↓
Order Database
```

Services must not directly modify another service's database tables.

For example:

```text
Order Service
      ❌
      ↓
Inventory DB
```

Instead:

```text
Order Service
      ↓
Inventory API / Events
      ↓
Inventory Service
      ↓
Inventory DB
```

---

# 15. CloudWatch

Terraform creates CloudWatch log groups for ECS workloads.

Application logs should flow:

```text
Spring Boot
     ↓
Docker
     ↓
ECS
     ↓
CloudWatch Logs
```

This provides centralized application logging.

---

# 16. Infrastructure Outputs

Terraform exposes important infrastructure values as outputs.

Examples:

```text
vpc_id

public_subnet_ids

private_subnet_ids

alb_dns_name

ecs_cluster_name

product_service_repository_url

db_endpoint

db_name

db_port

db_username
```

Application deployment should consume these outputs rather than hardcoding infrastructure IDs.

---

# 17. Environment Configuration

Infrastructure-specific configuration is managed through Terraform variables.

Examples:

```text
aws_region
environment
vpc_cidr
availability_zones
database_name
database_username
database_port
```

Secrets such as database passwords should **not** be committed directly into Terraform source code.

Future production architecture should use:

```text
AWS Secrets Manager
```

or an equivalent secret-management system.

---

# 18. Terraform Modules

Infrastructure is organized into modules.

Current structure:

```text
terraform/
│
├── modules/
│   ├── vpc/
│   ├── alb/
│   ├── ecr/
│   ├── rds/
│   └── ecs/
│
├── main.tf
├── variables.tf
├── outputs.tf
└── terraform.tfvars
```

Each module owns a specific infrastructure concern.

---

# 19. Terraform Principles

Terraform must provide:

### Declarative Infrastructure

We describe:

```text
WHAT infrastructure should exist
```

rather than manually executing:

```text
HOW to create it
```

### Idempotency

Running:

```bash
terraform apply
```

multiple times should converge on the same infrastructure state.

### Dependency Management

Terraform determines dependencies such as:

```text
VPC
 ↓
Subnets
 ↓
Security Groups
 ↓
RDS / ECS / ALB
```

### State Management

Terraform maintains infrastructure state so it knows:

```text
What exists
What changed
What must be created
What must be destroyed
```

---

# 20. Infrastructure Security Principles

The infrastructure follows:

```text
Least Privilege
Private Database
Private Application Services
Public ALB Only
Explicit Security Group Rules
IAM Role Separation
No Hardcoded Secrets
```

---

# 21. Deployment Flow

The intended deployment pipeline is:

```text
Developer
   │
   ▼
Spring Boot Application
   │
   ▼
Docker Image
   │
   ▼
ECR
   │
   ▼
ECS
   │
   ▼
Private Subnet
   │
   ├──────────────► RDS PostgreSQL
   │
   ▼
ALB
   │
   ▼
Client
```

---

# 22. Current Infrastructure Status

```text
ClothFlow Infrastructure

       │
       ├── VPC                  ✅
       ├── Public Subnets       ✅
       ├── Private Subnets      ✅
       ├── Security Groups      ✅
       ├── ALB                  ✅
       ├── ECR                  ✅
       ├── ECS Cluster          ✅
       ├── ECS IAM Roles        ✅
       ├── CloudWatch Logs      ✅
       ├── PostgreSQL RDS       ✅
       ├── Terraform Modules    ✅
       └── Terraform Outputs    ✅
```

### Current deployment status

```text
Infrastructure       ✅
Product Service      ✅
Product Docker Image ✅
ECR Image            ✅
ECR Verification     ✅

ECS Service Deployment
        ⏳ NEXT

ALB → ECS
        ⏳ NEXT

ECS → PostgreSQL
        ⏳ NEXT
```

---

# 23. Infrastructure Contract Rule

From this point forward, **we should treat this contract as the baseline**.

We should **not casually redesign the Terraform architecture** while implementing the services.

If we need to change something, we first identify:

```text
Why is the existing contract insufficient?
```

Then update the contract deliberately.

This keeps ClothFlow consistent as we move from:

```text
Terraform
   ↓
ECR
   ↓
ECS
   ↓
Product Service
   ↓
Inventory Service
   ↓
Order Service
   ↓
ALB
   ↓
Kafka
   ↓
Outbox/Saga
```

That's the infrastructure baseline I'd preserve for our next chats.
