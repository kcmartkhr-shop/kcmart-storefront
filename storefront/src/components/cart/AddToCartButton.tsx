"use client";
import { useState } from "react";
import Link from "next/link";
import { ShoppingCart, CheckCircle2 } from "lucide-react";
import { useCart, type CartItem } from "@/components/cart/CartProvider";

type ProductInput = Omit<CartItem, "quantity">;
export default function AddToCartButton({ product, compact = false }: { product: ProductInput; compact?: boolean }) {
  const { addItem, ready } = useCart();
  const [added, setAdded] = useState(false);
  return (
    <div className={compact ? "w-full" : "mt-6 space-y-3"}>
      <button type="button" disabled={!ready} onClick={() => { addItem(product); setAdded(true); }} className="kcmart-primary-button flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#075E54] px-3 py-3 text-sm font-extrabold text-white shadow-sm transition duration-200 active:scale-[0.97] hover:bg-[#064E46] disabled:opacity-50">
        {added ? <><CheckCircle2 size={19} /> Added to Cart</> : <><ShoppingCart size={19} /> Add to Cart</>}
      </button>
      {added && !compact && <Link href="/cart" className="block rounded-xl border border-[#BFDCD5] bg-[#EAF5F2] px-5 py-3 text-center font-bold text-[#075E54] transition hover:-translate-y-0.5 hover:bg-[#D9ECE7]">View Cart</Link>}
      {added && compact && <Link href="/cart" className="mt-1 block text-center text-xs font-bold text-[#075E54]">View Cart</Link>}
    </div>
  );
}