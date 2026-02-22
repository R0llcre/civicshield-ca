import { strFromU8, strToU8, unzipSync, zipSync } from "fflate";

import type { EncryptedBlob } from "./crypto";
import { encryptPayload } from "./crypto";
import type { VaultParams, VaultItemMeta } from "./db";

type VaultExportV1 = {
  version: 1;
  kdf: { saltB64: string; iterations: number };
  alg: "AES-GCM";
};

export async function exportVaultZip(
  key: CryptoKey,
  params: VaultParams,
  plain: { items: VaultItemMeta[]; files: Array<{ id: string; bytesB64: string }> },
): Promise<Blob> {
  // Encrypt a single payload to avoid leaking item counts/filenames on wrong passphrase.
  const payload = await encryptPayload(key, plain);

  const manifest: VaultExportV1 = {
    version: 1,
    kdf: { saltB64: params.saltB64, iterations: params.iterations },
    alg: "AES-GCM",
  };

  const files: Record<string, Uint8Array> = {
    "vault_v1.json": strToU8(JSON.stringify(manifest)),
    "vault_payload.enc": strToU8(JSON.stringify(payload)),
  };

  const zipped = zipSync(files, { level: 9 });
  const ab = zipped.buffer.slice(zipped.byteOffset, zipped.byteOffset + zipped.byteLength) as ArrayBuffer;
  return new Blob([ab], { type: "application/zip" });
}

const MAX_ZIP_BYTES = 256 * 1024 * 1024; // 256 MB

export async function importVaultZip(zipBytes: ArrayBuffer): Promise<{ params: VaultParams; payload: EncryptedBlob }> {
  if (zipBytes.byteLength > MAX_ZIP_BYTES) throw new Error("Invalid vault zip");
  const data = new Uint8Array(zipBytes);
  const unzipped = unzipSync(data);
  const manifestBytes = unzipped["vault_v1.json"];
  const payloadBytes = unzipped["vault_payload.enc"];
  if (!manifestBytes || !payloadBytes) throw new Error("Invalid vault zip");

  const manifest = JSON.parse(strFromU8(manifestBytes)) as VaultExportV1;
  if (manifest.version !== 1) throw new Error("Unsupported vault version");

  const payload = JSON.parse(strFromU8(payloadBytes)) as EncryptedBlob;
  if (!payload?.ivB64 || !payload?.ciphertextB64) throw new Error("Invalid payload");

  return {
    params: { version: 1, saltB64: manifest.kdf.saltB64, iterations: manifest.kdf.iterations },
    payload,
  };
}
