/**
 * @vitest-environment jsdom
 */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import DelhiveryServiceabilityPanel from "../components/admin/delivery/DelhiveryServiceabilityPanel";

describe("KCMART Delhivery Serviceability Panel", () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    cleanup();
    globalThis.fetch = originalFetch;
  });

  it("does not display verification from a previous order PIN", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        pincode: "751001",
        serviceable: true,
        prepaid: true,
      }),
    }) as typeof fetch;

    const view = render(
      <DelhiveryServiceabilityPanel initialPincode="751001" />
    );

    fireEvent.click(
      screen.getByRole("button", {
        name: /check serviceability/i,
      })
    );

    expect(
      await screen.findByText(/prepaid delivery verified/i)
    ).toBeTruthy();

    view.rerender(
      <DelhiveryServiceabilityPanel initialPincode="751002" />
    );

    expect(
      screen.queryByText(/prepaid delivery verified/i)
    ).toBeNull();

    const input = screen.getByRole("textbox", {
      name: /destination pincode/i,
    }) as HTMLInputElement;

    expect(input.value).toBe("751002");
  });
  it("shows the saved order PIN as read-only", () => {
    render(<DelhiveryServiceabilityPanel initialPincode="751001" />);

    const input = screen.getByRole("textbox", {
      name: /destination pincode/i,
    }) as HTMLInputElement;

    expect(input.value).toBe("751001");
    expect(input.readOnly).toBe(true);
  });

  it("disables checking when order PIN is missing", () => {
    render(<DelhiveryServiceabilityPanel initialPincode="" />);

    const button = screen.getByRole("button", {
      name: /check serviceability/i,
    }) as HTMLButtonElement;

    expect(button.disabled).toBe(true);
  });

  it("disables checking for an invalid PIN", () => {
    render(<DelhiveryServiceabilityPanel initialPincode="000000" />);

    const button = screen.getByRole("button", {
      name: /check serviceability/i,
    }) as HTMLButtonElement;

    expect(button.disabled).toBe(true);
  });

  it("checks only the saved order PIN and does not claim COD support", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        pincode: "751001",
        serviceable: true,
        prepaid: true,
        cod: "UNKNOWN",
      }),
    });

    globalThis.fetch = fetchMock as typeof fetch;

    render(<DelhiveryServiceabilityPanel initialPincode="751001" />);

    fireEvent.click(
      screen.getByRole("button", {
        name: /check serviceability/i,
      })
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/admin/delhivery/serviceability/751001",
      { method: "GET", cache: "no-store" }
    );

    expect(
      await screen.findByText(/prepaid delivery verified/i)
    ).toBeTruthy();

    expect(screen.queryByText(/COD availability/i)).toBeNull();
  });

  it("rejects a successful response for a different PIN", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        pincode: "751002",
        serviceable: true,
        prepaid: true,
      }),
    }) as typeof fetch;

    render(<DelhiveryServiceabilityPanel initialPincode="751001" />);

    fireEvent.click(
      screen.getByRole("button", {
        name: /check serviceability/i,
      })
    );

    expect(
      await screen.findByText(/unexpected serviceability response/i)
    ).toBeTruthy();

    expect(
      screen.queryByText(/prepaid delivery verified/i)
    ).toBeNull();
  });

  it("treats HTTP 503 as unconfirmed, not confirmed NSZ", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
    }) as typeof fetch;

    render(<DelhiveryServiceabilityPanel initialPincode="751001" />);

    fireEvent.click(
      screen.getByRole("button", {
        name: /check serviceability/i,
      })
    );

    expect(
      await screen.findByText(/could not be confirmed/i)
    ).toBeTruthy();

    expect(
      screen.queryByText(/prepaid delivery verified/i)
    ).toBeNull();
  });
});