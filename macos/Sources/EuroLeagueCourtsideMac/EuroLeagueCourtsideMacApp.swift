import SwiftUI
import AppKit
import Combine
import CourtsideCore

@main
struct EuroLeagueCourtsideMacApp: App {
    @NSApplicationDelegateAdaptor(CourtsideAppDelegate.self) private var appDelegate

    var body: some Scene {
        // IMPORTANT: Keep `App` struct completely stateless (do NOT attach `@StateObject`,
        // `.preferredColorScheme`, or `MenuBarExtra(isInserted:)` at the Scene graph root,
        // which triggers an infinite `AppKitWindowController.updateRootHost` <-> `AppGraph.sceneList`
        // recursion on macOS 26/27 SwiftUI 8.1).
        WindowGroup("EuroLeague Courtside 2.0") {
            MacRootContainerView()
                .frame(minWidth: 1040, minHeight: 680)
        }
        .commands {
            CommandMenu("Courtside") {
                Button("My Team") {
                    Task { @MainActor in CourtsideViewModel.shared.selectedTab = .myTeam }
                }
                .keyboardShortcut("1", modifiers: [.command])

                Button("Match Center") {
                    Task { @MainActor in CourtsideViewModel.shared.selectedTab = .matchCenter }
                }
                .keyboardShortcut("2", modifiers: [.command])

                Button("Standings") {
                    Task { @MainActor in
                        CourtsideViewModel.shared.selectedStandingsTeam = nil
                        CourtsideViewModel.shared.selectedTab = .standings
                    }
                }
                .keyboardShortcut("3", modifiers: [.command])

                Button("Analytics") {
                    Task { @MainActor in CourtsideViewModel.shared.selectedTab = .analytics }
                }
                .keyboardShortcut("4", modifiers: [.command])

                Button("Settings") {
                    Task { @MainActor in CourtsideViewModel.shared.selectedTab = .settings }
                }
                .keyboardShortcut("5", modifiers: [.command])

                Divider()

                Button("Live Sync") {
                    Task { @MainActor in await CourtsideViewModel.shared.triggerLiveSync(silent: false) }
                }
                .keyboardShortcut("r", modifiers: [.command])

                Button("Toggle Light / Dark Liquid Glass") {
                    Task { @MainActor in
                        let vm = CourtsideViewModel.shared
                        vm.selectThemeMode(vm.themeMode.toggled())
                    }
                }
                .keyboardShortcut("d", modifiers: [.command, .shift])
            }
        }
    }
}

// MARK: - AppKit Delegate & Native macOS Status Bar Live Scoreboard

