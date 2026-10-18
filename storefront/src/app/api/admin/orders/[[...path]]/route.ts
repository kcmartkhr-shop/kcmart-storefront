import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Context = {
  params: Promise<{ path?: string[] }>;
};

async function handle(
  request: NextRequest,
  context: Context
) {
  const sessionId = request.cookies.get(
    "kcmart_admin_session"
  )?.value;

  const session = getAdminSession(sessionId);

  if (!session) {
    return NextResponse.json(
      { error: "Unauthorized" },
      { status: 401, headers: { "Cache-Control": "no-store" } }
    );
  }

  const { path = [] } = await context.params;

  if (
    path.length > 3 ||
    path.some((part) => !/^[a-zA-Z0-9_-]+$/.test(part))
  ) {
    return NextResponse.json(
      { error: "Invalid order path" },
      { status: 400, headers: { "Cache-Control": "no-store" } }
    );
  }

  let allowed: string[] = [];

  if (path.length === 0) {
    allowed = ["GET"];
  } else if (
    path.length === 1 &&
    /^\d+$/.test(path[0])
  ) {
    allowed = ["GET"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "notifications"
  ) {
    allowed = ["GET"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "cancel"
  ) {
    allowed = ["PATCH"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "status"
  ) {
    allowed = ["PATCH"];
  } else if (
    path.length === 3 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "payment" &&
    path[2] === "received"
  ) {
    allowed = ["PATCH"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "invoice"
  ) {
    allowed = ["GET", "POST"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "shipment"
  ) {
    allowed = ["GET", "PUT"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "delhivery-package"
  ) {
    allowed = ["GET", "POST"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "delhivery-booking-preview"
  ) {
    allowed = ["GET"];
  } else if (
    path.length === 2 &&
    /^\d+$/.test(path[0]) &&
    path[1] === "delhivery-label-eligibility"
  ) {
    allowed = ["GET"];
  }

  if (!allowed.includes(request.method)) {
    return NextResponse.json(
      { error: "Method not allowed" },
      { status: 405, headers: { "Cache-Control": "no-store" } }
    );
  }

  const token = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  const endpoint =
    `${api}/api/v1/admin/orders` +
    (path.length ? `/${path.join("/")}` : "") +
    (request.nextUrl.search || "");

  try {
    const headers: Record<string, string> = {
      Authorization: `Basic ${token}`,
    };

    let body: BodyInit | undefined;

    if (request.method !== "GET") {
      const contentType =
        request.headers.get("content-type") ?? "";

      if (contentType.includes("multipart/form-data")) {
        body = await request.arrayBuffer();

        headers["Content-Type"] = contentType;
      } else {
        body = await request.text();

        if (body) {
          headers["Content-Type"] = "application/json";
        }
      }
    }

    const result = await fetch(endpoint, {
      method: request.method,
      headers,
      body,
      cache: "no-store",
    });

    const responseBody = await result.arrayBuffer();

    return new NextResponse(responseBody, {
      status: result.status,
      headers: {
        "Content-Type":
          result.headers.get("Content-Type") ??
          "application/json",
        "Cache-Control": "no-store",
      },
    });
  } catch {
    return NextResponse.json(
      { error: "Backend unavailable" },
      { status: 503, headers: { "Cache-Control": "no-store" } }
    );
  }
}

export const GET = handle;
export const POST = handle;
export const PATCH = handle;
export const PUT = handle;