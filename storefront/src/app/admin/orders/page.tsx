import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  PackageCheck,
  ShoppingBag,
} from "lucide-react";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type OrderItem = {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
};

type OrderHistory = {
  id: number;
  previousStatus: string | null;
  newStatus: string;
  actorType: string;
  actorIdentifier: string;
  note: string | null;
  createdAt: string;
};

type Order = {
  id: number;
  orderNumber: string;
  customerId: number;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  fulfillmentType: string;

  addressLine1: string | null;
  addressLine2: string | null;
  landmark: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;

  subtotal: number;
  deliveryCharge: number;
  totalAmount: number;

  paymentMethod: string;
  paymentStatus: string;
  orderStatus: string;

  createdAt: string;
  updatedAt: string;

  items: OrderItem[];
  history: OrderHistory[];
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}

function dateTime(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kolkata",
  }).format(new Date(value));
}

function readable(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase() +
        part.slice(1)
    )
    .join(" ");
}

function orderStatusStyle(status: string) {
  switch (status) {
    case "COMPLETED":
      return "bg-emerald-100 text-emerald-800";
    case "READY_FOR_PICKUP":
      return "bg-violet-100 text-violet-800";
    case "CONFIRMED":
      return "bg-[#D9ECE7] text-[#064E46]";
    case "PROCESSING":
    case "PACKED":
      return "bg-amber-100 text-amber-800";
    case "SHIPPED":
    case "OUT_FOR_DELIVERY":
      return "bg-[#D9ECE7] text-[#064E46]";
    case "DELIVERED":
      return "bg-green-100 text-green-800";
    case "CANCELLED":
      return "bg-red-100 text-red-800";
    default:
      return "bg-slate-100 text-slate-700";
  }
}

