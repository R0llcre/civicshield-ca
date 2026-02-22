export type AskAction =
  | { type: "open_verify"; label: string }
  | { type: "open_help"; label: string }
  | { type: "open_official_url"; label: string; payload?: { url?: string } };

export type AskCitation = { title: string; url: string };

export type ChatMsg = {
  id: string;
  role: "user" | "assistant";
  content: string;
  citations?: AskCitation[];
  actions?: AskAction[];
  ts: number;
};

export type AskThreadMeta = {
  id: string;
  title: string;
  createdAt: number;
  updatedAt: number;
  messageCount: number;
  lastPreview: string;
};

export type AskThreadsIndex = {
  version: 2;
  activeThreadId: string;
  threads: AskThreadMeta[];
};

export type KeyValueStore = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export const THREADS_INDEX_KEY = "regshield.ask.threads.v2";
export const THREAD_KEY_PREFIX = "regshield.ask.thread.v2:";

const MAX_THREADS = 30;
const MAX_MESSAGES_PER_THREAD = 50;
const MAX_PREVIEW_CHARS = 80;
const MAX_TITLE_CHARS = 48;

function threadKey(threadId: string) {
  return `${THREAD_KEY_PREFIX}${threadId}`;
}

function newId(): string {
  // Browser-first; provide a fallback for older runtimes/tests.
  const anyCrypto = globalThis.crypto as { randomUUID?: () => string } | undefined;
  if (anyCrypto?.randomUUID) return anyCrypto.randomUUID();
  return `t_${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function condenseWhitespace(s: string) {
  return s.replace(/\s+/g, " ").trim();
}

function safeParseJson<T>(raw: string | null): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function isThreadsIndex(x: unknown): x is AskThreadsIndex {
  if (!x || typeof x !== "object") return false;
  const r = x as Record<string, unknown>;
  if (r.version !== 2) return false;
  if (typeof r.activeThreadId !== "string" || !r.activeThreadId) return false;
  if (!Array.isArray(r.threads)) return false;
  return true;
}

function capMessages(msgs: ChatMsg[]): ChatMsg[] {
  if (msgs.length <= MAX_MESSAGES_PER_THREAD) return msgs;
  return msgs.slice(msgs.length - MAX_MESSAGES_PER_THREAD);
}

function deriveTitleFromMessages(msgs: ChatMsg[]): string {
  const firstUser = msgs.find((m) => m.role === "user")?.content;
  const fromUser = firstUser ? condenseWhitespace(firstUser).slice(0, MAX_TITLE_CHARS) : "";
  return fromUser || "Chat 1";
}

function buildMeta(threadId: string, title: string, msgs: ChatMsg[], now: number): AskThreadMeta {
  const createdAt = msgs[0]?.ts ?? now;
  const updatedAt = msgs[msgs.length - 1]?.ts ?? now;
  const lastPreview = condenseWhitespace(msgs[msgs.length - 1]?.content ?? "").slice(0, MAX_PREVIEW_CHARS);
  return {
    id: threadId,
    title: condenseWhitespace(title).slice(0, MAX_TITLE_CHARS) || "Chat 1",
    createdAt,
    updatedAt,
    messageCount: msgs.length,
    lastPreview,
  };
}

function normalizeThreads(threads: AskThreadMeta[]): AskThreadMeta[] {
  // Keep stable chronological order by createdAt.
  const sorted = [...threads].sort((a, b) => a.createdAt - b.createdAt);
  if (sorted.length <= MAX_THREADS) return sorted;
  return sorted.slice(sorted.length - MAX_THREADS);
}

export function loadThreadsIndex(storage: KeyValueStore): AskThreadsIndex | null {
  const idx = safeParseJson<AskThreadsIndex>(storage.getItem(THREADS_INDEX_KEY));
  return idx && isThreadsIndex(idx) ? idx : null;
}

export function saveThreadsIndex(storage: KeyValueStore, idx: AskThreadsIndex) {
  storage.setItem(THREADS_INDEX_KEY, JSON.stringify(idx));
}

export function loadThreadMessages(storage: KeyValueStore, threadId: string): ChatMsg[] {
  const msgs = safeParseJson<ChatMsg[]>(storage.getItem(threadKey(threadId)));
  return Array.isArray(msgs) ? msgs : [];
}

export function saveThreadMessages(storage: KeyValueStore, threadId: string, msgs: ChatMsg[]) {
  storage.setItem(threadKey(threadId), JSON.stringify(capMessages(msgs)));
}

export function deleteThread(storage: KeyValueStore, threadId: string) {
  storage.removeItem(threadKey(threadId));
}

export function ensureThreadsV2(storage: KeyValueStore, now: number = Date.now()): AskThreadsIndex {
  const existing = loadThreadsIndex(storage);
  if (existing) return existing;

  // Migration from v1 per-locale single chat.
  const v1Keys = ["en", "es", "zh-Hans"].map((l) => `regshield.ask.chat.v1:${l}`);
  let migrated: ChatMsg[] | null = null;
  for (const k of v1Keys) {
    const msgs = safeParseJson<ChatMsg[]>(storage.getItem(k));
    if (Array.isArray(msgs) && msgs.length) {
      migrated = msgs;
      break;
    }
  }

  const threadId = newId();
  const msgs = capMessages(migrated ?? []);
  const title = migrated ? deriveTitleFromMessages(msgs) : "Chat 1";
  saveThreadMessages(storage, threadId, msgs);
  // Best-effort cleanup of old v1 keys.
  for (const k of v1Keys) storage.removeItem(k);

  const meta = buildMeta(threadId, title, msgs, now);
  const idx: AskThreadsIndex = {
    version: 2,
    activeThreadId: threadId,
    threads: [meta],
  };
  saveThreadsIndex(storage, idx);
  return idx;
}

export function createThread(storage: KeyValueStore, title: string, now: number = Date.now()): AskThreadsIndex {
  const idx = ensureThreadsV2(storage, now);
  const threadId = newId();
  saveThreadMessages(storage, threadId, []);

  const meta = buildMeta(threadId, title, [], now);
  const nextThreads = normalizeThreads([...idx.threads, meta]);
  const kept = new Set(nextThreads.map((t) => t.id));
  for (const t of idx.threads) {
    if (!kept.has(t.id)) deleteThread(storage, t.id);
  }

  const next: AskThreadsIndex = { version: 2, activeThreadId: threadId, threads: nextThreads };
  saveThreadsIndex(storage, next);
  return next;
}

export function renameThreadInIndex(idx: AskThreadsIndex, threadId: string, title: string, now: number = Date.now()): AskThreadsIndex {
  const nextThreads = idx.threads.map((t) => (t.id === threadId ? { ...t, title: condenseWhitespace(title).slice(0, MAX_TITLE_CHARS) || t.title, updatedAt: now } : t));
  return { ...idx, threads: nextThreads };
}

export function updateThreadMetaInIndex(idx: AskThreadsIndex, threadId: string, title: string, msgs: ChatMsg[], now: number = Date.now()): AskThreadsIndex {
  const nextMeta = buildMeta(threadId, title, msgs, now);
  const nextThreads = idx.threads.map((t) => (t.id === threadId ? nextMeta : t));
  return { ...idx, threads: nextThreads };
}

export function exportThreadText(title: string, msgs: ChatMsg[]): string {
  const header = `${title}\nInformation only. Not legal advice.\n\n`;
  const lines: string[] = [header];
  for (const m of msgs) {
    const role = m.role === "user" ? "user" : "assistant";
    lines.push(`[${new Date(m.ts).toISOString()}] ${role}:`);
    lines.push(m.content.trim());
    lines.push("");
    if (role === "assistant" && m.citations?.length) {
      lines.push("Sources:");
      for (const c of m.citations.slice(0, 6)) {
        lines.push(`- ${c.title}: ${c.url}`);
      }
      lines.push("");
    }
  }
  const out = lines.join("\n").trim();
  return out.length > 120_000 ? `${out.slice(0, 120_000)}\n\n[Truncated]\n` : out;
}
