"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

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

type Props = {
  orderId: number;
  fulfillmentType: string;
  paymentMethod: string;
  paymentStatus: string;
  orderStatus: string;
  hasInvoice: boolean;
  shipment: Shipment | null;
};

type NextAction = {
  status: string;
  label: string;
  note: string;
};

function getNextAction(
  fulfillmentType: string,
  orderStatus: string
): NextAction | null {
  if (fulfillmentType === "STORE_PICKUP") {
    const actions: Record<string, NextAction> = {
      PENDING: {
        status: "CONFIRMED",
        label: "Confirm Order",
        note: "Order confirmed by admin.",
      },
      CONFIRMED: {
        status: "READY_FOR_PICKUP",
        label: "Mark Ready for Pickup",
        note: "Order prepared and ready for customer pickup.",
      },
    };

    return actions[orderStatus] ?? null;
  }

  if (fulfillmentType === "DELIVERY") {
    const actions: Record<string, NextAction> = {
      PENDING: {
        status: "CONFIRMED",
        label: "Confirm Order",
        note: "Order confirmed by admin.",
      },
      CONFIRMED: {
        status: "PROCESSING",
        label: "Start Processing",
        note: "Order processing started.",
      },
      PROCESSING: {
        status: "PACKED",
        label: "Mark Packed",
        note: "Order packed and ready for shipment.",
      },
      PACKED: {
        status: "SHIPPED",
        label: "Mark Shipped",
        note: "Order handed over for shipment.",
      },
      SHIPPED: {
        status: "OUT_FOR_DELIVERY",
        label: "Out for Delivery",
        note: "Order is out for delivery.",
      },
      OUT_FOR_DELIVERY: {
        status: "DELIVERED",
        label: "Mark Delivered",
        note: "Order delivered to customer.",
      },
      DELIVERED: {
        status: "COMPLETED",
        label: "Complete Order",
        note: "Delivery order completed.",
      },
    };

    return actions[orderStatus] ?? null;
  }

  return null;
}

async function readError(response: Response) {
  try {
    const data = await response.json();

    return (
      data?.message ||
      data?.error ||
      `Request failed (${response.status}).`
    );
  } catch {
    return `Request failed (${response.status}).`;
  }
}

