import Link from "next/link";
import { notFound } from "next/navigation";
import { getCategories, getProducts } from "@/lib/api";
import CategoryProducts from "./CategoryProducts";

type Props = {
  params: Promise<{ slug: string }>;
};

export default async function CategoryPage({ params }: Props) {
  const { slug } = await params;

  const categories = await getCategories();
  const category = categories.find(
    (item) => item.slug === slug && item.active
  );

  if (!category) {
    notFound();
  }

  const children = categories.filter(
    (item) => item.active && item.parentId === category.id
  );

  const ids = [category.id, ...children.map((item) => item.id)];

  const productGroups = await Promise.all(
    ids.map((id) => getProducts(id))
  );

  const uniqueProducts = Array.from(
    new Map(
      productGroups
        .flat()
        .filter((product) => product.active)
        .map((product) => [product.id, product])
    ).values()
  );

  return (
    <div className="min-h-screen bg-slate-50 pb-8">
      <div className="border-b border-slate-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 pb-5 pt-5 sm:px-6 lg:px-8">
          <nav
            aria-label="Breadcrumb"
            className="flex items-center gap-2 text-xs text-slate-500"
          >
            <Link href="/" className="hover:text-[#075E54]">
              Home
            </Link>
            <span>/</span>
            <Link
              href="/categories"
              className="hover:text-[#075E54]"
            >
              Categories
            </Link>
            <span>/</span>
            <span className="truncate font-semibold text-slate-900">
              {category.name}
            </span>
          </nav>

          <div className="mt-5 flex items-center gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[#EAF5F2] text-2xl font-black text-[#075E54] sm:h-20 sm:w-20">
              {category.name.charAt(0).toUpperCase()}
            </div>

            <div className="min-w-0">
              <h1 className="text-2xl font-black tracking-tight text-slate-950 sm:text-4xl">
                {category.name}
              </h1>

              <p className="mt-1 text-sm text-slate-500">
                {uniqueProducts.length} products available
              </p>
            </div>
          </div>

          {children.length > 0 && (
            <div className="-mx-4 mt-5 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:px-0">
              {children.map((child) => (
                <Link
                  key={child.id}
                  href={`/category/${child.slug}`}
                  className="shrink-0 rounded-full border border-[#D9ECE7] bg-[#EAF5F2] px-4 py-2 text-xs font-bold text-[#064E46] transition hover:bg-[#D9ECE7]"
                >
                  {child.name}
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>

      <CategoryProducts
        products={uniqueProducts}
        categoryName={category.name}
      />
    </div>
  );
}