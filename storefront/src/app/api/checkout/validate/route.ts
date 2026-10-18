import { auth } from "@/auth";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type BrowserCheckoutItem = {
  productId: number;
  quantity: number;
};

function validItems(
  value: unknown
): value is BrowserCheckoutItem[] {

  if (!Array.isArray(value)) {
    return false;
  }

  if (
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

    if (
      typeof candidate.productId !== "number" ||
      !Number.isSafeInteger(candidate.productId) ||
      candidate.productId <= 0
    ) {
      return false;
    }

    if (
      typeof candidate.quantity !== "number" ||
      !Number.isSafeInteger(candidate.quantity) ||
      candidate.quantity < 1 ||
      candidate.quantity > 10
    ) {
      return false;
    }

    if (seen.has(candidate.productId)) {
      return false;
    }

    seen.add(candidate.productId);
  }

  return true;
}

export async function POST(request: Request) {

  const session = await auth();

  const email =
    session?.user?.email?.trim();

  if (!email) {
    return NextResponse.json(
      {
        error: "Authentication required.",
      },
      {
        status: 401,
      }
    );
  }

  let body: unknown;

  try {
    body = await request.json();
  }
  catch {
    return NextResponse.json(
      {
        error: "Invalid JSON request.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    typeof body !== "object" ||
    body === null
  ) {
    return NextResponse.json(
      {
        error: "Invalid checkout request.",
      },
      {
        status: 400,
      }
    );
  }

  const browserBody =
    body as Record<string, unknown>;

  const fulfillmentType =
    browserBody.fulfillmentType;

  const items =
    browserBody.items;

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

  if (!validItems(items)) {
    return NextResponse.json(
      {
        error: "Invalid cart items.",
      },
      {
        status: 400,
      }
    );
  }

  const backendUrl =
    process.env.KCMART_API_URL
      ?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {

    console.error(
      "KCMART checkout server configuration is missing."
    );

    return NextResponse.json(
      {
        error: "Checkout is temporarily unavailable.",
      },
      {
        status: 503,
      }
    );
  }

  try {

    const response =
      await fetch(
        `${backendUrl}/api/v1/internal/checkout/validate`,
        {
          method: "POST",
          headers: {
            "Content-Type":
              "application/json",
            "X-KCMART-Internal-Key":
              internalKey,
          },
          body: JSON.stringify({
            customerEmail: email,
            fulfillmentType,
            items,
          }),
          cache: "no-store",
        }
      );

    const contentType =
      response.headers.get(
        "content-type"
      ) ?? "";

    if (!response.ok) {

      /*
       * Do not forward Spring's complete internal
       * error body to the browser.
       */

      if (response.status === 409) {
        return NextResponse.json(
          {
            error:
              "One or more items do not have enough stock.",
          },
          {
            status: 409,
          }
        );
      }

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        return NextResponse.json(
          {
            error:
              "Your customer account cannot use checkout.",
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
              "One or more cart items are no longer available.",
          },
          {
            status: 400,
          }
        );
      }

      console.error(
        "KCMART backend checkout validation failed:",
        response.status
      );

      return NextResponse.json(
        {
          error:
            "Checkout validation failed.",
        },
        {
          status: 502,
        }
      );
    }

    if (
      !contentType
        .toLowerCase()
        .includes("application/json")
    ) {
      console.error(
        "KCMART backend returned non-JSON checkout response."
      );

      return NextResponse.json(
        {
          error:
            "Checkout validation failed.",
        },
        {
          status: 502,
        }
      );
    }

    const result =
      await response.json();

    return NextResponse.json(
      result,
      {
        status: 200,
      }
    );
  }
  catch (error) {

    console.error(
      "KCMART checkout bridge error:",
      error
    );

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