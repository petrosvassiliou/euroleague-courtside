import SwiftUI
import CourtsideCore

struct MyTeamHubView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    private var clubRoster: [PlayerProfile] {
        let canon = CourtsideAnalytics.canonicalClubId(vm.primaryTeam.id)
        return vm.players
            .filter { CourtsideAnalytics.canonicalClubId($0.teamId) == canon }
            .sorted { $0.pir > $1.pir }
    }

    private var spotlightGame: EuroLeagueGame? {
        let canon = CourtsideAnalytics.canonicalClubId(vm.primaryTeam.id)
        let clubGames = vm.games.filter {
            CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == canon ||
            CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == canon
        }
        return clubGames.first(where: { $0.status == .live })
            ?? clubGames.first(where: { $0.round == vm.activeRound })
            ?? clubGames.first(where: { $0.status == .upcoming })
            ?? clubGames.last
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // 1. 20-Club Liquid Glass Switcher Strip
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(vm.standings) { club in
                            let isPrimary = CourtsideAnalytics.canonicalClubId(club.id) == CourtsideAnalytics.canonicalClubId(vm.primaryTeam.id)
                            let isFav = vm.favoriteTeamIds.contains(club.id)
                            HStack(spacing: 7) {
                                ClubCrestView(team: club, size: 22)
                                Text(club.code)
                                    .font(.system(size: 12, weight: isPrimary ? .black : .semibold, design: .rounded))
                                    .foregroundStyle(isPrimary ? palette.euroLeagueOrange : palette.primaryText)
                                Button {
                                    vm.toggleFavoriteTeam(club.id)
                                } label: {
                                    Image(systemName: isFav ? "star.fill" : "star")
                                        .font(.system(size: 10))
                                        .foregroundStyle(isFav ? Color.yellow : palette.mutedText)
                                }
                                .buttonStyle(.plain)
                            }
                            .padding(.horizontal, 11)
                            .padding(.vertical, 7)
                            .liquidGlassCard(
                                cornerRadius: 12,
                                isSelected: isPrimary,
                                accentGlow: Color(hex: club.primaryColorHex),
                                interactive: true
                            )
                            .onTapGesture {
                                vm.selectPrimaryTeam(club.id)
                            }
                        }
                    }
                    .padding(.vertical, 2)
                }

                // 2. Hero Club Banner + Next Matchup Spotlight Split
                HStack(alignment: .top, spacing: 16) {
                    clubHeroCard
                        .frame(maxWidth: .infinity)

                    if let game = spotlightGame {
                        spotlightMatchupCard(game)
                            .frame(maxWidth: .infinity)
                    }
                }

                // 3. Verified Club Roster & Injury Report Grid
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(vm.strings.activeRosterHeader) • \(vm.primaryTeam.name)")
                                .font(.system(size: 17, weight: .black, design: .rounded))
                                .foregroundStyle(palette.primaryText)
                            Text("\(clubRoster.count) EuroLeague Players • Click any player for Scouting Card & Game Log")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(palette.mutedText)
                        }
                        Spacer()
                        Button {
                            Task { await vm.verifyClubRosterNow() }
                        } label: {
                            Text(vm.strings.verifyRosterButton)
                                .font(.system(size: 12, weight: .bold))
                                .foregroundStyle(palette.neonMadeGreen)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 7)
                                .liquidGlassCard(cornerRadius: 10, isSelected: true, accentGlow: palette.neonMadeGreen, interactive: true)
                        }
                        .buttonStyle(.plain)
                    }

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 270), spacing: 12)], spacing: 12) {
                        ForEach(clubRoster) { player in
                            rosterPlayerCard(player)
                        }
                    }
                }
            }
            .padding(20)
        }
    }

    private var clubHeroCard: some View {
        let team = vm.primaryTeam
        let rank = (vm.standings.firstIndex(where: { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(team.id) }) ?? 0) + 1

        return VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                ClubCrestView(team: team, size: 58)
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        Text("#\(rank)")
                            .font(.system(size: 11, weight: .black, design: .rounded))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(palette.euroLeagueOrange, in: Capsule())
                        Text("\(team.city), \(team.country) • \(team.arena)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(palette.secondaryText)
                            .lineLimit(1)
                    }
                    Text(team.name)
                        .font(.system(size: 22, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)

                    HStack(spacing: 6) {
                        Text("\(team.wins)W – \(team.losses)L")
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                            .foregroundStyle(palette.euroLeagueOrange)
                        Text("•")
                            .foregroundStyle(palette.mutedText)
                        ForEach(Array(team.streak.enumerated()), id: \.offset) { _, s in
                            Text(s)
                                .font(.system(size: 10, weight: .black, design: .rounded))
                                .foregroundStyle(.white)
                                .frame(width: 18, height: 18)
                                .background(s == "W" ? palette.neonMadeGreen : palette.neonMissedRed, in: Circle())
                        }
                    }
                }
                Spacer()
            }

            Divider().opacity(0.35)

            HStack(spacing: 10) {
                statPill(label: "PPG", value: String(format: "%.1f", team.ppg))
                statPill(label: "OPP", value: String(format: "%.1f", team.oppPpg))
                statPill(label: "DIFF", value: team.pointDiff >= 0 ? "+\(team.pointDiff)" : "\(team.pointDiff)")
                statPill(label: "ORTG", value: String(format: "%.1f", team.offensiveRating))
                statPill(label: "DRTG", value: String(format: "%.1f", team.defensiveRating))
                statPill(label: "3P%", value: String(format: "%.1f%%", team.threePointPct))
            }
        }
        .padding(18)
        .liquidGlassCard(cornerRadius: 20, isSelected: true, accentGlow: Color(hex: team.primaryColorHex))
    }

    private func spotlightMatchupCard(_ game: EuroLeagueGame) -> some View {
        let activeReminders = vm.perGameReminders[game.id] ?? []
        let statusText: String = {
            switch game.status {
            case .live: return "🔴 \(vm.strings.statusLive) • Q\(game.currentQuarter) \(game.remainingClock)"
            case .final_: return "🏁 \(vm.strings.statusFinal) • \(vm.strings.roundPrefix) \(game.round)"
            case .upcoming: return "⏰ \(vm.strings.nextTipOffBadge) • \(vm.strings.roundPrefix) \(game.round)"
            }
        }()

        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(statusText)
                    .font(.system(size: 11, weight: .black, design: .rounded))
                    .foregroundStyle(game.status == .live ? palette.neonMissedRed : palette.euroLeagueOrange)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background((game.status == .live ? palette.neonMissedRed : palette.euroLeagueOrange).opacity(0.14), in: Capsule())

                Spacer()

                Button {
                    vm.reminderSheetGame = game
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: activeReminders.isEmpty ? "bell" : "bell.badge.fill")
                        if !activeReminders.isEmpty {
                            Text(activeReminders.count == GameReminderType.allCases.count ? "ALL" : "\(activeReminders.count)")
                        }
                    }
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(activeReminders.isEmpty ? palette.secondaryText : palette.euroLeagueOrange)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 5)
                    .background(palette.pillSurface, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 16) {
                VStack(spacing: 6) {
                    ClubCrestView(team: game.homeTeam, size: 44)
                    Text(game.homeTeam.code)
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                }
                .frame(maxWidth: .infinity)

                VStack(spacing: 4) {
                    if game.status == .upcoming {
                        Text(Self.formatTipOffDate(game.tipOffEpochMillis))
                            .font(.system(size: 18, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                    } else {
                        Text("\(game.homeScore) : \(game.awayScore)")
                            .font(.system(size: 26, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                    }
                    Text(game.venueName)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(palette.mutedText)
                        .lineLimit(1)
                }

                VStack(spacing: 6) {
                    ClubCrestView(team: game.awayTeam, size: 44)
                    Text(game.awayTeam.code)
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                }
                .frame(maxWidth: .infinity)
            }

            Divider().opacity(0.35)

            HStack(spacing: 8) {
                Button {
                    vm.openGameInMatchCenter(gameId: game.id, round: game.round)
                } label: {
                    Label("Gamecast", systemImage: "sportscourt.fill")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 11)
                        .padding(.vertical, 7)
                        .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)

                Button {
                    vm.exportSingleGameToAppleCalendar(game)
                } label: {
                    Label(vm.strings.exportAppleCalendarButton, systemImage: "calendar.badge.plus")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)

                Button {
                    vm.exportSeasonIcsWithSavePanel()
                } label: {
                    Label(vm.strings.exportSeasonIcsButton, systemImage: "square.and.arrow.down")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(palette.secondaryText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 7)
                        .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(18)
        .liquidGlassCard(cornerRadius: 20, isSelected: game.status == .live, accentGlow: palette.euroLeagueOrange)
    }

    private func rosterPlayerCard(_ player: PlayerProfile) -> some View {
        let injColor: Color = {
            switch player.injuryStatus {
            case .fit: return palette.neonMadeGreen
            case .questionable: return Color.orange
            case .out: return palette.neonMissedRed
            }
        }()

        return HStack(spacing: 12) {
            PlayerHeadshotAvatar(
                playerName: player.name,
                headshotUrl: player.headshotUrl,
                clubColorHex: vm.primaryTeam.primaryColorHex,
                size: 48
            )

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text("#\(player.number)")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .foregroundStyle(palette.euroLeagueOrange)
                    Text(player.name)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                        .lineLimit(1)
                }

                Text("\(player.position) • \(player.heightCm) cm • \(player.nationality)")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(palette.mutedText)

                HStack(spacing: 8) {
                    Text(vm.strings.localizedInjuryLabel(player.injuryStatus))
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(injColor)
                    Spacer()
                    Text(String(format: "%.1f PTS • %.1f PIR", player.ppg, player.pir))
                        .font(.system(size: 10, weight: .heavy, design: .rounded))
                        .foregroundStyle(palette.secondaryText)
                }
            }
        }
        .padding(12)
        .liquidGlassCard(cornerRadius: 14, interactive: true)
        .onTapGesture {
            vm.selectedPlayerProfile = player
        }
    }

    private func statPill(label: String, value: String) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(palette.mutedText)
            Text(value)
                .font(.system(size: 13, weight: .black, design: .rounded))
                .foregroundStyle(palette.primaryText)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 7)
        .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private static func formatTipOffDate(_ epochMs: Int64) -> String {
        guard epochMs > 0 else { return "TBA" }
        let date = Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000.0)
        let f = DateFormatter()
        f.dateFormat = "EEE dd MMM • HH:mm"
        return f.string(from: date)
    }
}
