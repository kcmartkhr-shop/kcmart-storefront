"use client";

import { useMemo, useState } from "react";
import { Package, X, ZoomIn } from "lucide-react";

type GalleryImage = {
  id: number;
  imageUrl: string;
  cover: boolean;
  displayOrder: number;
};

type Props = {
  imageUrl: string | null;
  images?: GalleryImage[];
  name: string;
};

function imageSource(imageUrl: string) {
  if (imageUrl.startsWith("/")) {
    return imageUrl.replace(
      "/api/v1/product-images/",
      "/api/product-images/"
    );
  }

  return imageUrl;
}

export default function ProductGallery({
  imageUrl,
  images = [],
  name,
}: Props) {
  const gallery = useMemo(() => {
    const sorted = [...images].sort((a, b) => {
      if (a.cover !== b.cover) {
        return a.cover ? -1 : 1;
      }

      return a.displayOrder - b.displayOrder;
    });

    if (sorted.length > 0) {
      return sorted;
    }

    if (imageUrl) {
      return [
        {
          id: -1,
          imageUrl,
          cover: true,
          displayOrder: 0,
        },
      ];
    }

    return [];
  }, [images, imageUrl]);

  const [selectedIndex, setSelectedIndex] = useState(0);
  const [zoom, setZoom] = useState(false);

  const safeIndex =
    selectedIndex < gallery.length ? selectedIndex : 0;

  const selected = gallery[safeIndex] ?? null;

  const source = selected
    ? imageSource(selected.imageUrl)
    : null;

  return (
    <div className="min-w-0">
      <section className="relative flex aspect-square min-h-[290px] items-center justify-center overflow-hidden rounded-[24px] border border-slate-100 bg-white p-6 shadow-sm sm:min-h-[480px] lg:aspect-auto">
        {source ? (
          <button
            type="button"
            aria-label="Enlarge product image"
            onClick={() => setZoom(true)}
            className="group flex h-full w-full cursor-zoom-in items-center justify-center"
          >
            <img
              src={source}
              alt={`${name} - photo ${safeIndex + 1}`}
              className="h-full max-h-[430px] w-full object-contain transition duration-300 group-active:scale-95"
            />


            <span className="absolute bottom-4 right-4 flex items-center gap-1.5 rounded-full border border-slate-200 bg-white/95 px-3 py-2 text-xs font-bold text-slate-700 shadow-sm">
              <ZoomIn size={15} />
              Zoom
            </span>
          </button>
        ) : (
          <div className="flex flex-col items-center gap-4 text-slate-400">
            <Package size={90} strokeWidth={1} />
            <span>Product photo coming soon</span>
          </div>
        )}
      </section>

      {gallery.length > 1 && (
        <div className="mt-3 flex gap-3 overflow-x-auto pb-2">
          {gallery.map((image, index) => {
            const thumbnail = imageSource(image.imageUrl);
            const active = index === safeIndex;

            return (
              <button
                key={image.id}
                type="button"
                onClick={() => setSelectedIndex(index)}
                aria-label={`View product photo ${index + 1}`}
                className={`relative h-[78px] w-[78px] shrink-0 overflow-hidden rounded-xl border-2 bg-white p-1.5 transition ${
                  active
                    ? "border-[#075E54] shadow-md"
                    : "border-slate-200 hover:border-[#BFDCD5]"
                }`}
              >
                <img
                  src={thumbnail}
                  alt={`${name} thumbnail ${index + 1}`}
                  className="h-full w-full object-contain"
                />

              </button>
            );
          })}
        </div>
      )}

      {gallery.length > 1 && (
        <p className="mt-1 text-xs font-medium text-slate-500">
          Photo {safeIndex + 1} of {gallery.length}
        </p>
      )}

      {zoom && source && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Enlarged product image"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/90 p-4"
          onClick={() => setZoom(false)}
        >
          <button
            type="button"
            aria-label="Close image"
            onClick={() => setZoom(false)}
            className="absolute right-4 top-4 flex h-12 w-12 items-center justify-center rounded-full bg-white text-slate-900"
          >
            <X size={23} />
          </button>

          <img
            src={source}
            alt={`${name} enlarged`}
            onClick={(event) => event.stopPropagation()}
            className="max-h-[85vh] max-w-full object-contain"
          />
        </div>
      )}
    </div>
  );
}