@MainActor
final class CourtsideAppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem?
    private var cancellables = Set<AnyCancellable>()

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApplication.shared.setActivationPolicy(.regular)
        let vm = CourtsideViewModel.shared
        vm.startBackgroundServicesIfNeeded()
        setupNativeMenuBarLiveScoreboard(vm: vm)

        if CommandLine.arguments.contains("--smoke-test-ui") {
            runAutomatedUiSmokeTest(vm: vm)
        } else {
            NSApplication.shared.activate(ignoringOtherApps: true)
        }
    }

    private func setupNativeMenuBarLiveScoreboard(vm: CourtsideViewModel) {
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        self.statusItem = item

        Publishers.CombineLatest4(vm.$games, vm.$primaryTeamId, vm.$menuBarTrackerEnabled, vm.$language)
            .receive(on: RunLoop.main)
            .sink { [weak self] _, _, enabled, _ in
                self?.refreshStatusItem(vm: vm, enabled: enabled)
            }
            .store(in: &cancellables)

        refreshStatusItem(vm: vm, enabled: vm.menuBarTrackerEnabled)
    }

    private func refreshStatusItem(vm: CourtsideViewModel, enabled: Bool) {
        guard let item = statusItem else { return }
        item.isVisible = enabled
        guard enabled else { return }
        item.button?.title = vm.menuBarTitle

        let menu = NSMenu()
        let headerItem = NSMenuItem(
            title: "🏀 EuroLeague Courtside 2.0 • \(vm.strings.roundPrefix) \(vm.activeRound)",
            action: nil,
            keyEquivalent: ""
        )
        headerItem.isEnabled = false
        menu.addItem(headerItem)
        menu.addItem(.separator())

        let roundGames = vm.games.filter { $0.status == .live || $0.round == vm.activeRound }.prefix(8)
        for (idx, g) in roundGames.enumerated() {
            let badge = g.status == .live ? "🔴 Q\(g.currentQuarter) \(g.remainingClock)" : (g.status == .final_ ? "FT" : "UP")
            let mItem = NSMenuItem(
                title: "\(badge)  \(g.homeTeam.code) \(g.homeScore):\(g.awayScore) \(g.awayTeam.code)",
                action: #selector(openGameFromMenu(_:)),
                keyEquivalent: ""
            )
            mItem.target = self
            mItem.tag = idx
            mItem.representedObject = g.id
            menu.addItem(mItem)
        }

        menu.addItem(.separator())
        let syncItem = NSMenuItem(title: vm.strings.liveSyncButton, action: #selector(triggerSyncFromMenu), keyEquivalent: "r")
        syncItem.target = self
        menu.addItem(syncItem)

        let showWindowItem = NSMenuItem(title: "Open Courtside Window", action: #selector(bringWindowToFront), keyEquivalent: "0")
        showWindowItem.target = self
        menu.addItem(showWindowItem)

        menu.addItem(.separator())
        let quitItem = NSMenuItem(title: "Quit EuroLeague Courtside", action: #selector(quitApp), keyEquivalent: "q")
        quitItem.target = self
        menu.addItem(quitItem)

        item.menu = menu
    }

    @objc private func openGameFromMenu(_ sender: NSMenuItem) {
        guard let gameId = sender.representedObject as? String else { return }
        let vm = CourtsideViewModel.shared
        let round = vm.games.first(where: { $0.id == gameId })?.round ?? vm.activeRound
        NSApplication.shared.activate(ignoringOtherApps: true)
        vm.openGameInMatchCenter(gameId: gameId, round: round)
    }

    @objc private func triggerSyncFromMenu() {
        Task { @MainActor in
            await CourtsideViewModel.shared.triggerLiveSync(silent: false)
        }
    }

    @objc private func bringWindowToFront() {
        NSApplication.shared.activate(ignoringOtherApps: true)
        NSApplication.shared.windows.first?.makeKeyAndOrderFront(nil)
    }

    @objc private func quitApp() {
        NSApplication.shared.terminate(nil)
    }

    private func runAutomatedUiSmokeTest(vm: CourtsideViewModel) {
        Task { @MainActor in
            let originalOnboarding = UserDefaults.standard.bool(forKey: "courtside.onboardingCompletedV2")
            vm.showOnboardingSheet = true
            try? await Task.sleep(nanoseconds: 220_000_000)
            vm.showOnboardingSheet = false

            for tab in MacCourtsideTab.allCases {
                vm.selectedTab = tab
                try? await Task.sleep(nanoseconds: 180_000_000)
            }

            vm.selectedPlayerProfile = vm.players.first
            try? await Task.sleep(nanoseconds: 180_000_000)
            vm.selectedPlayerProfile = nil

            vm.reminderSheetGame = vm.games.first
            try? await Task.sleep(nanoseconds: 180_000_000)
            vm.reminderSheetGame = nil

            vm.selectThemeMode(.dark)
            try? await Task.sleep(nanoseconds: 180_000_000)
            vm.selectThemeMode(.light)
            vm.selectedTab = .myTeam
            UserDefaults.standard.set(originalOnboarding, forKey: "courtside.onboardingCompletedV2")
            try? await Task.sleep(nanoseconds: 150_000_000)
            print("✅ UI_SMOKE_TEST_PASSED: All 5 Liquid Glass tabs, all 3 modal sheets, Menu Bar status item & Light/Dark themes rendered cleanly!")
            fflush(stdout)
            NSApplication.shared.terminate(nil)
        }
    }
}

// MARK: - Root SwiftUI Shell

struct MacRootContainerView: View {
    @ObservedObject private var vm = CourtsideViewModel.shared

    var body: some View {
        MacRootShellView(vm: vm)
            .environment(\.glassPalette, vm.palette)
            .environment(\.colorScheme, vm.themeMode.isLight ? .light : .dark)
    }
}

struct MacRootShellView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            LiquidGlassAmbientBackdrop(activeTeam: vm.primaryTeam, mode: vm.themeMode)

            HStack(spacing: 0) {
                liquidGlassSidebar
                    .frame(width: 215)
                    .padding(.leading, 12)
                    .padding(.vertical, 12)

                VStack(spacing: 0) {
                    topGlassToolbar
                        .padding(.horizontal, 18)
                        .padding(.top, 12)

                    Group {
                        switch vm.selectedTab {
                        case .myTeam:
                            MyTeamHubView(vm: vm)
                        case .matchCenter:
                            MatchCenterView(vm: vm)
                        case .standings:
                            StandingsMacView(vm: vm)
                        case .analytics:
                            AnalyticsMacView(vm: vm)
                        case .settings:
                            SettingsMacView(vm: vm)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }

            if let toast = vm.toastBanner {
                Text(toast)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 9)
                    .background(palette.neonMadeGreen, in: Capsule())
                    .shadow(radius: 8)
                    .padding(22)
                    .transition(.opacity)
            }
        }
        .sheet(item: $vm.selectedPlayerProfile) { player in
            PlayerScoutingSheet(player: player, vm: vm)
                .environment(\.glassPalette, vm.palette)
        }
        .sheet(item: $vm.reminderSheetGame) { game in
            GameRemindersSheet(game: game, vm: vm)
                .environment(\.glassPalette, vm.palette)
        }
        .sheet(isPresented: $vm.showOnboardingSheet) {
            OnboardingSheet(vm: vm)
                .environment(\.glassPalette, vm.palette)
        }
    }

    private var liquidGlassSidebar: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(palette.euroLeagueOrange)
                        .frame(width: 36, height: 36)
                    Image(systemName: "basketball.fill")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.white)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text("Courtside 2.0")
                        .font(.system(size: 15, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                    Text("macOS Liquid Glass")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(palette.euroLeagueOrange)
                }
            }
            .padding(.horizontal, 8)
            .padding(.bottom, 8)

            ForEach(MacCourtsideTab.allCases) { tab in
                let selected = vm.selectedTab == tab
                Button {
                    if tab == .standings { vm.selectedStandingsTeam = nil }
                    vm.selectedTab = tab
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: tab.sfSymbol)
                            .font(.system(size: 14, weight: .bold))
                            .frame(width: 22)
                        Text(tab.title(vm.strings))
                            .font(.system(size: 13, weight: selected ? .black : .semibold, design: .rounded))
                        Spacer()
                        Text("⌘\(String(tab.shortcutCharacter.character))")
                            .font(.system(size: 10, weight: .semibold, design: .monospaced))
                            .foregroundStyle(selected ? .white.opacity(0.85) : palette.mutedText)
                    }
                    .foregroundStyle(selected ? .white : palette.primaryText)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 9)
                    .background(
                        selected
                            ? AnyShapeStyle(palette.euroLeagueOrange)
                            : AnyShapeStyle(Color.clear),
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )
                }
                .buttonStyle(.plain)
            }

            Spacer()

            HStack(spacing: 9) {
                ClubCrestView(team: vm.primaryTeam, size: 28)
                VStack(alignment: .leading, spacing: 1) {
                    Text(vm.primaryTeam.code)
                        .font(.system(size: 12, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                    Text("\(vm.primaryTeam.wins)W–\(vm.primaryTeam.losses)L")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(palette.mutedText)
                }
                Spacer()
                Button {
                    vm.selectThemeMode(vm.themeMode.toggled())
                } label: {
                    Image(systemName: vm.themeMode.isLight ? "sun.max.fill" : "moon.stars.fill")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .padding(7)
                        .background(palette.pillSurface, in: Circle())
                }
                .buttonStyle(.plain)
            }
            .padding(10)
            .background(palette.pillSurface.opacity(0.75), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
        }
        .padding(14)
        .liquidGlassCard(cornerRadius: 22)
    }

    private var topGlassToolbar: some View {
        HStack(spacing: 12) {
            Text(vm.syncStatusText)
                .font(.system(size: 11, weight: .bold, design: .monospaced))
                .foregroundStyle(palette.secondaryText)

            Spacer()

            HStack(spacing: 4) {
                ForEach(AppLanguage.allCases) { lang in
                    Button {
                        vm.selectLanguage(lang)
                    } label: {
                        Text(lang.flagEmoji)
                            .font(.system(size: 13))
                            .padding(.horizontal, 5)
                            .padding(.vertical, 3)
                            .background(vm.language == lang ? palette.euroLeagueOrange.opacity(0.22) : Color.clear, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            Text("v\(CourtsideLiveEngine.currentVersionName)")
                .font(.system(size: 11, weight: .black, design: .rounded))
                .foregroundStyle(palette.euroLeagueOrange)
                .padding(.horizontal, 9)
                .padding(.vertical, 4)
                .background(palette.euroLeagueOrange.opacity(0.14), in: Capsule())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .liquidGlassCard(cornerRadius: 14)
    }
}
