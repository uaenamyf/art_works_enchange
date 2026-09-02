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
DB_USER=admin
DB_PASSWORD=服务器数据库密码
API_PORT=18080
WEB_PORT=3000
JWT_SECRET=至少32字节随机字符串
REDIS_PASSWORD=服务器Redis密码
CORS_ALLOWED_ORIGINS=https://creatormall.shop
WEB_PUBLIC_URL=https://creatormall.shop
NEXT_PUBLIC_API_BASE_URL=https://api.creatormall.shop
COOKIE_SECURE=true
# MinIO runs inside the production Docker network.
S3_REGION=us-east-1
S3_BUCKET=creatorhub-media
S3_ACCESS_KEY=admin
S3_SECRET_KEY=服务器MinIO密码
```

生产中间件账号约定为：PostgreSQL 和 MinIO 使用账号 `admin`；Redis 当前使用密码认证，没有用户名。密码只填写在服务器本地 `.env`，不要提交到 Git。若你决定使用同一个密码，可在服务器 `.env` 中将 `DB_PASSWORD`、`REDIS_PASSWORD` 和 `S3_SECRET_KEY` 都设置为同一值。`WEB_PUBLIC_URL` 是 Web 首页地址，`NEXT_PUBLIC_API_BASE_URL` 是浏览器调用 API 的地址，两者不是同一个地址。

部署脚本会自动：

1. 校验 JWT、数据库、Redis、MinIO、CORS 和浏览器 API 地址配置。
2. 校验生产 Compose 合并结果。
3. 构建 API 和 Web 镜像。
4. 启动 PostgreSQL、Redis、MinIO、API 和 Web。
5. 等待 API/Web 健康检查通过。

生产 Compose 使用独立项目和数据卷，不会复用开发数据卷。PostgreSQL、Redis 和 MinIO 不映射宿主机端口，API 通过 Docker 内网访问 MinIO。

## Nginx 反向代理与 HTTPS

Docker 中的 API 容器使用内部端口 `8080`，宿主机端口固定为 `18080`；Web 宿主机端口为 `3000`。两个端口只绑定服务器本机，公网由宿主机 Nginx 接收 `80/443` 请求。安装 Nginx：

```sh
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
sudo mkdir -p /var/www/letsencrypt/.well-known/acme-challenge
sudo chown -R www-data:www-data /var/www/letsencrypt
sudo cp infra/deploy/nginx.creatorhub.conf /etc/nginx/sites-available/creatorhub
sudo ln -s /etc/nginx/sites-available/creatorhub /etc/nginx/sites-enabled/creatorhub
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl enable --now nginx
```

先验证 ACME 文件可以从公网访问：

```sh
echo creatorhub-acme-ok | sudo tee /var/www/letsencrypt/.well-known/acme-challenge/healthcheck.txt
curl http://creatormall.shop/.well-known/acme-challenge/healthcheck.txt
curl http://api.creatormall.shop/.well-known/acme-challenge/healthcheck.txt
sudo rm /var/www/letsencrypt/.well-known/acme-challenge/healthcheck.txt
```

两个请求都必须返回 `creatorhub-acme-ok`。确认 DNS 已将 `creatormall.shop` 和 `api.creatormall.shop` 指向服务器，并且防火墙允许 `80/443` 后，申请 HTTPS 证书：

```sh
sudo certbot --nginx \\
    -d creatormall.shop \\
    -d api.creatormall.shop
```

Certbot 会自动将两个 HTTP 站点升级为 HTTPS，并配置证书续期。检查：

```sh
sudo nginx -t
sudo systemctl reload nginx
sudo certbot renew --dry-run
curl -I https://creatormall.shop
curl https://api.creatormall.shop/actuator/health
```

API 域名必须与 `.env` 中的 `NEXT_PUBLIC_API_BASE_URL` 一致；Web 首页地址是 `https://creatormall.shop`。

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