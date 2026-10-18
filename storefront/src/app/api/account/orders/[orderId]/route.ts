import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/auth";

export const dynamic = "force-dynamic";

type RouteContext = {
  params: Promise<{
    orderId: string;
  }>;
};

export async function GET(
  _request: NextRequest,
  context: RouteContext
) {
  const session = await auth();

  const customerEmail =
    session?.user?.email
      ?.trim()
      .toLowerCase();

  if (!customerEmail) {
    return NextResponse.json(
      { error: "Authentication required." },
      { status: 401 }
    );
  }

  const { orderId } = await context.params;

  if (!/^[1-9]\d*$/.test(orderId)) {
    return NextResponse.json(
      { error: "Order not found." },
      { status: 404 }
    );
  }

  const backendUrl =
    process.env.KCMART_API_URL
      ?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {
    console.error(
      "KCMART order detail service configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Order details are temporarily unavailable.",
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer/${orderId}`,
      {
        method: "POST",

        headers: {
          "Content-Type": "application/json",
          "X-KCMART-Internal-Key":
            internalKey,
        },

        /*
         * SECURITY:
         *
         * Customer identity comes only from
         * the server-side Auth.js session.
         *
         * The browser supplies only the order ID.
         *
         * Spring verifies that the requested
         * order belongs to this customer.
         */
        body: JSON.stringify({
          email: customerEmail,
        }),

        cache: "no-store",
      }
    );

    if (response.status === 404) {
      return NextResponse.json(
        { error: "Order not found." },
        { status: 404 }
      );
    }

    if (response.status === 403) {
      return NextResponse.json(
        { error: "Account unavailable." },
        { status: 403 }
      );
    }

    if (!response.ok) {
      console.error(
        `KCMART customer order detail backend returned HTTP ${response.status}.`
      );

      return NextResponse.json(
        {
          error:
            "Unable to load order details.",
        },
        { status: 502 }
      );
    }

    const order = await response.json();

    return NextResponse.json(
      { order },
      {
        status: 200,
        headers: {
          "Cache-Control":
            "private, no-store, max-age=0",
        },
      }
    );
  } catch (error) {
    console.error(
      "KCMART customer order detail request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the order service.",
      },
      { status: 502 }
    );
  }
}
export async function PATCH(
  request: NextRequest,
  context: RouteContext
) {
  const session = await auth();

  const customerEmail =
    session?.user?.email
      ?.trim()
      .toLowerCase();

  if (!customerEmail) {
    return NextResponse.json(
      { error: "Authentication required." },
      { status: 401 }
    );
  }

  const { orderId } = await context.params;

  if (!/^[1-9]\d*$/.test(orderId)) {
    return NextResponse.json(
      { error: "Order not found." },
      { status: 404 }
    );
  }

  let body: unknown;

  try {
    body = await request.json();
  } catch {
    body = {};
  }

  let note: string | null = null;

  if (
    body !== null &&
    typeof body === "object" &&
    "note" in body &&
    typeof (body as { note?: unknown }).note === "string"
  ) {
    const value =
      (body as { note: string }).note.trim();

    if (value.length > 1000) {
      return NextResponse.json(
        {
          error:
            "Cancellation reason must not exceed 1000 characters.",
        },
        { status: 400 }
      );
    }

    note = value || null;
  }

  const backendUrl =
    process.env.KCMART_API_URL
      ?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {
    console.error(
      "KCMART customer cancellation service configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Order cancellation is temporarily unavailable.",
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer/${orderId}/cancel`,
      {
        method: "PATCH",

        headers: {
          "Content-Type": "application/json",
          "X-KCMART-Internal-Key":
            internalKey,
        },

        /*
         * SECURITY:
         *
         * The browser never supplies the trusted
         * customer identity.
         *
         * Email comes only from the server-side
         * Auth.js session.
         */
        body: JSON.stringify({
          email: customerEmail,
          note,
        }),

        cache: "no-store",
      }
    );

    if (response.status === 404) {
      return NextResponse.json(
        { error: "Order not found." },
        { status: 404 }
      );
    }

    if (response.status === 403) {
      return NextResponse.json(
        { error: "Account unavailable." },
        { status: 403 }
      );
    }

    if (response.status === 409) {
      let message =
        "This order cannot be cancelled.";

      try {
        const backendError =
          await response.json();

        if (
          backendError &&
          typeof backendError === "object" &&
          "message" in backendError &&
          typeof backendError.message === "string" &&
          backendError.message.trim()
        ) {
          message =
            backendError.message.trim();
        }
      } catch {
        // Keep the safe fallback message.
      }

      return NextResponse.json(
        { error: message },
        { status: 409 }
      );
    }

    if (response.status === 400) {
      return NextResponse.json(
        { error: "Invalid cancellation request." },
        { status: 400 }
      );
    }

    if (!response.ok) {
      console.error(
        `KCMART customer cancellation backend returned HTTP ${response.status}.`
      );

      return NextResponse.json(
        {
          error:
            "Unable to cancel this order.",
        },
        { status: 502 }
      );
    }

    const order = await response.json();

    return NextResponse.json(
      { order },
      {
        status: 200,
        headers: {
          "Cache-Control":
            "private, no-store, max-age=0",
        },
      }
    );
  } catch (error) {
    console.error(
      "KCMART customer cancellation request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the order service.",
      },
      { status: 502 }
    );
  }
}