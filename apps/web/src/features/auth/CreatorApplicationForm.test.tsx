import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CreatorApplicationForm } from "./CreatorApplicationForm";

const replace = vi.fn();
const authenticatedFetch = vi.fn();
const refresh = vi.fn();
let currentSession: unknown = null;

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("./AuthProvider", () => ({
  useAuth: () => ({
    session: currentSession,
    loading: false,
    authenticatedFetch,
    refresh
  })
}));

/** 以用户视角验证 Creator 申请权限提示与提交链路。 */
describe("CreatorApplicationForm", () => {
  beforeEach(() => {
    replace.mockReset();
    authenticatedFetch.mockReset();
    refresh.mockReset();
    currentSession = null;
  });

  it("asks anonymous users to login", () => {
    render(<CreatorApplicationForm />);
    expect(screen.getByRole("alert")).toHaveTextContent("请先登录");
  });

  it("submits a creator application and opens the public profile", async () => {
    currentSession = { user: { role: "USER" }, accessToken: "access" };
    authenticatedFetch.mockResolvedValue({ id: "creator-1" });
    refresh.mockResolvedValue({});
    render(<CreatorApplicationForm />);

    fireEvent.change(screen.getByLabelText("创作者名称"), { target: { value: "Alice Studio" } });
    fireEvent.change(screen.getByLabelText(/公开 slug/), { target: { value: "alice-studio" } });
    fireEvent.change(screen.getByLabelText("简介"), { target: { value: "Independent artist" } });
    fireEvent.click(screen.getByRole("button", { name: "提交申请" }));

    await waitFor(() =>
      expect(authenticatedFetch).toHaveBeenCalledWith("/api/v1/me/creator", {
        method: "POST",
        body: {
          displayName: "Alice Studio",
          slug: "alice-studio",
          category: "ART",
          bio: "Independent artist",
          location: ""
        }
      })
    );
    expect(refresh).toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith("/creators/creator-1");
  });
});
