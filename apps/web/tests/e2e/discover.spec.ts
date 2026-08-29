import { expect, test } from "@playwright/test";

test("discover opens artwork detail and searches its category", async ({ page }) => {
  await page.goto("/");
  const firstArtwork = page.locator("article").first();
  await expect(firstArtwork).toBeVisible();

  const artworkTitle = await firstArtwork.getByRole("heading", { level: 3 }).innerText();
  await firstArtwork.getByRole("link", { name: /查看作品/ }).click();
  await expect(page).toHaveURL(/\/artworks\/[0-9a-f-]+$/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(artworkTitle);

  await page.getByRole("link", { name: "返回发现" }).click();
  await expect(page).toHaveURL("/");
  await page.getByRole("link", { name: "查看全部" }).first().click();
  await expect(page).toHaveURL(/\/categories\/[A-Z]+$/);

  await page.getByRole("textbox", { name: "搜索此分类" }).fill(artworkTitle);
  await page.getByRole("button", { name: "提交搜索" }).click();
  await expect(page).toHaveURL(/\/categories\/[A-Z]+\?query=/);
  await expect(page.locator("article").first()).toContainText(artworkTitle);
});