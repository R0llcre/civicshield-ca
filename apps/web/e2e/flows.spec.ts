import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

async function expectNoSeriousA11yViolations(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  const serious = results.violations.filter((v) => v.impact === "serious" || v.impact === "critical");
  expect(serious, JSON.stringify(serious, null, 2)).toEqual([]);
}

test("verify flow works in demo mode", async ({ page }) => {
  await page.goto("/verify");

  await page.getByLabel("Name").fill("Diaz");
  await page.getByRole("button", { name: "Search rosters" }).click();

  await expect(page.getByText("Diaz, Samantha", { exact: true })).toBeVisible();
  await expectNoSeriousA11yViolations(page);
});

test("help flow works in demo mode", async ({ page }) => {
  await page.goto("/help");

  await page.getByLabel("County").selectOption("Los Angeles");
  await page.getByRole("button", { name: "Find organizations" }).click();

  await expect(page.getByText("Sample Legal Aid (Demo)", { exact: true })).toBeVisible();
  await expectNoSeriousA11yViolations(page);
});

test("shield page is accessible baseline", async ({ page }) => {
  await page.goto("/shield");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Shield");
  await expectNoSeriousA11yViolations(page);
});
