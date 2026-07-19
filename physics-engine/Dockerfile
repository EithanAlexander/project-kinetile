# Build fat JAR from repo root (parent POM + physics-engine module).
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY physics-engine/pom.xml physics-engine/
COPY physics-engine/src physics-engine/src
RUN mvn -q -pl physics-engine -am package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /build/physics-engine/target/*.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=12 --start-period=90s \
    CMD curl -sf http://localhost:8080/actuator/health | grep -q "\"status\":\"UP\"" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
