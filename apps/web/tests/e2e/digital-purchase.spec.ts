import { expect, test } from "@playwright/test";

const email = process.env.CREATORHUB_E2E_EMAIL;
const password = process.env.CREATORHUB_E2E_PASSWORD;
const artworkId = process.env.CREATORHUB_E2E_DIGITAL_ARTWORK_ID;

test.skip(
  !email || !password || !artworkId,
  "需要 CREATORHUB_E2E_EMAIL、CREATORHUB_E2E_PASSWORD 和 CREATORHUB_E2E_DIGITAL_ARTWORK_ID"
);

test("digital checkout hides shipping fields and creates a digital order", async ({ page }) => {
  await page.goto("/auth/login");
  await page.getByLabel("邮箱").fill(email!);
  await page.getByLabel("密码").fill(password!);
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page).toHaveURL("/");

  await page.goto(`/checkout/${artworkId}`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("确认订单");
  await expect(page.getByText("数字交付")).toBeVisible();
  await expect(page.getByRole("heading", { name: "收货信息" })).toHaveCount(0);
  await expect(page.getByLabel("收货人")).toHaveCount(0);
  await expect(page.getByLabel("地址")).toHaveCount(0);

  await page.getByRole("button", { name: "创建数字订单" }).click();
  await expect(page).toHaveURL(/\/orders\/[0-9a-f-]+$/);
  await expect(page.getByText("状态：PENDING_PAYMENT")).toBeVisible();
});