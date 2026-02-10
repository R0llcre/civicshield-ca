import { describe, expect, it } from "vitest";

import { cn } from "@/lib/cn";

describe("cn", () => {
  it("merges classnames", () => {
    expect(cn("a", false && "b", undefined, "c")).toBe("a c");
  });
});

