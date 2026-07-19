# Build fat JAR from repo root (parent POM + db-init module).
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY db-init/pom.xml db-init/
COPY db-init/src db-init/src
COPY physics-engine/pom.xml physics-engine/
RUN mvn -q -pl db-init -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/db-init/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
