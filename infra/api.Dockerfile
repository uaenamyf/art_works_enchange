# ---- build stage ----
# Maven 镜像自带 JDK 21 且内置 Maven 3.9；一次基础镜像完成全部后端构建。
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 先拷贝 pom 触发依赖缓存命中（源码改动不会重下 ~500MB 依赖）。
COPY apps/api/pom.xml apps/api/pom.xml
RUN mvn -B -f apps/api/pom.xml -DskipTests dependency:go-offline

# 再拷贝源码并打包，跳过测试（测试在 CI 跑）。
COPY apps/api apps/api
RUN mvn -B -f apps/api/pom.xml -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system creatorhub && useradd --system --gid creatorhub --uid 1001 creatorhub
WORKDIR /app
COPY --from=build /workspace/apps/api/target/creatorhub-api.jar /app/app.jar
USER creatorhub
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
  CMD curl --fail --silent http://localhost:8080/actuator/health | grep -q UP || exit 1
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
