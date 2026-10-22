FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY infrastructure ./infrastructure
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/${MODULE}/target/*.jar /app/app.jar
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
