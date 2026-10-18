export default function Loading() {
  return (
    <div className="mx-auto max-w-7xl animate-pulse px-4 py-6">
      <div className="h-4 w-44 rounded bg-slate-200" />
      <div className="mt-8 h-9 w-64 rounded bg-slate-200" />
      <div className="mt-3 h-4 w-32 rounded bg-slate-200" />

      <div className="mt-10 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <div
            key={index}
            className="overflow-hidden rounded-2xl border border-slate-100 bg-white"
          >
            <div className="aspect-square bg-slate-100" />
            <div className="space-y-3 p-4">
              <div className="h-3 w-16 rounded bg-slate-200" />
              <div className="h-4 w-full rounded bg-slate-200" />
              <div className="h-4 w-3/4 rounded bg-slate-200" />
              <div className="h-6 w-24 rounded bg-slate-200" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}