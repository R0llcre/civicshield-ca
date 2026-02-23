import SwiftUI

struct VerifyView: View {
  @EnvironmentObject private var env: AppEnvironment
  @StateObject private var vm = VerifyViewModel()

  var body: some View {
    ScrollView {
      LazyVStack(alignment: .leading, spacing: 14) {
        GlassCard {
          Text("Verify credentials")
            .font(.system(.title, design: .rounded).weight(.bold))
            .foregroundStyle(Color(red: 0.06, green: 0.11, blue: 0.23))
          Text("Check whether a person or organization appears on official rosters, then review evidence links.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)
        }

        GlassCard {
          VStack(alignment: .leading, spacing: 10) {
            Text("Name")
              .font(.system(.caption, design: .rounded).weight(.semibold))
              .foregroundStyle(.secondary)
            TextField("e.g. Diaz, Samantha", text: $vm.name)
              .textInputAutocapitalization(.words)
              .autocorrectionDisabled()
              .padding(12)
              .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))

            HStack(spacing: 10) {
              VStack(alignment: .leading, spacing: 8) {
                Text("State")
                  .font(.system(.caption, design: .rounded).weight(.semibold))
                  .foregroundStyle(.secondary)
                TextField("CA", text: $vm.state)
                  .textInputAutocapitalization(.characters)
                  .autocorrectionDisabled()
                  .frame(width: 86)
                  .padding(12)
                  .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
              }

              Spacer()
            }

            Button {
              Task { await vm.search(api: env.api) }
            } label: {
              HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                Text(vm.loading ? "Searching..." : "Search rosters")
              }
            }
            .buttonStyle(PrimaryButtonStyle(tint: AppTab.verify.accent))
            .disabled(vm.loading)
          }
        }

        if let err = vm.error {
          GlassCard {
            Text(err)
              .foregroundStyle(.red)
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
          }
        } else if vm.shouldShowNoResults {
          GlassCard {
            Text("No results.")
              .font(.system(.subheadline, design: .rounded).weight(.semibold))
          }
        }

        if !vm.results.isEmpty {
          GlassCard {
            HStack(alignment: .firstTextBaseline) {
              VStack(alignment: .leading, spacing: 6) {
                Text("Matches")
                  .font(.system(.headline, design: .rounded).weight(.bold))
                if let ended = vm.freshness?.endedAt {
                  Text("Roster updated: \(ended)")
                    .font(.system(.caption, design: .rounded))
                    .foregroundStyle(.secondary)
                }
              }
              Spacer()
              StatusPill(text: "\(vm.results.count)", tint: AppTab.verify.accent)
            }
          }

          LazyVStack(spacing: 12) {
            ForEach(vm.results) { r in
              VerifyResultCard(entry: r)
            }
          }
        }

        GlassCard {
          Text("Official manual checks")
            .font(.system(.headline, design: .rounded).weight(.bold))
          Text("Always verify with official registries before taking action.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)

          OfficialLinkRow(
            title: "California State Bar",
            detail: "If someone claims to be an attorney, confirm their active status here.",
            url: URL(string: "https://www.calbar.ca.gov/")!
          )
          OfficialLinkRow(
            title: "California SOS Consultant Search",
            detail: "If someone claims to be an immigration consultant, confirm registration here.",
            url: URL(string: "https://www.sos.ca.gov/business-programs/bizfile/california-immigration-consultant-search")!
          )

          Divider()
          Text("Information only. Not legal advice.")
            .font(.system(.caption, design: .rounded))
            .foregroundStyle(.secondary)
        }
      }
      .padding(.horizontal, 16)
      .padding(.bottom, 28)
    }
  }
}

private struct VerifyResultCard: View {
  let entry: RaEntry
  @Environment(\.openURL) private var openURL

  var body: some View {
    GlassCard {
      HStack(alignment: .firstTextBaseline, spacing: 10) {
        Text(entry.name)
          .font(.system(.headline, design: .rounded).weight(.bold))
        Spacer()
        StatusPill(text: entry.entryType.capitalized, tint: AppTab.verify.accent)
      }

      let org = (entry.organizationName ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
      let city = (entry.city ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
      let state = entry.state.trimmingCharacters(in: .whitespacesAndNewlines)
      let parts = [org.isEmpty ? nil : org, city.isEmpty ? nil : city, state.isEmpty ? nil : state].compactMap { $0 }
      if !parts.isEmpty {
        Text(parts.joined(separator: " • "))
          .font(.system(.subheadline, design: .rounded))
          .foregroundStyle(.secondary)
      }

      if let status = entry.status, !status.isEmpty {
        Text(status)
          .font(.system(.subheadline, design: .rounded).weight(.semibold))
      }

      Button {
        if let u = URL(string: entry.sourceUrl) {
          openURL(u)
        }
      } label: {
        HStack(spacing: 10) {
          Image(systemName: "arrow.up.right.square")
          Text("Open official source")
        }
      }
      .buttonStyle(TonalButtonStyle(tint: AppTab.verify.accent))
    }
  }
}

private struct OfficialLinkRow: View {
  let title: String
  let detail: String
  let url: URL
  @Environment(\.openURL) private var openURL

  var body: some View {
    Button {
      openURL(url)
    } label: {
      HStack(alignment: .top, spacing: 12) {
        VStack(alignment: .leading, spacing: 4) {
          Text(title)
            .font(.system(.subheadline, design: .rounded).weight(.semibold))
            .foregroundStyle(.primary)
          Text(detail)
            .font(.system(.caption, design: .rounded))
            .foregroundStyle(.secondary)
        }
        Spacer()
        Image(systemName: "arrow.up.right")
          .font(.system(size: 14, weight: .semibold))
          .foregroundStyle(.secondary)
          .padding(.top, 2)
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(.vertical, 8)
    }
    .buttonStyle(.plain)
  }
}
