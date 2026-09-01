import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LocaleProvider } from "@/features/locale/LocaleProvider";
import { StudioArtworkEditor } from "./StudioArtworkEditor";

const authenticatedFetch = vi.fn();

vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({ session: { user: { role: "CREATOR" } }, loading: false, authenticatedFetch })
}));

function renderEditor() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <LocaleProvider><StudioArtworkEditor artworkId="artwork-id" /></LocaleProvider>
    </QueryClientProvider>
  );
}

/** 验证审核通过后 Creator 可以从 Studio 进入公开发布状态。 */
describe("StudioArtworkEditor", () => {
  beforeEach(() => {
    authenticatedFetch.mockReset();
    authenticatedFetch.mockImplementation((path: string) => {
      if (path === "/api/v1/studio/artworks/artwork-id") {
        return Promise.resolve({
          id: "artwork-id",
          title: "Approved artwork",
          description: "A sufficiently long approved artwork description",
          category: "ART",
          priceAmount: "89.00",
          priceCurrency: "USD",
          stock: 2,
          mediaIds: ["media-id"],
          status: "APPROVED",
          version: 1
        });
      }
      if (path.endsWith("/reviews")) return Promise.resolve([]);
      return Promise.resolve({ id: "artwork-id", status: "PUBLISHED" });
    });
  });

  it("publishes an approved artwork", async () => {
    renderEditor();

    await waitFor(() => expect(screen.getByRole("button", { name: "发布作品" })).toBeVisible());
    fireEvent.click(screen.getByRole("button", { name: "发布作品" }));

    await waitFor(() => expect(screen.getByRole("status")).toHaveTextContent("作品已发布"));
    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/artworks/artwork-id/publish",
      { method: "POST" }
    );
  });
});
