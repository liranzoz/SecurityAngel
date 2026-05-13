import SwiftUI
import Lottie

/// Catalogue of the Lottie animations shipped with the app. Matches the
/// JSON filenames copied across from the Android `res/raw/` folder.
enum LottieAnim: String {
    case vault       = "anim_vault"
    case family      = "anim_family"
    case familyBold  = "anim_family_bold"
    case maleAvatar  = "anim_male_avater"
    case femaleAvatar = "anim_female_avater"
    case elseAvatar  = "anim_else_avatar"
    case loading     = "anim_loading"

    static func avatar(for gender: String) -> LottieAnim {
        switch gender {
        case "Male":   return .maleAvatar
        case "Female": return .femaleAvatar
        default:       return .elseAvatar
        }
    }
}

/// Thin SwiftUI wrapper around `LottieView` so the rest of the app
/// doesn't need to import Lottie.
///
/// Plays on appear by default. `speed` and `loop` are exposed; the
/// default loop mode matches how the Android app plays them.
struct LottieAnimation: View {
    let animation: LottieAnim
    var loop: LottieLoopMode = .loop
    var speed: CGFloat = 1.0

    var body: some View {
        LottieView(animation: .named(animation.rawValue))
            .playing(loopMode: loop)
            .animationSpeed(speed)
    }
}

/// Circular Lottie avatar with a glass background, used in the nav header,
/// settings, and menu sheet (matches the Android `nav_header.xml` layout).
struct LottieAvatar: View {
    let gender: String
    var size: CGFloat = 56

    var body: some View {
        LottieAnimation(animation: LottieAnim.avatar(for: gender))
            .frame(width: size, height: size)
            .background(Brand.headerGradient, in: Circle())
            .clipShape(Circle())
    }
}

#Preview {
    HStack(spacing: 20) {
        LottieAvatar(gender: "Male")
        LottieAvatar(gender: "Female")
        LottieAvatar(gender: "Other")
    }
    .padding()
    .background(Brand.backgroundGradient)
}
