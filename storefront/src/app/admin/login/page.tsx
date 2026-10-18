"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { LockKeyhole, ArrowRight } from "lucide-react";

export default function AdminLoginPage() {
  const router = useRouter();

  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function login(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await fetch("/api/admin/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username,
          password,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.error ?? "Login failed"
        );
      }

      router.replace("/admin");
      router.refresh();
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Login failed"
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen bg-slate-100 px-4 py-16">
      <div className="mx-auto max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-[#075E54] text-white">
            <LockKeyhole size={28} />
          </div>

          <h1 className="text-3xl font-bold text-slate-900">
            KCMART Admin
          </h1>

          <p className="mt-2 text-slate-500">
            Sign in to manage your store
          </p>
        </div>

        <form
          onSubmit={login}
          className="rounded-3xl border border-slate-200 bg-white p-8 shadow-xl"
        >
          <label className="mb-2 block text-sm font-semibold text-slate-700">
            Username
          </label>

          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoComplete="username"
            className="mb-5 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 outline-none focus:border-[#0B7A69]"
          />

          <label className="mb-2 block text-sm font-semibold text-slate-700">
            Password
          </label>

          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
            className="mb-5 w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-900 outline-none focus:border-[#0B7A69]"
          />

          {error && (
            <p className="mb-5 rounded-xl bg-red-50 p-3 text-sm text-red-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-5 py-3 font-semibold text-white hover:bg-[#064E46] disabled:opacity-60"
          >
            {loading ? "Signing in..." : "Sign in"}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>
      </div>
    </main>
  );
}