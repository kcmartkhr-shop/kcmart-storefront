import { MapPin, Store, Truck } from "lucide-react";

export default function StorePage() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-12">
      <h1 className="text-3xl font-black">
        Visit Our Store
      </h1>

      <div className="mt-8 rounded-3xl border border-slate-100 bg-white p-8 shadow-sm">
        <Store size={42} className="text-[#075E54]" />

        <h2 className="mt-5 text-2xl font-black">
          Khariar Computer Service
        </h2>

        <p className="mt-3 flex items-center gap-2 text-slate-600">
          <MapPin size={19} />
          Khariar, Odisha, India
        </p>

        <p className="mt-6 text-sm leading-7 text-slate-600">
          KCMART is the online storefront of Khariar Computer
          Service. Our website will support store pickup and
          manually arranged delivery across India.
        </p>

        <div className="mt-6 flex items-center gap-2 text-sm font-semibold text-[#075E54]">
          <Truck size={20} />
          Shipping and pickup details will be confirmed during checkout.
        </div>
      </div>
    </div>
  );
}
