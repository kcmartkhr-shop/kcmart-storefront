"use client";

import { useEffect, useState } from "react";

type Eligibility = {
  eligible: boolean;
  blocker: string | null;
};

type State =
  | { kind: "loading" }
  | { kind: "ready" }
  | { kind: "blocked"; reason: string }
  | { kind: "error" };

export default function DelhiveryLabelEligibilityPanel({
  orderId,
}: {
  orderId: number;
}) {
  const [state, setState] = useState<State>({ kind: "loading" });

  useEffect(() => {
    const controller = new AbortController();

    async function load() {
      setState({ kind: "loading" });

      try {
        const response = await fetch(
          `/api/admin/orders/${orderId}/delhivery-label-eligibility`,
          {
            method: "GET",
            credentials: "same-origin",
            cache: "no-store",
            signal: controller.signal,
          }
        );

        if (!response.ok) {
          throw new Error("Eligibility request failed");
        }

        const data: unknown = await response.json();

        if (
          !data ||
          typeof data !== "object" ||
          !("eligible" in data) ||
          typeof data.eligible !== "boolean"
        ) {
          throw new Error("Invalid eligibility response");
        }

        const result = data as Eligibility;

        if (result.eligible) {
          setState({ kind: "ready" });
        } else {
          setState({
            kind: "blocked",
            reason:
              typeof result.blocker === "string" &&
              result.blocker.trim()
                ? result.blocker
                : "Label eligibility requirements are not satisfied.",
          });
        }
      } catch {
        if (!controller.signal.aborted) {
          setState({ kind: "error" });
        }
      }
    }

    void load();

    return () => controller.abort();
  }, [orderId]);

  return (
    <section
      aria-label="Delhivery label eligibility"
      className="mt-5 rounded-xl border border-slate-200 bg-white p-4"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-semibold text-slate-900">
          Delhivery Label Eligibility
        </h3>
        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
          Read-only
        </span>
      </div>

      <div aria-live="polite" className="mt-3 text-sm">
        {state.kind === "loading" && (
          <p className="text-slate-600">
            Checking label eligibility...
          </p>
        )}

        {state.kind === "ready" && (
          <p className="font-semibold text-emerald-700">
            Ready — confirmed Delhivery booking and valid AWB.
          </p>
        )}

        {state.kind === "blocked" && (
          <div className="space-y-1">
            <p className="font-semibold text-amber-800">
              Blocked
            </p>
            <p className="break-words text-slate-700">
              {state.reason}
            </p>
          </div>
        )}

        {state.kind === "error" && (
          <p className="text-rose-700">
            Unable to check label eligibility right now.
          </p>
        )}
      </div>

      <p className="mt-3 text-xs text-slate-500">
        Eligibility does not mean a shipping label PDF is available.
        Label download and courier booking remain disabled.
      </p>
    </section>
  );
}