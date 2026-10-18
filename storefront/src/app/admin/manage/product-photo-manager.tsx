"use client";

import { useCallback, useEffect, useState } from "react";

type ProductImage = {
  id: number;
  imageUrl: string;
  cover: boolean;
  displayOrder: number;
};

type Props = {
  productId: number;
  onChanged?: () => void | Promise<void>;
};

function source(url: string) {
  return url.startsWith("/")
    ? url.replace(
        "/api/v1/product-images/",
        "/api/product-images/"
      )
    : url;
}

async function errorMessage(response: Response) {
  const text = await response.text();

  if (!text) {
    return `HTTP ${response.status}`;
  }

  try {
    const body = JSON.parse(text);

    return (
      body.message ??
      body.error ??
      body.detail ??
      `HTTP ${response.status}`
    );
  } catch {
    return text;
  }
}

export default function ProductPhotoManager({
  productId,
  onChanged,
}: Props) {
  const [gallery, setGallery] = useState<ProductImage[]>([]);
  const [pending, setPending] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadGallery = useCallback(async () => {
    setLoading(true);

    try {
      const response = await fetch(
        `/api/admin/product-images/${productId}`,
        {
          cache: "no-store",
        }
      );

      if (!response.ok) {
        throw new Error(await errorMessage(response));
      }

      const images = (await response.json()) as ProductImage[];

      images.sort((a, b) => {
        if (a.cover !== b.cover) {
          return a.cover ? -1 : 1;
        }

        return (
          a.displayOrder - b.displayOrder ||
          a.id - b.id
        );
      });

      setGallery(images);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load product photos."
      );
    } finally {
      setLoading(false);
    }
  }, [productId]);

  useEffect(() => {
    setPending([]);
    setPreviews([]);
    setError("");
    setMessage("");
    void loadGallery();
  }, [loadGallery]);

  function removePending(index: number) {
    setPending((current) =>
      current.filter((_, i) => i !== index)
    );

    setPreviews((current) =>
      current.filter((_, i) => i !== index)
    );
  }

  async function chooseFiles(files: File[]) {
    setError("");
    setMessage("");

    if (files.length === 0) return;

    const remaining =
      6 - gallery.length - pending.length;

    if (remaining <= 0) {
      setError(
        "Maximum 6 photos reached. Delete a saved photo before adding another."
      );
      return;
    }

    if (files.length > remaining) {
      setError(
        `Only ${remaining} more photo${
          remaining === 1 ? "" : "s"
        } can be added. Maximum is 6.`
      );
      return;
    }

    const oversized = files.find(
      (file) =>
        file.size === 0 || file.size > 4_000_000
    );

    if (oversized) {
      setError(
        `${oversized.name} must be between 1 byte and 4 MB.`
      );
      return;
    }

    const invalid = files.find(
      (file) =>
        ![
          "image/jpeg",
          "image/png",
          "image/webp",
        ].includes(file.type)
    );

    if (invalid) {
      setError(
        `${invalid.name} is not JPG, PNG or WebP.`
      );
      return;
    }

    const newPreviews = await Promise.all(
      files.map(
        (file) =>
          new Promise<string>((resolve) => {
            const reader = new FileReader();

            reader.onload = () =>
              resolve(String(reader.result ?? ""));

            reader.readAsDataURL(file);
          })
      )
    );

    setPending((current) => [...current, ...files]);
    setPreviews((current) => [
      ...current,
      ...newPreviews,
    ]);
  }

  async function upload() {
    if (pending.length === 0 || busy) return;

    setBusy(true);
    setError("");
    setMessage("");

    let uploaded = 0;

    try {
      for (const file of pending) {
        const data = new FormData();

        data.append("productId", String(productId));
        data.append("file", file);

        const response = await fetch(
          "/api/admin/upload-image",
          {
            method: "POST",
            body: data,
          }
        );

        if (!response.ok) {
          throw new Error(
            `${file.name}: ${await errorMessage(response)}`
          );
        }

        uploaded++;
      }

      setPending([]);
      setPreviews([]);

      await loadGallery();
      await onChanged?.();

      setMessage(
        `${uploaded} photo${
          uploaded === 1 ? "" : "s"
        } uploaded successfully.`
      );
    } catch (err) {
      /*
       * Some earlier files in the batch may already have
       * succeeded, so always reload the authoritative gallery.
       */
      setPending([]);
      setPreviews([]);

      await loadGallery();
      await onChanged?.();

      setError(
        err instanceof Error
          ? err.message
          : "Photo upload failed."
      );
    } finally {
      setBusy(false);
    }
  }

  async function setCover(imageId: number) {
    if (busy) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
        `/api/admin/product-images/${productId}/${imageId}/cover`,
        {
          method: "PATCH",
        }
      );

      if (!response.ok) {
        throw new Error(await errorMessage(response));
      }

      await loadGallery();
      await onChanged?.();

      setMessage("Cover photo updated.");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to change cover photo."
      );
    } finally {
      setBusy(false);
    }
  }

  async function deleteImage(imageId: number) {
    if (busy) return;

    const confirmed = window.confirm(
      "Delete this product photo?"
    );

    if (!confirmed) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
        `/api/admin/product-images/${productId}/${imageId}`,
        {
          method: "DELETE",
        }
      );

      if (!response.ok) {
        throw new Error(await errorMessage(response));
      }

      await loadGallery();
      await onChanged?.();

      setMessage("Photo deleted.");
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to delete photo."
      );
    } finally {
      setBusy(false);
    }
  }

  const used = gallery.length + pending.length;
  const remaining = Math.max(0, 6 - used);

  return (
    <section className="mb-6 rounded-2xl border border-[#BFDCD5] bg-[#F4F9F7] p-5">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-lg font-bold text-[#111827]">
            Product Photos
          </h3>

          <p className="mt-1 text-sm text-[#6B7280]">
            Add up to 6 photos. Maximum 4 MB each.
            Choose any saved photo as the cover.
          </p>
        </div>

        <span className="rounded-full border border-[#BFDCD5] bg-white px-3 py-1 text-xs font-bold text-[#075E54]">
          {gallery.length} / 6 saved
        </span>
      </div>

      {error && (
        <div
          role="alert"
          className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-sm font-semibold text-red-700"
        >
          {error}
        </div>
      )}

      {message && (
        <div
          role="status"
          className="mb-4 rounded-xl border border-green-200 bg-green-50 p-3 text-sm font-semibold text-green-700"
        >
          {message}
        </div>
      )}

      {loading ? (
        <div className="mb-4 rounded-xl border border-[#DDEBE7] bg-white p-8 text-center text-sm font-semibold text-slate-500">
          Loading photos...
        </div>
      ) : gallery.length > 0 ? (
        <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {gallery.map((image) => (
            <article
              key={image.id}
              className={`relative overflow-hidden rounded-xl border bg-white p-2 shadow-sm ${
                image.cover
                  ? "border-[#075E54]"
                  : "border-[#DDEBE7]"
              }`}
            >
              <div className="relative">
                <img
                  src={source(image.imageUrl)}
                  alt="Product photo"
                  className="h-32 w-full rounded-lg object-contain"
                />

                {image.cover && (
                  <span className="absolute left-2 top-2 rounded-full bg-[#075E54] px-2.5 py-1 text-[10px] font-black uppercase tracking-wide text-white">
                    Cover
                  </span>
                )}
              </div>

              <div className="mt-2 space-y-2">
                {!image.cover && (
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() =>
                      void setCover(image.id)
                    }
                    className="w-full rounded-lg bg-[#EAF5F2] px-2 py-2 text-xs font-bold text-[#075E54] transition hover:bg-[#DDEBE7] disabled:opacity-50"
                  >
                    Set as cover
                  </button>
                )}

                <button
                  type="button"
                  disabled={busy}
                  onClick={() =>
                    void deleteImage(image.id)
                  }
                  className="w-full rounded-lg border border-red-200 px-2 py-2 text-xs font-bold text-red-600 transition hover:bg-red-50 disabled:opacity-50"
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="mb-5 rounded-xl border border-dashed border-[#BFDCD5] bg-white p-8 text-center text-sm text-[#6B7280]">
          No saved product photos.
        </div>
      )}

      {previews.length > 0 && (
        <>
          <p className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">
            Ready to upload
          </p>

          <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
            {previews.map((preview, index) => (
              <div
                key={`${preview}-${index}`}
                className="rounded-xl border border-amber-200 bg-white p-2"
              >
                <img
                  src={preview}
                  alt={`Pending photo ${index + 1}`}
                  className="h-28 w-full rounded-lg object-contain"
                />

                <button
                  type="button"
                  disabled={busy}
                  onClick={() => removePending(index)}
                  className="mt-2 w-full rounded-lg border border-red-200 px-2 py-1.5 text-xs font-bold text-red-600 hover:bg-red-50 disabled:opacity-50"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </>
      )}

      {remaining > 0 ? (
        <>
          <input
            type="file"
            multiple
            disabled={busy}
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => {
              const files = Array.from(
                event.target.files ?? []
              );

              event.target.value = "";

              void chooseFiles(files);
            }}
            className="mb-3 block w-full cursor-pointer rounded-xl border border-[#DDEBE7] bg-white p-3 text-sm text-slate-600 file:mr-4 file:rounded-lg file:border-0 file:bg-[#075E54] file:px-4 file:py-2 file:font-semibold file:text-white hover:file:bg-[#064E46] disabled:opacity-50"
          />

          <p className="mb-4 text-xs text-[#6B7280]">
            {remaining} photo{remaining === 1 ? "" : "s"} remaining.
            You may select one photo now and add more later, or select several together.
          </p>
        </>
      ) : (
        <div className="mb-4 rounded-xl border border-[#BFDCD5] bg-white p-3 text-sm font-semibold text-[#075E54]">
          Maximum 6 photos reached. Delete a photo before adding another.
        </div>
      )}

      {pending.length > 0 && (
        <button
          type="button"
          disabled={busy}
          onClick={() => void upload()}
          className="kcmart-primary-button w-full rounded-xl bg-[#075E54] px-5 py-3 font-bold text-white disabled:opacity-50"
        >
          {busy
            ? "Uploading..."
            : `Upload ${pending.length} photo${
                pending.length === 1 ? "" : "s"
              }`}
        </button>
      )}
    </section>
  );
}