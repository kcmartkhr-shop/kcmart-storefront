import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, BadgeCheck, ShieldCheck, Truck, MapPin } from "lucide-react";
import { getCategories, getProduct } from "@/lib/api";
import AddToCartButton from "@/components/cart/AddToCartButton";
import ProductGallery from "@/components/product/ProductGallery";

type Props = { params: Promise<{ slug: string }> };
const currency = (value: number) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(value);

export default async function ProductPage({ params }: Props) {
  const { slug } = await params;
  let product;
  try { product = await getProduct(slug); } catch { notFound(); }
  if (!product.active) notFound();
  const categories = await getCategories();
  const category = categories.find((item) => item.id === product.categoryId);
  const discount = product.mrp !== null && product.mrp > product.price ? Math.round(((product.mrp - product.price) / product.mrp) * 100) : 0;
  const cartProduct = { id: product.id, slug: product.slug, name: product.name, price: product.price, imageUrl: product.imageUrl };
  return (
    <main className="min-h-screen bg-[#F7FAF9] pb-28 md:pb-0">
      <div className="mx-auto max-w-7xl px-3 pb-8 pt-3 sm:px-6 sm:pt-8 lg:px-8">
        <nav aria-label="Breadcrumb" className="mb-3 hidden flex-wrap items-center gap-2 text-sm text-slate-500 sm:flex">
          <Link href="/" className="hover:text-[#075E54]">Home</Link><span>/</span>
          <Link href="/categories" className="hover:text-[#075E54]">Categories</Link>
          {category && <><span>/</span><Link href={`/category/${category.slug}`} className="hover:text-[#075E54]">{category.name}</Link></>}
          <span>/</span><span className="max-w-[250px] truncate font-semibold text-slate-900">{product.name}</span>
        </nav>
        <Link href={category ? `/category/${category.slug}` : "/categories"} className="mb-3 inline-flex min-h-11 items-center gap-2 text-sm font-bold text-slate-700 sm:hidden"><ArrowLeft size={19} /> Back to products</Link>
        <div className="grid items-start gap-4 lg:grid-cols-2 lg:gap-10">
          <ProductGallery
            imageUrl={product.imageUrl}
            images={product.images}
            name={product.name}
          />
          <section className="rounded-[24px] border border-slate-100 bg-white p-5 shadow-sm sm:p-9">
            <p className="text-xs font-extrabold uppercase tracking-[0.14em] text-[#075E54]">{product.brand ?? "KCMART"}</p>
            <h1 className="mt-2 text-[23px] font-black leading-tight text-slate-900 sm:text-3xl">{product.name}</h1>
            <p className="mt-3 text-xs text-slate-400">SKU: {product.sku}</p>
            <div className="my-5 border-y border-slate-100 py-5 sm:my-8">
              <div className="flex flex-wrap items-center gap-3"><span className="text-[30px] font-black tracking-tight text-slate-950 sm:text-4xl">{currency(product.price)}</span>{discount > 0 && <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-extrabold text-green-700">{discount}% OFF</span>}</div>
              {product.mrp !== null && product.mrp > product.price && <p className="mt-2 text-sm text-slate-500">MRP: <span className="line-through">{currency(product.mrp)}</span></p>}
              <p className="mt-3 text-xs text-slate-500">Taxes and availability subject to confirmation.</p>
            </div>
            <div className="hidden md:block"><AddToCartButton product={cartProduct} /></div>
            <div className="mt-5 rounded-2xl border border-[#D9ECE7] bg-[#EAF5F2] p-4"><div className="flex items-center gap-2 text-sm font-bold text-[#043D36]"><BadgeCheck size={19} /> Product enquiry</div><p className="mt-2 text-sm leading-6 text-[#064E46]">Online checkout is being prepared. Contact KCMART to confirm availability and place an order.</p></div>
            <div className="mt-5 grid grid-cols-2 gap-3 text-xs font-semibold text-slate-600"><div className="flex items-center gap-2"><MapPin size={19} className="text-[#075E54]" /> Store pickup</div><div className="flex items-center gap-2"><Truck size={19} className="text-[#075E54]" /> Delivery enquiry</div></div>
            <div className="mt-5 flex items-start gap-2 text-xs leading-5 text-slate-500"><ShieldCheck size={19} className="shrink-0 text-green-700" /><span>Warranty and product condition are described below where provided by the seller.</span></div>
          </section>
        </div>
        <section className="mt-5 rounded-[24px] border border-slate-100 bg-white p-5 shadow-sm sm:mt-10 sm:p-9"><h2 className="text-xl font-black text-slate-900 sm:text-2xl">Product description</h2><div className="mt-4 whitespace-pre-line text-sm leading-7 text-slate-600 sm:text-base">{product.description || "Detailed specifications coming soon."}</div></section>
      </div>
      <div className="fixed inset-x-0 z-40 border-t border-slate-200 bg-white/95 px-4 py-3 shadow-[0_-8px_25px_rgba(15,23,42,0.08)] backdrop-blur-md md:hidden" style={{ bottom: "calc(4rem + env(safe-area-inset-bottom))" }}>
        <div className="mx-auto flex max-w-xl items-center gap-3"><div className="min-w-0 shrink-0"><p className="text-[10px] text-slate-500">Price</p><p className="text-lg font-black text-slate-900">{currency(product.price)}</p></div><div className="min-w-0 flex-1"><AddToCartButton product={cartProduct} compact /></div></div>
      </div>
    </main>
  );
}