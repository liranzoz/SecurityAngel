import Foundation
import UIKit

/// REST client for Google's Gemini API (no SDK dependency). Targets
/// `gemini-2.5-flash` to match the Android app. Supports multimodal input
/// (text + image) for the phishing-screenshot analysis flow.
///
/// Endpoint:
///   POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={API_KEY}
final class GeminiAPI: Sendable {

    enum GeminiError: LocalizedError {
        case missingKey
        case http(Int, String?)
        case decoding
        case emptyResponse

        var errorDescription: String? {
            switch self {
            case .missingKey:        return "Gemini API key missing. Add GEMINI_API_KEY in Secrets.swift."
            case .http(let s, let m): return "Gemini HTTP \(s)\(m.map { " — \($0)" } ?? "")"
            case .decoding:          return "Gemini response could not be parsed"
            case .emptyResponse:     return "Gemini returned no text"
            }
        }
    }

    private let apiKey: String
    private let session: URLSession
    private let model: String

    init(apiKey: String = Secrets.geminiAPIKey, model: String = "gemini-2.5-flash", session: URLSession = .shared) {
        self.apiKey = apiKey
        self.model = model
        self.session = session
    }

    // MARK: - Public

    func generate(prompt: String, image: UIImage? = nil) async throws -> String {
        guard !apiKey.isEmpty else { throw GeminiError.missingKey }

        var parts: [Part] = [.init(text: prompt, inlineData: nil)]
        if let image, let jpeg = image.jpegData(compressionQuality: 0.85) {
            parts.append(.init(text: nil, inlineData: .init(mimeType: "image/jpeg", data: jpeg.base64EncodedString())))
        }

        let body = Request(contents: [.init(parts: parts, role: "user")])

        var url = URL(string: "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent")!
        url.append(queryItems: [.init(name: "key", value: apiKey)])

        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(body)

        let (data, response) = try await session.data(for: req)
        guard let http = response as? HTTPURLResponse else { throw GeminiError.decoding }
        guard (200..<300).contains(http.statusCode) else {
            throw GeminiError.http(http.statusCode, String(data: data, encoding: .utf8))
        }

        let decoded = try JSONDecoder().decode(Response.self, from: data)
        guard let text = decoded.candidates?.first?.content?.parts?.compactMap(\.text).joined(separator: "\n"),
              !text.isEmpty else {
            throw GeminiError.emptyResponse
        }
        return text
    }
}

// MARK: - Payloads

private extension GeminiAPI {
    struct Request: Encodable {
        let contents: [Content]
    }

    struct Content: Codable {
        let parts: [Part]?
        let role: String?
    }

    struct Part: Codable {
        let text: String?
        let inlineData: InlineData?

        enum CodingKeys: String, CodingKey {
            case text
            case inlineData = "inline_data"
        }
    }

    struct InlineData: Codable {
        let mimeType: String
        let data: String

        enum CodingKeys: String, CodingKey {
            case mimeType = "mime_type"
            case data
        }
    }

    struct Response: Decodable {
        let candidates: [Candidate]?
    }

    struct Candidate: Decodable {
        let content: Content?
    }
}

// MARK: - System-context prompt builder

enum GeminiPromptBuilder {

    /// Recreates the Android prompt format. `context` is the snapshot the
    /// app injects before the user's question — vault leaks, family status,
    /// recent scan, root + risky apps. The model can append `[ACTION:CODE]`
    /// to deep-link the UI somewhere.
    static func buildPrompt(userName: String,
                            totalPasswords: Int,
                            leakedPasswords: Int,
                            familyStatus: String,
                            lastScan: String,
                            rootStatus: String,
                            riskyAppsText: String,
                            userMessage: String) -> String {
        """
        SYSTEM_CONTEXT:
        Current System Status for user \(userName):
        - Total Passwords in Vault: \(totalPasswords)
        - Compromised Passwords: \(leakedPasswords)
        - Family Security Status: \(familyStatus)
        - Last URL Scan: \(lastScan)
        - Device Posture: \(rootStatus)
        - Risky Apps Installed: \(riskyAppsText)

        You are an actionable assistant. You can perform actions inside the app.
        If the user asks to do something, ANSWER normally, but append a special tag at the end.

        Supported Actions:
        1. To open password vault -> append [ACTION:OPEN_VAULT]
        2. To check family status -> append [ACTION:OPEN_FAMILY]
        3. To scan a website -> append [ACTION:OPEN_SCANNER]
        4. To generate a password -> append [ACTION:GENERATE_PASS]
        5. To view activity logs -> append [ACTION:OPEN_LOGS]

        Example:
        User: "I want to see my passwords"
        AI: "Sure, taking you to your vault now. [ACTION:OPEN_VAULT]"

        USER_INPUT:
        \(userMessage)

        INSTRUCTIONS:
        If an image is provided, analyze it for phishing, scams, or sensitive data leaks.
        If it's a screenshot of an email or SMS, check the sender and content for red flags.
        Give a clear verdict: SAFE, SUSPICIOUS, or DANGEROUS. Consult to user what to do.
        """
    }

    /// Parses a trailing `[ACTION:CODE]` tag out of the AI's reply.
    /// Returns `(cleanedText, actionCode?)`.
    static func extractAction(from text: String) -> (String, String?) {
        let pattern = #"\[ACTION:([A-Z_]+)\]"#
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)),
              let codeRange = Range(match.range(at: 1), in: text),
              let fullRange = Range(match.range, in: text)
        else {
            return (text, nil)
        }
        let action = String(text[codeRange])
        let cleaned = text.replacingCharacters(in: fullRange, with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        return (cleaned, action)
    }
}
