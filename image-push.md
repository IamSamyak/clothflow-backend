Absolutely. For ClothFlow, we should maintain a **deployment contract/runbook** so that when we move from Product → Inventory → Payment → Order, we follow the same process instead of rediscovering FLOCI/ECR problems each time.

Below is the contract I recommend we treat as the **official ClothFlow container → ECR → ECS deployment contract**.

# ClothFlow — Container & ECR Deployment Contract

**Document:** `CONTAINER_ECR_ECS_DEPLOYMENT_CONTRACT.md`
**Scope:** Docker → FLOCI Registry → ECR → ECS/Fargate → ALB
**Environment:** Local AWS emulation using FLOCI
**AWS Region:** `us-east-1`
**AWS Profile:** `clothflow`

---

## 1. Purpose

Every ClothFlow microservice must follow this deployment lifecycle:

```text
Source Code
    │
    ▼
Maven Build
    │
    ▼
Docker Image
    │
    ▼
Local Registry
localhost:5100
    │
    ▼
FLOCI ECR Repository
    │
    ▼
ECS Task Definition
    │
    ▼
ECS Fargate Service
    │
    ▼
ALB
    │
    ▼
HTTP API
```

The deployment process must be **repeatable** and must not depend on manually modifying running containers.

---

# 2. Naming Contract

Every service follows:

```text
clothflow/<service-name>
```

Examples:

```text
clothflow/product-service
clothflow/inventory-service
clothflow/order-service
clothflow/payment-service
clothflow/shipping-service
clothflow/notification-service
```

Docker image:

```text
clothflow/payment-service:1.0.0
```

ECR repository:

```text
clothflow/payment-service
```

ECS service:

```text
clothflow-payment-service
```

ECS task definition family:

```text
clothflow-payment-service
```

CloudWatch log group:

```text
/ecs/clothflow/payment-service
```

ALB target group:

```text
clothflow-payment-tg
```

---

# 3. Versioning Contract

For now:

```text
MAJOR.MINOR.PATCH
```

Example:

```text
1.0.0
1.0.1
1.1.0
2.0.0
```

Because our ECR repositories are:

```text
image_tag_mutability = "IMMUTABLE"
```

**Never reuse an existing tag.**

For example, after pushing:

```text
payment-service:1.0.0
```

do not rebuild something different and try to push:

```text
payment-service:1.0.0
```

Instead:

```text
payment-service:1.0.1
```

This gives us a very important production principle:

> **An image tag represents one immutable artifact.**

---

# 4. Docker Build Contract

Every service must have a multi-stage Dockerfile.

Example:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
```

The exposed port must match the service.

| Service   |                             Port |
| --------- | -------------------------------: |
| Product   | 8081 locally / 8080 ECS baseline |
| Inventory |                             8082 |
| Order     |                             8083 |
| Payment   |                             8084 |

---

# 5. Build Contract

From the service directory:

```bash
mvn clean package -DskipTests
```

Then:

```bash
docker build -t clothflow/payment-service:1.0.0 .
```

Verify:

```bash
docker images | grep clothflow/payment-service
```

Expected:

```text
clothflow/payment-service    1.0.0
```

---

# 6. Local Image Test Contract

Before pushing, the image should be tested locally.

Example:

```bash
docker run --rm \
  -p 8084:8084 \
  clothflow/payment-service:1.0.0
```

Then:

```bash
curl http://localhost:8084/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

For services that require PostgreSQL/Kafka, the necessary dependencies must be available before considering the service healthy.

---

# 7. FLOCI ECR Contract

This is particularly important because **FLOCI is not identical to real AWS ECR**.

Our environment:

```text
AWS CLI
   │
   │ endpoint-url
   ▼
localhost:4566
   │
   ▼
FLOCI
```

Registry:

```text
localhost:5100
```

AWS CLI profile:

```text
clothflow
```

Region:

```text
us-east-1
```

---

# 8. Create ECR Repository

Terraform is the source of truth.

Do **not** manually create repositories unless debugging.

Terraform:

