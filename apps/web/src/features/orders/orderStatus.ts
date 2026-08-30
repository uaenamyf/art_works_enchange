import type { components } from "@/lib/api/schema.gen";

type OrderStatus = components["schemas"]["OrderStatus"];

export interface OrderStatusCopy {
  title: string;
  detail: string;
  tone: "neutral" | "success" | "warning";
}

/** 订单结果态文案只根据服务端状态展示，不在客户端推断或推进订单状态。 */
export function getOrderStatusCopy(status: OrderStatus): OrderStatusCopy {
  switch (status) {
    case "PENDING_PAYMENT":
      return {
        title: "订单已创建，等待支付",
        detail: "订单已保留，支付渠道接入后可继续完成付款。",
        tone: "warning"
      };
    case "PAID":
      return {
        title: "支付已确认",
        detail: "订单已进入创作者处理流程。",
        tone: "success"
      };
    case "PROCESSING":
      return {
        title: "订单处理中",
        detail: "创作者正在准备你的订单。",
        tone: "neutral"
      };
    case "SHIPPED":
      return {
        title: "订单已发货",
        detail: "物流信息会在创作者补充后显示。",
        tone: "success"
      };
    case "COMPLETED":
      return {
        title: "订单已完成",
        detail: "感谢支持这位创作者。",
        tone: "success"
      };
    case "CANCELLED":
      return {
        title: "订单已取消",
        detail: "该订单不会继续进入支付或履约流程。",
        tone: "warning"
      };
    case "REFUNDING":
      return {
        title: "退款处理中",
        detail: "退款完成后，订单状态会更新。",
        tone: "warning"
      };
    case "REFUNDED":
      return {
        title: "退款已完成",
        detail: "该订单款项已退回原支付渠道。",
        tone: "neutral"
      };
  }
}