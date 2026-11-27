import SwiftUI
import AppKit
import CourtsideCore

@main
struct EuroLeagueCourtsideMacApp: App {
    @StateObject private var vm = CourtsideViewModel()

    init() {
        NSApplication.shared.setActivationPolicy(.regular)
    }

    var body: some Scene {
        WindowGroup("EuroLeague Courtside 2.0") {
            MacRootShellView(vm: vm)
                .environment(\.glassPalette, vm.palette)
                .preferredColorScheme(vm.themeMode.isLight ? .light : .dark)
                .frame(minWidth: 1040, minHeight: 680)
        }
        .commands {
            CommandMenu("Courtside") {
                ForEach(MacCourtsideTab.allCases) { tab in
                    Button(tab.title(vm.strings)) {
                        if tab == .standings { vm.selectedStandingsTeam = nil }
                        vm.selectedTab = tab
                    }
                    .keyboardShortcut(tab.shortcutCharacter, modifiers: [.command])
                }
                Divider()
                Button(vm.strings.liveSyncButton) {
                    Task { await vm.triggerLiveSync(silent: false) }
                }
                .keyboardShortcut("r", modifiers: [.command])

                Button("Toggle Light / Dark Liquid Glass") {
                    vm.selectThemeMode(vm.themeMode.toggled())
                }
                .keyboardShortcut("d", modifiers: [.command, .shift])
            }
        }

        // Native macOS Menu Bar Live Scoreboard Tracker (counterpart to Android's Lock-Screen Live Tracker)
        MenuBarExtra(isInserted: Binding(
            get: { vm.menuBarTrackerEnabled },
            set: { vm.setMenuBarTracker($0) }
        )) {
            MenuBarLiveDropdownView(vm: vm)
        } label: {
            Text(vm.menuBarTitle)
        }
    }
}

struct MacRootShellView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            LiquidGlassAmbientBackdrop(activeTeam: vm.primaryTeam, mode: vm.themeMode)

            HStack(spacing: 0) {
                // Translucent Liquid Glass Sidebar
                liquidGlassSidebar
                    .frame(width: 215)
                    .padding(.leading, 12)
                    .padding(.vertical, 12)

                // Active Content Area
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
            // Brand Header
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

            // Selected Club Mini Card in Sidebar Footer
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

struct MenuBarLiveDropdownView: View {
    @ObservedObject var vm: CourtsideViewModel

    var body: some View {
        let roundGames = vm.games.filter { $0.status == .live || $0.round == vm.activeRound }.prefix(6)

        VStack(alignment: .leading, spacing: 6) {
            Text("🏀 EuroLeague Courtside 2.0 • \(vm.strings.roundPrefix) \(vm.activeRound)")
                .font(.headline)

            Divider()

            ForEach(Array(roundGames)) { g in
                Button {
                    NSApplication.shared.activate(ignoringOtherApps: true)
                    vm.openGameInMatchCenter(gameId: g.id, round: g.round)
                } label: {
                    let badge = g.status == .live ? "🔴 Q\(g.currentQuarter) \(g.remainingClock)" : (g.status == .final_ ? "FT" : "UP")
                    Text("\(badge)  \(g.homeTeam.code) \(g.homeScore):\(g.awayScore) \(g.awayTeam.code)")
                }
            }

            Divider()

            Button(vm.strings.liveSyncButton) {
                Task { await vm.triggerLiveSync(silent: false) }
            }

            Button("Quit EuroLeague Courtside") {
                NSApplication.shared.terminate(nil)
            }
            .keyboardShortcut("q")
        }
        .padding(8)
    }
}
