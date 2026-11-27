#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION_NAME="${1:-2.0.0-beta.2}"
BUILD_NUMBER="${2:-26}"

APP_NAME="EuroLeague Courtside"
BUNDLE_DIR="${ROOT_DIR}/macos/dist/${APP_NAME}.app"
CONTENTS_DIR="${BUNDLE_DIR}/Contents"
MACOS_BIN_DIR="${CONTENTS_DIR}/MacOS"
RESOURCES_DIR="${CONTENTS_DIR}/Resources"

echo "🏀 Building ${APP_NAME} v${VERSION_NAME} (Build ${BUILD_NUMBER}) for macOS Liquid Glass…"

# 1. Run Core & Feature Parity Test Suite
(cd "${ROOT_DIR}" && swift run --package-path macos CourtsideMacTests)

# 2. Build Release Native Binary
swift build -c release --package-path "${ROOT_DIR}/macos" --product EuroLeagueCourtsideMac
BIN_PATH="$(swift build -c release --package-path "${ROOT_DIR}/macos" --show-bin-path)/EuroLeagueCourtsideMac"

# 3. Assemble .app Bundle Structure
rm -rf "${BUNDLE_DIR}"
mkdir -p "${MACOS_BIN_DIR}" "${RESOURCES_DIR}"
cp "${BIN_PATH}" "${MACOS_BIN_DIR}/EuroLeagueCourtsideMac"
chmod +x "${MACOS_BIN_DIR}/EuroLeagueCourtsideMac"

# 4. Bundle Offline Live Snapshot & All 20 Official Club Crests
cp "${ROOT_DIR}/app/src/main/assets/euroleague_live_snapshot.json" "${RESOURCES_DIR}/euroleague_live_snapshot.json"
cp "${ROOT_DIR}/app/src/main/res/drawable-nodpi/club_logo_"*.png "${RESOURCES_DIR}/"

# 5. Generate Liquid Glass macOS AppIcon.icns via CoreGraphics + iconutil
ICONSET_DIR="${ROOT_DIR}/macos/dist/AppIcon.iconset"
rm -rf "${ICONSET_DIR}"
mkdir -p "${ICONSET_DIR}"

swift - "${ICONSET_DIR}" << 'SWIFT_ICON'
import AppKit
import CoreGraphics

let iconsetPath = CommandLine.arguments[1]
let sizes = [16, 32, 64, 128, 256, 512, 1024]

for px in sizes {
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil,
        pixelsWide: px,
        pixelsHigh: px,
        bitsPerSample: 8,
        samplesPerPixel: 4,
        hasAlpha: true,
        isPlanar: false,
        colorSpaceName: .deviceRGB,
        bytesPerRow: 0,
        bitsPerPixel: 0
    )!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let ctx = NSGraphicsContext.current!.cgContext
    let s = CGFloat(px)
    let pad = s * 0.08
    let rect = CGRect(x: pad, y: pad, width: s - pad * 2, height: s - pad * 2)
    let radius = rect.width * 0.225

    // Liquid Glass Dark Obsidian Squircle Base
    let bgPath = CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil)
    ctx.saveGState()
    ctx.addPath(bgPath)
    ctx.clip()
    let colors = [
        CGColor(red: 0.14, green: 0.16, blue: 0.24, alpha: 1.0),
        CGColor(red: 0.05, green: 0.06, blue: 0.09, alpha: 1.0)
    ] as CFArray
    let grad = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: colors, locations: [0.0, 1.0])!
    ctx.drawLinearGradient(grad, start: CGPoint(x: 0, y: s), end: CGPoint(x: s, y: 0), options: [])

    // Specular Top-Left Liquid Glass Highlight
    let sheenColors = [
        CGColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 0.28),
        CGColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 0.0)
    ] as CFArray
    let sheenGrad = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: sheenColors, locations: [0.0, 1.0])!
    ctx.drawLinearGradient(sheenGrad, start: CGPoint(x: pad, y: s - pad), end: CGPoint(x: s * 0.55, y: s * 0.45), options: [])
    ctx.restoreGState()

    // Glowing EuroLeague Orange Basketball
    let ballR = rect.width * 0.34
    let center = CGPoint(x: s / 2, y: s / 2)
    let ballRect = CGRect(x: center.x - ballR, y: center.y - ballR, width: ballR * 2, height: ballR * 2)
    ctx.setFillColor(CGColor(red: 1.0, green: 0.40, blue: 0.0, alpha: 1.0))
    ctx.fillEllipse(in: ballRect)

    // Basketball Seams
    ctx.setStrokeColor(CGColor(red: 0.06, green: 0.07, blue: 0.10, alpha: 0.92))
    ctx.setLineWidth(max(1.5, s * 0.028))
    ctx.move(to: CGPoint(x: center.x - ballR, y: center.y))
    ctx.addLine(to: CGPoint(x: center.x + ballR, y: center.y))
    ctx.move(to: CGPoint(x: center.x, y: center.y - ballR))
    ctx.addLine(to: CGPoint(x: center.x, y: center.y + ballR))
    ctx.strokePath()

    // Refractive Rim Border
    ctx.addPath(bgPath)
    ctx.setStrokeColor(CGColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 0.35))
    ctx.setLineWidth(max(1.0, s * 0.015))
    ctx.strokePath()

    NSGraphicsContext.restoreGraphicsState()
    let png = rep.representation(using: .png, properties: [:])!
    try! png.write(to: URL(fileURLWithPath: "\(iconsetPath)/icon_\(px)x\(px).png"))
}
SWIFT_ICON

