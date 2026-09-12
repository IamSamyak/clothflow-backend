# ============================================================
# Stage 1: Build
# ============================================================

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven wrapper/project files first
# This allows Docker to cache dependency downloads.
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests


# ============================================================
# Stage 2: Runtime
# ============================================================

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]