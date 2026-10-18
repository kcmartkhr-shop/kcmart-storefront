import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";

type RouteContext = {
  params: Promise<{
    productId: string;
    path?: string[];
  }>;
};

function sameOrigin(request: NextRequest) {
  const origin = request.headers.get("origin");
  return Boolean(origin && origin === request.nextUrl.origin);
}

function backendAuth(username: string, password: string) {
  const token = Buffer.from(
    `${username}:${password}`
  ).toString("base64");

  return `Basic ${token}`;
}

function proxyResponse(
  body: string,
  status: number
) {
  return new NextResponse(body, {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

export async function GET(
  request: NextRequest,
  context: RouteContext
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

  const { productId, path } = await context.params;

  if (
    !/^[1-9][0-9]*$/.test(productId) ||
    (path && path.length > 0)
  ) {
    return NextResponse.json(
      { error: "Invalid gallery request" },
      { status: 400 }
    );
  }

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const response = await fetch(
      `${api}/api/v1/admin/products/${productId}/images`,
      {
        headers: {
          Authorization: backendAuth(
            session.username,
            session.password
          ),
        },
        cache: "no-store",
      }
    );

    return proxyResponse(
      await response.text(),
      response.status
    );
  } catch {
    return NextResponse.json(
      { error: "Unable to load product photos" },
      { status: 502 }
    );
  }
}

export async function PATCH(
  request: NextRequest,
  context: RouteContext
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

  if (!sameOrigin(request)) {
    return NextResponse.json(
      { error: "Invalid request origin" },
      { status: 403 }
    );
  }

  const { productId, path } = await context.params;

  if (
    !/^[1-9][0-9]*$/.test(productId) ||
    !path ||
    path.length !== 2 ||
    !/^[1-9][0-9]*$/.test(path[0]) ||
    path[1] !== "cover"
  ) {
    return NextResponse.json(
      { error: "Invalid cover request" },
      { status: 400 }
    );
  }

  const imageId = path[0];

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const response = await fetch(
      `${api}/api/v1/admin/products/${productId}/images/${imageId}/cover`,
      {
        method: "PATCH",
        headers: {
          Authorization: backendAuth(
            session.username,
            session.password
          ),
        },
        cache: "no-store",
      }
    );

    return proxyResponse(
      await response.text(),
      response.status
    );
  } catch {
    return NextResponse.json(
      { error: "Unable to change cover photo" },
      { status: 502 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  context: RouteContext
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

  if (!sameOrigin(request)) {
    return NextResponse.json(
      { error: "Invalid request origin" },
      { status: 403 }
    );
  }

  const { productId, path } = await context.params;

  if (
    !/^[1-9][0-9]*$/.test(productId) ||
    !path ||
    path.length !== 1 ||
    !/^[1-9][0-9]*$/.test(path[0])
  ) {
    return NextResponse.json(
      { error: "Invalid delete request" },
      { status: 400 }
    );
  }

  const imageId = path[0];

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  try {
    const response = await fetch(
      `${api}/api/v1/admin/products/${productId}/images/${imageId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: backendAuth(
            session.username,
            session.password
          ),
        },
        cache: "no-store",
      }
    );

    if (response.status === 204) {
      return new NextResponse(null, {
        status: 204,
        headers: {
          "Cache-Control": "no-store",
        },
      });
    }

    const body = await response.text();

    return proxyResponse(
      body,
      response.status
    );
  } catch {
    return NextResponse.json(
      { error: "Unable to delete product photo" },
      { status: 502 }
    );
  }
}