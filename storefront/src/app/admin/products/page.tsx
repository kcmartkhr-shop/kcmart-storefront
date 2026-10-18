import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getAdminSession } from "@/lib/admin/session";
import ProductManager from "../manage/product-manager";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export default async function AdminProductsPage() {
  const cookieStore = await cookies();

  const session = getAdminSession(
    cookieStore.get("kcmart_admin_session")?.value
  );

  if (!session) {
    redirect("/admin/login");
  }

  return <ProductManager />;
}