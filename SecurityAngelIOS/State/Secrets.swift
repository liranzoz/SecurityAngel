import Foundation

/// API keys & secrets. This file is **gitignored** — replace the placeholders
/// with your real keys before running the app.
///
/// To match the Android project's `local.properties`:
///   - `GEMINI_API_KEY`    → `Secrets.geminiAPIKey`
///   - `VIRUSTOTAL_API_KEY` → `Secrets.virusTotalAPIKey`
enum Secrets {
    static let geminiAPIKey: String       = ProcessInfo.processInfo.environment["GEMINI_API_KEY"]     ?? ""
    static let virusTotalAPIKey: String   = ProcessInfo.processInfo.environment["VIRUSTOTAL_API_KEY"] ?? ""
}
