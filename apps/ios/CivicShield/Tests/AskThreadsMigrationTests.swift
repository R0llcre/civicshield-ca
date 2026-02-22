import XCTest

@testable import CivicShield

final class AskThreadsMigrationTests: XCTestCase {
  func testMigratesLegacySingleChatToThreadsV2() async throws {
    let suite = "AskThreadsMigrationTests.\(UUID().uuidString)"
    // Seed legacy single-chat storage.
    let legacyMessages: [AskMessage] = [
      AskMessage(role: "User", content: "Hello"),
      AskMessage(role: "Assistant", content: "Hi there"),
    ]
    let enc = JSONEncoder()
    enc.dateEncodingStrategy = .iso8601
    let legacyData = try enc.encode(legacyMessages)

    await MainActor.run {
      guard let ud = UserDefaults(suiteName: suite) else {
        XCTFail("Unable to create UserDefaults suite")
        return
      }
      ud.removePersistentDomain(forName: suite)
      ud.set(legacyData, forKey: "ask.messages.v1")

      let vm = AskViewModel(userDefaults: ud)

      XCTAssertEqual(vm.threads.count, 1)
      XCTAssertNotNil(vm.activeThreadId)
      XCTAssertEqual(vm.messages.count, legacyMessages.count)
      XCTAssertEqual(vm.messages.first?.content, "Hello")

      // Legacy key should be cleared after migration.
      XCTAssertNil(ud.data(forKey: "ask.messages.v1"))

      // Threads index should exist.
      XCTAssertNotNil(ud.data(forKey: "ask.threads.index.v2"))
    }
  }
}
