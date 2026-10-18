"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProductPhotoManager from "./product-photo-manager";

type Product = {
  id: number;
  categoryId: number;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  brand: string | null;
  sku: string;
  price: number;
  mrp: number | null;
  active: boolean;
  onlineStock: number;
  storeStock: number;
};

type Category = {
  id: number;
  name: string;
};

type ProductImage = {
  id: number;
  imageUrl: string;
  cover: boolean;
  displayOrder: number;
};

type FormData = {
  categoryId: string;
  name: string;
  slug: string;
  description: string;
  imageUrl: string;
  brand: string;
  sku: string;
  price: string;
  mrp: string;
  onlineStock: string;
  storeStock: string;
};

const blank: FormData = {
  categoryId: "",
  name: "",
  slug: "",
  description: "",
  imageUrl: "",
  brand: "",
  sku: "",
  price: "",
  mrp: "",
  onlineStock: "0",
  storeStock: "0",
};

async function apiRequest(
  url: string,
  options?: RequestInit
) {
  const response = await fetch(url, {
    ...options,
    cache: "no-store",
  });

  if (!response.ok) {
    let detail = `HTTP ${response.status}`;

    try {
      const body = await response.json();
      detail = body.message ?? body.error ?? detail;
    } catch {
      // Preserve HTTP status when the response is not JSON.
    }

    throw new Error(detail);
  }

  return response.json();
}

