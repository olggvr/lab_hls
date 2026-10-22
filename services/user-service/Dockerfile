# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Сначала только pom.xml — чтобы зависимости кешировались отдельным слоем
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY lombok.config .
COPY src ./src
# Тесты требуют Docker (Testcontainers), поэтому внутри образа их не запускаем —
# они гоняются локально командой `mvn verify`
RUN mvn -B -q package -DskipTests -Djacoco.skip=true

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/school-management.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
