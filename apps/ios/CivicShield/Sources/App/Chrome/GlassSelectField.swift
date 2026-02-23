import SwiftUI

/// A labeled, glass-styled selection field that keeps the label visible (unlike Picker(.menu)).
struct GlassSelectField: View {
  let label: String
  let value: String
  let placeholder: String
  let options: [String]
  let onSelect: (String) -> Void

  init(
    label: String,
    value: String,
    placeholder: String = "Any",
    options: [String],
    onSelect: @escaping (String) -> Void
  ) {
    self.label = label
    self.value = value
    self.placeholder = placeholder
    self.options = options
    self.onSelect = onSelect
  }

  private var displayValue: String { value.isEmpty ? placeholder : value }

  var body: some View {
    VStack(alignment: .leading, spacing: 6) {
      Text(label)
        .font(.system(.caption, design: .rounded).weight(.semibold))
        .foregroundStyle(.secondary)

      Menu {
        // Any
        Button {
          onSelect("")
        } label: {
          if value.isEmpty {
            Label(placeholder, systemImage: "checkmark")
          } else {
            Text(placeholder)
          }
        }

        // Options
        ForEach(options, id: \.self) { opt in
          Button {
            onSelect(opt)
          } label: {
            if opt == value {
              Label(opt, systemImage: "checkmark")
            } else {
              Text(opt)
            }
          }
        }
      } label: {
        HStack(spacing: 10) {
          Text(displayValue)
            .font(.system(.subheadline, design: .rounded).weight(.semibold))
            .foregroundStyle(.primary)
            .lineLimit(1)

          Spacer(minLength: 0)

          Image(systemName: "chevron.down")
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
          RoundedRectangle(cornerRadius: 16, style: .continuous)
            .stroke(
              LinearGradient(
                colors: [.white.opacity(0.90), .white.opacity(0.20)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
              ),
              lineWidth: 1
            )
        )
      }
      .buttonStyle(.plain)
    }
  }
}

