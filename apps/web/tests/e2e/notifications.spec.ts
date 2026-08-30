import { expect, test } from "@playwright/test";

const email = process.env.CREATORHUB_E2E_EMAIL;
const password = process.env.CREATORHUB_E2E_PASSWORD;

test.skip(!email || !password, "需要 CREATORHUB_E2E_EMAIL 和 CREATORHUB_E2E_PASSWORD");

test("authenticated user can open notifications and filter unread items", async ({ page }) => {
  await page.goto("/auth/login");
  await page.getByLabel("邮箱").fill(email!);
  await page.getByLabel("密码").fill(password!);
  await page.getByRole("button", { name: "登录" }).click();
  await expect(page).toHaveURL("/");

  await page.goto("/notifications");
  await expect(page.getByRole("heading", { name: "我的通知" })).toBeVisible();
  await expect(page.getByRole("checkbox", { name: "只看未读" })).toBeVisible();

  await page.getByRole("checkbox", { name: "只看未读" }).check();
  await expect(page.getByRole("checkbox", { name: "只看未读" })).toBeChecked();
  await expect(page.getByRole("heading", { name: "我的通知" })).toBeVisible();
});