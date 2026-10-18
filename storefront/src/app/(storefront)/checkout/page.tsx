"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  ArrowLeft,
  CheckCircle2,
  LoaderCircle,
  Package,
  ShieldCheck,
  AlertCircle,
  Truck,
  Store,
  MapPin,
  Phone,
  ReceiptText,
  LockKeyhole,
} from "lucide-react";

import { useCart } from "@/components/cart/CartProvider";
import { useCustomerLogin } from "@/components/auth/CustomerLoginProvider";

type ValidatedItem = {
  productId: number;
  slug: string;
  name: string;
  sku: string | null;
  imageUrl: string | null;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableOnlineStock: number;
};

type ValidatedCheckout = {
  items: ValidatedItem[];
  itemCount: number;
  subtotal: number;
};

type ErrorResponse = {
  error?: string;
};

type RazorpayOrder = {
  orderId: number;
  razorpayOrderId: string;
  keyId: string;
  amountPaise: number;
  currency: string;
  name: string;
  description: string;
};

type RazorpaySuccessResponse = {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
};

type RazorpayCheckoutOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (
    response: RazorpaySuccessResponse
  ) => void | Promise<void>;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  theme?: {
    color?: string;
  };
  modal?: {
    ondismiss?: () => void;
  };
};

type RazorpayInstance = {
  open: () => void;
  on: (
    event: string,
    handler: (response: unknown) => void
  ) => void;
};

declare global {
  interface Window {
    Razorpay?: new (
      options: RazorpayCheckoutOptions
    ) => RazorpayInstance;
  }
}

type FulfillmentType =
  | "DELIVERY"
  | "STORE_PICKUP";

type PaymentMethod =
  | "ONLINE"
  | "PAY_AT_STORE";

type CheckoutDetails = {
  fullName: string;
  phone: string;
  addressLine1: string;
  addressLine2: string;
  landmark: string;
  city: string;
  state: string;
  postalCode: string;
};

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

const EMPTY_DETAILS: CheckoutDetails = {
  fullName: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  landmark: "",
  city: "",
  state: "",
  postalCode: "",
};

