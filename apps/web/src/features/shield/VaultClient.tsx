"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { Download, FileDown, FileUp, Lock, Trash2 } from "lucide-react";
import * as React from "react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";
import { vaultDb, type VaultItemMeta } from "@/features/shield/vault/db";
import { decryptPayload, encryptPayload, deriveKey } from "@/features/shield/vault/crypto";
import { exportVaultZip, importVaultZip } from "@/features/shield/vault/zip";

type Labels = {
  newTitle: string;
  unlockTitle: string;
  passphraseLabel: string;
  passphraseHint: string;
  createButton: string;
  unlockButton: string;
  lockButton: string;
  addFiles: string;
  export: string;
  import: string;
  itemsTitle: string;
  empty: string;
  download: string;
  delete: string;
  importDialogTitle: string;
  importDialogBody: string;
  importConfirm: string;
  cancel: string;
  addFilesFailed: string;
  exportFailed: string;
  wrongPassphrase: string;
  invalidFile: string;
};

type VaultState =
  | { kind: "locked"; hasVault: boolean }
  | { kind: "unlocked"; key: CryptoKey; meta: VaultItemMeta[] };

type VaultPlain = {
  items: VaultItemMeta[];
  files: Array<{ id: string; bytesB64: string }>;
};

export function EvidenceVault({ labels }: { labels: Labels }) {
  const [passphrase, setPassphrase] = React.useState("");
  const [state, setState] = React.useState<VaultState>({ kind: "locked", hasVault: false });
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [importOpen, setImportOpen] = React.useState(false);
  const fileInputRef = React.useRef<HTMLInputElement | null>(null);
  const importFileRef = React.useRef<HTMLInputElement | null>(null);

  React.useEffect(() => {
    (async () => {
      const has = await vaultDb.hasVault();
      setState({ kind: "locked", hasVault: has });
    })();
  }, []);

  async function unlock(createIfMissing: boolean) {
    setBusy(true);
    setError(null);
    try {
      const existing = await vaultDb.getParams();
      if (!existing && !createIfMissing) {
        setState({ kind: "locked", hasVault: false });
        return;
      }

      const params = existing ?? (await vaultDb.initParams());
      const key = await deriveKey(passphrase, params.saltB64, params.iterations);

      const encryptedManifest = await vaultDb.getManifest();
      if (!encryptedManifest) {
        await vaultDb.setManifest(await encryptPayload(key, { items: [] }));
      } else {
        // Verify passphrase by attempting decrypt.
        await decryptPayload(key, encryptedManifest);
      }

      const manifest = await decryptPayload<{ items: VaultItemMeta[] }>(key, (await vaultDb.getManifest())!);
      setState({ kind: "unlocked", key, meta: manifest.items });
    } catch {
      setError(labels.wrongPassphrase);
    } finally {
      setBusy(false);
    }
  }

  async function lockVault() {
    const has = await vaultDb.hasVault();
    setPassphrase("");
    setState({ kind: "locked", hasVault: has });
  }

  async function refreshManifest(key: CryptoKey) {
    const manifest = await decryptPayload<{ items: VaultItemMeta[] }>(key, (await vaultDb.getManifest())!);
    setState({ kind: "unlocked", key, meta: manifest.items });
  }

  async function addFiles(files: FileList) {
    if (state.kind !== "unlocked") return;
    setBusy(true);
    setError(null);
    try {
      const itemsToAdd: Array<{ meta: VaultItemMeta; bytes: Uint8Array }> = [];
      for (const file of Array.from(files)) {
        const bytes = new Uint8Array(await file.arrayBuffer());
        const id = crypto.randomUUID();
        const meta: VaultItemMeta = {
          id,
          createdAt: new Date().toISOString(),
          originalName: file.name,
          contentType: file.type || "application/octet-stream",
          size: bytes.byteLength,
        };
        itemsToAdd.push({ meta, bytes });
      }

      await vaultDb.addItems(state.key, itemsToAdd);
      await refreshManifest(state.key);
    } catch {
      setError(labels.addFilesFailed);
    } finally {
      setBusy(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  async function downloadItem(id: string) {
    if (state.kind !== "unlocked") return;
    setBusy(true);
    setError(null);
    try {
      const item = await vaultDb.getItem(id);
      if (!item) return;
      const bytes = await decryptPayload<Uint8Array>(state.key, item.encrypted);
      const meta = state.meta.find((m) => m.id === id);
      const ab = bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
      const blob = new Blob([ab], { type: meta?.contentType || "application/octet-stream" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = meta?.originalName || "evidence.bin";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } finally {
      setBusy(false);
    }
  }

  async function deleteItem(id: string) {
    if (state.kind !== "unlocked") return;
    setBusy(true);
    setError(null);
    try {
      await vaultDb.deleteItem(state.key, id);
      await refreshManifest(state.key);
    } finally {
      setBusy(false);
    }
  }

  async function exportZip() {
    if (state.kind !== "unlocked") return;
    setBusy(true);
    setError(null);
    try {
      const params = await vaultDb.getParams();
      if (!params) throw new Error("Missing vault params");
      const blob = await exportVaultZip(state.key, params, await vaultDb.exportPlain(state.key));
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "regshield-vault.zip";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError(labels.exportFailed);
    } finally {
      setBusy(false);
    }
  }

  async function doImport() {
    const file = importFileRef.current?.files?.[0];
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const { params, payload } = await importVaultZip(await file.arrayBuffer());
      const key = await deriveKey(passphrase, params.saltB64, params.iterations);
      const plain = await decryptPayload<VaultPlain>(key, payload);
      await vaultDb.importPlain(key, params, plain);
      setImportOpen(false);
      await refreshManifest(key);
      setState({ kind: "unlocked", key, meta: plain.items ?? [] });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "";
      if (/invalid vault|unsupported vault|invalid payload/i.test(msg)) {
        setError(labels.invalidFile);
      } else {
        setError(labels.wrongPassphrase);
      }
    } finally {
      setBusy(false);
      if (importFileRef.current) importFileRef.current.value = "";
    }
  }

  return (
    <div className="space-y-4">
      {state.kind === "locked" ? (
        <div className="space-y-4">
          <div className="soft-section rounded-2xl p-4">
            <div className="text-sm font-bold">{state.hasVault ? labels.unlockTitle : labels.newTitle}</div>
            <div className="mt-1 text-xs text-[color:var(--color-muted)]">{labels.passphraseHint}</div>
          </div>

          <label className="space-y-2">
            <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
              {labels.passphraseLabel}
            </div>
            <Input
              type="password"
              value={passphrase}
              onChange={(e) => setPassphrase(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </label>

          <div className="flex flex-wrap gap-2">
            {state.hasVault ? (
              <Button onClick={() => unlock(false)} disabled={busy || !passphrase} size="lg">
                <Lock className="h-4 w-4" />
                {labels.unlockButton}
              </Button>
            ) : (
              <Button onClick={() => unlock(true)} disabled={busy || !passphrase} size="lg" tone="teal">
                <Lock className="h-4 w-4" />
                {labels.createButton}
              </Button>
            )}

            <Dialog.Root open={importOpen} onOpenChange={setImportOpen}>
              <Dialog.Trigger asChild>
                <Button variant="secondary" disabled={busy} size="lg">
                  <FileUp className="h-4 w-4" />
                  {labels.import}
                </Button>
              </Dialog.Trigger>
              <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-slate-900/35 backdrop-blur-sm" />
                <Dialog.Content
                  className={cn(
                    "fixed left-1/2 top-1/2 w-[min(92vw,480px)] -translate-x-1/2 -translate-y-1/2",
                    "rounded-3xl border border-[color:var(--color-surface-border)] bg-white p-5 shadow-[var(--shadow-lg)]",
                  )}
                >
                  <Dialog.Title className="font-[family-name:var(--font-display)] text-lg font-bold">
                    {labels.importDialogTitle}
                  </Dialog.Title>
                  <Dialog.Description className="mt-1 text-sm text-[color:var(--color-muted)]">
                    {labels.importDialogBody}
                  </Dialog.Description>

                  <div className="mt-4 space-y-3">
                    <label className="block space-y-2">
                      <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                        {labels.import}
                      </div>
                      <input
                        ref={importFileRef}
                        type="file"
                        accept=".zip"
                        aria-label={labels.import}
                        className="w-full rounded-2xl border border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] px-3 py-2 text-sm"
                      />
                    </label>

                    <label className="block space-y-2">
                      <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                        {labels.passphraseLabel}
                      </div>
                      <Input
                        type="password"
                        value={passphrase}
                        onChange={(e) => setPassphrase(e.target.value)}
                        placeholder="••••••••"
                        aria-label={labels.passphraseLabel}
                      />
                    </label>
                    <div className="flex justify-end gap-2">
                      <Dialog.Close asChild>
                        <Button variant="ghost" disabled={busy} size="sm">
                          {labels.cancel}
                        </Button>
                      </Dialog.Close>
                      <Button onClick={doImport} disabled={busy || !passphrase} size="sm">
                        {labels.importConfirm}
                      </Button>
                    </div>
                  </div>
                </Dialog.Content>
              </Dialog.Portal>
            </Dialog.Root>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="text-sm font-bold">{labels.itemsTitle}</div>
            <div className="flex flex-wrap gap-2">
              <Button variant="secondary" onClick={exportZip} disabled={busy} size="sm">
                <FileDown className="h-4 w-4" />
                {labels.export}
              </Button>
              <Button variant="ghost" onClick={lockVault} disabled={busy} size="sm">
                <Lock className="h-4 w-4" />
                {labels.lockButton}
              </Button>
            </div>
          </div>

          <div className="soft-section rounded-2xl p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="text-sm font-semibold">{labels.addFiles}</div>
              <div>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  aria-label={labels.addFiles}
                  className="w-full max-w-[250px] rounded-2xl border border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] px-3 py-2 text-sm"
                  onChange={(e) => {
                    if (e.target.files) addFiles(e.target.files);
                  }}
                />
              </div>
            </div>
          </div>

          {error ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          {state.meta.length === 0 ? (
            <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/60 px-4 py-3 text-sm text-[color:var(--color-muted)]">
              {labels.empty}
            </div>
          ) : (
            <ul className="space-y-2">
              {state.meta
                .slice()
                .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
                .map((m) => (
                  <li
                    key={m.id}
                    className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/80 p-4 shadow-[var(--shadow-sm)]"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <div className="text-sm font-bold">{m.originalName}</div>
                        <div className="mt-1 text-xs text-[color:var(--color-muted)]">
                          {new Date(m.createdAt).toLocaleString()} · {Math.round(m.size / 1024)} KB
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button variant="secondary" onClick={() => downloadItem(m.id)} disabled={busy} size="sm">
                          <Download className="h-4 w-4" />
                          {labels.download}
                        </Button>
                        <Button variant="ghost" onClick={() => deleteItem(m.id)} disabled={busy} size="sm">
                          <Trash2 className="h-4 w-4" />
                          {labels.delete}
                        </Button>
                      </div>
                    </div>
                  </li>
                ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
