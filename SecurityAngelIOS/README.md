# Security Angel — iOS

Liquid-Glass SwiftUI port of the Android `SecurityAngel` app.

| Phase | Status |
|---|---|
| 1 — Static UI (all screens, mock data) | ✅ done |
| 2 — Core logic (crypto, posture, API clients) | ✅ done |
| 3 — Firebase Auth + Firestore repositories + wired views | ✅ done |
| 4 — AutoFill Credential Provider extension | ✅ done |
| 5 — Push notifications (APNs + FCM) | ⏳ later |
| 6 — Google Sign-In | ⏳ later |

## Enabling AutoFill

The build produces a second target, `SecurityAngelAutoFill`, embedded in
the main app as a Credential Provider extension. **The code is complete
and correct, but Apple gates this feature behind a paid developer
account** — the simulator alone is not enough.

### Why "Sign to Run Locally" doesn't show the extension

iOS requires the entitlement
`com.apple.developer.authentication-services.autofill-credential-provider`
on the extension. That entitlement must be allowlisted by a provisioning
profile, which is only issued to **paid Apple Developer Program members**
($99/year). With "Sign to Run Locally" the build succeeds and the
extension is bundled — pluginkit registers it — but at sign time the
entitlement is **stripped from the binary** (visible via
`codesign -d --entitlements -` returning `<dict></dict>`). Without the
entitlement, iOS refuses to surface the extension in the AutoFill picker.

That's why the toggle never appeared on the simulator. It isn't a bug
in our code — it's an Apple platform requirement.

### To actually use it

