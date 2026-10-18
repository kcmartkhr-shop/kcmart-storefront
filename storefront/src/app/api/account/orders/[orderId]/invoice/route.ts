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
      { error: "Invoice not found." },
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
      "KCMART customer invoice service configuration is missing."
    );

    return NextResponse.json(
      {
        error:
          "Invoice service is temporarily unavailable.",
      },
      { status: 503 }
    );
  }

  try {
    const response = await fetch(
      `${backendUrl}/api/v1/internal/orders/customer/${orderId}/invoice`,
      {
        method: "POST",

        headers: {
          "Content-Type": "application/json",
          "X-KCMART-Internal-Key": internalKey,
        },

        /*
         * SECURITY:
         *
         * The browser supplies only the order ID.
         * Customer identity comes only from the
         * server-side Auth.js session.
         *
         * Spring verifies that the order belongs
         * to this customer before returning the PDF.
         */
        body: JSON.stringify({
          email: customerEmail,
        }),

        cache: "no-store",
      }
    );

    if (response.status === 404) {
      return NextResponse.json(
        { error: "Invoice not found." },
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
        `KCMART customer invoice backend returned HTTP ${response.status}.`
      );

      return NextResponse.json(
        {
          error:
            "Unable to load the invoice.",
        },
        { status: 502 }
      );
    }

    const pdf = await response.arrayBuffer();

    const contentDisposition =
      response.headers.get("content-disposition") ??
      `inline; filename="invoice-${orderId}.pdf"`;

    return new NextResponse(pdf, {
      status: 200,
      headers: {
        "Content-Type": "application/pdf",
        "Content-Disposition": contentDisposition,
        "Cache-Control":
          "private, no-store, max-age=0",
      },
    });
  } catch (error) {
    console.error(
      "KCMART customer invoice request failed.",
      error
    );

    return NextResponse.json(
      {
        error:
          "Unable to connect to the invoice service.",
      },
      { status: 502 }
    );
  }
}