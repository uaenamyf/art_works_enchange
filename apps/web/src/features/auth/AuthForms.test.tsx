import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "@/lib/api/fetcher";
import { LoginForm } from "./LoginForm";
import { RegisterForm } from "./RegisterForm";
import { LocaleProvider } from "@/features/locale/LocaleProvider";

const replace = vi.fn();
const setSession = vi.fn();

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("./AuthProvider", () => ({ useAuth: () => ({ setSession }) }));
vi.mock("@/lib/api/fetcher", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/fetcher")>("@/lib/api/fetcher");
  return { ...actual, apiFetch: vi.fn() };
});

const mockedApiFetch = vi.mocked(apiFetch);

function renderWithLocale(form: React.ReactNode) {
  return render(<LocaleProvider>{form}</LocaleProvider>);
}

/** 从用户行为验证认证表单，不断言 React Hook Form 内部状态。 */
describe("auth forms", () => {
  beforeEach(() => {
    mockedApiFetch.mockReset();
    replace.mockReset();
    setSession.mockReset();
  });

  it("submits login credentials to the auth API and redirects", async () => {
    mockedApiFetch.mockResolvedValue({});
    renderWithLocale(<LoginForm />);

    fireEvent.change(screen.getByLabelText("邮箱"), { target: { value: "alice@example.com" } });
    fireEvent.change(screen.getByLabelText("密码"), {
      target: { value: "very-secure-password" }
    });
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() =>
      expect(mockedApiFetch).toHaveBeenCalledWith("/api/v1/auth/login", {
        method: "POST",
        body: { email: "alice@example.com", password: "very-secure-password" }
      })
    );
    expect(replace).toHaveBeenCalledWith("/");
  });

  it("shows a stable message for invalid credentials", async () => {
    mockedApiFetch.mockRejectedValue(
      new ApiError(
        "Invalid email or password",
        { code: "INVALID_CREDENTIALS", detail: "Invalid email or password" },
        401,
        null
      )
    );
    renderWithLocale(<LoginForm />);

    fireEvent.change(screen.getByLabelText("邮箱"), { target: { value: "alice@example.com" } });
    fireEvent.change(screen.getByLabelText("密码"), { target: { value: "wrong-password" } });
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("邮箱或密码错误");
  });

  it("prevents registration when password is shorter than the OpenAPI contract", async () => {
    renderWithLocale(<RegisterForm />);

    fireEvent.change(screen.getByLabelText("邮箱"), { target: { value: "alice@example.com" } });
    fireEvent.change(screen.getByLabelText("密码"), { target: { value: "short" } });
    fireEvent.change(screen.getByLabelText("显示名称"), { target: { value: "Alice" } });
    fireEvent.click(screen.getByRole("button", { name: "创建账号" }));

    expect(await screen.findByText("密码至少 10 个字符")).toBeInTheDocument();
    expect(mockedApiFetch).not.toHaveBeenCalled();
  });
});
