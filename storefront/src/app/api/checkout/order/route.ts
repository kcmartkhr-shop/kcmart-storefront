import { auth } from "@/auth";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type BrowserItem = {
  productId: number;
  quantity: number;
};

function getText(value: unknown): string {
  return typeof value === "string"
    ? value.trim()
    : "";
}

function validItems(
  value: unknown
): value is BrowserItem[] {

  if (
    !Array.isArray(value) ||
    value.length < 1 ||
    value.length > 50
  ) {
    return false;
  }

  const seen = new Set<number>();

  for (const item of value) {

    if (
      typeof item !== "object" ||
      item === null
    ) {
      return false;
    }

    const candidate =
      item as Record<string, unknown>;

    const productId =
      candidate.productId;

    const quantity =
      candidate.quantity;

    if (
      typeof productId !== "number" ||
      !Number.isSafeInteger(productId) ||
      productId <= 0
    ) {
      return false;
    }

    if (
      typeof quantity !== "number" ||
      !Number.isSafeInteger(quantity) ||
      quantity < 1 ||
      quantity > 10
    ) {
      return false;
    }

    if (seen.has(productId)) {
      return false;
    }

    seen.add(productId);
  }

  return true;
}

function validIdempotencyKey(
  value: string
): boolean {

  /*
   * Browser order attempts will use crypto.randomUUID().
   * Restrict this bridge to UUID format rather than accepting
   * arbitrary browser-controlled idempotency strings.
   */
  return /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    .test(value);
}

