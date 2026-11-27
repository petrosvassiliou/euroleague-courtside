import SwiftUI
import AppKit
import CourtsideCore

// MARK: - TAB 5 (⌘5): SETTINGS, 6 LANGUAGES, LIQUID GLASS APPEARANCE, MENU BAR TRACKER & UPDATER

struct SettingsMacView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        ScrollView {
            HStack(alignment: .top, spacing: 18) {
                // Left Column: Language, Appearance & Onboarding
                VStack(alignment: .leading, spacing: 14) {
                    // 1. 6-Language Switcher
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text(vm.strings.languageSectionTitle)
                                .font(.system(size: 15, weight: .black, design: .rounded))
                                .foregroundStyle(palette.primaryText)
                            Spacer()
                            Text("v\(CourtsideLiveEngine.currentVersionName) (Build \(CourtsideLiveEngine.currentBuildNumber))")
                                .font(.system(size: 11, weight: .black, design: .rounded))
                                .foregroundStyle(palette.euroLeagueOrange)
                                .padding(.horizontal, 9)
                                .padding(.vertical, 4)
                                .background(palette.euroLeagueOrange.opacity(0.15), in: Capsule())
                        }

                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 130), spacing: 8)], spacing: 8) {
                            ForEach(AppLanguage.allCases) { lang in
                                let active = vm.language == lang
                                Button {
                                    vm.selectLanguage(lang)
                                } label: {
                                    HStack(spacing: 6) {
                                        Text(lang.flagEmoji)
                                        Text(lang.nativeName)
                                            .font(.system(size: 12, weight: active ? .black : .semibold))
                                    }
                                    .foregroundStyle(active ? palette.euroLeagueOrange : palette.primaryText)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 8)
                                    .background(active ? palette.euroLeagueOrange.opacity(0.16) : palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(16)
                    .liquidGlassCard(cornerRadius: 18, isSelected: true, accentGlow: palette.euroLeagueOrange)

                    // 2. Daylight & Obsidian Liquid Glass Theme Switcher
                    VStack(alignment: .leading, spacing: 10) {
                        Text(vm.strings.themeSectionTitle)
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)

                        HStack(spacing: 10) {
                            ForEach([(AppThemeMode.light, vm.strings.lightModeLabel), (AppThemeMode.dark, vm.strings.darkModeLabel)], id: \.0.id) { (mode, label) in
                                let selected = vm.themeMode == mode
                                Button {
                                    vm.selectThemeMode(mode)
                                } label: {
                                    Text(label)
                                        .font(.system(size: 12, weight: selected ? .black : .semibold))
                                        .foregroundStyle(selected ? palette.euroLeagueOrange : palette.primaryText)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, 10)
                                        .background(selected ? palette.euroLeagueOrange.opacity(0.16) : palette.pillSurface, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(16)
                    .liquidGlassCard(cornerRadius: 18)

                    // 3. GitHub Releases macOS Updater
                    VStack(alignment: .leading, spacing: 10) {
                        Text(vm.strings.updateSectionTitle)
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)

                        Toggle(isOn: Binding(
                            get: { vm.betaUpdatesEnabled },
                            set: { vm.setBetaUpdates($0) }
                        )) {
                            Text(vm.strings.betaUpdateChannelTitle)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(palette.primaryText)
                        }
                        .toggleStyle(.switch)

                        let statusLine: String = {
                            switch vm.updateResult.status {
                            case .checking: return "Checking GitHub Releases…"
                            case .updateAvailable: return "🚀 Update Available: v\(vm.updateResult.latestVersionName)"
                            case .error: return "⚠️ \(vm.updateResult.errorMessage ?? "Unreachable")"
                            default: return "\(vm.strings.upToDateLabel) (v\(CourtsideLiveEngine.currentVersionName))"
                            }
                        }()

                        Text(statusLine)
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundStyle(vm.updateResult.status == .error ? palette.neonMissedRed : palette.neonMadeGreen)

                        HStack(spacing: 8) {
                            Button {
                                Task { await vm.checkForUpdates(manual: true) }
                            } label: {
                                Text(vm.strings.checkForUpdatesButton)
                                    .font(.system(size: 12, weight: .black))
                                    .foregroundStyle(.white)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }
                            .buttonStyle(.plain)

                            if vm.updateResult.status == .updateAvailable,
                               let url = URL(string: vm.updateResult.macDownloadUrl.isEmpty ? vm.updateResult.htmlUrl : vm.updateResult.macDownloadUrl) {
                                Button {
                                    NSWorkspace.shared.open(url)
                                } label: {
                                    Text("⬇ Download v\(vm.updateResult.latestVersionName)")
                                        .font(.system(size: 12, weight: .black))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(palette.neonMadeGreen, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(16)
                    .liquidGlassCard(cornerRadius: 18)
                }
                .frame(maxWidth: .infinity)

                // Right Column: Menu Bar Live Scoreboard & macOS Notifications
                VStack(alignment: .leading, spacing: 14) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(vm.strings.notificationChannelsTitle)
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)

                        settingToggleRow(
                            badge: "MENU BAR",
                            title: vm.strings.menuBarTrackerTitle,
                            subtitle: vm.strings.menuBarTrackerSubtitle,
                            isOn: Binding(get: { vm.menuBarTrackerEnabled }, set: { vm.setMenuBarTracker($0) })
                        )

                        settingToggleRow(
                            badge: "TIP-OFF",
                            title: "\(vm.strings.tipOffChannelTitle) (\(vm.primaryTeam.code))",
                            subtitle: "15-minute tip-off reminder via macOS Notification Center",
                            isOn: Binding(
                                get: { vm.tipOffEnabled },
                                set: { vm.updateNotificationChannels(tipOff: $0, htFinal: vm.halftimeFinalEnabled, closeGame: vm.closeGameAlertEnabled) }
                            )
                        )

                        settingToggleRow(
                            badge: "SCORES",
                            title: vm.strings.scoresChannelTitle,
                            subtitle: "Halftime & Final score notifications for favorite clubs and armed games",
                            isOn: Binding(
                                get: { vm.halftimeFinalEnabled },
                                set: { vm.updateNotificationChannels(tipOff: vm.tipOffEnabled, htFinal: $0, closeGame: vm.closeGameAlertEnabled) }
                            )
                        )

                        settingToggleRow(
                            badge: "CRUNCH",
                            title: vm.strings.closeGameChannelTitle,
                            subtitle: "Instant alert when a 4th-quarter game is within 5 points under 3:00",
                            isOn: Binding(
                                get: { vm.closeGameAlertEnabled },
                                set: { vm.updateNotificationChannels(tipOff: vm.tipOffEnabled, htFinal: vm.halftimeFinalEnabled, closeGame: $0) }
                            )
                        )

                        HStack(spacing: 8) {
                            Button {
                                vm.sendInstantTestNotification()
                            } label: {
                                Text(vm.strings.sendTestPushButton)
                                    .font(.system(size: 12, weight: .black))
                                    .foregroundStyle(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 10)
                                    .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                            }
                            .buttonStyle(.plain)

                            Button {
                                vm.showOnboardingSheet = true
                            } label: {
                                Text(vm.strings.replayOnboardingButton)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundStyle(palette.primaryText)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 10)
                                    .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
                    .liquidGlassCard(cornerRadius: 18)
                }
                .frame(maxWidth: .infinity)
            }
            .padding(20)
        }
    }

    private func settingToggleRow(badge: String, title: String, subtitle: String, isOn: Binding<Bool>) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(badge)
                        .font(.system(size: 9, weight: .black))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(palette.euroLeagueOrange.opacity(0.15), in: Capsule())
                    Text(title)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                }
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(palette.mutedText)
            }
            Spacer()
            Toggle("", isOn: isOn)
                .labelsHidden()
                .toggleStyle(.switch)
        }
        .padding(10)
        .background(palette.pillSurface.opacity(0.65), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
    }
}

// MARK: - GLOBAL MODAL 1: PLAYER SCOUTING CARD & GAME LOG SHEET

struct PlayerScoutingSheet: View {
    let player: PlayerProfile
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        let club = vm.standings.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(player.teamId) }
        let summary = CourtsideAnalytics.buildPlayerScoutingSummary(player: player, games: vm.games)

        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                PlayerHeadshotAvatar(
                    playerName: player.name,
                    headshotUrl: player.headshotUrl,
                    clubColorHex: club?.primaryColorHex ?? 0xFFFF6600,
                    size: 72
                )
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        if let club {
                            ClubCrestView(team: club, size: 20)
                            Text(club.name)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(palette.secondaryText)
                        }
                        Text("• #\(player.number) • \(player.position)")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(palette.euroLeagueOrange)
                    }
                    Text(player.name)
                        .font(.system(size: 22, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                    Text("\(player.heightCm) cm • \(player.nationality) • \(vm.strings.localizedInjuryLabel(player.injuryStatus))")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(palette.neonMadeGreen)
                }
                Spacer()
                Button {
                    vm.selectedPlayerProfile = nil
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 22))
                        .foregroundStyle(palette.mutedText)
                }
                .buttonStyle(.plain)
            }

            // Season Averages + Shooting Splits
            HStack(spacing: 10) {
                scoutingMetricBox(label: "PPG", value: String(format: "%.1f", player.ppg))
                scoutingMetricBox(label: "RPG", value: String(format: "%.1f", player.rpg))
                scoutingMetricBox(label: "APG", value: String(format: "%.1f", player.apg))
                scoutingMetricBox(label: "PIR", value: String(format: "%.1f", player.pir))
                scoutingMetricBox(label: "2P%", value: String(format: "%.1f%%", summary.twoPtPct))
                scoutingMetricBox(label: "3P%", value: String(format: "%.1f%%", summary.threePtPct))
                scoutingMetricBox(label: "FT%", value: String(format: "%.1f%%", summary.ftPct))
            }

            Text("\(vm.strings.gameLogHeader) (\(summary.gamesLogged))")
                .font(.system(size: 14, weight: .black, design: .rounded))
                .foregroundStyle(palette.primaryText)

            ScrollView {
                VStack(spacing: 5) {
                    ForEach(summary.gameLog) { row in
                        HStack {
                            Text("R\(row.round)")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundStyle(palette.euroLeagueOrange)
                                .frame(width: 36, alignment: .leading)
                            Text("\(row.isHome ? "vs" : "@") \(row.opponentCode)")
                                .font(.system(size: 12, weight: .bold))
                                .frame(width: 80, alignment: .leading)
                            Text("\(row.teamWon ? "W" : "L") \(row.scoreText)")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundStyle(row.teamWon ? palette.neonMadeGreen : palette.neonMissedRed)
                                .frame(width: 82, alignment: .leading)
                            Text(row.minutes).frame(width: 48)
                            Text("\(row.points) PTS").fontWeight(.black).frame(width: 58)
                            Text("\(row.rebounds) REB").frame(width: 56)
                            Text("\(row.assists) AST").frame(width: 56)
                            Spacer()
                            Text("PIR \(row.pir)")
                                .font(.system(size: 11, weight: .black))
                                .foregroundStyle(palette.euroLeagueOrange)
                        }
                        .font(.system(size: 11))
                        .foregroundStyle(palette.primaryText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .onTapGesture {
                            vm.openGameInMatchCenter(gameId: row.gameId, round: row.round)
                        }
                    }
                }
            }
            .frame(maxHeight: 230)
        }
        .padding(22)
        .frame(width: 620)
        .background(palette.mode.isLight ? Color(red: 0.96, green: 0.97, blue: 0.99) : Color(red: 0.08, green: 0.09, blue: 0.13))
    }

    private func scoutingMetricBox(label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(label).font(.system(size: 9, weight: .bold)).foregroundStyle(palette.mutedText)
            Text(value).font(.system(size: 13, weight: .black, design: .rounded)).foregroundStyle(palette.primaryText)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

// MARK: - GLOBAL MODAL 2: PER-GAME REMINDERS SHEET

struct GameRemindersSheet: View {
    let game: EuroLeagueGame
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        let active = vm.perGameReminders[game.id] ?? []
        let allOn = active.count == GameReminderType.allCases.count

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                ClubCrestView(team: game.homeTeam, size: 28)
                Text("\(game.homeTeam.code) vs \(game.awayTeam.code) • \(vm.strings.roundPrefix) \(game.round)")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)
                ClubCrestView(team: game.awayTeam, size: 28)
                Spacer()
                Button {
                    vm.reminderSheetGame = nil
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundStyle(palette.mutedText)
                }
                .buttonStyle(.plain)
            }

            Text(vm.strings.gameRemindersSubtitle)
                .font(.system(size: 12))
                .foregroundStyle(palette.secondaryText)

            ForEach(GameReminderType.allCases) { type in
                let isOn = active.contains(type)
                HStack {
                    Text(type.shortBadge)
                        .font(.system(size: 10, weight: .black))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .frame(width: 42, alignment: .leading)
                    Text(vm.strings.localizedReminderTitle(type))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                    Spacer()
                    Toggle("", isOn: Binding(
                        get: { isOn },
                        set: { _ in vm.toggleSingleReminder(for: game, type: type) }
                    ))
                    .labelsHidden()
                    .toggleStyle(.switch)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            }

            HStack(spacing: 10) {
                Button {
                    vm.updateReminders(for: game, reminders: allOn ? [] : Set(GameReminderType.allCases))
                } label: {
                    Text(allOn ? vm.strings.clearRemindersLabel : vm.strings.enableAllRemindersLabel)
                        .font(.system(size: 12, weight: .black))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)

                Button {
                    vm.reminderSheetGame = nil
                } label: {
                    Text("✓ OK")
                        .font(.system(size: 12, weight: .black))
                        .foregroundStyle(.white)
                        .frame(width: 110)
                        .padding(.vertical, 9)
                        .background(palette.neonMadeGreen, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(20)
        .frame(width: 440)
        .background(palette.mode.isLight ? Color(red: 0.96, green: 0.97, blue: 0.99) : Color(red: 0.08, green: 0.09, blue: 0.13))
    }
}

// MARK: - GLOBAL MODAL 3: FIRST-RUN WELCOME & CLUB SETUP ONBOARDING

struct OnboardingSheet: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Image(systemName: "basketball.fill")
                    .font(.system(size: 26))
                    .foregroundStyle(palette.euroLeagueOrange)
                VStack(alignment: .leading, spacing: 2) {
                    Text(vm.strings.onboardingWelcomeTitle)
                        .font(.system(size: 19, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                    Text(vm.strings.onboardingWelcomeSubtitle)
                        .font(.system(size: 12))
                        .foregroundStyle(palette.secondaryText)
                }
                Spacer()
                ClubCrestView(team: vm.primaryTeam, size: 38)
            }

            HStack(spacing: 6) {
                ForEach(AppLanguage.allCases) { lang in
                    Button {
                        vm.selectLanguage(lang)
                    } label: {
                        Text("\(lang.flagEmoji) \(lang.rawValue.uppercased())")
                            .font(.system(size: 11, weight: vm.language == lang ? .black : .semibold))
                            .foregroundStyle(vm.language == lang ? palette.euroLeagueOrange : palette.secondaryText)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 5)
                            .background(palette.pillSurface, in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
                Button {
                    vm.selectThemeMode(vm.themeMode.toggled())
                } label: {
                    Text(vm.themeMode.isLight ? "☀️ Light" : "🌙 Dark")
                        .font(.system(size: 11, weight: .black))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(palette.pillSurface, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            Text(vm.strings.onboardingPickClubTitle)
                .font(.system(size: 13, weight: .black))
                .foregroundStyle(palette.primaryText)

            LazyVGrid(columns: [GridItem(.adaptive(minimum: 95), spacing: 8)], spacing: 8) {
                ForEach(vm.standings) { club in
                    let selected = CourtsideAnalytics.canonicalClubId(club.id) == CourtsideAnalytics.canonicalClubId(vm.primaryTeam.id)
                    Button {
                        vm.selectPrimaryTeam(club.id)
                    } label: {
                        HStack(spacing: 6) {
                            ClubCrestView(team: club, size: 20)
                            Text(club.code)
                                .font(.system(size: 11, weight: selected ? .black : .semibold))
                                .foregroundStyle(selected ? palette.euroLeagueOrange : palette.primaryText)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 7)
                        .background(selected ? palette.euroLeagueOrange.opacity(0.18) : palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }

            Toggle(isOn: Binding(get: { vm.menuBarTrackerEnabled }, set: { vm.setMenuBarTracker($0) })) {
                Text(vm.strings.menuBarTrackerTitle)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(palette.primaryText)
            }
            .toggleStyle(.switch)

            Button {
                vm.completeOnboarding()
            } label: {
                Text(vm.strings.onboardingFinishButton)
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
        }
        .padding(22)
        .frame(width: 600)
        .background(palette.mode.isLight ? Color(red: 0.96, green: 0.97, blue: 0.99) : Color(red: 0.08, green: 0.09, blue: 0.13))
    }
}
