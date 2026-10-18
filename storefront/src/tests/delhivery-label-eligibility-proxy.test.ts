/* @vitest-environment node */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NextRequest } from "next/server";

const mocks = vi.hoisted(() => ({
  getAdminSession: vi.fn(),
}));

vi.mock("../lib/admin/session", () => ({
  getAdminSession: mocks.getAdminSession,
}));

import {
  GET,
  POST,
  PATCH,
  PUT,
} from "../app/api/admin/orders/[[...path]]/route";

const path = ["123", "delhivery-label-eligibility"];

function request(method: string, pathname = path.join("/")) {
  return new NextRequest(
    `http://localhost:3000/api/admin/orders/${pathname}`,
    {
      method,
      headers: {
        cookie: "kcmart_admin_session=test-session",
      },
    }
  );
}

function context(parts = path) {
  return {
    params: Promise.resolve({ path: parts }),
  };
}

describe("Delhivery label eligibility admin proxy security", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    mocks.getAdminSession.mockReset();

    mocks.getAdminSession.mockReturnValue({
      username: "test-admin",
      password: "test-password",
    });

    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("rejects unauthenticated GET without forwarding", async () => {
    mocks.getAdminSession.mockReturnValue(null);

    const result = await GET(request("GET"), context());

    expect(result.status).toBe(401);
    expect(result.headers.get("Cache-Control")).toBe("no-store");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("forwards authorized GET with no-store", async () => {
    vi.stubEnv("KCMART_API_URL", "http://127.0.0.1:8080");

    fetchMock.mockResolvedValueOnce(
      new Response(
        JSON.stringify({ eligible: false, blocker: "Not booked." }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }
      )
    );

    const result = await GET(request("GET"), context());

    expect(result.status).toBe(200);
    expect(result.headers.get("Cache-Control")).toBe("no-store");

    expect(await result.json()).toEqual({
      eligible: false,
      blocker: "Not booked.",
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);

    const [url, options] = fetchMock.mock.calls[0];

    expect(url).toBe(
      "http://127.0.0.1:8080/api/v1/admin/orders/123/delhivery-label-eligibility"
    );

    expect(options.method).toBe("GET");
    expect(options.cache).toBe("no-store");
    expect(options.body).toBeUndefined();
    expect(options.headers.Authorization).toMatch(/^Basic /);
  });

  it.each([
    ["POST", POST],
    ["PATCH", PATCH],
    ["PUT", PUT],
  ])("blocks %s without forwarding", async (method, handler) => {
    const result = await handler(request(method), context());

    expect(result.status).toBe(405);
    expect(result.headers.get("Cache-Control")).toBe("no-store");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("rejects an invalid path without forwarding", async () => {
    const result = await GET(
      request("GET", "123/invalid!"),
      context(["123", "invalid!"])
    );

    expect(result.status).toBe(400);
    expect(result.headers.get("Cache-Control")).toBe("no-store");
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("returns 503 when backend is unavailable", async () => {
    fetchMock.mockRejectedValueOnce(new Error("Backend unavailable"));

    const result = await GET(request("GET"), context());

    expect(result.status).toBe(503);
    expect(result.headers.get("Cache-Control")).toBe("no-store");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});