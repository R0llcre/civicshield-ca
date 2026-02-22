import Foundation

// MARK: - Meta

struct HealthResponse: Codable {
  let ok: Bool?
  let mode: String?
  let dbConfigured: Bool?

  enum CodingKeys: String, CodingKey {
    case ok
    case mode
    case dbConfigured = "db_configured"
  }
}

// MARK: - Verify

struct VerifyRequest: Codable {
  let name: String
  let state: String
}

struct VerifyResponse: Codable {
  let results: [RaEntry]
  let dataFreshness: DataFreshness?

  enum CodingKeys: String, CodingKey {
    case results
    case dataFreshness = "data_freshness"
  }
}

struct DataFreshness: Codable {
  let endedAt: String?
  let sourceSha256: String?

  enum CodingKeys: String, CodingKey {
    case endedAt = "ended_at"
    case sourceSha256 = "source_sha256"
  }
}

struct RaEntry: Codable, Identifiable {
  var id: String { "\(entryType)-\(name)-\(organizationName ?? "")-\(city ?? "")-\(state)" }

  let entryType: String
  let name: String
  let organizationName: String?
  let city: String?
  let state: String
  let address: String?
  let phone: String?
  let website: String?
  let accreditationLevel: String?
  let status: String?
  let expiresAt: String?
  let pendingRenewal: Bool?
  let sourceUrl: String
  let sourceSha256: String?
  let score: Double?
  let matchedOn: String?

  enum CodingKeys: String, CodingKey {
    case entryType = "entry_type"
    case name
    case organizationName = "organization_name"
    case city
    case state
    case address
    case phone
    case website
    case accreditationLevel = "accreditation_level"
    case status
    case expiresAt = "expires_at"
    case pendingRenewal = "pending_renewal"
    case sourceUrl = "source_url"
    case sourceSha256 = "source_sha256"
    case score
    case matchedOn = "matched_on"
  }
}

// MARK: - Directory

struct DirectoryResponse: Codable {
  let results: [AidOrg]
}

struct AidOrg: Codable, Identifiable {
  let id: String
  let name: String
  let website: String?
  let phone: String?
  let email: String?
  let counties: [String]
  let languages: [String]
  let services: [String]
  let intakeStatus: String
  let nextClinicAt: String?
  let etaCallbackDays: Int?
  let sourceUrl: String?
  let verifiedAt: String?
  let verifiedBy: String?
  let updatedAt: String

  enum CodingKeys: String, CodingKey {
    case id
    case name
    case website
    case phone
    case email
    case counties
    case languages
    case services
    case intakeStatus = "intake_status"
    case nextClinicAt = "next_clinic_at"
    case etaCallbackDays = "eta_callback_days"
    case sourceUrl = "source_url"
    case verifiedAt = "verified_at"
    case verifiedBy = "verified_by"
    case updatedAt = "updated_at"
  }
}

// MARK: - Ask

struct AskRequest: Codable {
  let locale: String
  let messages: [AskApiMessage]
}

struct AskMessage: Codable, Identifiable {
  let id: UUID
  let role: String
  let content: String
  let createdAt: Date
  let citations: [AskCitation]?
  let suggestedActions: [AskSuggestedAction]?

  init(
    id: UUID = UUID(),
    role: String,
    content: String,
    createdAt: Date = Date(),
    citations: [AskCitation]? = nil,
    suggestedActions: [AskSuggestedAction]? = nil
  ) {
    self.id = id
    self.role = role
    self.content = content
    self.createdAt = createdAt
    self.citations = citations
    self.suggestedActions = suggestedActions
  }
}

struct AskApiMessage: Codable {
  let role: String
  let content: String
}

struct AskResponse: Codable {
  let answerMarkdown: String
  let citations: [AskCitation]
  let suggestedActions: [AskSuggestedAction]
  let disclaimer: String

  enum CodingKeys: String, CodingKey {
    case answerMarkdown = "answer_markdown"
    case citations
    case suggestedActions = "suggested_actions"
    case disclaimer
  }
}

struct AskCitation: Codable, Identifiable {
  var id: String { url }
  let title: String
  let url: String
}

struct AskSuggestedAction: Codable, Identifiable {
  var id: String { "\(type)-\(label)" }
  let type: String
  let label: String
  let payload: [String: String]?
}
