import Link from "next/link";
import { ArrowRight, ChevronRight, Grid2X2 } from "lucide-react";
import { getCategories } from "@/lib/api";

export const metadata = {
  title: "Shop by Category | KCMART",
  description:
    "Explore computers, printers, accessories and more at KCMART.",
};

export const dynamic = "force-dynamic";

export default async function CategoriesPage() {
  const allCategories = await getCategories();
  const categories = allCategories.filter(
    (category) => category.active
  );

  const parents = categories.filter(
    (category) =>
      category.parentId === null ||
      !categories.some(
        (parent) => parent.id === category.parentId
      )
  );

  return (
    <div className="min-h-screen bg-slate-50 pb-10">
      <div className="border-b border-slate-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 pb-7 pt-6 sm:px-6 lg:px-8">
          <nav className="flex items-center gap-2 text-xs text-slate-500">
            <Link href="/">Home</Link>
            <ChevronRight size={13} />
            <span className="font-bold text-[#075E54]">
              Categories
            </span>
          </nav>

          <div className="mt-6 flex items-center gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[#EAF5F2] text-[#075E54]">
              <Grid2X2 size={26} />
            </div>

            <div>
              <p className="text-xs font-extrabold uppercase tracking-widest text-[#075E54]">
                Explore KCMART
              </p>

              <h1 className="mt-1 text-2xl font-black tracking-tight text-slate-950 sm:text-4xl">
                Shop by Category
              </h1>

              <p className="mt-1 text-xs text-slate-500 sm:text-sm">
                Find the technology you need.
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-7xl px-3 py-5 sm:px-6 lg:px-8">
        <p className="mb-4 px-1 text-sm font-bold text-slate-600">
          {parents.length} main categories
        </p>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
          {parents.map((category) => {
            const children = categories.filter(
              (item) => item.parentId === category.id
            );

            return (
              <Link
                key={category.id}
                href={`/category/${category.slug}`}
                className="group flex min-h-44 flex-col rounded-2xl border border-slate-100 bg-white p-4 shadow-sm transition hover:-translate-y-1 hover:border-[#BFDCD5] hover:shadow-md sm:min-h-52 sm:p-6"
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#EAF5F2] to-[#D9ECE7] text-3xl font-black text-[#075E54] transition group-hover:from-[#075E54] group-hover:to-[#0B7A69] group-hover:text-white sm:h-20 sm:w-20">
                  {category.name.charAt(0).toUpperCase()}
                </div>

                <h2 className="mt-4 line-clamp-2 text-sm font-extrabold text-slate-900 sm:text-base">
                  {category.name}
                </h2>

                <p className="mt-1 text-xs text-slate-500">
                  {children.length > 0
                    ? `${children.length} subcategories`
                    : "Explore products"}
                </p>

                <span className="mt-auto flex items-center gap-1 pt-4 text-xs font-extrabold text-[#075E54]">
                  Shop now
                  <ArrowRight size={14} />
                </span>
              </Link>
            );
          })}
        </div>

        {categories.length === 0 && (
          <div className="rounded-2xl bg-white p-10 text-center text-slate-500">
            Categories are being updated.
          </div>
        )}
      </div>
    </div>
  );
}