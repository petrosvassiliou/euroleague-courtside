import SwiftUI
import AppKit
import CourtsideCore

// MARK: - macOS Liquid Glass Design System (Daylight Light Default & Obsidian Dark)

public struct LiquidGlassPalette {
    public let mode: AppThemeMode
    public let euroLeagueOrange = Color(red: 1.0, green: 0.40, blue: 0.0)
    public let neonMadeGreen = Color(red: 0.04, green: 0.78, blue: 0.44)
    public let neonMissedRed = Color(red: 0.94, green: 0.24, blue: 0.30)
    public let skyAccent = Color(red: 0.08, green: 0.56, blue: 0.96)

    public var primaryText: Color {
        mode.isLight ? Color(red: 0.06, green: 0.09, blue: 0.16) : Color(red: 0.96, green: 0.97, blue: 0.99)
    }
    public var secondaryText: Color {
        mode.isLight ? Color(red: 0.24, green: 0.30, blue: 0.40) : Color(red: 0.74, green: 0.78, blue: 0.86)
    }
    public var mutedText: Color {
        mode.isLight ? Color(red: 0.40, green: 0.46, blue: 0.56) : Color(red: 0.55, green: 0.60, blue: 0.70)
    }
    public var cardTint: Color {
        mode.isLight ? Color.white.opacity(0.62) : Color(red: 0.09, green: 0.10, blue: 0.14).opacity(0.64)
    }
    public var pillSurface: Color {
        mode.isLight ? Color.white.opacity(0.72) : Color.white.opacity(0.07)
    }
    public var specularTopBorder: Color {
        mode.isLight ? Color.white.opacity(0.92) : Color.white.opacity(0.24)
    }
    public var specularBottomBorder: Color {
        mode.isLight ? Color.black.opacity(0.08) : Color.white.opacity(0.06)
    }
}

private struct LiquidGlassPaletteKey: EnvironmentKey {
    static let defaultValue = LiquidGlassPalette(mode: .light)
}

extension EnvironmentValues {
    var glassPalette: LiquidGlassPalette {
        get { self[LiquidGlassPaletteKey.self] }
        set { self[LiquidGlassPaletteKey.self] = newValue }
    }
}

extension Color {
    init(hex: UInt64, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}

/// Applies genuine macOS 26+ `.glassEffect` Liquid Glass when available, enhanced with refractive
/// specular highlights, and gracefully layers `.ultraThinMaterial` on macOS 14–15.
struct LiquidGlassSurfaceModifier: ViewModifier {
    var cornerRadius: CGFloat = 18
    var isSelected: Bool = false
    var accentGlow: Color? = nil
    var interactive: Bool = false

    @Environment(\.glassPalette) private var palette

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        let rimColors: [Color] = {
            if isSelected, let glow = accentGlow {
                return [glow.opacity(0.85), palette.specularTopBorder, glow.opacity(0.45)]
            }
            return [palette.specularTopBorder, palette.specularBottomBorder, palette.specularTopBorder.opacity(0.5)]
        }()

        Group {
            #if compiler(>=6.2)
            if #available(macOS 26.0, *) {
                content
                    .background(palette.cardTint, in: shape)
                    .glassEffect(
                        interactive ? .regular.interactive() : .regular,
                        in: shape
                    )
                    .overlay(
                        shape.strokeBorder(
                            LinearGradient(colors: rimColors, startPoint: .topLeading, endPoint: .bottomTrailing),
                            lineWidth: isSelected ? 1.5 : 1.0
                        )
                    )
            } else {
                fallbackGlass(content: content, shape: shape, rimColors: rimColors)
            }
            #else
            fallbackGlass(content: content, shape: shape, rimColors: rimColors)
            #endif
        }
        .shadow(
            color: (isSelected ? (accentGlow ?? palette.euroLeagueOrange).opacity(0.16) : Color.black.opacity(palette.mode.isLight ? 0.06 : 0.28)),
            radius: isSelected ? 14 : 10,
            x: 0,
            y: 4
        )
    }

    @ViewBuilder
    private func fallbackGlass(content: Content, shape: RoundedRectangle, rimColors: [Color]) -> some View {
        content
            .background {
                ZStack {
                    shape.fill(.ultraThinMaterial)
                    shape.fill(palette.cardTint)
                    shape.fill(
                        LinearGradient(
                            colors: [
                                Color.white.opacity(palette.mode.isLight ? 0.45 : 0.10),
                                Color.clear
                            ],
                            startPoint: .topLeading,
                            endPoint: .center
                        )
                    )
                }
            }
            .overlay(
                shape.strokeBorder(
                    LinearGradient(colors: rimColors, startPoint: .topLeading, endPoint: .bottomTrailing),
                    lineWidth: isSelected ? 1.5 : 1.0
                )
            )
    }
}

extension View {
    func liquidGlassCard(
        cornerRadius: CGFloat = 18,
        isSelected: Bool = false,
        accentGlow: Color? = nil,
        interactive: Bool = false
    ) -> some View {
        modifier(LiquidGlassSurfaceModifier(
            cornerRadius: cornerRadius,
            isSelected: isSelected,
            accentGlow: accentGlow,
            interactive: interactive
        ))
    }
}

