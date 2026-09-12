# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy dependency descriptors first
COPY pom.xml ./

# Download dependencies
RUN mvn -B dependency:go-offline

# Copy source
COPY src ./src

# Build
RUN mvn -B clean package -DskipTests


# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd \
        --system \
        --uid 1001 \
        --no-create-home \
        --shell /usr/sbin/nologin \
        appuser

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
