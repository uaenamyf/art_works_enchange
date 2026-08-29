import { describe, expect, it } from "vitest";
import { checkoutSchema } from "./checkoutSchema";

/** 锁定实体与数字商品不同的地址不变量，避免隐藏字段重新阻塞数字结算。 */
describe("checkoutSchema", () => {
  it("accepts a digital order without shipping fields", () => {
    const result = checkoutSchema.safeParse({ quantity: 1, productType: "DIGITAL" });

    expect(result.success).toBe(true);
  });

  it("requires a complete address for a physical order", () => {
    const result = checkoutSchema.safeParse({ quantity: 1, productType: "PHYSICAL" });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.flatten().fieldErrors).toMatchObject({
        recipientName: ["请输入收货人"],
        line1: ["请输入地址"],
        countryCode: ["请输入两位国家代码"]
      });
    }
  });

  it("accepts a valid physical order", () => {
    const result = checkoutSchema.safeParse({
      quantity: 2,
      productType: "PHYSICAL",
      recipientName: "Alice",
      line1: "1 Main Street",
      city: "Shanghai",
      postalCode: "200000",
      countryCode: "CN",
      phone: "13800138000"
    });

    expect(result.success).toBe(true);
  });
});