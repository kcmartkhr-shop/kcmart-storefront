import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { getProducts } from "@/lib/api";

export default async function FeaturedProducts() {
  const products = (await getProducts()).filter(
    (product) => product.active
  ).slice(0, 8);

  return (
    <section className="mx-auto max-w-[1440px] px-4 pb-16 sm:px-6">
      <div className="mb-8 flex items-end justify-between gap-4">
        <div>
          <span className="text-xs font-bold uppercase tracking-[0.2em] text-[#075E54]">
            Explore Our Collection
          </span>

          <h2 className="mt-3 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
            Featured Products
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Explore products available at KCMART.
          </p>
        </div>

        <Link
          href="/categories"
          className="inline-flex shrink-0 items-center gap-2 text-sm font-bold text-[#075E54]"
        >
          View All
          <ArrowRight size={17} />
        </Link>
      </div>

      {products.length === 0 ? (
        <div className="rounded-2xl bg-slate-50 p-10 text-center text-slate-500">
          Products coming soon.
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
          {products.map((product) => (
            <article
              key={product.id}
              className="overflow-hidden rounded-2xl border border-slate-200 bg-white p-3 shadow-sm transition hover:-translate-y-1 hover:shadow-lg sm:p-5"
            >
              <Link href={`/product/${product.slug}`} className="block">
              <div className="mb-4 flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-slate-50">
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
                  <span className="text-5xl">📦</span>
                )}
              </div>

              <p className="text-xs font-bold uppercase text-[#075E54]">
                {product.brand ?? "KCMART"}
              </p>

              <h3 className="mt-2 line-clamp-2 min-h-12 font-bold text-slate-900">
                {product.name}
              </h3>

              <p className="mt-4 text-xl font-extrabold text-slate-900">
                ₹{product.price.toLocaleString("en-IN")}
              </p>

              {product.mrp !== null &&
                product.mrp > product.price && (
                  <p className="text-sm text-slate-400 line-through">
                    ₹{product.mrp.toLocaleString("en-IN")}
                  </p>
                )}
              </Link>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}