1. Enroll in the [Apple Developer Program](https://developer.apple.com/programs/)
   if you haven't ($99/year).
2. In Xcode, select **both** the `SecurityAngelIOS` target and the
   `SecurityAngelAutoFill` target → **Signing & Capabilities** →
   pick your Apple Developer Team.
3. Add the **"AutoFill Credential Provider"** capability on the
   `SecurityAngelAutoFill` target (Xcode → Signing & Capabilities →
   `+ Capability` → AutoFill Credential Provider). Xcode will provision
   a profile that includes the required entitlement.
4. Build & run.
5. On the simulator/device: **Settings → General → AutoFill & Passwords →
   AutoFill From** → toggle on **Security Angel**.
6. Focus a password field in Safari — the QuickType bar shows Security
   Angel. Tap → Face ID → fills the matching credential from your vault.

### How it shares state with the main app (when the entitlement is granted)

- **Keychain Access Group** `com.zoz.SecurityAngelIOS` (declared in both
  targets' entitlements) is the rendezvous point.
- `Auth.auth().useUserAccessGroup(…)` puts the Firebase Auth session in
  the shared group, so the extension's `Auth.auth().currentUser` is the
  same user that's signed in inside the app.
- `KeychainHelper` reads/writes the master vault PIN through the same
  shared group, so Face ID unlock works inside the extension.
- Firestore in the extension reads `users/{uid}/vault` and filters by
  `domain` before decryption.

## Requirements

- Xcode 26.0+
- iOS 18.0+ deployment target (Liquid Glass renders natively on iOS 26+; older iOS gets a `.regularMaterial` fallback)
- Firebase project — see setup below

## First-run setup

### 1. Drop in `GoogleService-Info.plist`

1. Open your existing Firebase project in the [Firebase console](https://console.firebase.google.com/).
2. **Add app → iOS**, enter bundle id **`com.zoz.SecurityAngelIOS`**.
3. Download **`GoogleService-Info.plist`**.
4. Drop it into `SecurityAngelIOS/SecurityAngelIOS/` (next to the `App/` folder).
5. In Xcode's Project Navigator, drag the file into the `SecurityAngelIOS` group — check "Copy items if needed" + the SecurityAngelIOS target.

Without this file the app launches into a setup-instructions screen and stays out of Firebase calls; the build itself still succeeds.

### 2. Set API keys for VirusTotal + Gemini

`Secrets.swift` reads env vars. Set them on the **Run** action of the scheme:

> Product → Scheme → Edit Scheme… → **Run → Arguments → Environment Variables**

| Name | Value |
|---|---|
| `GEMINI_API_KEY` | your Gemini key (same as Android `local.properties`) |
| `VIRUSTOTAL_API_KEY` | your VirusTotal key |

### 3. ⌘R

Sign up (email verification required, same as Android), or sign in with an existing Firestore account.

## How to open

```bash
open SecurityAngelIOS.xcodeproj
```

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
│   ├── JailbreakDetector.swift
│   ├── DevicePostureService.swift
│   ├── VirusTotalAPI.swift
│   ├── HaveIBeenPwnedAPI.swift
│   ├── GeminiAPI.swift
│   ├── AuthRepository.swift           (FirebaseAuth)
│   ├── UserRepository.swift           (users/{uid})
│   ├── FamilyRepository.swift         (families + members)
│   ├── InvitationRepository.swift     (invitations/{email})
│   ├── VaultRepository.swift          (encrypted vault CRUD)
│   ├── ScanHistoryRepository.swift
│   ├── ChatHistoryRepository.swift
│   └── SecurityLogger.swift           (security_logs + risk-count updates)
├── Logic/
│   ├── ScoreCalculator.swift
│   ├── DomainMatcher.swift
│   └── SecurityConstants.swift
├── State/
│   ├── FirebaseSupport.swift          (guarded FirebaseApp.configure())
│   ├── AppState.swift                 (@Observable root — owns repos + listeners)
│   ├── VaultSession.swift             (in-memory PIN + salt cache)
│   └── Secrets.swift                  (API key lookup)
├── Features/
│   ├── Auth/      LoginView · SignUpView                        (Firebase Auth)
│   ├── Dashboard/ DashboardView                                 (real score)
│   ├── Scanner/   URLScannerView                                (VirusTotal → Firestore)
│   ├── AI/        AIChatView (Gemini + real RAG context)         · ChatBubble
│   ├── Vault/     PasswordVaultView (Firestore + crypto)         · Row · AddSheet
│   ├── Generator/ PasswordGeneratorView
│   ├── Family/    Safety · Management · AddMember (invites) · JoinFamily
│   ├── Posture/   DevicePostureView (real jailbreak detection)
│   ├── Logs/      SecurityLogView (admin-only family timeline)
│   ├── Settings/  SettingsView (real sign out)
│   └── Menu/      MenuSheet
└── Mock/          MockData (preview-only fixtures)
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

The hamburger button in any header opens `MenuSheet` (Family Management, Password Generator, Device Posture, Activity Log, Settings).

## Cross-platform parity

- **Same Firebase project** — sign in with an existing Android user, see the same `users/{uid}` doc, family membership, vault, scans, chat history, security logs.
- **Vault crypto is byte-for-byte compatible with Android**: PBKDF2-HMAC-SHA256, 100,000 iterations, 256-bit key, AES-256-GCM with 12-byte IV + 16-byte tag, base64. Decrypt-from-Android works without any transformation.
- **Same Firestore schema** — `users/{uid}/vault/{id}`, `users/{uid}/scans/{id}`, `users/{uid}/chat_history/{id}`, `families/{familyId}`, `invitations/{email}`, `security_logs/{id}`. Field names match Android exactly.
- **Same Gemini prompt format** — `SYSTEM_CONTEXT` block with vault/family/scan/posture stats, then user input. `[ACTION:CODE]` tag parsing works.

## Differences from Android

- **TabView + MenuSheet** instead of a hamburger drawer (Apple HIG).
- **No Permission Monitor** — iOS sandboxes other apps' permissions. Replaced by `DevicePostureView` (jailbreak, passcode set, biometrics, sandbox integrity, Lockdown Mode).
- **Root Check** folded into `DevicePostureView`.
- **AI chat action deep-links**: the alert confirmation appears but actual navigation will be wired in a follow-up (needs tab + sheet bindings exposed to AIChatView).
- **Email verification flow**: same as Android.
- **No Google Sign-In yet** — needs `REVERSED_CLIENT_ID` URL scheme in Info.plist.
- **No push notifications yet** — Android's legacy FCM HTTP API was deprecated mid-2024; iOS path is APNs + a Cloud Function.

## What works now

- ✅ Sign up + email verification + sign in (Firebase Auth).
- ✅ Per-user Firestore document with realtime listener.
- ✅ Family create / invite (6-digit code + share sheet) / join / remove member.
- ✅ Password vault: encrypted CRUD with Android-compatible crypto, search, Face ID unlock, "scan for leaks" via HIBP, leak flagging persisted to Firestore.
- ✅ URL scanner persists results to `users/{uid}/scans` and logs to `security_logs`.
- ✅ AI chat with real RAG context (vault counts, family status, last scan, jailbreak), persisted to `chat_history`.
- ✅ Dashboard shows real personal score (or family-blended for admins), real recent scans, real family alert count.
- ✅ Activity log: admin-only view of `security_logs` with snapshot listener + clear-all.
- ✅ Settings sign-out clears Keychain PIN + vault session + Firebase Auth.

## Next phase ideas

- AutoFill extension (`ASCredentialProviderViewController`) sharing the Keychain access group + vault Firestore reads.
- Google Sign-In + URL scheme.
- APNs + Cloud Function for family admin alerts.
- Hook the AI's `[ACTION:OPEN_VAULT]` etc. into actual tab + sheet navigation.