export default function OrderControlCenter({
  orderId,
  fulfillmentType,
  paymentMethod,
  paymentStatus,
  orderStatus,
  hasInvoice,
  shipment,
}: Props) {
  const router = useRouter();

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(
    null
  );

  const [invoiceFile, setInvoiceFile] =
    useState<File | null>(null);

  const [carrierName, setCarrierName] = useState(
    shipment?.carrierName ?? ""
  );

  const [trackingNumber, setTrackingNumber] = useState(
    shipment?.trackingNumber ?? ""
  );

  const [trackingUrl, setTrackingUrl] = useState(
    shipment?.trackingUrl ?? ""
  );

  const [shippingNote, setShippingNote] = useState(
    shipment?.adminNote ?? ""
  );

  const [trackingBusy, setTrackingBusy] = useState(false);
  const [trackingError, setTrackingError] = useState<string | null>(null);
  const [trackingResult, setTrackingResult] = useState<unknown>(null);

  const isDelhivery =
    carrierName.trim().toLowerCase() === "delhivery";

  const nextAction = getNextAction(
    fulfillmentType,
    orderStatus
  );

  const deliveryAwaitingPayment =
    fulfillmentType === "DELIVERY" &&
    orderStatus === "PENDING" &&
    (
      paymentMethod !== "ONLINE" ||
      paymentStatus !== "PAID"
    );

  const canCancelOrder =
    fulfillmentType === "STORE_PICKUP" &&
    orderStatus === "PENDING" &&
    paymentMethod === "PAY_AT_STORE" &&
    paymentStatus !== "PAID";

  const canMarkPayment =
    fulfillmentType === "STORE_PICKUP" &&
    paymentMethod === "PAY_AT_STORE" &&
    orderStatus === "READY_FOR_PICKUP" &&
    paymentStatus === "PENDING";

  const canCompletePickup =
    fulfillmentType === "STORE_PICKUP" &&
    orderStatus === "READY_FOR_PICKUP" &&
    paymentStatus === "PAID" &&
    hasInvoice;

  const deliveryInvoiceStatuses = [
    "PACKED",
    "SHIPPED",
    "OUT_FOR_DELIVERY",
    "DELIVERED",
  ];

  const canUploadDeliveryInvoice =
    fulfillmentType === "DELIVERY" &&
    paymentMethod === "ONLINE" &&
    paymentStatus === "PAID" &&
    deliveryInvoiceStatuses.includes(orderStatus) &&
    !hasInvoice;

  const canCompleteDelivery =
    fulfillmentType === "DELIVERY" &&
    paymentMethod === "ONLINE" &&
    paymentStatus === "PAID" &&
    orderStatus === "DELIVERED" &&
    shipment?.shipmentStatus === "DELIVERED" &&
    hasInvoice;

  const blockGenericDeliveryCompletion =
    fulfillmentType === "DELIVERY" &&
    nextAction?.status === "COMPLETED";

  async function updateStatus(
    status: string,
    note: string
  ) {
    if (busy) return;

    setBusy(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/admin/orders/${orderId}/status`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            status,
            note,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to update order."
      );
    } finally {
      setBusy(false);
    }
  }

  async function markPaymentReceived() {
    if (busy) return;

    setBusy(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/admin/orders/${orderId}/payment/received`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            note: "Pay at Store payment received by admin.",
          }),
        }
      );

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to update payment."
      );
    } finally {
      setBusy(false);
    }
  }

  async function cancelOrder() {
    if (busy || !canCancelOrder) return;

    const confirmed = window.confirm(
      "Cancel this order? Reserved store stock will be restored. This action cannot be undone."
    );

    if (!confirmed) return;

    const reason = window.prompt(
      "Cancellation reason (optional):",
      "Cancelled by admin."
    );

    if (reason === null) return;

    setBusy(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/admin/orders/${orderId}/cancel`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            note: reason.trim() || "Cancelled by admin.",
          }),
        }
      );

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to cancel order."
      );
    } finally {
      setBusy(false);
    }
  }

  async function checkDelhiveryTracking() {
    if (trackingBusy || busy) return;

    const waybill = trackingNumber.trim();

    setTrackingError(null);
    setTrackingResult(null);

    if (!/^[A-Za-z0-9-]{5,50}$/.test(waybill)) {
      setTrackingError(
        "Enter a valid Delhivery tracking number (5-50 letters, numbers or hyphens)."
      );
      return;
    }

    setTrackingBusy(true);

    try {
      const response = await fetch(
        `/api/admin/delhivery/track/${encodeURIComponent(waybill)}`,
        {
          method: "GET",
          cache: "no-store",
        }
      );

      if (!response.ok) {
        if (response.status === 503) {
          throw new Error(
            "Delhivery tracking is currently unavailable or disabled."
          );
        }

        if (response.status === 401) {
          throw new Error(
            "Admin session expired. Please log in again."
          );
        }

        throw new Error(await readError(response));
      }

      const result: unknown = await response.json();
      setTrackingResult(result);
    } catch (err) {
      setTrackingError(
        err instanceof Error
          ? err.message
          : "Unable to retrieve tracking information."
      );
    } finally {
      setTrackingBusy(false);
    }
  }

  async function saveShipment() {
    if (busy || fulfillmentType !== "DELIVERY") return;

    const carrier = carrierName.trim();
    const tracking = trackingNumber.trim();
    const url = trackingUrl.trim();
    const note = shippingNote.trim();

    if (!carrier) {
      setError("Courier / carrier name is required.");
      return;
    }

    if (!tracking) {
      setError("Tracking number is required.");
      return;
    }

    setBusy(true);
    setError(null);

    try {
      const response = await fetch(
        `/api/admin/orders/${orderId}/shipment`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            carrierName: carrier,
            trackingNumber: tracking,
            trackingUrl: url || null,
            adminNote: note || null,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to save shipping details."
      );
    } finally {
      setBusy(false);
    }
  }

  async function uploadInvoice() {
    if (busy || !invoiceFile) return;

    if (invoiceFile.type !== "application/pdf") {
      setError("Invoice must be a PDF file.");
      return;
    }

    if (invoiceFile.size > 10 * 1024 * 1024) {
      setError("Invoice PDF must not exceed 10 MB.");
      return;
    }

    setBusy(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append("file", invoiceFile);

      const response = await fetch(
        `/api/admin/orders/${orderId}/invoice`,
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      setInvoiceFile(null);
      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to upload invoice."
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
          Admin Controls
        </p>

        <h2 className="mt-1 text-lg font-bold text-slate-950">
          Next Order Action
        </h2>

        <p className="mt-2 text-sm leading-6 text-slate-600">
          Only actions allowed by the KCMART order
          lifecycle are shown here.
        </p>
      </div>

      {error && (
        <div className="mt-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="mt-5 space-y-3">
        {fulfillmentType === "DELIVERY" &&
          orderStatus !== "COMPLETED" && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <div>
                <p className="text-sm font-semibold text-slate-900">
                  Shipping Management
                </p>

                <p className="mt-1 text-xs leading-5 text-slate-500">
                  Add or update courier and tracking details.
                  Shipment status and timestamps are controlled
                  automatically by the order lifecycle.
                </p>
              </div>

              <div className="mt-4 space-y-3">
                <label className="block">
                  <span className="text-xs font-medium text-slate-600">
                    Courier / Carrier
                  </span>

                  <input
                    type="text"
                    value={carrierName}
                    maxLength={100}
                    disabled={busy}
                    onChange={(event) =>
                      setCarrierName(event.target.value)
                    }
                    placeholder="e.g. Delhivery"
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-500"
                  />
                </label>

                <label className="block">
                  <span className="text-xs font-medium text-slate-600">
                    Tracking Number
                  </span>

                  <input
                    type="text"
                    value={trackingNumber}
                    maxLength={150}
                    disabled={busy}
                    onChange={(event) =>
                      setTrackingNumber(event.target.value)
                    }
                    placeholder="Enter courier tracking number"
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-500"
                  />
                </label>

                {isDelhivery && (
                  <div className="rounded-xl border border-slate-200 bg-white p-3">
                    <button
                      type="button"
                      onClick={checkDelhiveryTracking}
                      disabled={busy || trackingBusy || !trackingNumber.trim()}
                      className="w-full rounded-lg border border-slate-300 bg-slate-100 px-4 py-2.5 text-sm font-semibold text-slate-900 transition hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {trackingBusy
                        ? "Checking Delhivery Tracking..."
                        : "Check Delhivery Tracking"}
                    </button>

                    <p className="mt-2 text-xs text-slate-500">
                      Read-only lookup. This does not update the order or shipment.
                    </p>

                    {trackingError && (
                      <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">
                        {trackingError}
                      </p>
                    )}

                    {trackingResult !== null && (
                      <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3">
                        <p className="mb-2 text-sm font-semibold text-slate-800">
                          Delhivery Tracking Response
                        </p>
                        <pre className="max-h-64 overflow-auto whitespace-pre-wrap break-words text-xs text-slate-700">
                          {JSON.stringify(trackingResult, null, 2)}
                        </pre>
                      </div>
                    )}
                  </div>
                )}

                <label className="block">
                  <span className="text-xs font-medium text-slate-600">
                    Tracking URL
                  </span>

                  <input
                    type="url"
                    value={trackingUrl}
                    maxLength={1000}
                    disabled={busy}
                    onChange={(event) =>
                      setTrackingUrl(event.target.value)
                    }
                    placeholder="https://..."
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-500"
                  />
                </label>

                <label className="block">
                  <span className="text-xs font-medium text-slate-600">
                    Admin Note
                  </span>

                  <textarea
                    value={shippingNote}
                    maxLength={1000}
                    rows={3}
                    disabled={busy}
                    onChange={(event) =>
                      setShippingNote(event.target.value)
                    }
                    placeholder="Optional internal shipping note"
                    className="mt-1 w-full resize-none rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none focus:border-slate-500"
                  />
                </label>

                <button
                  type="button"
                  disabled={
                    busy ||
                    !carrierName.trim() ||
                    !trackingNumber.trim()
                  }
                  onClick={saveShipment}
                  className="w-full rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {busy
                    ? "Saving..."
                    : shipment
                      ? "Update Shipping Details"
                      : "Save Shipping Details"}
                </button>
              </div>
            </div>
          )}


        {fulfillmentType === "DELIVERY" &&
          orderStatus === "COMPLETED" && (
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
              <p className="text-sm font-semibold text-slate-800">
                Shipping details locked
              </p>

              <p className="mt-1 text-xs leading-5 text-slate-500">
                This delivery order is completed. Shipping
                information can no longer be edited here.
              </p>
            </div>
          )}
        {canMarkPayment && (
          <button
            type="button"
            disabled={busy}
            onClick={markPaymentReceived}
            className="w-full rounded-xl bg-emerald-700 px-4 py-3 text-sm font-semibold text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy
              ? "Updating..."
              : "Mark Payment Received"}
          </button>
        )}

        {canCancelOrder && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-3">
            <p className="text-sm font-semibold text-red-900">
              Cancel Order
            </p>

            <p className="mt-1 text-xs leading-5 text-red-700">
              Cancelling this unpaid pickup order will restore its reserved store stock.
            </p>

            <button
              type="button"
              disabled={busy}
              onClick={cancelOrder}
              className="mt-3 w-full rounded-xl border border-red-300 bg-white px-4 py-3 text-sm font-semibold text-red-700 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {busy ? "Processing..." : "Cancel Order"}
            </button>
          </div>
        )}

        {canCompletePickup && (
          <button
            type="button"
            disabled={busy}
            onClick={() =>
              updateStatus(
                "COMPLETED",
                "Customer collected the order. Payment received and invoice issued."
              )
            }
            className="w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy
              ? "Updating..."
              : "Complete Pickup Order"}
          </button>
        )}

        {!canMarkPayment &&
          !canCompletePickup &&
          !deliveryAwaitingPayment &&
          !blockGenericDeliveryCompletion &&
          nextAction && (
            <button
              type="button"
              disabled={busy}
              onClick={() =>
                updateStatus(
                  nextAction.status,
                  nextAction.note
                )
              }
              className="w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {busy
                ? "Updating..."
                : nextAction.label}
            </button>
          )}

        {fulfillmentType === "STORE_PICKUP" &&
          orderStatus === "READY_FOR_PICKUP" &&
          paymentStatus === "PAID" &&
          !hasInvoice && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
              <p className="text-sm font-semibold text-amber-900">
                Payment received
              </p>

              <p className="mt-1 text-xs leading-5 text-amber-800">
                Upload the PDF invoice before completing
                this order.
              </p>

              <label className="mt-3 block">
                <span className="sr-only">
                  Select invoice PDF
                </span>

                <input
                  type="file"
                  accept="application/pdf,.pdf"
                  disabled={busy}
                  onChange={(event) => {
                    setError(null);
                    setInvoiceFile(
                      event.target.files?.[0] ?? null
                    );
                  }}
                  className="block w-full text-xs text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-white file:px-3 file:py-2 file:text-xs file:font-semibold file:text-slate-800"
                />
              </label>

              {invoiceFile && (
                <p className="mt-2 break-all text-xs text-amber-900">
                  Selected: {invoiceFile.name}
                </p>
              )}

              <button
                type="button"
                disabled={busy || !invoiceFile}
                onClick={uploadInvoice}
                className="mt-3 w-full rounded-lg bg-slate-950 px-3 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {busy
                  ? "Uploading..."
                  : "Upload Invoice PDF"}
              </button>
            </div>
          )}

        {canUploadDeliveryInvoice && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
            <p className="text-sm font-semibold text-amber-900">
              Delivery invoice required
            </p>

            <p className="mt-1 text-xs leading-5 text-amber-800">
              Upload the PDF invoice before this delivery order can be completed.
            </p>

            <input
              type="file"
              accept="application/pdf,.pdf"
              disabled={busy}
              onChange={(event) =>
                setInvoiceFile(event.target.files?.[0] ?? null)
              }
              className="mt-3 block w-full text-sm text-slate-700 file:mr-3 file:rounded-lg file:border-0 file:bg-white file:px-3 file:py-2 file:text-sm file:font-semibold file:text-slate-800"
            />

            {invoiceFile && (
              <p className="mt-2 break-all text-xs text-slate-600">
                Selected: {invoiceFile.name}
              </p>
            )}

            <button
              type="button"
              disabled={busy || !invoiceFile}
              onClick={uploadInvoice}
              className="mt-3 w-full rounded-lg bg-slate-950 px-3 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {busy ? "Uploading..." : "Upload Delivery Invoice PDF"}
            </button>
          </div>
        )}

        {canCompleteDelivery && (
          <button
            type="button"
            disabled={busy}
            onClick={() =>
              updateStatus(
                "COMPLETED",
                "Delivery completed. Payment confirmed, shipment delivered and invoice issued."
              )
            }
            className="w-full rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {busy ? "Updating..." : "Complete Delivery Order"}
          </button>
        )}

        {fulfillmentType === "DELIVERY" &&
          orderStatus === "DELIVERED" &&
          !canCompleteDelivery &&
          !canUploadDeliveryInvoice && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
              <p className="text-sm font-semibold text-amber-900">
                Delivery completion requirements not met
              </p>

              <p className="mt-1 text-xs leading-5 text-amber-800">
                Completion requires confirmed online payment, a delivered shipment,
                and an uploaded invoice.
              </p>
            </div>
          )}

        {deliveryAwaitingPayment && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-3">
            <p className="text-sm font-semibold text-amber-900">
              Waiting for online payment
            </p>

            <p className="mt-1 text-xs leading-5 text-amber-800">
              This delivery order cannot be confirmed until its secure
              online payment has been recorded as PAID.
            </p>
          </div>
        )}

        {orderStatus === "COMPLETED" && (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm font-medium text-emerald-800">
            This order is completed. No lifecycle action
            is required.
          </div>
        )}

        {!nextAction &&
          !canMarkPayment &&
          !canCompletePickup &&
          !canUploadDeliveryInvoice &&
          !canCompleteDelivery &&
          orderStatus !== "COMPLETED" &&
          !(
            fulfillmentType === "STORE_PICKUP" &&
            orderStatus === "READY_FOR_PICKUP" &&
            paymentStatus === "PAID" &&
            !hasInvoice
          ) && (
            <div className="rounded-xl bg-slate-50 p-3 text-sm text-slate-600">
              No admin lifecycle action is currently
              available.
            </div>
          )}
      </div>
    </section>
  );
}