```hcl
resource "aws_ecr_repository" "payment_service" {
  name                 = "clothflow/payment-service"
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}
```

Verify:

```bash
aws ecr describe-repositories \
  --repository-name clothflow/payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

---

# 9. Important FLOCI ECR URI Contract

This is the part we just discovered.

The ECR API may return an ECR URI such as:

```text
000000000000.dkr.ecr.us-east-1.localhost:5100/clothflow/product-service
```

or, depending on FLOCI's configured URI style:

```text
localhost:5100/000000000000/us-east-1/clothflow/payment-service
```

Therefore:

> **Never assume that the repository URI format is the same for every existing FLOCI repository.**

Always check:

```bash
aws ecr describe-repositories \
  --repository-name clothflow/payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

Look at:

```json
"repositoryUri": "..."
```

---

# 10. Known FLOCI Docker Push Problem

We already encountered:

```text
lookup 000000000000.dkr.ecr.us-east-1.localhost: no such host
```

This occurs because Docker/WSL may not resolve the FLOCI ECR hostname even though AWS CLI can communicate with:

```text
localhost:4566
```

Therefore the **known local workaround** is:

```bash
docker tag \
  clothflow/payment-service:1.0.0 \
  localhost:5100/clothflow/payment-service:1.0.0
```

Then:

```bash
docker push \
  localhost:5100/clothflow/payment-service:1.0.0
```

However, there is an important caveat.

---

# 11. FLOCI Repository Path Contract

Before pushing, check the registry:

```bash
curl http://localhost:5100/v2/_catalog
```

If the ECR repository uses path-style URI, you may see:

```text
000000000000/us-east-1/clothflow/payment-service
```

Therefore the image may need to be pushed to:

```bash
docker tag \
  localhost:5100/clothflow/payment-service:1.0.0 \
  localhost:5100/000000000000/us-east-1/clothflow/payment-service:1.0.0
```

Then:

```bash
docker push \
  localhost:5100/000000000000/us-east-1/clothflow/payment-service:1.0.0
```

This is the exact workaround that fixed our Payment Service deployment.

---

# 12. ECR Verification Contract

**Never assume that a successful `docker push` means ECR sees the image.**

Always perform:

```bash
aws ecr describe-images \
  --repository-name clothflow/payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

Expected:

```json
{
    "imageDetails": [
        {
            "repositoryName": "clothflow/payment-service",
            "imageTags": [
                "1.0.0"
            ],
            "imageDigest": "sha256:..."
        }
    ]
}
```

The critical check is:

```text
imageTags → 1.0.0
```

---

# 13. Registry Verification

You can also verify the Docker Registry directly:

```bash
curl \
  http://localhost:5100/v2/clothflow/payment-service/tags/list
```

or, for path-style repositories:

```bash
curl \
  http://localhost:5100/v2/000000000000/us-east-1/clothflow/payment-service/tags/list
```

Expected:

```json
{
  "name": "...",
  "tags": [
    "1.0.0"
  ]
}
```

But remember:

```text
Registry tag exists
        ≠
ECR API recognizes image
```

We just experienced this.

---

# 14. Common ECR Errors

## Error 1 — DNS failure

```text
lookup 000000000000.dkr.ecr.us-east-1.localhost:
no such host
```

### Meaning

Docker cannot resolve FLOCI's generated ECR hostname.

### Action

Use the local registry:

```text
localhost:5100
```

and apply the appropriate repository path.

---

## Error 2 — Docker push succeeds but ECR is empty

You see:

```text
docker push ... SUCCESS
```

but:

```bash
aws ecr describe-images ...
```

returns:

```json
{
    "imageDetails": []
}
```

### Meaning

The image was pushed into Docker Registry, but probably under the wrong FLOCI repository path.

### Action

Check:

```bash
curl http://localhost:5100/v2/_catalog
```

and:

```bash
aws ecr describe-repositories ...
```

Compare:

```text
repositoryUri
```

with the actual registry path.

---

# 15. Common Error — Immutable Tag

You may see something equivalent to:

```text
ImageTagAlreadyExistsException
```

### Meaning

You attempted:

```text
1.0.0 → existing image
```

with a different image.

### Action

Use:

```text
1.0.1
```

instead.

Never delete/reuse production-style tags just to make a deployment work.

---

# 16. Common Error — Repository Doesn't Exist

Example:

```text
RepositoryNotFoundException
```

### Action

Check:

```bash
aws ecr describe-repositories \
  --repository-name clothflow/payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

