import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import NotificationsPage from "./page";
import { LocaleProvider } from "@/features/locale/LocaleProvider";

const authenticatedFetch = vi.fn();
let currentSession: unknown = null;

vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({ session: currentSession, loading: false, authenticatedFetch })
}));

describe("NotificationsPage", () => {
  beforeEach(() => {
    authenticatedFetch.mockReset();
    currentSession = { user: { displayName: "Alice", role: "USER" }, accessToken: "access" };
  });

  it("loads notifications, filters unread items, and marks one read", async () => {
    authenticatedFetch.mockResolvedValue({
      items: [
        {
          id: "notification-1",
          type: "ORDER_CREATED",
          title: "订单已创建",
          body: "你的订单已创建。",
          link: "/orders/order-1",
          readAt: null,
          createdAt: "2026-08-28T00:00:00Z"
        }
      ],
      nextCursor: null,
      hasMore: false,
      unreadCount: 1
    });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <LocaleProvider>
          <NotificationsPage />
        </LocaleProvider>
      </QueryClientProvider>
    );

    await waitFor(() => expect(screen.getByText("订单已创建")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("checkbox", { name: "只看未读" }));
    await waitFor(() =>
      expect(authenticatedFetch).toHaveBeenCalledWith(
        "/api/v1/notifications?limit=20&unreadOnly=true"
      )
    );
    fireEvent.click(screen.getByRole("button", { name: "标记已读" }));
    await waitFor(() =>
      expect(authenticatedFetch).toHaveBeenCalledWith(
        "/api/v1/notifications/notification-1/read",
        { method: "POST" }
      )
    );
  });
});