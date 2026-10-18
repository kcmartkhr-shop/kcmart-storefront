"use client";

import { useEffect, useState } from "react";
import { Loader2, X } from "lucide-react";

export type Category = {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  active: boolean;
  productCount: number;
  createdAt: string | null;
};

type Props = {
  categories: Category[];
  editing?: Category | null;
  onClose: () => void;
  onSaved: () => void | Promise<void>;
};

function makeSlug(value: string) {
  return value
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export default function CategoryForm({
  categories,
  editing,
  onClose,
  onSaved,
}: Props) {
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [parentId, setParentId] = useState("");
  const [active, setActive] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [slugEdited, setSlugEdited] = useState(false);

  useEffect(() => {
    setName(editing?.name ?? "");
    setSlug(editing?.slug ?? "");
    setParentId(editing?.parentId?.toString() ?? "");
    setActive(editing?.active ?? true);
    setSlugEdited(Boolean(editing));
    setError("");
  }, [editing]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (saving) return;

    setSaving(true);
    setError("");

    try {
      const response = await fetch(
        "/api/admin/category-management" +
          (editing ? `/${editing.id}` : ""),
        {
          method: editing ? "PUT" : "POST",
          credentials: "same-origin",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            name: name.trim(),
            slug: makeSlug(slug),
            parentId: parentId ? Number(parentId) : null,
            active,
          }),
        }
      );

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("Session expired. Please log in again.");
        }

        if (response.status === 409) {
          throw new Error(
            "Category name or slug already exists."
          );
        }

        throw new Error(
          `Unable to save category (HTTP ${response.status}).`
        );
      }

      await onSaved();
      onClose();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to save category."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="rounded-2xl border border-[#DDEBE7] bg-white p-5 shadow-sm sm:p-6">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="text-xl font-black text-[#111827]">
          {editing ? "Edit Category" : "Add Category"}
        </h2>

        <button
          type="button"
          onClick={onClose}
          disabled={saving}
          aria-label="Close form"
          className="rounded-lg p-2 text-[#6B7280] hover:bg-[#F4F9F7]"
        >
          <X size={19} />
        </button>
      </div>

      <form onSubmit={submit} className="space-y-4">
        <div>
          <label htmlFor="category-name" className="mb-1 block text-sm font-bold">
            Category Name
          </label>
          <input
            id="category-name"
            required
            maxLength={120}
            value={name}
            onChange={(event) => {
              const value = event.target.value;
              setName(value);
              if (!slugEdited) setSlug(makeSlug(value));
            }}
            placeholder="Example: Gaming Accessories"
            className="w-full rounded-xl border border-[#DDEBE7] px-4 py-3 outline-none focus:border-[#075E54]"
          />
        </div>

        <div>
          <label htmlFor="category-slug" className="mb-1 block text-sm font-bold">
            URL Slug
          </label>
          <input
            id="category-slug"
            required
            maxLength={150}
            value={slug}
            onChange={(event) => {
              setSlug(event.target.value);
              setSlugEdited(true);
            }}
            placeholder="gaming-accessories"
            className="w-full rounded-xl border border-[#DDEBE7] px-4 py-3 outline-none focus:border-[#075E54]"
          />
        </div>

        <div>
          <label htmlFor="category-parent" className="mb-1 block text-sm font-bold">
            Parent Category
          </label>
          <select
            id="category-parent"
            value={parentId}
            onChange={(event) => setParentId(event.target.value)}
            className="w-full rounded-xl border border-[#DDEBE7] bg-white px-4 py-3 outline-none focus:border-[#075E54]"
          >
            <option value="">None (Top-level category)</option>
            {categories
              .filter((category) => category.id !== editing?.id)
              .map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
          </select>
        </div>

        <label className="flex items-center gap-3 text-sm font-semibold">
          <input
            type="checkbox"
            checked={active}
            onChange={(event) => setActive(event.target.checked)}
            className="h-4 w-4 accent-[#075E54]"
          />
          Active category
        </label>

        {error && (
          <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm text-red-700">
            {error}
          </p>
        )}

        <div className="flex flex-wrap gap-3 pt-2">
          <button
            type="submit"
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-xl bg-[#075E54] px-5 py-3 text-sm font-bold text-white hover:bg-[#064E46] disabled:opacity-50"
          >
            {saving && <Loader2 size={16} className="animate-spin" />}
            {saving ? "Saving..." : editing ? "Save Changes" : "Create Category"}
          </button>

          <button
            type="button"
            disabled={saving}
            onClick={onClose}
            className="rounded-xl border border-[#DDEBE7] px-5 py-3 text-sm font-bold"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}