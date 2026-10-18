"use client";

import Link from "next/link";
import {
  ArrowLeft,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  CreditCard,
  ExternalLink,
  FileText,
  LoaderCircle,
  MapPin,
  Package,
  PackageCheck,
  ReceiptText,
  RefreshCw,
  ShoppingBag,
  Store,
  Truck,
} from "lucide-react";
import {
  useCallback,
  useEffect,
  useMemo,
  use,
  useState,
} from "react";

type OrderItem = {
  productId: number;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
};

type StatusHistory = {
  status: string;
  createdAt: string;
};

type Shipment = {
  carrierName: string | null;
  trackingNumber: string | null;
  trackingUrl: string | null;
  shipmentStatus: string;
  shippedAt: string | null;
  outForDeliveryAt: string | null;
  deliveredAt: string | null;
};

type Invoice = {
  available: boolean;
  invoiceNumber: string;
  originalFilename: string;
  uploadedAt: string;
};
type OrderDetail = {
  id: number;
  orderNumber: string;
  fulfillmentType: string;

  customerName: string;
  customerEmail: string;
  customerPhone: string;

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
  statusHistory: StatusHistory[];
  shipment: Shipment | null;
  invoice: Invoice | null;
};

type ApiResponse = {
  order?: OrderDetail;
  error?: string;
};

type RazorpayOrder = {
  orderId: number;
  razorpayOrderId: string;
  keyId: string;
  amountPaise: number;
  currency: string;
  name: string;
  description: string;
};

type RazorpaySuccessResponse = {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
};

type RazorpayCheckoutOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (
    response: RazorpaySuccessResponse
  ) => void | Promise<void>;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  theme?: {
    color?: string;
  };
  modal?: {
    ondismiss?: () => void;
  };
};

type RazorpayInstance = {
  open: () => void;
  on: (
    event: string,
    handler: (response: unknown) => void
  ) => void;
};

declare global {
  interface Window {
    Razorpay?: new (
      options: RazorpayCheckoutOptions
    ) => RazorpayInstance;
  }
}
type PageProps = {
  params: Promise<{
    orderId: string;
  }>;
};

function pretty(value: string | null | undefined) {
  if (!value) return "â€”";

  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) =>
      letter.toUpperCase()
    );
}

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value || 0));
}

