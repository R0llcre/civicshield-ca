import { openDB } from "idb";

import { bytesToB64, b64ToBytes, decryptPayload, encryptPayload, randomSaltB64, type EncryptedBlob } from "./crypto";

export type VaultParams = {
  version: 1;
  saltB64: string;
  iterations: number;
};

export type VaultItemMeta = {
  id: string;
  createdAt: string;
  originalName: string;
  contentType: string;
  size: number;
};

type VaultManifest = { items: VaultItemMeta[] };

type DbSchema = {
  meta: {
    key: string;
    value: unknown;
  };
  items: {
    key: string;
    value: { id: string; encrypted: EncryptedBlob };
  };
};

const DB_NAME = "regshield-vault";
const DB_VERSION = 1;

async function db() {
  return openDB<DbSchema>(DB_NAME, DB_VERSION, {
    upgrade(db) {
      if (!db.objectStoreNames.contains("meta")) db.createObjectStore("meta");
      if (!db.objectStoreNames.contains("items")) db.createObjectStore("items", { keyPath: "id" });
    },
  });
}

const DEFAULT_ITERATIONS = 210_000;

export const vaultDb = {
  async hasVault() {
    const d = await db();
    const params = await d.get("meta", "params");
    const manifest = await d.get("meta", "manifest");
    return !!params || !!manifest;
  },

  async getParams(): Promise<VaultParams | null> {
    const d = await db();
    const val = (await d.get("meta", "params")) as VaultParams | undefined;
    return val ?? null;
  },

  async initParams(): Promise<VaultParams> {
    const d = await db();
    const existing = await d.get("meta", "params");
    if (existing) return existing as VaultParams;
    const params: VaultParams = { version: 1, saltB64: randomSaltB64(16), iterations: DEFAULT_ITERATIONS };
    await d.put("meta", params, "params");
    return params;
  },

  async getManifest(): Promise<EncryptedBlob | null> {
    const d = await db();
    const val = (await d.get("meta", "manifest")) as EncryptedBlob | undefined;
    return val ?? null;
  },

  async setManifest(blob: EncryptedBlob) {
    const d = await db();
    await d.put("meta", blob, "manifest");
  },

  async getItem(id: string): Promise<{ id: string; encrypted: EncryptedBlob } | null> {
    const d = await db();
    return (await d.get("items", id)) ?? null;
  },

  async addItems(
    key: CryptoKey,
    items: Array<{ meta: VaultItemMeta; bytes: Uint8Array }>,
  ) {
    const d = await db();
    const manifestBlob = (await d.get("meta", "manifest")) as EncryptedBlob | undefined;
    const manifest = manifestBlob
      ? await decryptPayload<VaultManifest>(key, manifestBlob)
      : { items: [] };

    // Pre-encrypt all items before opening the transaction (crypto is async).
    const encrypted: Array<{ id: string; encrypted: EncryptedBlob }> = [];
    for (const it of items) {
      encrypted.push({ id: it.meta.id, encrypted: await encryptPayload(key, it.bytes) });
      manifest.items.push(it.meta);
    }
    const encryptedManifest = await encryptPayload(key, manifest);

    // Write atomically inside a single transaction.
    const tx = d.transaction(["meta", "items"], "readwrite");
    for (const item of encrypted) {
      await tx.objectStore("items").put(item);
    }
    await tx.objectStore("meta").put(encryptedManifest, "manifest");
    await tx.done;
  },

  async deleteItem(key: CryptoKey, id: string) {
    const d = await db();
    await d.delete("items", id);

    const manifestBlob = (await d.get("meta", "manifest")) as EncryptedBlob | undefined;
    if (!manifestBlob) return;

    const manifest = await decryptPayload<VaultManifest>(key, manifestBlob);
    manifest.items = manifest.items.filter((m) => m.id !== id);
    await d.put("meta", await encryptPayload(key, manifest), "manifest");
  },

  async exportPlain(key: CryptoKey): Promise<{ items: VaultItemMeta[]; files: Array<{ id: string; bytesB64: string }> }> {
    const d = await db();
    const manifestBlob = (await d.get("meta", "manifest")) as EncryptedBlob | undefined;
    if (!manifestBlob) return { items: [], files: [] };

    const manifest = await decryptPayload<VaultManifest>(key, manifestBlob);
    const files: Array<{ id: string; bytesB64: string }> = [];

    for (const m of manifest.items) {
      const row = await d.get("items", m.id);
      if (!row) continue;
      const bytes = await decryptPayload<Uint8Array>(key, row.encrypted);
      files.push({ id: m.id, bytesB64: bytesToB64(bytes) });
    }

    return { items: manifest.items, files };
  },

  async importPlain(
    key: CryptoKey,
    params: VaultParams,
    plain: { items: VaultItemMeta[]; files: Array<{ id: string; bytesB64: string }> },
  ) {
    const d = await db();
    const tx = d.transaction(["meta", "items"], "readwrite");
    await tx.objectStore("items").clear();
    await tx.objectStore("meta").put(params, "params");

    const manifest: VaultManifest = { items: plain.items ?? [] };
    await tx.objectStore("meta").put(await encryptPayload(key, manifest), "manifest");

    for (const file of plain.files ?? []) {
      const bytes = b64ToBytes(file.bytesB64);
      const encrypted = await encryptPayload(key, bytes);
      await tx.objectStore("items").put({ id: file.id, encrypted });
    }

    await tx.done;
  },
};
