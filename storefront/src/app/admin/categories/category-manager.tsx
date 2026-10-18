"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import CategoryForm from "./category-form";
import type { Category } from "./category-form";
import {
  FolderTree,
  Search,
  RefreshCw,
  Package,
  AlertCircle,
} from "lucide-react";

export default function CategoryManager() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadCategories = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const response = await fetch(
        "/api/admin/category-management",
        {
          cache: "no-store",
          credentials: "same-origin",
        }
      );

      if (!response.ok) {
        throw new Error(
          response.status === 401
            ? "Admin session expired. Please log in again."
            : `Unable to load categories (${response.status}).`
        );
      }

      const data: unknown = await response.json();

      if (!Array.isArray(data)) {
        throw new Error("Unexpected category response.");
      }

      setCategories(data as Category[]);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load categories."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCategories();
  }, [loadCategories]);

  const [deletingId, setDeletingId] = useState<number | null>(null);

  async function deleteCategory(category: Category) {
    if (deletingId !== null) return;

    const hasChildren = categories.some(
      (item) => item.parentId === category.id
    );

    if (category.productCount > 0 || hasChildren) {
      window.alert(
        "This category contains products or child categories. Remove those relationships before deleting it, or deactivate the category."
      );
      return;
    }

    const confirmed = window.confirm(
      `Delete "${category.name}" permanently? This cannot be undone.`
    );

    if (!confirmed) return;

    setDeletingId(category.id);
    setError("");

    try {
      const response = await fetch(
        `/api/admin/category-management/${category.id}`,
        {
          method: "DELETE",
          credentials: "same-origin",
        }
      );

      if (!response.ok) {
        if (response.status === 409) {
          throw new Error(
            "Category cannot be deleted because it contains products or child categories."
          );
        }

        if (response.status === 401) {
          throw new Error("Admin session expired. Please log in again.");
        }

        throw new Error(
          `Unable to delete category (HTTP ${response.status}).`
        );
      }

      await loadCategories();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to delete category."
      );
    } finally {
      setDeletingId(null);
    }
  }
  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();

    return categories.filter((category) =>
      `${category.name} ${category.slug} ${category.id}`
        .toLowerCase()
        .includes(query)
    );
  }, [categories, search]);

  const activeCount = categories.filter(
    (category) => category.active
  ).length;

  const productCount = categories.reduce(
    (total, category) => total + category.productCount,
    0
  );

  return (
    <div className="p-4 sm:p-6 lg:p-8">
      <div className="mx-auto max-w-[1500px]">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.18em] text-[#0B7A69]">
              Catalog Management
            </p>

            <h1 className="mt-2 text-3xl font-black text-[#111827]">
              Categories
            </h1>

            <p className="mt-2 text-sm text-[#6B7280]">
              Organize and manage your product categories.
            </p>
          </div>

          <button
            type="button"
            onClick={() => { setEditing(null); setShowForm(true); }}
            className="rounded-xl bg-[#075E54] px-5 py-3 text-sm font-bold text-white hover:bg-[#064E46]"
          >
            + Add Category
          </button>

          <button
            type="button"
            onClick={() => void loadCategories()}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-xl border border-[#DDEBE7] bg-white px-4 py-3 text-sm font-bold text-[#075E54] hover:bg-[#F4F9F7] disabled:opacity-50"
          >
            <RefreshCw size={17} />
            Refresh
          </button>
        </div>

        {showForm && (
          <div className="mt-6">
            <CategoryForm
              categories={categories}
              editing={editing}
              onClose={() => { setShowForm(false); setEditing(null); }}
              onSaved={loadCategories}
            />
          </div>
        )}

        <div className="mt-7 grid gap-4 sm:grid-cols-3">
          <Stat label="Total Categories" value={categories.length} />
          <Stat label="Active Categories" value={activeCount} />
          <Stat label="Assigned Products" value={productCount} />
        </div>

        <section className="mt-7 overflow-hidden rounded-2xl border border-[#DDEBE7] bg-white shadow-sm">
          <div className="border-b border-[#E5E7EB] p-5 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-black text-[#111827]">
                  All Categories
                </h2>

                <p className="mt-1 text-sm text-[#6B7280]">
                  {filtered.length} categories displayed
                </p>
              </div>

              <div className="relative w-full sm:w-72">
                <Search
                  size={17}
                  className="absolute left-3 top-3 text-[#6B7280]"
                />

                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search categories..."
                  className="w-full rounded-xl border border-[#DDEBE7] bg-white py-2.5 pl-10 pr-4 text-sm outline-none focus:border-[#075E54]"
                />
              </div>
            </div>
          </div>

          {error && (
            <div className="m-5 flex items-center gap-2 rounded-xl bg-red-50 p-4 text-sm text-red-700">
              <AlertCircle size={18} />
              {error}
            </div>
          )}

          {loading ? (
            <div className="p-12 text-center text-sm text-[#6B7280]">
              Loading categories...
            </div>
          ) : filtered.length === 0 ? (
            <div className="p-12 text-center text-sm text-[#6B7280]">
              No categories found.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[650px] text-left text-sm">
                <thead className="bg-[#F4F9F7] text-[#6B7280]">
                  <tr>
                    <th className="px-5 py-4">Category</th>
                    <th className="px-5 py-4">Slug</th>
                    <th className="px-5 py-4">Products</th>
                    <th className="px-5 py-4">Status</th>
                    <th className="px-5 py-4">Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {filtered.map((category) => (
                    <tr
                      key={category.id}
                      className="border-t border-[#EEF2F1]"
                    >
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-3">
                          <div className="rounded-xl bg-[#EAF5F2] p-2.5 text-[#075E54]">
                            <FolderTree size={18} />
                          </div>

                          <div>
                            <p className="font-bold text-[#111827]">
                              {category.name}
                            </p>
                            <p className="mt-1 text-xs text-[#9CA3AF]">
                              ID #{category.id}
                            </p>
                          </div>
                        </div>
                      </td>

                      <td className="px-5 py-4 text-[#6B7280]">
                        {category.slug}
                      </td>

                      <td className="px-5 py-4">
                        <span className="inline-flex items-center gap-2 font-semibold">
                          <Package size={15} />
                          {category.productCount}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <span
                          className={
                            category.active
                              ? "rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700"
                              : "rounded-full bg-gray-100 px-3 py-1 text-xs font-bold text-gray-600"
                          }
                        >
                          {category.active ? "Active" : "Inactive"}
                        </span>
                      </td>

                      <td className="px-5 py-4">
                        <button
                          type="button"
                          onClick={() => {
                            setEditing(category);
                            setShowForm(true);
                            window.scrollTo({ top: 0, behavior: "smooth" });
                          }}
                          className="rounded-lg border border-[#DDEBE7] px-3 py-2 text-xs font-bold text-[#075E54] hover:bg-[#EAF5F2]"
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => void deleteCategory(category)}
                          disabled={deletingId !== null}
                          className="ml-2 rounded-lg border border-red-200 px-3 py-2 text-xs font-bold text-red-600 hover:bg-red-50 disabled:opacity-50"
                        >
                          {deletingId === category.id ? "Deleting..." : "Delete"}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Stat({
  label,
  value,
}: {
  label: string;
  value: number;
}) {
  return (
    <div className="rounded-2xl border border-[#DDEBE7] bg-white p-5 shadow-sm">
      <p className="text-sm font-semibold text-[#6B7280]">
        {label}
      </p>
      <p className="mt-3 text-3xl font-black text-[#075E54]">
        {value}
      </p>
    </div>
  );
}