import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Context = {
  params: Promise<{ pincode: string }>;
};

export async function GET(
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
      {
        status: 401,
        headers: { "Cache-Control": "no-store" },
      }
    );
  }

  const { pincode } = await context.params;

  if (!/^[1-9][0-9]{5}$/.test(pincode)) {
    return NextResponse.json(
      { error: "Invalid Indian pincode" },
      {
        status: 400,
        headers: { "Cache-Control": "no-store" },
      }
    );
  }

  const token = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const result = await fetch(
      `${api}/api/v1/admin/delhivery/serviceability/${pincode}`,
      {
        method: "GET",
        headers: {
          Authorization: `Basic ${token}`,
        },
        cache: "no-store",
        signal: AbortSignal.timeout(20000),
      }
    );

    const noStore = { "Cache-Control": "no-store" };

    if (!result.ok) {
      const status =
        result.status === 401 ||
        result.status === 403 ||
        result.status === 503
          ? result.status
          : 502;

      return NextResponse.json(
        { error: "Serviceability could not be confirmed" },
        { status, headers: noStore }
      );
    }

    const contentType =
      result.headers.get("content-type") ?? "";

    if (!/^application\/(?:[\w.+-]*\+)?json(?:\s*;|$)/i.test(contentType)) {
      return NextResponse.json(
        { error: "Invalid serviceability response" },
        { status: 502, headers: noStore }
      );
    }

    const declaredLength = Number(
      result.headers.get("content-length")
    );

    if (
      Number.isFinite(declaredLength) &&
      declaredLength > 262144
    ) {
      return NextResponse.json(
        { error: "Serviceability response too large" },
        { status: 502, headers: noStore }
      );
    }

    const reader = result.body?.getReader();

    if (!reader) {
      return NextResponse.json(
        { error: "Invalid serviceability response" },
        { status: 502, headers: noStore }
      );
    }

    const chunks: Uint8Array[] = [];
    let total = 0;

    while (true) {
      const { done, value } = await reader.read();

      if (done) break;

      total += value.byteLength;

      if (total > 262144) {
        await reader.cancel();
        return NextResponse.json(
          { error: "Serviceability response too large" },
          { status: 502, headers: noStore }
        );
      }

      chunks.push(value);
    }

    const bytes = new Uint8Array(total);
    let offset = 0;

    for (const chunk of chunks) {
      bytes.set(chunk, offset);
      offset += chunk.byteLength;
    }

    let data: unknown;

    try {
      data = JSON.parse(
        new TextDecoder("utf-8", { fatal: true }).decode(bytes)
      );
    } catch {
      return NextResponse.json(
        { error: "Invalid serviceability response" },
        { status: 502, headers: noStore }
      );
    }

    return NextResponse.json(data, {
      status: 200,
      headers: noStore,
    });
  } catch {
    return NextResponse.json(
      { error: "Serviceability service unavailable" },
      {
        status: 503,
        headers: { "Cache-Control": "no-store" },
      }
    );
  }
}