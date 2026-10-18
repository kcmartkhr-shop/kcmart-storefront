"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import {
  CheckCircle2,
  MapPin,
  PackageCheck,
  Store,
} from "lucide-react";

export default function OrderConfirmationPage() {
  const searchParams = useSearchParams();

  const orderNumber =
    searchParams.get("order")?.trim() ?? "";

  const hasOrderNumber =
    /^KCM-[A-Za-z0-9-]+$/.test(orderNumber);

  return (
    <main className="min-h-[70vh] bg-slate-50">
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:py-16">

        <section className="overflow-hidden rounded-[28px] border border-slate-100 bg-white shadow-sm">

          <div className="px-5 py-8 text-center sm:px-10 sm:py-10">

            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
              <CheckCircle2
                size={34}
                className="text-green-700"
              />
            </div>

            <p className="mt-5 text-xs font-extrabold uppercase tracking-[0.18em] text-green-700">
              Order confirmed
            </p>

            <h1 className="mt-2 text-2xl font-black text-slate-950 sm:text-3xl">
              Thank you for your order
            </h1>

            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-600">
              Your Store Pickup order has been received by
              KCMART. Payment will be made at the store when
              you collect your order.
            </p>

            {hasOrderNumber ? (
              <div className="mx-auto mt-7 max-w-md rounded-2xl border border-[#D9ECE7] bg-[#EAF5F2] p-5">

                <p className="text-xs font-bold uppercase tracking-wide text-[#075E54]">
                  KCMART Order Number
                </p>

                <p className="mt-2 break-all text-lg font-black text-slate-950">
                  {orderNumber}
                </p>

                <p className="mt-2 text-xs leading-5 text-slate-600">
                  Keep this order number for reference when
                  contacting or visiting the store.
                </p>

              </div>
            ) : (
              <div className="mx-auto mt-7 max-w-md rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                Order confirmation number is unavailable.
              </div>
            )}

          </div>

          <div className="border-t border-slate-100 bg-slate-50/70 px-5 py-6 sm:px-10">

            <div className="grid gap-4 sm:grid-cols-2">

              <div className="rounded-2xl border border-slate-200 bg-white p-4">

                <div className="flex items-center gap-3">

                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#EAF5F2]">
                    <Store
                      size={20}
                      className="text-[#075E54]"
                    />
                  </div>

                  <div>
                    <p className="font-black text-slate-950">
                      Store Pickup
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      Collect from Khariar Computer Service
                    </p>
                  </div>

                </div>

              </div>

              <div className="rounded-2xl border border-slate-200 bg-white p-4">

                <div className="flex items-center gap-3">

                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-green-50">
                    <PackageCheck
                      size={20}
                      className="text-green-700"
                    />
                  </div>

                  <div>
                    <p className="font-black text-slate-950">
                      Pay at Store
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      No online payment was charged
                    </p>
                  </div>

                </div>

              </div>

            </div>

            <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-5">

              <div className="flex items-start gap-3">

                <MapPin
                  size={21}
                  className="mt-0.5 shrink-0 text-[#075E54]"
                />

                <div>
                  <p className="font-black text-slate-950">
                    Khariar Computer Service
                  </p>

                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    College Road, Near Koshaleswar Temple
                    <br />
                    Raj Khariar, Gadramunda
                    <br />
                    Odisha - 766107
                  </p>

                  <p className="mt-2 text-sm font-bold text-slate-800">
                    Mobile: 8637277240
                  </p>

                </div>

              </div>

            </div>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row">

              <Link
                href="/"
                className="flex min-h-12 flex-1 items-center justify-center rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46]"
              >
                Continue Shopping
              </Link>

              <Link
                href="/account"
                className="flex min-h-12 flex-1 items-center justify-center rounded-xl border border-slate-300 bg-white px-5 text-sm font-extrabold text-slate-800 transition hover:bg-slate-50"
              >
                My Account
              </Link>

            </div>

          </div>

        </section>

      </div>
    </main>
  );
}