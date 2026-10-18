export default function SearchLoading() {
  return (
    <main className="mx-auto max-w-7xl animate-pulse px-4 py-8">
      <div className="h-8 w-52 rounded-xl bg-slate-200" />
      <div className="mt-6 h-16 rounded-2xl bg-slate-100" />

      <div className="mt-8 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="rounded-2xl bg-white p-3">
            <div className="aspect-square rounded-xl bg-slate-200" />
            <div className="mt-4 h-4 rounded bg-slate-200" />
            <div className="mt-2 h-4 w-3/4 rounded bg-slate-100" />
            <div className="mt-5 h-6 w-1/2 rounded bg-slate-200" />
          </div>
        ))}
      </div>
    </main>
  );
}