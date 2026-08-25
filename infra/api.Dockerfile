# syntax=docker/dockerfile:1.7
# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle.kts ./
COPY apps/api/build.gradle.kts apps/api/build.gradle.kts
COPY apps/api/settings.gradle.kts apps/api/settings.gradle.kts
RUN chmod +x gradlew && ./gradlew :apps:api:dependencies --no-daemon || true
COPY apps/api apps/api
RUN ./gradlew :apps:api:bootJar --no-daemon -x test

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN groupadd --system creatorhub && useradd --system --gid creatorhub --uid 1001 creatorhub
WORKDIR /app
COPY --from=build /workspace/apps/api/build/libs/*.jar /app/app.jar
USER creatorhub
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
