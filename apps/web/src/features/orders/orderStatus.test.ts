import { describe, expect, it } from "vitest";
import { getOrderStatusCopy } from "./orderStatus";

/** 锁定订单结果态的用户可见反馈，防止新增状态落回含糊的通用提示。 */
describe("getOrderStatusCopy", () => {
  it("describes payment waiting without pretending payment succeeded", () => {
    expect(getOrderStatusCopy("PENDING_PAYMENT")).toEqual({
      title: "订单已创建，等待支付",
      detail: "订单已保留，支付渠道接入后可继续完成付款。",
      tone: "warning"
    });
  });

  it("describes active fulfillment states", () => {
    expect(getOrderStatusCopy("PAID").title).toBe("支付已确认");
    expect(getOrderStatusCopy("PROCESSING").title).toBe("订单处理中");
    expect(getOrderStatusCopy("SHIPPED").title).toBe("订单已发货");
    expect(getOrderStatusCopy("COMPLETED").title).toBe("订单已完成");
  });

  it("describes cancellation and refund terminal states", () => {
    expect(getOrderStatusCopy("CANCELLED").title).toBe("订单已取消");
    expect(getOrderStatusCopy("REFUNDING").title).toBe("退款处理中");
    expect(getOrderStatusCopy("REFUNDED").title).toBe("退款已完成");
  });
});