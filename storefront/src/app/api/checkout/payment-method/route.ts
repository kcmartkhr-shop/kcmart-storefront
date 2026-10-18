import { NextResponse } from "next/server";

import { auth } from "@/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type PaymentRequest = {
  fulfillmentType?: unknown;
  paymentMethod?: unknown;
};

function getText(value: unknown): string {
  return typeof value === "string"
    ? value.trim()
    : "";
}

export async function POST(request: Request) {
  const session = await auth();

  const customerEmail =
    session?.user?.email
      ?.trim()
      .toLowerCase();

  if (!customerEmail) {
    return NextResponse.json(
      {
        error:
          "Please sign in before continuing checkout.",
      },
      {
        status: 401,
      }
    );
  }

  let body: PaymentRequest;

  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      {
        error: "Invalid payment request.",
      },
      {
        status: 400,
      }
    );
  }

  const fulfillmentType =
    getText(body.fulfillmentType);

  const paymentMethod =
    getText(body.paymentMethod);

  if (
    fulfillmentType !== "DELIVERY" &&
    fulfillmentType !== "STORE_PICKUP"
  ) {
    return NextResponse.json(
      {
        error: "Invalid fulfillment type.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    fulfillmentType === "DELIVERY" &&
    paymentMethod !== "ONLINE"
  ) {
    return NextResponse.json(
      {
        error:
          "Home Delivery supports Online Payment only.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    fulfillmentType === "STORE_PICKUP" &&
    paymentMethod !== "ONLINE" &&
    paymentMethod !== "PAY_AT_STORE"
  ) {
    return NextResponse.json(
      {
        error:
          "Invalid payment method for Store Pickup.",
      },
      {
        status: 400,
      }
    );
  }

  const backendUrl =
    process.env.KCMART_API_URL ??
    "http://localhost:8080";

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!internalKey) {
    return NextResponse.json(
      {
        error:
          "Checkout service is not configured.",
      },
      {
        status: 503,
      }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/checkout/payment-method`,
      {
        method: "POST",

        headers: {
          "Content-Type":
            "application/json",

          "X-KCMART-Internal-Key":
            internalKey,
        },

        body: JSON.stringify({
          customerEmail,
          fulfillmentType,
          paymentMethod,
        }),

        cache: "no-store",
      }
    );

    let result:
      | {
          valid?: boolean;
          fulfillmentType?: string;
          paymentMethod?: string;
        }
      | undefined;

    try {
      result = await response.json();
    } catch {
      result = undefined;
    }

    if (response.status === 401) {
      return NextResponse.json(
        {
          error:
            "Your customer account could not be verified.",
        },
        {
          status: 401,
        }
      );
    }

    if (response.status === 403) {
      return NextResponse.json(
        {
          error:
            "Your customer account cannot checkout.",
        },
        {
          status: 403,
        }
      );
    }

    if (response.status === 400) {
      return NextResponse.json(
        {
          error:
            "The selected payment method is not allowed.",
        },
        {
          status: 400,
        }
      );
    }

    if (
      !response.ok ||
      !result?.valid
    ) {
      return NextResponse.json(
        {
          error:
            "Payment method could not be verified.",
        },
        {
          status: 502,
        }
      );
    }

    return NextResponse.json({
      valid: true,
      fulfillmentType:
        result.fulfillmentType,
      paymentMethod:
        result.paymentMethod,
    });

  } catch {
    return NextResponse.json(
      {
        error:
          "Checkout service is temporarily unavailable.",
      },
      {
        status: 503,
      }
    );
  }
}