import Foundation
import SwiftUI

// MARK: - Mock models (mirror Android Firestore shapes; Swift-only for now)

struct MockUser: Identifiable, Hashable {
    let id: String
    let firstName: String
    let lastName: String
    let email: String
    let gender: String
    let riskCount: Int
    var fullName: String { "\(firstName) \(lastName)" }
    var avatar: String {
        switch gender {
        case "Male":   return "person.crop.circle.fill"
        case "Female": return "person.crop.circle.badge.heart"
        default:       return "person.crop.circle"
        }
    }
}

struct MockPasswordAccount: Identifiable, Hashable {
    let id: String
    let siteName: String
    let email: String
    let domain: String
    let password: String
    var isLeaked: Bool
}

struct MockScan: Identifiable, Hashable {
    let id = UUID()
    let url: String
    let isSafe: Bool
    let date: Date
}

struct MockEngineResult: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let status: String
    var isClean: Bool { status == "clean" || status == "unrated" }
}

struct MockChatMessage: Identifiable, Hashable {
    let id = UUID()
    let text: String
    let isUser: Bool
    let isLoading: Bool
    let hasImage: Bool
}

struct MockSecurityLog: Identifiable, Hashable {
    let id = UUID()
    let title: String
    let description: String
    let timestamp: Date
    let kind: PillKind
    let icon: String
}

struct MockRiskyApp: Identifiable, Hashable {
    let id = UUID()
    let appName: String
    let packageName: String
    let riskLabel: String      // "HIGH" / "MEDIUM" / "LOW" / "SAFE"
    let permissions: String
    let explanation: String
}

enum MockData {
    static let currentUser = MockUser(
        id: "u1", firstName: "Liran", lastName: "Zoz",
        email: "liran@example.com", gender: "Male", riskCount: 0
    )

    static let familyMembers: [MockUser] = [
        currentUser,
        MockUser(id: "u2", firstName: "Maya", lastName: "Zoz", email: "maya@example.com", gender: "Female", riskCount: 0),
        MockUser(id: "u3", firstName: "Noam", lastName: "Zoz", email: "noam@example.com", gender: "Male", riskCount: 2),
        MockUser(id: "u4", firstName: "Tal", lastName: "Zoz", email: "tal@example.com", gender: "Female", riskCount: 1),
    ]

    static let passwords: [MockPasswordAccount] = [
        MockPasswordAccount(id: "p1", siteName: "GitHub", email: "liran@example.com", domain: "github.com", password: "x9Kp!1qLm#Vs", isLeaked: false),
        MockPasswordAccount(id: "p2", siteName: "Netflix", email: "liran@example.com", domain: "netflix.com", password: "MovieNight22", isLeaked: true),
        MockPasswordAccount(id: "p3", siteName: "Google", email: "liran@example.com", domain: "google.com", password: "S3cur3#Angel!", isLeaked: false),
        MockPasswordAccount(id: "p4", siteName: "Amazon", email: "liran@example.com", domain: "amazon.com", password: "buyEverything1", isLeaked: true),
        MockPasswordAccount(id: "p5", siteName: "Spotify", email: "liran@example.com", domain: "spotify.com", password: "MusicLover!88", isLeaked: false),
        MockPasswordAccount(id: "p6", siteName: "Apple", email: "liran@example.com", domain: "apple.com", password: "iC@reAboutSecurity", isLeaked: false),
    ]

    static let recentScans: [MockScan] = [
        MockScan(url: "https://github.com/anthropics", isSafe: true,  date: Date().addingTimeInterval(-3600)),
        MockScan(url: "https://bit.ly/3xK9w8z",      isSafe: false, date: Date().addingTimeInterval(-7200)),
        MockScan(url: "https://news.ycombinator.com", isSafe: true,  date: Date().addingTimeInterval(-86400)),
        MockScan(url: "https://login-paypal-update.tk", isSafe: false, date: Date().addingTimeInterval(-172800)),
    ]

