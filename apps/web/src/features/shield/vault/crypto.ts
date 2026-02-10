export type EncryptedBlob = {
  ivB64: string;
  ciphertextB64: string;
};

const TEXT_ENCODER = new TextEncoder();
const TEXT_DECODER = new TextDecoder();

export function bytesToB64(bytes: Uint8Array): string {
  // Browser-safe base64 encoding.
  let bin = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    bin += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(bin);
}

export function b64ToBytes(b64: string): Uint8Array<ArrayBuffer> {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes as Uint8Array<ArrayBuffer>;
}

function encodePayload(payload: unknown): Uint8Array<ArrayBuffer> {
  if (payload instanceof Uint8Array) {
    const out = new Uint8Array(1 + payload.byteLength);
    out[0] = 2; // bytes
    out.set(payload, 1);
    return out as Uint8Array<ArrayBuffer>;
  }

  const json = JSON.stringify(payload);
  const bytes = TEXT_ENCODER.encode(json);
  const out = new Uint8Array(1 + bytes.byteLength);
  out[0] = 1; // json
  out.set(bytes, 1);
  return out as Uint8Array<ArrayBuffer>;
}

function decodePayload<T>(bytes: Uint8Array): T {
  const kind = bytes[0];
  const rest = bytes.subarray(1);
  if (kind === 2) return rest as unknown as T;
  if (kind !== 1) throw new Error("Unknown payload kind");
  const json = TEXT_DECODER.decode(rest);
  return JSON.parse(json) as T;
}

export async function deriveKey(passphrase: string, saltB64: string, iterations: number) {
  const passphraseKey = await crypto.subtle.importKey(
    "raw",
    TEXT_ENCODER.encode(passphrase),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  const salt = b64ToBytes(saltB64);

  return crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt,
      iterations,
      hash: "SHA-256",
    },
    passphraseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

export async function encryptPayload(key: CryptoKey, payload: unknown): Promise<EncryptedBlob> {
  const iv = crypto.getRandomValues(new Uint8Array(12) as Uint8Array<ArrayBuffer>);
  const plaintext = encodePayload(payload);
  const ciphertext = new Uint8Array(
    await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, plaintext),
  );
  return { ivB64: bytesToB64(iv), ciphertextB64: bytesToB64(ciphertext) };
}

export async function decryptPayload<T>(key: CryptoKey, blob: EncryptedBlob): Promise<T> {
  const iv = b64ToBytes(blob.ivB64);
  const ciphertext = b64ToBytes(blob.ciphertextB64);
  const plaintext = new Uint8Array(
    await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, ciphertext),
  );
  return decodePayload<T>(plaintext);
}

export function randomSaltB64(bytes = 16) {
  const salt = crypto.getRandomValues(new Uint8Array(bytes));
  return bytesToB64(salt);
}