export default function CheckoutPage() {
  const { items, ready, clearCart } = useCart();
  const { data: session, status } =
    useSession();

  const { openLogin } =
    useCustomerLogin();

  const router = useRouter();

  const [validated, setValidated] =
    useState<ValidatedCheckout | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState<string | null>(null);

  const [fulfillment, setFulfillment] =
    useState<FulfillmentType>("DELIVERY");

  const [details, setDetails] =
    useState<CheckoutDetails>(
      EMPTY_DETAILS
    );

  const [detailsAccepted, setDetailsAccepted] =
    useState(false);

  const [formError, setFormError] =
    useState<string | null>(null);

  const [paymentMethod, setPaymentMethod] =
    useState<PaymentMethod>("ONLINE");

  const [paymentAccepted, setPaymentAccepted] =
    useState(false);

  const [paymentLoading, setPaymentLoading] =
    useState(false);

  const [paymentError, setPaymentError] =
    useState<string | null>(null);

  const [orderLoading, setOrderLoading] =
    useState(false);

  const [orderError, setOrderError] =
    useState<string | null>(null);

  const [orderAttemptKey, setOrderAttemptKey] =
    useState<string | null>(null);

  const [orderCompleted, setOrderCompleted] =
    useState(false);

  /*
   * Synchronous success guard.
   *
   * React state updates are asynchronous. After the backend confirms
   * an order, clearCart() changes `items` immediately enough to
   * retrigger checkout effects before `orderCompleted` necessarily
   * appears in the next render.
   *
   * This ref becomes true synchronously before the cart is cleared.
   */
  const orderCompletedRef = useRef(false);

  useEffect(() => {
    if (
      status === "authenticated" &&
      session?.user?.name
    ) {
      setDetails((current) => {
        if (current.fullName.trim()) {
          return current;
        }

        return {
          ...current,
          fullName:
            session.user?.name ?? "",
        };
      });
    }
  }, [
    session?.user?.name,
    status,
  ]);

  useEffect(() => {
    if (
      !ready ||
      status === "loading"
    ) {
      return;
    }

    if (status !== "authenticated") {
      setLoading(false);
      openLogin("checkout");
      return;
    }

    if (items.length === 0) {
      setLoading(false);

      if (!orderCompletedRef.current) {
        router.replace("/cart");
      }

      return;
    }

    let cancelled = false;

    async function validateCart() {
      setLoading(true);
      setError(null);

      try {
        /*
         * SECURITY:
         * Fresh server validation.
         *
         * Only product ID + quantity are sent.
         * Browser price/subtotal/customerId are
         * not authoritative.
         */
        const response = await fetch(
          "/api/checkout/validate",
          {
            method: "POST",
            headers: {
              "Content-Type":
                "application/json",
            },
            body: JSON.stringify({
              fulfillmentType: fulfillment,
              items: items.map((item) => ({
                productId: item.id,
                quantity: item.quantity,
              })),
            }),
          }
        );

        let body:
          | ValidatedCheckout
          | ErrorResponse;

        try {
          body = await response.json();
        } catch {
          body = {};
        }

        if (cancelled) {
          return;
        }

        if (response.status === 401) {
          setValidated(null);
          setLoading(false);
          openLogin("checkout");
          return;
        }

        if (!response.ok) {
          const failure =
            body as ErrorResponse;

          setValidated(null);

          setError(
            failure.error ||
              "Your cart could not be verified."
          );

          return;
        }

        setValidated(
          body as ValidatedCheckout
        );
      } catch {
        if (!cancelled) {
          setValidated(null);

          setError(
            "Checkout service is temporarily unavailable."
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void validateCart();

    return () => {
      cancelled = true;
    };
  }, [
    items,
    ready,
    status,
    fulfillment,
    openLogin,
    orderCompleted,
    router,
  ]);

  function updateDetail(
    field: keyof CheckoutDetails,
    value: string
  ) {
    setDetails((current) => ({
      ...current,
      [field]: value,
    }));

    setDetailsAccepted(false);

    setPaymentAccepted(false);
    setPaymentError(null);

    setFormError(null);

    setOrderAttemptKey(null);
    setOrderError(null);
  }

  function chooseFulfillment(
    type: FulfillmentType
  ) {
    setFulfillment(type);

    setDetailsAccepted(false);

    setPaymentMethod("ONLINE");
    setPaymentAccepted(false);
    setPaymentError(null);

    setFormError(null);

    setOrderAttemptKey(null);
    setOrderError(null);
  }

  function validateDetails() {
    const name =
      details.fullName.trim();

    const phone =
      details.phone
        .replace(/\s+/g, "")
        .replace(/^\+91/, "");

    if (name.length < 2) {
      return "Please enter the customer's full name.";
    }

    if (!/^[6-9]\d{9}$/.test(phone)) {
      return "Enter a valid 10-digit Indian mobile number.";
    }

    if (fulfillment === "DELIVERY") {
      if (
        details.addressLine1.trim().length < 5
      ) {
        return "Please enter the delivery address.";
      }

      if (
        details.city.trim().length < 2
      ) {
        return "Please enter the city.";
      }

      if (
        details.state.trim().length < 2
      ) {
        return "Please enter the state.";
      }

      if (
        !/^\d{6}$/.test(
          details.postalCode.trim()
        )
      ) {
        return "Enter a valid 6-digit PIN code.";
      }
    }

    return null;
  }

  async function handleDetailsContinue(
    event: FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    const validationError =
      validateDetails();

    if (validationError) {
      setDetailsAccepted(false);
      setFormError(validationError);
      return;
    }

    setDetailsAccepted(false);
    setFormError(null);

    const phone =
      details.phone
        .replace(/\s+/g, "")
        .replace(/^\+91/, "");

    try {
      const response = await fetch(
        "/api/checkout/details",
        {
          method: "POST",
          headers: {
            "Content-Type":
              "application/json",
          },
          body: JSON.stringify({
            fulfillmentType: fulfillment,
            customerName:
              details.fullName.trim(),
            customerPhone: phone,

            addressLine1:
              fulfillment === "DELIVERY"
                ? details.addressLine1.trim()
                : null,

            addressLine2:
              fulfillment === "DELIVERY"
                ? details.addressLine2.trim()
                : null,

            landmark:
              fulfillment === "DELIVERY"
                ? details.landmark.trim()
                : null,

            city:
              fulfillment === "DELIVERY"
                ? details.city.trim()
                : null,

            state:
              fulfillment === "DELIVERY"
                ? details.state.trim()
                : null,

            postalCode:
              fulfillment === "DELIVERY"
                ? details.postalCode.trim()
                : null,

            country:
              fulfillment === "DELIVERY"
                ? "India"
                : null,
          }),
        }
      );

      let body:
        | {
            valid?: boolean;
            error?: string;
          }
        | undefined;

      try {
        body = await response.json();
      }
      catch {
        body = undefined;
      }

      if (response.status === 401) {
        openLogin("checkout");
        return;
      }

      if (!response.ok || !body?.valid) {
        setFormError(
          body?.error ||
            "Your checkout details could not be verified."
        );
        return;
      }

      /*
       * Stage 5C-9B:
       * Spring Boot has now validated the fulfillment
       * and customer/contact/address details.
       *
       * Still intentionally NO:
       * - order creation
       * - stock deduction
       * - payment processing
       * - cart clearing
       */
      setDetailsAccepted(true);
    }
    catch {
      setFormError(
        "Checkout service is temporarily unavailable."
      );
    }
  }

  function loadRazorpayCheckout(): Promise<boolean> {
    return new Promise((resolve) => {
      if (window.Razorpay) {
        resolve(true);
        return;
      }

      const existingScript =
        document.querySelector<HTMLScriptElement>(
          'script[src="https://checkout.razorpay.com/v1/checkout.js"]'
        );

      if (existingScript) {
        existingScript.addEventListener(
          "load",
          () => resolve(Boolean(window.Razorpay)),
          { once: true }
        );

        existingScript.addEventListener(
          "error",
          () => resolve(false),
          { once: true }
        );

        return;
      }

      const script =
        document.createElement("script");

      script.src =
        "https://checkout.razorpay.com/v1/checkout.js";

      script.async = true;

      script.onload = () =>
        resolve(Boolean(window.Razorpay));

      script.onerror = () =>
        resolve(false);

      document.body.appendChild(script);
    });
  }

  async function handlePlaceOrder() {
    if (orderLoading) {
      return;
    }

    setOrderError(null);

    if (
      !ready ||
      !validated ||
      items.length === 0
    ) {
      setOrderError(
        "Your cart is not ready. Please review it and try again."
      );
      return;
    }

    const cartMatchesValidated =
      validated.items.length === items.length &&
      validated.items.every(
        (validatedItem) =>
          items.some(
            (cartItem) =>
              cartItem.id === validatedItem.productId &&
              cartItem.quantity === validatedItem.quantity
          )
      );

    if (!cartMatchesValidated) {
      setOrderError(
        "Your cart changed after validation. Please wait for checkout to refresh and try again."
      );
      return;
    }

    if (
      fulfillment === "DELIVERY" &&
      paymentMethod !== "ONLINE"
    ) {
      setOrderError(
        "Home delivery requires secure online payment."
      );
      return;
    }

    if (
      !detailsAccepted ||
      !paymentAccepted
    ) {
      setOrderError(
        "Please verify your customer details and payment method first."
      );
      return;
    }

    const normalizedName =
      details.fullName.trim();

    const normalizedPhone =
      details.phone
        .replace(/\s+/g, "")
        .replace(/^\+91/, "");

    if (
      normalizedName.length < 2 ||
      !/^[6-9][0-9]{9}$/.test(
        normalizedPhone
      )
    ) {
      setOrderError(
        "Please verify your name and mobile number."
      );
      return;
    }

    if (fulfillment === "DELIVERY") {
      if (
        details.addressLine1.trim().length < 5 ||
        details.city.trim().length < 2 ||
        details.state.trim().length < 2 ||
        !/^[1-9][0-9]{5}$/.test(
          details.postalCode.trim()
        )
      ) {
        setOrderError(
          "Please verify your complete delivery address."
        );
        return;
      }
    }

    const idempotencyKey =
      orderAttemptKey ??
      crypto.randomUUID();

    if (!orderAttemptKey) {
      setOrderAttemptKey(idempotencyKey);
    }

    setOrderLoading(true);

    try {
      const response =
        await fetch(
          "/api/checkout/order",
          {
            method: "POST",

            headers: {
              "Content-Type":
                "application/json",
            },

            body: JSON.stringify({
              idempotencyKey,

              fulfillmentType:
                fulfillment,

              paymentMethod,

              customerName:
                normalizedName,

              customerPhone:
                normalizedPhone,

              addressLine1:
                fulfillment === "DELIVERY"
                  ? details.addressLine1.trim()
                  : null,

              addressLine2:
                fulfillment === "DELIVERY"
                  ? details.addressLine2.trim()
                  : null,

              landmark:
                fulfillment === "DELIVERY"
                  ? details.landmark.trim()
                  : null,

              city:
                fulfillment === "DELIVERY"
                  ? details.city.trim()
                  : null,

              state:
                fulfillment === "DELIVERY"
                  ? details.state.trim()
                  : null,

              postalCode:
                fulfillment === "DELIVERY"
                  ? details.postalCode.trim()
                  : null,

              country:
                fulfillment === "DELIVERY"
                  ? "India"
                  : null,

              items: items.map(
                (item) => ({
                  productId: item.id,
                  quantity:
                    item.quantity,
                })
              ),
            }),
          }
        );

      const result: unknown =
        await response
          .json()
          .catch(() => null);

      if (response.status === 401) {
        setOrderError(
          "Your login session has expired. Please sign in again."
        );

        openLogin("checkout");
        return;
      }

      if (!response.ok) {
        let message =
          "Unable to place your order. Please try again.";

        if (
          typeof result === "object" &&
          result !== null &&
          "error" in result &&
          typeof (
            result as {
              error?: unknown;
            }
          ).error === "string"
        ) {
          message = (
            result as {
              error: string;
            }
          ).error;
        }

        setOrderError(message);
        return;
      }

      if (
        typeof result !== "object" ||
        result === null ||
        !("orderId" in result) ||
        !("orderNumber" in result) ||
        typeof (
          result as {
            orderId?: unknown;
          }
        ).orderId !== "number" ||
        typeof (
          result as {
            orderNumber?: unknown;
          }
        ).orderNumber !== "string"
      ) {
        setOrderError(
          "The order service returned an invalid confirmation. Please retry."
        );
        return;
      }

      const createdOrder =
        result as {
          orderId: number;
          orderNumber: string;
          paymentMethod?: string;
          paymentStatus?: string;
        };

      /*
       * STORE PICKUP + PAY_AT_STORE:
       * The order is complete from the checkout perspective.
       */
      if (
        fulfillment === "STORE_PICKUP" &&
        paymentMethod === "PAY_AT_STORE"
      ) {
        orderCompletedRef.current = true;
        setOrderCompleted(true);

        clearCart();

        router.replace(
          `/order-confirmation?order=${encodeURIComponent(
            createdOrder.orderNumber
          )}`
        );

        return;
      }

      /*
       * ONLINE PAYMENT:
       *
       * The order now exists as an unpaid order.
       * Do NOT clear the cart yet.
       *
       * Razorpay payment is created against this exact order.
       */
      if (
        paymentMethod !== "ONLINE"
      ) {
        setOrderError(
          "The selected payment method could not be processed."
        );
        return;
      }

      const checkoutLoaded =
        await loadRazorpayCheckout();

      if (
        !checkoutLoaded ||
        !window.Razorpay
      ) {
        setOrderError(
          "Unable to load the secure payment window. Your order has not been paid."
        );
        return;
      }

      const paymentResponse =
        await fetch(
          `/api/account/orders/${encodeURIComponent(
            String(createdOrder.orderId)
          )}/payment/razorpay`,
          {
            method: "POST",
            cache: "no-store",
          }
        );

      const paymentData =
        (await paymentResponse.json()) as {
          paymentOrder?: RazorpayOrder;
          error?: string;
        };

      if (
        !paymentResponse.ok ||
        !paymentData.paymentOrder
      ) {
        setOrderError(
          paymentData.error ||
            "Unable to start secure online payment. Your order remains unpaid."
        );
        return;
      }

      const paymentOrder =
        paymentData.paymentOrder;

      if (
        !paymentOrder.razorpayOrderId ||
        !paymentOrder.keyId ||
        !Number.isSafeInteger(
          paymentOrder.amountPaise
        ) ||
        paymentOrder.amountPaise <= 0 ||
        paymentOrder.currency !== "INR"
      ) {
        setOrderError(
          "Invalid payment order received. Your order remains unpaid."
        );
        return;
      }

      const options:
        RazorpayCheckoutOptions = {
        key:
          paymentOrder.keyId,

        amount:
          paymentOrder.amountPaise,

        currency:
          paymentOrder.currency,

        name: "KCMART",

        description:
          `Payment for ${createdOrder.orderNumber}`,

        order_id:
          paymentOrder.razorpayOrderId,

        prefill: {
          name:
            normalizedName,

          email:
            session?.user?.email ?? "",

          contact:
            normalizedPhone,
        },

        theme: {
          color: "#075E54",
        },

        modal: {
          ondismiss: () => {
            setOrderLoading(false);

            setOrderError(
              "Payment window closed. Your order remains unpaid. You can try payment again from My Orders."
            );
          },
        },

        handler: async (
          paymentResponse:
            RazorpaySuccessResponse
        ) => {
          try {
            const verifyResponse =
              await fetch(
                `/api/account/orders/${encodeURIComponent(
                  String(createdOrder.orderId)
                )}/payment/razorpay/verify`,
                {
                  method: "POST",

                  headers: {
                    "Content-Type":
                      "application/json",
                  },

                  body:
                    JSON.stringify(
                      paymentResponse
                    ),
                }
              );

            const verifyData =
              (await verifyResponse.json()) as {
                verified?: boolean;
                outcome?: string;
                error?: string;
              };

            if (
              verifyData.outcome ===
              "RECONCILIATION_REQUIRED"
            ) {
              setOrderError(
                "Payment received, but your order expired. Your payment is under review. Please do not pay again for this order."
              );

              setOrderLoading(false);
              return;
            }

            if (
              !verifyResponse.ok ||
              !verifyData.verified
            ) {
              throw new Error(
                verifyData.error ||
                  "Payment could not be verified."
              );
            }

            /*
             * Payment is now cryptographically verified
             * by the backend.
             *
             * Only now clear the browser cart.
             */
            orderCompletedRef.current =
              true;

            setOrderCompleted(true);

            clearCart();

            router.replace(
              `/order-confirmation?order=${encodeURIComponent(
                createdOrder.orderNumber
              )}`
            );
          } catch (
            verificationError
          ) {
            setOrderError(
              verificationError instanceof Error
                ? verificationError.message
                : "Payment verification failed. Your order remains unpaid."
            );

            setOrderLoading(false);
          }
        },
      };

      const razorpay =
        new window.Razorpay(
          options
        );

      razorpay.on(
        "payment.failed",
        () => {
          setOrderLoading(false);

          setOrderError(
            "Payment was not completed. Your order remains unpaid. You can try again from My Orders."
          );
        }
      );

      razorpay.open();

    } catch {
      setOrderError(
        "Unable to reach the order service. Please try again."
      );

      setOrderLoading(false);
    }
  }
  function choosePaymentMethod(
    method: PaymentMethod
  ) {
    if (!detailsAccepted) {
      return;
    }

    if (
      fulfillment === "DELIVERY" &&
      method !== "ONLINE"
    ) {
      return;
    }

    setPaymentMethod(method);
    setPaymentAccepted(false);
    setPaymentError(null);

    setOrderAttemptKey(null);
    setOrderError(null);
  }

  async function handlePaymentContinue() {
    if (
      !detailsAccepted ||
      paymentLoading
    ) {
      return;
    }

    /*
     * ONLINE is now supported by the real Razorpay
     * payment flow.
     *
     * This step verifies that the selected payment
     * method is valid. The actual payment happens
     * only after the order is created and Razorpay
     * Checkout is opened.
     */

    setPaymentLoading(true);
    setPaymentAccepted(false);
    setPaymentError(null);

    try {
      const response = await fetch(
        "/api/checkout/payment-method",
        {
          method: "POST",

          headers: {
            "Content-Type":
              "application/json",
          },

          body: JSON.stringify({
            fulfillmentType: fulfillment,
            paymentMethod,
          }),
        }
      );

      let body:
        | {
            valid?: boolean;
            paymentMethod?: string;
            error?: string;
          }
        | undefined;

      try {
        body = await response.json();
      } catch {
        body = undefined;
      }

      if (response.status === 401) {
        openLogin("checkout");
        return;
      }

      if (
        !response.ok ||
        !body?.valid
      ) {
        setPaymentError(
          body?.error ||
            "Payment method could not be verified."
        );

        return;
      }

      /*
       * Stage 5C-10 validates only
       * the payment method.
       *
       * No order creation.
       * No payment charge.
       * No stock deduction.
       * No cart clearing.
       */
      setPaymentAccepted(true);

    } catch {
      setPaymentError(
        "Checkout service is temporarily unavailable."
      );

    } finally {
      setPaymentLoading(false);
    }
  }

  if (
    !ready ||
    loading ||
    status === "loading"
  ) {
    return (
      <main className="min-h-screen bg-[#F7FAF9] px-4 py-10">
        <div className="mx-auto max-w-5xl">
          <div className="flex min-h-[55vh] flex-col items-center justify-center text-center">

            <LoaderCircle
              size={40}
              className="animate-spin text-[#075E54]"
            />

            <h1 className="mt-5 text-xl font-black text-slate-900">
              Securing your checkout
            </h1>

            <p className="mt-2 text-sm text-slate-500">
              Verifying current prices and stock
              with KCMART.
            </p>

          </div>
        </div>
      </main>
    );
  }

  if (status !== "authenticated") {
    return (
      <main className="min-h-[60vh] bg-[#F7FAF9] px-4 py-12">

        <div className="mx-auto max-w-lg rounded-3xl bg-white p-7 text-center shadow-sm">

          <ShieldCheck
            size={42}
            className="mx-auto text-[#075E54]"
          />

          <h1 className="mt-4 text-xl font-black text-slate-900">
            Sign in to continue
          </h1>

          <p className="mt-2 text-sm text-slate-500">
            Your cart remains saved in this browser.
          </p>

        </div>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#F7FAF9] pb-16">

      <div className="mx-auto max-w-6xl px-3 py-6 sm:px-6 sm:py-10">

        <Link
          href="/cart"
          className="inline-flex min-h-11 items-center gap-2 text-sm font-bold text-[#075E54]"
        >
          <ArrowLeft size={18} />
          Back to cart
        </Link>

        <div className="mt-3">

          <p className="text-xs font-extrabold uppercase tracking-[0.18em] text-[#075E54]">
            Secure checkout
          </p>

          <h1 className="mt-2 text-2xl font-black text-slate-950 sm:text-3xl">
            Complete your order
          </h1>

          <p className="mt-2 text-sm leading-6 text-slate-500">
            Review your verified cart and choose
            how you want to receive your order.
          </p>

        </div>

        {error && (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 p-5">

            <div className="flex items-start gap-3">

              <AlertCircle
                size={22}
                className="mt-0.5 shrink-0 text-red-700"
              />

              <div>
                <h2 className="font-black text-red-900">
                  Cart verification failed
                </h2>

                <p className="mt-1 text-sm leading-6 text-red-800">
                  {error}
                </p>
              </div>

            </div>

            <Link
              href="/cart"
              className="mt-5 inline-flex min-h-11 items-center rounded-xl bg-red-700 px-5 text-sm font-bold text-white"
            >
              Return to cart
            </Link>

          </div>
        )}

        {validated && (
          <div className="mt-7 grid items-start gap-6 lg:grid-cols-[1fr_350px]">

            <div className="space-y-5">

              {/* CART VERIFIED */}

              <section className="overflow-hidden rounded-[22px] border border-slate-100 bg-white shadow-sm">

                <div className="flex items-start gap-3 border-b border-green-100 bg-green-50 p-4">

                  <CheckCircle2
                    size={22}
                    className="mt-0.5 shrink-0 text-green-700"
                  />

                  <div>
                    <p className="font-extrabold text-green-900">
                      Cart verified
                    </p>

                    <p className="mt-1 text-xs leading-5 text-green-800">
                      Current prices and online
                      availability were checked
                      securely.
                    </p>
                  </div>

                </div>

                <div className="divide-y divide-slate-100">

                  {validated.items.map(
                    (item) => (
                      <article
                        key={item.productId}
                        className="flex gap-4 p-4"
                      >

                        <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-slate-50">

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
                              size={32}
                              className="text-slate-300"
                            />
                          )}

                        </div>

                        <div className="min-w-0 flex-1">

                          <p className="text-sm font-extrabold leading-5 text-slate-900">
                            {item.name}
                          </p>

                          {item.sku && (
                            <p className="mt-1 text-xs text-slate-400">
                              SKU: {item.sku}
                            </p>
                          )}

                          <div className="mt-2 flex items-center justify-between gap-3">

                            <p className="text-xs text-slate-500">
                              {money(
                                item.unitPrice
                              )} Ã—{" "}
                              {item.quantity}
                            </p>

                            <p className="text-sm font-black text-slate-900">
                              {money(
                                item.lineTotal
                              )}
                            </p>

                          </div>

                        </div>

                      </article>
                    )
                  )}

                </div>
              </section>


              {/* FULFILLMENT */}

              <section className="rounded-[22px] border border-slate-100 bg-white p-4 shadow-sm sm:p-6">

                <div>
                  <p className="text-xs font-black uppercase tracking-[0.16em] text-[#075E54]">
                    Step 2
                  </p>

                  <h2 className="mt-1 text-xl font-black text-slate-950">
                    How would you like to receive it?
                  </h2>
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2">

                  <button
                    type="button"
                    onClick={() =>
                      chooseFulfillment(
                        "DELIVERY"
                      )
                    }
                    className={`rounded-2xl border-2 p-4 text-left transition ${
                      fulfillment ===
                      "DELIVERY"
                        ? "border-[#075E54] bg-[#EAF5F2]"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    }`}
                  >
                    <div className="flex items-start gap-3">

                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white shadow-sm">
                        <Truck
                          size={22}
                          className="text-[#075E54]"
                        />
                      </div>

                      <div>
                        <p className="font-black text-slate-950">
                          Home Delivery
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                          Deliver to your address
                          anywhere serviceable.
                        </p>
                      </div>

                    </div>
                  </button>

                  <button
                    type="button"
                    onClick={() =>
                      chooseFulfillment(
                        "STORE_PICKUP"
                      )
                    }
                    className={`rounded-2xl border-2 p-4 text-left transition ${
                      fulfillment ===
                      "STORE_PICKUP"
                        ? "border-[#075E54] bg-[#EAF5F2]"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    }`}
                  >
                    <div className="flex items-start gap-3">

                      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white shadow-sm">
                        <Store
                          size={22}
                          className="text-[#075E54]"
                        />
                      </div>

                      <div>
                        <p className="font-black text-slate-950">
                          Store Pickup
                        </p>

                        <p className="mt-1 text-xs leading-5 text-slate-500">
                          Collect from Khariar
                          Computer Service.
                        </p>
                      </div>

                    </div>
                  </button>

                </div>
              </section>


              {/* CUSTOMER DETAILS */}

              <form
                onSubmit={
                  handleDetailsContinue
                }
                className="rounded-[22px] border border-slate-100 bg-white p-4 shadow-sm sm:p-6"
              >

                <div>
                  <p className="text-xs font-black uppercase tracking-[0.16em] text-[#075E54]">
                    Step 3
                  </p>

                  <h2 className="mt-1 text-xl font-black text-slate-950">
                    {fulfillment ===
                    "DELIVERY"
                      ? "Delivery details"
                      : "Pickup details"}
                  </h2>

                  <p className="mt-1 text-sm leading-6 text-slate-500">
                    We&apos;ll use these details
                    for this order.
                  </p>
                </div>

                <div className="mt-5 grid gap-4 sm:grid-cols-2">

                  <label className="sm:col-span-2">
                    <span className="mb-1.5 block text-sm font-bold text-slate-700">
                      Full name *
                    </span>

                    <input
                      type="text"
                      autoComplete="name"
                      maxLength={255}
                      value={
                        details.fullName
                      }
                      onChange={(event) =>
                        updateDetail(
                          "fullName",
                          event.target.value
                        )
                      }
                      placeholder="Customer full name"
                      className="min-h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                    />
                  </label>

                  <label className="sm:col-span-2">
                    <span className="mb-1.5 block text-sm font-bold text-slate-700">
                      Mobile number *
                    </span>

                    <div className="flex min-h-12 overflow-hidden rounded-xl border border-slate-200 focus-within:border-[#075E54] focus-within:ring-2 focus-within:ring-[#D9ECE7]">

                      <span className="flex items-center border-r border-slate-200 bg-slate-50 px-3 text-sm font-bold text-slate-600">
                        +91
                      </span>

                      <input
                        type="tel"
                        inputMode="numeric"
                        autoComplete="tel"
                        maxLength={10}
                        value={
                          details.phone
                        }
                        onChange={(event) =>
                          updateDetail(
                            "phone",
                            event.target.value.replace(
                              /\D/g,
                              ""
                            )
                          )
                        }
                        placeholder="10-digit mobile number"
                        className="min-w-0 flex-1 px-4 text-sm outline-none"
                      />

                    </div>
                  </label>


                  {fulfillment ===
                    "DELIVERY" && (
                    <>
                      <label className="sm:col-span-2">

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          Address line 1 *
                        </span>

                        <input
                          type="text"
                          autoComplete="address-line1"
                          maxLength={255}
                          value={
                            details.addressLine1
                          }
                          onChange={(event) =>
                            updateDetail(
                              "addressLine1",
                              event.target.value
                            )
                          }
                          placeholder="House / flat / building / street"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label className="sm:col-span-2">

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          Address line 2
                        </span>

                        <input
                          type="text"
                          autoComplete="address-line2"
                          maxLength={255}
                          value={
                            details.addressLine2
                          }
                          onChange={(event) =>
                            updateDetail(
                              "addressLine2",
                              event.target.value
                            )
                          }
                          placeholder="Area / locality (optional)"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label className="sm:col-span-2">

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          Landmark
                        </span>

                        <input
                          type="text"
                          maxLength={255}
                          value={
                            details.landmark
                          }
                          onChange={(event) =>
                            updateDetail(
                              "landmark",
                              event.target.value
                            )
                          }
                          placeholder="Nearby landmark (optional)"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label>

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          City *
                        </span>

                        <input
                          type="text"
                          autoComplete="address-level2"
                          maxLength={120}
                          value={
                            details.city
                          }
                          onChange={(event) =>
                            updateDetail(
                              "city",
                              event.target.value
                            )
                          }
                          placeholder="City"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label>

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          State *
                        </span>

                        <input
                          type="text"
                          autoComplete="address-level1"
                          maxLength={120}
                          value={
                            details.state
                          }
                          onChange={(event) =>
                            updateDetail(
                              "state",
                              event.target.value
                            )
                          }
                          placeholder="State"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label>

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          PIN code *
                        </span>

                        <input
                          type="text"
                          inputMode="numeric"
                          autoComplete="postal-code"
                          maxLength={6}
                          value={
                            details.postalCode
                          }
                          onChange={(event) =>
                            updateDetail(
                              "postalCode",
                              event.target.value.replace(
                                /\D/g,
                                ""
                              )
                            )
                          }
                          placeholder="6-digit PIN"
                          className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none transition focus:border-[#075E54] focus:ring-2 focus:ring-[#D9ECE7]"
                        />

                      </label>

                      <label>

                        <span className="mb-1.5 block text-sm font-bold text-slate-700">
                          Country
                        </span>

                        <input
                          type="text"
                          value="India"
                          disabled
                          className="min-h-12 w-full cursor-not-allowed rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm font-semibold text-slate-600"
                        />

                      </label>
                    </>
                  )}

                </div>


                {fulfillment ===
                  "STORE_PICKUP" && (
                  <div className="mt-5 overflow-hidden rounded-2xl border border-[#D9ECE7] bg-[#EAF5F2]">

                    <div className="flex items-start gap-3 p-4">

                      <MapPin
                        size={22}
                        className="mt-0.5 shrink-0 text-[#075E54]"
                      />

                      <div>
                        <p className="font-black text-slate-950">
                          Khariar Computer Service
                        </p>

                        <p className="mt-2 text-sm leading-6 text-slate-700">
                          College Road, Near
                          Koshaleswar Temple
                          <br />
                          Raj Khariar, Gadramunda
                          <br />
                          Odisha - 766107
                        </p>

                        <div className="mt-3 flex items-center gap-2 text-sm font-bold text-slate-700">
                          <Phone size={15} />
                          8637277240
                        </div>

                        <div className="mt-2 flex items-center gap-2 text-xs text-slate-600">
                          <ReceiptText
                            size={15}
                          />
                          GSTIN:
                          21AMZPK0583F1ZX
                        </div>

                      </div>

                    </div>
                  </div>
                )}


                {formError && (
                  <div className="mt-5 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">

                    <AlertCircle
                      size={18}
                      className="mt-0.5 shrink-0"
                    />

                    <span>
                      {formError}
                    </span>

                  </div>
                )}


                {detailsAccepted && (
                  <div className="mt-5 flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-4">

                    <CheckCircle2
                      size={20}
                      className="mt-0.5 shrink-0 text-green-700"
                    />

                    <div>
                      <p className="text-sm font-black text-green-900">
                        {fulfillment ===
                        "DELIVERY"
                          ? "Delivery details ready"
                          : "Pickup details ready"}
                      </p>

                      <p className="mt-1 text-xs leading-5 text-green-800">
                        Your details were securely
                        validated by KCMART.
                        No order has been created
                        yet.
                      </p>
                    </div>

                  </div>
                )}


                {!detailsAccepted && (
                  <button
                    type="submit"
                    className="mt-6 flex min-h-12 w-full items-center justify-center rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46]"
                  >
                    Continue
                  </button>
                )}

              </form>


              {/* PAYMENT METHOD */}

              <section
                className={`rounded-[22px] border p-5 transition ${
                  detailsAccepted
                    ? "border-slate-100 bg-white shadow-sm"
                    : "border-slate-200 bg-slate-100"
                }`}
              >

                <div className="flex items-start gap-3">

                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white">

                    {detailsAccepted ? (
                      <ShieldCheck
                        size={19}
                        className="text-[#075E54]"
                      />
                    ) : (
                      <LockKeyhole
                        size={19}
                        className="text-slate-500"
                      />
                    )}

                  </div>

                  <div>

                    <p
                      className={`text-xs font-black uppercase tracking-[0.16em] ${
                        detailsAccepted
                          ? "text-[#075E54]"
                          : "text-slate-500"
                      }`}
                    >
                      Step 4
                    </p>

                    <h2 className="mt-1 font-black text-slate-900">
                      Payment method
                    </h2>

                    <p className="mt-1 text-xs leading-5 text-slate-500">

                      {!detailsAccepted
                        ? "Verify your customer details first."
                        : fulfillment === "DELIVERY"
                          ? "Home Delivery supports Online Payment only."
                          : "Choose Online Payment or Pay at Store."}

                    </p>

                  </div>

                </div>


                {detailsAccepted && (

                  <div className="mt-5 space-y-3">

                    <button
                      type="button"
                      onClick={() =>
                        choosePaymentMethod("ONLINE")
                      }
                      className={`w-full rounded-2xl border-2 p-4 text-left transition ${
                        paymentMethod === "ONLINE"
                          ? "border-[#075E54] bg-[#EAF5F2]"
                          : "border-slate-200 bg-white hover:border-slate-300"
                      }`}
                    >

                      <div className="flex items-start justify-between gap-4">

                        <div>

                          <p className="font-black text-slate-950">
                            Online Payment
                          </p>

                          <p className="mt-1 text-xs leading-5 text-slate-500">
                              Pay securely online using Razorpay.
                            </p>

                        </div>

                        <div
                          className={`mt-1 h-5 w-5 shrink-0 rounded-full border-[5px] ${
                            paymentMethod === "ONLINE"
                              ? "border-[#075E54]"
                              : "border-slate-300"
                          }`}
                        />

                      </div>

                    </button>


                    {fulfillment === "STORE_PICKUP" && (

                      <button
                        type="button"
                        onClick={() =>
                          choosePaymentMethod(
                            "PAY_AT_STORE"
                          )
                        }
                        className={`w-full rounded-2xl border-2 p-4 text-left transition ${
                          paymentMethod === "PAY_AT_STORE"
                            ? "border-[#075E54] bg-[#EAF5F2]"
                            : "border-slate-200 bg-white hover:border-slate-300"
                        }`}
                      >

                        <div className="flex items-start justify-between gap-4">

                          <div>

                            <p className="font-black text-slate-950">
                              Pay at Store
                            </p>

                            <p className="mt-1 text-xs leading-5 text-slate-500">
                              Pay at Khariar Computer
                              Service when collecting
                              your order.
                            </p>

                          </div>

                          <div
                            className={`mt-1 h-5 w-5 shrink-0 rounded-full border-[5px] ${
                              paymentMethod === "PAY_AT_STORE"
                                ? "border-[#075E54]"
                                : "border-slate-300"
                            }`}
                          />

                        </div>

                      </button>
                    )}


                    {paymentError && (

                      <div className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">

                        <AlertCircle
                          size={18}
                          className="mt-0.5 shrink-0"
                        />

                        <span>
                          {paymentError}
                        </span>

                      </div>
                    )}


                    {paymentAccepted && (

                      <div className="flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-4">

                        <CheckCircle2
                          size={20}
                          className="mt-0.5 shrink-0 text-green-700"
                        />

                        <div>

                          <p className="text-sm font-black text-green-900">
                            Payment method verified
                          </p>

                          <p className="mt-1 text-xs leading-5 text-green-800">

                            {paymentMethod === "ONLINE"
                              ? "Online payment is ready. Razorpay will open after you place the order."
                              : "Pay at Store is approved for this Store Pickup."}

                          </p>

                          <p className="mt-1 text-xs leading-5 text-green-800">
                            No order or payment has
                            been created yet.
                          </p>

                        </div>

                      </div>
                    )}


                    <button
                      type="button"
                      disabled={paymentLoading}
                      onClick={() =>
                        void handlePaymentContinue()
                      }
                      className="flex min-h-12 w-full items-center justify-center rounded-xl bg-[#075E54] px-5 text-sm font-extrabold text-white transition hover:bg-[#064E46] disabled:cursor-not-allowed disabled:bg-[#8CBDB2]"
                    >

                      {paymentLoading
                        ? "Verifying..."
                        : paymentAccepted
                          ? "Payment Method Verified"
                          : "Continue with Payment Method"}

                    </button>

                  </div>
                )}

              </section>

            </div>


            {/* SUMMARY */}

            <aside className="rounded-[22px] border border-slate-100 bg-white p-5 shadow-sm lg:sticky lg:top-32">

              <h2 className="text-lg font-black text-slate-950">
                Order summary
              </h2>

              <div className="mt-5 flex justify-between text-sm text-slate-600">

                <span>
                  Items ({validated.itemCount})
                </span>

                <span className="font-bold text-slate-900">
                  {money(
                    validated.subtotal
                  )}
                </span>

              </div>

              <div className="mt-4 flex justify-between text-sm text-slate-600">

                <span>
                  Delivery
                </span>

                <span className="font-bold text-slate-700">
                  {fulfillment ===
                  "STORE_PICKUP"
                    ? "Store pickup"
                    : "Calculated later"}
                </span>

              </div>

              <div className="mt-5 flex justify-between border-t border-slate-100 pt-5 text-xl font-black text-slate-950">

                <span>Subtotal</span>

                <span>
                  {money(
                    validated.subtotal
                  )}
                </span>

              </div>

              <div className="mt-5 flex items-start gap-2 rounded-xl bg-[#EAF5F2] p-3 text-xs leading-5 text-[#043D36]">

                <ShieldCheck
                  size={18}
                  className="mt-0.5 shrink-0"
                />

                <span>
                  Product prices and stock were
                  verified by KCMART&apos;s
                  backend.
                </span>

              </div>

              {orderError && (
                <div className="mt-5 rounded-xl border border-red-200 bg-red-50 p-3 text-sm leading-5 text-red-800">
                  {orderError}
                </div>
              )}

              <button
                type="button"
                disabled={
                  orderLoading ||
                  !detailsAccepted ||
                  !paymentAccepted ||
                  (
                    fulfillment === "DELIVERY" &&
                    paymentMethod !== "ONLINE"
                  )
                }
                onClick={() =>
                  void handlePlaceOrder()
                }
                className="mt-6 w-full rounded-xl bg-[#075E54] px-5 py-4 font-extrabold text-white transition hover:bg-[#064E46] disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500"
              >
                {orderLoading
                  ? "Processing..."
                  : fulfillment === "DELIVERY" && paymentMethod === "ONLINE"
                    ? "Pay & Place Order"
                    : "Place Order"}
              </button>

              <p className="mt-3 text-center text-xs leading-5 text-slate-500">
                {fulfillment === "STORE_PICKUP" &&
                paymentMethod === "PAY_AT_STORE"
                  ? "Your order will be reserved from store stock. Pay when you collect it."
                  : fulfillment === "DELIVERY"
                    ? "Your order will be created and you will be redirected to secure Razorpay payment."
                    : "Choose an approved payment method to continue."}
              </p>

            </aside>

          </div>
        )}

      </div>
    </main>
  );
}










