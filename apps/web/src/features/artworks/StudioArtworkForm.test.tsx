import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StudioArtworkForm } from "./StudioArtworkForm";
import { LocaleProvider } from "@/features/locale/LocaleProvider";

const replace = vi.fn();
const authenticatedFetch = vi.fn();
let currentSession: unknown = null;

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("@/features/auth/AuthProvider", () => ({
  useAuth: () => ({ session: currentSession, loading: false, authenticatedFetch })
}));

function renderWithLocale() {
  return render(
    <LocaleProvider>
      <StudioArtworkForm />
    </LocaleProvider>
  );
}

/** 以用户视角验证作品提交前必须选择媒体，避免创建无内容审核任务。 */
describe("StudioArtworkForm", () => {
  beforeEach(() => {
    replace.mockReset();
    authenticatedFetch.mockReset();
    currentSession = { user: { role: "CREATOR" }, accessToken: "access" };
  });

  it("blocks submission when no artwork image is selected", async () => {
    renderWithLocale();

    fireEvent.change(screen.getByLabelText("作品标题"), { target: { value: "A new artwork" } });
    fireEvent.change(screen.getByLabelText("作品描述"), {
      target: { value: "A sufficiently long artwork description" }
    });
    fireEvent.change(screen.getByLabelText("价格"), { target: { value: "89" } });
    fireEvent.click(screen.getByRole("button", { name: /提交作品|Submit artwork/ }));

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("至少选择一张"));
    expect(authenticatedFetch).not.toHaveBeenCalled();
  });

  it("uploads and confirms a digital asset for digital products", async () => {
    vi.stubGlobal("crypto", {
      subtle: { digest: vi.fn().mockResolvedValue(new ArrayBuffer(32)) }
    });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
    authenticatedFetch.mockResolvedValue({
      id: "artwork-id",
      version: 0,
      mediaId: "media-id",
      uploadUrl: "https://upload.example/asset",
      method: "PUT",
      contentType: "application/octet-stream",
      versionId: "asset-version-id"
    });
    renderWithLocale();

    fireEvent.click(screen.getByDisplayValue("DIGITAL"));
    fireEvent.change(screen.getByLabelText("作品标题"), {
      target: { value: "A digital artwork" }
    });
    fireEvent.change(screen.getByLabelText("作品描述"), {
      target: { value: "A sufficiently long digital artwork description" }
    });
    fireEvent.change(screen.getByLabelText("价格"), { target: { value: "89" } });
    fireEvent.change(screen.getByLabelText("作品图片（最多 12 张）"), {
      target: { files: [new File(["cover"], "cover.png", { type: "image/png" })] }
    });
    fireEvent.change(screen.getByLabelText("数字资产文件"), {
      target: { files: [new File(["asset"], "bundle.zip", { type: "application/zip" })] }
    });
    fireEvent.click(screen.getByRole("button", { name: /提交作品|Submit artwork/ }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/studio/artworks/artwork-id"));
    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/digital-assets/upload-requests",
      expect.objectContaining({ method: "POST" })
    );
    expect(authenticatedFetch).toHaveBeenCalledWith(
      "/api/v1/digital-assets/asset-version-id/confirm",
      { method: "POST" }
    );
  });
});