import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError, apiFetch } from "@/lib/api/fetcher";
import { AuthProvider, useAuth } from "./AuthProvider";

vi.mock("@/lib/api/fetcher", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api/fetcher")>("@/lib/api/fetcher");
  return { ...actual, apiFetch: vi.fn() };
});

const mockedApiFetch = vi.mocked(apiFetch);

function SessionProbe() {
  const { session, loading } = useAuth();
  return <p>{loading ? "loading" : session?.user.displayName ?? "anonymous"}</p>;
}

function AuthenticatedFetchProbe() {
  const { authenticatedFetch } = useAuth();
  return <button onClick={() => void authenticatedFetch("/api/v1/orders")}>load orders</button>;
}

/** 验证 Provider 只通过 Refresh Cookie 恢复会话，不依赖 Web Storage。 */
describe("AuthProvider", () => {
  it("restores the in-memory session from the refresh endpoint", async () => {
    mockedApiFetch.mockResolvedValue({
      accessToken: "access",
      expiresIn: 900,
      user: { id: "u-1", displayName: "Alice", avatarUrl: null, role: "USER" }
    });

    render(
      <AuthProvider>
        <SessionProbe />
      </AuthProvider>
    );

    expect(screen.getByText("loading")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("Alice")).toBeInTheDocument());
    expect(mockedApiFetch).toHaveBeenCalledWith("/api/v1/auth/refresh", { method: "POST" });
  });

  it("refreshes once and retries a request after an expired access token", async () => {
    const initialSession = {
      accessToken: "initial-access",
      expiresIn: 900,
      user: { id: "u-1", displayName: "Alice", avatarUrl: null, role: "USER" as const }
    };
    const restoredSession = { ...initialSession, accessToken: "restored-access" };
    mockedApiFetch.mockResolvedValue(initialSession);

    render(
      <AuthProvider>
        <AuthenticatedFetchProbe />
      </AuthProvider>
    );
    await waitFor(() => expect(screen.getByText("load orders")).toBeInTheDocument());
    mockedApiFetch.mockClear();
    mockedApiFetch
      .mockRejectedValueOnce(new ApiError("expired", {}, 401, null))
      .mockResolvedValueOnce(restoredSession)
      .mockResolvedValueOnce({ items: [], hasMore: false });
    screen.getByText("load orders").click();
    await waitFor(() => expect(mockedApiFetch).toHaveBeenCalledTimes(3));
    expect(mockedApiFetch.mock.calls[2]).toEqual([
      "/api/v1/orders",
      expect.objectContaining({ headers: { Authorization: "Bearer restored-access" } })
    ]);
  });
});