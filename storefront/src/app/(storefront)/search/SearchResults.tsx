"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import {
  Search,
  SlidersHorizontal,
  Package,
  X,
  ArrowRight
} from "lucide-react";
import type { Product, Category } from "@/lib/api";

type Sort = "default" | "low" | "high" | "newest";

const money = (value: number) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(value);

export default function SearchResults({
  initialQuery,
  products,
  categories
}: {
  initialQuery: string;
  products: Product[];
  categories: Category[];
}) {
  const [query, setQuery] = useState(initialQuery);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [sort, setSort] = useState<Sort>("default");
  const [saleOnly, setSaleOnly] = useState(false);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();

    const filtered = products.filter(product => {
      const category = categories.find(
        item => item.id === product.categoryId
      );

      const text = [
        product.name,
        product.brand ?? "",
        product.sku,
        product.description ?? "",
        category?.name ?? ""
      ].join(" ").toLowerCase();

      return (
        (!q || text.includes(q)) &&
        (categoryId === null ||
          product.categoryId === categoryId) &&
        (!saleOnly ||
          (product.mrp !== null &&
            product.mrp > product.price))
      );
    });

    if (sort === "low") {
      filtered.sort((a, b) => a.price - b.price);
    } else if (sort === "high") {
      filtered.sort((a, b) => b.price - a.price);
    } else if (sort === "newest") {
      filtered.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() -
          new Date(a.createdAt).getTime()
      );
    }

    return filtered;
  }, [products, categories, query, categoryId, sort, saleOnly]);

  const matchingCategories = categories.filter(
    category =>
      query.trim() &&
      category.name.toLowerCase().includes(
        query.trim().toLowerCase()
      )
  );

  return (
    <main className="min-h-screen bg-[#F7FAF9] px-4 pb-12 pt-6">
      <div className="mx-auto max-w-7xl">
        <p className="text-xs font-bold uppercase tracking-widest text-[#075E54]">
          Discover KCMART
        </p>

        <h1 className="mt-2 text-2xl font-black text-slate-900 sm:text-4xl">
          Find your next upgrade
        </h1>

        <p className="mt-2 text-sm text-slate-500">
          Explore computers and technology products.
        </p>

        <section className="mt-6 rounded-3xl bg-white p-3 shadow-sm sm:p-5">
          <div className="flex items-center gap-3 rounded-2xl border border-[#D9ECE7] bg-[#EAF5F2]/40 px-4">
            <Search size={20} className="text-[#075E54]" />

            <input
              type="search"
              value={query}
              onChange={event => setQuery(event.target.value)}
              placeholder="Search laptops, printers, SSD..."
              aria-label="Search products"
              className="min-w-0 flex-1 bg-transparent py-4 text-sm outline-none"
            />

            {query && (
              <button
                type="button"
                onClick={() => setQuery("")}
                aria-label="Clear search"
              >
                <X size={18} />
              </button>
            )}
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setCategoryId(null)}
              className={`rounded-xl px-4 py-3 text-xs font-bold ${
                categoryId === null
                  ? "bg-[#075E54] text-white"
                  : "bg-slate-100 text-slate-700"
              }`}
            >
              All
            </button>

            {categories.map(category => (
              <button
                key={category.id}
                type="button"
                onClick={() => setCategoryId(category.id)}
                className={`rounded-xl px-4 py-3 text-xs font-bold ${
                  categoryId === category.id
                    ? "bg-[#075E54] text-white"
                    : "bg-slate-100 text-slate-700"
                }`}
              >
                {category.name}
              </button>
            ))}
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <label className="flex min-h-11 flex-1 items-center gap-2 rounded-xl border border-slate-200 px-3 sm:flex-none">
              <SlidersHorizontal size={17} />

              <select
                aria-label="Sort products"
                value={sort}
                onChange={event =>
                  setSort(event.target.value as Sort)
                }
                className="min-w-0 flex-1 bg-white py-3 text-sm outline-none"
              >
                <option value="default">Default Order</option>
                <option value="low">Price: Low to High</option>
                <option value="high">Price: High to Low</option>
                <option value="newest">Newest First</option>
              </select>
            </label>

            <button
              type="button"
              aria-pressed={saleOnly}
              onClick={() => setSaleOnly(value => !value)}
              className={`min-h-11 rounded-xl border px-4 text-sm font-bold ${
                saleOnly
                  ? "border-[#075E54] bg-[#075E54] text-white"
                  : "border-slate-200 bg-white text-slate-700"
              }`}
            >
              On Sale
            </button>
          </div>
        </section>

        {matchingCategories.length > 0 && (
          <section className="mt-6">
            <h2 className="font-bold text-slate-900">
              Matching Categories
            </h2>

            <div className="mt-3 flex flex-wrap gap-2">
              {matchingCategories.map(category => (
                <Link
                  key={category.id}
                  href={`/category/${category.slug}`}
                  className="rounded-xl border border-[#BFDCD5] bg-white px-4 py-3 text-sm font-semibold text-[#075E54]"
                >
                  {category.name}
                  <ArrowRight size={14} className="ml-2 inline" />
                </Link>
              ))}
            </div>
          </section>
        )}

        <div className="mb-5 mt-8">
          <h2 className="text-xl font-black text-slate-900">
            {query.trim() ? "Search Results" : "All Products"}
          </h2>

          <p className="mt-1 text-sm text-slate-500">
            {results.length} products found
          </p>
        </div>

        {results.length === 0 ? (
          <div className="rounded-3xl bg-white p-12 text-center">
            <Search
              size={42}
              className="mx-auto text-[#075E54]"
            />

            <h2 className="mt-4 text-xl font-black">
              No products found
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              Try a different search or clear your filters.
            </p>

            <button
              type="button"
              onClick={() => {
                setQuery("");
                setCategoryId(null);
                setSaleOnly(false);
                setSort("default");
              }}
              className="mt-5 rounded-xl bg-[#075E54] px-6 py-3 text-sm font-bold text-white"
            >
              Clear Filters
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 lg:gap-5">
            {results.map(product => {
              const discount =
                product.mrp !== null &&
                product.mrp > product.price
                  ? Math.round(
                      (product.mrp - product.price) /
                        product.mrp * 100
                    )
                  : 0;

              return (
                <Link
                  key={product.id}
                  href={`/product/${product.slug}`}
                  className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white p-3 shadow-sm transition active:scale-[0.98]"
                >
                  <div className="relative flex aspect-square items-center justify-center rounded-xl bg-slate-50">
                    {product.imageUrl ? (
                      <img
                        src={
                          product.imageUrl.startsWith("/")
                            ? `${product.imageUrl.replace("/api/v1/product-images/", "/api/product-images/")}`
                            : product.imageUrl
                        }
                        alt={product.name}
                        loading="lazy"
                        className="h-full w-full object-contain p-3"
                      />
                    ) : (
                      <Package
                        size={42}
                        className="text-slate-300"
                      />
                    )}

                    {discount > 0 && (
                      <span className="absolute left-2 top-2 rounded-lg bg-green-600 px-2 py-1 text-[10px] font-bold text-white">
                        {discount}% OFF
                      </span>
                    )}
                  </div>

                  <p className="mt-3 text-[10px] font-bold uppercase text-[#075E54]">
                    {product.brand ?? "KCMART"}
                  </p>

                  <h3 className="mt-1 line-clamp-2 min-h-10 text-sm font-bold text-slate-900">
                    {product.name}
                  </h3>

                  <p className="mt-3 text-lg font-black text-slate-900">
                    {money(product.price)}
                  </p>

                  {product.mrp !== null &&
                    product.mrp > product.price && (
                      <p className="text-xs text-slate-400 line-through">
                        {money(product.mrp)}
                      </p>
                    )}

                  <span className="mt-auto pt-4">
                    <span className="flex min-h-11 items-center justify-center rounded-xl bg-[#075E54] px-2 text-xs font-bold text-white">
                      View Product
                    </span>
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </main>
  );
}