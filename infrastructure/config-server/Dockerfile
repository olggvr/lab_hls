FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY infrastructure ./infrastructure
COPY services ./services
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl infrastructure/config-server -am package -DskipTests

COPY config-repo /config-repo
RUN git -C /config-repo init --initial-branch=main && \
    git -C /config-repo config user.name "School Config Server" && \
    git -C /config-repo config user.email "config@school.local" && \
    git -C /config-repo add . && \
    git -C /config-repo commit -m "Config snapshot"

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/infrastructure/config-server/target/config-server.jar /app/app.jar
COPY --from=build --chown=app:app /config-repo /config-repo
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