function dateTime(value: string | null | undefined) {
  if (!value) return "â€”";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "â€”";
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
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

    case "PACKED":
    case "READY_FOR_PICKUP":
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

function buildAddress(order: OrderDetail) {
  return [
    order.addressLine1,
    order.addressLine2,
    order.landmark,
    order.city,
    order.state,
    order.postalCode,
    order.country,
  ]
    .filter(Boolean)
    .join(", ");
}

function TrackingTimeline({
  order,
}: {
  order: OrderDetail;
}) {
  const isDelivery =
    order.fulfillmentType === "DELIVERY";

  const expected = isDelivery
    ? [
        "PENDING",
        "CONFIRMED",
        "PROCESSING",
        "PACKED",
        "SHIPPED",
        "OUT_FOR_DELIVERY",
        "DELIVERED",
        "COMPLETED",
      ]
    : [
        "PENDING",
        "CONFIRMED",
        "READY_FOR_PICKUP",
        "COMPLETED",
      ];

  const historyMap = useMemo(() => {
    const map = new Map<string, string>();

    for (const event of order.statusHistory ?? []) {
      if (!map.has(event.status)) {
        map.set(
          event.status,
          event.createdAt
        );
      }
    }

    return map;
  }, [order.statusHistory]);

  const currentIndex =
    expected.indexOf(order.orderStatus);

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#EAF5F2] text-[#075E54]">
          {isDelivery ? (
            <Truck size={20} />
          ) : (
            <Store size={20} />
          )}
        </div>

        <div>
          <h2 className="font-black text-slate-950">
            {isDelivery
              ? "Delivery Tracking"
              : "Pickup Progress"}
          </h2>

          <p className="mt-0.5 text-xs text-slate-500">
            Current status:{" "}
            {pretty(order.orderStatus)}
          </p>
        </div>
      </div>

      <div className="mt-7">
        {expected.map((status, index) => {
          const completed =
            currentIndex >= 0 &&
            index <= currentIndex;

          const current =
            status === order.orderStatus;

          const timestamp =
            historyMap.get(status);

          return (
            <div
              key={status}
              className="relative flex gap-4 pb-7 last:pb-0"
            >
              {index !==
                expected.length - 1 && (
                <div
                  className={`absolute left-[15px] top-8 h-[calc(100%-20px)] w-0.5 ${
                    completed &&
                    index < currentIndex
                      ? "bg-[#075E54]"
                      : "bg-slate-200"
                  }`}
                />
              )}

              <div
                className={`relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 ${
                  completed
                    ? "border-[#075E54] bg-[#075E54] text-white"
                    : "border-slate-200 bg-white text-slate-300"
                }`}
              >
                {completed ? (
                  <Check size={15} />
                ) : (
                  <Clock3 size={14} />
                )}
              </div>

              <div className="-mt-0.5 min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p
                    className={`text-sm font-extrabold ${
                      completed
                        ? "text-slate-900"
                        : "text-slate-400"
                    }`}
                  >
                    {pretty(status)}
                  </p>

                  {current && (
                    <span className="rounded-full bg-[#EAF5F2] px-2 py-0.5 text-[10px] font-black uppercase tracking-wide text-[#075E54]">
                      Current
                    </span>
                  )}
                </div>

                {timestamp && (
                  <p className="mt-1 text-xs text-slate-500">
                    {dateTime(timestamp)}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function ShipmentCard({
  shipment,
}: {
  shipment: Shipment;
}) {
  return (
    <section className="rounded-3xl border border-[#BFDCD7] bg-[#EAF5F2] p-5 sm:p-6">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white text-[#075E54] shadow-sm">
          <Truck size={22} />
        </div>

        <div>
          <p className="text-xs font-bold uppercase tracking-wide text-[#0B7A69]">
            Shipment
          </p>

          <h2 className="font-black text-slate-950">
            {pretty(
              shipment.shipmentStatus
            )}
          </h2>
        </div>
      </div>

      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <div>
          <p className="text-xs font-semibold text-slate-500">
            Courier
          </p>

          <p className="mt-1 font-bold text-slate-900">
            {shipment.carrierName ||
              "Will be updated"}
          </p>
        </div>

        <div>
          <p className="text-xs font-semibold text-slate-500">
            Tracking number
          </p>

          <p className="mt-1 break-all font-bold text-slate-900">
            {shipment.trackingNumber ||
              "Will be updated"}
          </p>
        </div>
      </div>

      {shipment.trackingUrl && (
        <a
          href={shipment.trackingUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-5 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46] sm:w-auto"
        >
          Track with Courier
          <ExternalLink size={16} />
        </a>
      )}
    </section>
  );
}

export default function CustomerOrderPage({
  params,
}: PageProps) {
  const { orderId } = use(params);

  const [order, setOrder] =
    useState<OrderDetail | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState<string | null>(null);

  const [notFound, setNotFound] =
    useState(false);

  const [cancelling, setCancelling] =
    useState(false);

  const [cancelError, setCancelError] =
    useState<string | null>(null);

  const [paying, setPaying] =
    useState(false);

  const [paymentError, setPaymentError] =
    useState<string | null>(null);

  const [paymentSuccess, setPaymentSuccess] =
    useState<string | null>(null);

  const loadOrder = useCallback(
    async () => {
      setLoading(true);
      setError(null);
      setNotFound(false);

      try {
        const response = await fetch(
          `/api/account/orders/${encodeURIComponent(
            orderId
          )}`,
          {
            method: "GET",
            cache: "no-store",
          }
        );

        const data =
          (await response.json()) as ApiResponse;

        if (response.status === 404) {
          setOrder(null);
          setNotFound(true);
          return;
        }

        if (!response.ok || !data.order) {
          throw new Error(
            data.error ||
              "Unable to load order details."
          );
        }

        setOrder(data.order);
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Unable to load order details."
        );
      } finally {
        setLoading(false);
      }
    },
    [orderId]
  );

  useEffect(() => {
    void loadOrder();
  }, [loadOrder]);

  function loadRazorpayCheckout(): Promise<boolean> {
    return new Promise((resolve) => {
      if (window.Razorpay) {
        resolve(true);
        return;
      }

      const existingScript =
        document.querySelector<HTMLScriptElement>(
          'script[src="https://checkout.razorpay.com/v1/checkout.js"]'
        );

      if (existingScript) {
        existingScript.addEventListener(
          "load",
          () => resolve(Boolean(window.Razorpay)),
          { once: true }
        );

        existingScript.addEventListener(
          "error",
          () => resolve(false),
          { once: true }
        );

        return;
      }

      const script =
        document.createElement("script");

      script.src =
        "https://checkout.razorpay.com/v1/checkout.js";

      script.async = true;

      script.onload = () =>
        resolve(Boolean(window.Razorpay));

      script.onerror = () =>
        resolve(false);

      document.body.appendChild(script);
    });
  }

  async function startOnlinePayment() {
    if (paying || !order) return;

    const eligible =
      order.paymentMethod === "ONLINE" &&
      order.paymentStatus === "PENDING" &&
      order.orderStatus !== "CANCELLED" &&
      order.orderStatus !== "COMPLETED";

    if (!eligible) return;

    setPaying(true);
    setPaymentError(null);
    setPaymentSuccess(null);

    try {
      const checkoutLoaded =
        await loadRazorpayCheckout();

      if (!checkoutLoaded || !window.Razorpay) {
        throw new Error(
          "Unable to load the secure payment window."
        );
      }

      const orderResponse = await fetch(
        `/api/account/orders/${encodeURIComponent(
          orderId
        )}/payment/razorpay`,
        {
          method: "POST",
          cache: "no-store",
        }
      );

      const orderData =
        (await orderResponse.json()) as {
          paymentOrder?: RazorpayOrder;
          error?: string;
        };

      if (
        !orderResponse.ok ||
        !orderData.paymentOrder
      ) {
        throw new Error(
          orderData.error ||
            "Unable to start online payment."
        );
      }

      const paymentOrder =
        orderData.paymentOrder;

      if (
        !paymentOrder.razorpayOrderId ||
        !paymentOrder.keyId ||
        !Number.isSafeInteger(
          paymentOrder.amountPaise
        ) ||
        paymentOrder.amountPaise <= 0 ||
        paymentOrder.currency !== "INR"
      ) {
        throw new Error(
          "Invalid payment order received."
        );
      }

      const options: RazorpayCheckoutOptions = {
        key: paymentOrder.keyId,
        amount: paymentOrder.amountPaise,
        currency: paymentOrder.currency,
        name: "KCMART",
        description:
          `Payment for ${order.orderNumber}`,
        order_id:
          paymentOrder.razorpayOrderId,

        prefill: {
          name: order.customerName,
          email: order.customerEmail,
          contact: order.customerPhone,
        },

        theme: {
          color: "#075E54",
        },

        modal: {
          ondismiss: () => {
            setPaying(false);
          },
        },

        handler: async (
          response: RazorpaySuccessResponse
        ) => {
          try {
            const verifyResponse = await fetch(
              `/api/account/orders/${encodeURIComponent(
                orderId
              )}/payment/razorpay/verify`,
              {
                method: "POST",
                headers: {
                  "Content-Type":
                    "application/json",
                },
                body: JSON.stringify(response),
              }
            );

            const verifyData =
              (await verifyResponse.json()) as {
                verified?: boolean;
                outcome?: string;
                error?: string;
              };

            if (
              verifyData.outcome ===
              "RECONCILIATION_REQUIRED"
            ) {
              setPaymentSuccess("");
              setPaymentError(
                "Payment received, but your order expired. Your payment is under review. Please do not pay again for this order."
              );

              await loadOrder();
              return;
            }

            if (
              !verifyResponse.ok ||
              !verifyData.verified
            ) {
              throw new Error(
                verifyData.error ||
                  "Payment could not be verified."
              );
            }

            setPaymentSuccess(
              "Payment verified successfully."
            );

            await loadOrder();
          } catch (verificationError) {
            setPaymentError(
              verificationError instanceof Error
                ? verificationError.message
                : "Payment verification failed."
            );
          } finally {
            setPaying(false);
          }
        },
      };

      const razorpay =
        new window.Razorpay(options);

      razorpay.on(
        "payment.failed",
        () => {
          setPaymentError(
            "Payment was not completed. You can try again."
          );
          setPaying(false);
        }
      );

      razorpay.open();
    } catch (paymentRequestError) {
      setPaymentError(
        paymentRequestError instanceof Error
          ? paymentRequestError.message
          : "Unable to start payment."
      );

      setPaying(false);
    }
  }
  async function cancelOrder() {
    if (cancelling || !order) return;

    const eligible =
      order.fulfillmentType === "STORE_PICKUP" &&
      order.orderStatus === "PENDING" &&
      order.paymentMethod === "PAY_AT_STORE" &&
      order.paymentStatus !== "PAID";

    if (!eligible) return;

    const confirmed = window.confirm(
      "Cancel this order? This action cannot be undone."
    );

    if (!confirmed) return;

    setCancelling(true);
    setCancelError(null);

    try {
      const response = await fetch(
        `/api/account/orders/${encodeURIComponent(
          orderId
        )}`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            note: "Cancelled by customer.",
          }),
        }
      );

      const data =
        (await response.json()) as ApiResponse;

      if (!response.ok) {
        throw new Error(
          data.error ||
            "Unable to cancel this order."
        );
      }

      if (data.order) {
        setOrder(data.order);
      } else {
        await loadOrder();
      }
    } catch (requestError) {
      setCancelError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to cancel this order."
      );
    } finally {
      setCancelling(false);
    }
  }

  if (loading) {
    return (
      <main className="min-h-[70vh] bg-slate-50 px-4 py-10">
        <div className="mx-auto flex max-w-4xl flex-col items-center justify-center py-24">
          <LoaderCircle
            size={36}
            className="animate-spin text-[#075E54]"
          />

          <p className="mt-4 text-sm font-semibold text-slate-500">
            Loading your order...
          </p>
        </div>
      </main>
    );
  }

  if (notFound) {
    return (
      <main className="min-h-[70vh] bg-slate-50 px-4 py-10">
        <div className="mx-auto max-w-lg rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <Package
            size={44}
            className="mx-auto text-slate-300"
          />

          <h1 className="mt-5 text-2xl font-black text-slate-950">
            Order not found
          </h1>

          <p className="mt-2 text-sm leading-6 text-slate-500">
            This order is unavailable or is
            not associated with your account.
          </p>

          <Link
            href="/account"
            className="mt-6 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white"
          >
            <ArrowLeft size={17} />
            Back to My Orders
          </Link>
        </div>
      </main>
    );
  }

  if (error || !order) {
    return (
      <main className="min-h-[70vh] bg-slate-50 px-4 py-10">
        <div className="mx-auto max-w-lg rounded-3xl border border-red-100 bg-white p-8 text-center shadow-sm">
          <h1 className="text-xl font-black text-slate-950">
            Unable to load order
          </h1>

          <p className="mt-2 text-sm text-red-600">
            {error ||
              "Something went wrong."}
          </p>

          <button
            type="button"
            onClick={() =>
              void loadOrder()
            }
            className="mt-6 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 text-sm font-bold text-white"
          >
            <RefreshCw size={16} />
            Try Again
          </button>
        </div>
      </main>
    );
  }

  const isDelivery =
    order.fulfillmentType === "DELIVERY";

  const canCancelOrder =
    order.fulfillmentType === "STORE_PICKUP" &&
    order.orderStatus === "PENDING" &&
    order.paymentMethod === "PAY_AT_STORE" &&
    order.paymentStatus !== "PAID";

  const address = buildAddress(order);

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-6 sm:py-10">
      <div className="mx-auto max-w-5xl">
        <Link
          href="/account"
          className="inline-flex items-center gap-2 text-sm font-bold text-slate-600 transition hover:text-[#075E54]"
        >
          <ArrowLeft size={17} />
          My Orders
        </Link>

        <section className="mt-5 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="bg-gradient-to-br from-[#EAF5F2] via-white to-slate-50 p-5 sm:p-8">
            <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-start">
              <div>
                <p className="text-xs font-black uppercase tracking-widest text-[#0B7A69]">
                  KCMART Order
                </p>

                <h1 className="mt-2 break-all text-xl font-black text-slate-950 sm:text-3xl">
                  {order.orderNumber}
                </h1>

                <p className="mt-2 flex items-center gap-2 text-sm text-slate-500">
                  <CalendarDays size={15} />
                  {dateTime(order.createdAt)}
                </p>
              </div>

              <span
                className={`w-fit rounded-full border px-4 py-2 text-xs font-black ${statusClass(
                  order.orderStatus
                )}`}
              >
                {pretty(order.orderStatus)}
              </span>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              <div className="rounded-2xl bg-white p-4 shadow-sm">
                <CreditCard
                  size={18}
                  className="text-[#075E54]"
                />
                <p className="mt-2 text-xs text-slate-400">
                  Payment
                </p>
                <p className="mt-1 text-sm font-black text-slate-900">
                  {pretty(
                    order.paymentStatus
                  )}
                </p>
              </div>

              <div className="rounded-2xl bg-white p-4 shadow-sm">
                {isDelivery ? (
                  <Truck
                    size={18}
                    className="text-[#075E54]"
                  />
                ) : (
                  <Store
                    size={18}
                    className="text-[#075E54]"
                  />
                )}

                <p className="mt-2 text-xs text-slate-400">
                  Fulfilment
                </p>

                <p className="mt-1 text-sm font-black text-slate-900">
                  {pretty(
                    order.fulfillmentType
                  )}
                </p>
              </div>

              <div className="rounded-2xl bg-white p-4 shadow-sm">
                <ReceiptText
                  size={18}
                  className="text-[#075E54]"
                />
                <p className="mt-2 text-xs text-slate-400">
                  Method
                </p>
                <p className="mt-1 text-sm font-black text-slate-900">
                  {pretty(
                    order.paymentMethod
                  )}
                </p>
              </div>

              <div className="rounded-2xl bg-white p-4 shadow-sm">
                <ShoppingBag
                  size={18}
                  className="text-[#075E54]"
                />
                <p className="mt-2 text-xs text-slate-400">
                  Total
                </p>
                <p className="mt-1 text-sm font-black text-[#075E54]">
                  {money(
                    order.totalAmount
                  )}
                </p>
              </div>
            </div>
          </div>
        </section>

        <div className="mt-5 grid gap-5 lg:grid-cols-[1.35fr_0.85fr]">
          <div className="space-y-5">
            <TrackingTimeline
              order={order}
            />

            {isDelivery &&
              order.shipment && (
                <ShipmentCard
                  shipment={
                    order.shipment
                  }
                />
              )}

            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
              <div className="flex items-center gap-3">
                <PackageCheck
                  size={21}
                  className="text-[#075E54]"
                />

                <h2 className="font-black text-slate-950">
                  Order Items
                </h2>
              </div>

              <div className="mt-5 divide-y divide-slate-100">
                {order.items.map(
                  (item) => (
                    <div
                      key={`${item.productId}-${item.sku}`}
                      className="flex items-start justify-between gap-4 py-4 first:pt-0 last:pb-0"
                    >
                      <div className="min-w-0">
                        <p className="font-bold text-slate-900">
                          {
                            item.productName
                          }
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          SKU: {item.sku}
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          {money(
                            item.unitPrice
                          )}{" "}
                          Ã— {item.quantity}
                        </p>
                      </div>

                      <p className="shrink-0 font-black text-slate-900">
                        {money(
                          item.lineTotal
                        )}
                      </p>
                    </div>
                  )
                )}
              </div>
            </section>
          </div>

          <div className="space-y-5">
            {order.invoice?.available && (
              <section className="rounded-3xl border border-[#BFDCD7] bg-[#EAF5F2] p-5 shadow-sm sm:p-6">
                <div className="flex items-start gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white text-[#075E54] shadow-sm">
                    <FileText size={21} />
                  </div>

                  <div className="min-w-0">
                    <p className="text-xs font-black uppercase tracking-wide text-[#0B7A69]">
                      Invoice
                    </p>

                    <h2 className="mt-1 font-black text-slate-950">
                      {order.invoice.invoiceNumber}
                    </h2>

                    <p className="mt-1 break-all text-xs text-slate-500">
                      {order.invoice.originalFilename}
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      Uploaded {dateTime(order.invoice.uploadedAt)}
                    </p>
                  </div>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2">
                  <a
                    href={`/api/account/orders/${encodeURIComponent(orderId)}/invoice`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#075E54] px-4 text-sm font-extrabold text-white transition hover:bg-[#064E46]"
                  >
                    <FileText size={17} />
                    View Invoice
                  </a>

                  <a
                    href={`/api/account/orders/${encodeURIComponent(orderId)}/invoice`}
                    download={order.invoice.originalFilename || "invoice.pdf"}
                    className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-[#BFDCD7] bg-white px-4 text-sm font-extrabold text-[#075E54] transition hover:bg-[#F4F9F7]"
                  >
                    <Package size={17} />
                    Download Invoice
                  </a>
                </div>
              </section>
            )}
            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <h2 className="font-black text-slate-950">
                Payment Summary
              </h2>

              <div className="mt-5 space-y-3 text-sm">
                <div className="flex justify-between gap-4">
                  <span className="text-slate-500">
                    Subtotal
                  </span>
                  <span className="font-bold text-slate-900">
                    {money(
                      order.subtotal
                    )}
                  </span>
                </div>

                <div className="flex justify-between gap-4">
                  <span className="text-slate-500">
                    Delivery
                  </span>
                  <span className="font-bold text-slate-900">
                    {Number(
                      order.deliveryCharge
                    ) === 0
                      ? "Free"
                      : money(
                          order.deliveryCharge
                        )}
                  </span>
                </div>

                <div className="border-t border-slate-100 pt-3">
                  <div className="flex items-center justify-between gap-4">
                    <span className="font-black text-slate-950">
                      Total
                    </span>
                    <span className="text-lg font-black text-[#075E54]">
                      {money(
                        order.totalAmount
                      )}
                    </span>
                  </div>
                </div>

                <div
                  className={`mt-4 flex items-center gap-2 rounded-xl px-3 py-3 ${
                    order.paymentStatus ===
                    "PAID"
                      ? "bg-emerald-50 text-emerald-700"
                      : "bg-amber-50 text-amber-700"
                  }`}
                >
                  {order.paymentStatus ===
                  "PAID" ? (
                    <CheckCircle2
                      size={18}
                    />
                  ) : (
                    <Clock3 size={18} />
                  )}

                  <span className="text-xs font-black">
                    {pretty(
                      order.paymentStatus
                    )}{" "}
                    Â·{" "}
                    {pretty(
                      order.paymentMethod
                    )}
                  </span>
                </div>
              </div>
            </section>

            {order.paymentMethod === "ONLINE" &&
              order.paymentStatus === "PENDING" &&
              order.orderStatus !== "CANCELLED" &&
              order.orderStatus !== "COMPLETED" && (
                <section className="rounded-3xl border border-[#BFDCD7] bg-white p-5 shadow-sm sm:p-6">
                  <div className="flex items-start gap-3">
                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#EAF5F2] text-[#075E54]">
                      <CreditCard size={21} />
                    </div>

                    <div>
                      <h2 className="font-black text-slate-950">
                        Complete Payment
                      </h2>

                      <p className="mt-1 text-sm leading-6 text-slate-500">
                        Pay securely online to continue processing your order.
                      </p>
                    </div>
                  </div>

                  <div className="mt-5 rounded-2xl bg-[#EAF5F2] p-4">
                    <div className="flex items-center justify-between gap-4">
                      <span className="text-sm font-semibold text-slate-600">
                        Amount to pay
                      </span>

                      <span className="text-xl font-black text-[#075E54]">
                        {money(order.totalAmount)}
                      </span>
                    </div>
                  </div>

                  {paymentError && (
                    <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
                      {paymentError}
                    </div>
                  )}

                  {paymentSuccess && (
                    <div className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">
                      {paymentSuccess}
                    </div>
                  )}

                  <button
                    type="button"
                    disabled={paying}
                    onClick={() =>
                      void startOnlinePayment()
                    }
                    className="mt-5 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-sm font-black text-white transition hover:bg-[#064E46] disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {paying ? (
                      <>
                        <LoaderCircle
                          size={18}
                          className="animate-spin"
                        />
                        Processing...
                      </>
                    ) : (
                      <>
                        <CreditCard size={18} />
                        Pay {money(order.totalAmount)} Securely
                      </>
                    )}
                  </button>

                  <p className="mt-3 text-center text-[11px] leading-5 text-slate-400">
                    Secure payment powered by Razorpay
                  </p>
                </section>
              )}
            {canCancelOrder && (
              <section className="rounded-3xl border border-red-200 bg-white p-5 shadow-sm sm:p-6">
                <h2 className="font-black text-slate-950">
                  Cancel Order
                </h2>

                <p className="mt-2 text-sm leading-6 text-slate-500">
                  You can cancel this order because it has not
                  been confirmed or paid yet. Reserved store
                  stock will be released automatically.
                </p>

                {cancelError && (
                  <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
                    {cancelError}
                  </div>
                )}

                <button
                  type="button"
                  disabled={cancelling}
                  onClick={() => void cancelOrder()}
                  className="mt-5 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 px-5 text-sm font-extrabold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {cancelling ? (
                    <>
                      <LoaderCircle
                        size={17}
                        className="animate-spin"
                      />
                      Cancelling...
                    </>
                  ) : (
                    "Cancel Order"
                  )}
                </button>
              </section>
            )}

            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <div className="flex items-center gap-2">
                {isDelivery ? (
                  <MapPin
                    size={19}
                    className="text-[#075E54]"
                  />
                ) : (
                  <Store
                    size={19}
                    className="text-[#075E54]"
                  />
                )}

                <h2 className="font-black text-slate-950">
                  {isDelivery
                    ? "Delivery Details"
                    : "Store Pickup"}
                </h2>
              </div>

              <div className="mt-4 text-sm leading-6 text-slate-600">
                <p className="font-bold text-slate-900">
                  {order.customerName}
                </p>

                <p>
                  {order.customerPhone}
                </p>

                {isDelivery ? (
                  <p className="mt-3">
                    {address ||
                      "Delivery address unavailable."}
                  </p>
                ) : (
                  <div className="mt-3 rounded-xl bg-[#EAF5F2] p-3">
                    <p className="font-bold text-slate-900">
                      Khariar Computer Service
                    </p>
                    <p className="mt-1">
                      College Road, Near
                      Koshaleswar Temple,
                      Raj Khariar, Gadramunda,
                      Odisha - 766107
                    </p>
                    <p className="mt-1 font-semibold">
                      8637277240
                    </p>
                  </div>
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </main>
  );
}