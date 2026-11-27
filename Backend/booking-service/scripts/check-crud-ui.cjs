// Test navigateur facultatif : PLAYWRIGHT_MODULE peut pointer vers Playwright installe.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");
const path = require("node:path");

(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1050 } });
  page.setDefaultTimeout(60000);
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  let bookingId;
  async function submit(selector, method, endpoint) {
    const [response] = await Promise.all([
      page.waitForResponse((r) => r.request().method() === method && new URL(r.url()).pathname === endpoint),
      page.locator(selector).click()
    ]);
    assert.ok(response.ok(), `${method} ${endpoint}: ${response.status()}`);
    await page.waitForFunction(() => !document.querySelector("#refresh").disabled);
    return response.status() === 204 ? null : response.json();
  }
  try {
    await Promise.all([
      page.waitForResponse((r) => new URL(r.url()).pathname === "/dev/bookings"),
      page.goto("http://localhost:8084/crud")
    ]);
    await page.waitForFunction(() => !document.querySelector("#refresh").disabled);
    for (const [id, value] of Object.entries({ rideId: "501", driverId: "502", passengerId: "503", seats: "2", totalPrice: "18.50", changedBy: "503" })) {
      await page.locator(`#${id}`).fill(value);
    }
    const booking = await submit("#booking-submit", "POST", "/dev/bookings");
    bookingId = booking.id;
    const bookingRow = () => page.locator("#booking-rows tr").filter({ has: page.locator(`button[data-id="${bookingId}"]`) });
    await bookingRow().getByRole("button", { name: "Modifier", exact: true }).click();
    await page.locator("#status").selectOption("CONFIRMED");
    await page.locator("#changedBy").fill("503");
    await submit("#booking-submit", "PUT", `/dev/bookings/${bookingId}`);
    assert.equal((await bookingRow().locator(".badge").textContent()).trim(), "CONFIRMED");
    await page.screenshot({ path: path.join(__dirname, "../target/crud-bookings-desktop.png"), fullPage: true });

    await bookingRow().getByRole("button", { name: "Historique", exact: true }).click();
    await page.waitForFunction(() => !document.querySelector("#history-panel").hidden && document.querySelectorAll("#history-rows tr").length === 2);
    await page.locator("#previousStatus").selectOption("CONFIRMED");
    await page.locator("#newStatus").selectOption("COMPLETED");
    await page.locator("#history-author").fill("504");
    const history = await submit("#history-submit", "POST", `/dev/bookings/${bookingId}/history`);
    const historyRow = () => page.locator("#history-rows tr").filter({ has: page.locator(`button[data-id="${history.id}"]`) });
    await historyRow().getByRole("button", { name: "Modifier", exact: true }).click();
    await page.locator("#history-author").fill("505");
    await submit("#history-submit", "PUT", `/dev/bookings/${bookingId}/history/${history.id}`);
    assert.ok((await historyRow().textContent()).includes("#505"));
    await page.screenshot({ path: path.join(__dirname, "../target/crud-history-desktop.png"), fullPage: true });

    await historyRow().getByRole("button", { name: "Supprimer", exact: true }).click();
    await submit("#delete-dialog button[value=confirm]", "DELETE", `/dev/bookings/${bookingId}/history/${history.id}`);
    assert.equal(await historyRow().count(), 0);
    await page.locator("#bookings-tab").click();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: path.join(__dirname, "../target/crud-bookings-mobile.png"), fullPage: true });
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), "Mobile page must not overflow horizontally");
    await bookingRow().getByRole("button", { name: "Supprimer", exact: true }).click();
    await submit("#delete-dialog button[value=confirm]", "DELETE", `/dev/bookings/${bookingId}`);
    assert.equal(await bookingRow().count(), 0);
    assert.deepEqual(errors, []);
    console.log("PASS: Booking create/read/update/delete, automatic history, history create/read/update/delete, desktop/mobile layout, no browser errors.");
  } finally {
    if (bookingId) await fetch(`http://localhost:8084/dev/bookings/${bookingId}`, { method: "DELETE" }).catch(() => {});
    await browser.close();
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
