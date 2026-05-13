import Foundation

/// VirusTotal v3 URL-scan client. Mirrors the Android `VirusTotalApi`:
///
///   GET  /api/v3/urls/{id}              → lookup by url-id (404 if unknown)
///   POST /api/v3/urls                   → submit new URL for analysis
///   GET  /api/v3/analyses/{analysisId}  → poll until status == completed
///
/// `id` is `urlsafe_base64_no_padding(url)` (matches `urlToBase64` on Android).
final class VirusTotalAPI: Sendable {

    enum APIError: LocalizedError {
        case missingKey
        case http(Int)
        case decoding
        case noAnalysisId

        var errorDescription: String? {
            switch self {
            case .missingKey:     return "VirusTotal API key missing. Add VIRUSTOTAL_API_KEY in Secrets.swift."
            case .http(let code): return "VirusTotal HTTP \(code)"
            case .decoding:       return "VirusTotal response could not be parsed"
            case .noAnalysisId:   return "VirusTotal did not return an analysis id"
            }
        }
    }

    private let session: URLSession
    private let baseURL = URL(string: "https://www.virustotal.com")!
    private let apiKey: String

    init(apiKey: String = Secrets.virusTotalAPIKey, session: URLSession = .shared) {
        self.apiKey = apiKey
        self.session = session
    }

    // MARK: - Public flow (matches SandBoxActivity)

    /// Scan a URL end-to-end. Looks it up first, submits + polls if unknown,
    /// then returns the final attributes block.
    func scan(url: String) async throws -> VTAttributes {
        if let attrs = try await lookup(url: url) { return attrs }
        let analysisId = try await submit(url: url)
        try await pollUntilComplete(analysisId: analysisId)
        guard let final = try await lookup(url: url) else { throw APIError.noAnalysisId }
        return final
    }

    // MARK: - Endpoints

    /// Returns `nil` on 404 (URL unknown to VT).
    func lookup(url: String) async throws -> VTAttributes? {
        let id = Self.urlToBase64(url)
        let request = makeRequest(path: "/api/v3/urls/\(id)", method: "GET")
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.decoding }
        if http.statusCode == 404 { return nil }
        guard (200..<300).contains(http.statusCode) else { throw APIError.http(http.statusCode) }
        return try JSONDecoder().decode(VTResponse.self, from: data).data?.attributes
    }

    func submit(url: String) async throws -> String {
        var request = makeRequest(path: "/api/v3/urls", method: "POST")
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let encoded = "url=\(url.addingPercentEncoding(withAllowedCharacters: .urlHostAllowed) ?? url)"
        request.httpBody = encoded.data(using: .utf8)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw APIError.http((response as? HTTPURLResponse)?.statusCode ?? -1)
        }
        let decoded = try JSONDecoder().decode(VTResponse.self, from: data)
        guard let id = decoded.data?.id else { throw APIError.noAnalysisId }
        return id
    }

    func pollUntilComplete(analysisId: String, every interval: Duration = .milliseconds(2000)) async throws {
        while true {
            let request = makeRequest(path: "/api/v3/analyses/\(analysisId)", method: "GET")
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw APIError.http((response as? HTTPURLResponse)?.statusCode ?? -1)
            }
            let decoded = try JSONDecoder().decode(VTResponse.self, from: data)
            if decoded.data?.attributes?.status == "completed" { return }
            try await Task.sleep(for: interval)
        }
    }

    // MARK: - Helpers

    private func makeRequest(path: String, method: String) -> URLRequest {
        var url = baseURL
        url.append(path: path)
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue(apiKey, forHTTPHeaderField: "x-apikey")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        return req
    }

    /// `Base64.NO_PADDING | NO_WRAP | URL_SAFE` — matches the Android encoder.
    static func urlToBase64(_ url: String) -> String {
        let data = Data(url.utf8)
        var b64 = data.base64EncodedString()
        b64 = b64.replacingOccurrences(of: "+", with: "-")
                 .replacingOccurrences(of: "/", with: "_")
                 .replacingOccurrences(of: "=", with: "")
        return b64
    }
}

// MARK: - Response payloads

struct VTResponse: Decodable {
    let data: VTData?
}

struct VTData: Decodable {
    let id: String?
    let type: String?
    let attributes: VTAttributes?
}

struct VTAttributes: Decodable {
    let lastAnalysisStats: VTStats?
    let lastAnalysisResults: [String: VTEngineResult]?
    let status: String?

    enum CodingKeys: String, CodingKey {
        case lastAnalysisStats = "last_analysis_stats"
        case lastAnalysisResults = "last_analysis_results"
        case status
    }

    var maliciousCount: Int { lastAnalysisStats?.malicious ?? 0 }
    var isSafe: Bool { maliciousCount == 0 }
}

struct VTStats: Decodable {
    var malicious: Int = 0
    var suspicious: Int = 0
    var harmless: Int = 0
    var undetected: Int = 0
}

struct VTEngineResult: Decodable, Hashable {
    let engineName: String?
    let result: String?
    let category: String?

    enum CodingKeys: String, CodingKey {
        case engineName = "engine_name"
        case result
        case category
    }

    var isClean: Bool { (result ?? "") == "clean" || (result ?? "") == "unrated" }
}
