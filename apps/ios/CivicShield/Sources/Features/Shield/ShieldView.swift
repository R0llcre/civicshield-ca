import SwiftUI

struct ShieldView: View {
  @StateObject private var vm = ShieldViewModel()
  @State private var showShare = false
  @State private var toast: String? = nil
  @State private var flagsExpanded: Bool = true
  @State private var planExpanded: Bool = true

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 14) {
        GlassCard {
          Text("Scam shield")
            .font(.system(.title, design: .rounded).weight(.bold))
            .foregroundStyle(Color(red: 0.06, green: 0.11, blue: 0.23))
          Text("Spot red flags early and document evidence safely on-device.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)

          HStack(spacing: 10) {
            StatusPill(text: "Signals \(vm.selectedFlags.count)/\(vm.flags.count)", tint: AppTab.shield.accent)
            StatusPill(text: vm.planProgressLabel(), tint: .gray)
            StatusPill(text: vm.riskLabel(), tint: vm.selectedFlags.count >= 2 ? .orange : .green)
          }
        }

        GlassCard {
          VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
              HStack(spacing: 8) {
                Image(systemName: flagsExpanded ? "chevron.down" : "chevron.right")
                  .font(.system(size: 14, weight: .semibold))
                  .foregroundStyle(.secondary)

                Text("Red flags checklist")
                  .font(.system(.headline, design: .rounded).weight(.bold))

                Spacer(minLength: 0)

                Text("\(vm.selectedFlags.count) selected")
                  .font(.system(.caption, design: .rounded).weight(.semibold))
                  .foregroundStyle(.secondary)
              }
              .frame(maxWidth: .infinity, alignment: .leading)
              .contentShape(Rectangle())
              .onTapGesture {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                  flagsExpanded.toggle()
                }
              }

              Button {
                vm.clearAllFlags()
              } label: {
                Text("Clear")
                  .font(.system(.caption, design: .rounded).weight(.semibold))
              }
              .buttonStyle(.plain)
              .foregroundStyle(AppTab.shield.accent)
              .padding(.horizontal, 12)
              .padding(.vertical, 8)
              .frame(minHeight: 44)
              .background(AppTab.shield.accent.opacity(0.10), in: Capsule())
              .overlay(Capsule().stroke(AppTab.shield.accent.opacity(0.18), lineWidth: 1))
            }

            if flagsExpanded {
              VStack(spacing: 10) {
                ForEach(vm.flags) { f in
                  Button {
                    vm.toggleFlag(f.id)
                  } label: {
                    HStack(alignment: .top, spacing: 12) {
                      Image(systemName: vm.selectedFlags.contains(f.id) ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(vm.selectedFlags.contains(f.id) ? AppTab.shield.accent : .secondary)
                        .font(.system(size: 18, weight: .semibold))
                        .padding(.top, 2)

                      VStack(alignment: .leading, spacing: 3) {
                        Text(f.title)
                          .font(.system(.subheadline, design: .rounded).weight(.semibold))
                          .foregroundStyle(.primary)
                        Text(f.detail)
                          .font(.system(.caption, design: .rounded))
                          .foregroundStyle(.primary.opacity(0.78))
                      }

                      Spacer(minLength: 0)
                    }
                    .padding(12)
                    .background(AppTab.shield.accent.opacity(vm.selectedFlags.contains(f.id) ? 0.10 : 0.06), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(
                      RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(AppTab.shield.accent.opacity(vm.selectedFlags.contains(f.id) ? 0.22 : 0.10), lineWidth: 1)
                    )
                  }
                  .buttonStyle(.plain)
                }
              }
            }
          }
        }

        GlassCard {
          VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
              HStack(spacing: 8) {
                Image(systemName: planExpanded ? "chevron.down" : "chevron.right")
                  .font(.system(size: 14, weight: .semibold))
                  .foregroundStyle(.secondary)

                Text("Action plan")
                  .font(.system(.headline, design: .rounded).weight(.bold))

                Spacer(minLength: 0)

                Text(vm.planProgressLabel())
                  .font(.system(.caption, design: .rounded).weight(.semibold))
                  .foregroundStyle(.secondary)
              }
              .frame(maxWidth: .infinity, alignment: .leading)
              .contentShape(Rectangle())
              .onTapGesture {
                withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                  planExpanded.toggle()
                }
              }
            }

            if planExpanded {
              VStack(alignment: .leading, spacing: 10) {
                ForEach(Array(vm.steps.enumerated()), id: \.element.id) { idx, s in
                  Button {
                    vm.toggleStep(s.id)
                  } label: {
                    HStack(alignment: .top, spacing: 12) {
                      Text("\(idx + 1)")
                        .font(.system(.caption, design: .rounded).weight(.bold))
                        .foregroundStyle(AppTab.shield.accent)
                        .frame(width: 22, height: 22)
                        .background(AppTab.shield.accent.opacity(0.12), in: RoundedRectangle(cornerRadius: 8, style: .continuous))

                      Text(s.text)
                        .font(.system(.subheadline, design: .rounded).weight(.semibold))
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)

                      Image(systemName: vm.completedSteps.contains(s.id) ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(vm.completedSteps.contains(s.id) ? AppTab.shield.accent : .secondary)
                    }
                    .padding(12)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                  }
                  .buttonStyle(.plain)
                }
              }

              HStack(spacing: 12) {
                Button {
                  UIPasteboard.general.string = vm.summaryText()
                  toast = "Copied"
                } label: {
                  Label("Copy summary", systemImage: "doc.on.doc")
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(TonalButtonStyle(tint: AppTab.shield.accent))

                Button {
                  showShare = true
                } label: {
                  Label("Share summary", systemImage: "square.and.arrow.up")
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(TonalButtonStyle(tint: AppTab.shield.accent))
              }
            }
          }
        }

        GlassCard {
          Text("Evidence Vault")
            .font(.system(.headline, design: .rounded).weight(.bold))
          Text("Local-only encrypted storage for files and notes. Nothing uploads by default.")
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(.secondary)

          NavigationLink {
            VaultView()
          } label: {
            HStack(spacing: 10) {
              Image(systemName: "lock.shield")
              Text("Open Evidence Vault")
            }
          }
          .buttonStyle(PrimaryButtonStyle(tint: AppTab.shield.accent))
        }

        GlassCard {
          Text("Information only, not legal advice.")
            .font(.system(.caption, design: .rounded))
            .foregroundStyle(.secondary)
        }
      }
      .padding(.horizontal, 16)
      .padding(.bottom, 28)
    }
    .sheet(isPresented: $showShare) {
      ActivityView(items: [vm.summaryText()])
    }
    .overlay(alignment: .top) {
      if let toast {
        Text(toast)
          .font(.system(.caption, design: .rounded).weight(.semibold))
          .padding(.horizontal, 12)
          .padding(.vertical, 8)
          .background(.ultraThinMaterial, in: Capsule())
          .padding(.top, 14)
          .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
              withAnimation { self.toast = nil }
            }
          }
      }
    }
  }
}
