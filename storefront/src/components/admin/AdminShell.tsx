"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Truck,
  Boxes,
  ExternalLink,
  FolderTree,
  LayoutDashboard,
  LogOut,
  Menu,
  PackageSearch,
  ShoppingBag,
  Store,
  Users,
  X,
} from "lucide-react";
import { useState } from "react";

const navigation = [
  {
    label: "Dashboard",
    href: "/admin",
    icon: LayoutDashboard,
  },
  {
    label: "Products",
    href: "/admin/products",
    icon: PackageSearch,
  },
  {
    label: "Inventory",
    href: "/admin/inventory",
    icon: Boxes,
  },
  {
    label: "Categories",
    href: "/admin/categories",
    icon: FolderTree,
  },
  {
    label: "Orders",
    href: "/admin/orders",
    icon: ShoppingBag,
  },
  {
    label: "Delivery Management",
    href: "/admin/delivery",
    icon: Truck,
  },
  {
    label: "Customers",
    href: "/admin/customers",
    icon: Users,
  },
];

export default function AdminShell({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);

  if (pathname === "/admin/login") {
    return <>{children}</>;
  }

  async function logout() {
    await fetch("/api/admin/logout", {
      method: "POST",
    });

    router.replace("/admin/login");
    router.refresh();
  }

  function isActive(href: string) {
    if (href === "/admin") {
      return pathname === "/admin";
    }

    return pathname.startsWith(href);
  }

  const sidebar = (
    <div className="flex h-full flex-col border-r border-[#E5E7EB] bg-white text-[#111827]">
      <div className="border-b border-[#E5E7EB] px-6 py-6">
        <Link
          href="/admin"
          onClick={() => setMobileOpen(false)}
          className="block"
        >
          <p className="text-2xl font-black tracking-[-0.06em]">
            <span className="text-[#075E54]">KC</span>
            <span className="text-[#111827]">MART</span>
          </p>

          <p className="mt-1 text-xs font-bold uppercase tracking-[0.22em] text-[#6B7280]">
            Administration
          </p>
        </Link>
      </div>

      <nav className="flex-1 space-y-1.5 overflow-y-auto px-3 py-5">
        {navigation.map((item) => {
          const Icon = item.icon;
          const active = isActive(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={() => setMobileOpen(false)}
              className={[
                "flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition",
                active
                  ? "bg-[#EAF5F2] text-[#075E54] shadow-sm ring-1 ring-[#DDEBE7]"
                  : "text-[#4B5563] hover:bg-[#F4F9F7] hover:text-[#075E54]",
              ].join(" ")}
            >
              <Icon size={19} />
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-[#E5E7EB] p-3">
        <Link
          href="/"
          target="_blank"
          className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-[#4B5563] transition duration-200 hover:bg-[#F4F9F7] hover:text-[#075E54] active:scale-[0.98]"
        >
          <ExternalLink size={18} />
          View Store
        </Link>

        <button
          type="button"
          onClick={logout}
          className="mt-1 flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-[#4B5563] transition duration-200 hover:bg-red-50 hover:text-red-600 active:scale-[0.98]"
        >
          <LogOut size={18} />
          Log out
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-[#F7FAF9] text-[#111827]">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 bg-white shadow-[4px_0_24px_rgba(15,23,42,0.035)] lg:block">
        {sidebar}
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            aria-label="Close admin navigation"
            onClick={() => setMobileOpen(false)}
            className="absolute inset-0 bg-black/40"
          />

          <aside className="relative h-full w-72 max-w-[85vw] shadow-2xl">
            {sidebar}
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 border-b border-[#E5E7EB] bg-white/95 shadow-[0_1px_12px_rgba(15,23,42,0.035)] backdrop-blur">
          <div className="flex h-16 items-center justify-between px-4 sm:px-6 lg:px-8">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setMobileOpen(true)}
                className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-[#DDEBE7] text-[#075E54] transition hover:bg-[#EAF5F2] active:scale-95 lg:hidden"
                aria-label="Open admin navigation"
              >
                <Menu size={21} />
              </button>

              <div>
                <p className="text-sm font-bold text-[#075E54]">
                  KCMART Admin
                </p>

                <p className="hidden text-xs text-[#6B7280] sm:block">
                  Store management control center
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 rounded-full border border-[#DDEBE7] bg-[#F4F9F7] px-3 py-2 text-xs font-bold text-[#075E54]">
              <Store size={15} />
              Khariar Computer Service
            </div>
          </div>
        </header>

        <main>{children}</main>
      </div>

      {mobileOpen && (
        <button
          type="button"
          onClick={() => setMobileOpen(false)}
          aria-label="Close navigation"
          className="fixed right-4 top-4 z-[60] flex h-10 w-10 items-center justify-center rounded-full bg-white text-[#064E46] shadow-lg lg:hidden"
        >
          <X size={20} />
        </button>
      )}
    </div>
  );
}