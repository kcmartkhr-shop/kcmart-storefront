/* @vitest-environment jsdom */

import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import DelhiveryPackagePanel from "../components/admin/delivery/DelhiveryPackagePanel";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: vi.fn(),
    refresh: vi.fn(),
    replace: vi.fn(),
  }),
}));

const endpoint = "/api/admin/orders/999999999/delhivery-package";

const baseProps = {
  orderId: 999999999,
  fulfillmentType: "DELIVERY",
  paymentMethod: "COD",
  paymentStatus: "PENDING",
  orderStatus: "PACKED",
  hasInvoice: false,
  shipment: null,
};

const savedPackage = {
  id: 1,
  orderId: 999999999,
  actualWeightGrams: 1200,
  lengthCm: 30,
  widthCm: 20,
  heightCm: 10,
  volumetricWeightGrams: 1200,
  chargeableWeightGrams: 1200,
};

function response(status: number, body: unknown = {}) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

function renderPanel(overrides: Partial<typeof baseProps> = {}) {
  return render(<DelhiveryPackagePanel orderId={999999999} />);
}

async function checkExisting() {
  fireEvent.click(screen.getByRole("button", { name: /check existing package/i }));
}

async function enterValidMeasurements() {
  fireEvent.change(screen.getByLabelText(/actual weight/i), {
    target: { value: "1200" },
  });
  fireEvent.change(screen.getByLabelText(/length \(cm\)/i), {
    target: { value: "30" },
  });
  fireEvent.change(screen.getByLabelText(/width \(cm\)/i), {
    target: { value: "20" },
  });
  fireEvent.change(screen.getByLabelText(/height \(cm\)/i), {
    target: { value: "10" },
  });
}

describe("KCMART Delhivery Package Panel", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("shows the panel for a packed delivery without shipment", () => {
    renderPanel();
    expect(screen.getByText("Delhivery Package Details")).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  

  

  it("loads an existing package and keeps it read-only", async () => {
    fetchMock.mockResolvedValueOnce(response(200, savedPackage));
    renderPanel();
    await checkExisting();

    await waitFor(() => {
      expect(screen.getByText(/package saved/i)).toBeTruthy();
    });

    expect(screen.getByText(/chargeable weight: 1200 g/i)).toBeTruthy();
    expect(screen.queryByRole("button", { name: /save package details/i })).toBeNull();
    expect(fetchMock).toHaveBeenCalledWith(endpoint, { cache: "no-store" });
  });

  it("rejects invalid weight without sending a POST", async () => {
    fetchMock.mockResolvedValueOnce(response(404));
    renderPanel();
    await checkExisting();

    await screen.findByRole("button", { name: /save package details/i });

    fireEvent.change(screen.getByLabelText(/actual weight/i), {
      target: { value: "0" },
    });

    fireEvent.click(screen.getByRole("button", { name: /save package details/i }));

    expect(screen.getByRole("alert").textContent).toMatch(/positive whole grams/i);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("rejects dimensions with more than two decimals", async () => {
    fetchMock.mockResolvedValueOnce(response(404));
    renderPanel();
    await checkExisting();

    await screen.findByRole("button", { name: /save package details/i });
    await enterValidMeasurements();

    fireEvent.change(screen.getByLabelText(/length \(cm\)/i), {
      target: { value: "30.123" },
    });

    fireEvent.click(screen.getByRole("button", { name: /save package details/i }));

    expect(screen.getByRole("alert").textContent).toMatch(/up to 2 decimals/i);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("displays API errors without saving a package", async () => {
    fetchMock.mockResolvedValueOnce(response(500, { message: "Temporary error" }));
    renderPanel();
    await checkExisting();

    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toContain("Temporary error");
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("saves a valid package through the mocked API", async () => {
    fetchMock
      .mockResolvedValueOnce(response(404))
      .mockResolvedValueOnce(response(200, savedPackage));

    renderPanel();
    await checkExisting();

    await screen.findByRole("button", { name: /save package details/i });
    await enterValidMeasurements();

    fireEvent.click(screen.getByRole("button", { name: /save package details/i }));

    await waitFor(() => {
      expect(screen.getByText(/package saved/i)).toBeTruthy();
    });

    expect(fetchMock).toHaveBeenCalledTimes(2);

    const [url, options] = fetchMock.mock.calls[1];

    expect(url).toBe(endpoint);
    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body)).toEqual({
      actualWeightGrams: 1200,
      lengthCm: 30,
      widthCm: 20,
      heightCm: 10,
    });
  });
});
