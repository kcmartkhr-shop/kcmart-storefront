import Link from "next/link";
import { cookies } from "next/headers";
import { redirect, notFound } from "next/navigation";
import { getAdminSession } from "@/lib/admin/session";
import OrderControlCenter from "./OrderControlCenter";

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

type AdminOrder = {
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

type Invoice = {
  id: number;
  orderId: number;
  invoiceNumber: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  uploadedBy: string;
  createdAt: string;
  updatedAt: string;
};

type Shipment = {
  id: number;
  orderId: number;
  carrierName: string | null;
  trackingNumber: string | null;
  trackingUrl: string | null;
  shipmentStatus: string;
  shippedAt: string | null;
  outForDeliveryAt: string | null;
  deliveredAt: string | null;
  adminNote: string | null;
  createdBy: string;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string;
};

type Notification = {
  id: number;
  notificationType: string;
  channel: string;
  recipient: string;
  subject: string;
  status: string;
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
  sentAt: string | null;
};

function money(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
  }).format(value);
}

function dateTime(value: string | null) {
  if (!value) return "-";

  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Kolkata",
  }).format(new Date(value));
}

function pretty(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map(
      (part) =>
        part.charAt(0).toUpperCase() + part.slice(1)
    )
    .join(" ");
}

function badge(status: string) {
  switch (status) {
    case "COMPLETED":
    case "PAID":
    case "SENT":
      return "bg-emerald-50 text-emerald-700 border-emerald-200";

    case "FAILED":
    case "CANCELLED":
      return "bg-red-50 text-red-700 border-red-200";

    case "PENDING":
      return "bg-amber-50 text-amber-700 border-amber-200";

    default:
      return "bg-[#EAF5F2] text-[#075E54] border-[#BFDCD5]";
  }
}

async function getData(id: string) {
  const cookieStore = await cookies();

  const sessionId = cookieStore.get(
    "kcmart_admin_session"
  )?.value;

  const session = getAdminSession(sessionId);

  if (!session) {
    redirect("/admin/login");
  }

  const api =
    process.env.KCMART_API_URL ??
    "http://127.0.0.1:8080";

  const auth = Buffer.from(
    `${session.username}:${session.password}`
  ).toString("base64");

  const headers = {
    Authorization: `Basic ${auth}`,
  };

  const orderResponse = await fetch(
    `${api}/api/v1/admin/orders/${id}`,
    {
      headers,
      cache: "no-store",
    }
  );

  if (orderResponse.status === 404) {
    notFound();
  }

  if (!orderResponse.ok) {
    throw new Error(
      `Unable to load order (${orderResponse.status}).`
    );
  }

  const order =
    (await orderResponse.json()) as AdminOrder;

  const [
    invoiceResponse,
    notificationResponse,
    shipmentResponse,
  ] = await Promise.all([
    fetch(
      `${api}/api/v1/admin/orders/${id}/invoice`,
      {
        headers,
        cache: "no-store",
      }
    ),
    fetch(
      `${api}/api/v1/admin/orders/${id}/notifications`,
      {
        headers,
        cache: "no-store",
      }
    ),
    order.fulfillmentType === "DELIVERY"
      ? fetch(
          `${api}/api/v1/admin/orders/${id}/shipment`,
          {
            headers,
            cache: "no-store",
          }
        )
      : Promise.resolve(null),
  ]);

  let invoice: Invoice | null = null;

  if (invoiceResponse.ok) {
    invoice =
      (await invoiceResponse.json()) as Invoice;
  } else if (invoiceResponse.status !== 404) {
    throw new Error(
      `Unable to load invoice (${invoiceResponse.status}).`
    );
  }

  if (!notificationResponse.ok) {
    throw new Error(
      `Unable to load notifications (${notificationResponse.status}).`
    );
  }

  const notifications =
    (await notificationResponse.json()) as Notification[];

  let shipment: Shipment | null = null;

  if (shipmentResponse) {
    if (shipmentResponse.ok) {
      shipment =
        (await shipmentResponse.json()) as Shipment;
    } else if (shipmentResponse.status !== 404) {
      throw new Error(
        `Unable to load shipment (${shipmentResponse.status}).`
      );
    }
  }

  return {
    order,
    invoice,
    notifications,
    shipment,
  };
}

