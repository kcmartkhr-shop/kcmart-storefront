"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import {
  ArrowLeft,
  LockKeyhole,
  Mail,
  ShieldCheck,
  ShoppingBag,
  X,
} from "lucide-react";
import { signIn, useSession } from "next-auth/react";

type LoginReason = "account" | "checkout";

type LoginContextValue = {
  openLogin: (reason?: LoginReason) => void;
  closeLogin: () => void;
};

const LoginContext = createContext<LoginContextValue | null>(null);

export function CustomerLoginProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState<LoginReason>("account");
  const [emailMode, setEmailMode] = useState(false);
  const [email, setEmail] = useState("");
  const [googleLoading, setGoogleLoading] = useState(false);
  const { status } = useSession();

  const closeLogin = useCallback(() => {
    setOpen(false);
  }, []);

  const openLogin = useCallback(
    (nextReason: LoginReason = "account") => {
      setReason(nextReason);
      setEmailMode(false);
      setEmail("");
      setOpen(true);
    },
    []
  );

  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        closeLogin();
      }
    }

    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [open, closeLogin]);

  return (
    <LoginContext.Provider value={{ openLogin, closeLogin }}>
      {children}

      {open && (
        <div
          className="fixed inset-0 z-[200] flex items-end justify-center bg-slate-950/65 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeLogin();
            }
          }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="customer-login-title"
            className="max-h-[95dvh] w-full max-w-md overflow-y-auto rounded-t-[28px] bg-white shadow-2xl sm:rounded-[28px]"
          >
            <div className="relative bg-gradient-to-br from-[#EFF5FF] to-white px-6 pb-7 pt-8 text-center">
              <button
                type="button"
                onClick={closeLogin}
                aria-label="Close login"
                className="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full bg-white text-slate-600 shadow-sm"
              >
                <X size={20} />
              </button>

              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-white shadow-sm">
                <ShoppingBag size={31} className="text-[#075E54]" />
              </div>

              <h2
                id="customer-login-title"
                className="mt-5 text-2xl font-black tracking-tight text-slate-950"
              >
                Welcome to KCMART
              </h2>

              <p className="mx-auto mt-2 max-w-xs text-sm leading-6 text-slate-500">
                {reason === "checkout"
                  ? "Sign in to continue to checkout."
                  : "Sign in to manage your KCMART account."}
              </p>
            </div>

            <div className="px-6 pb-8 pt-5">
              {!emailMode ? (
                <div className="space-y-4">
                  <div>
                    <button
                      type="button"
                      disabled={googleLoading || status === "loading"}
                      onClick={async () => {
                        setGoogleLoading(true);

                        try {
                          await signIn("google", {
                            callbackUrl:
                              reason === "checkout"
                                ? "/cart"
                                : "/account",
                          });
                        } catch {
                          setGoogleLoading(false);
                        }
                      }}
                      className="flex min-h-13 w-full items-center justify-center gap-3 rounded-xl border border-slate-200 bg-white px-4 font-bold text-slate-800 shadow-sm transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <span className="flex h-7 w-7 items-center justify-center rounded-full border border-slate-200 bg-white text-lg font-black text-[#0B7A69]">
                        G
                      </span>

                      {googleLoading
                        ? "Connecting to Google..."
                        : "Continue with Google"}
                    </button>

                    <p className="mt-2 text-center text-xs text-slate-400">
                      Secure sign-in with your Google account.
                    </p>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="h-px flex-1 bg-slate-200" />
                    <span className="text-xs font-bold text-slate-400">
                      OR
                    </span>
                    <div className="h-px flex-1 bg-slate-200" />
                  </div>

                  <button
                    type="button"
                    onClick={() => setEmailMode(true)}
                    className="flex min-h-13 w-full items-center justify-center gap-3 rounded-xl bg-[#075E54] px-4 font-bold text-white transition hover:bg-[#064E46]"
                  >
                    <Mail size={19} />
                    Continue with Email
                  </button>
                </div>
              ) : (
                <div>
                  <button
                    type="button"
                    onClick={() => setEmailMode(false)}
                    className="mb-5 flex items-center gap-2 text-sm font-bold text-[#075E54]"
                  >
                    <ArrowLeft size={17} />
                    Other sign-in options
                  </button>

                  <label
                    htmlFor="customer-login-email"
                    className="text-sm font-bold text-slate-800"
                  >
                    Email address
                  </label>

                  <input
                    id="customer-login-email"
                    type="email"
                    autoComplete="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="you@example.com"
                    className="mt-2 min-h-13 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-[#0B7A69] focus:ring-4 focus:ring-[#EAF5F2]"
                  />

                  <button
                    type="button"
                    disabled
                    className="mt-4 min-h-13 w-full rounded-xl bg-slate-200 px-4 font-extrabold text-slate-500"
                  >
                    Email sign-in coming soon
                  </button>

                  <p className="mt-3 text-center text-xs leading-5 text-slate-500">
                    Email verification will be enabled after
                    the authentication service is connected.
                  </p>
                </div>
              )}

              <div className="mt-7 flex items-start gap-2 border-t border-slate-100 pt-5 text-xs leading-5 text-slate-500">
                <ShieldCheck
                  size={18}
                  className="mt-0.5 shrink-0 text-green-700"
                />
                <span>
                  Your existing shopping cart will remain saved
                  in this browser.
                </span>
              </div>

              {reason === "checkout" && (
                <div className="mt-4 flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-900">
                  <LockKeyhole size={17} className="shrink-0" />
                  Online checkout is not enabled yet.
                  No order or payment will be created.
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </LoginContext.Provider>
  );
}

export function useCustomerLogin() {
  const context = useContext(LoginContext);

  if (!context) {
    throw new Error(
      "useCustomerLogin must be used inside CustomerLoginProvider"
    );
  }

  return context;
}