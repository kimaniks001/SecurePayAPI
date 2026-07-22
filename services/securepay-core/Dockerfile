# SecurePay securepay-core — multi-stage runtime image (Phase 2)
# Build context: repository root

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY shared ./shared
COPY services ./services
COPY database ./database
RUN chmod +x gradlew && ./gradlew :services:securepay-core:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S securepay && adduser -S securepay -G securepay
WORKDIR /app
USER securepay
COPY --from=build /workspace/services/securepay-core/build/libs/securepay-core.jar /app/securepay-core.jar
EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health/live >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/securepay-core.jar"]
