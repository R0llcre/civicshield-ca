import fs from "node:fs";
import path from "node:path";

import { strToU8, zipSync } from "fflate";

const TEXT_ENCODER = new TextEncoder();

function b64(bytes) {
  return Buffer.from(bytes).toString("base64");
}

function u8(bytes) {
  return new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength);
}

async function deriveKey(passphrase, saltBytes, iterations) {
  const passphraseKey = await crypto.subtle.importKey(
    "raw",
    TEXT_ENCODER.encode(passphrase),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt: saltBytes, iterations, hash: "SHA-256" },
    passphraseKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

function encodeJsonPayload(obj) {
  const json = JSON.stringify(obj);
  const bytes = TEXT_ENCODER.encode(json);
  const out = new Uint8Array(1 + bytes.byteLength);
  out[0] = 1; // json
  out.set(bytes, 1);
  return out;
}

async function encryptAesGcm(key, iv, plaintext) {
  const ciphertext = new Uint8Array(await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, plaintext));
  return { ivB64: b64(iv), ciphertextB64: b64(ciphertext) };
}

function makeZipBytes(manifestObj, payloadObj) {
  const files = {
    "vault_v1.json": strToU8(JSON.stringify(manifestObj)),
    "vault_payload.enc": strToU8(JSON.stringify(payloadObj)),
  };
  return zipSync(files, { level: 9 });
}

async function generateFixture({ passphrase, saltUtf8, ivBytes, plain }) {
  const iterations = 210_000;
  const saltBytes = new Uint8Array(Buffer.from(saltUtf8, "utf8"));
  if (saltBytes.byteLength !== 16) throw new Error("saltUtf8 must be 16 bytes");
  const saltB64 = b64(saltBytes);

  const key = await deriveKey(passphrase, saltBytes, iterations);
  const plaintext = encodeJsonPayload(plain);
  const enc = await encryptAesGcm(key, ivBytes, plaintext);

  const manifest = { version: 1, kdf: { saltB64, iterations }, alg: "AES-GCM" };
  const zip = makeZipBytes(manifest, enc);
  return { zip, passphrase, saltB64, iterations };
}

function ensureDir(p) {
  fs.mkdirSync(path.dirname(p), { recursive: true });
}

async function main() {
  const webPlain = {
    items: [
      {
        id: "web-item-1",
        createdAt: "2026-02-10T00:00:00.000Z",
        originalName: "web-hello.txt",
        contentType: "text/plain",
        size: 5,
      },
      {
        id: "web-item-2",
        createdAt: "2026-02-10T00:00:00.000Z",
        originalName: "web-evidence.bin",
        contentType: "application/octet-stream",
        size: 3,
      },
    ],
    files: [
      { id: "web-item-1", bytesB64: Buffer.from("hello", "utf8").toString("base64") },
      { id: "web-item-2", bytesB64: Buffer.from([1, 2, 3]).toString("base64") },
    ],
  };

  const androidPlain = {
    items: [
      {
        id: "android-item-1",
        createdAt: "2026-02-10T00:00:00.000Z",
        originalName: "android-note.txt",
        contentType: "text/plain",
        size: 12,
      },
    ],
    files: [
      { id: "android-item-1", bytesB64: Buffer.from("hello android", "utf8").toString("base64") },
    ],
  };

  const webIv = new Uint8Array([0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]);
  const androidIv = new Uint8Array([11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0]);

  const web = await generateFixture({
    passphrase: "web-fixture-passphrase",
    saltUtf8: "web-fixture-salt", // 16 bytes
    ivBytes: webIv,
    plain: webPlain,
  });

  const android = await generateFixture({
    passphrase: "android-fixture-passphrase",
    saltUtf8: "android-fixture0", // 16 bytes
    ivBytes: androidIv,
    plain: androidPlain,
  });

  const webOut = "apps/android/app/src/test/resources/vault_web_fixture.zip";
  const androidOut = "apps/web/e2e/fixtures/vault_android_fixture.zip";

  ensureDir(webOut);
  ensureDir(androidOut);
  fs.writeFileSync(webOut, Buffer.from(u8(web.zip)));
  fs.writeFileSync(androidOut, Buffer.from(u8(android.zip)));

  console.log("Wrote fixtures:");
  console.log(`- ${webOut} (passphrase=${web.passphrase})`);
  console.log(`- ${androidOut} (passphrase=${android.passphrase})`);
}

await main();