export default function ProductManager() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selected, setSelected] = useState<Product | null>(null);
  const [form, setForm] = useState<FormData>({ ...blank });
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<
    "ALL" | "ACTIVE" | "INACTIVE" | "LOW_STOCK"
  >("ALL");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [photos, setPhotos] = useState<File[]>([]);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const [gallery, setGallery] = useState<ProductImage[]>([]);
  const [galleryLoading, setGalleryLoading] = useState(false);

  const refresh = useCallback(async () => {
    const [productData, categoryData] = await Promise.all([
      apiRequest("/api/admin/products"),
      apiRequest("/api/admin/categories"),
    ]);

    setProducts(productData);
    setCategories(categoryData);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function initialize() {
      try {
        await refresh();
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? err.message : "Loading failed"
          );
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void initialize();

    return () => {
      cancelled = true;
    };
  }, [refresh]);

  function setField(key: keyof FormData, value: string) {
    setForm((previous) => ({
      ...previous,
      [key]: value,
    }));
  }

  async function loadGallery(productId: number) {
    setGalleryLoading(true);

    try {
      const images = await apiRequest(
        `/api/admin/product-images/${productId}`
      );

      setGallery(images);
    } catch (err) {
      setGallery([]);
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load product photos"
      );
    } finally {
      setGalleryLoading(false);
    }
  }

  function startNew() {
    setSelected(null);
    setGallery([]);
    setPhotos([]);
    setPhotoPreviews([]);
    setForm({ ...blank });
    setMessage("");
    setError("");
  }

  function startEdit(product: Product) {
    setPhotos([]);
    setPhotoPreviews([]);
    setSelected(product);
    setForm({
      categoryId: String(product.categoryId),
      name: product.name,
      slug: product.slug,
      description: product.description ?? "",
      imageUrl: product.imageUrl ?? "",
      brand: product.brand ?? "",
      sku: product.sku,
      price: String(product.price),
      mrp: product.mrp == null ? "" : String(product.mrp),
      onlineStock: String(product.onlineStock),
      storeStock: String(product.storeStock),
    });
    setMessage("");
    setError("");
    void loadGallery(product.id);
  }

  function validate() {
    if (
      !form.categoryId ||
      !form.name.trim() ||
      !form.slug.trim() ||
      !form.sku.trim() ||
      form.price.trim() === ""
    ) {
      throw new Error("Complete all required fields.");
    }

    if (
      !Number.isFinite(Number(form.price)) ||
      Number(form.price) < 0
    ) {
      throw new Error("Enter a valid selling price.");
    }

    if (
      form.mrp !== "" &&
      Number(form.mrp) < Number(form.price)
    ) {
      throw new Error("MRP cannot be below selling price.");
    }

    for (const stock of [
      form.onlineStock,
      form.storeStock,
    ]) {
      if (
        stock.trim() === "" ||
        !Number.isSafeInteger(Number(stock)) ||
        Number(stock) < 0
      ) {
        throw new Error(
          "Stock must be a non-negative whole number."
        );
      }
    }
  }

  async function uploadPhotos() {
    if (!selected || photos.length === 0) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      for (const photo of photos) {
        const data = new FormData();
        data.append("productId", String(selected.id));
        data.append("file", photo);

        const response = await fetch("/api/admin/upload-image", {
          method: "POST",
          body: data,
        });

        const result = await response.json();

        if (!response.ok) {
          throw new Error(result.error ?? "Photo upload failed");
        }
      }

      const updated = await apiRequest(
        `/api/admin/products/${selected.id}`
      );

      await refresh();
      startEdit(updated);
      setPhotos([]);
      setPhotoPreviews([]);
      setMessage(
        `${photos.length} photo${photos.length === 1 ? "" : "s"} uploaded successfully.`
      );
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Photo upload failed"
      );
    } finally {
      setBusy(false);
    }
  }
  async function saveProduct() {
    setBusy(true);
    setMessage("");
    setError("");

    try {
      validate();

      const payload = {
        categoryId: Number(form.categoryId),
        name: form.name.trim(),
        slug: form.slug.trim(),
        description: form.description.trim() || null,
        imageUrl: form.imageUrl.trim() || null,
        brand: form.brand.trim() || null,
        sku: form.sku.trim(),
        price: Number(form.price),
        mrp: form.mrp === "" ? null : Number(form.mrp),
        ...(!selected
          ? {
              onlineStock: Number(form.onlineStock),
              storeStock: Number(form.storeStock),
            }
          : {}),
      };

      const saved = await apiRequest(
        selected
          ? `/api/admin/products/${selected.id}`
          : "/api/admin/products",
        {
          method: selected ? "PUT" : "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        }
      );

      // For a new product, upload all selected photos
      // immediately after the backend assigns a product ID.
      let photoWarning = "";

      if (!selected && photos.length > 0) {
        try {
          for (const photo of photos) {
            const data = new FormData();
            data.append("productId", String(saved.id));
            data.append("file", photo);

            const imageResponse = await fetch(
              "/api/admin/upload-image",
              {
                method: "POST",
                body: data,
              }
            );

            if (!imageResponse.ok) {
              const errorBody = await imageResponse.json();
              throw new Error(
                errorBody.error ?? "Photo upload failed"
              );
            }
          }
        } catch (err) {
          photoWarning =
            err instanceof Error
              ? err.message
              : "Photo upload failed";
        }
      }
      await refresh();

      if (selected) {
        const updated = await apiRequest(
          `/api/admin/products/${selected.id}`
        );
        startEdit(updated);
        setMessage("Product updated successfully.");
      } else {
        startNew();
        setMessage(
          photoWarning
            ? `Product created (ID ${saved.id}), but ${photoWarning}. Edit the product to retry.`
            : `Product and ${photos.length} photo${photos.length === 1 ? "" : "s"} saved successfully (ID ${saved.id}).`
        );
      }
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Save failed"
      );
    } finally {
      setBusy(false);
    }
  }

  async function saveInventory() {
    if (!selected) return;

    setBusy(true);
    setMessage("");
    setError("");

    try {
      validate();

      const updated = await apiRequest(
        `/api/admin/products/${selected.id}/inventory`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            onlineStock: Number(form.onlineStock),
            storeStock: Number(form.storeStock),
          }),
        }
      );

      await refresh();
      startEdit(updated);
      setMessage("Inventory updated successfully.");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Inventory update failed"
      );
    } finally {
      setBusy(false);
    }
  }

  async function toggleActive(product: Product) {
    setBusy(true);
    setMessage("");
    setError("");

    try {
      await apiRequest(
        `/api/admin/products/${product.id}/active?active=${!product.active}`,
        { method: "PATCH" }
      );

      await refresh();

      if (selected?.id === product.id) {
        const updated = await apiRequest(
          `/api/admin/products/${product.id}`
        );
        startEdit(updated);
      }

      setMessage(
        product.active
          ? "Product deactivated."
          : "Product activated."
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Status update failed"
      );
    } finally {
      setBusy(false);
    }
  }

  const filtered = products.filter((product) => {
    const matchesSearch =
      `${product.name} ${product.sku} ${product.brand ?? ""}`
        .toLowerCase()
        .includes(search.trim().toLowerCase());

    const matchesFilter =
      filter === "ALL" ||
      (filter === "ACTIVE" && product.active) ||
      (filter === "INACTIVE" && !product.active) ||
      (filter === "LOW_STOCK" &&
        (product.onlineStock <= 5 || product.storeStock <= 5));

    return matchesSearch && matchesFilter;
  });

  const activeCount = products.filter(
    (product) => product.active
  ).length;

  const inactiveCount = products.length - activeCount;

  const lowStockCount = products.filter(
    (product) =>
      product.onlineStock <= 5 || product.storeStock <= 5
  ).length;

  return (
    <main className="min-h-screen bg-[#F7FAF9] p-4 text-slate-900 md:p-8">
      <div className="mx-auto max-w-7xl">
        <header className="mb-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-widest text-[#075E54]">
              KCMART Administration
            </p>
            <h1 className="mt-2 text-3xl font-bold">
              Product Management
            </h1>
          </div>

          <Link
            href="/admin"
            className="rounded-xl border border-[#DDEBE7] bg-white px-5 py-3 font-semibold text-[#075E54] transition duration-200 hover:bg-[#EAF5F2] active:scale-95"
          >
            Dashboard
          </Link>
        </header>

        {message && (
          <div
            role="status"
            className="mb-5 rounded-xl border border-green-200 bg-green-50 p-4 text-green-800"
          >
            {message}
          </div>
        )}

        {error && (
          <div
            role="alert"
            className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-red-800"
          >
            {error}
          </div>
        )}

        <div className="grid gap-6 lg:grid-cols-[1fr_1.2fr]">
          <section className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-[0_8px_30px_rgba(15,23,42,0.04)]">
            <div className="mb-5 flex items-center justify-between gap-3">
              <h2 className="text-xl font-bold">
                Products ({filtered.length})
              </h2>

              <button
                type="button"
                onClick={startNew}
                className="kcmart-primary-button rounded-xl bg-[#075E54] px-4 py-2 font-semibold text-white"
              >
                + Add
              </button>
            </div>

            <div className="mb-5">
              <div className="relative">
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search name, SKU or brand"
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition duration-200 focus:border-[#075E54] focus:ring-4 focus:ring-[#EAF5F2]"
                />
              </div>

              <div className="mt-3 flex flex-wrap gap-2">
                {[
                  ["ALL", `All ${products.length}`],
                  ["ACTIVE", `Active ${activeCount}`],
                  ["INACTIVE", `Inactive ${inactiveCount}`],
                  ["LOW_STOCK", `Low stock ${lowStockCount}`],
                ].map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() =>
                      setFilter(
                        value as
                          | "ALL"
                          | "ACTIVE"
                          | "INACTIVE"
                          | "LOW_STOCK"
                      )
                    }
                    className={`rounded-full border px-3 py-2 text-xs font-bold transition duration-200 active:scale-95 ${
                      filter === value
                        ? "border-[#075E54] bg-[#075E54] text-white shadow-sm"
                        : "border-slate-200 bg-white text-slate-600 hover:border-[#BFDCD5] hover:bg-[#F4F9F7] hover:text-[#075E54]"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {loading && (
              <div className="flex items-center justify-center gap-3 rounded-xl border border-[#EAF5F2] bg-[#F7FAF9] px-4 py-10 text-sm font-semibold text-slate-600">
                <span className="kcmart-loader" />
                Loading products...
              </div>
            )}

            <div className="space-y-3">
              {filtered.map((product) => (
                <article
                  key={product.id}
                  className="kcmart-hover-card rounded-xl border border-slate-200 bg-white p-4 transition duration-200"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="font-bold">
                        {product.name}
                      </h3>
                      <p className="mt-1 text-sm text-slate-500">
                        {product.sku}
                      </p>
                      <p className="mt-2 font-semibold">
                        ₹{product.price}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        Online: {product.onlineStock} · Store:{" "}
                        {product.storeStock}
                      </p>
                    </div>

                    <span
                      className={
                        product.active
                          ? "rounded-full bg-green-100 px-3 py-1 text-xs font-bold text-green-700"
                          : "rounded-full bg-red-100 px-3 py-1 text-xs font-bold text-red-700"
                      }
                    >
                      {product.active
                        ? "Active"
                        : "Inactive"}
                    </span>
                  </div>

                  <div className="mt-4 flex gap-2">
                    <button
                      type="button"
                      onClick={() => startEdit(product)}
                      className="rounded-lg bg-[#075E54] px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#064E46] active:scale-95"
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => toggleActive(product)}
                      className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold transition hover:border-[#BFDCD5] hover:bg-[#F4F9F7] hover:text-[#075E54] active:scale-95 disabled:opacity-50"
                    >
                      {product.active
                        ? "Deactivate"
                        : "Activate"}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <section className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-[0_8px_30px_rgba(15,23,42,0.04)]">
            <h2 className="mb-6 text-xl font-bold">
              {selected
                ? `Edit product #${selected.id}`
                : "Add new product"}
            </h2>

            {selected ? (
              <ProductPhotoManager
                key={selected.id}
                productId={selected.id}
                onChanged={async () => {
                  await refresh();

                  const updated = await apiRequest(
                    `/api/admin/products/${selected.id}`
                  );

                  setSelected(updated);

                  setForm((current) => ({
                    ...current,
                    imageUrl: updated.imageUrl ?? "",
                  }));
                }}
              />
            ) : (
              <section className="mb-6 rounded-2xl border border-dashed border-[#BFDCD5] bg-[#F4F9F7] p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <div>
                    <h3 className="text-lg font-bold text-[#111827]">
                      Product Photos
                    </h3>

                    <p className="mt-1 text-sm text-[#6B7280]">
                      Select up to 6 photos. Maximum 4 MB per photo.
                    </p>
                  </div>

                  <span className="rounded-full border border-[#BFDCD5] bg-white px-3 py-1 text-xs font-bold text-[#075E54]">
                    {photos.length} / 6 selected
                  </span>
                </div>

                {photoPreviews.length > 0 && (
                  <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                    {photoPreviews.map((preview, index) => (
                      <div
                        key={`${preview}-${index}`}
                        className="rounded-xl border border-[#DDEBE7] bg-white p-2 shadow-sm"
                      >
                        <img
                          src={preview}
                          alt={`Selected product photo ${index + 1}`}
                          className="h-32 w-full rounded-lg object-contain"
                        />

                        <button
                          type="button"
                          onClick={() => {
                            setPhotos((current) =>
                              current.filter((_, i) => i !== index)
                            );

                            setPhotoPreviews((current) =>
                              current.filter((_, i) => i !== index)
                            );
                          }}
                          className="mt-2 w-full rounded-lg border border-red-200 px-2 py-1.5 text-xs font-bold text-red-600 transition hover:bg-red-50"
                        >
                          Remove
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                <input
                  type="file"
                  multiple
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(event) => {
                    const chosen = Array.from(
                      event.target.files ?? []
                    );

                    event.target.value = "";
                    setError("");

                    if (chosen.length === 0) return;

                    if (chosen.length > 6) {
                      setError(
                        "You can select a maximum of 6 photos."
                      );
                      return;
                    }

                    const oversized = chosen.find(
                      (file) =>
                        file.size === 0 ||
                        file.size > 4_000_000
                    );

                    if (oversized) {
                      setError(
                        `${oversized.name} must be between 1 byte and 4 MB.`
                      );
                      return;
                    }

                    const invalid = chosen.find(
                      (file) =>
                        ![
                          "image/jpeg",
                          "image/png",
                          "image/webp",
                        ].includes(file.type)
                    );

                    if (invalid) {
                      setError(
                        `${invalid.name} is not JPG, PNG or WebP.`
                      );
                      return;
                    }

                    setPhotos(chosen);

                    Promise.all(
                      chosen.map(
                        (file) =>
                          new Promise<string>((resolve) => {
                            const reader = new FileReader();

                            reader.onload = () =>
                              resolve(
                                String(reader.result ?? "")
                              );

                            reader.readAsDataURL(file);
                          })
                      )
                    ).then(setPhotoPreviews);
                  }}
                  className="mb-3 block w-full cursor-pointer rounded-xl border border-[#DDEBE7] bg-white p-3 text-sm text-slate-600 file:mr-4 file:rounded-lg file:border-0 file:bg-[#075E54] file:px-4 file:py-2 file:font-semibold file:text-white hover:file:bg-[#064E46]"
                />

                <p className="text-sm text-slate-600">
                  These photos will upload after the product is created.
                  The first uploaded photo becomes the initial cover.
                  You can change the cover later from Edit Product.
                </p>
              </section>
            )}
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="sm:col-span-2">
                <span className="mb-2 block text-sm font-semibold">
                  Category *
                </span>

                <select
                  value={form.categoryId}
                  onChange={(event) =>
                    setField(
                      "categoryId",
                      event.target.value
                    )
                  }
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition duration-200 focus:border-[#075E54] focus:ring-4 focus:ring-[#EAF5F2]"
                >
                  <option value="">
                    Select a category
                  </option>

                  {categories.map((category) => (
                    <option
                      key={category.id}
                      value={category.id}
                    >
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>

              {(
                [
                  ["name", "Product name *"],
                  ["slug", "URL slug *"],
                  ["sku", "SKU *"],
                  ["brand", "Brand"],
                  ["price", "Selling price *"],
                  ["mrp", "MRP"],
                  
                ] as const
              ).map(([field, label]) => (
                <label
                  key={field}
                  className=""
                >
                  <span className="mb-2 block text-sm font-semibold">
                    {label}
                  </span>

                  <input
                    value={form[field]}
                    onChange={(event) =>
                      setField(field, event.target.value)
                    }
                    type={
                      field === "price" || field === "mrp"
                        ? "number"
                        : "text"
                    }
                    min={
                      field === "price" || field === "mrp"
                        ? "0"
                        : undefined
                    }
                    step={
                      field === "price" || field === "mrp"
                        ? "0.01"
                        : undefined
                    }
                    className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition duration-200 focus:border-[#075E54] focus:ring-4 focus:ring-[#EAF5F2]"
                  />
                </label>
              ))}

              <label className="sm:col-span-2">
                <span className="mb-2 block text-sm font-semibold">
                  Description
                </span>

                <textarea
                  value={form.description}
                  onChange={(event) =>
                    setField(
                      "description",
                      event.target.value
                    )
                  }
                  rows={4}
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition duration-200 focus:border-[#075E54] focus:ring-4 focus:ring-[#EAF5F2]"
                />
              </label>
            </div>

            <button
              type="button"
              disabled={busy}
              onClick={saveProduct}
              className="kcmart-primary-button mt-6 w-full rounded-xl bg-[#075E54] px-5 py-3 font-bold text-white shadow-sm disabled:pointer-events-none disabled:opacity-50"
            >
              {busy
                ? "Saving..."
                : selected
                  ? "Save changes"
                  : "Create product"}
            </button>

            <div className="mt-8 rounded-2xl border border-[#DDEBE7] bg-[#F7FAF9] p-5">
              <h3 className="mb-4 text-lg font-bold">
                Inventory
              </h3>

              <div className="grid gap-4 sm:grid-cols-2">
                {(
                  [
                    ["onlineStock", "Online stock"],
                    ["storeStock", "Store stock"],
                  ] as const
                ).map(([field, label]) => (
                  <label key={field}>
                    <span className="mb-2 block text-sm font-semibold">
                      {label}
                    </span>

                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={form[field]}
                      onChange={(event) =>
                        setField(
                          field,
                          event.target.value
                        )
                      }
                      className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 outline-none transition duration-200 focus:border-[#075E54] focus:ring-4 focus:ring-[#EAF5F2]"
                    />
                  </label>
                ))}
              </div>

              {selected ? (
                <button
                  type="button"
                  disabled={busy}
                  onClick={saveInventory}
                  className="kcmart-primary-button mt-5 w-full rounded-xl bg-[#075E54] px-5 py-3 font-bold text-white shadow-sm disabled:pointer-events-none disabled:opacity-50"
                >
                  Update inventory
                </button>
              ) : (
                <p className="mt-4 text-sm text-slate-500">
                  Initial stock will be saved when you
                  create the product.
                </p>
              )}
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}