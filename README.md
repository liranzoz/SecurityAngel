# Security Angel — iOS

Liquid-Glass SwiftUI port of the Android `SecurityAngel` app. This phase contains **only the static UI** — no Firebase, no Gemini, no networking. All screens are populated with mock data from `Mock/MockData.swift`.

## Requirements

- Xcode 26.0+
- iOS 18.0+ deployment target (Liquid Glass effects render natively on iOS 26+; older iOS gets a `.regularMaterial` fallback automatically)

## How to open

```
open SecurityAngelIOS.xcodeproj
```

Pick an iPhone 16 / 17 simulator and hit ⌘R.

## What's here

```
SecurityAngelIOS/
├── App/                  app entry + tabs + theme
├── DesignSystem/         GlassCard, ScoreRing, StatusPill, PrimaryButton, GlassTextField…
├── Features/
│   ├── Auth/             LoginView, SignUpView
│   ├── Dashboard/        DashboardView (score ring + tiles + recent scans)
│   ├── Scanner/          URLScannerView (mock VirusTotal flow)
│   ├── AI/               AIChatView + ChatBubble
│   ├── Vault/            PasswordVaultView + Row + AddSheet (with Face ID lock screen)
│   ├── Generator/        PasswordGeneratorView
│   ├── Family/           Safety, Management, AddMember, JoinFamily
│   ├── Posture/          DevicePostureView (replaces Android's Root Check + Permission Monitor)
│   ├── Logs/             SecurityLogView
│   ├── Settings/         SettingsView
│   └── Menu/             MenuSheet (overflow drawer)
└── Mock/                 MockData
```

## Navigation map

Five-tab `TabView` (Liquid Glass automatically on iOS 26+):

| Tab    | Screen                |
|--------|-----------------------|
| Home   | DashboardView         |
| Scanner| URLScannerView        |
| AI     | AIChatView            |
| Vault  | PasswordVaultView     |
| Family | FamilySafetyView      |

Tap the hamburger button in any screen's header → `MenuSheet` opens with:
- Family Management
- Password Generator
- Device Posture
- Activity Log
- Settings (which contains Sign Out)

## Next phases (not in this build)

1. Firebase Auth + Firestore models (port `User`, `Family`, `PasswordAccount`, `ScanHistoryItem`, `SecurityLog`, `ChatMessage` as `Codable` structs)
2. `VaultCryptoManager` (CryptoKit AES-GCM + CommonCrypto PBKDF2 — **must match Android byte-for-byte** to share encrypted vault docs across platforms)
3. Keychain biometric PIN wrap (`SecAccessControlCreateWithFlags(.biometryCurrentSet)`)
4. VirusTotal & HaveIBeenPwned `URLSession` clients
5. Gemini Swift SDK + system-context builder + `[ACTION:…]` deep-linking
6. Jailbreak + device posture checks
7. `ASCredentialProviderViewController` AutoFill extension

## Differences from Android

- **No hamburger drawer** — replaced with `TabView` + an overflow `MenuSheet`, per Apple HIG.
- **No Permission Monitor** — iOS doesn't expose third-party apps' permissions. Replaced by `DevicePostureView` (jailbreak, passcode set, biometrics, iCloud Keychain, auto-lock).
- **Root Check** → folded into the same `DevicePostureView`.
- **Glide favicons** → `AsyncImage` from `google.com/s2/favicons`.
- **Lottie** → SF Symbols for now. Drop the existing `.json` files into `Resources/Lottie/` and use `Lottie-iOS` package when needed.
