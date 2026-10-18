/* @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import DelhiveryBookingPreviewPanel from "../components/admin/delivery/DelhiveryBookingPreviewPanel";

const endpoint = "/api/admin/orders/123/delhivery-booking-preview";

const readyPreview = {
  orderId: 123,
  locallyReady: true,
  prepaidOnly: true,
  serviceabilityVerified: false,
  shipmentBooked: false,
  blockers: [],
};

function response(status: number, body: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

describe("Delhivery Booking Preview UI", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("does not request booking automatically", () => {
    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    expect(screen.getByText("Delhivery Booking Preview")).toBeTruthy();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("checks readiness using GET only", async () => {
    fetchMock.mockResolvedValueOnce(response(200, readyPreview));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByText("Local checks passed")).toBeTruthy();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(endpoint, {
      method: "GET",
      cache: "no-store",
    });

    expect(screen.getByText("Disabled")).toBeTruthy();
    expect(screen.getByText("Prepaid only")).toBeTruthy();
    expect(screen.getByText("Not verified by preview")).toBeTruthy();
  });

  it("displays booking blockers", async () => {
    fetchMock.mockResolvedValueOnce(response(200, {
      ...readyPreview,
      locallyReady: false,
      blockers: ["Order must be PAID.", "Package missing."],
    }));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByText("Booking blocked by local checks")).toBeTruthy();
    expect(screen.getByText("Order must be PAID.")).toBeTruthy();
    expect(screen.getByText("Package missing.")).toBeTruthy();
  });

  it("rejects mismatched order ID", async () => {
    fetchMock.mockResolvedValueOnce(response(200, {
      ...readyPreview,
      orderId: 999,
    }));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      "Invalid booking preview response."
    );

    expect(screen.queryByText("Local checks passed")).toBeNull();
  });

  it("rejects non-prepaid preview response", async () => {
    fetchMock.mockResolvedValueOnce(response(200, {
      ...readyPreview,
      prepaidOnly: false,
    }));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByText("Invalid booking preview response.")).toBeTruthy();
    expect(screen.queryByText("Local checks passed")).toBeNull();
  });

  it("shows session expiry on HTTP 401", async () => {
    fetchMock.mockResolvedValueOnce(response(401, {}));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByText(/session expired/i)).toBeTruthy();
  });

  it("refreshes preview without any POST", async () => {
    fetchMock
      .mockResolvedValueOnce(response(200, readyPreview))
      .mockResolvedValueOnce(response(200, {
        ...readyPreview,
        locallyReady: false,
        shipmentBooked: true,
        blockers: ["Shipment already exists."],
      }));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByText("Local checks passed")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", {
      name: /refresh preview/i,
    }));

    expect(await screen.findByText("Shipment already exists.")).toBeTruthy();

    expect(fetchMock).toHaveBeenCalledTimes(2);

    for (const [, options] of fetchMock.mock.calls) {
      expect(options.method).toBe("GET");
    }
  });

  it("does not treat API failure as successful readiness", async () => {
    fetchMock.mockResolvedValueOnce(response(503, {}));

    render(<DelhiveryBookingPreviewPanel orderId={123} />);

    fireEvent.click(screen.getByRole("button", {
      name: /check booking readiness/i,
    }));

    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.queryByText("Local checks passed")).toBeNull();
  });
});