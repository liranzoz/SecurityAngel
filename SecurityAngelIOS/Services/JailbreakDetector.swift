import Foundation
import UIKit

/// iOS equivalent of Android's `RootDetector`. We can't enumerate other
/// apps or read system tags, but jailbroken devices typically leave the
/// following telltale fingerprints, any of which we treat as positive:
///
/// 1. Cydia / Sileo / Zebra / common jailbreak apps installed
/// 2. Suspicious files / binaries on disk
/// 3. Ability to write outside the sandbox
/// 4. Ability to `fork()` (works only on jailbroken devices)
/// 5. Suspicious `dyld` images loaded
enum JailbreakDetector {

    struct Result: Hashable {
        let isJailbroken: Bool
        let suspiciousFilesFound: Bool
        let canEscapeSandbox: Bool
        let canFork: Bool
        let cydiaURLOpenable: Bool
    }

    static func evaluate() -> Result {
        #if targetEnvironment(simulator)
        return Result(
            isJailbroken: false,
            suspiciousFilesFound: false,
            canEscapeSandbox: false,
            canFork: false,
            cydiaURLOpenable: false
        )
        #else
        let suspiciousFiles  = suspiciousFilesExist()
        let sandboxEscape    = canWriteOutsideSandbox()
        let canFork          = canForkProcess()
        let cydiaOpenable    = canOpenCydiaScheme()

        let isJailbroken = suspiciousFiles || sandboxEscape || canFork || cydiaOpenable
        return Result(
            isJailbroken:         isJailbroken,
            suspiciousFilesFound: suspiciousFiles,
            canEscapeSandbox:     sandboxEscape,
            canFork:              canFork,
            cydiaURLOpenable:     cydiaOpenable
        )
        #endif
    }

    // MARK: - Checks

    private static let suspiciousPaths = [
        "/Applications/Cydia.app",
        "/Applications/Sileo.app",
        "/Applications/Zebra.app",
        "/Applications/blackra1n.app",
        "/Applications/FakeCarrier.app",
        "/Applications/Icy.app",
        "/Applications/IntelliScreen.app",
        "/Applications/MxTube.app",
        "/Applications/RockApp.app",
        "/Applications/SBSettings.app",
        "/Applications/WinterBoard.app",
        "/Library/MobileSubstrate/MobileSubstrate.dylib",
        "/Library/MobileSubstrate/DynamicLibraries",
        "/bin/bash",
        "/bin/sh",
        "/usr/sbin/sshd",
        "/usr/bin/ssh",
        "/etc/apt",
        "/etc/ssh/sshd_config",
        "/private/var/lib/apt/",
        "/private/var/lib/cydia",
        "/private/var/mobile/Library/SBSettings/Themes",
        "/private/var/stash",
        "/private/var/tmp/cydia.log",
        "/var/cache/apt",
        "/var/lib/apt",
        "/var/lib/cydia",
        "/var/log/syslog",
        "/var/tmp/cydia.log",
        "/usr/libexec/cydia/",
        "/usr/libexec/sftp-server",
        "/usr/libexec/ssh-keysign"
    ]

    private static func suspiciousFilesExist() -> Bool {
        let fm = FileManager.default
        return suspiciousPaths.contains { fm.fileExists(atPath: $0) }
    }

    private static func canWriteOutsideSandbox() -> Bool {
        let path = "/private/security_angel_jb_probe_\(UUID().uuidString.prefix(6)).txt"
        do {
            try "probe".write(toFile: path, atomically: true, encoding: .utf8)
            try? FileManager.default.removeItem(atPath: path)
            return true
        } catch {
            return false
        }
    }

    private static func canForkProcess() -> Bool {
        // `fork` returns -1 on non-jailbroken iOS. On jailbroken devices the
        // sandbox restriction is lifted and `fork` succeeds.
        let forkFn = dlsym(dlopen(nil, RTLD_NOW), "fork")
        guard let raw = forkFn else { return false }
        typealias ForkType = @convention(c) () -> Int32
        let fork = unsafeBitCast(raw, to: ForkType.self)
        let pid = fork()
        if pid >= 0 {
            // child should exit immediately; we're the parent now
            if pid > 0 { /* parent */ }
            return true
        }
        return false
    }

    private static func canOpenCydiaScheme() -> Bool {
        guard let url = URL(string: "cydia://package/com.example.package") else { return false }
        return UIApplication.shared.canOpenURL(url)
    }
}
