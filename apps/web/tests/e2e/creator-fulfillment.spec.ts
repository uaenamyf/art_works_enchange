import { expect, test } from "@playwright/test";

const email = process.env.CREATORHUB_E2E_EMAIL;
const password = process.env.CREATORHUB_E2E_PASSWORD;
const orderId = process.env.CREATORHUB_E2E_PAID_ORDER_ID;

test.skip(
  !email || !password || !orderId,
  "需要 CREATORHUB_E2E_EMAIL、CREATORHUB_E2E_PASSWORD 和 CREATORHUB_E2E_PAID_ORDER_ID"
);

test("creator fulfillment advances a paid order to shipped", async ({ page }) => {
  await page.goto("/auth/login");
  await page.getByLabel("邮箱").fill(email!);
  await page.getByLabel("密码").fill(password!);
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page).toHaveURL("/");

  await page.goto("/studio/orders?status=PAID");
  const order = page.locator(`a[href="/orders/${orderId}"]`);
  await expect(order).toBeVisible();
  await expect(order.locator(".." ).getByText("PAID")).toBeVisible();
  await order.locator(".." ).getByRole("button", { name: "接单" }).click();
  await expect(page.getByText("PROCESSING")).toBeVisible();

  await page.getByRole("button", { name: "标记发货" }).click();
  await expect(page.getByText("SHIPPED")).toBeVisible();
});