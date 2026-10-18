import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  const session = getAdminSession(
    request.cookies.get("kcmart_admin_session")?.value
  );

  if (!session) {
    return NextResponse.json(
      { error: "Unauthorized" },
      { status: 401 }
    );
  }

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const response = await fetch(
      `${api}/api/v1/categories`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return NextResponse.json(
        { error: "Unable to load categories" },
        { status: 502 }
      );
    }

    return NextResponse.json(
      await response.json(),
      {
        headers: {
          "Cache-Control": "no-store",
        },
      }
    );
  } catch {
    return NextResponse.json(
      { error: "Backend unavailable" },
      { status: 503 }
    );
  }
}