export async function POST(
  request: Request
) {

  const session = await auth();

  const customerEmail =
    session?.user?.email?.trim();

  if (!customerEmail) {
    return NextResponse.json(
      {
        error: "Authentication required.",
      },
      {
        status: 401,
      }
    );
  }

  let body: Record<string, unknown>;

  try {

    const parsed: unknown =
      await request.json();

    if (
      typeof parsed !== "object" ||
      parsed === null ||
      Array.isArray(parsed)
    ) {
      throw new Error("Invalid body");
    }

    body =
      parsed as Record<string, unknown>;

  } catch {

    return NextResponse.json(
      {
        error: "Invalid request body.",
      },
      {
        status: 400,
      }
    );
  }

  const idempotencyKey =
    getText(body.idempotencyKey);

  const fulfillmentType =
    getText(body.fulfillmentType);

  const paymentMethod =
    getText(body.paymentMethod);

  const customerName =
    getText(body.customerName);

  const customerPhone =
    getText(body.customerPhone);

  const addressLine1 =
    getText(body.addressLine1);

  const addressLine2 =
    getText(body.addressLine2);

  const landmark =
    getText(body.landmark);

  const city =
    getText(body.city);

  const state =
    getText(body.state);

  const postalCode =
    getText(body.postalCode);

  const country =
    getText(body.country);

  const items = body.items;

  /*
   * Supported production checkout paths:
   *
   * DELIVERY + ONLINE
   * STORE_PICKUP + ONLINE
   * STORE_PICKUP + PAY_AT_STORE
   *
   * DELIVERY + PAY_AT_STORE remains disabled.
   */
  const validCheckoutPath =
    (fulfillmentType === "DELIVERY" &&
      paymentMethod === "ONLINE") ||
    (fulfillmentType === "STORE_PICKUP" &&
      (paymentMethod === "ONLINE" ||
        paymentMethod === "PAY_AT_STORE"));

  if (!validCheckoutPath) {
    return NextResponse.json(
      {
        error:
          "The selected delivery and payment method combination is not available.",
      },
      {
        status: 400,
      }
    );
  }

  if (fulfillmentType === "DELIVERY") {
    if (
      addressLine1.length < 5 ||
      addressLine1.length > 255
    ) {
      return NextResponse.json(
        {
          error: "Please enter a valid delivery address.",
        },
        {
          status: 400,
        }
      );
    }

    if (
      city.length < 2 ||
      city.length > 120
    ) {
      return NextResponse.json(
        {
          error: "Please enter a valid city.",
        },
        {
          status: 400,
        }
      );
    }

    if (
      state.length < 2 ||
      state.length > 120
    ) {
      return NextResponse.json(
        {
          error: "Please enter a valid state.",
        },
        {
          status: 400,
        }
      );
    }

    if (!/^[1-9][0-9]{5}$/.test(postalCode)) {
      return NextResponse.json(
        {
          error: "Please enter a valid 6-digit PIN code.",
        },
        {
          status: 400,
        }
      );
    }

    if (country !== "India") {
      return NextResponse.json(
        {
          error: "Delivery is currently available only in India.",
        },
        {
          status: 400,
        }
      );
    }
  }

  if (!validIdempotencyKey(idempotencyKey)) {
    return NextResponse.json(
      {
        error:
          "Invalid order attempt identifier.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    customerName.length < 2 ||
    customerName.length > 255
  ) {
    return NextResponse.json(
      {
        error: "Invalid customer name.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    !/^[6-9][0-9]{9}$/.test(
      customerPhone
    )
  ) {
    return NextResponse.json(
      {
        error: "Invalid mobile number.",
      },
      {
        status: 400,
      }
    );
  }

  if (!validItems(items)) {
    return NextResponse.json(
      {
        error: "Invalid cart.",
      },
      {
        status: 400,
      }
    );
  }

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!internalKey) {
    return NextResponse.json(
      {
        error:
          "Order service is not configured.",
      },
      {
        status: 503,
      }
    );
  }

  const apiBase =
    process.env.KCMART_API_URL ??
    "http://localhost:8080";

  try {

    const response = await fetch(
      `${apiBase}/api/v1/internal/checkout/order`,
      {
        method: "POST",

        headers: {
          "Content-Type": "application/json",
          "X-KCMART-Internal-Key":
            internalKey,
        },

        body: JSON.stringify({
          /*
           * SECURITY:
           * customerEmail comes from Auth.js.
           * It is never trusted from browser JSON.
           */
          customerEmail,
          idempotencyKey,
          fulfillmentType,
          paymentMethod,
          customerName,
          customerPhone,

          /*
           * Delivery address is forwarded only for DELIVERY.
           * Pickup orders intentionally send null address fields.
           */
          addressLine1:
            fulfillmentType === "DELIVERY"
              ? addressLine1
              : null,
          addressLine2:
            fulfillmentType === "DELIVERY"
              ? addressLine2
              : null,
          landmark:
            fulfillmentType === "DELIVERY"
              ? landmark
              : null,
          city:
            fulfillmentType === "DELIVERY"
              ? city
              : null,
          state:
            fulfillmentType === "DELIVERY"
              ? state
              : null,
          postalCode:
            fulfillmentType === "DELIVERY"
              ? postalCode
              : null,
          country:
            fulfillmentType === "DELIVERY"
              ? country
              : null,

          items,
        }),

        cache: "no-store",
      }
    );

    let result: unknown;

    try {
      result = await response.json();
    } catch {
      result = undefined;
    }

    if (!response.ok) {

      let message =
        "Unable to create order.";

      if (
        typeof result === "object" &&
        result !== null &&
        "message" in result &&
        typeof (
          result as {
            message?: unknown;
          }
        ).message === "string"
      ) {
        message = (
          result as {
            message: string;
          }
        ).message;
      }

      /*
       * Preserve useful validation/conflict/auth statuses,
       * but do not expose internal implementation details.
       */
      const status =
        response.status === 400 ||
        response.status === 401 ||
        response.status === 403 ||
        response.status === 409
          ? response.status
          : 502;

      return NextResponse.json(
        {
          error: message,
        },
        {
          status,
        }
      );
    }

    if (
      typeof result !== "object" ||
      result === null
    ) {
      return NextResponse.json(
        {
          error:
            "Invalid response from order service.",
        },
        {
          status: 502,
        }
      );
    }

    const order =
      result as Record<string, unknown>;

    /*
     * Return only the order confirmation fields needed by
     * the storefront. No internal key or customer email.
     */
    return NextResponse.json({
      orderId: order.orderId,
      orderNumber: order.orderNumber,
      fulfillmentType:
        order.fulfillmentType,
      paymentMethod:
        order.paymentMethod,
      paymentStatus:
        order.paymentStatus,
      orderStatus:
        order.orderStatus,
      subtotal: order.subtotal,
      deliveryCharge:
        order.deliveryCharge,
      totalAmount:
        order.totalAmount,
      idempotentReplay:
        order.idempotentReplay,
    });

  } catch {

    return NextResponse.json(
      {
        error:
          "Order service is temporarily unavailable.",
      },
      {
        status: 502,
      }
    );
  }
}
