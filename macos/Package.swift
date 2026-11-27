// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "EuroLeagueCourtsideMac",
    platforms: [
        .macOS(.v14)
    ],
    products: [
        .library(name: "CourtsideCore", targets: ["CourtsideCore"]),
        .executable(name: "EuroLeagueCourtsideMac", targets: ["EuroLeagueCourtsideMac"]),
        .executable(name: "CourtsideMacTests", targets: ["CourtsideMacTests"])
    ],
    targets: [
        .target(
            name: "CourtsideCore",
            path: "Sources/CourtsideCore"
        ),
        .executableTarget(
            name: "EuroLeagueCourtsideMac",
            dependencies: ["CourtsideCore"],
            path: "Sources/EuroLeagueCourtsideMac"
        ),
        .executableTarget(
            name: "CourtsideMacTests",
            dependencies: ["CourtsideCore"],
            path: "Tests/CourtsideCoreTests"
        )
    ]
)
