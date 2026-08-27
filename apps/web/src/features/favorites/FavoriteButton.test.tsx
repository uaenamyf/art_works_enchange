import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { FavoriteButton } from "./FavoriteButton";

const authenticatedFetch = vi.fn();
let currentSession: unknown = null;

vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({ session: currentSession, authenticatedFetch })
}));

/** 以用户视角验证收藏按钮的认证门禁、乐观状态和失败回滚。 */
describe("FavoriteButton", () => {
  beforeEach(() => {
    currentSession = null;
    authenticatedFetch.mockReset();
  });

  it("asks anonymous users to login without sending a request", async () => {
    render(<FavoriteButton artworkId="artwork-1" />);
    fireEvent.click(screen.getByRole("button", { name: "收藏作品" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("请先登录");
    expect(authenticatedFetch).not.toHaveBeenCalled();
  });

  it("optimistically favorites and sends a PUT request", async () => {
    currentSession = { accessToken: "access" };
    authenticatedFetch.mockResolvedValue({ favorited: true, favoriteCount: 1 });
    render(<FavoriteButton artworkId="artwork-1" />);

    fireEvent.click(screen.getByRole("button", { name: "收藏作品" }));

    expect(screen.getByRole("button", { name: "取消收藏" })).toBeInTheDocument();
    await waitFor(() =>
      expect(authenticatedFetch).toHaveBeenCalledWith("/api/v1/favorites/artwork-1", { method: "PUT" })
    );
  });

  it("rolls back the optimistic state when the request fails", async () => {
    currentSession = { accessToken: "access" };
    authenticatedFetch.mockRejectedValue(new Error("network"));
    render(<FavoriteButton artworkId="artwork-1" />);

    fireEvent.click(screen.getByRole("button", { name: "收藏作品" }));

    await waitFor(() => expect(screen.getByRole("button", { name: "收藏作品" })).toBeInTheDocument());
    expect(screen.getByRole("alert")).toHaveTextContent("收藏失败");
  });
});