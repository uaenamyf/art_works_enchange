# Infra

Docker Compose 只负责 CreatorHub 本地中间件。本机 API 使用 Java 21 + Maven，Web 使用
Node.js + pnpm；PostgreSQL、Redis、MinIO 由容器统一版本。

## compose.yaml

包含以下服务：

- `postgres`：PostgreSQL 17，容器端口 5432，主机端口 15432（避免与宿主机 PostgreSQL 冲突）。
- `redis`：Redis 8，6379。
- `minio`：S3 兼容对象存储，9000（API）、9001（控制台）。

## 首次启动

```powershell
Copy-Item infra/env.example .env
docker compose -f infra/compose.yaml up -d
docker compose -f infra/compose.yaml ps
```

Linux / macOS 使用 `cp infra/env.example .env`。

部署镜像职责（不由当前 compose.yaml 启动）：

- `api.Dockerfile` 使用 Maven 3.9 + JDK 21 构建 Spring Boot jar。
- `web.Dockerfile` 使用 Node 22 + pnpm 11 构建 Next.js standalone 产物；不依赖本机 Node/pnpm。
- Web 依赖下载使用 BuildKit cache mount；网络中断后重跑 `docker compose build web` 会复用已下载的 pnpm store，不会从零开始。
- Docker 构建显式使用 pnpm `hoisted` linker，避免 `isolated` linker 的跨层符号链接指向临时 cache mount；仓库本地配置仍保持 `isolated`。
- Web 构建默认使用 `https://registry.npmmirror.com`，因为当前网络访问 npm 官方源的大包会反复超时。其他网络可在 `.env` 设置 `NPM_REGISTRY=https://registry.npmjs.org` 覆盖。
- 浏览器通过 `http://localhost:8080` 访问 API；容器内部服务通过 `http://api:8080` 通信。不要把 Docker 内部主机名 `api` 写入 `NEXT_PUBLIC_*`，因为该变量会被内联到浏览器 bundle。

## 日常开发

中间件启动后，在仓库根分别启动 API 与 Web：

```powershell
mvn -B -ntp -f apps/api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
pnpm web:dev
```

查看日志：

```powershell
docker compose -f infra/compose.yaml logs -f postgres
docker compose -f infra/compose.yaml logs -f redis
docker compose -f infra/compose.yaml logs -f minio
```

执行验证：

```powershell
mvn -B -ntp -f apps/api/pom.xml test
pnpm web:typecheck
docker compose -f infra/compose.yaml config
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:3000/
```

PowerShell 可使用：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:3000/ -UseBasicParsing
```

## 关闭与清理

```bash
docker compose -f infra/compose.yaml down            # 保留数据
docker compose -f infra/compose.yaml down -v         # 同时清理卷
```

## 端口分配

| 端口 | 服务 |
| --- | --- |
| 3000 | web |
| 15432 | postgres（宿主机映射端口） |
| 6379 | redis |
| 8080 | api |
| 9000 | minio api |
| 9001 | minio console |
