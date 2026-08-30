import { expect, test } from "@playwright/test";

const email = process.env.CREATORHUB_E2E_EMAIL;
const password = process.env.CREATORHUB_E2E_PASSWORD;
const artworkId = "0794f379-84ff-40f2-b23f-93603543e2f6";

test.skip(!email || !password, "需要 CREATORHUB_E2E_EMAIL 和 CREATORHUB_E2E_PASSWORD");

test("physical purchase creates a pending order and opens its detail", async ({ page }) => {
  await page.goto("/auth/login");
  await page.getByLabel("邮箱").fill(email!);
  await page.getByLabel("密码").fill(password!);
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page.getByText("Local Admin")).toBeVisible();

  await page.goto(`/checkout/${artworkId}`);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("确认订单");
  await page.getByLabel("收货人").fill("Alice");
  await page.getByLabel("地址").fill("1 Main Street");
  await page.getByLabel("城市").fill("Shanghai");
  await page.getByLabel("邮编").fill("200000");
  await page.getByLabel("国家代码").fill("CN");
  await page.getByLabel("手机号").fill("13800138000");
  await page.getByRole("button", { name: "创建待支付订单" }).click();

  await expect(page).toHaveURL(/\/orders\/[0-9a-f-]+$/);
  await expect(page.getByRole("heading", { name: "订单摘要" })).toBeVisible();
  await expect(page.getByText("状态：PENDING_PAYMENT")).toBeVisible();
  await expect(page.getByRole("heading", { name: "收货信息" })).toBeVisible();
});