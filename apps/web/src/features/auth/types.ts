/**
 * Week 3 认证响应的前端只读视图。
 *
 * Refresh Token 不属于此类型；它只能由浏览器通过 HttpOnly Cookie 管理。
 * 后续 OpenAPI 生成客户端接入后，本文件应替换为生成类型引用。
 */
export interface AuthSession {
  accessToken: string;
  expiresIn: number;
  user: {
    id: string;
    displayName: string;
    avatarUrl: string | null;
    role: "USER" | "CREATOR" | "ADMIN";
  };
}

/** 判断当前账号是否可以进入 Creator Studio；ADMIN 用于平台运营和本地联调。 */
export function hasCreatorWorkspaceAccess(session: AuthSession | null): boolean {
  return session?.user.role === "CREATOR" || session?.user.role === "ADMIN";
}
