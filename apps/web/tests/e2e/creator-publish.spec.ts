import { expect, request, test, type APIResponse } from "@playwright/test";

const adminEmail = process.env.CREATORHUB_E2E_ADMIN_EMAIL;
const adminPassword = process.env.CREATORHUB_E2E_ADMIN_PASSWORD;

test.skip(!adminEmail || !adminPassword, "需要 CREATORHUB_E2E_ADMIN_EMAIL 和 CREATORHUB_E2E_ADMIN_PASSWORD");

/** 使用真实注册、上传、审核和发布流程验证 Creator/Admin 跨角色闭环。 */
test("creator publish crosses admin review boundary", async ({ browser }) => {
  test.setTimeout(120_000);
  const fixture = await createPendingArtwork();
  const adminContext = await browser.newContext();
  const creatorContext = await browser.newContext();
  const adminPage = await adminContext.newPage();
  const creatorPage = await creatorContext.newPage();

  try {
    await adminPage.goto("/auth/login");
    await adminPage.getByLabel("邮箱").fill(adminEmail!);
    await adminPage.getByLabel("密码").fill(adminPassword!);
    await adminPage.getByRole("button", { name: "登录" }).click();
    await expect(adminPage).toHaveURL("/");

    await adminPage.goto("/admin/artworks/review");
    const reviewLink = adminPage.locator(`a[href="/admin/artworks/review/${fixture.artworkId}"]`);
    await expect(reviewLink).toBeVisible({ timeout: 15_000 });
    await adminPage.goto(`/admin/artworks/review/${fixture.artworkId}`);
    await expect(adminPage.getByRole("heading", { name: "审核作品" })).toBeVisible();
    await adminPage.getByLabel("审核理由").fill("符合平台内容规范");
    await adminPage.getByRole("button", { name: "批准并进入发布" }).click();
    await expect(adminPage).toHaveURL("/admin/artworks/review");

    await creatorPage.goto("/auth/login");
    await creatorPage.getByLabel("邮箱").fill(fixture.creatorEmail);
    await creatorPage.getByLabel("密码").fill(fixture.creatorPassword);
    await creatorPage.getByRole("button", { name: "登录" }).click();
    await expect(creatorPage).toHaveURL("/");

    await creatorPage.goto(`/studio/artworks/${fixture.artworkId}`);
    await expect(creatorPage.getByRole("button", { name: "发布作品" })).toBeVisible();
    await creatorPage.getByRole("button", { name: "发布作品" }).click();
    await expect(creatorPage.getByText("作品已发布")).toBeVisible();

    await creatorPage.goto(`/artworks/${fixture.artworkId}`);
    await expect(creatorPage.getByRole("heading", { level: 1 })).toBeVisible();
  } finally {
    await adminContext.close();
    await creatorContext.close();
  }
});

async function createPendingArtwork() {
  const api = await request.newContext({
    baseURL: process.env.CREATORHUB_E2E_API_URL ?? "http://localhost:8080",
    extraHTTPHeaders: { Origin: "http://localhost:3000" }
  });
  const creatorEmail = `creator-e2e-${Date.now()}@creatorhub.test`;
  const creatorPassword = "CreatorHubE2E!2026";
  try {
    const registered = await api.post("/api/v1/auth/register", {
      data: { email: creatorEmail, password: creatorPassword, displayName: "E2E Creator" }
    });
    await expectResponse(registered, "注册 Creator");
    const session = (await registered.json()) as { accessToken: string };
    const headers = { Authorization: `Bearer ${session.accessToken}` };

    const applied = await api.post("/api/v1/me/creator", {
      headers,
      data: {
        displayName: "E2E Creator Studio",
        slug: `creator-e2e-${Date.now()}`,
        category: "ART",
        bio: "Creator used by the local end-to-end test."
      }
    });
    await expectResponse(applied, "申请 Creator");
    const refreshed = await api.post("/api/v1/auth/refresh");
    await expectResponse(refreshed, "刷新 Creator 会话");
    const creatorSession = (await refreshed.json()) as { accessToken: string };
    const creatorHeaders = { Authorization: `Bearer ${creatorSession.accessToken}` };

    const created = await api.post("/api/v1/artworks", {
      headers: creatorHeaders,
      data: {
        title: `E2E artwork ${Date.now()}`,
        description: "Artwork prepared by the cross-role end-to-end test.",
        category: "ART",
        productType: "PHYSICAL",
        price: { amount: "19.00", currency: "USD" },
        stock: 1,
        mediaIds: [],
        version: 0
      }
    });
    await expectResponse(created, "创建作品草稿");
    const artwork = (await created.json()) as { id: string; version: number };

    const uploadRequest = await api.post("/api/v1/media/upload-requests", {
      headers: creatorHeaders,
      data: {
        fileName: "e2e-cover.png",
        contentType: "image/png",
        sizeBytes: 46,
        purpose: "ARTWORK"
      }
    });
    await expectResponse(uploadRequest, "申请媒体上传");
    const upload = (await uploadRequest.json()) as { mediaId: string; uploadUrl: string; method: string; contentType: string };
    const objectUpload = await api.fetch(upload.uploadUrl, {
      method: upload.method,
      headers: { "Content-Type": upload.contentType },
      data: Buffer.from("e2e fixture image content that is not rendered")
    });
    await expectResponse(objectUpload, "上传作品媒体");

    const confirmed = await api.post(`/api/v1/media/${upload.mediaId}/confirm`, { headers: creatorHeaders });
    await expectResponse(confirmed, "确认作品媒体");
    const updated = await api.put(`/api/v1/artworks/${artwork.id}`, {
      headers: creatorHeaders,
      data: {
        title: `E2E artwork ${Date.now()}`,
        description: "Artwork prepared by the cross-role end-to-end test.",
        category: "ART",
        productType: "PHYSICAL",
        price: { amount: "19.00", currency: "USD" },
        stock: 1,
        mediaIds: [upload.mediaId],
        version: artwork.version
      }
    });
    await expectResponse(updated, "保存作品媒体");
    const submitted = await api.post(`/api/v1/artworks/${artwork.id}/submit-review`, { headers: creatorHeaders });
    await expectResponse(submitted, "提交作品审核");
    return { artworkId: artwork.id, creatorEmail, creatorPassword };
  } finally {
    await api.dispose();
  }
}

async function expectResponse(response: APIResponse, action: string) {
  const body = await response.text();
  expect(response.ok(), `${action}失败 (${response.status()}): ${body}`).toBeTruthy();
}
