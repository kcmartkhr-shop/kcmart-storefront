"use client";

import { useState } from "react";
import Link from "next/link";
import {
  Search, ShoppingCart, UserRound, Menu, X,
  MapPin, ChevronDown, Headphones, Heart,
  Truck, ArrowRight
} from "lucide-react";

import { categories } from "@/data/catalog";
import { useCart } from "@/components/cart/CartProvider";
import { useCustomerLogin } from "@/components/auth/CustomerLoginProvider";
import { useSession } from "next-auth/react";

const navItems = [
  { name: "Laptops", href: "/category/laptop" },
  { name: "Desktops", href: "/category/desktop" },
  { name: "Printers", href: "/category/printers" },
  { name: "Monitors", href: "/category/monitor" },
  { name: "Components", href: "/category/ssd" },
  { name: "Accessories", href: "/categories" }
];

export default function Header() {
  const { itemCount, ready } = useCart();
  const { openLogin } = useCustomerLogin();
  const { data: session, status } = useSession();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [megaOpen, setMegaOpen] = useState(false);

  const groups = Array.from(
    new Set(categories.map((category) => category.group))
  );

  return (
    <header className="sticky top-0 z-50 bg-white shadow-sm">
      <div className="hidden border-b border-slate-100 bg-[#F7FAF9] lg:block">
        <div className="mx-auto flex max-w-[1440px] items-center justify-between px-6 py-2 text-xs text-slate-600">
          <span className="flex items-center gap-2">
            <MapPin size={13} />
            Khariar Computer Service, Odisha
          </span>

          <div className="flex items-center gap-6">
            <span className="flex items-center gap-1">
              <Truck size={13} />
              Pan-India Delivery
            </span>

            <Link href="/store" className="hover:text-[#075E54]">
              Store Pickup
            </Link>

            <span className="flex items-center gap-1">
              <Headphones size={13} />
              Customer Support
            </span>
          </div>
        </div>
      </div>

      <div className="mx-auto flex max-w-[1440px] items-center gap-5 px-4 py-4 sm:px-6 lg:gap-10">
        <button
          type="button"
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
          aria-expanded={mobileOpen}
          onClick={() => setMobileOpen(!mobileOpen)}
          className="rounded-xl p-2 text-slate-800 transition duration-200 hover:bg-[#EAF5F2] hover:text-[#075E54] active:scale-90 lg:hidden"
        >
          {mobileOpen ? <X size={24} /> : <Menu size={24} />}
        </button>

        <Link href="/" className="group shrink-0 transition duration-300 hover:scale-[1.025]">
          <span className="block text-2xl font-black tracking-[-0.06em] text-[#075E54] sm:text-3xl">
            KC<span className="text-slate-900">MART</span>
          </span>

          <span className="block text-[8px] font-bold uppercase tracking-[0.27em] text-slate-500 sm:text-[9px]">
            Computer & IT Store
          </span>
        </Link>

        <form
          action="/search"
          className="group hidden flex-1 items-center rounded-xl border border-slate-200 bg-[#F7FAF9] p-1.5 pl-4 transition duration-300 focus-within:border-[#075E54] focus-within:bg-white focus-within:shadow-[0_8px_28px_rgba(7,94,84,0.10)] focus-within:ring-4 focus-within:ring-[#EAF5F2] md:flex"
        >
          <Search size={21} className="shrink-0 text-slate-400 transition duration-300 group-focus-within:scale-110 group-focus-within:text-[#075E54]" />

          <input
            name="q"
            aria-label="Search products"
            placeholder="Search laptops, printers, monitors and more..."
            className="ml-3 min-w-0 flex-1 bg-transparent py-2 text-sm text-slate-900 outline-none placeholder:text-slate-400"
          />

          <button
            type="submit"
            className="kcmart-primary-button rounded-lg bg-[#075E54] px-6 py-3 text-sm font-bold text-white"
          >
            Search
          </button>
        </form>

        <div className="ml-auto flex items-center gap-4 sm:gap-6">
          {status === "authenticated" ? (
            <Link
              href="/account"
              aria-label="My account"
              className="group hidden items-center gap-2 rounded-xl px-2 py-2 text-slate-700 transition duration-200 hover:bg-[#F4F9F7] hover:text-[#075E54] sm:flex"
            >
              {session?.user?.image ? (
                <img
                  src={session.user.image}
                  alt=""
                  className="h-8 w-8 rounded-full border border-slate-200 object-cover transition duration-300 group-hover:scale-110 group-hover:border-[#BFDCD5]"
                />
              ) : (
                <UserRound size={23} />
              )}

              <span className="hidden max-w-28 truncate text-sm font-bold xl:block">
                {session?.user?.name?.split(" ")[0] || "Account"}
              </span>
            </Link>
          ) : (
            <button
              type="button"
              onClick={() => openLogin("account")}
              aria-label="My account"
              className="group hidden items-center gap-2 rounded-xl px-2 py-2 text-slate-700 transition duration-200 hover:bg-[#F4F9F7] hover:text-[#075E54] active:scale-95 sm:flex"
            >
              <UserRound size={23} />
              <span className="hidden text-sm font-bold xl:block">
                Account
              </span>
            </button>
          )}

          <Link
            href="/cart"
            aria-label="Shopping cart"
            className="group flex items-center gap-2 rounded-xl px-2 py-2 text-slate-700 transition duration-200 hover:bg-[#F4F9F7] hover:text-[#075E54] active:scale-95"
          >
            <span className="relative">
              <ShoppingCart size={24} className="transition duration-300 group-hover:-translate-y-0.5 group-hover:scale-110" />
              {ready && itemCount > 0 && (
                <span className="absolute -right-3 -top-3 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#075E54] px-1 text-[10px] font-bold text-white shadow-sm transition duration-300 group-hover:scale-110">
                  {itemCount}
                </span>
              )}
            </span>
            <span className="hidden text-sm font-bold xl:block">
              Cart
            </span>
          </Link>
        </div>
      </div>

      <div className="px-4 pb-3 md:hidden">
        <form
          action="/search"
          className="group flex items-center rounded-xl border border-slate-200 bg-[#F7FAF9] px-4 py-3 transition duration-300 focus-within:border-[#075E54] focus-within:bg-white focus-within:ring-4 focus-within:ring-[#EAF5F2]"
        >
          <Search size={19} className="shrink-0 text-slate-400" />

          <input
            name="q"
            aria-label="Search products"
            placeholder="What are you looking for?"
            className="ml-3 min-w-0 flex-1 bg-transparent text-sm outline-none"
          />

          <button type="submit" aria-label="Submit search" className="rounded-lg p-1 transition duration-200 hover:bg-[#EAF5F2] active:scale-90">
            <ArrowRight size={19} className="text-[#075E54] transition duration-200 group-focus-within:translate-x-0.5" />
          </button>
        </form>
      </div>

      <nav className="hidden border-t border-slate-100 lg:block">
        <div className="relative mx-auto flex max-w-[1440px] items-center gap-8 px-6 py-3">
          <button
            type="button"
            onClick={() => setMegaOpen(!megaOpen)}
            aria-expanded={megaOpen}
            className="group flex items-center gap-2 rounded-lg bg-[#EAF5F2] px-4 py-2 text-sm font-bold text-[#075E54] transition duration-200 hover:bg-[#D9ECE7] active:scale-[0.98]"
          >
            <Menu size={18} />
            All Categories
            <ChevronDown size={15} />
          </button>

          {navItems.map((item) => (
            <Link
              key={item.name}
              href={item.href}
              className="relative text-sm font-semibold text-slate-700 transition duration-200 after:absolute after:-bottom-1 after:left-0 after:h-0.5 after:w-0 after:rounded-full after:bg-[#075E54] after:transition-all after:duration-300 hover:text-[#075E54] hover:after:w-full"
            >
              {item.name}
            </Link>
          ))}

          <Link
            href="/store"
            className="group ml-auto flex items-center gap-2 rounded-lg px-2 py-2 text-sm font-bold text-[#075E54] transition hover:bg-[#F4F9F7]"
          >
            <MapPin size={17} />
            Visit Our Store
          </Link>

          {megaOpen && (
            <>
              <button
                type="button"
                aria-label="Close categories"
                onClick={() => setMegaOpen(false)}
                className="fixed inset-0 top-36 z-40 cursor-default bg-slate-950/20"
              />

              <div className="absolute left-6 right-6 top-full z-50 grid grid-cols-4 gap-8 rounded-b-2xl border border-[#DDEBE7] bg-white p-8 shadow-[0_24px_60px_rgba(15,23,42,0.16)]">
                {groups.map((group) => (
                  <div key={group}>
                    <h3 className="mb-4 border-b border-slate-100 pb-3 text-sm font-black text-slate-900">
                      {group}
                    </h3>

                    <div className="flex flex-col gap-3">
                      {categories
                        .filter((category) => category.group === group)
                        .map((category) => (
                          <Link
                            key={category.slug}
                            href={`/category/${category.slug}`}
                            onClick={() => setMegaOpen(false)}
                            className="text-sm text-slate-600 transition hover:translate-x-1 hover:text-[#075E54]"
                          >
                            {category.name}
                          </Link>
                        ))}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </nav>

      {mobileOpen && (
        <nav className="max-h-[70vh] overflow-y-auto border-t border-slate-100 bg-white px-5 py-5 lg:hidden">
          <Link
            href="/categories"
            onClick={() => setMobileOpen(false)}
            className="mb-5 block rounded-xl bg-[#EAF5F2] px-4 py-4 text-sm font-black text-[#075E54] transition duration-200 hover:bg-[#D9ECE7] active:scale-[0.98]"
          >
            Browse All Categories
          </Link>

          <div className="flex flex-col divide-y divide-slate-100">
            {navItems.map((item) => (
              <Link
                key={item.name}
                href={item.href}
                onClick={() => setMobileOpen(false)}
                className="px-2 py-4 text-sm font-semibold text-slate-700 transition duration-200 hover:translate-x-1 hover:text-[#075E54] active:bg-[#F4F9F7]"
              >
                {item.name}
              </Link>
            ))}

            <Link
              href="/store"
              onClick={() => setMobileOpen(false)}
              className="px-2 py-4 text-sm font-semibold text-slate-700 transition duration-200 hover:translate-x-1 hover:text-[#075E54] active:bg-[#F4F9F7]"
            >
              Store Pickup
            </Link>
          </div>
        </nav>
      )}
    </header>
  );
}
