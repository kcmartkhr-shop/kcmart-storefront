import Link from "next/link";
import {
  Laptop,
  HardDrive,
  Monitor,
  Keyboard,
  ArrowUpRight
} from "lucide-react";

import { Product, formatINR } from "@/data/catalog";

const productIcons = {
  laptop: Laptop,
  "hard-drive": HardDrive,
  monitor: Monitor,
  keyboard: Keyboard
};

export default function ProductCard({
  product
}: {
  product: Product;
}) {
  const Icon =
    productIcons[product.icon as keyof typeof productIcons] || Monitor;

  return (
    <article className="group kcmart-hover-card overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm">
      <div className="relative kcmart-image-hover flex aspect-[4/3] items-center justify-center bg-gradient-to-br from-slate-50 to-[#EAF5F2]">
        <Icon
          size={95}
          strokeWidth={1}
          className="text-slate-400 transition duration-500 group-hover:scale-110 group-hover:text-[#075E54]"
        />

        {product.badge && (
          <span className="absolute left-3 top-3 rounded-full bg-[#D9ECE7] px-3 py-1 text-[11px] font-bold text-[#075E54]">
            {product.badge}
          </span>
        )}
      </div>

      <div className="p-4 sm:p-5">
        <p className="text-xs font-semibold uppercase tracking-wide text-[#075E54]">
          {product.category}
        </p>

        <h3 className="mt-2 min-h-12 text-sm font-bold leading-6 text-slate-900 sm:text-base">
          {product.name}
        </h3>

        <div className="mt-4 flex flex-wrap items-baseline gap-2">
          <span className="text-lg font-black text-slate-900">
            {formatINR(product.price)}
          </span>

          <span className="text-xs text-slate-400 line-through">
            {formatINR(product.originalPrice)}
          </span>
        </div>

        <p className="mt-1 text-xs text-slate-500">
          Illustrative price • Not available to order yet
        </p>

        <Link
          href={`/category/${product.category.toLowerCase().replace(/\s+/g, "-")}`}
          className="kcmart-primary-button mt-5 flex w-full items-center justify-center gap-2 rounded-xl border border-[#BFDCD5] bg-[#075E54] px-4 py-3 text-sm font-bold text-white"
        >
          Browse Category
          <ArrowUpRight size={16} />
        </Link>
      </div>
    </article>
  );
}
