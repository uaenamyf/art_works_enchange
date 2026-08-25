import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { Button } from "./Button";

describe("Button", () => {
  it("renders children and forwards aria attributes", () => {
    render(<Button aria-label="提交">提交</Button>);
    const btn = screen.getByRole("button", { name: "提交" });
    expect(btn).toBeInTheDocument();
  });

  it("disables while loading", () => {
    render(<Button loading>保存</Button>);
    const btn = screen.getByRole("button", { name: "保存" });
    expect(btn).toBeDisabled();
  });
});
