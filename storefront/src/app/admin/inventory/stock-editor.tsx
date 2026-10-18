"use client";

import { useState } from "react";
import { Loader2, X } from "lucide-react";

export type StockProduct = {
  id: number;
  name: string;
  sku: string | null;
  onlineStock: number;
  storeStock: number;
};

type Props = {
  product: StockProduct;
  onClose: () => void;
  onSaved: () => Promise<void>;
};

export default function StockEditor({
  product,
  onClose,
  onSaved,
}: Props) {
  const [online, setOnline] = useState(String(product.onlineStock));
  const [store, setStore] = useState(String(product.storeStock));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const validQuantity = (value: string) =>
    /^\d+$/.test(value) &&
    Number.isSafeInteger(Number(value)) &&
    Number(value) <= 2147483647;

  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (saving) return;

    if (!validQuantity(online) || !validQuantity(store)) {
      setError("Enter valid nonnegative whole-number quantities.");
      return;
    }

    const onlineStock = Number(online);
    const storeStock = Number(store);

    if (
      onlineStock === product.onlineStock &&
      storeStock === product.storeStock
    ) {
      onClose();
      return;
    }

    const confirmed = window.confirm(
      `Update stock for "${product.name}"?\n\n` +
        `Online: ${product.onlineStock} → ${onlineStock}\n` +
        `Store: ${product.storeStock} → ${storeStock}`
    );

    if (!confirmed) return;

    setSaving(true);
    setError("");

    try {
      const response = await fetch(
        `/api/admin/products/${product.id}/inventory`,
        {
          method: "PUT",
          credentials: "same-origin",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            onlineStock,
            storeStock,
            expectedOnlineStock: product.onlineStock,
            expectedStoreStock: product.storeStock,
          }),
        }
      );

      if (!response.ok) {
        if (response.status === 409) {
          throw new Error(
            "Stock changed since you opened this editor. Close it, refresh inventory, and try again."
          );
        }

        if (response.status === 401) {
          throw new Error("Admin session expired. Please log in again.");
        }

        throw new Error(
          `Unable to update stock (HTTP ${response.status}).`
        );
      }

      await onSaved();
      onClose();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to update stock."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4"
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="stock-editor-title"
        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="stock-editor-title"
              className="text-xl font-extrabold text-[#111827]"
            >
              Edit Inventory
            </h2>
            <p className="mt-1 text-sm text-slate-500">
              {product.name}
            </p>
            <p className="mt-1 text-xs text-slate-400">
              SKU: {product.sku || "N/A"}
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            aria-label="Close stock editor"
            className="rounded-lg p-2 hover:bg-slate-100 disabled:opacity-50"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={save} className="mt-6 space-y-5">
          <div>
            <label
              htmlFor="online-stock"
              className="mb-2 block text-sm font-semibold"
            >
              Online Stock
            </label>
            <input
              id="online-stock"
              type="number"
              min="0"
              max="2147483647"
              step="1"
              required
              value={online}
              onChange={(event) => setOnline(event.target.value)}
              disabled={saving}
              className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-[#075E54]"
            />
            <p className="mt-1 text-xs text-slate-500">
              Units available for online orders.
            </p>
          </div>

          <div>
            <label
              htmlFor="store-stock"
              className="mb-2 block text-sm font-semibold"
            >
              Store Stock
            </label>
            <input
              id="store-stock"
              type="number"
              min="0"
              max="2147483647"
              step="1"
              required
              value={store}
              onChange={(event) => setStore(event.target.value)}
              disabled={saving}
              className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:border-[#075E54]"
            />
            <p className="mt-1 text-xs text-slate-500">
              Units available at the physical shop.
            </p>
          </div>

          {error && (
            <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">
              {error}
            </p>
          )}

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-semibold"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-2 rounded-xl bg-[#075E54] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#064E46] disabled:opacity-50"
            >
              {saving && <Loader2 size={16} className="animate-spin" />}
              {saving ? "Saving..." : "Save Stock"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}