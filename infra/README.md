# Infra

Docker Compose 默认启动 CreatorHub 完整本地栈：API、Web、PostgreSQL、Redis 和 MinIO。
只需要中间件时可以显式指定 `postgres redis minio` 服务。

数据库环境隔离：开发使用 `creatorhub` 和 `postgres_data`；E2E 使用独立的
`creatorhub_e2e`、端口 `15433` 和 `postgres_e2e_data`；生产使用独立的
`postgres_prod_data`，并默认不向宿主机暴露 PostgreSQL 端口。Compose 项目名也按环境区分。

## compose.yaml

包含以下服务：

- `api`：Spring Boot API，8080。
- `web`：Next.js standalone，3000。
- `postgres`：PostgreSQL 17，容器端口 5432，主机端口 15432（避免与宿主机 PostgreSQL 冲突）。
- `redis`：Redis 8，6379。
- `minio`：S3 兼容对象存储，9000（API）、9001（控制台）。

## 首次启动

```powershell
Copy-Item infra/env.example .env
docker compose -f infra/compose.yaml up -d
docker compose -f infra/compose.yaml ps
```

完整栈启动会先等待 PostgreSQL、Redis、MinIO 健康，再启动 API，最后启动 Web。
浏览器访问 `http://localhost:3000`，API 健康检查访问 `http://localhost:8080/actuator/health`。
如果镜像构建因 Docker Hub 或 npm registry 网络不可用失败，修复网络后重新执行同一命令即可；镜像构建不会修改数据库卷。

Linux / macOS 使用 `cp infra/env.example .env`。

## E2E 环境

E2E 必须使用独立 Compose 项目和端口，不能连接开发栈的 `creatorhub` 数据库：

```powershell
$env:E2E_DB_PASSWORD = "creatorhub_e2e_password"
docker compose -p creatorhub-e2e -f infra/compose.yaml -f infra/compose.e2e.yaml up -d --build
```

E2E Web 地址为 `http://localhost:3001`，API 地址为 `http://localhost:8081`。

## 生产环境

生产必须使用独立项目名、生产变量和生产卷，不得复用开发 `.env`：

```powershell
docker compose -p creatorhub-prod -f infra/compose.yaml -f infra/compose.prod.yaml up -d --build --wait
```

生产 `.env` 至少需要显式设置 `DB_NAME`、`DB_USER`、`DB_PASSWORD`、`JWT_SECRET`、
`REDIS_PASSWORD`、`CORS_ALLOWED_ORIGINS`、`S3_ACCESS_KEY`、`S3_SECRET_KEY` 和
`NEXT_PUBLIC_API_BASE_URL`。当前前端域名为 `https://creatormall.shop`，因此
`CORS_ALLOWED_ORIGINS=https://creatormall.shop`；API 地址应填写浏览器可访问的
公开 API 域名，例如 `https://api.creatormall.shop`，不能填写 Docker 内部的 `api`。

镜像职责：

- `api.Dockerfile` 使用 Maven 3.9 + JDK 21 构建 Spring Boot jar。
- `web.Dockerfile` 使用 Node 22 + pnpm 11 构建 Next.js standalone 产物；不依赖本机 Node/pnpm。
- Web 依赖下载使用 BuildKit cache mount；网络中断后重跑 `docker compose build web` 会复用已下载的 pnpm store，不会从零开始。
- Docker 构建显式使用 pnpm `hoisted` linker，避免 `isolated` linker 的跨层符号链接指向临时 cache mount；仓库本地配置仍保持 `isolated`。
- Web 构建默认使用 `https://registry.npmmirror.com`，因为当前网络访问 npm 官方源的大包会反复超时。其他网络可在 `.env` 设置 `NPM_REGISTRY=https://registry.npmjs.org` 覆盖。
- 浏览器通过 `http://localhost:8080` 访问 API；容器内部服务通过 `http://api:8080` 通信。不要把 Docker 内部主机名 `api` 写入 `NEXT_PUBLIC_*`，因为该变量会被内联到浏览器 bundle。

## 日常开发

不使用 Compose 时，也可以在仓库根分别启动 API 与 Web：

```powershell
mvn -B -ntp -f apps/api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
pnpm web:dev
```

## 镜像部署与回滚

部署脚本使用显式镜像标签，适合 staging 或单机演练；不会自动连接 AWS，也不会删除数据库卷。

```bash
CREATORHUB_API_IMAGE=registry.example/creatorhub-api:2026-08-30 \
CREATORHUB_WEB_IMAGE=registry.example/creatorhub-web:2026-08-30 \
sh infra/deploy/deploy.sh
```

回滚必须显式提供上一版本镜像，避免误回滚到未知版本：

```bash
CREATORHUB_ROLLBACK_API_IMAGE=registry.example/creatorhub-api:previous \
CREATORHUB_ROLLBACK_WEB_IMAGE=registry.example/creatorhub-web:previous \
sh infra/deploy/rollback.sh
```

脚本依赖 Docker Compose v2 的 `--wait` 健康检查；运行前应准备生产环境变量和私有镜像凭据。

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
