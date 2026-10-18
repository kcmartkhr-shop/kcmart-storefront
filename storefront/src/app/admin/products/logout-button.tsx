"use client";

import { useRouter } from "next/navigation";

export default function LogoutButton() {
  const router = useRouter();

  async function logout() {
    await fetch("/api/admin/logout", {
      method: "POST",
    });

    router.replace("/admin/login");
    router.refresh();
  }

  return (
    <button
      onClick={logout}
      className="rounded-xl border border-slate-300 bg-white px-5 py-3 font-semibold text-slate-800 hover:bg-slate-50"
    >
      Log out
    </button>
  );
}