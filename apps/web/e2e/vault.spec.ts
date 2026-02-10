import { expect, test } from "@playwright/test";

test("evidence vault export/import roundtrip (chromium only)", async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== "chromium", "File download handling is most reliable in chromium.");

  const passphrase = "correct horse battery staple";

  await page.goto("/shield");

  await page.getByLabel("Passphrase").fill(passphrase);
  await page.getByRole("button", { name: "Create" }).click();

  const fileInput = page.locator('input[type="file"][multiple]');
  await expect(fileInput).toBeVisible();
  await fileInput.setInputFiles("e2e/fixtures/sample.txt");

  await expect(page.getByText("sample.txt", { exact: true })).toBeVisible();

  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export vault (zip)" }).click();
  const download = await downloadPromise;

  const zipPath = testInfo.outputPath("vault.zip");
  await download.saveAs(zipPath);

  await page.getByRole("button", { name: "Lock" }).click();

  await page.getByRole("button", { name: "Import vault (zip)" }).click();
  const importDialog = page.getByRole("dialog");
  await expect(importDialog).toBeVisible();

  await importDialog.locator('input[type="file"]').setInputFiles(zipPath);
  await importDialog.getByPlaceholder("••••••••").fill(passphrase);
  await importDialog.getByRole("button", { name: "Import", exact: true }).click();

  await expect(page.getByText("sample.txt", { exact: true })).toBeVisible();
});

test("evidence vault can import android fixture zip (chromium only)", async ({ page }, testInfo) => {
  test.skip(testInfo.project.name !== "chromium", "File handling is most reliable in chromium.");

  const passphrase = "android-fixture-passphrase";

  await page.goto("/shield");

  await page.getByRole("button", { name: "Import vault (zip)" }).click();
  const importDialog = page.getByRole("dialog");
  await expect(importDialog).toBeVisible();

  await importDialog.locator('input[type="file"]').setInputFiles("e2e/fixtures/vault_android_fixture.zip");
  await importDialog.getByPlaceholder("••••••••").fill(passphrase);
  await importDialog.getByRole("button", { name: "Import", exact: true }).click();

  await expect(page.getByText("android-note.txt", { exact: true })).toBeVisible();
});
