export default function AdminLoading() {
  return (
    <div className="min-h-[60vh] bg-[#F7FAF9] p-6 lg:p-8">
      <div className="mx-auto max-w-[1500px]">
        <div className="flex min-h-[320px] items-center justify-center">
          <div className="text-center">

            <div
              className="kcmart-loader mx-auto"
              role="status"
              aria-label="Loading admin panel"
            />

            <p className="mt-4 text-sm font-bold text-[#075E54]">
              Loading Admin Panel...
            </p>

            <p className="mt-1 text-xs text-[#6B7280]">
              Preparing your KCMART dashboard
            </p>

          </div>
        </div>
      </div>
    </div>
  );
}