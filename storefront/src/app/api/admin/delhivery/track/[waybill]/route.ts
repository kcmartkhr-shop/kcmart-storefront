import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Context = {
  params: Promise<{ waybill: string }>;
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

  const { waybill } = await context.params;

  if (!/^[A-Za-z0-9-]{5,50}$/.test(waybill)) {
    return NextResponse.json(
      { error: "Invalid tracking number" },
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
      `${api}/api/v1/admin/delhivery/track/${waybill}`,
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
        { error: "Tracking request could not be completed" },
        { status, headers: noStore }
      );
    }

    const contentType =
      result.headers.get("content-type") ?? "";

    if (!/^application\/(?:[\w.+-]*\+)?json(?:\s*;|$)/i.test(contentType)) {
      return NextResponse.json(
        { error: "Invalid tracking service response" },
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
        { error: "Tracking response too large" },
        { status: 502, headers: noStore }
      );
    }

    const reader = result.body?.getReader();

    if (!reader) {
      return NextResponse.json(
        { error: "Invalid tracking service response" },
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
          { error: "Tracking response too large" },
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
        { error: "Invalid tracking service response" },
        { status: 502, headers: noStore }
      );
    }

    return NextResponse.json(data, {
      status: 200,
      headers: noStore,
    });
  } catch {
    return NextResponse.json(
      { error: "Tracking service unavailable" },
      {
        status: 503,
        headers: { "Cache-Control": "no-store" },
      }
    );
  }
}