cp "${ICONSET_DIR}/icon_32x32.png" "${ICONSET_DIR}/icon_16x16@2x.png"
cp "${ICONSET_DIR}/icon_64x64.png" "${ICONSET_DIR}/icon_32x32@2x.png"
cp "${ICONSET_DIR}/icon_256x256.png" "${ICONSET_DIR}/icon_128x128@2x.png"
cp "${ICONSET_DIR}/icon_512x512.png" "${ICONSET_DIR}/icon_256x256@2x.png"
cp "${ICONSET_DIR}/icon_1024x1024.png" "${ICONSET_DIR}/icon_512x512@2x.png"
rm -f "${ICONSET_DIR}/icon_64x64.png" "${ICONSET_DIR}/icon_1024x1024.png"

iconutil -c icns "${ICONSET_DIR}" -o "${RESOURCES_DIR}/AppIcon.icns"
rm -rf "${ICONSET_DIR}"

# 6. Write Info.plist
cat > "${CONTENTS_DIR}/Info.plist" << PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>en</string>
    <key>CFBundleDisplayName</key>
    <string>EuroLeague Courtside</string>
    <key>CFBundleExecutable</key>
    <string>EuroLeagueCourtsideMac</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.euroleague.courtside.macos</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>EuroLeague Courtside</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION_NAME}</string>
    <key>CFBundleVersion</key>
    <string>${BUILD_NUMBER}</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
</dict>
</plist>
PLIST

# 7. Ad-hoc Code Sign & Verify
codesign --force --deep --sign - "${BUNDLE_DIR}"
codesign --verify --verbose=2 "${BUNDLE_DIR}"

# 8. Package Distributable .zip and Drag-to-Applications .dmg
ZIP_OUT="${ROOT_DIR}/macos/dist/EuroLeague-Courtside-v${VERSION_NAME}-macOS.zip"
DMG_OUT="${ROOT_DIR}/macos/dist/EuroLeague-Courtside-v${VERSION_NAME}-macOS.dmg"
DMG_STAGE="${ROOT_DIR}/macos/dist/dmg_stage"

rm -f "${ZIP_OUT}" "${DMG_OUT}"
ditto -c -k --sequesterRsrc --keepParent "${BUNDLE_DIR}" "${ZIP_OUT}"

rm -rf "${DMG_STAGE}"
mkdir -p "${DMG_STAGE}"
cp -R "${BUNDLE_DIR}" "${DMG_STAGE}/"
ln -s /Applications "${DMG_STAGE}/Applications"
hdiutil create -volname "EuroLeague Courtside ${VERSION_NAME}" -srcfolder "${DMG_STAGE}" -ov -format UDZO "${DMG_OUT}" >/dev/null
rm -rf "${DMG_STAGE}"

echo "✅ Packaged macOS Liquid Glass App, DMG & ZIP:"
ls -lh "${ZIP_OUT}" "${DMG_OUT}"
