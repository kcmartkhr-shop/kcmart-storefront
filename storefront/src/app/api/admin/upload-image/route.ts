import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const session = getAdminSession(
    request.cookies.get("kcmart_admin_session")?.value
  );

  if (!session) {
    return NextResponse.json(
      { error: "Unauthorized" },
      { status: 401 }
    );
  }

  // Require a same-origin request for this cookie-authenticated action.
  const origin = request.headers.get("origin");
  if (!origin || origin !== request.nextUrl.origin) {
    return NextResponse.json(
      { error: "Invalid request origin" },
      { status: 403 }
    );
  }

  try {
    const data = await request.formData();
    const productId = String(data.get("productId") ?? "");
    const file = data.get("file");

    if (
      !/^[1-9][0-9]*$/.test(productId) ||
      !(file instanceof File)
    ) {
      return NextResponse.json(
        { error: "Select a product and image" },
        { status: 400 }
      );
    }

    if (
      file.size === 0 ||
      file.size > 4_000_000 ||
      !["image/jpeg", "image/png", "image/webp"].includes(file.type)
    ) {
      return NextResponse.json(
        { error: "Use JPG, PNG or WebP up to 4 MB" },
        { status: 400 }
      );
    }

    const upload = new FormData();
    upload.append("file", file);

    const api =
      process.env.KCMART_API_URL ??
      "http://127.0.0.1:8080";

    const token = Buffer.from(
      `${session.username}:${session.password}`
    ).toString("base64");

    const response = await fetch(
      `${api}/api/v1/admin/products/${productId}/image`,
      {
        method: "POST",
        headers: {
          Authorization: `Basic ${token}`,
        },
        body: upload,
        cache: "no-store",
      }
    );

    const body = await response.text();

    return new NextResponse(body, {
      status: response.status,
      headers: {
        "Content-Type": "application/json",
        "Cache-Control": "no-store",
      },
    });
  } catch {
    return NextResponse.json(
      { error: "Image upload failed" },
      { status: 500 }
    );
  }
}