import { auth } from "@/auth";
import { NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type FulfillmentType =
  | "DELIVERY"
  | "STORE_PICKUP";

type DetailsBody = {
  fulfillmentType?: unknown;
  customerName?: unknown;
  customerPhone?: unknown;
  addressLine1?: unknown;
  addressLine2?: unknown;
  landmark?: unknown;
  city?: unknown;
  state?: unknown;
  postalCode?: unknown;
  country?: unknown;
};

function textValue(
  value: unknown,
  maxLength: number
): string | null {

  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim();

  if (normalized.length > maxLength) {
    return null;
  }

  return normalized;
}

export async function POST(
  request: Request
) {

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

  let raw: unknown;

  try {
    raw = await request.json();
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
    typeof raw !== "object" ||
    raw === null
  ) {
    return NextResponse.json(
      {
        error: "Invalid checkout details.",
      },
      {
        status: 400,
      }
    );
  }

  const body = raw as DetailsBody;

  const fulfillmentType =
    body.fulfillmentType;

  if (
    fulfillmentType !== "DELIVERY" &&
    fulfillmentType !== "STORE_PICKUP"
  ) {
    return NextResponse.json(
      {
        error: "Invalid fulfillment method.",
      },
      {
        status: 400,
      }
    );
  }

  const customerName =
    textValue(body.customerName, 255);

  const customerPhone =
    textValue(body.customerPhone, 10);

  if (
    customerName === null ||
    customerName.length < 2
  ) {
    return NextResponse.json(
      {
        error: "Enter a valid customer name.",
      },
      {
        status: 400,
      }
    );
  }

  if (
    customerPhone === null ||
    !/^[6-9]\d{9}$/.test(customerPhone)
  ) {
    return NextResponse.json(
      {
        error: "Enter a valid mobile number.",
      },
      {
        status: 400,
      }
    );
  }

  let addressLine1: string | null = null;
  let addressLine2: string | null = null;
  let landmark: string | null = null;
  let city: string | null = null;
  let state: string | null = null;
  let postalCode: string | null = null;
  let country: string | null = null;

  if (fulfillmentType === "DELIVERY") {

    addressLine1 =
      textValue(body.addressLine1, 255);

    addressLine2 =
      textValue(body.addressLine2, 255);

    landmark =
      textValue(body.landmark, 255);

    city =
      textValue(body.city, 120);

    state =
      textValue(body.state, 120);

    postalCode =
      textValue(body.postalCode, 20);

    country =
      textValue(body.country, 80);

    if (
      addressLine1 === null ||
      addressLine1.length < 5 ||
      city === null ||
      city.length < 2 ||
      state === null ||
      state.length < 2 ||
      postalCode === null ||
      !/^\d{6}$/.test(postalCode) ||
      country?.toLowerCase() !== "india"
    ) {
      return NextResponse.json(
        {
          error:
            "Please check the delivery address.",
        },
        {
          status: 400,
        }
      );
    }
  }

  const backendUrl =
    process.env.KCMART_API_URL
      ?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {

    console.error(
      "KCMART checkout details configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Checkout is temporarily unavailable.",
      },
      {
        status: 503,
      }
    );
  }

  try {

    const response =
      await fetch(
        `${backendUrl}/api/v1/internal/checkout/details`,
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
            customerName,
            customerPhone,
            addressLine1,
            addressLine2,
            landmark,
            city,
            state,
            postalCode,
            country,
          }),
          cache: "no-store",
        }
      );

    if (!response.ok) {

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
              "Please check your checkout details.",
          },
          {
            status: 400,
          }
        );
      }

      console.error(
        "KCMART backend details validation failed:",
        response.status
      );

      return NextResponse.json(
        {
          error:
            "Checkout details validation failed.",
        },
        {
          status: 502,
        }
      );
    }

    const contentType =
      response.headers.get(
        "content-type"
      ) ?? "";

    if (
      !contentType
        .toLowerCase()
        .includes("application/json")
    ) {
      return NextResponse.json(
        {
          error:
            "Checkout details validation failed.",
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
      "KCMART checkout details bridge error:",
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