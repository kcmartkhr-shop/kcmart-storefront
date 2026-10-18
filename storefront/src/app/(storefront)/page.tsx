import Link from "next/link";
import FeaturedProducts from "@/components/home/FeaturedProducts";
import MobileHome from "@/components/home/MobileHome";
import Image from "next/image";
import {
  ArrowRight,
  BadgeCheck,
  MapPin,
  Truck,
  Headphones,
  ChevronRight
} from "lucide-react";

const featuredCategories = [
  {
    name: "Laptops",
    href: "/category/laptop",
    image: "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500&q=85"
  },
  {
    name: "Desktops",
    href: "/category/desktop",
    image: "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=500&q=85"
  },
  {
    name: "Printers",
    href: "/category/printers",
    image: "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=500&q=85"
  },
  {
    name: "Monitors",
    href: "/category/monitor",
    image: "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&q=85"
  },
  {
    name: "Components",
    href: "/category/ssd",
    image: "https://images.unsplash.com/photo-1541029071515-84cc54f84dc5?w=500&q=85"
  },
  {
    name: "Accessories",
    href: "/categories",
    image: "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500&q=85"
  }
];

export const dynamic = "force-dynamic";

export default function HomePage() {
  return (
    <div className="bg-[#F7FAF9]">

      <MobileHome />
      <div className="hidden lg:block">

      {/* PREMIUM HERO */}
      <section className="mx-auto max-w-[1440px] px-4 pt-5 sm:px-6 lg:pt-8">
        <div className="grid gap-4 lg:grid-cols-[1.75fr_1fr]">

          <div className="group relative min-h-[450px] overflow-hidden rounded-[28px] bg-[#064E46] text-white shadow-[0_18px_55px_rgba(6,78,70,0.14)] transition duration-500 hover:shadow-[0_24px_70px_rgba(6,78,70,0.20)] sm:min-h-[490px]">

            <div className="absolute inset-0 bg-gradient-to-r from-[#043D36] via-[#075E54] to-[#0B7A69]" />

            <div className="absolute -right-32 -top-32 h-[450px] w-[450px] rounded-full border-[70px] border-white/5" />

            <div className="relative z-10 flex h-full flex-col justify-center px-7 py-12 sm:px-12 lg:max-w-[65%]">

              <span className="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-xs font-bold uppercase tracking-widest text-emerald-100">
                <BadgeCheck size={15} />
                Welcome to KCMART
              </span>

              <h1 className="max-w-xl text-4xl font-black leading-[1.12] tracking-tight sm:text-5xl lg:text-[55px]">
                Upgrade Your
                <span className="block text-[#A7E3D5]">
                  Digital World.
                </span>
              </h1>

              <p className="mt-6 max-w-md text-sm leading-7 text-emerald-100 sm:text-base">
                Discover computers, laptops, printers and
                everyday technology essentials at KCMART.
              </p>

              <div className="mt-9 flex flex-wrap gap-3">
                <Link
                  href="/categories"
                  className="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-4 text-sm font-bold text-[#075E54] transition duration-300 hover:-translate-y-1 hover:shadow-[0_12px_30px_rgba(0,0,0,0.18)] active:scale-[0.98]"
                >
                  Shop Now
                  <ArrowRight size={18} />
                </Link>

                <Link
                  href="/store"
                  className="inline-flex items-center gap-2 rounded-xl border border-white/30 bg-white/10 px-6 py-4 text-sm font-bold text-white backdrop-blur-sm transition duration-300 hover:-translate-y-1 hover:border-white/50 hover:bg-white/20 active:scale-[0.98]"
                >
                  Visit Our Store
                </Link>
              </div>
            </div>

            <div className="absolute bottom-0 right-[-65px] hidden w-[52%] lg:block">
              <Image
                src="https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=1000&q=90"
                alt="Laptop computer"
                width={850}
                height={650}
                unoptimized
                priority
                className="rounded-tl-[40px] object-cover shadow-2xl transition duration-700 ease-out group-hover:scale-[1.035]"
              />
            </div>
          </div>

          {/* RIGHT PROMOTIONAL PANELS */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-1">

            <Link
              href="/category/printers"
              className="group kcmart-hover-card relative flex min-h-[235px] overflow-hidden rounded-[28px] border border-transparent bg-[#EAF5F2] p-7"
            >
              <div className="relative z-10 max-w-[65%]">
                <span className="text-xs font-bold uppercase tracking-widest text-[#075E54]">
                  Office Essentials
                </span>

                <h2 className="mt-3 text-2xl font-black leading-tight text-slate-900">
                  Print Smarter.
                  <br />
                  Work Better.
                </h2>

                <span className="mt-6 inline-flex items-center gap-1 text-sm font-bold text-[#075E54]">
                  Explore Printers
                  <ChevronRight size={17} />
                </span>
              </div>

              <Image
                src="https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=600&q=85"
                alt="Office printer"
                width={250}
                height={250}
                unoptimized
                className="absolute -bottom-4 -right-12 h-[190px] w-[190px] rounded-2xl object-cover shadow-lg transition duration-700 ease-out group-hover:scale-110 group-hover:-translate-x-1"
              />
            </Link>

            <Link
              href="/category/ssd"
              className="group kcmart-hover-card relative flex min-h-[235px] overflow-hidden rounded-[28px] border border-transparent bg-[#e9f5ef] p-7"
            >
              <div className="relative z-10 max-w-[65%]">
                <span className="text-xs font-bold uppercase tracking-widest text-emerald-700">
                  Performance Essentials
                </span>

                <h2 className="mt-3 text-2xl font-black leading-tight text-slate-900">
                  Build a Faster
                  <br />
                  Setup.
                </h2>

                <span className="mt-6 inline-flex items-center gap-1 text-sm font-bold text-emerald-700">
                  Shop Components
                  <ChevronRight size={17} />
                </span>
              </div>

              <Image
                src="https://images.unsplash.com/photo-1541029071515-84cc54f84dc5?w=600&q=85"
                alt="Computer components"
                width={250}
                height={250}
                unoptimized
                className="absolute -bottom-4 -right-12 h-[190px] w-[190px] rounded-2xl object-cover shadow-lg transition duration-700 ease-out group-hover:scale-110 group-hover:-translate-x-1"
              />
            </Link>
          </div>
        </div>
      </section>

      {/* SERVICE BENEFITS */}
      <section className="mx-auto max-w-[1440px] px-4 pt-5 sm:px-6">
        <div className="grid grid-cols-2 gap-3 rounded-2xl border border-slate-100 bg-white p-5 lg:grid-cols-4 lg:p-7">
          {[
            {
              icon: Truck,
              title: "Pan-India Delivery",
              description: "Manually arranged shipping"
            },
            {
              icon: MapPin,
              title: "Store Pickup",
              description: "Collect from Khariar"
            },
            {
              icon: BadgeCheck,
              title: "IT Product Selection",
              description: "Computers and accessories"
            },
            {
              icon: Headphones,
              title: "Product Assistance",
              description: "Help with your purchase"
            }
          ].map((item) => (
            <div
              key={item.title}
              className="group rounded-xl p-2 transition duration-300 hover:-translate-y-0.5 hover:bg-[#F4F9F7] flex flex-col gap-3 sm:flex-row sm:items-center"
            >
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-[#EAF5F2] text-[#075E54] transition duration-300 group-hover:scale-110 group-hover:bg-[#075E54] group-hover:text-white">
                <item.icon size={23} strokeWidth={1.7} />
              </div>

              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  {item.title}
                </h3>
                <p className="mt-1 text-xs text-slate-500">
                  {item.description}
                </p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* PHOTOGRAPHIC CATEGORIES */}
      <section className="mx-auto max-w-[1440px] px-4 py-14 sm:px-6">
        <div className="mb-8 flex items-end justify-between gap-4">
          <div>
            <span className="text-xs font-bold uppercase tracking-[0.2em] text-[#075E54]">
              Discover KCMART
            </span>

            <h2 className="mt-3 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">
              Shop by Category
            </h2>

            <p className="mt-2 text-sm text-slate-500">
              Everything you need for your home, office and setup.
            </p>
          </div>

          <Link
            href="/categories"
            className="group inline-flex shrink-0 items-center gap-2 text-sm font-bold text-[#075E54] transition hover:text-[#064E46]"
          >
            All Categories
            <ArrowRight size={17} />
          </Link>
        </div>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6 lg:gap-5">
          {featuredCategories.map((category) => (
            <Link
              key={category.name}
              href={category.href}
              className="group kcmart-hover-card overflow-hidden rounded-2xl border border-slate-100 bg-white p-3 text-center shadow-sm"
            >
              <div className="relative aspect-square overflow-hidden rounded-xl bg-slate-100">
                <Image
                  src={category.image}
                  alt={category.name}
                  fill
                  unoptimized
                  sizes="(max-width: 640px) 45vw, (max-width: 1024px) 30vw, 16vw"
                  className="object-cover transition duration-500 group-hover:scale-110"
                />
              </div>

              <h3 className="mt-4 pb-2 text-sm font-bold text-slate-900 sm:text-base">
                {category.name}
              </h3>
            </Link>
          ))}
        </div>
      </section>

      <FeaturedProducts />

      {/* BUSINESS PROMOTION */}
      <section className="mx-auto max-w-[1440px] px-4 pb-16 sm:px-6">
        <div className="kcmart-hover-card flex flex-col items-start justify-between gap-6 overflow-hidden rounded-[28px] border border-[#DDEBE7] bg-[#F0F8F5] px-7 py-10 sm:px-12 lg:flex-row lg:items-center">
          <div>
            <span className="text-xs font-bold uppercase tracking-widest text-[#075E54]">
              KCMART for Business
            </span>

            <h2 className="mt-3 text-2xl font-black text-slate-900 sm:text-3xl">
              Technology for Every Workspace.
            </h2>

            <p className="mt-3 max-w-xl text-sm leading-7 text-slate-600">
              Explore desktops, printers, accessories and IT essentials
              for your office or business.
            </p>
          </div>

          <Link
            href="/categories"
            className="kcmart-primary-button inline-flex shrink-0 items-center gap-2 rounded-xl bg-[#075E54] px-7 py-4 text-sm font-bold text-white"
          >
            Explore Products
            <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      </div>
    </div>
  );
}