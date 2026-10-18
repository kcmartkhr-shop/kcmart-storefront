"use client";

import { useState } from "react";

async function readError(response: Response) {
  try {
    const data = await response.json();
    return data?.message || data?.error || `Request failed (${response.status}).`;
  } catch {
    return `Request failed (${response.status}).`;
  }
}
type DelhiveryPackageData = {
  id: number;
  orderId: number;
  actualWeightGrams: number;
  lengthCm: number;
  widthCm: number;
  heightCm: number;
  volumetricWeightGrams: number;
  chargeableWeightGrams: number;
};

export default function DelhiveryPackagePanel({ orderId }: { orderId: number }) {
  const [actualWeightGrams, setActualWeightGrams] = useState("");
  const [lengthCm, setLengthCm] = useState("");
  const [widthCm, setWidthCm] = useState("");
  const [heightCm, setHeightCm] = useState("");
  const [saved, setSaved] = useState<DelhiveryPackageData | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [checked, setChecked] = useState(false);

  const endpoint = `/api/admin/orders/${orderId}/delhivery-package`;

  async function loadPackage() {
    if (loading || busy) return;

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(endpoint, { cache: "no-store" });

      if (response.status === 404) {
        setSaved(null);
        setChecked(true);
        return;
      }

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = (await response.json()) as DelhiveryPackageData;
      setSaved(data);
      setChecked(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load package.");
    } finally {
      setLoading(false);
    }
  }

  async function savePackage() {
    if (busy || loading || saved || !checked) return;

    const weight = Number(actualWeightGrams);
    const dimensions = [lengthCm, widthCm, heightCm].map(Number);

    if (
      !Number.isSafeInteger(weight) ||
      weight <= 0 ||
      dimensions.some(
        (value) => !Number.isFinite(value) || value <= 0
      ) ||
      [lengthCm, widthCm, heightCm].some(
        (value) => !/^\d+(\.\d{1,2})?$/.test(value.trim())
      )
    ) {
      setError("Enter positive whole grams and dimensions with up to 2 decimals.");
      return;
    }

    setBusy(true);
    setError(null);

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          actualWeightGrams: weight,
          lengthCm: dimensions[0],
          widthCm: dimensions[1],
          heightCm: dimensions[2],
        }),
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = (await response.json()) as DelhiveryPackageData;
      setSaved(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save package.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="rounded-xl border border-indigo-200 bg-indigo-50/50 p-4">
      <h3 className="text-sm font-bold text-slate-950">
        Delhivery Package Details
      </h3>
      <p className="mt-1 text-xs leading-5 text-slate-600">
        Prepare package measurements before shipment booking.
        Saving does not create a shipment or generate an AWB.
      </p>

      {!checked && (
        <button
          type="button"
          onClick={loadPackage}
          disabled={loading}
          className="mt-4 rounded-lg bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
        >
          {loading ? "Checking..." : "Check Existing Package"}
        </button>
      )}

      {checked && saved && (
        <div className="mt-4 space-y-2 text-sm text-slate-800">
          <p className="font-semibold text-emerald-700">
            Package saved — read only
          </p>
          <p>Actual weight: {saved.actualWeightGrams} g</p>
          <p>
            Dimensions: {saved.lengthCm} × {saved.widthCm} × {saved.heightCm} cm
          </p>
          <p>Volumetric weight: {saved.volumetricWeightGrams} g</p>
          <p className="font-bold">
            Chargeable weight: {saved.chargeableWeightGrams} g
          </p>
          <p className="text-xs text-slate-500">
            Weight calculation uses the backend configuration.
            Confirm the volumetric divisor with Delhivery before booking.
          </p>
        </div>
      )}

      {checked && !saved && (
        <div className="mt-4 space-y-3">
          <label className="block text-xs font-semibold text-slate-700">
            Actual Weight (grams)
            <input
              type="number"
              min="1"
              step="1"
              value={actualWeightGrams}
              onChange={(event) => setActualWeightGrams(event.target.value)}
              className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
              placeholder="Example: 1200"
            />
          </label>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {[
              ["Length (cm)", lengthCm, setLengthCm],
              ["Width (cm)", widthCm, setWidthCm],
              ["Height (cm)", heightCm, setHeightCm],
            ].map(([label, value, setter]) => (
              <label
                key={label as string}
                className="block text-xs font-semibold text-slate-700"
              >
                {label as string}
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={value as string}
                  onChange={(event) =>
                    (setter as (value: string) => void)(event.target.value)
                  }
                  className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
                  placeholder="cm"
                />
              </label>
            ))}
          </div>

          <button
            type="button"
            onClick={savePackage}
            disabled={busy || loading}
            className="w-full rounded-lg bg-indigo-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-800 disabled:opacity-50"
          >
            {busy ? "Saving Package..." : "Save Package Details"}
          </button>

          <p className="text-xs text-amber-800">
            Package details cannot be edited after saving.
            Verify all measurements before submitting.
          </p>
        </div>
      )}

      {error && (
        <p role="alert" className="mt-3 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
