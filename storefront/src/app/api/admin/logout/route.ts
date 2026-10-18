import { NextRequest, NextResponse } from "next/server";
import { deleteAdminSession } from "@/lib/admin/session";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const id = request.cookies.get(
    "kcmart_admin_session"
  )?.value;

  deleteAdminSession(id);

  const response = NextResponse.json({
    success: true,
  });

  response.cookies.delete("kcmart_admin_session");

  return response;
}