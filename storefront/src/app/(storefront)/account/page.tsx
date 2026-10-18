"use client";

import Image from "next/image";
import Link from "next/link";
import {
  ArrowRight,
  Box,
  CalendarDays,
  CheckCircle2,
  Clock3,
  CreditCard,
  LogOut,
  Mail,
  MapPin,
  PackageCheck,
  RefreshCw,
  ShieldCheck,
  ShoppingBag,
  Store,
  Truck,
  UserRound,
} from "lucide-react";
import { signOut, useSession } from "next-auth/react";
import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { useCustomerLogin } from "@/components/auth/CustomerLoginProvider";

type CustomerOrder = {
  id: number;
  orderNumber: string;
  fulfillmentType: string;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  orderStatus: string;
  createdAt: string;
  updatedAt: string;
};

type OrdersResponse = {
  orders?: CustomerOrder[];
  error?: string;
};

function pretty(value: string | null | undefined) {
  if (!value) return "—";

  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(date);
}

function statusClass(status: string) {
  switch (status) {
    case "COMPLETED":
    case "DELIVERED":
      return "border-emerald-200 bg-emerald-50 text-emerald-700";

    case "SHIPPED":
    case "OUT_FOR_DELIVERY":
      return "border-[#BFDCD5] bg-[#EAF5F2] text-[#075E54]";

    case "READY_FOR_PICKUP":
    case "PACKED":
      return "border-violet-200 bg-violet-50 text-violet-700";

    case "CONFIRMED":
    case "PROCESSING":
      return "border-[#BFDCD5] bg-[#EAF5F2] text-[#075E54]";

    case "CANCELLED":
      return "border-red-200 bg-red-50 text-red-700";

    default:
      return "border-amber-200 bg-amber-50 text-amber-700";
  }
}

function paymentClass(status: string) {
  if (status === "PAID") {
    return "text-emerald-700";
  }

  return "text-amber-700";
}

function OrderCard({
  order,
}: {
  order: CustomerOrder;
}) {
  const isDelivery =
    order.fulfillmentType === "DELIVERY";

  return (
    <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-100 bg-slate-50/70 px-4 py-4 sm:px-5">
        <div>
          <p className="text-xs font-bold uppercase tracking-wide text-slate-400">
            Order
          </p>

          <p className="mt-1 break-all text-sm font-black text-slate-950 sm:text-base">
            {order.orderNumber}
          </p>
        </div>

        <span
          className={`rounded-full border px-3 py-1.5 text-xs font-extrabold ${statusClass(
            order.orderStatus
          )}`}
        >
          {pretty(order.orderStatus)}
        </span>
      </div>

      <div className="p-4 sm:p-5">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-400">
              <CalendarDays size={14} />
              Ordered
            </p>

            <p className="mt-1 text-sm font-bold text-slate-800">
              {formatDate(order.createdAt)}
            </p>
          </div>

          <div>
            <p className="text-xs font-semibold text-slate-400">
              Total
            </p>

            <p className="mt-1 text-base font-black text-[#075E54]">
              {formatMoney(
                Number(order.totalAmount)
              )}
            </p>
          </div>

          <div>
            <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-400">
              {isDelivery ? (
                <Truck size={14} />
              ) : (
                <Store size={14} />
              )}
              Fulfilment
            </p>

            <p className="mt-1 text-sm font-bold text-slate-800">
              {pretty(order.fulfillmentType)}
            </p>
          </div>

          <div>
            <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-400">
              <CreditCard size={14} />
              Payment
            </p>

            <p
              className={`mt-1 text-sm font-extrabold ${paymentClass(
                order.paymentStatus
              )}`}
            >
              {pretty(order.paymentStatus)}
            </p>
          </div>
        </div>

        <div className="mt-5 rounded-xl border border-slate-100 bg-slate-50 px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs font-semibold text-slate-500">
              {pretty(order.paymentMethod)}
            </span>

            <span className="flex items-center gap-1.5 text-xs font-bold text-slate-600">
              {order.orderStatus ===
              "COMPLETED" ? (
                <CheckCircle2
                  size={15}
                  className="text-emerald-600"
                />
              ) : (
                <Clock3
                  size={15}
                  className="text-[#075E54]"
                />
              )}

              {order.orderStatus ===
              "COMPLETED"
                ? "Order complete"
                : isDelivery
                  ? "Tracking available as order progresses"
                  : "Pickup status updates here"}
            </span>
          </div>
        </div>

        <Link
          href={`/account/orders/${order.id}`}
          className="mt-5 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46]"
        >
          {isDelivery
            ? "View Order & Track"
            : "View Order Details"}

          <ArrowRight size={17} />
        </Link>
      </div>
    </article>
  );
}

