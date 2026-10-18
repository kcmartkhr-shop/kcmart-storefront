"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { useRef, useState } from "react";

const slides = [
  {
    eyebrow: "YOUR TECH DESTINATION",
    title: "Upgrade your digital world.",
    description: "Explore laptops, desktops and everyday tech.",
    button: "Shop Now",
    href: "/categories",
    image:
      "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=900&q=85",
    background: "from-[#043D36] via-[#075E54] to-[#14947F]"
  },
  {
    eyebrow: "BUILD YOUR SETUP",
    title: "Everything for your workspace.",
    description: "Computers, components and accessories.",
    button: "Explore Products",
    href: "/categories",
    image:
      "https://images.unsplash.com/photo-1587831990711-23ca6441447b?w=900&q=85",
    background: "from-[#043D36] via-[#075E54] to-[#28A58E]"
  },
  {
    eyebrow: "OFFICE ESSENTIALS",
    title: "Make every workday easier.",
    description: "Discover printers and technology essentials.",
    button: "Explore Printers",
    href: "/category/printers",
    image:
      "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=900&q=85",
    background: "from-[#043D36] via-[#087263] to-[#32AD96]"
  }
];

export default function MobileHeroCarousel() {
  const containerRef = useRef<HTMLDivElement>(null);
  const [active, setActive] = useState(0);

  function goTo(index: number) {
    const element = containerRef.current;
    if (!element) return;

    element.scrollTo({
      left: index * element.clientWidth,
      behavior: "smooth"
    });

    setActive(index);
  }

  function updateActive() {
    const element = containerRef.current;
    if (!element || !element.clientWidth) return;

    setActive(
      Math.max(
        0,
        Math.min(
          slides.length - 1,
          Math.round(element.scrollLeft / element.clientWidth)
        )
      )
    );
  }

  return (
    <section aria-label="KCMART promotions">
      <div
        ref={containerRef}
        onScroll={updateActive}
        className="kcmart-app-carousel flex snap-x snap-mandatory overflow-x-auto rounded-[26px]"
      >
        {slides.map((slide) => (
          <article
            key={slide.eyebrow}
            className={
              "relative min-h-[345px] w-full shrink-0 snap-start overflow-hidden rounded-[26px] bg-gradient-to-br text-white " +
              slide.background
            }
          >
            <div className="pointer-events-none absolute -right-16 -top-20 h-64 w-64 rounded-full border-[40px] border-white/10" />

            <div className="relative z-10 flex min-h-[345px] flex-col p-5">
              <span className="w-fit rounded-full border border-white/20 bg-white/15 px-3 py-1.5 text-[10px] font-extrabold tracking-widest backdrop-blur">
                {slide.eyebrow}
              </span>

              <h2 className="mt-5 max-w-[260px] text-[29px] font-black leading-[1.1] tracking-tight">
                {slide.title}
              </h2>

              <p className="mt-3 max-w-[210px] text-[12px] leading-5 text-emerald-100">
                {slide.description}
              </p>

              <div className="pointer-events-none absolute bottom-14 right-[-20px] h-[145px] w-[180px] rotate-[-8deg] overflow-hidden rounded-2xl border-4 border-white/15 shadow-2xl">
                <img
                  src={slide.image}
                  alt=""
                  className="h-full w-full object-cover"
                />
              </div>

              <Link
                href={slide.href}
                className="relative z-10 mt-auto inline-flex min-h-12 w-fit items-center gap-2 rounded-xl bg-white px-5 py-3 text-[13px] font-extrabold text-[#075E54] shadow-lg transition active:scale-95"
              >
                {slide.button}
                <ArrowRight size={17} />
              </Link>
            </div>
          </article>
        ))}
      </div>

      <div className="mt-3 flex justify-center gap-2">
        {slides.map((slide, index) => (
          <button
            key={slide.eyebrow}
            type="button"
            onClick={() => goTo(index)}
            aria-label={`Show promotion ${index + 1}`}
            aria-current={active === index ? "true" : undefined}
            className={
              "h-2 rounded-full transition-all duration-300 " +
              (active === index
                ? "w-7 bg-[#075E54]"
                : "w-2 bg-slate-300")
            }
          />
        ))}
      </div>
    </section>
  );
}