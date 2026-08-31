import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AdminReviewHistoryPage from "./page";

const authenticatedFetch = vi.fn();
let currentSession: unknown = null;

vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({ session: currentSession, loading: false, authenticatedFetch })
}));

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminReviewHistoryPage />
    </QueryClientProvider>
  );
}

describe("AdminReviewHistoryPage", () => {
  beforeEach(() => {
    authenticatedFetch.mockReset();
    currentSession = { user: { displayName: "Admin", role: "ADMIN" }, accessToken: "access" };
  });

  it("loads review history and requests the next cursor", async () => {
    authenticatedFetch
      .mockResolvedValueOnce({
        items: [
          {
            id: "review-1",
            artworkId: "artwork-1",
            artworkTitle: "春日版画",
            creator: { id: "creator-1", slug: "alice", displayName: "Alice Studio", category: "ART" },
            reviewerId: "reviewer-12345678",
            decision: "REJECTED",
            reason: "请补充作品信息。",
            createdAt: "2026-08-31T00:00:00Z"
          }
        ],
        nextCursor: "next-cursor",
        hasMore: true
      })
      .mockResolvedValueOnce({ items: [], nextCursor: null, hasMore: false });

    renderPage();

    await waitFor(() => expect(screen.getByText("春日版画")).toBeInTheDocument());
    expect(screen.getByText("请补充作品信息。")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "加载下一页" }));

    await waitFor(() =>
      expect(authenticatedFetch).toHaveBeenLastCalledWith(
        "/api/v1/admin/artworks/review-history?cursor=next-cursor&limit=20"
      )
    );
  });

  it("blocks non-admin users before querying history", () => {
    currentSession = { user: { displayName: "User", role: "USER" }, accessToken: "access" };

    renderPage();

    expect(screen.getByRole("alert")).toHaveTextContent("只有管理员可以访问此页面。");
    expect(authenticatedFetch).not.toHaveBeenCalled();
  });
});