If missing, Terraform should create it.

---

# 17. Common Error — ECS Cannot Pull Image

If ECR works but ECS fails, check:

```text
ECS Task
   │
   ├── image URI
   ├── execution role
   ├── network
   └── ECR connectivity
```

Check ECS events:

```bash
aws ecs describe-services \
  --cluster clothflow \
  --services clothflow-payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

Typical causes:

```text
wrong image URI
wrong tag
ECR image missing
execution IAM role
network connectivity
FLOCI ECS/ECR emulation limitation
```

---

# 18. ECS Image URI Contract

There is an important distinction:

### Docker local push URI

May be:

```text
localhost:5100/...
```

### ECS Terraform image

Should use the ECR-style URI expected by the ECS/ECR integration.

For our Payment configuration:

```hcl
payment_container_image =
  "000000000000.dkr.ecr.us-east-1.localhost:5100/clothflow/payment-service:1.0.0"
```

Do **not** blindly replace this with:

```text
localhost:5100/...
```

The local Docker workaround and ECS image reference serve different purposes.

---

# 19. ECS Environment Contract

Never hardcode environment-specific infrastructure values into Java code.

Instead:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:...}

server:
  port: ${SERVER_PORT:8084}

spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

Local development:

```text
localhost
```

ECS:

```text
FLOCI internal hostname / AWS private DNS
```

Production:

```text
AWS RDS / MSK / Secrets Manager
```

Same application artifact, different environment configuration.

---

# 20. Secrets Contract

Never put:

```text
DB password
Kafka credentials
API keys
gateway secrets
```

inside:

```text
Dockerfile
Git
Terraform variables committed to Git
application.yml
```

For ECS:

```text
AWS Secrets Manager
        │
        ▼
ECS task definition
        │
        ▼
environment variable
        │
        ▼
Spring Boot
```

Our current ECS configuration uses:

```hcl
secrets = [
  {
    name      = "SPRING_DATASOURCE_USERNAME"
    valueFrom = "${var.rds_secret_arn}:username::"
  },
  {
    name      = "SPRING_DATASOURCE_PASSWORD"
    valueFrom = "${var.rds_secret_arn}:password::"
  }
]
```

---

# 21. ALB Contract

Every externally accessible service gets:

```text
ECS Service
    │
    ▼
Target Group
    │
    ▼
