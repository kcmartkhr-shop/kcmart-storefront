import Link from "next/link";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  ArrowRight,
  CheckCircle2,
  Clock3,
  PackageCheck,
  PackageSearch,
  Truck,
} from "lucide-react";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Order = {
  id: number;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  fulfillmentType: string;
  paymentStatus: string;
  orderStatus: string;
  totalAmount: number;
};

function readable(value: string) {
  return value.replaceAll("_", " ");
}

export default async function DeliveryPage() {
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

  let orders: Order[] = [];
  let error = "";

  try {
    const response = await fetch(`${api}/api/v1/admin/orders`, {
      headers: { Authorization: `Basic ${token}` },
      cache: "no-store",
    });

    if (!response.ok) {
      throw new Error("Orders API failed");
    }

    const data: unknown = await response.json();

    if (Array.isArray(data)) {
      orders = data as Order[];
    } else if (
      data &&
      typeof data === "object" &&
      "orders" in data &&
      Array.isArray(data.orders)
    ) {
      orders = data.orders as Order[];
    } else {
      throw new Error("Unexpected response");
    }
  } catch {
    error = "Unable to load delivery orders.";
  }

  const deliveryOrders = orders.filter(
    (order) => order.fulfillmentType === "DELIVERY"
  );

  const ready = deliveryOrders.filter(
    (order) => order.orderStatus === "PACKED"
  );

  const preparing = deliveryOrders.filter((order) =>
    ["PENDING", "CONFIRMED", "PROCESSING"].includes(order.orderStatus)
  );

  const inTransit = deliveryOrders.filter((order) =>
    ["SHIPPED", "OUT_FOR_DELIVERY"].includes(order.orderStatus)
  );

  const delivered = deliveryOrders.filter((order) =>
    ["DELIVERED", "COMPLETED"].includes(order.orderStatus)
  );

  const stats = [
    { label: "Ready to Ship", value: ready.length, icon: PackageCheck },
    { label: "In Transit", value: inTransit.length, icon: Truck },
    { label: "Delivered", value: delivered.length, icon: CheckCircle2 },
    { label: "Preparing", value: preparing.length, icon: PackageSearch },
  ];

  return (
    <main className="min-h-screen bg-slate-50 p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-7xl space-y-7">
        <div>
          <p className="text-sm font-semibold text-teal-700">
            KCMART ADMIN
          </p>
          <h1 className="mt-1 text-3xl font-bold text-slate-900">
            Delivery Management
          </h1>
          <p className="mt-2 text-sm text-slate-600">
            Prepare shipments, manage courier operations and track deliveries.
          </p>
        </div>

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {stats.map((stat) => {
            const Icon = stat.icon;

            return (
              <div
                key={stat.label}
                className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
              >
                <Icon className="h-6 w-6 text-teal-700" />
                <p className="mt-4 text-3xl font-bold text-slate-900">
                  {stat.value}
                </p>
                <p className="mt-1 text-sm text-slate-600">
                  {stat.label}
                </p>
              </div>
            );
          })}
        </div>

        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-100 p-5">
            <h2 className="text-xl font-bold text-slate-900">
              Ready to Ship
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              Packed delivery orders awaiting courier preparation. Prepaid and COD orders are included; booking eligibility is checked separately.
            </p>
          </div>

          {ready.length === 0 ? (
            <div className="p-10 text-center">
              <Clock3 className="mx-auto h-9 w-9 text-slate-400" />
              <p className="mt-3 font-semibold text-slate-800">
                No eligible orders found
              </p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {ready.map((order) => (
                <div
                  key={order.id}
                  className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="font-bold text-slate-900">
                      {order.orderNumber}
                    </p>
                    <p className="mt-1 text-sm text-slate-600">
                      {order.customerName} · {order.customerPhone}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {readable(order.orderStatus)} · {readable(order.paymentStatus)}
                    </p>
                  </div>
                  <Link
                    href={`/admin/delivery/${order.id}`}
                    className="inline-flex items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800"
                  >
                    Manage Delivery
                    <ArrowRight className="h-4 w-4" />
                  </Link>
                </div>
              ))}
            </div>
          )}
        </section>

        <p className="text-xs text-slate-500">
          Shipment booking and pickup requests are not enabled on this page yet.
        </p>
      </div>
    </main>
  );
}