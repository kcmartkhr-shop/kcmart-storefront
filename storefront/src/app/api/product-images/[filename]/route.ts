import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";

const API_URL =
  process.env.KCMART_API_URL ?? "http://localhost:8080";

export async function GET(
  _request: NextRequest,
  context: { params: Promise<{ filename: string }> }
) {
  const { filename } = await context.params;

  // Only accept generated image filenames, never arbitrary URLs.
  if (!/^[a-zA-Z0-9_-]+\.(png|jpg|jpeg|webp)$/i.test(filename)) {
    return new NextResponse("Invalid filename", { status: 400 });
  }

  const upstream = await fetch(
    `${API_URL}/api/v1/product-images/${filename}`,
    {
      cache: "no-store",
      signal: AbortSignal.timeout(10000),
    }
  ).catch(() => null);

  if (!upstream || !upstream.ok) {
    return new NextResponse("Image unavailable", { status: 502 });
  }

  const contentType = upstream.headers.get("content-type") ?? "";

  if (!/^image\/(png|jpeg|webp)$/i.test(contentType)) {
    return new NextResponse("Invalid image response", {
      status: 502,
    });
  }

  return new NextResponse(upstream.body, {
    status: 200,
    headers: {
      "Content-Type": contentType,
      "Cache-Control": "public, max-age=300",
      "X-Content-Type-Options": "nosniff",
    },
  });
}