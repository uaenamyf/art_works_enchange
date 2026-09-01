# CreatorHub 生产一键部署

## 前置条件

- Ubuntu/Debian Linux 服务器。
- Docker Engine 和 Docker Compose v2 已安装。
- DNS 已将 `creatormall.shop` 指向服务器；API 建议使用 `api.creatormall.shop`。
- 项目根目录存在生产 `.env`；对象存储使用服务器 Docker 内的 MinIO。

## 首次部署

在服务器执行：

```sh
git clone https://github.com/uaenamyf/art_works_enchange.git
cd art_works_enchange
cp infra/env.production.example .env
chmod 600 .env
vi .env
./infra/deploy/deploy.sh
```

`.env` 至少要填写：

```env
DB_NAME=creatorhub
DB_USER=creatorhub
DB_PASSWORD=生产数据库强密码
JWT_SECRET=至少32字节随机字符串
REDIS_PASSWORD=Redis生产强密码
CORS_ALLOWED_ORIGINS=https://creatormall.shop
NEXT_PUBLIC_API_BASE_URL=https://api.creatormall.shop
COOKIE_SECURE=true
# MinIO runs inside the production Docker network.
S3_REGION=us-east-1
S3_BUCKET=creatorhub-media
S3_ACCESS_KEY=生产 MinIO 管理员账号
S3_SECRET_KEY=生产 MinIO 强密码
```

部署脚本会自动：

1. 校验 JWT、数据库、Redis、MinIO、CORS 和浏览器 API 地址配置。
2. 校验生产 Compose 合并结果。
3. 构建 API 和 Web 镜像。
4. 启动 PostgreSQL、Redis、MinIO、API 和 Web。
5. 等待 API/Web 健康检查通过。

生产 Compose 使用独立项目和数据卷，不会复用开发数据卷。PostgreSQL、Redis 和 MinIO 不映射宿主机端口，API 通过 Docker 内网访问 MinIO。

## HTTPS 反向代理

Compose 对外提供 API `8080` 和 Web `3000`，建议在宿主机使用 Caddy：

```caddyfile
creatormall.shop {
    reverse_proxy 127.0.0.1:3000
}

api.creatormall.shop {
    reverse_proxy 127.0.0.1:8080
}
```

确认 DNS 和防火墙允许 `80/443` 后，Caddy 会自动申请 HTTPS 证书。API 域名必须与 `.env` 中的 `NEXT_PUBLIC_API_BASE_URL` 一致。

## 日常更新

```sh
git pull --ff-only origin main
./infra/deploy/deploy.sh
```

脚本不会删除数据库、Redis 或 MinIO 数据卷。

## 回滚

```sh
CREATORHUB_ROLLBACK_API_IMAGE=registry.example/creatorhub-api:previous \
CREATORHUB_ROLLBACK_WEB_IMAGE=registry.example/creatorhub-web:previous \
./infra/deploy/rollback.sh
```

回滚也会使用生产 Compose override，并等待 API/Web 健康检查。

## 检查

```sh
docker compose -p creatorhub-prod -f infra/compose.yaml -f infra/compose.prod.yaml ps
curl https://api.creatormall.shop/actuator/health
curl -I https://creatormall.shop
```