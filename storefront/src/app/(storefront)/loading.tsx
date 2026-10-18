export default function StorefrontLoading() {
  return (
    <div className="min-h-[65vh] bg-[#F7FAF9] px-4 py-10">
      <div className="mx-auto max-w-[1440px]">

        <div className="flex items-center justify-center py-8">
          <div className="text-center">
            <div
              className="kcmart-loader mx-auto"
              role="status"
              aria-label="Loading"
            />

            <p className="mt-4 text-sm font-bold text-[#075E54]">
              Loading KCMART...
            </p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map((item) => (
            <div
              key={item}
              className="rounded-2xl border border-[#E5E7EB] bg-white p-4"
            >
              <div className="kcmart-skeleton aspect-[4/3] rounded-xl" />
              <div className="kcmart-skeleton mt-4 h-3 w-20 rounded-full" />
              <div className="kcmart-skeleton mt-3 h-5 w-full rounded-full" />
              <div className="kcmart-skeleton mt-2 h-5 w-2/3 rounded-full" />
              <div className="kcmart-skeleton mt-5 h-11 w-full rounded-xl" />
            </div>
          ))}
        </div>

      </div>
    </div>
  );
}