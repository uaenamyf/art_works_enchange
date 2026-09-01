import type { components } from "@/lib/api/schema.gen";

type OrderStatus = components["schemas"]["OrderStatus"];

export interface OrderStatusCopy {
  title: string;
  detail: string;
  tone: "neutral" | "success" | "warning";
}

/** 订单结果态文案只根据服务端状态展示，不在客户端推断或推进订单状态。 */
export function getOrderStatusCopy(status: OrderStatus, locale: "zh-CN" | "en-US" = "zh-CN"): OrderStatusCopy {
  const english = locale === "en-US";
  switch (status) {
    case "PENDING_PAYMENT":
      return {
        title: english ? "Order created, awaiting payment" : "订单已创建，等待支付",
        detail: english ? "Your order is reserved and can be paid when a payment method is connected." : "订单已保留，支付渠道接入后可继续完成付款。",
        tone: "warning"
      };
    case "PAID":
      return {
        title: english ? "Payment confirmed" : "支付已确认",
        detail: english ? "Your order has entered the creator's fulfillment process." : "订单已进入创作者处理流程。",
        tone: "success"
      };
    case "PROCESSING":
      return {
        title: english ? "Order processing" : "订单处理中",
        detail: english ? "The creator is preparing your order." : "创作者正在准备你的订单。",
        tone: "neutral"
      };
    case "SHIPPED":
      return {
        title: english ? "Order shipped" : "订单已发货",
        detail: english ? "Shipping details appear after the creator adds them." : "物流信息会在创作者补充后显示。",
        tone: "success"
      };
    case "COMPLETED":
      return {
        title: english ? "Order completed" : "订单已完成",
        detail: english ? "Thank you for supporting this creator." : "感谢支持这位创作者。",
        tone: "success"
      };
    case "CANCELLED":
      return {
        title: english ? "Order cancelled" : "订单已取消",
        detail: english ? "This order will not continue to payment or fulfillment." : "该订单不会继续进入支付或履约流程。",
        tone: "warning"
      };
    case "REFUNDING":
      return {
        title: english ? "Refund processing" : "退款处理中",
        detail: english ? "The order status will update when the refund is complete." : "退款完成后，订单状态会更新。",
        tone: "warning"
      };
    case "REFUNDED":
      return {
        title: english ? "Refund completed" : "退款已完成",
        detail: english ? "The order amount has been returned to the original payment method." : "该订单款项已退回原支付渠道。",
        tone: "neutral"
      };
  }
}