export default async function AdminOrdersPage() {
  const cookieStore = await cookies();

  const session = getAdminSession(
    cookieStore.get("kcmart_admin_session")?.value
  );

  if (!session) {
    redirect("/admin/login");
  }

  const token = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  const response = await fetch(
    `${api}/api/v1/admin/orders`,
    {
      headers: {
        Authorization: `Basic ${token}`,
      },
      cache: "no-store",
    }
  );

  if (
    response.status === 401 ||
    response.status === 403
  ) {
    redirect("/admin/login");
  }

  if (!response.ok) {
    return (
      <main className="min-h-screen bg-slate-100 p-6">
        <div className="mx-auto max-w-7xl">
          <h1 className="text-2xl font-bold text-slate-900">
            KCMART Orders
          </h1>

          <p className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
            Could not load orders. Check the KCMART backend.
          </p>
        </div>
      </main>
    );
  }

  const orders: Order[] = await response.json();

  const pending = orders.filter(
    (order) => order.orderStatus === "PENDING"
  ).length;

  const active = orders.filter(
    (order) =>
      !["PENDING", "COMPLETED", "CANCELLED"].includes(
        order.orderStatus
      )
  ).length;

  const completed = orders.filter(
    (order) => order.orderStatus === "COMPLETED"
  ).length;

  return (
    <main className="min-h-screen bg-slate-100 p-4 text-slate-900 md:p-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-widest text-[#075E54]">
              KCMART Administration
            </p>

            <h1 className="mt-2 text-3xl font-bold">
              Order Management
            </h1>

            <p className="mt-2 text-sm text-slate-500">
              Review customer orders, payments and fulfillment.
            </p>
          </div>

          <Link
            href="/admin/products"
            className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-3 font-semibold shadow-sm"
          >
            <ArrowLeft size={18} />
            Dashboard
          </Link>
        </header>

        <section className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Stat
            icon={<ShoppingBag size={22} />}
            label="Total orders"
            value={orders.length}
          />

          <Stat
            icon={<Clock3 size={22} />}
            label="Pending"
            value={pending}
          />

          <Stat
            icon={<PackageCheck size={22} />}
            label="In progress"
            value={active}
          />

          <Stat
            icon={<CheckCircle2 size={22} />}
            label="Completed"
            value={completed}
          />
        </section>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-200 p-5 md:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold">
                  Orders
                </h2>

                <p className="mt-1 text-sm text-slate-500">
                  Newest orders are shown first.
                </p>
              </div>

              <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-600">
                {orders.length} total
              </span>
            </div>
          </div>

          <div className="hidden overflow-x-auto md:block">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="p-4">Order</th>
                  <th className="p-4">Customer</th>
                  <th className="p-4">Fulfillment</th>
                  <th className="p-4">Payment</th>
                  <th className="p-4">Total</th>
                  <th className="p-4">Status</th>
                  <th className="p-4"></th>
                </tr>
              </thead>

              <tbody>
                {orders.map((order) => (
                  <tr
                    key={order.id}
                    className="border-t border-slate-100"
                  >
                    <td className="p-4">
                      <p className="font-bold">
                        #{order.id}
                      </p>

                      <p className="mt-1 max-w-48 truncate text-xs text-slate-500">
                        {order.orderNumber}
                      </p>

                      <p className="mt-1 text-xs text-slate-400">
                        {dateTime(order.createdAt)}
                      </p>
                    </td>

                    <td className="p-4">
                      <p className="font-semibold">
                        {order.customerName}
                      </p>

                      <p className="mt-1 text-xs text-slate-500">
                        {order.customerPhone}
                      </p>
                    </td>

                    <td className="p-4">
                      {readable(order.fulfillmentType)}
                    </td>

                    <td className="p-4">
                      <p className="font-medium">
                        {readable(order.paymentMethod)}
                      </p>

                      <p
                        className={
                          order.paymentStatus === "PAID"
                            ? "mt-1 text-xs font-bold text-emerald-700"
                            : "mt-1 text-xs font-bold text-amber-700"
                        }
                      >
                        {readable(order.paymentStatus)}
                      </p>
                    </td>

                    <td className="p-4 font-bold">
                      {money(order.totalAmount)}
                    </td>

                    <td className="p-4">
                      <span
                        className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${orderStatusStyle(
                          order.orderStatus
                        )}`}
                      >
                        {readable(order.orderStatus)}
                      </span>
                    </td>

                    <td className="p-4 text-right">
                      <Link
                        href={`/admin/orders/${order.id}`}
                        className="inline-flex rounded-lg bg-slate-900 px-4 py-2 text-xs font-bold text-white"
                      >
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="divide-y divide-slate-100 md:hidden">
            {orders.map((order) => (
              <article
                key={order.id}
                className="p-5"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-bold">
                      Order #{order.id}
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      {dateTime(order.createdAt)}
                    </p>
                  </div>

                  <span
                    className={`rounded-full px-3 py-1 text-xs font-bold ${orderStatusStyle(
                      order.orderStatus
                    )}`}
                  >
                    {readable(order.orderStatus)}
                  </span>
                </div>

                <div className="mt-4">
                  <p className="font-semibold">
                    {order.customerName}
                  </p>

                  <p className="text-sm text-slate-500">
                    {order.customerPhone}
                  </p>
                </div>

                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-xs text-slate-400">
                      Fulfillment
                    </p>
                    <p className="mt-1 font-medium">
                      {readable(order.fulfillmentType)}
                    </p>
                  </div>

                  <div>
                    <p className="text-xs text-slate-400">
                      Payment
                    </p>
                    <p className="mt-1 font-medium">
                      {readable(order.paymentStatus)}
                    </p>
                  </div>
                </div>

                <div className="mt-5 flex items-center justify-between">
                  <p className="text-lg font-bold">
                    {money(order.totalAmount)}
                  </p>

                  <Link
                    href={`/admin/orders/${order.id}`}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-bold text-white"
                  >
                    View order
                  </Link>
                </div>
              </article>
            ))}
          </div>

          {orders.length === 0 && (
            <p className="p-10 text-center text-slate-500">
              No orders found.
            </p>
          )}
        </section>
      </div>
    </main>
  );
}

function Stat({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: number;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="rounded-xl bg-slate-100 p-3 text-slate-700">
          {icon}
        </div>

        <div>
          <p className="text-sm text-slate-500">
            {label}
          </p>

          <p className="text-2xl font-bold">
            {value}
          </p>
        </div>
      </div>
    </div>
  );
}