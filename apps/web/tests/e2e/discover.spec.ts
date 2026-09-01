import { expect, test } from "@playwright/test";

test("discover opens artwork detail and searches its category", async ({ page }) => {
  await page.goto("/");
  await expect.poll(async () => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  const firstArtwork = page.locator("article").first();
  await expect(firstArtwork).toBeVisible();

  const artworkTitle = await firstArtwork.getByRole("heading", { level: 3 }).innerText();
  const artworkLink = firstArtwork.getByRole("link", { name: /查看作品|View artwork/ });
  await expect(artworkLink).toHaveAttribute("href", /\/artworks\/[0-9a-f-]+$/);
  await artworkLink.click();
  await expect(page).toHaveURL(/\/artworks\/[0-9a-f-]+$/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText(artworkTitle);

  await page.getByRole("link", { name: /返回发现|Back to discover/ }).click();
  await expect(page).toHaveURL("/");
  await page.getByRole("link", { name: /查看全部|View all/ }).first().click();
  await expect(page).toHaveURL(/\/categories\/[A-Z]+$/);

  await page.getByRole("textbox", { name: /搜索此分类|Search this category/ }).fill(artworkTitle);
  await page.getByRole("button", { name: /提交搜索|Submit search/ }).click();
  await expect(page).toHaveURL(/\/categories\/[A-Z]+\?query=/);
  await expect(page.locator("article").first()).toContainText(artworkTitle);
});