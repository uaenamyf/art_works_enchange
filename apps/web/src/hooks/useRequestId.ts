/**
 * 简单的请求 ID 工具。
 * 客户端在发起请求时给每一次 mutation/query 注入一个 UUID；
 * 出现错误时把它和 Problem Details 一起显示，便于日志检索与售后排查。
 */
import { useId } from "react";

export function generateRequestId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `req-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

/**
 * 组件内使用：返回当前 React 树的稳定 ID，作为同次渲染的请求关联。
 */
export function useRequestId(): string {
  return useId();
}
