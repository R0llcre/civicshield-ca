export type RaEntry = {
  entry_type: "organization" | "representative";
  name: string;
  organization_name?: string | null;
  city?: string | null;
  state: string;
  address?: string | null;
  phone?: string | null;
  website?: string | null;
  accreditation_level?: string | null;
  status?: string | null;
  expires_at?: string | null;
  pending_renewal?: boolean | null;
  source_url?: string | null;
  source_sha256?: string | null;
};

export const SAMPLE_RA: RaEntry[] = [
  {
    entry_type: "organization",
    name: "Immigrant Hope - Wyoming Idaho",
    city: "Jackson",
    state: "WY",
    address: "Principal Office\n90 West Kelly Avenue\nJackson, WY 83001",
    phone: "(208) 709-0131",
    source_url: "https://www.justice.gov/eoir/media/1398081/dl?inline",
  },
  {
    entry_type: "representative",
    name: "Diaz, Samantha",
    organization_name: "International Institute of Los Angeles",
    state: "US",
    accreditation_level: "DHS only",
    source_url: "https://www.justice.gov/media/1193716/dl?inline",
  },
];

