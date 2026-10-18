import { getCategories, getProducts } from "@/lib/api";
import SearchResults from "./SearchResults";

export const metadata = {
  title: "Search Products | KCMART"
};

export default async function SearchPage({
  searchParams
}: {
  searchParams: Promise<{ q?: string }>
}) {
  const { q = "" } = await searchParams;

  const [products, categories] = await Promise.all([
    getProducts(),
    getCategories()
  ]);

  return (
    <SearchResults
      initialQuery={q}
      products={products.filter(p => p.active)}
      categories={categories.filter(c => c.active)}
    />
  );
}