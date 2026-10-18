import Link from "next/link";
import DelhiveryPackagePanel from "@/components/admin/delivery/DelhiveryPackagePanel";
import DelhiveryBookingPreviewPanel from "@/components/admin/delivery/DelhiveryBookingPreviewPanel";
import DelhiveryLabelEligibilityPanel from "@/components/admin/delivery/DelhiveryLabelEligibilityPanel";
import DelhiveryServiceabilityPanel from "@/components/admin/delivery/DelhiveryServiceabilityPanel";
import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { ArrowLeft, Package, MapPin, Truck } from "lucide-react";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type OrderItem = {
  id: number;
  productName: string;
  sku: string;
  quantity: number;
};

type Order = {
  id: number;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  fulfillmentType: string;
  addressLine1: string | null;
  addressLine2: string | null;
  landmark: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  orderStatus: string;
  paymentStatus: string;
  items: OrderItem[];
};

export default async function DeliveryDetails({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  if (!/^\d+$/.test(id)) {
    notFound();
  }

  const cookieStore = await cookies();
  const sessionId = cookieStore.get("kcmart_admin_session")?.value;
  const session = getAdminSession(sessionId);

  if (!session) {
    redirect("/admin/login");
  }

  const api = process.env.KCMART_API_URL ?? "http://127.0.0.1:8080";
  const token = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  let order: Order;

  try {
    const response = await fetch(`${api}/api/v1/admin/orders/${id}`, {
      headers: { Authorization: `Basic ${token}` },
      cache: "no-store",
    });

    if (response.status === 404) {
      notFound();
    }

    if (!response.ok) {
      throw new Error("Unable to load order");
    }

    order = (await response.json()) as Order;
  } catch (error) {
    if (error && typeof error === "object" && "digest" in error) {
      throw error;
    }
    throw new Error("Delivery order could not be loaded.");
  }

  if (order.fulfillmentType !== "DELIVERY") {
    return (
      <main className="p-6">
        <Link href="/admin/delivery" className="text-teal-700">
          Back to Delivery Management
        </Link>
        <p className="mt-6 text-slate-700">
          This order is not marked for home delivery.
        </p>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-6xl space-y-6">
        <Link
          href="/admin/delivery"
          className="inline-flex items-center gap-2 text-sm font-semibold text-teal-700"
        >
          <ArrowLeft className="h-4 w-4" />
          Delivery Management
        </Link>

        <div>
          <p className="text-sm font-semibold text-teal-700">
            DELIVERY WORKSPACE
          </p>
          <h1 className="mt-1 text-3xl font-bold text-slate-900">
            {order.orderNumber}
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Order: {order.orderStatus} Ã‚Â· Payment: {order.paymentStatus}
          </p>
        </div>

        <div className="grid gap-5 lg:grid-cols-2">
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-2">
              <MapPin className="h-5 w-5 text-teal-700" />
              <h2 className="text-lg font-bold">Delivery Address</h2>
            </div>

            <div className="mt-4 space-y-2 text-sm text-slate-700">
              <p className="font-semibold">{order.customerName}</p>
              <p>{order.customerPhone}</p>
              <p>{order.customerEmail}</p>
              <p>{order.addressLine1}</p>
              {order.addressLine2 && <p>{order.addressLine2}</p>}
              {order.landmark && <p>{order.landmark}</p>}
              <p>
                {[order.city, order.state, order.postalCode]
                  .filter(Boolean)
                  .join(", ")}
              </p>
              <p>{order.country}</p>
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-2">
              <Package className="h-5 w-5 text-teal-700" />
              <h2 className="text-lg font-bold">Products to Dispatch</h2>
            </div>

            <div className="mt-4 divide-y divide-slate-100">
              {(order.items ?? []).map((item) => (
                <div key={item.id} className="py-3">
                  <p className="font-semibold text-slate-900">
                    {item.productName}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    SKU: {item.sku} Ã‚Â· Quantity: {item.quantity}
                  </p>
                </div>
              ))}
            </div>
          </section>
        </div>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-2">
            <Truck className="h-5 w-5 text-teal-700" />
            <h2 className="text-lg font-bold">
              Shipment Preparation
            </h2>
          </div>

          <p className="mt-3 text-sm text-slate-600">
            Record actual packed weight and dimensions before courier booking.
          </p>

          {order.orderStatus === "PACKED" ? (
            <div className="mt-5">
              <DelhiveryServiceabilityPanel initialPincode={order.postalCode ?? ""} />
          <DelhiveryPackagePanel orderId={order.id} />
          <DelhiveryBookingPreviewPanel orderId={order.id} />
          <DelhiveryLabelEligibilityPanel orderId={order.id} />
            </div>
          ) : (
            <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
              Package preparation becomes available when the order reaches PACKED status.
              Current status: {order.orderStatus}
            </div>
          )}

          <div className="mt-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">
            Shipment actions are not enabled yet. No courier booking
            or pickup request will be sent from this page.
          </div>
        </section>
      </div>
    </main>
  );
}