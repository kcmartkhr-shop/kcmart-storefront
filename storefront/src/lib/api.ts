export type Category = {
  id: number;
  name: string;
  slug: string;
  parentId: number | null;
  active: boolean;
  createdAt: string;
};

export type Product = {
  id: number;
  categoryId: number;
  name: string;
  slug: string;
  description: string | null;
  imageUrl: string | null;
  images?: {
    id: number;
    imageUrl: string;
    cover: boolean;
    displayOrder: number;
  }[];
  brand: string | null;
  sku: string;
  price: number;
  mrp: number | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

const API_URL =
  process.env.KCMART_API_URL ?? "http://localhost:8080";

async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    next: { revalidate: 60 },
  });

  if (!response.ok) {
    throw new Error(`KCMART API error: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getCategories(): Promise<Category[]> {
  return getJson<Category[]>("/api/v1/categories");
}

export function getProducts(categoryId?: number): Promise<Product[]> {
  const query =
    categoryId === undefined ? "" : `?categoryId=${categoryId}`;

  return getJson<Product[]>(`/api/v1/products${query}`);
}

export async function getProduct(slug: string): Promise<Product> {
  const response = await fetch(
    `${API_URL}/api/v1/products/${encodeURIComponent(slug)}`,
    {
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new Error(`KCMART API error: ${response.status}`);
  }

  return response.json() as Promise<Product>;
}
