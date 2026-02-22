import { describe, expect, it } from "vitest";

import {
  THREAD_KEY_PREFIX,
  createThread,
  ensureThreadsV2,
  loadThreadMessages,
  loadThreadsIndex,
  saveThreadMessages,
  type ChatMsg,
  type KeyValueStore,
} from "./askStorage";

class MemStore implements KeyValueStore {
  private m = new Map<string, string>();
  getItem(key: string) {
    return this.m.has(key) ? (this.m.get(key) as string) : null;
  }
  setItem(key: string, value: string) {
    this.m.set(key, value);
  }
  removeItem(key: string) {
    this.m.delete(key);
  }
  keys(): string[] {
    return [...this.m.keys()];
  }
}

describe("askStorage", () => {
  it("migrates v1 per-locale chat into a single v2 thread", () => {
    const s = new MemStore();
    const v1Key = "regshield.ask.chat.v1:en";
    const v1Msgs: ChatMsg[] = [
      { id: "u1", role: "user", content: "How do I verify an accredited representative in CA?", ts: 100 },
      { id: "a1", role: "assistant", content: "Use the DOJ roster.", ts: 200 },
    ];
    s.setItem(v1Key, JSON.stringify(v1Msgs));

    const idx = ensureThreadsV2(s, 999);
    expect(idx.version).toBe(2);
    expect(idx.threads).toHaveLength(1);
    expect(idx.activeThreadId).toBeTruthy();
    expect(s.getItem(v1Key)).toBeNull();

    const savedIdx = loadThreadsIndex(s);
    expect(savedIdx?.activeThreadId).toBe(idx.activeThreadId);
    const threadKey = `${THREAD_KEY_PREFIX}${idx.activeThreadId}`;
    expect(s.getItem(threadKey)).toBeTruthy();

    const savedMsgs = loadThreadMessages(s, idx.activeThreadId);
    expect(savedMsgs).toHaveLength(2);
    expect(savedIdx?.threads[0]?.title.toLowerCase()).toContain("verify");
  });

  it("createThread caps messages to 50 and keeps active thread in index", () => {
    const s = new MemStore();
    const idx = ensureThreadsV2(s, 1000);
    const next = createThread(s, "New chat", 2000);
    expect(next.activeThreadId).not.toBe(idx.activeThreadId);
    expect(next.threads.length).toBeGreaterThanOrEqual(1);

    const tid = next.activeThreadId;
    const msgs: ChatMsg[] = Array.from({ length: 80 }).map((_, i) => ({
      id: `m${i}`,
      role: i % 2 === 0 ? "user" : "assistant",
      content: `msg ${i}`,
      ts: 3000 + i,
    }));
    saveThreadMessages(s, tid, msgs);
    const loaded = loadThreadMessages(s, tid);
    expect(loaded).toHaveLength(50);
    expect(loaded[0]?.id).toBe("m30");
  });
});
