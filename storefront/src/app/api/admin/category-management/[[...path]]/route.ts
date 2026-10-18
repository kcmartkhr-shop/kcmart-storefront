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
  const session = getAdminSession(
    request.cookies.get("kcmart_admin_session")?.value
  );

  if (!session) {
    return NextResponse.json(
      { error: "Unauthorized" },
      { status: 401 }
    );
  }

  const { path = [] } = await context.params;

  if (
    path.length > 1 ||
    path.some((part) => !/^\d+$/.test(part))
  ) {
    return NextResponse.json(
      { error: "Invalid category path" },
      { status: 400 }
    );
  }

  const allowed =
    path.length === 0
      ? ["GET", "POST"]
      : ["GET", "PUT", "DELETE"];

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
    `${api}/api/v1/admin/categories` +
    (path.length ? `/${path[0]}` : "");

  try {
    const body =
      request.method === "POST" ||
      request.method === "PUT"
        ? await request.text()
        : undefined;

    const response = await fetch(endpoint, {
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

    if (response.status === 204) {
      return new NextResponse(null, {
        status: 204,
        headers: {
          "Cache-Control": "no-store",
        },
      });
    }

    const responseBody = await response.text();

    return new NextResponse(responseBody, {
      status: response.status,
      headers: {
        "Content-Type":
          response.headers.get("Content-Type") ??
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
export const DELETE = handle;