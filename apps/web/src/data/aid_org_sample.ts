export type AidOrg = {
  id: string;
  name: string;
  website?: string | null;
  phone?: string | null;
  email?: string | null;
  counties: string[];
  languages: string[];
  services: string[];
  intake_status: "open" | "waitlist" | "closed";
  next_clinic_at?: string | null;
  eta_callback_days?: number | null;
  source_url?: string | null;
  verified_at?: string | null;
  verified_by?: string | null;
  updated_at: string;
};

export const SAMPLE_AID_ORGS: AidOrg[] = [
  {
    id: "sample-1",
    name: "Sample Legal Aid (Demo)",
    website: "https://example.org",
    phone: "(555) 010-0101",
    counties: ["Los Angeles"],
    languages: ["English", "Spanish"],
    services: ["Asylum", "Family"],
    intake_status: "waitlist",
    eta_callback_days: 5,
    source_url: "https://example.org",
    verified_at: new Date().toISOString(),
    verified_by: "demo",
    updated_at: new Date().toISOString(),
  },
  {
    id: "sample-2",
    name: "Community Immigration Clinic (Demo)",
    website: "https://example.org",
    phone: "(555) 010-0202",
    counties: ["San Diego"],
    languages: ["English", "Spanish", "Chinese"],
    services: ["Work authorization", "VAWA"],
    intake_status: "open",
    next_clinic_at: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString(),
    source_url: "https://example.org",
    verified_at: new Date().toISOString(),
    verified_by: "demo",
    updated_at: new Date().toISOString(),
  }
];
