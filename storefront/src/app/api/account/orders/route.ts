import { NextResponse } from "next/server";
import { auth } from "@/auth";

export const dynamic = "force-dynamic";

export async function GET() {
  const session = await auth();

  const customerEmail =
    session?.user?.email
      ?.trim()
      .toLowerCase();

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

  const backendUrl =
    process.env.KCMART_API_URL
      ?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl || !internalKey) {
    console.error(
      "KCMART account order service configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Order history is temporarily unavailable.",
      },
      {
        status: 503,
      }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer`,
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
         * The customer email comes only from the
         * verified Auth.js server session.
         *
         * The browser cannot choose a customerId
         * or another customer's email.
         */
        body: JSON.stringify({
          email: customerEmail,
        }),

        cache: "no-store",
      }
    );

    if (!response.ok) {
      console.error(
        `KCMART customer orders backend returned HTTP ${response.status}.`
      );

      if (response.status === 404) {
        return NextResponse.json(
          {
            orders: [],
          },
          {
            status: 200,
          }
        );
      }

      return NextResponse.json(
        {
          error:
            "Unable to load order history.",
        },
        {
          status: 502,
        }
      );
    }

    const orders = await response.json();

    return NextResponse.json(
      {
        orders,
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
      "KCMART customer order request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the order service.",
      },
      {
        status: 502,
      }
    );
  }
}