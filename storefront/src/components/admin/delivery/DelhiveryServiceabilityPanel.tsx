"use client";

import { useEffect, useRef, useState } from "react";

type Result = {
  pincode: string;
  serviceable: boolean;
  prepaid: boolean;
};

export default function DelhiveryServiceabilityPanel({
  initialPincode = "",
}: {
  initialPincode?: string;
}) {
  const pincode = initialPincode;
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState<string | null>(null);
  const latestPincode = useRef(pincode);
  latestPincode.current = pincode;

  useEffect(() => {
    setResult(null);
    setError(null);
  }, [pincode]);

  async function checkServiceability() {
    if (loading) return;

    const pin = pincode.trim();

    setResult(null);
    setError(null);

    if (!/^[1-9][0-9]{5}$/.test(pin)) {
      setError("Enter a valid six-digit Indian pincode.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
        `/api/admin/delhivery/serviceability/${encodeURIComponent(pin)}`,
        { method: "GET", cache: "no-store" }
      );

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Admin session expired. Please log in again.");
        }

        if (response.status === 503) {
          throw new Error(
            "Delhivery serviceability is unavailable or could not be confirmed. Do not book a shipment."
          );
        }

        throw new Error("Unable to verify serviceability.");
      }

      const data: unknown = await response.json();

      if (
        typeof data !== "object" ||
        data === null ||
        !("pincode" in data) ||
        data.pincode !== pin ||
        !("serviceable" in data) ||
        data.serviceable !== true ||
        !("prepaid" in data) ||
        data.prepaid !== true
      ) {
        throw new Error("Unexpected serviceability response. Verification failed.");
      }

      if (latestPincode.current !== pin) return;

      setResult({
        pincode: pin,
        serviceable: true,
        prepaid: true,
      });
    } catch (err) {
      if (latestPincode.current !== pin) return;
      setError(
        err instanceof Error
          ? err.message
          : "Serviceability verification failed."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="rounded-xl border border-sky-200 bg-sky-50/50 p-4">
      <h3 className="text-sm font-bold text-slate-950">
        Delhivery Pincode Serviceability
      </h3>

      <p className="mt-1 text-xs leading-5 text-slate-600">
        Verify the destination PIN saved on this order before booking.
        This is a read-only verification and does not create a shipment.
      </p>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <label className="flex-1 text-xs font-semibold text-slate-700">
          Destination Pincode
          <input
            type="text"
            inputMode="numeric"
            maxLength={6}
            value={pincode}
            readOnly
            aria-readonly="true"
            placeholder="No valid order pincode"
            className="mt-1 w-full rounded-lg border border-slate-300 bg-slate-100 px-3 py-2.5 text-sm text-slate-950"
          />
        </label>

        <button
          type="button"
          onClick={checkServiceability}
          disabled={loading || !/^[1-9][0-9]{5}$/.test(pincode.trim())}
          className="self-end rounded-lg bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {loading ? "Checking..." : "Check Serviceability"}
        </button>
      </div>

      {result && result.pincode === pincode && (
        <div
          role="status"
          className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm"
        >
          <p className="font-bold text-emerald-800">
            Prepaid delivery verified for {result.pincode}
          </p>
          <p className="mt-2 text-xs text-slate-600">
            Serviceability is not a shipment booking confirmation.
            Additional booking checks are still required.
          </p>
        </div>
      )}

      {error && !loading && (
        <p
          role="alert"
          className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {error}
        </p>
      )}
    </section>
  );
}