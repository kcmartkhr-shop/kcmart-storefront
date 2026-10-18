"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useSession } from "next-auth/react";
import {
  Minus,
  Plus,
  Trash2,
  ShoppingBag,
  ArrowRight,
  Package,
  ShieldCheck,
  LoaderCircle,
  AlertCircle,
} from "lucide-react";

import { useCart } from "@/components/cart/CartProvider";
import { useCustomerLogin } from "@/components/auth/CustomerLoginProvider";

const money = (value: number) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);

function imageSource(url: string) {
  return url.startsWith("/")
    ? url.replace(
        "/api/v1/product-images/",
        "/api/product-images/"
      )
    : url;
}

type CheckoutErrorResponse = {
  error?: string;
};

export default function CartPage() {
  const {
    items,
    itemCount,
    subtotal,
    ready,
    updateQuantity,
    removeItem,
    clearCart,
  } = useCart();

  const { openLogin } = useCustomerLogin();
  const { status } = useSession();
  const router = useRouter();

  const [validating, setValidating] =
    useState(false);

  const [checkoutError, setCheckoutError] =
    useState<string | null>(null);

  function handleCheckout() {
    if (
      status === "loading" ||
      validating
    ) {
      return;
    }

    setCheckoutError(null);

    if (status !== "authenticated") {
      openLogin("checkout");
      return;
    }

    if (items.length === 0) {
      setCheckoutError(
        "Your cart is empty."
      );
      return;
    }

    /*
     * Do not perform fulfillment-specific stock
     * validation here.
     *
     * The cart does not yet know whether the
     * customer wants DELIVERY or STORE_PICKUP.
     *
     * /checkout performs fresh server validation
     * using the customer's selected fulfillment
     * type and therefore the correct stock pool.
     */
    router.push("/checkout");
  }
  if (!ready) {
    return (
      <main className="mx-auto max-w-7xl px-4 py-8">
        <div className="h-8 w-44 animate-pulse rounded-xl bg-slate-200" />

        <div className="mt-7 grid gap-4 lg:grid-cols-[1fr_350px]">
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div
                key={i}
                className="h-36 animate-pulse rounded-2xl bg-slate-200"
              />
            ))}
          </div>

          <div className="h-56 animate-pulse rounded-2xl bg-slate-200" />
        </div>
      </main>
    );
  }

  if (items.length === 0) {
    return (
      <main className="flex min-h-[65vh] flex-col items-center justify-center bg-[#F7FAF9] px-4 pb-24 text-center">
        <div className="flex h-24 w-24 items-center justify-center rounded-full bg-[#D9ECE7]">
          <ShoppingBag
            size={45}
            className="text-[#075E54]"
          />
        </div>

        <h1 className="mt-6 text-2xl font-black text-slate-900">
          Your cart is empty
        </h1>

        <p className="mt-3 max-w-xs text-sm leading-6 text-slate-500">
          Explore KCMART and add your favourite
          products.
        </p>

        <Link
          href="/categories"
          className="mt-7 inline-flex min-h-12 items-center gap-2 rounded-xl bg-[#075E54] px-7 font-bold text-white"
        >
          Continue Shopping
          <ArrowRight size={18} />
        </Link>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#F7FAF9] pb-44 md:pb-8">
      <div className="mx-auto max-w-7xl px-3 py-5 sm:px-6 sm:py-10">

        <div className="mb-5 flex items-start justify-between gap-3 sm:mb-9">
          <div>
            <h1 className="text-2xl font-black text-slate-900 sm:text-3xl">
              My Cart
            </h1>

            <p className="mt-1 text-sm text-slate-500">
              {itemCount} item
              {itemCount === 1 ? "" : "s"} in your
              cart
            </p>
          </div>

          <button
            type="button"
            onClick={clearCart}
            disabled={validating}
            className="min-h-11 rounded-xl px-3 text-xs font-bold text-red-600 transition active:scale-95 disabled:opacity-50"
          >
            Clear Cart
          </button>
        </div>

        {checkoutError && (
          <div className="mb-5 flex items-start gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
            <AlertCircle
              size={20}
              className="mt-0.5 shrink-0"
            />

            <div>
              <p className="font-extrabold">
                Checkout needs attention
              </p>

              <p className="mt-1 leading-5">
                {checkoutError}
              </p>
            </div>
          </div>
        )}

        <div className="grid items-start gap-5 lg:grid-cols-[1fr_350px] lg:gap-7">

          <section className="space-y-3">
            {items.map((item) => (
              <article
                key={item.id}
                className="flex gap-3 rounded-[20px] border border-slate-100 bg-white p-3 shadow-sm sm:gap-5 sm:p-5"
              >
                <Link
                  href={`/product/${item.slug}`}
                  className="flex h-28 w-28 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-[#F4F7FC] sm:h-36 sm:w-36"
                >
                  {item.imageUrl ? (
                    <img
                      src={imageSource(
                        item.imageUrl
                      )}
                      alt={item.name}
                      className="h-full w-full object-contain p-2"
                    />
                  ) : (
                    <Package
                      size={45}
                      className="text-slate-300"
                    />
                  )}
                </Link>

                <div className="min-w-0 flex-1">

                  <Link
                    href={`/product/${item.slug}`}
                    className="line-clamp-2 text-sm font-bold leading-5 text-slate-900 hover:text-[#075E54] sm:text-base"
                  >
                    {item.name}
                  </Link>

                  <p className="mt-2 text-lg font-black text-slate-900">
                    {money(item.price)}
                  </p>

                  <div className="mt-3 flex flex-wrap items-center justify-between gap-2">

                    <div className="inline-flex items-center rounded-xl border border-slate-200 bg-slate-50">

                      <button
                        type="button"
                        aria-label={`Decrease quantity of ${item.name}`}
                        disabled={
                          item.quantity <= 1 ||
                          validating
                        }
                        onClick={() =>
                          updateQuantity(
                            item.id,
                            item.quantity - 1
                          )
                        }
                        className="flex h-11 w-11 items-center justify-center disabled:opacity-30"
                      >
                        <Minus size={16} />
                      </button>

                      <span className="min-w-7 text-center text-sm font-extrabold">
                        {item.quantity}
                      </span>

                      <button
                        type="button"
                        aria-label={`Increase quantity of ${item.name}`}
                        disabled={
                          item.quantity >= 10 ||
                          validating
                        }
                        onClick={() =>
                          updateQuantity(
                            item.id,
                            item.quantity + 1
                          )
                        }
                        className="flex h-11 w-11 items-center justify-center disabled:opacity-30"
                      >
                        <Plus size={16} />
                      </button>
                    </div>

                    <button
                      type="button"
                      aria-label={`Remove ${item.name}`}
                      disabled={validating}
                      onClick={() =>
                        removeItem(item.id)
                      }
                      className="flex min-h-11 items-center gap-1 text-xs font-bold text-red-600 disabled:opacity-50"
                    >
                      <Trash2 size={16} />
                      Remove
                    </button>

                  </div>

                  <p className="mt-2 text-xs text-slate-500">
                    Item total:{" "}
                    <span className="font-bold text-slate-800">
                      {money(
                        item.price *
                          item.quantity
                      )}
                    </span>
                  </p>

                </div>
              </article>
            ))}

            <Link
              href="/categories"
              className="inline-flex min-h-11 items-center gap-2 px-2 text-sm font-bold text-[#075E54]"
            >
              Continue Shopping
              <ArrowRight size={17} />
            </Link>
          </section>

          <aside className="rounded-[22px] border border-slate-100 bg-white p-5 shadow-sm lg:sticky lg:top-32">

            <h2 className="text-xl font-black text-slate-900">
              Order Summary
            </h2>

            <div className="mt-6 flex justify-between gap-2 text-sm text-slate-600">
              <span>
                Subtotal ({itemCount} items)
              </span>

              <span className="font-bold text-slate-900">
                {money(subtotal)}
              </span>
            </div>

            <div className="mt-5 flex justify-between border-t border-slate-100 pt-5 text-lg font-black">
              <span>Estimated total</span>
              <span>{money(subtotal)}</span>
            </div>

            <p className="mt-4 text-xs leading-5 text-slate-500">
              Cart prices are estimates. KCMART
              securely verifies current price and
              availability before checkout.
            </p>

            <button
              type="button"
              onClick={handleCheckout}
              disabled={
                validating ||
                status === "loading"
              }
              className="mt-6 hidden w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-6 py-4 font-bold text-white transition hover:bg-[#064E46] disabled:cursor-not-allowed disabled:bg-[#5A9F90] md:flex"
            >
              {validating ? (
                <>
                  <LoaderCircle
                    size={19}
                    className="animate-spin"
                  />
                  Verifying cart...
                </>
              ) : (
                <>
                  Proceed to Checkout
                  <ArrowRight size={18} />
                </>
              )}
            </button>

            <p className="mt-3 hidden text-center text-xs text-slate-500 md:block">
              {status === "authenticated"
                ? "Signed in. Price and stock will be verified securely."
                : "Sign in securely to continue."}
            </p>

            <div className="mt-5 flex items-start gap-2 border-t border-slate-100 pt-4 text-xs text-slate-500">
              <ShieldCheck
                size={17}
                className="shrink-0 text-green-700"
              />

              <span>
                Your cart is saved in this browser.
              </span>
            </div>

          </aside>
        </div>
      </div>

      <div
        className="fixed inset-x-0 z-40 border-t border-slate-200 bg-white/95 px-4 py-3 shadow-[0_-8px_25px_rgba(15,23,42,0.08)] backdrop-blur-md md:hidden"
        style={{
          bottom:
            "calc(4rem + env(safe-area-inset-bottom))",
        }}
      >
        <div className="mx-auto flex max-w-xl items-center justify-between gap-3">

          <div>
            <p className="text-xs text-slate-500">
              Estimated subtotal
            </p>

            <p className="text-xl font-black text-slate-900">
              {money(subtotal)}
            </p>
          </div>

          <button
            type="button"
            onClick={handleCheckout}
            disabled={
              validating ||
              status === "loading"
            }
            className="flex min-h-12 items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 text-center text-sm font-extrabold text-white disabled:bg-[#5A9F90]"
          >
            {validating ? (
              <>
                <LoaderCircle
                  size={17}
                  className="animate-spin"
                />
                Verifying...
              </>
            ) : (
              "Proceed to Checkout"
            )}
          </button>

        </div>
      </div>
    </main>
  );
}