import SwiftUI

private let COUNTY_OPTIONS: [String] = [
  "",
  "Los Angeles",
  "San Diego",
  "Orange",
  "San Francisco",
  "Santa Clara",
  "Alameda",
  "Sacramento",
  "Riverside",
  "San Bernardino",
  "Fresno",
  "Ventura",
]

private let LANGUAGE_OPTIONS: [String] = ["", "English", "Spanish", "Chinese", "Korean", "Vietnamese", "Tagalog"]
private let SERVICE_OPTIONS: [String] = ["", "Asylum", "Family", "VAWA", "Work authorization", "Naturalization", "Removal defense"]

struct HelpView: View {
  @EnvironmentObject private var env: AppEnvironment
  @StateObject private var vm = HelpViewModel()
  @Environment(\.openURL) private var openURL

  var body: some View {
    ScrollView {
      LazyVStack(alignment: .leading, spacing: 14) {
        GlassCard {
          Text("Find legal aid faster")
            .font(.system(.title, design: .rounded).weight(.bold))
            .foregroundStyle(Color(red: 0.06, green: 0.11, blue: 0.23))
          Text("Filter by county, language, and service, then check intake status.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)
        }

        GlassCard {
          VStack(alignment: .leading, spacing: 12) {
            GlassSelectField(
              label: "County",
              value: vm.county,
              options: COUNTY_OPTIONS.filter { !$0.isEmpty },
              onSelect: { vm.county = $0 }
            )

            GlassSelectField(
              label: "Language",
              value: vm.language,
              options: LANGUAGE_OPTIONS.filter { !$0.isEmpty },
              onSelect: { vm.language = $0 }
            )

            GlassSelectField(
              label: "Service",
              value: vm.service,
              options: SERVICE_OPTIONS.filter { !$0.isEmpty },
              onSelect: { vm.service = $0 }
            )

            Button {
              Task { await vm.search(api: env.api) }
            } label: {
              HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                Text(vm.loading ? "Searching..." : "Find organizations")
              }
            }
            .buttonStyle(PrimaryButtonStyle(tint: AppTab.help.accent))
            .disabled(vm.loading)
          }
        }

        OfficialDirectoriesCard(onOpen: { openURL($0) })

        if vm.loading {
          GlassCard {
            HStack(spacing: 10) {
              ProgressView()
              Text("Loading…")
                .font(.system(.subheadline, design: .rounded).weight(.semibold))
            }
          }
        } else if let err = vm.error {
          GlassCard {
            Text(err)
              .foregroundStyle(.red)
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
          }
        } else if vm.hasSearched && vm.results.isEmpty {
          GlassCard {
            Text("No organizations match these filters.")
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
          }
        }

        if !vm.results.isEmpty {
          LazyVStack(spacing: 12) {
            ForEach(vm.results) { org in
              AidOrgCard(org: org, onOpen: {
                if let raw = org.website ?? org.sourceUrl, let u = URL(string: raw) { openURL(u) }
              })
            }
          }
        }

        GlassCard {
          Text("Information only. Not legal advice.")
            .font(.system(.caption, design: .rounded))
            .foregroundStyle(.secondary)
        }
      }
      .padding(.horizontal, 16)
      .padding(.bottom, 28)
    }
    .task {
      await vm.initialLoadIfNeeded(api: env.api)
    }
  }
}

private struct OfficialDirectoriesCard: View {
  let onOpen: (URL) -> Void

  var body: some View {
    GlassCard {
      Text("Official directories")
        .font(.system(.headline, design: .rounded).weight(.bold))
      Text("If you can't find a match here, try these official or partner directories.")
        .font(.system(.subheadline, design: .rounded))
        .foregroundStyle(.secondary)

      OfficialLinkButton(title: "LawHelpCA", url: URL(string: "https://lawhelpca.org/")!, onOpen: onOpen)
      OfficialLinkButton(title: "Immigration Advocates Directory", url: URL(string: "https://www.immigrationadvocates.org/")!, onOpen: onOpen)
      OfficialLinkButton(title: "DOJ/EOIR R&A Roster", url: URL(string: "https://www.justice.gov/eoir/recognition-and-accreditation-roster-reports")!, onOpen: onOpen)
    }
  }
}

private struct OfficialLinkButton: View {
  let title: String
  let url: URL
  let onOpen: (URL) -> Void

  var body: some View {
    Button {
      onOpen(url)
    } label: {
      HStack {
        Text(title)
          .font(.system(.subheadline, design: .rounded).weight(.semibold))
        Spacer()
        Image(systemName: "arrow.up.right")
          .font(.system(size: 14, weight: .semibold))
          .foregroundStyle(.secondary)
      }
      .padding(.vertical, 6)
    }
    .buttonStyle(.plain)
  }
}

private struct AidOrgCard: View {
  let org: AidOrg
  let onOpen: () -> Void

  private func statusTint(_ s: String) -> Color {
    switch s.lowercased() {
    case "open": return .green
    case "waitlist": return .orange
    case "closed": return .gray
    default: return .blue
    }
  }

  var body: some View {
    GlassCard {
      HStack(alignment: .firstTextBaseline) {
        Text(org.name)
          .font(.system(.headline, design: .rounded).weight(.bold))
        Spacer()
        StatusPill(text: org.intakeStatus.capitalized, tint: statusTint(org.intakeStatus))
      }

      Text("Counties: \(org.counties.joined(separator: ", "))")
        .font(.system(.caption, design: .rounded))
        .foregroundStyle(.secondary)
      Text("Languages: \(org.languages.joined(separator: ", "))")
        .font(.system(.caption, design: .rounded))
        .foregroundStyle(.secondary)
      Text("Services: \(org.services.joined(separator: ", "))")
        .font(.system(.caption, design: .rounded))
        .foregroundStyle(.secondary)

      if let d = org.etaCallbackDays {
        Text("Estimated callback: \(d) day(s)")
          .font(.system(.caption, design: .rounded))
          .foregroundStyle(.secondary)
      }
      if let t = org.nextClinicAt, !t.isEmpty {
        Text("Next clinic: \(t)")
          .font(.system(.caption, design: .rounded))
          .foregroundStyle(.secondary)
      }
      let v = (org.verifiedAt ?? org.updatedAt).trimmingCharacters(in: .whitespacesAndNewlines)
      if !v.isEmpty {
        Text("Last verified: \(v)")
          .font(.system(.caption, design: .rounded))
          .foregroundStyle(.secondary)
      }

      Button(action: onOpen) {
        HStack(spacing: 10) {
          Image(systemName: "arrow.up.right.square")
          Text("Open official source")
        }
      }
      .buttonStyle(TonalButtonStyle(tint: AppTab.help.accent))
      .disabled((org.website ?? org.sourceUrl) == nil)
    }
  }
}
