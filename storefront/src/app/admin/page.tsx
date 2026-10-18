import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import {
  AlertTriangle,
  ArrowRight,
  Boxes,
  CheckCircle2,
  Clock3,
  IndianRupee,
  PackageSearch,
  ShoppingBag,
  Store,
} from "lucide-react";
import { getAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

type Product = {
  id: number;
  name: string;
  sku: string;
  price: number;
  active: boolean;
  onlineStock: number;
  storeStock: number;
};

type Order = {
  id: number;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  fulfillmentType: string;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  orderStatus: string;
  createdAt: string;
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
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

function dateTime(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kolkata",
  }).format(new Date(value));
}

function statusStyle(status: string) {
  switch (status) {
    case "COMPLETED":
    case "DELIVERED":
      return "bg-emerald-50 text-emerald-700";
    case "CANCELLED":
      return "bg-red-50 text-red-700";
    case "PENDING":
      return "bg-amber-50 text-amber-700";
    case "SHIPPED":
    case "OUT_FOR_DELIVERY":
      return "bg-[#EAF5F2] text-[#075E54]";
    default:
      return "bg-[#EAF5F2] text-[#075E54]";
  }
}

export default async function AdminDashboardPage() {
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

  const headers = {
    Authorization: `Basic ${token}`,
  };

  const [productResponse, orderResponse] =
    await Promise.all([
      fetch(`${api}/api/v1/admin/products`, {
        headers,
        cache: "no-store",
      }),
      fetch(`${api}/api/v1/admin/orders`, {
        headers,
        cache: "no-store",
      }),
    ]);

  if (
    productResponse.status === 401 ||
    productResponse.status === 403 ||
    orderResponse.status === 401 ||
    orderResponse.status === 403
  ) {
    redirect("/admin/login");
  }

  const products: Product[] =
    productResponse.ok
      ? await productResponse.json()
      : [];

  const orders: Order[] =
    orderResponse.ok
      ? await orderResponse.json()
      : [];

  const onlineStock = products.reduce(
    (sum, product) => sum + product.onlineStock,
    0
  );

  const storeStock = products.reduce(
    (sum, product) => sum + product.storeStock,
    0
  );

  const pendingOrders = orders.filter(
    (order) => order.orderStatus === "PENDING"
  ).length;

  const paidRevenue = orders
    .filter(
      (order) =>
        order.paymentStatus === "PAID" &&
        order.orderStatus !== "CANCELLED"
    )
    .reduce(
      (sum, order) => sum + order.totalAmount,
      0
    );

  const lowStockProducts = products.filter(
    (product) =>
      product.active &&
      (product.onlineStock <= 5 ||
        product.storeStock <= 5)
  );

  const recentOrders = [...orders]
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() -
        new Date(a.createdAt).getTime()
    )
    .slice(0, 6);

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-[1500px]">
        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-[#0B7A69]">
              Overview
            </p>

            <h1 className="mt-2 text-3xl font-black tracking-tight text-[#111827]">
              Dashboard
            </h1>

            <p className="mt-2 text-sm text-[#6B7280]">
              Store, inventory and order overview.
            </p>
          </div>

          <Link
            href="/admin/manage"
            className="inline-flex items-center gap-2 rounded-xl bg-[#075E54] px-5 py-3 text-sm font-bold text-white transition hover:bg-[#064E46]"
          >
            Manage Products
            <ArrowRight size={17} />
          </Link>
        </div>

        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard
            icon={<PackageSearch size={21} />}
            label="Total Products"
            value={String(products.length)}
          />

          <StatCard
            icon={<ShoppingBag size={21} />}
            label="Total Orders"
            value={String(orders.length)}
          />

          <StatCard
            icon={<Clock3 size={21} />}
            label="Pending Orders"
            value={String(pendingOrders)}
          />

          <StatCard
            icon={<IndianRupee size={21} />}
            label="Paid Revenue"
            value={money(paidRevenue)}
          />
        </section>

        <section className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <StatCard
            icon={<Boxes size={21} />}
            label="Online Stock"
            value={String(onlineStock)}
          />

          <StatCard
            icon={<Store size={21} />}
            label="Store Stock"
            value={String(storeStock)}
          />

          <StatCard
            icon={
              lowStockProducts.length > 0 ? (
                <AlertTriangle size={21} />
              ) : (
                <CheckCircle2 size={21} />
              )
            }
            label="Low Stock Products"
            value={String(lowStockProducts.length)}
          />
        </section>

        <div className="mt-8 grid gap-6 xl:grid-cols-[1.7fr_1fr]">
          <section className="overflow-hidden rounded-2xl border border-[#DDEBE7] bg-white shadow-sm">
            <div className="flex items-center justify-between border-b border-[#E5E7EB] px-5 py-5 sm:px-6">
              <div>
                <h2 className="text-lg font-black text-[#111827]">
                  Recent Orders
                </h2>

                <p className="mt-1 text-sm text-[#6B7280]">
                  Latest customer orders.
                </p>
              </div>

              <Link
                href="/admin/orders"
                className="text-sm font-bold text-[#075E54] hover:underline"
              >
                View all
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px] text-left text-sm">
                <thead className="bg-[#F6FAF9] text-[#6B7280]">
                  <tr>
                    <th className="px-5 py-3">Order</th>
                    <th className="px-5 py-3">Customer</th>
                    <th className="px-5 py-3">Payment</th>
                    <th className="px-5 py-3">Total</th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3"></th>
                  </tr>
                </thead>

                <tbody>
                  {recentOrders.map((order) => (
                    <tr
                      key={order.id}
                      className="border-t border-[#EEF2F1]"
                    >
                      <td className="px-5 py-4">
                        <p className="font-bold text-[#111827]">
                          #{order.id}
                        </p>

                        <p className="mt-1 text-xs text-[#9CA3AF]">
                          {dateTime(order.createdAt)}
                        </p>
                      </td>

                      <td className="px-5 py-4">
                        <p className="font-semibold">
                          {order.customerName}
                        </p>

                        <p className="mt-1 text-xs text-[#6B7280]">
                          {order.customerPhone}
                        </p>
                      </td>

                      <td className="px-5 py-4">
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

                      <td className="px-5 py-4 font-bold">
                        {money(order.totalAmount)}
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex rounded-full px-3 py-1 text-xs font-bold ${statusStyle(
                            order.orderStatus
                          )}`}
                        >
                          {readable(order.orderStatus)}
                        </span>
                      </td>

                      <td className="px-5 py-4 text-right">
                        <Link
                          href={`/admin/orders/${order.id}`}
                          className="font-bold text-[#075E54]"
                        >
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {recentOrders.length === 0 && (
              <p className="p-10 text-center text-sm text-[#6B7280]">
                No orders yet.
              </p>
            )}
          </section>

          <section className="overflow-hidden rounded-2xl border border-[#DDEBE7] bg-white shadow-sm">
            <div className="border-b border-[#E5E7EB] px-5 py-5 sm:px-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-black text-[#111827]">
                    Low Stock
                  </h2>

                  <p className="mt-1 text-sm text-[#6B7280]">
                    Products needing attention.
                  </p>
                </div>

                <div className="rounded-xl bg-amber-50 p-2.5 text-amber-700">
                  <AlertTriangle size={20} />
                </div>
              </div>
            </div>

            <div className="divide-y divide-[#EEF2F1]">
              {lowStockProducts
                .slice(0, 7)
                .map((product) => (
                  <div
                    key={product.id}
                    className="flex items-center justify-between gap-4 px-5 py-4 sm:px-6"
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-bold text-[#111827]">
                        {product.name}
                      </p>

                      <p className="mt-1 text-xs text-[#6B7280]">
                        {product.sku}
                      </p>
                    </div>

                    <div className="shrink-0 text-right text-xs">
                      <p className="font-bold text-[#075E54]">
                        Online {product.onlineStock}
                      </p>

                      <p className="mt-1 font-semibold text-[#6B7280]">
                        Store {product.storeStock}
                      </p>
                    </div>
                  </div>
                ))}
            </div>

            {lowStockProducts.length === 0 && (
              <div className="p-8 text-center">
                <CheckCircle2
                  size={28}
                  className="mx-auto text-emerald-600"
                />

                <p className="mt-3 text-sm font-bold text-[#111827]">
                  Stock levels look good
                </p>
              </div>
            )}

            {lowStockProducts.length > 7 && (
              <Link
                href="/admin/inventory"
                className="block border-t border-[#E5E7EB] px-6 py-4 text-center text-sm font-bold text-[#075E54]"
              >
                View all low-stock products
              </Link>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}

function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-[#DDEBE7] bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-[#6B7280]">
            {label}
          </p>

          <p className="mt-3 text-2xl font-black tracking-tight text-[#111827]">
            {value}
          </p>
        </div>

        <div className="rounded-xl bg-[#EAF5F2] p-3 text-[#075E54]">
          {icon}
        </div>
      </div>
    </div>
  );
}