import SwiftUI
import CourtsideCore

// MARK: - TAB 3 (⌘3): OFFICIAL EUROLEAGUE STANDINGS & CLUB DETAIL VIEW

struct StandingsMacView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    var body: some View {
        if let detailTeam = vm.selectedStandingsTeam {
            standingsTeamDetailView(detailTeam)
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        Text(vm.strings.standingsHeader)
                            .font(.system(size: 20, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                        Spacer()
                        Text("Top 6 Quarterfinals • 7–10 Play-In • Regulation PF/PA (OT Excluded)")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(palette.mutedText)
                    }

                    VStack(spacing: 6) {
                        HStack {
                            Text("#").frame(width: 28, alignment: .leading)
                            Text("CLUB").frame(maxWidth: .infinity, alignment: .leading)
                            Text("GP").frame(width: 38)
                            Text("W").frame(width: 36)
                            Text("L").frame(width: 36)
                            Text("PF").frame(width: 52)
                            Text("PA").frame(width: 52)
                            Text("DIFF").frame(width: 54)
                            Text("ORTG").frame(width: 52)
                            Text("FORM").frame(width: 108)
                        }
                        .font(.system(size: 10, weight: .black))
                        .foregroundStyle(palette.mutedText)
                        .padding(.horizontal, 14)

                        ForEach(Array(vm.standings.enumerated()), id: \.element.id) { (idx, club) in
                            let rank = idx + 1
                            let isMyClub = CourtsideAnalytics.canonicalClubId(club.id) == CourtsideAnalytics.canonicalClubId(vm.primaryTeam.id)
                            let zoneColor: Color = rank <= 6 ? palette.neonMadeGreen : (rank <= 10 ? palette.euroLeagueOrange : palette.mutedText.opacity(0.35))

                            HStack {
                                Text("\(rank)")
                                    .font(.system(size: 12, weight: .black, design: .rounded))
                                    .foregroundStyle(zoneColor)
                                    .frame(width: 28, alignment: .leading)

                                HStack(spacing: 10) {
                                    ClubCrestView(team: club, size: 26)
                                    Text(club.name)
                                        .font(.system(size: 13, weight: isMyClub ? .black : .bold))
                                        .foregroundStyle(palette.primaryText)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)

                                Text("\(club.gamesPlayed)").frame(width: 38)
                                Text("\(club.wins)").fontWeight(.black).foregroundStyle(palette.neonMadeGreen).frame(width: 36)
                                Text("\(club.losses)").foregroundStyle(palette.neonMissedRed).frame(width: 36)
                                Text("\(club.pointsFor)").frame(width: 52)
                                Text("\(club.pointsAgainst)").frame(width: 52)
                                Text(club.pointDiff >= 0 ? "+\(club.pointDiff)" : "\(club.pointDiff)")
                                    .fontWeight(.bold)
                                    .foregroundStyle(club.pointDiff >= 0 ? palette.neonMadeGreen : palette.neonMissedRed)
                                    .frame(width: 54)
                                Text(String(format: "%.1f", club.offensiveRating)).frame(width: 52)

                                HStack(spacing: 4) {
                                    ForEach(Array(club.streak.enumerated()), id: \.offset) { _, s in
                                        Text(s)
                                            .font(.system(size: 9, weight: .black))
                                            .foregroundStyle(.white)
                                            .frame(width: 17, height: 17)
                                            .background(s == "W" ? palette.neonMadeGreen : palette.neonMissedRed, in: Circle())
                                    }
                                }
                                .frame(width: 108)
                            }
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundStyle(palette.secondaryText)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .liquidGlassCard(
                                cornerRadius: 13,
                                isSelected: isMyClub,
                                accentGlow: Color(hex: club.primaryColorHex),
                                interactive: true
                            )
                            .onTapGesture {
                                vm.selectedStandingsTeam = club
                            }
                        }
                    }
                }
                .padding(20)
            }
        }
    }

    private func standingsTeamDetailView(_ team: EuroLeagueTeam) -> some View {
        let (last3, next3) = CourtsideAnalytics.resolveTeamLastAndUpcomingGames(team: team, games: vm.games)
        let clubPlayers = vm.players
            .filter { CourtsideAnalytics.canonicalClubId($0.teamId) == CourtsideAnalytics.canonicalClubId(team.id) }
            .sorted { $0.pir > $1.pir }

        return ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Button {
                        vm.selectedStandingsTeam = nil
                    } label: {
                        Text(vm.strings.backToStandingsButton)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(palette.primaryText)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 7)
                            .liquidGlassCard(cornerRadius: 10, interactive: true)
                    }
                    .buttonStyle(.plain)

                    Spacer()

                    Button {
                        vm.selectPrimaryTeam(team.id)
                        vm.showToast("★ \(team.name) → \(vm.strings.navMyTeam)")
                    } label: {
                        Text(vm.strings.setAsMyTeamButton)
                            .font(.system(size: 12, weight: .black))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 7)
                            .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }

                HStack(spacing: 14) {
                    ClubCrestView(team: team, size: 54)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(team.name)
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                        Text("\(team.wins)W–\(team.losses)L • \(team.arena), \(team.city)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(palette.secondaryText)
                    }
                    Spacer()
                }
                .padding(16)
                .liquidGlassCard(cornerRadius: 18, isSelected: true, accentGlow: Color(hex: team.primaryColorHex))

                HStack(alignment: .top, spacing: 16) {
                    miniScheduleColumn(title: vm.strings.last3GamesHeader, games: last3)
                    miniScheduleColumn(title: vm.strings.next3GamesHeader, games: next3)
                }

                Text("\(vm.strings.activeRosterHeader) (\(clubPlayers.count))")
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)

                LazyVGrid(columns: [GridItem(.adaptive(minimum: 250), spacing: 10)], spacing: 10) {
                    ForEach(clubPlayers) { p in
                        HStack(spacing: 10) {
                            PlayerHeadshotAvatar(playerName: p.name, headshotUrl: p.headshotUrl, clubColorHex: team.primaryColorHex, size: 42)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("#\(p.number) \(p.name)")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundStyle(palette.primaryText)
                                    .lineLimit(1)
                                Text(String(format: "%.1f PTS • %.1f REB • %.1f AST • %.1f PIR", p.ppg, p.rpg, p.apg, p.pir))
                                    .font(.system(size: 10, weight: .semibold))
                                    .foregroundStyle(palette.secondaryText)
                            }
                            Spacer()
                        }
                        .padding(10)
                        .liquidGlassCard(cornerRadius: 12, interactive: true)
                        .onTapGesture {
                            vm.selectedPlayerProfile = p
                        }
                    }
                }
            }
            .padding(20)
        }
    }

    private func miniScheduleColumn(title: String, games: [EuroLeagueGame]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 14, weight: .black, design: .rounded))
                .foregroundStyle(palette.primaryText)

            ForEach(games) { g in
                HStack {
                    Text("R\(g.round)")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .frame(width: 34, alignment: .leading)
                    ClubCrestView(team: g.homeTeam, size: 20)
                    Text("\(g.homeTeam.code) vs \(g.awayTeam.code)")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                    ClubCrestView(team: g.awayTeam, size: 20)
                    Spacer()
                    Text(g.status == .final_ ? "\(g.homeScore):\(g.awayScore)" : vm.strings.statusUpcoming)
                        .font(.system(size: 12, weight: .black, design: .monospaced))
                        .foregroundStyle(palette.secondaryText)
                }
                .padding(10)
                .liquidGlassCard(cornerRadius: 12, interactive: true)
                .onTapGesture {
                    vm.selectedStandingsTeam = nil
                    vm.openGameInMatchCenter(gameId: g.id, round: g.round)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - TAB 4 (⌘4): SEASON LEADERS, HEAD-TO-HEAD TEAM COMPARISON & PLAYOFF SERIES

struct AnalyticsMacView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    private var topPlayers: [PlayerProfile] {
        let sorted: [PlayerProfile]
        switch vm.leaderMetric {
        case 1: sorted = vm.players.sorted { $0.ppg > $1.ppg }
        case 2: sorted = vm.players.sorted { $0.rpg > $1.rpg }
        case 3: sorted = vm.players.sorted { $0.apg > $1.apg }
        default: sorted = vm.players.sorted { $0.pir > $1.pir }
        }
        return Array(sorted.prefix(12))
    }

    private var teamA: EuroLeagueTeam {
        vm.standings.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(vm.comparisonTeamAId) }
            ?? vm.primaryTeam
    }

    private var teamB: EuroLeagueTeam {
        vm.standings.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(vm.comparisonTeamBId) }
            ?? vm.standings.dropFirst().first ?? vm.primaryTeam
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // 1. Season Leaders
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(vm.strings.seasonLeadersHeader)
                            .font(.system(size: 19, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                        Spacer()
                        ForEach([(0, "PIR"), (1, "PPG"), (2, "RPG"), (3, "APG")], id: \.0) { (idx, label) in
                            Button {
                                vm.leaderMetric = idx
                            } label: {
                                Text(label)
                                    .font(.system(size: 11, weight: vm.leaderMetric == idx ? .black : .semibold))
                                    .foregroundStyle(vm.leaderMetric == idx ? .white : palette.primaryText)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 5)
                                    .background(vm.leaderMetric == idx ? palette.euroLeagueOrange : palette.pillSurface, in: Capsule())
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 250), spacing: 10)], spacing: 10) {
                        ForEach(Array(topPlayers.enumerated()), id: \.element.id) { (idx, p) in
                            let club = vm.standings.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(p.teamId) }
                            let valStr: String = {
                                switch vm.leaderMetric {
                                case 1: return String(format: "%.1f PTS", p.ppg)
                                case 2: return String(format: "%.1f REB", p.rpg)
                                case 3: return String(format: "%.1f AST", p.apg)
                                default: return String(format: "%.1f PIR", p.pir)
                                }
                            }()
                            HStack(spacing: 10) {
                                Text("#\(idx + 1)")
                                    .font(.system(size: 12, weight: .black, design: .rounded))
                                    .foregroundStyle(palette.euroLeagueOrange)
                                    .frame(width: 24)
                                PlayerHeadshotAvatar(
                                    playerName: p.name,
                                    headshotUrl: p.headshotUrl,
                                    clubColorHex: club?.primaryColorHex ?? 0xFFFF6600,
                                    size: 40
                                )
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(p.name)
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(palette.primaryText)
                                        .lineLimit(1)
                                    Text("\(club?.code ?? p.teamId) • \(p.position)")
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundStyle(palette.mutedText)
                                }
                                Spacer()
                                Text(valStr)
                                    .font(.system(size: 13, weight: .black, design: .rounded))
                                    .foregroundStyle(palette.neonMadeGreen)
                            }
                            .padding(10)
                            .liquidGlassCard(cornerRadius: 13, interactive: true)
                            .onTapGesture {
                                vm.selectedPlayerProfile = p
                            }
                        }
                    }
                }

                // 2. Head-to-Head Team Comparison & Playoff Projection Split
                HStack(alignment: .top, spacing: 16) {
                    teamComparisonStudio
                        .frame(maxWidth: .infinity)
                    playoffSeriesCard
                        .frame(width: 360)
                }
            }
            .padding(20)
        }
    }

    private var teamComparisonStudio: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(vm.strings.teamComparisonHeader)
                    .font(.system(size: 16, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)
                Spacer()
                Picker("", selection: $vm.comparisonTeamAId) {
                    ForEach(vm.standings) { t in Text(t.code).tag(t.id) }
                }
                .labelsHidden()
                .frame(width: 85)
                Text("vs").font(.system(size: 11, weight: .bold)).foregroundStyle(palette.mutedText)
                Picker("", selection: $vm.comparisonTeamBId) {
                    ForEach(vm.standings) { t in Text(t.code).tag(t.id) }
                }
                .labelsHidden()
                .frame(width: 85)
            }

            HStack {
                HStack(spacing: 8) {
                    ClubCrestView(team: teamA, size: 30)
                    Text(teamA.name).font(.system(size: 13, weight: .black)).foregroundStyle(palette.primaryText)
                }
                Spacer()
                HStack(spacing: 8) {
                    Text(teamB.name).font(.system(size: 13, weight: .black)).foregroundStyle(palette.primaryText)
                    ClubCrestView(team: teamB, size: 30)
                }
            }

            comparisonBar(label: "Points Per Game (PPG)", a: teamA.ppg, b: teamB.ppg)
            comparisonBar(label: "Offensive Rating (ORtg)", a: teamA.offensiveRating, b: teamB.offensiveRating)
            comparisonBar(label: "Defensive Rating (Lower = Better)", a: teamA.defensiveRating, b: teamB.defensiveRating, lowerBetter: true)
            comparisonBar(label: "3-Point %", a: teamA.threePointPct, b: teamB.threePointPct)
            comparisonBar(label: "2-Point %", a: teamA.twoPointPct, b: teamB.twoPointPct)
            comparisonBar(label: "Rebound Rate %", a: teamA.reboundRate, b: teamB.reboundRate)
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 18)
    }

    private func comparisonBar(label: String, a: Double, b: Double, lowerBetter: Bool = false) -> some View {
        let aWins = lowerBetter ? (a <= b) : (a >= b)
        let sum = max(1.0, a + b)
        let ratioA = CGFloat(a / sum)

        return VStack(spacing: 4) {
            HStack {
                Text(String(format: "%.1f", a))
                    .font(.system(size: 12, weight: .black, design: .monospaced))
                    .foregroundStyle(aWins ? palette.neonMadeGreen : palette.secondaryText)
                Spacer()
                Text(label)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(palette.mutedText)
                Spacer()
                Text(String(format: "%.1f", b))
                    .font(.system(size: 12, weight: .black, design: .monospaced))
                    .foregroundStyle(!aWins ? palette.neonMadeGreen : palette.secondaryText)
            }
            GeometryReader { g in
                HStack(spacing: 3) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(hex: teamA.primaryColorHex))
                        .frame(width: max(12, g.size.width * ratioA - 2))
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(hex: teamB.primaryColorHex))
                }
            }
            .frame(height: 7)
        }
    }

    private var playoffSeriesCard: some View {
        let series = CourtsideAnalytics.buildPlayoffBracket(standings: vm.standings, games: vm.games)
        return VStack(alignment: .leading, spacing: 10) {
            Text(vm.strings.playoffProjectionHeader)
                .font(.system(size: 15, weight: .black, design: .rounded))
                .foregroundStyle(palette.primaryText)

            ForEach(series) { s in
                HStack {
                    Text(s.seriesLabel)
                        .font(.system(size: 10, weight: .black))
                        .foregroundStyle(palette.euroLeagueOrange)
                        .frame(width: 36, alignment: .leading)
                    ClubCrestView(team: s.homeTeam, size: 20)
                    Text("#\(s.highSeedRank) \(s.homeTeam.code)")
                        .font(.system(size: 11, weight: .bold))
                    Spacer()
                    Text("\(s.homeWins) – \(s.awayWins)")
                        .font(.system(size: 11, weight: .black, design: .monospaced))
                        .foregroundStyle(palette.secondaryText)
                    Spacer()
                    Text("#\(s.lowSeedRank) \(s.awayTeam.code)")
                        .font(.system(size: 11, weight: .bold))
                    ClubCrestView(team: s.awayTeam, size: 20)
                }
                .foregroundStyle(palette.primaryText)
                .padding(10)
                .background(palette.pillSurface.opacity(0.65), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
            }
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 18)
    }
}
