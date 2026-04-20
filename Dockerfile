# Stage 1 — build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline

COPY src/ src/

RUN ./mvnw package -DskipTests

# Stage 2 — run
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S singify && adduser -S singify -G singify

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown singify:singify app.jar

USER singify

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
