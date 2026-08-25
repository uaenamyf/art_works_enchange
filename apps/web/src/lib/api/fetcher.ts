/**
 * 基础 fetcher 封装。
 *
 * 后续在 OpenAPI 客户端生成后，TanStack Query 的 queryFn / mutationFn
 * 应通过本文件调用，避免在组件中散落原始 fetch、Token 处理与状态码判断。
 *
 * 错误统一映射为 {@link ProblemDetails}，调用方按 code 路由错误消息。
 */
import { generateRequestId } from "@/hooks/useRequestId";
import type { ProblemDetails } from "./problem";

export interface ApiFetchOptions extends Omit<RequestInit, "headers" | "body"> {
  /**
   * 请求体，会被 JSON.stringify 后以 application/json 发送。
   * 类型上接受任意可序列化值；调用方需要自己保证 JSON 安全。
   */
  body?: unknown;
  headers?: Record<string, string>;
  /** 注入到请求头的 Request ID（来自 useRequestId 或调用方生成）。 */
  requestId?: string;
}

export class ApiError extends Error {
  readonly problem: ProblemDetails;
  readonly status: number;
  readonly requestId: string | null;

  constructor(message: string, problem: ProblemDetails, status: number, requestId: string | null) {
    super(message);
    this.name = "ApiError";
    this.problem = problem;
    this.status = status;
    this.requestId = requestId;
  }
}

export function getApiBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
}

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const requestId = options.requestId ?? generateRequestId();
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Request-Id": requestId,
    ...options.headers
  };

  let body: BodyInit | undefined;
  if (options.body !== undefined) {
    headers["Content-Type"] ??= "application/json";
    // JSON.stringify 的重载不接受 unknown；显式 narrow 到标准可序列化值
    body = JSON.stringify(options.body as Parameters<typeof JSON.stringify>[0]);
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    method: options.method ?? "GET",
    headers,
    body,
    credentials: "include"
  });

  if (!response.ok) {
    let problem: ProblemDetails = {};
    try {
      problem = (await response.json()) as ProblemDetails;
    } catch {
      problem = { detail: response.statusText };
    }
    throw new ApiError(
      problem.detail ?? response.statusText,
      problem,
      response.status,
      response.headers.get("X-Request-Id") ?? requestId
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
