import { describe, expect, it } from "vitest";

describe("KCMART frontend testing setup", () => {
  it("executes a basic assertion", () => {
    expect(2 + 2).toBe(4);
  });

  it("supports package dimension calculations", () => {
    const length = 30;
    const width = 20;
    const height = 10;

    expect(length * width * height).toBe(6000);
  });

  it("rejects invalid package weight values", () => {
    const isValidWeight = (weight: number) =>
      Number.isSafeInteger(weight) && weight > 0;

    expect(isValidWeight(1200)).toBe(true);
    expect(isValidWeight(0)).toBe(false);
    expect(isValidWeight(-100)).toBe(false);
    expect(isValidWeight(1.5)).toBe(false);
  });
});
