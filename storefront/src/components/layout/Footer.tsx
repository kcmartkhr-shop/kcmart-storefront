import Link from "next/link";
import {
  MapPin,
  Truck,
  Store,
  ShieldCheck
} from "lucide-react";

export default function Footer() {
  return (
    <footer className="mt-16 border-t border-slate-200 bg-white">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-14 md:grid-cols-4 lg:px-6">
        <div className="md:col-span-2">
          <Link
            href="/"
            className="inline-block text-3xl font-black tracking-tight text-[#075E54] transition duration-300 hover:scale-[1.03]"
          >
            KC<span className="text-slate-900">MART</span>
          </Link>

          <p className="mt-4 max-w-md text-sm leading-7 text-slate-500">
            Your destination for computers, printers, IT components
            and essential accessories. Operated by Khariar Computer
            Service in Khariar, Odisha.
          </p>

          <div className="mt-5 flex items-center gap-2 text-sm text-slate-600">
            <MapPin size={17} />
            Khariar, Odisha, India
          </div>
        </div>

        <div>
          <h3 className="mb-5 text-sm font-black text-slate-900">
            Quick Links
          </h3>

          <div className="flex flex-col gap-3 text-sm text-slate-600">
            <Link className="w-fit transition hover:translate-x-1 hover:text-[#075E54]" href="/categories">All Categories</Link>
            <Link className="w-fit transition hover:translate-x-1 hover:text-[#075E54]" href="/category/laptop">Laptops</Link>
            <Link className="w-fit transition hover:translate-x-1 hover:text-[#075E54]" href="/category/printers">Printers</Link>
            <Link className="w-fit transition hover:translate-x-1 hover:text-[#075E54]" href="/store">Our Store</Link>
          </div>
        </div>

        <div>
          <h3 className="mb-5 text-sm font-black text-slate-900">
            Shopping Information
          </h3>

          <div className="flex flex-col gap-4 text-sm text-slate-600">
            <span className="flex items-center gap-2">
              <Truck size={18} />
              Manually arranged delivery
            </span>

            <span className="flex items-center gap-2">
              <Store size={18} />
              Store pickup
            </span>

            <span className="flex items-center gap-2">
              <ShieldCheck size={18} />
              Product assistance
            </span>
          </div>
        </div>
      </div>

      <div className="border-t border-slate-100 px-5 py-5 text-center text-xs text-slate-500">
        © {new Date().getFullYear()} KCMART • Khariar Computer Service.
        All rights reserved.
      </div>
    </footer>
  );
}
