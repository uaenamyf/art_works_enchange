import { describe, expect, it } from "vitest";
import { humanizeProblem, isProblemDetails } from "./problem";

describe("problem details", () => {
  it("detects a problem-like object", () => {
    expect(isProblemDetails({ code: "EMAIL_TAKEN", detail: "dup" })).toBe(true);
    expect(isProblemDetails({ detail: "x" })).toBe(true);
    expect(isProblemDetails(null)).toBe(false);
    expect(isProblemDetails("oops")).toBe(false);
  });

  it("humanizes a problem with code and detail", () => {
    const out = humanizeProblem({ code: "EMAIL_TAKEN", detail: "邮箱已注册" });
    expect(out).toContain("EMAIL_TAKEN");
    expect(out).toContain("邮箱已注册");
  });

  it("falls back gracefully for empty input", () => {
    expect(humanizeProblem(null)).toBeTruthy();
    expect(humanizeProblem(undefined)).toBeTruthy();
  });
});