    static let engineResults: [MockEngineResult] = [
        MockEngineResult(name: "Kaspersky",         status: "clean"),
        MockEngineResult(name: "Google Safebrowsing", status: "clean"),
        MockEngineResult(name: "BitDefender",       status: "clean"),
        MockEngineResult(name: "PhishTank",         status: "phishing"),
        MockEngineResult(name: "OpenPhish",         status: "phishing"),
        MockEngineResult(name: "ESET",              status: "clean"),
        MockEngineResult(name: "Sophos",            status: "clean"),
        MockEngineResult(name: "McAfee",            status: "malicious"),
    ]

    static let chatHistory: [MockChatMessage] = [
        MockChatMessage(text: "Hello! I'm Security Angel AI. How can I help you stay safe today?", isUser: false, isLoading: false, hasImage: false),
        MockChatMessage(text: "Can you check my password vault for leaks?", isUser: true, isLoading: false, hasImage: false),
        MockChatMessage(text: "I see **2 of your passwords** appear in known breaches:\n\n- Netflix\n- Amazon\n\nLet's update them right away.", isUser: false, isLoading: false, hasImage: false),
        MockChatMessage(text: "Open my vault", isUser: true, isLoading: false, hasImage: false),
    ]

    static let logs: [MockSecurityLog] = [
        MockSecurityLog(title: "Security Breach Detected", description: "Password for Netflix was found in a breach.", timestamp: Date().addingTimeInterval(-3600), kind: .unsafe, icon: "exclamationmark.shield.fill"),
        MockSecurityLog(title: "Scan Completed Safely",    description: "Website https://github.com/anthropics is safe.", timestamp: Date().addingTimeInterval(-7200), kind: .safe, icon: "checkmark.shield.fill"),
        MockSecurityLog(title: "Family Update",            description: "Member with email maya@example.com joined the family!", timestamp: Date().addingTimeInterval(-86400), kind: .info, icon: "person.crop.circle.badge.plus"),
        MockSecurityLog(title: "Scan Completed Safely",    description: "Website https://news.ycombinator.com is safe.", timestamp: Date().addingTimeInterval(-172800), kind: .safe, icon: "checkmark.shield.fill"),
    ]

    static let riskyApps: [MockRiskyApp] = [
        MockRiskyApp(appName: "FlashLite Pro",   packageName: "com.flashlite.pro",   riskLabel: "HIGH",   permissions: "Camera · SMS · Contacts", explanation: "Categorized as a Photo/Image app but requests Camera, SMS, Contacts. Possible SMS data exfiltration."),
        MockRiskyApp(appName: "QuickWallpapers", packageName: "com.quick.walls",     riskLabel: "MEDIUM", permissions: "Location · Storage",     explanation: "Categorized as a Photo/Image app but requests Background Location."),
        MockRiskyApp(appName: "MyNotes",         packageName: "com.example.notes",   riskLabel: "LOW",    permissions: "Storage",                explanation: "Requests storage outside of its declared category baseline."),
        MockRiskyApp(appName: "WhatsApp",        packageName: "com.whatsapp",        riskLabel: "SAFE",   permissions: "Contacts · Camera",      explanation: "Recognized as a trusted application."),
        MockRiskyApp(appName: "Instagram",       packageName: "com.instagram.android", riskLabel: "SAFE", permissions: "Camera · Location",      explanation: "Recognized as a trusted application."),
    ]
}

extension PillKind {
    static func forRiskLabel(_ label: String) -> PillKind {
        switch label {
        case "HIGH":   return .unsafe
        case "MEDIUM": return .warning
        case "LOW":    return .warning
        default:       return .safe
        }
    }
}

extension Date {
    var relativeString: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: self, relativeTo: .now)
    }

    var shortStamp: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM, HH:mm"
        return formatter.string(from: self)
    }
}