ALB Listener Rule
```

Payment:

```text
/api/v1/payments
/api/v1/payments/*
```

→

```text
clothflow-payment-tg
```

→

```text
payment-service:8084
```

Health check:

```text
/actuator/health
```

Expected:

```text
HTTP 200
```

---

# 22. ECS Deployment Contract

After the image exists in ECR:

```bash
terraform plan
```

Review carefully.

Then:

```bash
terraform apply
```

Terraform manages:

```text
ECR
ECS task definition
ECS service
ALB
target group
CloudWatch
security groups
```

Do not manually create equivalent ECS infrastructure.

---

# 23. Post-Deployment Verification

After ECS deployment:

### 1. ECS service

```bash
aws ecs describe-services \
  --cluster clothflow \
  --services clothflow-payment-service \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

Check:

```text
runningCount = desiredCount
```

---

### 2. ALB target

Verify the Payment target becomes:

```text
healthy
```

---

### 3. Health endpoint

Through ALB:

```bash
curl http://<ALB-DNS>/actuator/health
```

---

### 4. API

```bash
curl http://<ALB-DNS>/api/v1/payments/...
```

---

### 5. CloudWatch

Check:

```text
/ecs/clothflow/payment-service
```

for:

```text
Spring Boot startup
Flyway migrations
Kafka connection
database connection
application errors
```

---

# 24. Complete Deployment Checklist

This should become our standard checklist for **every future microservice**.

```text
[ ] Code compiles
[ ] Unit tests pass
[ ] Maven package succeeds
[ ] Dockerfile exists
[ ] Docker image builds
[ ] Container starts
[ ] /actuator/health = UP
[ ] ECR repository exists
[ ] Image tag is unique
[ ] Docker image tagged
[ ] Image pushed to FLOCI registry
[ ] Registry tag verified
[ ] ECR describe-images verified
[ ] Terraform variables updated
[ ] ECS task definition configured
[ ] Environment variables configured
[ ] Secrets configured
[ ] Kafka configuration verified
[ ] CloudWatch log group exists
[ ] ALB target group exists
[ ] ALB listener rule exists
[ ] Security group allows traffic
[ ] terraform plan reviewed
[ ] terraform apply
[ ] ECS task RUNNING
[ ] ALB target HEALTHY
[ ] /actuator/health = UP
[ ] API tested through ALB
[ ] CloudWatch logs verified
```

---

# 25. Troubleshooting Decision Tree

When deployment fails, **don't randomly rebuild/restart things**.

Use this:

```text
Docker build failed?
        │
        └── Fix Docker/Maven

Docker works?
        │
        ▼
Docker push failed?
        │
        ├── DNS error?
        │      └── Check FLOCI registry URI/path
        │
        └── Other?
               └── Check registry

Push succeeded?
        │
        ▼
ECR describe-images
        │
        ├── imageDetails empty
        │       │
        │       └── Registry path mismatch
        │
        └── image exists
                │
                ▼
        Terraform
                │
                ▼
        ECS task
                │
                ├── STOPPED
                │      └── ECS events / logs
                │
                └── RUNNING
                        │
                        ▼
                    ALB
                        │
                        ├── unhealthy
                        │      └── port / SG / health endpoint
                        │
                        └── healthy
                               │
                               ▼
                            API test
```

---

# 26. Golden Rule

For ClothFlow, remember these **three separate truths**:

```text
Docker Registry
    ↓
"Does the image physically exist?"

ECR API
    ↓
"Does FLOCI recognize the image as an ECR image?"

ECS
    ↓
"Can the task actually pull and run that image?"
```

A successful Docker push proves **only the first one**.

Our Payment incident demonstrated exactly why we need this contract:

```text
Docker push                  ✅
Registry tag                 ✅
ECR repository               ✅
ECR imageDetails             ❌
                         ↓
              Repository path mismatch
                         ↓
ECR imageDetails             ✅
```

---

## 27. Standard Commands — Quick Reference

For future services, this is the condensed version:

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Docker build
docker build -t clothflow/<service>:<version> .

# 3. Tag local registry
docker tag \
  clothflow/<service>:<version> \
  localhost:5100/clothflow/<service>:<version>

# 4. Push
docker push \
  localhost:5100/clothflow/<service>:<version>

# 5. Check registry
curl \
  http://localhost:5100/v2/clothflow/<service>/tags/list

# 6. Check ECR
aws ecr describe-images \
  --repository-name clothflow/<service> \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566

# 7. Terraform
terraform plan

# 8. Deploy
terraform apply

# 9. Check ECS
aws ecs describe-services \
  --cluster clothflow \
  --services clothflow-<service> \
  --profile clothflow \
  --region us-east-1 \
  --endpoint-url http://localhost:4566
```

**Exception:** If ECR `imageDetails` is empty after the Docker push, check the repository URI/path and use the FLOCI path-style push we discovered rather than assuming the normal command worked.

---

### Our project rule going forward

I suggest we treat this document as the **deployment contract for ClothFlow**.

So when we build **Order Service**, **Shipping**, **Notification**, etc., we won't reinvent the Docker/ECR/ECS process. We'll follow this contract and only add service-specific requirements.

And one more senior-level distinction we'll preserve:

> **Terraform owns infrastructure. Docker owns artifacts. ECS owns runtime. Spring Boot owns application behavior.**

That separation will become very important once we move from FLOCI to actual AWS.
