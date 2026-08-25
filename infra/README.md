# Infra

容器化与本地开发环境。

## compose.yaml

包含以下服务：

- `postgres`：PostgreSQL 17，5432。
- `redis`：Redis 8，6379。
- `minio`：S3 兼容对象存储，9000（API）、9001（控制台）。
- `api`：Spring Boot 后端，8080，profile=docker。
- `web`：Next.js 前端，3000。

## 启动

```bash
cp infra/env.example .env
docker compose -f infra/compose.yaml up -d
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:3000/
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
| 5432 | postgres |
| 6379 | redis |
| 8080 | api |
| 9000 | minio api |
| 9001 | minio console |
