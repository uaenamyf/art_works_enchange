# lib/auth

> 客户端会话状态：Access Token 内存缓存、Refresh 协调、登出与权限判断。

## 进入条件

- 调整会话存储或权限判断。
- 调整登录/刷新/登出时的副作用。

## 必读

- 根 `AGENTS.md` §4、§7（不把 Access Token 写入 localStorage）。
- 根 `DEVELOPMENT_GUIDE.md` §8 Refresh Cookie 策略。
- 14 周计划 Week 3、Week 12。

## 约束

- Access Token 仅保留在内存（Context/Store），不写入 localStorage/sessionStorage。
- Refresh Token 完全由浏览器 Cookie 管理；前端不读不写。
- 401 时尝试一次静默刷新；失败则跳转登录并保留 `returnTo`。
- 不在前端写“是否已登录”之外的鉴权逻辑；接口级权限由后端把关。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- lib/auth
```

## 交叉引用

- `features/auth/AGENTS.md` — 登录/注册/注销页面。
- `lib/api/AGENTS.md` — 受保护请求与 401 处理。
