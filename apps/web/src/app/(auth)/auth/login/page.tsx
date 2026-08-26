import { LoginForm } from "@/features/auth/LoginForm";

/** 登录路由保持 Server Component，浏览器交互由最小化的 LoginForm Client Component 承担。 */
export default function LoginPage() {
  return <LoginForm />;
}
