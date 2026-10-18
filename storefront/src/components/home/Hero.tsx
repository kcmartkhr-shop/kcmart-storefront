import Link from "next/link";
import {
  ArrowRight,
  Laptop,
  ShieldCheck,
  Truck,
  Store,
  Sparkles
} from "lucide-react";

export default function Hero() {
  return (
    <section className="mx-auto max-w-7xl px-4 pt-6 lg:px-6 lg:pt-9">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#043D36] via-[#075E54] to-[#0B7A69] px-6 py-12 text-white sm:px-10 lg:px-16 lg:py-20">
        <div className="absolute -right-24 -top-32 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute bottom-0 right-12 h-48 w-48 rounded-full bg-emerald-300/20 blur-3xl" />

        <div className="relative grid items-center gap-10 lg:grid-cols-2">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-4 py-2 text-xs font-bold uppercase tracking-widest">
              <Sparkles size={14} />
              Welcome to KCMART
            </span>

            <h1 className="mt-6 max-w-xl text-4xl font-black leading-tight tracking-tight sm:text-5xl lg:text-6xl">
              Everything Tech.
              <span className="block text-emerald-100">
                One Trusted Store.
              </span>
            </h1>

            <p className="mt-5 max-w-lg text-base leading-8 text-emerald-100">
              Explore computers, laptops, printers, components and
              essential IT accessories from Khariar Computer Service.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/categories"
                className="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-4 text-sm font-bold text-[#075E54] transition hover:-translate-y-0.5 hover:shadow-xl"
              >
                Explore Categories
                <ArrowRight size={18} />
              </Link>

              <Link
                href="/store"
                className="inline-flex items-center gap-2 rounded-xl border border-white/40 px-6 py-4 text-sm font-bold text-white hover:bg-white/10"
              >
                Visit Our Store
              </Link>
            </div>
          </div>

          <div className="relative flex min-h-64 items-center justify-center lg:min-h-96">
            <div className="absolute h-64 w-64 rounded-full border border-white/20 sm:h-80 sm:w-80" />
            <div className="absolute h-52 w-52 rounded-full border border-white/20 sm:h-64 sm:w-64" />

            <div className="relative rounded-[2rem] border border-white/20 bg-white/15 p-10 shadow-2xl backdrop-blur-lg sm:p-16">
              <Laptop
                size={150}
                strokeWidth={1.1}
                className="h-32 w-32 text-white sm:h-44 sm:w-44"
              />
            </div>

            <div className="absolute bottom-2 left-2 rounded-2xl bg-white px-5 py-4 text-slate-900 shadow-xl sm:left-8">
              <p className="text-xs font-semibold text-slate-500">
                Your destination for
              </p>
              <p className="mt-1 text-lg font-black">
                Computer & IT Products
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {[
          {
            title: "Pan-India Delivery",
            subtitle: "Manually arranged shipping",
            icon: Truck
          },
          {
            title: "Store Pickup",
            subtitle: "Collect from Khariar",
            icon: Store
          },
          {
            title: "Product Assistance",
            subtitle: "Help choosing your tech",
            icon: ShieldCheck
          },
          {
            title: "IT Essentials",
            subtitle: "Computers to accessories",
            icon: Laptop
          }
        ].map((item) => (
          <div
            key={item.title}
            className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm"
          >
            <div className="rounded-xl bg-[#EAF5F2] p-3 text-[#075E54]">
              <item.icon size={22} />
            </div>

            <div>
              <p className="text-sm font-bold text-slate-900">
                {item.title}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {item.subtitle}
              </p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
