import Link from "next/link";
import {
  House,
  LayoutGrid,
  ShoppingCart,
  UserRound
} from "lucide-react";

const links = [
  { label: "Home", href: "/", icon: House },
  { label: "Categories", href: "/categories", icon: LayoutGrid },
  { label: "Cart", href: "/cart", icon: ShoppingCart },
  { label: "Account", href: "/account", icon: UserRound }
];

export default function MobileNav() {
  return (
    <nav
      aria-label="Mobile navigation"
      className="fixed bottom-0 left-0 right-0 z-40 grid grid-cols-4 border-t border-slate-200 bg-white pb-[env(safe-area-inset-bottom)] shadow-lg md:hidden"
    >
      {links.map(({ label, href, icon: Icon }) => (
        <Link
          key={label}
          href={href}
          className="group flex min-h-16 flex-col items-center justify-center gap-1 text-slate-600 transition duration-200 hover:bg-[#F4F9F7] hover:text-[#075E54] active:scale-95 active:bg-[#EAF5F2]"
        >
          <Icon size={21} strokeWidth={1.8} className="transition duration-200 group-hover:-translate-y-0.5 group-hover:scale-110" />
          <span className="text-[11px] font-semibold">
            {label}
          </span>
        </Link>
      ))}
    </nav>
  );
}
