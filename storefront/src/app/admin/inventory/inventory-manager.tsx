"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import StockEditor from "./stock-editor";
import {
  AlertTriangle,
  Boxes,
  Loader2,
  Package,
  RefreshCw,
  Search,
  Store,
} from "lucide-react";

type InventoryProduct = {
  id: number;
  name: string;
  sku: string | null;
  onlineStock: number;
  storeStock: number;
  active: boolean;
};

export default function InventoryManager() {
  const [products, setProducts] = useState<InventoryProduct[]>([]);
  const [editing, setEditing] = useState<InventoryProduct | null>(null);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await fetch("/api/admin/products", {
        credentials: "same-origin",
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(
          response.status === 401
            ? "Admin session expired. Please log in again."
            : `Unable to load inventory (HTTP ${response.status}).`
        );
      }

      const data: unknown = await response.json();

      if (!Array.isArray(data)) {
        throw new Error("Unexpected inventory response.");
      }

      setProducts(data as InventoryProduct[]);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to load inventory."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProducts();
  }, [loadProducts]);

  const stats = useMemo(() => {
    return {
      products: products.length,
      online: products.reduce((sum, p) => sum + p.onlineStock, 0),
      store: products.reduce((sum, p) => sum + p.storeStock, 0),
      low: products.filter(
        (p) =>
          (p.onlineStock > 0 && p.onlineStock <= 5) ||
          (p.storeStock > 0 && p.storeStock <= 5)
      ).length,
      out: products.filter(
        (p) => p.onlineStock === 0 || p.storeStock === 0
      ).length,
    };
  }, [products]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();

    return products.filter((p) => {
      const matchesSearch =
        p.name.toLowerCase().includes(query) ||
        (p.sku ?? "").toLowerCase().includes(query);

      if (!matchesSearch) return false;

      if (filter === "low") {
        return (
          (p.onlineStock > 0 && p.onlineStock <= 5) ||
          (p.storeStock > 0 && p.storeStock <= 5)
        );
      }

      if (filter === "out") {
        return p.onlineStock === 0 || p.storeStock === 0;
      }

      return true;
    });
  }, [products, search, filter]);

  const cards = [
    { label: "Total Products", value: stats.products, icon: Boxes },
    { label: "Online Units", value: stats.online, icon: Package },
    { label: "Store Units", value: stats.store, icon: Store },
    { label: "Low Stock Products", value: stats.low, icon: AlertTriangle },
    { label: "Out of Stock Products", value: stats.out, icon: AlertTriangle },
  ];

  return (
    <div className="mx-auto w-full max-w-[1600px] min-w-0 space-y-6 px-4 py-5 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-widest text-[#0B7A69]">
            KCMART / ADMIN
          </p>
          <h1 className="mt-1 text-2xl font-extrabold text-[#111827]">
            Inventory Management
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Manage online and physical store stock separately.
          </p>
        </div>

        <button
          type="button"
          onClick={() => void loadProducts()}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-xl border border-[#075E54] px-4 py-2 text-sm font-semibold text-[#075E54] hover:bg-[#EAF5F2] disabled:opacity-50"
        >
          <RefreshCw size={16} />
          Refresh
        </button>
      </div>

      {error && (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5">
        {cards.map((card) => {
          const Icon = card.icon;

          return (
            <div
              key={card.label}
              className="rounded-2xl border border-[#DCEBE6] bg-white p-4 shadow-sm"
            >
              <Icon size={20} className="text-[#075E54]" />
              <p className="mt-3 text-xs font-medium text-slate-500">
                {card.label}
              </p>
              <p className="mt-1 text-2xl font-extrabold text-[#111827]">
                {loading ? "—" : card.value}
              </p>
            </div>
          );
        })}
      </div>

      <div className="min-w-0 overflow-hidden rounded-2xl border border-[#DCEBE6] bg-white shadow-sm">
        <div className="flex flex-wrap gap-3 border-b border-slate-100 p-4">
          <div className="flex min-w-[200px] flex-1 items-center gap-2 rounded-xl border border-slate-200 px-3 py-2">
            <Search size={17} className="text-slate-400" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by product name or SKU"
              className="w-full bg-transparent text-sm outline-none"
            />
          </div>

          <select
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
          >
            <option value="all">All Products</option>
            <option value="low">Low Stock</option>
            <option value="out">Out of Stock</option>
          </select>
        </div>

        <div className="w-full max-w-full overflow-x-auto">
          <table className="w-full min-w-[850px] text-left text-sm">
            <thead className="bg-[#F4F9F7] text-xs uppercase text-slate-600">
              <tr>
                <th className="px-5 py-4">Product</th>
                <th className="px-5 py-4">SKU</th>
                <th className="px-5 py-4">Online Stock</th>
                <th className="px-5 py-4">Store Stock</th>
                <th className="px-5 py-4">Status</th>
                <th className="px-5 py-4">Action</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center">
                    <Loader2 className="mx-auto animate-spin text-[#075E54]" />
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center text-slate-500">
                    No matching products found.
                  </td>
                </tr>
              ) : (
                filtered.map((product) => (
                  <tr key={product.id} className="hover:bg-[#F4F9F7]">
                    <td className="px-5 py-4 font-semibold text-[#111827]">
                      {product.name}
                    </td>
                    <td className="px-5 py-4 text-slate-500">
                      {product.sku || "—"}
                    </td>
                    <td className="px-5 py-4 font-bold">
                      {product.onlineStock}
                    </td>
                    <td className="px-5 py-4 font-bold">
                      {product.storeStock}
                    </td>
                    <td className="px-5 py-4">
                      <span
                        className={`rounded-full px-3 py-1 text-xs font-bold ${
                          product.onlineStock === 0 || product.storeStock === 0
                            ? "bg-red-50 text-red-700"
                            : product.onlineStock <= 5 || product.storeStock <= 5
                              ? "bg-amber-50 text-amber-700"
                              : "bg-[#EAF5F2] text-[#075E54]"
                        }`}
                      >
                        {product.onlineStock === 0 || product.storeStock === 0
                          ? "Out of Stock"
                          : product.onlineStock <= 5 || product.storeStock <= 5
                            ? "Low Stock"
                            : "In Stock"}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <button
                        type="button"
                        onClick={() => setEditing(product)}
                        className="rounded-lg border border-[#075E54] px-3 py-2 text-xs font-bold text-[#075E54] hover:bg-[#EAF5F2]"
                      >
                        Edit Stock
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
      {editing && (
        <StockEditor
          key={editing.id}
          product={editing}
          onClose={() => setEditing(null)}
          onSaved={loadProducts}
        />
      )}
    </div>
  );
}