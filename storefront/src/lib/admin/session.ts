import "server-only";
import { randomBytes } from "node:crypto";

type AdminSession = {
  username: string;
  password: string;
  expiresAt: number;
};

const globalSessions = globalThis as typeof globalThis & {
  kcmartAdminSessions?: Map<string, AdminSession>;
};

const sessions =
  globalSessions.kcmartAdminSessions ??
  new Map<string, AdminSession>();

globalSessions.kcmartAdminSessions = sessions;

export function createAdminSession(
  username: string,
  password: string
) {
  const id = randomBytes(32).toString("hex");

  sessions.set(id, {
    username,
    password,
    expiresAt: Date.now() + 8 * 60 * 60 * 1000,
  });

  return id;
}

export function getAdminSession(id?: string) {
  if (!id) return null;

  const session = sessions.get(id);

  if (!session) return null;

  if (session.expiresAt < Date.now()) {
    sessions.delete(id);
    return null;
  }

  return session;
}

export function deleteAdminSession(id?: string) {
  if (id) sessions.delete(id);
}