# Security Angel — iOS

Liquid-Glass SwiftUI port of the Android `SecurityAngel` app.

| Phase | Status |
|---|---|
| 1 — Static UI (all screens, mock data) | ✅ done |
| 2 — Core logic (crypto, posture, API clients) | ✅ done |
| 3 — Firebase Auth + Firestore repositories | ⏳ next |
| 4 — AutoFill Credential Provider extension | ⏳ later |
| 5 — Push notifications (APNs + FCM) | ⏳ later |

## Requirements

- Xcode 26.0+
- iOS 18.0+ deployment target (Liquid Glass renders natively on iOS 26+; older iOS gets a `.regularMaterial` fallback)

## How to open

```bash
open SecurityAngelIOS.xcodeproj
```

Pick an iPhone 16 / 17 simulator and hit ⌘R.

## API keys (Gemini + VirusTotal)

`Secrets.swift` reads from environment variables by default. Set them on the **Run** action of the scheme:

> Product → Scheme → Edit Scheme… → **Run** → **Arguments** → **Environment Variables**

Add:

| Name | Value |
|---|---|
| `GEMINI_API_KEY` | your Gemini key (same one Android uses) |
| `VIRUSTOTAL_API_KEY` | your VirusTotal key |

Or, for local dev only, hardcode them in `State/Secrets.swift` and add `Secrets.swift` to `.gitignore`.

## Layout

```
SecurityAngelIOS/
├── App/                   SecurityAngelIOSApp · RootView · Theme
├── DesignSystem/          LiquidGlass · GlassCard · ScoreRing · StatusPill · …
├── Models/                Codable structs mirroring the Firestore schema
├── Crypto/
│   ├── VaultCryptoManager.swift  (AES-GCM + PBKDF2 — byte-compatible with Android)
│   └── KeychainHelper.swift      (biometric-wrapped PIN, .biometryCurrentSet)
├── Services/
│   ├── JailbreakDetector.swift   (iOS analogue of RootDetector)
│   ├── DevicePostureService.swift
│   ├── VirusTotalAPI.swift       (URLSession, full scan flow)
│   ├── HaveIBeenPwnedAPI.swift   (k-anonymity password breach check)
│   └── GeminiAPI.swift           (REST, multimodal, system-context builder)
├── Logic/
│   ├── ScoreCalculator.swift     (personal + family + permissions blend)
│   ├── DomainMatcher.swift
│   └── SecurityConstants.swift
├── State/
│   └── Secrets.swift             (API key lookup)
├── Features/
│   ├── Auth/      LoginView · SignUpView
│   ├── Dashboard/ DashboardView
│   ├── Scanner/   URLScannerView (real VirusTotal calls)
│   ├── AI/        AIChatView (real Gemini multimodal) · ChatBubble
│   ├── Vault/     PasswordVaultView (real Face ID unlock) · Row · AddSheet
│   ├── Generator/ PasswordGeneratorView
│   ├── Family/    Safety · Management · AddMember · JoinFamily
│   ├── Posture/   DevicePostureView (real jailbreak detection)
│   ├── Logs/      SecurityLogView
│   ├── Settings/  SettingsView
│   └── Menu/      MenuSheet
└── Mock/          MockData (still used by Vault/Family/Logs until Firebase lands)
```

## Navigation

Five-tab `TabView`:

| Tab    | Screen                |
|--------|-----------------------|
| Home   | DashboardView         |
| Scanner| URLScannerView        |
| AI     | AIChatView            |
| Vault  | PasswordVaultView     |
| Family | FamilySafetyView      |

The hamburger button in any header opens `MenuSheet` for: Family Management, Password Generator, Device Posture, Activity Log, Settings.

## Differences from Android

- **No hamburger drawer** — replaced with `TabView` + an overflow `MenuSheet`, per Apple HIG.
- **No Permission Monitor** — iOS sandboxes other apps' permissions. Replaced by `DevicePostureView` (jailbreak, passcode set, biometrics, sandbox integrity, Lockdown Mode).
- **Root Check** → folded into `DevicePostureView`.
- **Vault crypto is byte-for-byte compatible with Android**: PBKDF2-HMAC-SHA256, 100,000 iterations, 256-bit key, AES-256-GCM with 12-byte IV + 16-byte tag, base64 wire format. Vault docs encrypted on either platform decrypt on the other.
- **Gemini** via REST (no SDK dependency). System-context prompt and `[ACTION:CODE]` tag parsing match Android exactly.
- **Glide favicons** → SwiftUI `AsyncImage` from `google.com/s2/favicons`.
- **Lottie** → SF Symbols for now; can adopt Lottie-iOS later.

## What works right now without Firebase

- ✅ URL scanner with real VirusTotal lookup → submit → poll → final report.
- ✅ AI chat with real Gemini multimodal (text + photo).
- ✅ Device posture with real jailbreak detection.
- ✅ Vault Face ID unlock via Keychain biometric-wrapped PIN (the PIN itself is set up next phase, with sign-in).
- ✅ Password generator.

## What's coming next phase

- Firebase Auth (email + Google sign-in) → real Login/SignUp.
- Firestore repositories for users, families, vault, scans, chat history, security logs, invitations.
- Vault wired to Firestore using the byte-compatible crypto.
- Real per-user security score on Dashboard (admin gets family-blended score).
- Family Safety with live snapshot listeners.
- Activity log (admin-only).
- Real settings (dark mode, screenshot block via `FLAG_SECURE` equivalent, sign out).
