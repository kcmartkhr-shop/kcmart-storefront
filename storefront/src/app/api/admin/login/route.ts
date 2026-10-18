import { NextRequest, NextResponse } from "next/server";
import { createAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  let body: { username?: string; password?: string };

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { error: "Invalid request" },
      { status: 400 }
    );
  }

  const username = body.username?.trim();
  const password = body.password;

  if (!username || !password) {
    return NextResponse.json(
      { error: "Username and password are required" },
      { status: 400 }
    );
  }

  const token = Buffer.from(
    `${username}:${password}`
  ).toString("base64");

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const result = await fetch(
      `${api}/api/v1/admin/products`,
      {
        headers: {
          Authorization: `Basic ${token}`,
        },
        cache: "no-store",
      }
    );

    if (!result.ok) {
      return NextResponse.json(
        { error: "Invalid admin credentials" },
        { status: 401 }
      );
    }

    const sessionId = createAdminSession(
      username,
      password
    );

    const response = NextResponse.json({
      success: true,
    });

    response.cookies.set(
      "kcmart_admin_session",
      sessionId,
      {
        httpOnly: true,
        sameSite: "strict",
        secure: process.env.NODE_ENV === "production",
        path: "/",
        maxAge: 8 * 60 * 60,
      }
    );

    return response;
  } catch {
    return NextResponse.json(
      { error: "Cannot connect to KCMART backend" },
      { status: 503 }
    );
  }
}