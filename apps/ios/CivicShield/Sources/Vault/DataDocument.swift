import Foundation
import SwiftUI
import UniformTypeIdentifiers

struct DataDocument: FileDocument {
  static var readableContentTypes: [UTType] { [.zip] }

  var data: Data

  init(data: Data) {
    self.data = data
  }

  init(configuration: ReadConfiguration) throws {
    self.data = configuration.file.regularFileContents ?? Data()
  }

  func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
    FileWrapper(regularFileWithContents: data)
  }
}

