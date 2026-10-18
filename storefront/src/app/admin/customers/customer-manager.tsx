"use client";

import { useEffect, useMemo, useState } from "react";

type Customer = {
  id: number;
  email: string;
  name: string | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
};

type CustomerOrder = {
  id: number;
  orderNumber: string;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  orderStatus: string;
  createdAt: string;
};

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatAmount(value: number) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(value);
}

function statusClass(value: string) {
  const normalized = value.toUpperCase();

  if (
    normalized === "PAID" ||
    normalized === "COMPLETED" ||
    normalized === "ACTIVE"
  ) {
    return "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200";
  }

  if (
    normalized === "PENDING"
  ) {
    return "bg-amber-50 text-amber-700 ring-1 ring-amber-200";
  }

  if (
    normalized === "CANCELLED" ||
    normalized === "FAILED" ||
    normalized === "INACTIVE"
  ) {
    return "bg-red-50 text-red-700 ring-1 ring-red-200";
  }

  return "bg-slate-50 text-slate-700 ring-1 ring-slate-200";
}

export default function CustomerManager() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<
    "ALL" | "ACTIVE" | "INACTIVE"
  >("ALL");

  const [selectedCustomer, setSelectedCustomer] =
    useState<Customer | null>(null);

  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadCustomers() {
      try {
        setLoading(true);
        setError("");

        const response = await fetch("/api/admin/customers", {
          credentials: "same-origin",
          cache: "no-store",
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data = (await response.json()) as Customer[];

        if (!cancelled) {
          setCustomers(data);
        }
      } catch {
        if (!cancelled) {
          setError("Unable to load customers.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadCustomers();

    return () => {
      cancelled = true;
    };
  }, []);

  async function openCustomer(customer: Customer) {
    setSelectedCustomer(customer);
    setOrders([]);
    setOrdersError("");
    setOrdersLoading(true);

    try {
      const response = await fetch(
        `/api/admin/customers/${customer.id}/orders`,
        {
          credentials: "same-origin",
          cache: "no-store",
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = (await response.json()) as CustomerOrder[];
      setOrders(data);
    } catch {
      setOrdersError("Unable to load this customer's orders.");
    } finally {
      setOrdersLoading(false);
    }
  }

  function closeCustomer() {
    setSelectedCustomer(null);
    setOrders([]);
    setOrdersError("");
  }

  const filteredCustomers = useMemo(() => {
    const query = search.trim().toLowerCase();

    return customers.filter((customer) => {
      const matchesSearch =
        !query ||
        customer.id.toString().includes(query) ||
        customer.email.toLowerCase().includes(query) ||
        (customer.name ?? "").toLowerCase().includes(query);

      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "ACTIVE" && customer.active) ||
        (statusFilter === "INACTIVE" && !customer.active);

      return matchesSearch && matchesStatus;
    });
  }, [customers, search, statusFilter]);

  const activeCount = customers.filter((customer) => customer.active).length;
  const inactiveCount = customers.length - activeCount;

  return (
    <main className="min-h-screen bg-[#F4F9F7] px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mb-6">
          <p className="text-sm font-semibold uppercase tracking-wide text-[#0B7A69]">
            Admin
          </p>

          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827] sm:text-3xl">
            Customers
          </h1>

          <p className="mt-1 text-sm text-slate-500">
            View registered customers and their order history.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <SummaryCard label="Total Customers" value={customers.length} />
          <SummaryCard label="Active" value={activeCount} />
          <SummaryCard label="Inactive" value={inactiveCount} />
        </div>

        <section className="mt-6 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200 sm:p-5">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative flex-1">
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search name, email or customer ID..."
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-[#0B7A69] focus:ring-2 focus:ring-[#0B7A69]/15"
              />
            </div>

            <div className="flex rounded-xl bg-[#EAF5F2] p-1">
              {(["ALL", "ACTIVE", "INACTIVE"] as const).map((status) => (
                <button
                  key={status}
                  type="button"
                  onClick={() => setStatusFilter(status)}
                  className={`rounded-lg px-3 py-2 text-xs font-semibold transition ${
                    statusFilter === status
                      ? "bg-[#075E54] text-white shadow-sm"
                      : "text-[#064E46] hover:bg-white"
                  }`}
                >
                  {status === "ALL"
                    ? "All"
                    : status === "ACTIVE"
                      ? "Active"
                      : "Inactive"}
                </button>
              ))}
            </div>
          </div>

          {loading && (
            <div className="py-12 text-center text-sm text-slate-500">
              Loading customers...
            </div>
          )}

          {error && !loading && (
            <div className="mt-5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-200">
              {error}
            </div>
          )}

          {!loading && !error && (
            <div className="mt-5 overflow-x-auto">
              <table className="w-full min-w-[760px] text-left">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <th className="px-3 py-3 font-semibold">Customer</th>
                    <th className="px-3 py-3 font-semibold">ID</th>
                    <th className="px-3 py-3 font-semibold">Status</th>
                    <th className="px-3 py-3 font-semibold">Joined</th>
                    <th className="px-3 py-3 text-right font-semibold">
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-slate-100">
                  {filteredCustomers.map((customer) => (
                    <tr
                      key={customer.id}
                      className="transition hover:bg-[#F4F9F7]"
                    >
                      <td className="px-3 py-4">
                        <div className="font-semibold text-[#111827]">
                          {customer.name || "Unnamed customer"}
                        </div>
                        <div className="mt-0.5 text-sm text-slate-500">
                          {customer.email}
                        </div>
                      </td>

                      <td className="px-3 py-4 text-sm font-medium text-slate-700">
                        #{customer.id}
                      </td>

                      <td className="px-3 py-4">
                        <span
                          className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(
                            customer.active ? "ACTIVE" : "INACTIVE"
                          )}`}
                        >
                          {customer.active ? "Active" : "Inactive"}
                        </span>
                      </td>

                      <td className="px-3 py-4 text-sm text-slate-600">
                        {formatDate(customer.createdAt)}
                      </td>

                      <td className="px-3 py-4 text-right">
                        <button
                          type="button"
                          onClick={() => openCustomer(customer)}
                          className="rounded-lg bg-[#075E54] px-3 py-2 text-xs font-semibold text-white transition hover:bg-[#064E46]"
                        >
                          View Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {filteredCustomers.length === 0 && (
                <div className="py-12 text-center text-sm text-slate-500">
                  No customers match your search.
                </div>
              )}
            </div>
          )}
        </section>
      </div>

      {selectedCustomer && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4">
          <div className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4">
              <div>
                <h2 className="text-lg font-bold text-[#111827]">
                  Customer Details
                </h2>
                <p className="text-sm text-slate-500">
                  Customer #{selectedCustomer.id}
                </p>
              </div>

              <button
                type="button"
                onClick={closeCustomer}
                className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-500 hover:bg-slate-100 hover:text-slate-800"
              >
                Close
              </button>
            </div>

            <div className="space-y-6 p-5">
              <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <DetailItem
                  label="Name"
                  value={selectedCustomer.name || "Unnamed customer"}
                />
                <DetailItem
                  label="Email"
                  value={selectedCustomer.email}
                />
                <DetailItem
                  label="Status"
                  value={selectedCustomer.active ? "Active" : "Inactive"}
                />
                <DetailItem
                  label="Registered"
                  value={formatDate(selectedCustomer.createdAt)}
                />
              </section>

              <section>
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h3 className="text-base font-bold text-[#111827]">
                      Order History
                    </h3>
                    <p className="text-sm text-slate-500">
                      {orders.length} order{orders.length === 1 ? "" : "s"}
                    </p>
                  </div>
                </div>

                {ordersLoading && (
                  <div className="rounded-xl bg-[#F4F9F7] px-4 py-10 text-center text-sm text-slate-500">
                    Loading order history...
                  </div>
                )}

                {ordersError && !ordersLoading && (
                  <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-200">
                    {ordersError}
                  </div>
                )}

                {!ordersLoading && !ordersError && orders.length === 0 && (
                  <div className="rounded-xl bg-[#F4F9F7] px-4 py-10 text-center text-sm text-slate-500">
                    This customer has no orders.
                  </div>
                )}

                {!ordersLoading && !ordersError && orders.length > 0 && (
                  <div className="overflow-x-auto rounded-xl ring-1 ring-slate-200">
                    <table className="w-full min-w-[900px] text-left">
                      <thead className="bg-[#F4F9F7]">
                        <tr className="text-xs uppercase tracking-wide text-slate-500">
                          <th className="px-4 py-3 font-semibold">Order</th>
                          <th className="px-4 py-3 font-semibold">Date</th>
                          <th className="px-4 py-3 font-semibold">Amount</th>
                          <th className="px-4 py-3 font-semibold">Payment</th>
                          <th className="px-4 py-3 font-semibold">
                            Payment Status
                          </th>
                          <th className="px-4 py-3 font-semibold">
                            Order Status
                          </th>
                        </tr>
                      </thead>

                      <tbody className="divide-y divide-slate-100 bg-white">
                        {orders.map((order) => (
                          <tr
                            key={order.id}
                            className="hover:bg-[#F4F9F7]"
                          >
                            <td className="px-4 py-4 text-sm font-semibold text-[#111827]">
                              {order.orderNumber}
                            </td>

                            <td className="px-4 py-4 text-sm text-slate-600">
                              {formatDate(order.createdAt)}
                            </td>

                            <td className="px-4 py-4 text-sm font-semibold text-[#111827]">
                              {formatAmount(order.totalAmount)}
                            </td>

                            <td className="px-4 py-4 text-sm text-slate-600">
                              {order.paymentMethod}
                            </td>

                            <td className="px-4 py-4">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(
                                  order.paymentStatus
                                )}`}
                              >
                                {order.paymentStatus}
                              </span>
                            </td>

                            <td className="px-4 py-4">
                              <span
                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusClass(
                                  order.orderStatus
                                )}`}
                              >
                                {order.orderStatus}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

function SummaryCard({
  label,
  value,
}: {
  label: string;
  value: number;
}) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-[#111827]">{value}</p>
    </div>
  );
}

function DetailItem({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl bg-[#F4F9F7] p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        {label}
      </p>
      <p className="mt-1 break-words text-sm font-semibold text-[#111827]">
        {value}
      </p>
    </div>
  );
}