function MyOrders() {
  const [orders, setOrders] =
    useState<CustomerOrder[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState<string | null>(null);

  const loadOrders = useCallback(
    async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetch(
          "/api/account/orders",
          {
            method: "GET",
            cache: "no-store",
          }
        );

        const data =
          (await response.json()) as OrdersResponse;

        if (!response.ok) {
          throw new Error(
            data.error ||
              "Unable to load your orders."
          );
        }

        setOrders(
          Array.isArray(data.orders)
            ? data.orders
            : []
        );
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to load your orders."
        );
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  return (
    <section className="mt-6 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
      <div className="flex items-center justify-between gap-4 border-b border-slate-100 px-5 py-5 sm:px-7">
        <div>
          <div className="flex items-center gap-2">
            <ShoppingBag
              size={20}
              className="text-[#075E54]"
            />

            <h2 className="text-lg font-black text-slate-950">
              My Orders
            </h2>
          </div>

          <p className="mt-1 text-xs text-slate-500">
            Your KCMART purchases and order status.
          </p>
        </div>

        {!loading && (
          <button
            type="button"
            onClick={() =>
              void loadOrders()
            }
            className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 text-slate-600 transition hover:border-[#075E54] hover:text-[#075E54]"
            aria-label="Refresh orders"
          >
            <RefreshCw size={17} />
          </button>
        )}
      </div>

      <div className="p-4 sm:p-6">
        {loading && (
          <div className="space-y-4">
            {[1, 2].map((item) => (
              <div
                key={item}
                className="animate-pulse rounded-2xl border border-slate-100 p-5"
              >
                <div className="h-4 w-40 rounded bg-slate-200" />
                <div className="mt-5 grid grid-cols-2 gap-4">
                  <div className="h-12 rounded-lg bg-slate-100" />
                  <div className="h-12 rounded-lg bg-slate-100" />
                  <div className="h-12 rounded-lg bg-slate-100" />
                  <div className="h-12 rounded-lg bg-slate-100" />
                </div>
              </div>
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="rounded-2xl border border-red-100 bg-red-50 p-5 text-center">
            <p className="text-sm font-bold text-red-700">
              {error}
            </p>

            <button
              type="button"
              onClick={() =>
                void loadOrders()
              }
              className="mt-4 rounded-xl bg-slate-950 px-5 py-2.5 text-sm font-bold text-white"
            >
              Try Again
            </button>
          </div>
        )}

        {!loading &&
          !error &&
          orders.length === 0 && (
            <div className="px-3 py-8 text-center sm:py-10">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[#EAF5F2]">
                <Box
                  size={30}
                  className="text-[#075E54]"
                />
              </div>

              <h3 className="mt-5 text-lg font-black text-slate-900">
                No orders yet
              </h3>

              <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-500">
                When you place an order with
                KCMART, you&apos;ll be able to
                see its payment, pickup or
                delivery status here.
              </p>

              <a
                href="/"
                className="mt-6 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46]"
              >
                Start Shopping
                <ArrowRight size={17} />
              </a>
            </div>
          )}

        {!loading &&
          !error &&
          orders.length > 0 && (
            <div className="space-y-4">
              {orders.map((order) => (
                <OrderCard
                  key={order.id}
                  order={order}
                />
              ))}
            </div>
          )}
      </div>
    </section>
  );
}

export default function AccountPage() {
  const { openLogin } =
    useCustomerLogin();

  const { data: session, status } =
    useSession();

  if (status === "loading") {
    return (
      <main className="min-h-[70vh] bg-slate-50 px-4 py-12">
        <div className="mx-auto max-w-2xl animate-pulse rounded-3xl border border-slate-100 bg-white p-7 shadow-sm sm:p-10">
          <div className="mx-auto h-20 w-20 rounded-full bg-slate-200" />
          <div className="mx-auto mt-6 h-8 w-64 rounded-lg bg-slate-200" />
          <div className="mx-auto mt-4 h-5 w-48 rounded-lg bg-slate-100" />
        </div>
      </main>
    );
  }

  if (
    status === "authenticated" &&
    session?.user
  ) {
    const user = session.user;

    return (
      <main className="min-h-[70vh] bg-slate-50 px-4 py-8 sm:py-12">
        <div className="mx-auto max-w-2xl">
          <section className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
            <div className="bg-gradient-to-br from-[#EAF5F2] via-white to-slate-50 px-7 pb-8 pt-10 text-center sm:px-10">
              {user.image ? (
                <Image
                  src={user.image}
                  alt={
                    user.name ||
                    "Customer profile"
                  }
                  width={96}
                  height={96}
                  className="mx-auto h-24 w-24 rounded-full border-4 border-white object-cover shadow-md"
                  unoptimized
                />
              ) : (
                <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full border-4 border-white bg-[#EAF5F2] shadow-md">
                  <UserRound
                    size={42}
                    className="text-[#075E54]"
                  />
                </div>
              )}

              <div className="mt-5 inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700">
                <ShieldCheck size={15} />
                Signed in securely
              </div>

              <h1 className="mt-4 text-2xl font-black text-slate-950 sm:text-3xl">
                {user.name ||
                  "KCMART Customer"}
              </h1>

              {user.email && (
                <p className="mt-2 flex items-center justify-center gap-2 text-sm text-slate-500">
                  <Mail size={16} />
                  {user.email}
                </p>
              )}
            </div>

            <div className="border-t border-slate-100 p-6 sm:p-8">
              <h2 className="text-lg font-black text-slate-900">
                Your KCMART Account
              </h2>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                Manage your account and review
                your KCMART order history.
              </p>

              <div className="mt-5 flex items-center gap-3 rounded-2xl bg-[#EAF5F2] px-4 py-3">
                <PackageCheck
                  size={20}
                  className="shrink-0 text-[#075E54]"
                />

                <div>
                  <p className="text-sm font-bold text-slate-800">
                    Order updates
                  </p>

                  <p className="mt-0.5 text-xs text-slate-500">
                    Pickup and delivery progress
                    will appear in My Orders.
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() =>
                  signOut({
                    callbackUrl: "/",
                  })
                }
                className="mt-6 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 px-5 font-bold text-red-700 transition hover:bg-red-100"
              >
                <LogOut size={18} />
                Sign Out
              </button>
            </div>
          </section>

          <MyOrders />

          <div className="mt-5 flex items-start gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-4 text-xs leading-5 text-slate-500">
            <MapPin
              size={17}
              className="mt-0.5 shrink-0 text-[#075E54]"
            />

            <p>
              Store pickup is available from
              Khariar Computer Service, College
              Road, Near Koshaleswar Temple,
              Raj Khariar, Odisha - 766107.
            </p>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="min-h-[70vh] bg-slate-50 px-4 py-12">
      <div className="mx-auto max-w-lg rounded-3xl border border-slate-100 bg-white p-7 text-center shadow-sm sm:p-10">
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-[#EAF5F2]">
          <UserRound
            size={37}
            className="text-[#075E54]"
          />
        </div>

        <h1 className="mt-6 text-3xl font-black text-slate-950">
          Your KCMART Account
        </h1>

        <p className="mt-3 text-sm leading-6 text-slate-500">
          Sign in securely to access your
          KCMART customer account and orders.
        </p>

        <button
          type="button"
          onClick={() =>
            openLogin("account")
          }
          className="mt-7 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 font-extrabold text-white transition hover:bg-[#064E46]"
        >
          Sign in or Create Account
          <ArrowRight size={18} />
        </button>

        <div className="mt-7 flex items-center justify-center gap-2 border-t border-slate-100 pt-6 text-xs text-slate-500">
          <ShieldCheck
            size={17}
            className="text-emerald-700"
          />
          Secure authentication powered by
          Google.
        </div>
      </div>
    </main>
  );
}