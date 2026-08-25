/**
 * RFC 9457 Problem Details 基础解析。
 *
 * 后端 GlobalExceptionHandler 会返回 type/title/status/detail/code 等字段；
 * 这里只关心稳定 code 与 message，instance 透传到日志。
 */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  fieldErrors?: Record<string, string>;
}

export function isProblemDetails(value: unknown): value is ProblemDetails {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const v = value as Record<string, unknown>;
  return (
    typeof v.type === "string" ||
    typeof v.title === "string" ||
    typeof v.status === "number" ||
    typeof v.detail === "string" ||
    typeof v.code === "string"
  );
}

export function humanizeProblem(problem: ProblemDetails | null | undefined): string {
  if (!problem) {
    return "请求失败，请稍后再试。";
  }
  if (problem.code && problem.detail) {
    return `${problem.code}: ${problem.detail}`;
  }
  return problem.detail ?? problem.title ?? "请求失败，请稍后再试。";
}
