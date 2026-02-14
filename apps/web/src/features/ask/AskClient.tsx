"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { Download, ExternalLink, MessageSquare, Pencil, Plus, Send, Trash2, X } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import * as React from "react";

import { SectionHeader } from "@/components/ui/AppShell";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { cn } from "@/lib/cn";
import { Link } from "@/i18n/navigation";
import type { AppLocale } from "@/i18n/routing";

import {
  createThread,
  deleteThread,
  ensureThreadsV2,
  exportThreadText,
  loadThreadMessages,
  renameThreadInIndex,
  saveThreadMessages,
  saveThreadsIndex,
  updateThreadMetaInIndex,
  type AskAction,
  type AskThreadMeta,
  type AskThreadsIndex,
  type ChatMsg,
} from "./askStorage";

type AskApiResponse = {
  answer_markdown: string;
  citations: Array<{ title: string; url: string }>;
  suggested_actions: Array<{ type: string; label: string; payload?: { url?: string } }>;
  disclaimer: string;
  error?: string;
};

const CONSENT_KEY = "regshield.ask.consent.v1";

function safeParseJson<T>(raw: string | null): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

function trimToLast<T>(arr: T[], n: number): T[] {
  if (arr.length <= n) return arr;
  return arr.slice(arr.length - n);
}

function toApiMessages(msgs: ChatMsg[]): Array<{ role: "user" | "assistant"; content: string }> {
  return msgs
    .filter((m) => m.role === "user" || m.role === "assistant")
    .map((m) => ({ role: m.role, content: m.content }));
}

function normalizeActions(
  raw: AskApiResponse["suggested_actions"] | undefined,
): AskAction[] {
  if (!raw || !raw.length) return [];
  return raw
    .map((a): AskAction | null => {
      if (a.type === "open_verify") return { type: "open_verify", label: a.label };
      if (a.type === "open_help") return { type: "open_help", label: a.label };
      if (a.type === "open_official_url") return { type: "open_official_url", label: a.label, payload: a.payload };
      return null;
    })
    .filter((a): a is AskAction => a !== null);
}

