import { createClient } from "@supabase/supabase-js";
import { NextResponse, type NextRequest } from "next/server";
import { z } from "zod";

const updateSchema = z.object({
  intake_status: z.enum(["open", "waitlist", "closed"]).optional(),
  next_clinic_at: z.string().datetime().nullable().optional(),
  eta_callback_days: z.number().int().nonnegative().nullable().optional(),
  languages: z.array(z.string()).optional(),
  counties: z.array(z.string()).optional(),
});

export async function POST(req: NextRequest, ctx: { params: Promise<{ orgId: string }> }) {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  if (!url || !key) return NextResponse.json({ error: "Supabase not configured" }, { status: 501 });

  const auth = req.headers.get("authorization") ?? "";
  const token = auth.toLowerCase().startsWith("bearer ") ? auth.slice(7).trim() : null;
  if (!token) return NextResponse.json({ error: "Missing bearer token" }, { status: 401 });

  const body = await req.json().catch(() => null);
  const parsed = updateSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid body" }, { status: 400 });
  }

  const { orgId } = await ctx.params;

  const supabase = createClient(url, key, {
    auth: { persistSession: false },
    global: { headers: { Authorization: `Bearer ${token}` } },
  });

  const res = await supabase.from("aid_org").update(parsed.data).eq("id", orgId).select("id").maybeSingle();
  if (res.error) {
    // If RLS blocks, Supabase often returns 401/403-like errors surfaced here.
    return NextResponse.json({ error: res.error.message }, { status: 403 });
  }

  return NextResponse.json({ ok: true });
}
