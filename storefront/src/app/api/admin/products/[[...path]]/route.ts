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
      { status: 401 }
    );
  }

  const { path = [] } = await context.params;

  if (
    path.length > 2 ||
    path.some((part) => !/^[a-zA-Z0-9_-]+$/.test(part))
  ) {
    return NextResponse.json(
      { error: "Invalid product path" },
      { status: 400 }
    );
  }

  const allowed =
    path.length === 0
      ? ["GET", "POST"]
      : path.length === 1
        ? ["GET", "PUT"]
        : path[1] === "inventory"
          ? ["PUT"]
          : path[1] === "active"
            ? ["PATCH"]
            : [];

  if (!allowed.includes(request.method)) {
    return NextResponse.json(
      { error: "Method not allowed" },
      { status: 405 }
    );
  }

  const token = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  const endpoint =
    `${api}/api/v1/admin/products` +
    (path.length ? `/${path.join("/")}` : "") +
    (request.nextUrl.search || "");

  try {
    const body =
      request.method === "GET"
        ? undefined
        : await request.text();

    const result = await fetch(endpoint, {
      method: request.method,
      headers: {
        Authorization: `Basic ${token}`,
        ...(body
          ? { "Content-Type": "application/json" }
          : {}),
      },
      body,
      cache: "no-store",
    });

    const responseBody = await result.text();

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
      { status: 503 }
    );
  }
}

export const GET = handle;
export const POST = handle;
export const PUT = handle;
export const PATCH = handle;