export function AskClient() {
  const t = useTranslations("Ask");
  const tNav = useTranslations("Nav");
  const locale = useLocale() as AppLocale;

  const [consented, setConsented] = React.useState<boolean>(false);
  const [consentOpen, setConsentOpen] = React.useState(false);

  const [threadsIndex, setThreadsIndex] = React.useState<AskThreadsIndex | null>(null);
  const [activeThreadId, setActiveThreadId] = React.useState<string | null>(null);
  const [messages, setMessages] = React.useState<ChatMsg[]>([]);
  const [draft, setDraft] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [threadsOpen, setThreadsOpen] = React.useState(false);
  const [threadQuery, setThreadQuery] = React.useState("");
  const [renamingId, setRenamingId] = React.useState<string | null>(null);
  const [renameDraft, setRenameDraft] = React.useState("");

  React.useEffect(() => {
    const c = safeParseJson<boolean>(localStorage.getItem(CONSENT_KEY));
    setConsented(Boolean(c));
    // v2: multi-chat threads (not locale-scoped).
    const idx = ensureThreadsV2(localStorage);
    setThreadsIndex(idx);
    setActiveThreadId(idx.activeThreadId);
    setMessages(loadThreadMessages(localStorage, idx.activeThreadId));
  }, []);

  React.useEffect(() => {
    localStorage.setItem(CONSENT_KEY, JSON.stringify(consented));
  }, [consented]);

  function activeMeta(): AskThreadMeta | null {
    if (!threadsIndex || !activeThreadId) return null;
    return threadsIndex.threads.find((t) => t.id === activeThreadId) ?? null;
  }

  function setIndex(next: AskThreadsIndex) {
    setThreadsIndex(next);
    setActiveThreadId(next.activeThreadId);
    saveThreadsIndex(localStorage, next);
  }

  function switchThread(threadId: string) {
    if (!threadsIndex) return;
    const next = { ...threadsIndex, activeThreadId: threadId };
    setIndex(next);
    setMessages(loadThreadMessages(localStorage, threadId));
    setThreadsOpen(false);
    setError(null);
  }

  function doNewChat() {
    const next = createThread(localStorage, "New chat");
    setThreadsIndex(next);
    setActiveThreadId(next.activeThreadId);
    setMessages([]);
    setError(null);
    setThreadsOpen(false);
    setRenamingId(null);
  }

  function doClearCurrent() {
    if (!activeThreadId || !threadsIndex) return;
    setMessages([]);
    saveThreadMessages(localStorage, activeThreadId, []);
    const meta = threadsIndex.threads.find((t) => t.id === activeThreadId);
    const title = meta?.title ?? "Chat";
    setIndex(updateThreadMetaInIndex(threadsIndex, activeThreadId, title, [], Date.now()));
    setError(null);
  }

  function doDeleteThread(threadId: string) {
    if (!threadsIndex) return;
    deleteThread(localStorage, threadId);
    const remaining = threadsIndex.threads.filter((t) => t.id !== threadId);
    if (!remaining.length) {
      const next = createThread(localStorage, "Chat 1");
      setThreadsIndex(next);
      setActiveThreadId(next.activeThreadId);
      setMessages([]);
      return;
    }
    const nextActive = activeThreadId === threadId ? remaining[remaining.length - 1]!.id : (activeThreadId ?? remaining[remaining.length - 1]!.id);
    const next: AskThreadsIndex = { version: 2, activeThreadId: nextActive, threads: remaining };
    setIndex(next);
    if (activeThreadId === threadId) {
      setMessages(loadThreadMessages(localStorage, nextActive));
    }
    setError(null);
  }

  function doExportCurrent() {
    const meta = activeMeta();
    const title = meta?.title ?? "Chat";
    const text = exportThreadText(title, messages);
    const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    const safeName = title.replace(/[^\w\s.-]+/g, "").trim().slice(0, 40) || "chat";
    a.download = `${safeName}.txt`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function sendNow() {
    const q = draft.trim();
    if (!q) return;
    let workingIdx = threadsIndex ?? ensureThreadsV2(localStorage);
    const threadId = activeThreadId ?? workingIdx.activeThreadId;
    if (!threadsIndex) {
      setThreadsIndex(workingIdx);
      setActiveThreadId(threadId);
    }

    setError(null);
    setLoading(true);

    const next: ChatMsg[] = [
      ...messages,
      { id: crypto.randomUUID(), role: "user", content: q, ts: Date.now() },
    ];
    const trimmedLocal = trimToLast(next, 50);
    setMessages(trimmedLocal);
    saveThreadMessages(localStorage, threadId, trimmedLocal);
    {
      const meta = workingIdx.threads.find((t) => t.id === threadId);
      const title = meta?.title ?? "Chat";
      workingIdx = updateThreadMetaInIndex(workingIdx, threadId, title, trimmedLocal, Date.now());
      setIndex(workingIdx);
    }
    setDraft("");

    try {
      const apiMsgs = trimToLast(toApiMessages(trimmedLocal), 20);
      const res = await fetch("/api/ask", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ locale, messages: apiMsgs }),
      });
      const data = (await res.json().catch(() => null)) as AskApiResponse | null;
      if (!res.ok) {
        const msg = data?.error || (res.status === 501 ? t("errorNotConfigured") : t("errorTryAgain"));
        throw new Error(msg);
      }
      if (!data?.answer_markdown) throw new Error(t("errorTryAgain"));

      const assistant: ChatMsg = {
        id: crypto.randomUUID(),
        role: "assistant",
        content: data.answer_markdown,
        citations: data.citations ?? [],
        actions: normalizeActions(data.suggested_actions),
        ts: Date.now(),
      };
      const nextMsgs = trimToLast([...trimmedLocal, assistant], 50);
      setMessages(nextMsgs);
      saveThreadMessages(localStorage, threadId, nextMsgs);
      {
        const meta = workingIdx.threads.find((t) => t.id === threadId);
        const title = meta?.title ?? "Chat";
        workingIdx = updateThreadMetaInIndex(workingIdx, threadId, title, nextMsgs, Date.now());
        setIndex(workingIdx);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : t("errorTryAgain"));
      // Keep the user's message; they can retry.
    } finally {
      setLoading(false);
    }
  }

  function onSend() {
    if (!consented) {
      setConsentOpen(true);
      return;
    }
    void sendNow();
  }

  function clearChat() {
    doClearCurrent();
    setDraft("");
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[1.08fr_0.92fr]">
      <Card variant="highlight" className="animate-rise-in overflow-hidden">
        <CardHeader>
          <SectionHeader
            title={t("chatTitle")}
            subtitle={t("disclaimerBody")}
            action={
              <div className="flex flex-wrap items-center justify-end gap-2">
                <Button variant="secondary" size="sm" onClick={() => setThreadsOpen(true)}>
                  <MessageSquare className="h-4 w-4" />
                  {t("chats")}
                </Button>
                <Button variant="secondary" size="sm" onClick={doNewChat}>
                  <Plus className="h-4 w-4" />
                  {t("newChat")}
                </Button>
                <Button variant="secondary" size="sm" onClick={doExportCurrent} disabled={messages.length === 0}>
                  <Download className="h-4 w-4" />
                  {t("export")}
                </Button>
                <Button variant="secondary" size="sm" onClick={clearChat} disabled={messages.length === 0}>
                  <Trash2 className="h-4 w-4" />
                  {t("clear")}
                </Button>
              </div>
            }
          />
        </CardHeader>
        <CardContent className="flex min-h-[420px] flex-col gap-4">
          <div className="flex-1 space-y-2 overflow-auto rounded-2xl border border-[color:var(--color-surface-border)] bg-white/70 p-3 shadow-[var(--shadow-sm)]">
            {messages.length === 0 ? (
              <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] px-4 py-3 text-sm text-[color:var(--color-muted)]">
                {t("empty")}
              </div>
            ) : null}
            {messages.map((m) => (
              <div
                key={m.id}
                className={cn(
                  "max-w-[92%] rounded-2xl border px-4 py-3 text-sm leading-6 shadow-[var(--shadow-sm)]",
                  m.role === "user"
                    ? "ml-auto border-transparent bg-[color:var(--color-accent)] text-white"
                    : "border-[color:var(--color-surface-border)] bg-white/90 text-[color:var(--color-foreground)]",
                )}
              >
                <div className="whitespace-pre-wrap">{m.content}</div>

                {m.role === "assistant" && m.citations && m.citations.length ? (
                  <div className="mt-3 border-t border-[color:var(--color-surface-border)] pt-3">
                    <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[color:var(--color-muted)]">
                      {t("sources")}
                    </div>
                    <ul className="mt-2 space-y-1.5">
                      {m.citations.slice(0, 6).map((c) => (
                        <li key={`${c.url}-${c.title}`}>
                          <a
                            href={c.url}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-2 rounded-xl border border-[color:var(--color-surface-border)] bg-white px-3 py-1.5 text-xs font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
                          >
                            <ExternalLink className="h-3.5 w-3.5" />
                            {c.title}
                          </a>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}

                {m.role === "assistant" && m.actions && m.actions.length ? (
                  <div className="mt-3 flex flex-wrap gap-2">
                    {m.actions.slice(0, 6).map((a, idx) => {
                      if (a.type === "open_verify") {
                        return (
                          <Link
                            key={`${a.type}-${idx}`}
                            href="/verify"
                            className="inline-flex h-9 items-center rounded-2xl bg-[color:var(--color-accent-soft)] px-3 text-xs font-semibold text-[color:var(--color-accent)] hover:brightness-105"
                          >
                            {a.label || tNav("verify")}
                          </Link>
                        );
                      }
                      if (a.type === "open_help") {
                        return (
                          <Link
                            key={`${a.type}-${idx}`}
                            href="/help"
                            className="inline-flex h-9 items-center rounded-2xl bg-[color:var(--color-accent-soft)] px-3 text-xs font-semibold text-[color:var(--color-accent)] hover:brightness-105"
                          >
                            {a.label || tNav("help")}
                          </Link>
                        );
                      }
                      const url = a.payload?.url;
                      if (a.type === "open_official_url" && url) {
                        return (
                          <a
                            key={`${a.type}-${idx}`}
                            href={url}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex h-9 items-center rounded-2xl border border-[color:var(--color-surface-border)] bg-white/80 px-3 text-xs font-semibold text-[color:var(--color-foreground)] hover:bg-white"
                          >
                            {a.label}
                          </a>
                        );
                      }
                      return null;
                    })}
                  </div>
                ) : null}
              </div>
            ))}
          </div>

          {error ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/70 p-3 shadow-[var(--shadow-sm)]">
            <label className="space-y-2">
              <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                {t("inputLabel")}
              </div>
              <textarea
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder={t("inputPlaceholder")}
                rows={3}
                className={cn(
                  "w-full resize-none rounded-2xl border bg-[color:var(--color-surface-strong)] px-3.5 py-3 text-sm text-[color:var(--color-foreground)]",
                  "border-[color:var(--color-surface-border)] shadow-[var(--shadow-sm)]",
                  "placeholder:text-[color:var(--color-muted-2)]",
                  "focus:border-[color:var(--color-focus)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]/35",
                )}
              />
            </label>
            <div className="mt-3 flex items-center justify-between gap-3">
              <div className="text-xs text-[color:var(--color-muted-2)]">
                <div>{t("disclaimerInline")}</div>
                <div>{t("contextHint")}</div>
              </div>
              <Button onClick={onSend} disabled={loading || !draft.trim()} size="lg">
                <Send className="h-4 w-4" />
                {loading ? t("sending") : t("send")}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card variant="elevated" className="animate-rise-in">
        <CardHeader>
          <SectionHeader title={t("tipsTitle")} subtitle={t("tipsBody")} />
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-[color:var(--color-muted)]">
          <ul className="space-y-2 leading-6">
            <li>{t("tip1")}</li>
            <li>{t("tip2")}</li>
            <li>{t("tip3")}</li>
          </ul>
          <div className="flex flex-wrap gap-2 pt-2">
            <Link
              href="/verify"
              className="inline-flex h-10 items-center rounded-2xl bg-[color:var(--color-accent)] px-4 text-sm font-semibold text-white shadow-[var(--shadow-sm)] hover:brightness-105"
            >
              {tNav("verify")}
            </Link>
            <Link
              href="/help"
              className="inline-flex h-10 items-center rounded-2xl border border-[color:var(--color-surface-border)] bg-white/85 px-4 text-sm font-semibold text-[color:var(--color-foreground)] hover:bg-white"
            >
              {tNav("help")}
            </Link>
          </div>
        </CardContent>
      </Card>

      <Dialog.Root
        open={threadsOpen}
        onOpenChange={(open) => {
          setThreadsOpen(open);
          if (!open) {
            setThreadQuery("");
            setRenamingId(null);
            setRenameDraft("");
          }
        }}
      >
        <Dialog.Portal>
          <Dialog.Overlay className="fixed inset-0 bg-slate-900/35 backdrop-blur-sm" />
          <Dialog.Content
            className={cn(
              "fixed left-1/2 top-1/2 w-[min(720px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-[28px] border",
              "border-[color:var(--color-surface-border)] bg-white/95 p-6 shadow-[var(--shadow-lg)] backdrop-blur",
            )}
          >
            <div className="flex items-center justify-between gap-3">
              <Dialog.Title className="font-[family-name:var(--font-display)] text-lg font-bold">
                {t("chats")}
              </Dialog.Title>
              <Dialog.Close asChild>
                <Button variant="secondary" size="sm">
                  <X className="h-4 w-4" />
                </Button>
              </Dialog.Close>
            </div>

            <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
              <input
                value={threadQuery}
                onChange={(e) => setThreadQuery(e.target.value)}
                placeholder={t("searchChats")}
                className={cn(
                  "h-10 w-full rounded-2xl border bg-[color:var(--color-surface-strong)] px-3 text-sm",
                  "border-[color:var(--color-surface-border)] shadow-[var(--shadow-sm)]",
                  "placeholder:text-[color:var(--color-muted-2)]",
                  "focus:border-[color:var(--color-focus)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]/35",
                )}
              />
              <Button onClick={doNewChat} className="sm:shrink-0">
                <Plus className="h-4 w-4" />
                {t("newChat")}
              </Button>
            </div>

            <div className="mt-4 max-h-[52vh] space-y-2 overflow-auto rounded-2xl border border-[color:var(--color-surface-border)] bg-white/70 p-3">
              {(() => {
                const list = threadsIndex?.threads ?? [];
                const q = threadQuery.trim().toLowerCase();
                const filtered =
                  q.length === 0
                    ? list
                    : list.filter(
                        (th) =>
                          th.title.toLowerCase().includes(q) || th.lastPreview.toLowerCase().includes(q),
                      );

                if (filtered.length === 0) {
                  return (
                    <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] px-4 py-3 text-sm text-[color:var(--color-muted)]">
                      {t("noChatsMatch")}
                    </div>
                  );
                }

                return filtered.map((th) => {
                  const isActive = th.id === activeThreadId;
                  const isRenaming = renamingId === th.id;
                  return (
                    <div
                      key={th.id}
                      className={cn(
                        "rounded-2xl border bg-white/90 px-4 py-3 shadow-[var(--shadow-sm)]",
                        "border-[color:var(--color-surface-border)]",
                        isActive ? "ring-2 ring-[color:var(--color-accent)]/25" : "",
                      )}
                    >
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <div className="min-w-0">
                          {isRenaming ? (
                            <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                              <input
                                value={renameDraft}
                                onChange={(e) => setRenameDraft(e.target.value)}
                                placeholder={t("renamePlaceholder")}
                                className={cn(
                                  "h-10 w-full rounded-2xl border bg-[color:var(--color-surface-strong)] px-3 text-sm",
                                  "border-[color:var(--color-surface-border)] shadow-[var(--shadow-sm)]",
                                  "focus:border-[color:var(--color-focus)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]/35",
                                )}
                              />
                              <div className="flex items-center gap-2">
                                <Button
                                  size="sm"
                                  onClick={() => {
                                    if (!threadsIndex) return;
                                    const next = renameThreadInIndex(threadsIndex, th.id, renameDraft, Date.now());
                                    setIndex(next);
                                    setRenamingId(null);
                                    setRenameDraft("");
                                  }}
                                  disabled={!renameDraft.trim()}
                                >
                                  {t("save")}
                                </Button>
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  onClick={() => {
                                    setRenamingId(null);
                                    setRenameDraft("");
                                  }}
                                >
                                  {t("cancel")}
                                </Button>
                              </div>
                            </div>
                          ) : (
                            <>
                              <div className="truncate text-sm font-semibold text-[color:var(--color-foreground)]">
                                {th.title}
                              </div>
                              {th.lastPreview ? (
                                <div className="mt-1 line-clamp-2 text-xs leading-5 text-[color:var(--color-muted)]">
                                  {th.lastPreview}
                                </div>
                              ) : null}
                              <div className="mt-2 text-[11px] text-[color:var(--color-muted-2)]">
                                {new Date(th.updatedAt).toLocaleString()} · {th.messageCount} msgs
                              </div>
                            </>
                          )}
                        </div>

                        {!isRenaming ? (
                          <div className="flex items-center gap-2 sm:shrink-0">
                            <Button
                              size="sm"
                              variant={isActive ? "secondary" : "primary"}
                              onClick={() => switchThread(th.id)}
                            >
                              {isActive ? t("active") : t("open")}
                            </Button>
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => {
                                setRenamingId(th.id);
                                setRenameDraft(th.title);
                              }}
                            >
                              <Pencil className="h-4 w-4" />
                              {t("rename")}
                            </Button>
                            <Button
                              size="sm"
                              variant="secondary"
                              onClick={() => {
                                if (confirm(t("confirmDelete"))) {
                                  doDeleteThread(th.id);
                                }
                              }}
                            >
                              <Trash2 className="h-4 w-4" />
                              {t("delete")}
                            </Button>
                          </div>
                        ) : null}
                      </div>
                    </div>
                  );
                });
              })()}
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>

      <Dialog.Root open={consentOpen} onOpenChange={setConsentOpen}>
        <Dialog.Portal>
          <Dialog.Overlay className="fixed inset-0 bg-slate-900/35 backdrop-blur-sm" />
          <Dialog.Content
            className={cn(
              "fixed left-1/2 top-1/2 w-[min(560px,92vw)] -translate-x-1/2 -translate-y-1/2 rounded-[28px] border",
              "border-[color:var(--color-surface-border)] bg-white/95 p-6 shadow-[var(--shadow-lg)] backdrop-blur",
            )}
          >
            <Dialog.Title className="font-[family-name:var(--font-display)] text-lg font-bold">
              {t("consentTitle")}
            </Dialog.Title>
            <Dialog.Description className="mt-2 text-sm leading-6 text-[color:var(--color-muted)]">
              {t("consentBody")}
            </Dialog.Description>
            <div className="mt-5 flex items-center justify-end gap-2">
              <Dialog.Close asChild>
                <Button variant="secondary">{t("consentCancel")}</Button>
              </Dialog.Close>
              <Button
                onClick={() => {
                  setConsented(true);
                  setConsentOpen(false);
                  void sendNow();
                }}
              >
                {t("consentAgree")}
              </Button>
            </div>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </div>
  );
}
