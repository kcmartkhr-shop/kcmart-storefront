import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/auth";

export const dynamic = "force-dynamic";

type RouteContext = {
  params: Promise<{
    orderId: string;
  }>;
};

type VerificationBody = {
  razorpay_order_id?: unknown;
  razorpay_payment_id?: unknown;
  razorpay_signature?: unknown;
};

export async function POST(
  request: NextRequest,
  context: RouteContext
) {
  const session = await auth();

  const customerEmail =
    session?.user?.email?.trim().toLowerCase();

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

  let body: VerificationBody;

  try {
    body = (await request.json()) as VerificationBody;
  } catch {
    return NextResponse.json(
      { error: "Invalid payment response." },
      { status: 400 }
    );
  }

  const razorpayOrderId =
    typeof body.razorpay_order_id === "string"
      ? body.razorpay_order_id.trim()
      : "";

  const razorpayPaymentId =
    typeof body.razorpay_payment_id === "string"
      ? body.razorpay_payment_id.trim()
      : "";

  const razorpaySignature =
    typeof body.razorpay_signature === "string"
      ? body.razorpay_signature.trim()
      : "";

  if (
    !razorpayOrderId ||
    !razorpayPaymentId ||
    !razorpaySignature
  ) {
    return NextResponse.json(
      { error: "Incomplete payment response." },
      { status: 400 }
    );
  }

  const backendUrl =
    process.env.KCMART_API_URL?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {
    console.error(
      "KCMART payment verification configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Payment verification is temporarily unavailable.",
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer/${orderId}/payment/razorpay/verify`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-KCMART-Internal-Key": internalKey,
        },

        /*
         * SECURITY:
         * Customer identity comes from Auth.js.
         * The browser supplies only Razorpay callback values.
         * Spring performs the authoritative verification.
         */
        body: JSON.stringify({
          email: customerEmail,
          razorpayOrderId,
          razorpayPaymentId,
          razorpaySignature,
        }),

        cache: "no-store",
      }
    );

    let backendBody: unknown = null;

    try {
      backendBody = await response.json();
    } catch {
      // Keep safe fallback below.
    }

    if (!response.ok) {
      if (
        response.status === 409 &&
        response.headers.get("X-KCMART-Payment-Outcome") ===
          "RECONCILIATION_REQUIRED"
      ) {
        return NextResponse.json(
          {
            verified: false,
            outcome: "RECONCILIATION_REQUIRED",
            error:
              "Payment was captured, but the order expired. Your payment is under review.",
          },
          {
            status: 409,
            headers: {
              "Cache-Control": "private, no-store, max-age=0",
            },
          }
        );
      }

      let message = "Unable to verify payment.";

      if (
        backendBody &&
        typeof backendBody === "object" &&
        "message" in backendBody &&
        typeof backendBody.message === "string" &&
        backendBody.message.trim()
      ) {
        message = backendBody.message.trim();
      }

      return NextResponse.json(
        { error: message },
        { status: response.status }
      );
    }

    return NextResponse.json(
      {
        verified: true,
        payment: backendBody,
      },
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
      "KCMART Razorpay verification request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the payment verification service.",
      },
      { status: 502 }
    );
  }
}