import Link from "next/link";
import {
  Laptop,
  Monitor,
  Printer,
  HardDrive,
  Camera,
  BatteryCharging,
  Keyboard,
  ArrowRight
} from "lucide-react";

import { featuredCategories } from "@/data/catalog";

const icons = {
  Laptop,
  Desktop: Monitor,
  Printers: Printer,
  SSD: HardDrive,
  Monitor,
  "CCTV Camera": Camera,
  UPS: BatteryCharging,
  Keyboard
};

export default function Categories() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-14 lg:px-6">
      <div className="mb-7 flex items-end justify-between gap-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#075E54]">
            Explore our store
          </p>

          <h2 className="mt-2 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
            Shop by Category
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            Find exactly what your setup needs.
          </p>
        </div>

        <Link
          href="/categories"
          className="group flex shrink-0 items-center gap-2 text-sm font-bold text-[#075E54] transition hover:text-[#064E46]"
        >
          View All
          <ArrowRight size={17} />
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8">
        {featuredCategories.map((category) => {
          const Icon =
            icons[category.name as keyof typeof icons] || Monitor;

          return (
            <Link
              key={category.slug}
              href={`/category/${category.slug}`}
              className="group kcmart-hover-card flex flex-col items-center rounded-2xl border border-slate-100 bg-white p-5 text-center shadow-sm"
            >
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#EAF5F2] text-[#075E54] transition duration-300 group-hover:scale-110 group-hover:bg-[#075E54] group-hover:text-white group-hover:shadow-lg">
                <Icon size={31} strokeWidth={1.5} />
              </div>

              <span className="text-sm font-bold text-slate-800">
                {category.name}
              </span>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
