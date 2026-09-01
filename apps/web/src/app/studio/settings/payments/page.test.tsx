import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LocaleProvider } from "@/features/locale/LocaleProvider";
import StudioPaymentSettingsPage from "./page";

const authenticatedFetch = vi.fn();

vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({
    session: { user: { role: "CREATOR" } },
    loading: false,
    authenticatedFetch
  })
}));

describe("StudioPaymentSettingsPage", () => {
  beforeEach(() => {
    authenticatedFetch.mockReset();
    authenticatedFetch.mockResolvedValue({ configured: false, merchantId: null });
  });

  it("loads and saves the Creator PayPal merchant ID", async () => {
    authenticatedFetch
      .mockResolvedValueOnce({ configured: false, merchantId: null })
      .mockResolvedValueOnce({ configured: true, merchantId: "merchant-123" });

    render(<LocaleProvider><StudioPaymentSettingsPage /></LocaleProvider>);

    await waitFor(() => expect(screen.getByRole("textbox")).toBeInTheDocument());
    fireEvent.change(screen.getByRole("textbox"), { target: { value: "merchant-123" } });
    fireEvent.click(screen.getByRole("button", { name: "保存 PayPal 收款方式" }));

    await waitFor(() => expect(screen.getByText("PayPal 收款方式已保存。")).toBeInTheDocument());
    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/me/creator/paypal-account",
      { method: "PUT", body: { merchantId: "merchant-123" } }
    );
  });

  it("keeps saving disabled until a merchant ID is entered", async () => {
    render(<LocaleProvider><StudioPaymentSettingsPage /></LocaleProvider>);

    await waitFor(() => expect(screen.getByRole("textbox")).toBeInTheDocument());
    expect(screen.getByRole("button", { name: "保存 PayPal 收款方式" })).toBeDisabled();
  });
});
