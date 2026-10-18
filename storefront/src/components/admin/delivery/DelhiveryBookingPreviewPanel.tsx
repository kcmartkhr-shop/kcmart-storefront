"use client";

import { useState } from "react";
import { AlertTriangle, CheckCircle2, ClipboardCheck, RefreshCw } from "lucide-react";

type Preview = {
  orderId: number;
  locallyReady: boolean;
  prepaidOnly: boolean;
  serviceabilityVerified: boolean;
  shipmentBooked: boolean;
  blockers: string[];
};

function isPreview(value: unknown): value is Preview {
  if (!value || typeof value !== "object") return false;

  const data = value as Record<string, unknown>;

  return (
    typeof data.orderId === "number" &&
    Number.isSafeInteger(data.orderId) &&
    typeof data.locallyReady === "boolean" &&
    data.prepaidOnly === true &&
    typeof data.serviceabilityVerified === "boolean" &&
    typeof data.shipmentBooked === "boolean" &&
    Array.isArray(data.blockers) &&
    data.blockers.every((item) => typeof item === "string")
  );
}

export default function DelhiveryBookingPreviewPanel({
  orderId,
}: {
  orderId: number;
}) {
  const [preview, setPreview] = useState<Preview | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function checkPreview() {
    if (loading) return;

    setLoading(true);
    setPreview(null);
    setError(null);

    try {
      const response = await fetch(
        `/api/admin/orders/${orderId}/delhivery-booking-preview`,
        { method: "GET", cache: "no-store" }
      );

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Admin session expired. Please log in again.");
        }

        throw new Error(
          `Booking preview unavailable (HTTP ${response.status}).`
        );
      }

      const data: unknown = await response.json();

      if (!isPreview(data) || data.orderId !== orderId) {
        throw new Error("Invalid booking preview response.");
      }

      setPreview(data);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to check booking readiness."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mt-5 rounded-xl border border-teal-200 bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <ClipboardCheck className="mt-0.5 h-5 w-5 text-teal-700" />

        <div>
          <h3 className="font-bold text-slate-900">
            Delhivery Booking Preview
          </h3>
          <p className="mt-1 text-xs leading-5 text-slate-600">
            Read-only eligibility check for prepaid shipments.
            This does not create a shipment, AWB, or pickup request.
          </p>
        </div>
      </div>

      <button
        type="button"
        onClick={checkPreview}
        disabled={loading}
        className="mt-4 inline-flex items-center gap-2 rounded-lg bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-50"
      >
        <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
        {loading ? "Checking..." : preview ? "Refresh Preview" : "Check Booking Readiness"}
      </button>

      {error && (
        <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">
          {error}
        </p>
      )}

      {preview && (
        <div className="mt-4 space-y-3">
          <div
            role="status"
            className={`rounded-lg border p-3 text-sm ${
              preview.locallyReady
                ? "border-emerald-200 bg-emerald-50 text-emerald-900"
                : "border-amber-200 bg-amber-50 text-amber-900"
            }`}
          >
            <div className="flex items-center gap-2 font-bold">
              {preview.locallyReady ? (
                <CheckCircle2 className="h-4 w-4" />
              ) : (
                <AlertTriangle className="h-4 w-4" />
              )}
              {preview.locallyReady
                ? "Local checks passed"
                : "Booking blocked by local checks"}
            </div>
          </div>

          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-slate-500">Payment capability</dt>
              <dd className="mt-1 font-semibold text-slate-900">
                Prepaid only
              </dd>
            </div>

            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-slate-500">Serviceability</dt>
              <dd className="mt-1 font-semibold text-amber-800">
                {preview.serviceabilityVerified
                  ? "Verified by preview"
                  : "Not verified by preview"}
              </dd>
            </div>

            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-slate-500">Existing shipment</dt>
              <dd className="mt-1 font-semibold text-slate-900">
                {preview.shipmentBooked ? "Recorded" : "Not recorded"}
              </dd>
            </div>

            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-slate-500">Live booking</dt>
              <dd className="mt-1 font-semibold text-amber-800">
                Disabled
              </dd>
            </div>
          </dl>

          {preview.blockers.length > 0 && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3">
              <h4 className="text-sm font-bold text-amber-900">
                Booking blockers
              </h4>
              <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-amber-900">
                {preview.blockers.map((blocker, index) => (
                  <li key={`${index}-${blocker}`}>{blocker}</li>
                ))}
              </ul>
            </div>
          )}

          <p className="text-xs leading-5 text-slate-600">
            Local readiness is not authorization to book.
            Delhivery serviceability, operational activation, and final
            safety checks are separate requirements.
          </p>
        </div>
      )}
    </section>
  );
}