export default async function AdminOrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  if (!/^\d+$/.test(id)) {
    notFound();
  }

  const {
    order,
    invoice,
    notifications,
    shipment,
  } = await getData(id);

  const address = [
    order.addressLine1,
    order.addressLine2,
    order.landmark,
    order.city,
    order.state,
    order.postalCode,
    order.country,
  ].filter(Boolean);

  return (
    <main className="min-h-screen bg-slate-50">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <div className="mb-6">
          <Link
            href="/admin/orders"
            className="text-sm font-medium text-slate-600 hover:text-slate-950"
          >
            ← Back to orders
          </Link>

          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p className="text-sm text-slate-500">
                Order #{order.id}
              </p>

              <h1 className="mt-1 break-all text-xl font-bold text-slate-950 sm:text-2xl">
                {order.orderNumber}
              </h1>

              <p className="mt-2 text-sm text-slate-500">
                Created {dateTime(order.createdAt)}
              </p>
            </div>

            <span
              className={`w-fit rounded-full border px-3 py-1 text-xs font-semibold ${badge(
                order.orderStatus
              )}`}
            >
              {pretty(order.orderStatus)}
            </span>
          </div>
        </div>

        <div className="grid min-w-0 gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(360px,1fr)]">
          <div className="min-w-0 space-y-6">
            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-base font-bold text-slate-950">
                Products
              </h2>

              <div className="mt-4 divide-y divide-slate-100">
                {order.items.map((item) => (
                  <div
                    key={item.id}
                    className="flex gap-4 py-4 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-slate-900">
                        {item.productName}
                      </p>

                      <p className="mt-1 text-xs text-slate-500">
                        SKU: {item.sku}
                      </p>

                      <p className="mt-1 text-sm text-slate-600">
                        {money(item.unitPrice)} ×{" "}
                        {item.quantity}
                      </p>
                    </div>

                    <p className="font-semibold text-slate-950">
                      {money(item.lineTotal)}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-5 border-t border-slate-200 pt-4 text-sm">
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">
                    Subtotal
                  </span>
                  <span>{money(order.subtotal)}</span>
                </div>

                <div className="flex justify-between py-1">
                  <span className="text-slate-500">
                    Delivery
                  </span>
                  <span>
                    {money(order.deliveryCharge)}
                  </span>
                </div>

                <div className="mt-2 flex justify-between border-t border-slate-100 pt-3 text-base font-bold">
                  <span>Total</span>
                  <span>{money(order.totalAmount)}</span>
                </div>
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-base font-bold text-slate-950">
                Order Timeline
              </h2>

              <div className="mt-5 space-y-5">
                {order.history.length === 0 ? (
                  <p className="text-sm text-slate-500">
                    No lifecycle history is available for this order.
                  </p>
                ) : (
                  order.history.map((event) => (
                    <div
                      key={event.id}
                      className="relative border-l-2 border-slate-200 pl-5"
                    >
                      <div className="absolute -left-[7px] top-1 h-3 w-3 rounded-full bg-slate-900" />

                      <p className="font-semibold text-slate-900">
                        {pretty(event.newStatus)}
                      </p>

                      <p className="mt-1 text-xs text-slate-500">
                        {dateTime(event.createdAt)}
                        {" · "}
                        {event.actorType}
                        {" · "}
                        {event.actorIdentifier}
                      </p>

                      {event.note && (
                        <p className="mt-2 text-sm text-slate-600">
                          {event.note}
                        </p>
                      )}
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>

          <div className="space-y-6">
            <OrderControlCenter
              orderId={order.id}
              fulfillmentType={order.fulfillmentType}
              paymentMethod={order.paymentMethod}
              paymentStatus={order.paymentStatus}
              orderStatus={order.orderStatus}
              hasInvoice={invoice !== null}
              shipment={shipment}
            />

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-bold text-slate-950">
                Customer
              </h2>

              <div className="mt-4 space-y-3 text-sm">
                <div>
                  <p className="text-xs text-slate-500">
                    Name
                  </p>
                  <p className="font-medium">
                    {order.customerName}
                  </p>
                </div>

                <div>
                  <p className="text-xs text-slate-500">
                    Mobile
                  </p>
                  <p className="font-medium">
                    {order.customerPhone}
                  </p>
                </div>

                <div>
                  <p className="text-xs text-slate-500">
                    Email
                  </p>
                  <p className="break-all font-medium">
                    {order.customerEmail}
                  </p>
                </div>
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-bold text-slate-950">
                Fulfillment
              </h2>

              <p className="mt-4 font-semibold">
                {pretty(order.fulfillmentType)}
              </p>

              {order.fulfillmentType ===
              "STORE_PICKUP" ? (
                <div className="mt-3 text-sm leading-6 text-slate-600">
                  <p className="font-medium text-slate-900">
                    Khariar Computer Service
                  </p>
                  <p>
                    College Road, Near Koshaleswar Temple
                  </p>
                  <p>Raj Khariar, Gadramunda</p>
                  <p>Odisha - 766107</p>
                  <p>Mobile: 8637277240</p>
                </div>
              ) : (
                <div className="mt-3 text-sm leading-6 text-slate-600">
                  {address.map((line, index) => (
                    <p key={index}>{line}</p>
                  ))}
                </div>
              )}
            </section>

            {order.fulfillmentType === "DELIVERY" && (
              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <h2 className="font-bold text-slate-950">
                    Shipping & Tracking
                  </h2>

                  {shipment && (
                    <span
                      className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${badge(
                        shipment.shipmentStatus
                      )}`}
                    >
                      {pretty(shipment.shipmentStatus)}
                    </span>
                  )}
                </div>

                {shipment ? (
                  <div className="mt-4 space-y-4 text-sm">
                    <div>
                      <p className="text-xs text-slate-500">
                        Courier / Carrier
                      </p>
                      <p className="font-medium text-slate-900">
                        {shipment.carrierName || "-"}
                      </p>
                    </div>

                    <div>
                      <p className="text-xs text-slate-500">
                        Tracking Number
                      </p>
                      <p className="break-all font-medium text-slate-900">
                        {shipment.trackingNumber || "-"}
                      </p>
                    </div>

                    {shipment.trackingUrl && (
                      <div>
                        <p className="text-xs text-slate-500">
                          Tracking Link
                        </p>

                        <a
                          href={shipment.trackingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-1 inline-flex break-all font-semibold text-[#075E54] hover:underline"
                        >
                          Open courier tracking
                        </a>
                      </div>
                    )}

                    <div className="grid grid-cols-1 gap-3 border-t border-slate-100 pt-4">
                      <div>
                        <p className="text-xs text-slate-500">
                          Shipped
                        </p>
                        <p className="font-medium">
                          {dateTime(shipment.shippedAt)}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-slate-500">
                          Out for Delivery
                        </p>
                        <p className="font-medium">
                          {dateTime(
                            shipment.outForDeliveryAt
                          )}
                        </p>
                      </div>

                      <div>
                        <p className="text-xs text-slate-500">
                          Delivered
                        </p>
                        <p className="font-medium">
                          {dateTime(shipment.deliveredAt)}
                        </p>
                      </div>
                    </div>

                    {shipment.adminNote && (
                      <div className="rounded-xl bg-slate-50 p-3">
                        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Admin Note
                        </p>

                        <p className="mt-1 leading-5 text-slate-700">
                          {shipment.adminNote}
                        </p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="mt-4 rounded-xl bg-slate-50 p-4">
                    <p className="text-sm font-medium text-slate-700">
                      Shipping details not added yet.
                    </p>

                    <p className="mt-1 text-xs leading-5 text-slate-500">
                      Carrier and tracking information can be
                      added when this order is prepared for
                      shipment.
                    </p>
                  </div>
                )}
              </section>
            )}

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-bold text-slate-950">
                Payment
              </h2>

              <div className="mt-4 space-y-3 text-sm">
                <div>
                  <p className="text-xs text-slate-500">
                    Method
                  </p>
                  <p className="font-medium">
                    {pretty(order.paymentMethod)}
                  </p>
                </div>

                <div>
                  <p className="text-xs text-slate-500">
                    Status
                  </p>

                  <span
                    className={`mt-1 inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${badge(
                      order.paymentStatus
                    )}`}
                  >
                    {pretty(order.paymentStatus)}
                  </span>
                </div>
              </div>
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-bold text-slate-950">
                Invoice
              </h2>

              {invoice ? (
                <div className="mt-4 space-y-2 text-sm">
                  <p className="break-all font-semibold text-slate-900">
                    {invoice.invoiceNumber}
                  </p>

                  <p className="break-all text-slate-600">
                    {invoice.originalFilename}
                  </p>

                  <p className="text-xs text-slate-500">
                    Uploaded by {invoice.uploadedBy}
                  </p>

                  <p className="text-xs text-slate-500">
                    {dateTime(invoice.createdAt)}
                  </p>
                </div>
              ) : (
                <div className="mt-4 rounded-xl bg-slate-50 p-4 text-sm text-slate-600">
                  No invoice uploaded.
                </div>
              )}
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-bold text-slate-950">
                Email Notifications
              </h2>

              <div className="mt-4 space-y-4">
                {notifications.length === 0 ? (
                  <p className="text-sm text-slate-500">
                    No email notification has been created.
                  </p>
                ) : (
                  notifications.map((notification) => (
                    <div
                      key={notification.id}
                      className="rounded-xl border border-slate-200 p-3"
                    >
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-sm font-semibold text-slate-900">
                          {pretty(
                            notification.notificationType
                          )}
                        </p>

                        <span
                          className={`rounded-full border px-2 py-0.5 text-[11px] font-semibold ${badge(
                            notification.status
                          )}`}
                        >
                          {pretty(notification.status)}
                        </span>
                      </div>

                      <p className="mt-2 break-all text-xs text-slate-500">
                        {notification.recipient}
                      </p>

                      <p className="mt-2 text-xs text-slate-500">
                        Attempts:{" "}
                        {notification.attemptCount}
                      </p>

                      {notification.sentAt && (
                        <p className="mt-1 text-xs text-slate-500">
                          Sent {dateTime(notification.sentAt)}
                        </p>
                      )}

                      {notification.lastError && (
                        <div className="mt-3 rounded-lg bg-red-50 p-2 text-xs text-red-700">
                          {notification.lastError}
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </main>
  );
}