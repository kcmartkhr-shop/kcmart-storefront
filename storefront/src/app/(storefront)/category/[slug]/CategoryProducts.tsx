"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  ArrowDownUp,
  Check,
  ChevronDown,
  Package,
  SlidersHorizontal,
  X,
} from "lucide-react";
import type { Product } from "@/lib/api";

type SortMode =
  | "featured"
  | "price-low"
  | "price-high"
  | "newest"
  | "discount";

type Props = {
  products: Product[];
  categoryName: string;
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

function imageSource(imageUrl: string | null) {
  if (!imageUrl) return null;

  if (imageUrl.startsWith("/")) {
    return `${imageUrl.replace("/api/v1/product-images/", "/api/product-images/")}`;
  }

  return imageUrl;
}

function discountPercent(product: Product) {
  if (!product.mrp || product.mrp <= product.price) {
    return 0;
  }

  return Math.round(
    ((product.mrp - product.price) / product.mrp) * 100
  );
}

function ProductTile({ product }: { product: Product }) {
  const discount = discountPercent(product);
  const image = imageSource(product.imageUrl);

  return (
    <article className="group min-w-0 overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition hover:border-[#BFDCD5] hover:shadow-md">
      <Link
        href={`/product/${product.slug}`}
        className="block h-full"
      >
        <div className="relative flex aspect-square items-center justify-center overflow-hidden bg-white p-3 sm:p-5">
          {discount > 0 && (
            <span className="absolute left-2 top-2 z-10 rounded-lg bg-green-600 px-2 py-1 text-[10px] font-extrabold text-white sm:text-xs">
              {discount}% OFF
            </span>
          )}

          {image ? (
            <img
              src={image}
              alt={product.name}
              loading="lazy"
              className="h-full w-full object-contain transition duration-300 group-hover:scale-105"
            />
          ) : (
            <Package
              size={58}
              strokeWidth={1}
              className="text-slate-300"
            />
          )}
        </div>

        <div className="p-3 sm:p-5">
          <p className="truncate text-[10px] font-bold uppercase tracking-wide text-[#075E54] sm:text-xs">
            {product.brand || "KCMART"}
          </p>

          <h2 className="mt-1 line-clamp-2 min-h-10 text-[13px] font-bold leading-5 text-slate-900 sm:text-base">
            {product.name}
          </h2>

          <div className="mt-3 flex flex-wrap items-baseline gap-x-2 gap-y-1">
            <span className="text-base font-black text-slate-950 sm:text-xl">
              {money.format(product.price)}
            </span>

            {discount > 0 && product.mrp && (
              <span className="text-xs text-slate-400 line-through">
                {money.format(product.mrp)}
              </span>
            )}
          </div>

          <span className="mt-3 flex w-full items-center justify-center rounded-xl bg-[#075E54] px-3 py-2.5 text-xs font-bold text-white transition group-hover:bg-[#064E46] sm:text-sm">
            View Product
          </span>
        </div>
      </Link>
    </article>
  );
}

export default function CategoryProducts({
  products,
  categoryName,
}: Props) {
  const [sort, setSort] = useState<SortMode>("featured");
  const [selectedBrands, setSelectedBrands] = useState<string[]>([]);
  const [onSale, setOnSale] = useState(false);
  const [maxPrice, setMaxPrice] = useState<number | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const brands = useMemo(
    () =>
      Array.from(
        new Set(
          products
            .map((product) => product.brand?.trim())
            .filter((brand): brand is string => Boolean(brand))
        )
      ).sort((a, b) => a.localeCompare(b)),
    [products]
  );

  const highestPrice = useMemo(
    () =>
      Math.ceil(
        Math.max(1000, ...products.map((product) => product.price)) /
          1000
      ) * 1000,
    [products]
  );

  const visibleProducts = useMemo(() => {
    const result = products.filter((product) => {
      if (
        selectedBrands.length > 0 &&
        !selectedBrands.includes(product.brand?.trim() || "")
      ) {
        return false;
      }

      if (onSale && discountPercent(product) === 0) {
        return false;
      }

      if (maxPrice !== null && product.price > maxPrice) {
        return false;
      }

      return true;
    });

    switch (sort) {
      case "price-low":
        return result.sort((a, b) => a.price - b.price);
      case "price-high":
        return result.sort((a, b) => b.price - a.price);
      case "newest":
        return result.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() -
            new Date(a.createdAt).getTime()
        );
      case "discount":
        return result.sort(
          (a, b) => discountPercent(b) - discountPercent(a)
        );
      default:
        return result;
    }
  }, [products, selectedBrands, onSale, maxPrice, sort]);

  const activeFilterCount =
    selectedBrands.length +
    (onSale ? 1 : 0) +
    (maxPrice !== null ? 1 : 0);

  function toggleBrand(brand: string) {
    setSelectedBrands((current) =>
      current.includes(brand)
        ? current.filter((item) => item !== brand)
        : [...current, brand]
    );
  }

  function clearFilters() {
    setSelectedBrands([]);
    setOnSale(false);
    setMaxPrice(null);
  }

  const filters = (
    <div className="space-y-6">
      <div>
        <div className="flex items-center justify-between">
          <h3 className="font-extrabold text-slate-900">
            Filters
          </h3>

          <button
            type="button"
            onClick={clearFilters}
            className="text-xs font-bold text-[#075E54]"
          >
            Clear all
          </button>
        </div>

        <p className="mt-1 text-xs text-slate-500">
          Refine your product selection
        </p>
      </div>

      <div className="border-t border-slate-100 pt-5">
        <label className="flex cursor-pointer items-center justify-between gap-3">
          <span className="text-sm font-bold text-slate-800">
            On sale only
          </span>

          <input
            type="checkbox"
            checked={onSale}
            onChange={(event) =>
              setOnSale(event.target.checked)
            }
            className="h-5 w-5 accent-[#075E54]"
          />
        </label>
      </div>

      {brands.length > 0 && (
        <div className="border-t border-slate-100 pt-5">
          <h4 className="mb-4 text-sm font-extrabold text-slate-900">
            Brand
          </h4>

          <div className="space-y-3">
            {brands.map((brand) => (
              <label
                key={brand}
                className="flex cursor-pointer items-center gap-3 text-sm text-slate-700"
              >
                <input
                  type="checkbox"
                  checked={selectedBrands.includes(brand)}
                  onChange={() => toggleBrand(brand)}
                  className="h-4 w-4 accent-[#075E54]"
                />
                {brand}
              </label>
            ))}
          </div>
        </div>
      )}

      <div className="border-t border-slate-100 pt-5">
        <h4 className="text-sm font-extrabold text-slate-900">
          Maximum price
        </h4>

        <p className="mt-2 text-sm font-bold text-[#075E54]">
          {maxPrice === null
            ? "Any price"
            : `Up to ${money.format(maxPrice)}`}
        </p>

        <input
          aria-label="Maximum product price"
          type="range"
          min={0}
          max={highestPrice}
          step={1000}
          value={maxPrice ?? highestPrice}
          onChange={(event) => {
            const value = Number(event.target.value);
            setMaxPrice(
              value >= highestPrice ? null : value
            );
          }}
          className="mt-4 w-full accent-[#075E54]"
        />

        <div className="mt-1 flex justify-between text-xs text-slate-400">
          <span>{money.format(0)}</span>
          <span>{money.format(highestPrice)}</span>
        </div>
      </div>
    </div>
  );

  return (
    <div className="mx-auto max-w-7xl px-3 py-4 sm:px-6 lg:px-8">
      <div className="mb-4 flex items-center justify-between gap-3 rounded-2xl border border-slate-100 bg-white p-3 shadow-sm lg:hidden">
        <button
          type="button"
          onClick={() => setFiltersOpen(true)}
          className="flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 text-sm font-bold text-slate-800"
        >
          <SlidersHorizontal size={17} />
          Filter
          {activeFilterCount > 0 && (
            <span className="rounded-full bg-[#075E54] px-1.5 py-0.5 text-[10px] text-white">
              {activeFilterCount}
            </span>
          )}
        </button>

        <div className="relative flex min-h-11 flex-1 items-center justify-center gap-2 rounded-xl border border-slate-200 px-2">
          <ArrowDownUp size={16} className="shrink-0" />

          <select
            aria-label="Sort products"
            value={sort}
            onChange={(event) =>
              setSort(event.target.value as SortMode)
            }
            className="w-full min-w-0 bg-transparent text-xs font-bold text-slate-800 outline-none"
          >
            <option value="featured">Featured</option>
            <option value="price-low">Price: Low to High</option>
            <option value="price-high">Price: High to Low</option>
            <option value="newest">Newest</option>
            <option value="discount">Biggest Discount</option>
          </select>
        </div>
      </div>

      <div className="flex gap-6">
        <aside className="hidden w-64 shrink-0 self-start rounded-2xl border border-slate-100 bg-white p-5 lg:block">
          {filters}
        </aside>

        <section
          aria-label={`${categoryName} products`}
          className="min-w-0 flex-1"
        >
          <div className="mb-4 flex items-center justify-between gap-3">
            <p className="text-xs font-semibold text-slate-500 sm:text-sm">
              Showing{" "}
              <span className="font-extrabold text-slate-900">
                {visibleProducts.length}
              </span>{" "}
              products
            </p>

            <div className="hidden items-center gap-2 lg:flex">
              <label
                htmlFor="desktop-category-sort"
                className="text-sm text-slate-500"
              >
                Sort by
              </label>

              <select
                id="desktop-category-sort"
                value={sort}
                onChange={(event) =>
                  setSort(event.target.value as SortMode)
                }
                className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-bold text-slate-800"
              >
                <option value="featured">Featured</option>
                <option value="price-low">Price: Low to High</option>
                <option value="price-high">Price: High to Low</option>
                <option value="newest">Newest</option>
                <option value="discount">Biggest Discount</option>
              </select>
            </div>
          </div>

          {activeFilterCount > 0 && (
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <span className="text-xs text-slate-500">
                {activeFilterCount} active filters
              </span>

              <button
                type="button"
                onClick={clearFilters}
                className="rounded-full bg-[#EAF5F2] px-3 py-1 text-xs font-bold text-[#075E54]"
              >
                Clear all
              </button>
            </div>
          )}

          {visibleProducts.length > 0 ? (
            <div className="grid grid-cols-2 gap-2.5 sm:gap-4 xl:grid-cols-3">
              {visibleProducts.map((product) => (
                <ProductTile
                  key={product.id}
                  product={product}
                />
              ))}
            </div>
          ) : (
            <div className="rounded-2xl border border-slate-100 bg-white px-6 py-16 text-center">
              <Package
                size={44}
                className="mx-auto text-slate-300"
              />

              <h2 className="mt-4 text-xl font-extrabold text-slate-900">
                {products.length === 0
                  ? "Products coming soon"
                  : "No matching products"}
              </h2>

              <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-500">
                {products.length === 0
                  ? `We're updating our ${categoryName} collection.`
                  : "Try removing some filters to see more products."}
              </p>

              {products.length > 0 ? (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="mt-5 rounded-xl bg-[#075E54] px-5 py-3 text-sm font-bold text-white"
                >
                  Clear filters
                </button>
              ) : (
                <Link
                  href="/categories"
                  className="mt-5 inline-block rounded-xl bg-[#075E54] px-5 py-3 text-sm font-bold text-white"
                >
                  Explore categories
                </Link>
              )}
            </div>
          )}
        </section>
      </div>

      {filtersOpen && (
        <div
          className="fixed inset-0 z-[100] flex items-end bg-slate-950/60 lg:hidden"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              setFiltersOpen(false);
            }
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Filter products"
            className="flex max-h-[85dvh] w-full flex-col overflow-hidden rounded-t-3xl bg-white"
          >
            <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
              <span className="text-lg font-black text-slate-950">
                Filter products
              </span>

              <button
                type="button"
                aria-label="Close filters"
                onClick={() => setFiltersOpen(false)}
                className="rounded-full bg-slate-100 p-2"
              >
                <X size={20} />
              </button>
            </div>

            <div className="overflow-y-auto px-5 py-6">
              {filters}
            </div>

            <div className="border-t border-slate-100 bg-white px-5 py-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
              <button
                type="button"
                onClick={() => setFiltersOpen(false)}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 py-3.5 text-sm font-extrabold text-white"
              >
                <Check size={18} />
                Show {visibleProducts.length} products
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}