import Link from "next/link";
import {
  ArrowRight,
  Headphones,
  MapPin,
  Package,
  Truck
} from "lucide-react";

import { getProducts } from "@/lib/api";
import MobileHeroCarousel from "./MobileHeroCarousel";

const categories = [
  {
    name: "Laptops",
    href: "/category/laptop",
    image:
      "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=300&q=80"
  },
  {
    name: "Desktops",
    href: "/category/desktop",
    image:
      "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=300&q=80"
  },
  {
    name: "Printers",
    href: "/category/printers",
    image:
      "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=300&q=80"
  },
  {
    name: "Monitors",
    href: "/category/monitor",
    image:
      "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=300&q=80"
  },
  {
    name: "Components",
    href: "/category/ssd",
    image:
      "https://images.unsplash.com/photo-1541029071515-84cc54f84dc5?w=300&q=80"
  },
  {
    name: "View All",
    href: "/categories",
    image:
      "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=300&q=80"
  }
];

function productImage(url: string) {
  return url.startsWith("/")
    ? `${url.replace("/api/v1/product-images/", "/api/product-images/")}`
    : url;
}

export default async function MobileHome() {
  const products = (await getProducts())
    .filter((product) => product.active)
    .slice(0, 8);

  return (
    <div className="bg-[#F7FAF9] pb-8 lg:hidden">
      <div className="px-3 pt-3">
        <MobileHeroCarousel />
      </div>

      <section className="mt-7 px-4">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-black tracking-tight text-slate-900">
            Shop by Category
          </h2>

          <Link
            href="/categories"
            className="inline-flex items-center gap-1 text-xs font-bold text-[#075E54]"
          >
            View All
            <ArrowRight size={15} />
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-x-3 gap-y-5">
          {categories.map((category) => (
            <Link
              key={category.name}
              href={category.href}
              className="flex min-w-0 flex-col items-center gap-2 transition active:scale-95"
            >
              <div className="flex aspect-square w-full max-w-[110px] items-center justify-center overflow-hidden rounded-[22px] border border-[#EAF5F2] bg-white p-2 shadow-sm">
                <img
                  src={category.image}
                  alt=""
                  loading="lazy"
                  className="h-full w-full rounded-[15px] object-cover"
                />
              </div>

              <span className="text-center text-[12px] font-bold text-slate-800">
                {category.name}
              </span>
            </Link>
          ))}
        </div>
      </section>

      <section className="mt-9 px-4">
        <div className="mb-4 flex items-end justify-between gap-2">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-widest text-[#075E54]">
              Discover KCMART
            </p>

            <h2 className="mt-1 text-xl font-black text-slate-900">
              Featured Products
            </h2>
          </div>

          <Link
            href="/categories"
            className="inline-flex items-center gap-1 text-xs font-bold text-[#075E54]"
          >
            View All
            <ArrowRight size={15} />
          </Link>
        </div>

        {products.length === 0 ? (
          <div className="rounded-2xl bg-white p-8 text-center text-slate-500">
            Products coming soon.
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {products.map((product) => {
              const discount =
                product.mrp !== null &&
                product.mrp > product.price
                  ? Math.round(
                      ((product.mrp - product.price) /
                        product.mrp) *
                        100
                    )
                  : 0;

              return (
                <Link
                  key={product.id}
                  href={`/product/${product.slug}`}
                  className="group kcmart-hover-card flex min-w-0 flex-col overflow-hidden rounded-[20px] border border-slate-100 bg-white p-2.5 shadow-sm active:scale-[0.97]"
                >
                  <div className="relative kcmart-image-hover flex aspect-square items-center justify-center overflow-hidden rounded-[15px] bg-[#F4F9F7]">
                    {product.imageUrl ? (
                      <img
                        src={productImage(product.imageUrl)}
                        alt={product.name}
                        loading="lazy"
                        className="h-full w-full object-contain p-2 transition duration-500 group-hover:scale-105 group-active:scale-95"
                      />
                    ) : (
                      <Package
                        size={50}
                        className="text-slate-300"
                      />
                    )}

                    {discount > 0 && (
                      <span className="absolute left-2 top-2 rounded-lg bg-green-600 px-2 py-1 text-[10px] font-extrabold text-white">
                        {discount}% OFF
                      </span>
                    )}
                  </div>

                  <p className="mt-3 text-[10px] font-extrabold uppercase tracking-wide text-[#075E54]">
                    {product.brand ?? "KCMART"}
                  </p>

                  <h3 className="mt-1 line-clamp-2 min-h-[38px] text-[13px] font-bold leading-[19px] text-slate-900">
                    {product.name}
                  </h3>

                  <div className="mt-2 flex flex-wrap items-baseline gap-1.5">
                    <span className="text-[17px] font-black text-slate-900">
                      ₹{product.price.toLocaleString("en-IN")}
                    </span>

                    {product.mrp !== null &&
                      product.mrp > product.price && (
                        <span className="text-[11px] text-slate-400 line-through">
                          ₹{product.mrp.toLocaleString("en-IN")}
                        </span>
                      )}
                  </div>

                  <span className="kcmart-primary-button mt-auto flex min-h-11 items-center justify-center rounded-xl bg-[#075E54] px-2 py-3 text-xs font-extrabold text-white transition active:bg-[#064E46]">
                    View Product
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </section>

      <section className="mx-4 mt-8 grid grid-cols-3 gap-2">
        {[
          { title: "Delivery", icon: Truck },
          { title: "Store Pickup", icon: MapPin },
          { title: "Assistance", icon: Headphones }
        ].map(({ title, icon: Icon }) => (
          <div
            key={title}
            className="kcmart-hover-card flex min-h-[85px] flex-col items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-white p-2 shadow-sm"
          >
            <Icon size={22} className="text-[#075E54]" />
            <span className="text-center text-[11px] font-semibold text-slate-700">
              {title}
            </span>
          </div>
        ))}
      </section>
    </div>
  );
}