import { describe, expect, it } from "vitest";
import { formatPrice } from "./format";

describe("format helpers", () => {
  it("formats CNY price in yuan", () => {
    const out = formatPrice(18800, "CNY");
    expect(out).toContain("188");
  });
});
