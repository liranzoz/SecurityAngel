import Foundation

/// Domain comparison helpers used by the vault and (eventually) the autofill
/// extension. Mirrors the `domainsMatch` / `rootDomain` helpers in Android's
/// `SecurityAngelAutofillService` and `AutofillUnlockActivity`.
enum DomainMatcher {

    static func match(current: String, stored: String) -> Bool {
        if current.caseInsensitiveCompare(stored) == .orderedSame { return true }
        return rootDomain(current).caseInsensitiveCompare(rootDomain(stored)) == .orderedSame
    }

    static func rootDomain(_ domain: String) -> String {
        let parts = domain.split(separator: ".")
        guard parts.count >= 2 else { return domain }
        return "\(parts[parts.count - 2]).\(parts[parts.count - 1])"
    }

    /// Normalize the domain we record from autofill (drop subdomain unless
    /// it's a known TLD prefix like `co.uk`).
    static func normalize(_ domain: String) -> String {
        let parts = domain.split(separator: ".")
        guard parts.count >= 2 else { return domain }
        let tldPrefixes: Set<String> = ["com", "org", "net", "io", "app", "dev", "co"]
        if let first = parts.first.map(String.init), tldPrefixes.contains(first.lowercased()) {
            return domain
        }
        return "\(parts[parts.count - 2]).\(parts[parts.count - 1])"
    }

    static func siteName(from domain: String) -> String {
        let parts = domain.split(separator: ".").map(String.init)
        let knownTlds: Set<String> = ["com", "org", "net", "io", "app", "dev", "co"]
        let name: String
        if parts.count >= 3, let first = parts.first, knownTlds.contains(first.lowercased()) {
            name = parts[1]
        } else if parts.count >= 2 {
            name = parts[parts.count - 2]
        } else {
            name = parts.first ?? domain
        }
        return name.prefix(1).uppercased() + name.dropFirst()
    }
}