/// Ambient refractive mesh-like backdrop behind Liquid Glass surfaces, tuned to the user's selected club!
struct LiquidGlassAmbientBackdrop: View {
    let activeTeam: EuroLeagueTeam
    let mode: AppThemeMode

    var body: some View {
        ZStack {
            if mode.isLight {
                LinearGradient(
                    colors: [
                        Color(red: 0.94, green: 0.96, blue: 0.99),
                        Color(red: 0.91, green: 0.94, blue: 0.98),
                        Color(red: 0.96, green: 0.95, blue: 0.94)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            } else {
                LinearGradient(
                    colors: [
                        Color(red: 0.04, green: 0.05, blue: 0.08),
                        Color(red: 0.07, green: 0.08, blue: 0.12),
                        Color(red: 0.05, green: 0.05, blue: 0.07)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }

            GeometryReader { geo in
                Circle()
                    .fill(Color(hex: activeTeam.primaryColorHex, alpha: mode.isLight ? 0.16 : 0.22))
                    .frame(width: 440, height: 440)
                    .blur(radius: 95)
                    .offset(x: -80, y: -90)

                Circle()
                    .fill(Color(red: 1.0, green: 0.40, blue: 0.0).opacity(mode.isLight ? 0.13 : 0.18))
                    .frame(width: 380, height: 380)
                    .blur(radius: 90)
                    .offset(x: geo.size.width - 280, y: geo.size.height - 260)

                Circle()
                    .fill(Color(hex: activeTeam.secondaryColorHex, alpha: mode.isLight ? 0.10 : 0.15))
                    .frame(width: 320, height: 320)
                    .blur(radius: 85)
                    .offset(x: geo.size.width * 0.42, y: -60)
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: - Shared Club Crest & Top-Aligned 3:4 Player Headshot Components

struct ClubCrestView: View {
    let team: EuroLeagueTeam
    var size: CGFloat = 32

    var body: some View {
        Group {
            if let localImg = Self.loadBundledCrest(named: team.localLogoResName.isEmpty ? "club_logo_\(team.id.lowercased())" : team.localLogoResName) {
                Image(nsImage: localImg)
                    .resizable()
                    .interpolation(.high)
                    .scaledToFit()
            } else if let url = URL(string: team.logoUrl), !team.logoUrl.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().interpolation(.high).scaledToFit()
                    default:
                        fallbackBadge
                    }
                }
            } else {
                fallbackBadge
            }
        }
        .frame(width: size, height: size)
    }

    private var fallbackBadge: some View {
        ZStack {
            Circle()
                .fill(Color(hex: team.primaryColorHex))
            Text(team.code)
                .font(.system(size: size * 0.34, weight: .black, design: .rounded))
                .foregroundStyle(.white)
        }
    }

    private static var imageCache: [String: NSImage] = [:]

    static func loadBundledCrest(named resName: String) -> NSImage? {
        if let cached = imageCache[resName] {
            return cached
        }
        let candidates: [URL?] = [
            Bundle.main.url(forResource: resName, withExtension: "png"),
            Bundle.main.resourceURL?.appendingPathComponent("\(resName).png"),
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
                .appendingPathComponent("app/src/main/res/drawable-nodpi/\(resName).png"),
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
                .appendingPathComponent("../app/src/main/res/drawable-nodpi/\(resName).png")
        ]
        for cand in candidates.compactMap({ $0 }) {
            if FileManager.default.fileExists(atPath: cand.path), let img = NSImage(contentsOf: cand) {
                imageCache[resName] = img
                return img
            }
        }
        return nil
    }
}

/// Top-aligned 3:4 portrait crop (`750x1000`) with 3pt top headroom so player heads are NEVER cut off!
struct PlayerHeadshotAvatar: View {
    let playerName: String
    let headshotUrl: String
    let clubColorHex: UInt64
    var size: CGFloat = 46

    var body: some View {
        ZStack {
            Circle()
                .fill(Color(hex: clubColorHex, alpha: 0.16))
            if let url = URL(string: headshotUrl), !headshotUrl.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable()
                            .interpolation(.high)
                            .scaledToFill()
                            .padding(.top, 3)
                            .frame(width: size, height: size * 1.28, alignment: .top)
                            .frame(width: size, height: size, alignment: .top)
                            .clipShape(Circle())
                    default:
                        initialsView
                    }
                }
            } else {
                initialsView
            }
        }
        .frame(width: size, height: size)
        .overlay(
            Circle().strokeBorder(Color(hex: clubColorHex, alpha: 0.75), lineWidth: 1.5)
        )
    }

    private var initialsView: some View {
        let initials = playerName
            .split(separator: " ")
            .prefix(2)
            .compactMap { $0.first.map(String.init) }
            .joined()
        return Text(initials.isEmpty ? "EL" : initials)
            .font(.system(size: size * 0.34, weight: .heavy, design: .rounded))
            .foregroundStyle(Color(hex: clubColorHex))
    }
}
