import Header from "@/components/layout/Header";
import { CartProvider } from "@/components/cart/CartProvider";
import { CustomerLoginProvider } from "@/components/auth/CustomerLoginProvider";
import AuthSessionProvider from "@/components/auth/AuthSessionProvider";
import Footer from "@/components/layout/Footer";
import MobileNav from "@/components/layout/MobileNav";

export default function StorefrontLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <AuthSessionProvider>
      <CartProvider>
        <CustomerLoginProvider>
          <Header />

          <main className="min-h-screen">
            {children}
          </main>

          <Footer />
          <MobileNav />

          <div
            className="h-16 md:hidden"
            aria-hidden="true"
          />
        </CustomerLoginProvider>
      </CartProvider>
    </AuthSessionProvider>
  );
}