import type { Metadata } from "next";
import { Inter } from "next/font/google";

import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "KCMART | Computers & IT Accessories",
    template: "%s | KCMART",
  },
  description:
    "Explore computers, laptops, printers and IT accessories at KCMART. Pan-India delivery and store pickup from Khariar, Odisha.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${inter.className} bg-slate-50 text-slate-900`}>
        {children}
      </body>
    </html>
  );
}