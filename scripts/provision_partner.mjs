import process from "node:process";

import { createClient } from "@supabase/supabase-js";

function getArg(name) {
  const idx = process.argv.indexOf(`--${name}`);
  if (idx === -1) return null;
  return process.argv[idx + 1] ?? null;
}

function usage() {
  console.error(
    [
      "Usage:",
      "  node scripts/provision_partner.mjs --email user@example.com --org-id <uuid> [--role admin|editor] [--redirect-to <url>]",
      "",
      "Env required:",
      "  SUPABASE_URL (or NEXT_PUBLIC_SUPABASE_URL)",
      "  SUPABASE_SERVICE_ROLE_KEY",
    ].join("\n"),
  );
}

async function findUserIdByEmail(supabase, email) {
  // Supabase Admin API doesn't offer a direct "get by email"; we page through results.
  const perPage = 1000;
  for (let page = 1; page <= 50; page++) {
    const res = await supabase.auth.admin.listUsers({ page, perPage });
    if (res.error) throw res.error;
    const users = res.data?.users ?? [];
    const hit = users.find((u) => (u.email ?? "").toLowerCase() === email.toLowerCase());
    if (hit?.id) return hit.id;
    if (users.length < perPage) break;
  }
  return null;
}

async function main() {
  const email = (getArg("email") ?? "").trim();
  const orgId = (getArg("org-id") ?? "").trim();
  const role = (getArg("role") ?? "editor").trim();
  const redirectTo = (getArg("redirect-to") ?? "").trim();

  if (!email || !orgId) {
    usage();
    process.exit(2);
  }
  if (!["admin", "editor"].includes(role)) {
    console.error(`Invalid --role: ${role}`);
    process.exit(2);
  }

  const url = process.env.SUPABASE_URL || process.env.NEXT_PUBLIC_SUPABASE_URL;
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !serviceKey) {
    console.error("Missing SUPABASE_URL (or NEXT_PUBLIC_SUPABASE_URL) and/or SUPABASE_SERVICE_ROLE_KEY");
    process.exit(2);
  }

  const supabase = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
  });

  let userId = null;
  let invited = false;

  const invite = await supabase.auth.admin.inviteUserByEmail(email, redirectTo ? { redirectTo } : undefined);
  if (invite.error) {
    const msg = invite.error.message || "";
    // Common cases: user already exists or signups disabled.
    if (/already|exists|registered/i.test(msg)) {
      userId = await findUserIdByEmail(supabase, email);
      if (!userId) throw invite.error;
    } else {
      throw invite.error;
    }
  } else {
    invited = true;
    userId = invite.data?.user?.id ?? null;
  }

  if (!userId) {
    console.error("Could not determine user id for:", email);
    process.exit(1);
  }

  const membership = await supabase
    .from("aid_org_membership")
    .upsert({ user_id: userId, org_id: orgId, role }, { onConflict: "user_id,org_id" })
    .select("user_id,org_id,role")
    .maybeSingle();
  if (membership.error) throw membership.error;

  console.log(
    JSON.stringify(
      {
        ok: true,
        email,
        user_id: userId,
        org_id: orgId,
        role,
        invited,
        redirect_to: redirectTo || null,
      },
      null,
      2,
    ),
  );
}

main().catch((e) => {
  console.error(e instanceof Error ? e.message : String(e));
  process.exit(1);
});

