import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/auth";

export const dynamic = "force-dynamic";

type RouteContext = {
  params: Promise<{
    orderId: string;
  }>;
};

export async function POST(
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
      "KCMART Razorpay payment service configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Online payment is temporarily unavailable.",
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer/${orderId}/payment/razorpay`,
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
         * The browser never supplies the trusted
         * customer identity.
         *
         * Customer email comes only from the
         * server-side Auth.js session.
         *
         * Spring verifies order ownership and
         * determines the trusted payment amount
         * from the database.
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

    if (response.status === 409) {
      let message =
        "Online payment cannot be started for this order.";

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
        // Keep safe fallback.
      }

      return NextResponse.json(
        { error: message },
        { status: 409 }
      );
    }

    if (response.status === 400) {
      return NextResponse.json(
        { error: "Invalid payment request." },
        { status: 400 }
      );
    }

    if (response.status === 502) {
      return NextResponse.json(
        {
          error:
            "Unable to create the payment order.",
        },
        { status: 502 }
      );
    }

    if (!response.ok) {
      console.error(
        `KCMART Razorpay backend returned HTTP ${response.status}.`
      );

      return NextResponse.json(
        {
          error:
            "Online payment is temporarily unavailable.",
        },
        { status: 502 }
      );
    }

    const paymentOrder =
      await response.json();

    return NextResponse.json(
      { paymentOrder },
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
      "KCMART Razorpay payment request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the payment service.",
      },
      { status: 502 }
    );
  }
}