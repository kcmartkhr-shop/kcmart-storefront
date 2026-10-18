/* @vitest-environment jsdom */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import DelhiveryLabelEligibilityPanel from "../components/admin/delivery/DelhiveryLabelEligibilityPanel";

const endpoint = "/api/admin/orders/123/delhivery-label-eligibility";

function response(status: number, body: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

describe("Delhivery Label Eligibility Panel", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it("shows Ready for confirmed eligibility", async () => {
    fetchMock.mockResolvedValueOnce(
      response(200, { eligible: true, blocker: null })
    );

    render(<DelhiveryLabelEligibilityPanel orderId={123} />);

    expect(
      await screen.findByText(/Ready.*confirmed Delhivery booking/i)
    ).toBeTruthy();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe(endpoint);
    expect(fetchMock.mock.calls[0][1]).toMatchObject({
      method: "GET",
      cache: "no-store",
      credentials: "same-origin",
    });

    expect(screen.queryByRole("button")).toBeNull();
  });

  it("shows exact backend blocker", async () => {
    fetchMock.mockResolvedValueOnce(
      response(200, {
        eligible: false,
        blocker: "Delhivery booking is not confirmed.",
      })
    );

    render(<DelhiveryLabelEligibilityPanel orderId={123} />);

    expect(await screen.findByText("Blocked")).toBeTruthy();

    expect(
      screen.getByText("Delhivery booking is not confirmed.")
    ).toBeTruthy();

    expect(screen.queryByText(/^Ready/)).toBeNull();
  });

  it("does not claim readiness after HTTP failure", async () => {
    fetchMock.mockResolvedValueOnce(response(503, {}));

    render(<DelhiveryLabelEligibilityPanel orderId={123} />);

    expect(
      await screen.findByText(
        "Unable to check label eligibility right now."
      )
    ).toBeTruthy();

    expect(screen.queryByText(/^Ready/)).toBeNull();
  });

  it("rejects invalid eligibility response", async () => {
    fetchMock.mockResolvedValueOnce(
      response(200, { eligible: "true", blocker: null })
    );

    render(<DelhiveryLabelEligibilityPanel orderId={123} />);

    expect(
      await screen.findByText(
        "Unable to check label eligibility right now."
      )
    ).toBeTruthy();

    expect(screen.queryByText(/^Ready/)).toBeNull();
  });

  it("never sends POST, PUT, PATCH or DELETE", async () => {
    fetchMock.mockResolvedValueOnce(
      response(200, { eligible: false, blocker: "Not booked." })
    );

    render(<DelhiveryLabelEligibilityPanel orderId={123} />);

    await screen.findByText("Not booked.");

    expect(fetchMock).toHaveBeenCalledTimes(1);

    for (const [, options] of fetchMock.mock.calls) {
      expect(options.method).toBe("GET");
    }

    expect(screen.queryByRole("button")).toBeNull();
  });

  it("checks the new order when orderId changes", async () => {
    fetchMock
      .mockResolvedValueOnce(
        response(200, { eligible: false, blocker: "First order blocked." })
      )
      .mockResolvedValueOnce(
        response(200, { eligible: true, blocker: null })
      );

    const view = render(
      <DelhiveryLabelEligibilityPanel orderId={123} />
    );

    expect(
      await screen.findByText("First order blocked.")
    ).toBeTruthy();

    view.rerender(
      <DelhiveryLabelEligibilityPanel orderId={456} />
    );

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledTimes(2);
    });

    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/admin/orders/456/delhivery-label-eligibility"
    );

    expect(
      await screen.findByText(/Ready.*confirmed Delhivery booking/i)
    ).toBeTruthy();

    expect(screen.queryByText("First order blocked.")).toBeNull();
  });
});