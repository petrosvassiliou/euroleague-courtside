import SwiftUI
import CourtsideCore

struct MatchCenterView: View {
    @ObservedObject var vm: CourtsideViewModel
    @Environment(\.glassPalette) private var palette

    private var filteredRoundGames: [EuroLeagueGame] {
        let favCanon = Set(vm.favoriteTeamIds.union([vm.primaryTeamId]).map { CourtsideAnalytics.canonicalClubId($0) })
        let q = vm.matchSearchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        return vm.games.filter { g in
            if vm.matchFilterMode == 2 {
                if g.status != .live { return false }
            } else if g.round != vm.selectedRound {
                return false
            }
            if vm.matchFilterMode == 1 {
                let inv = favCanon.contains(CourtsideAnalytics.canonicalClubId(g.homeTeam.id)) ||
                    favCanon.contains(CourtsideAnalytics.canonicalClubId(g.awayTeam.id))
                if !inv { return false }
            }
            if !q.isEmpty {
                let hay = "\(g.homeTeam.name) \(g.homeTeam.code) \(g.awayTeam.name) \(g.awayTeam.code) \(g.venueName)".lowercased()
                if !hay.contains(q) { return false }
            }
            return true
        }
    }

    private var selectedGame: EuroLeagueGame? {
        if let id = vm.selectedGameId, let match = vm.games.first(where: { $0.id == id }) {
            return match
        }
        return filteredRoundGames.first ?? vm.games.first(where: { $0.round == vm.selectedRound })
    }

    var body: some View {
        VStack(spacing: 14) {
            // Top Matchday Controls Strip
            HStack(spacing: 10) {
                Button {
                    vm.selectedRound = max(1, vm.selectedRound - 1)
                    vm.ensureSelectedGameInRound()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 12, weight: .bold))
                        .padding(8)
                        .background(palette.pillSurface, in: Circle())
                }
                .buttonStyle(.plain)

                Picker("", selection: $vm.selectedRound) {
                    ForEach(1...38, id: \.self) { rnd in
                        Text("\(vm.strings.roundPrefix) \(rnd)").tag(rnd)
                    }
                }
                .labelsHidden()
                .frame(width: 135)
                .onChange(of: vm.selectedRound) { _, _ in
                    vm.ensureSelectedGameInRound()
                }

                Button {
                    vm.selectedRound = min(38, vm.selectedRound + 1)
                    vm.ensureSelectedGameInRound()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .bold))
                        .padding(8)
                        .background(palette.pillSurface, in: Circle())
                }
                .buttonStyle(.plain)

                HStack(spacing: 6) {
                    filterModePill(0, title: vm.strings.filterAllGames)
                    filterModePill(1, title: vm.strings.filterFavorites)
                    filterModePill(2, title: vm.strings.filterLiveOnly)
                }

                TextField(vm.strings.searchPlaceholder, text: $vm.matchSearchQuery)
                    .textFieldStyle(.plain)
                    .font(.system(size: 12))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .frame(maxWidth: 220)

                Spacer()

                Button {
                    Task { await vm.triggerLiveSync(silent: false) }
                } label: {
                    HStack(spacing: 6) {
                        if vm.isSyncing {
                            ProgressView().controlSize(.small)
                        }
                        Text(vm.strings.liveSyncButton)
                            .font(.system(size: 12, weight: .black, design: .rounded))
                    }
                    .foregroundStyle(.white)
                    .padding(.horizontal, 13)
                    .padding(.vertical, 7)
                    .background(palette.euroLeagueOrange, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 18)
            .padding(.top, 14)

            // Master-Detail Split View
            HStack(alignment: .top, spacing: 16) {
                // Left: Matchday Scoreboard Column
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(filteredRoundGames) { game in
                            matchScoreCard(game)
                        }
                    }
                    .padding(.vertical, 2)
                }
                .frame(width: 330)

                // Right: Full Gamecast Inspector (Reminders Strip + Court/Momentum/PBP + Boxscores)
                if let game = selectedGame {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 14) {
                            gamecastHeaderCard(game)
                            perGameRemindersQuickStrip(game)
                            courtMomentumPlayByPlayCard(game)
                            boxscoreCard(game)
                        }
                        .padding(.bottom, 18)
                    }
                    .frame(maxWidth: .infinity)
                    .task(id: game.id) {
                        await vm.refreshSelectedGameFullGamecast(game)
                    }
                } else {
                    Spacer()
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 12)
        }
    }

    private func filterModePill(_ mode: Int, title: String) -> some View {
        let active = vm.matchFilterMode == mode
        return Button {
            vm.matchFilterMode = mode
        } label: {
            Text(title)
                .font(.system(size: 11, weight: active ? .black : .semibold))
                .foregroundStyle(active ? palette.euroLeagueOrange : palette.secondaryText)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(active ? palette.euroLeagueOrange.opacity(0.16) : palette.pillSurface, in: Capsule())
        }
        .buttonStyle(.plain)
    }

    private func matchScoreCard(_ game: EuroLeagueGame) -> some View {
        let isSelected = selectedGame?.id == game.id
        let activeReminders = vm.perGameReminders[game.id] ?? []

        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                if game.isCloseGameFourthQuarter {
                    Text(vm.strings.crunchTimeBadge)
                        .font(.system(size: 9, weight: .black))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(palette.neonMissedRed, in: Capsule())
                } else {
                    let badge: String = {
                        switch game.status {
                        case .live: return "🔴 Q\(game.currentQuarter) \(game.remainingClock)"
                        case .final_: return vm.strings.statusFinal
                        case .upcoming: return vm.strings.statusUpcoming
                        }
                    }()
                    Text(badge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(game.status == .live ? palette.neonMissedRed : palette.mutedText)
                }

                Spacer()

                Button {
                    vm.reminderSheetGame = game
                } label: {
                    HStack(spacing: 3) {
                        Image(systemName: activeReminders.isEmpty ? "bell" : "bell.badge.fill")
                        if !activeReminders.isEmpty {
                            Text("\(activeReminders.count)")
                        }
                    }
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(activeReminders.isEmpty ? palette.mutedText : palette.euroLeagueOrange)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(palette.pillSurface, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            HStack {
                ClubCrestView(team: game.homeTeam, size: 24)
                Text(game.homeTeam.name)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(palette.primaryText)
                    .lineLimit(1)
                Spacer()
                Text(game.status == .upcoming ? "–" : "\(game.homeScore)")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)
            }

            HStack {
                ClubCrestView(team: game.awayTeam, size: 24)
                Text(game.awayTeam.name)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(palette.primaryText)
                    .lineLimit(1)
                Spacer()
                Text(game.status == .upcoming ? "–" : "\(game.awayScore)")
                    .font(.system(size: 14, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)
            }
        }
        .padding(12)
        .liquidGlassCard(
            cornerRadius: 15,
            isSelected: isSelected,
            accentGlow: game.status == .live ? palette.neonMissedRed : palette.euroLeagueOrange,
            interactive: true
        )
        .onTapGesture {
            vm.selectedGameId = game.id
            vm.inspectedShot = nil
        }
    }

    private func gamecastHeaderCard(_ game: EuroLeagueGame) -> some View {
        let q = game.quarters
        return VStack(spacing: 12) {
            HStack {
                HStack(spacing: 10) {
                    ClubCrestView(team: game.homeTeam, size: 46)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(game.homeTeam.name)
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                        Text("\(game.homeTeam.wins)W–\(game.homeTeam.losses)L")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(palette.mutedText)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(spacing: 3) {
                    Text(game.status == .upcoming ? "VS" : "\(game.homeScore) : \(game.awayScore)")
                        .font(.system(size: 30, weight: .black, design: .rounded))
                        .foregroundStyle(palette.primaryText)
                    Text(
                        game.status == .live
                            ? "🔴 Q\(game.currentQuarter) • \(game.remainingClock)"
                            : "\(vm.strings.roundPrefix) \(game.round) • \(game.venueName)"
                    )
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(game.status == .live ? palette.neonMissedRed : palette.secondaryText)
                }

                HStack(spacing: 10) {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(game.awayTeam.name)
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundStyle(palette.primaryText)
                        Text("\(game.awayTeam.wins)W–\(game.awayTeam.losses)L")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(palette.mutedText)
                    }
                    ClubCrestView(team: game.awayTeam, size: 46)
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
            }

            if game.status != .upcoming {
                HStack(spacing: 14) {
                    Text("Q1 \(q.q1Home):\(q.q1Away)")
                    Text("Q2 \(q.q2Home):\(q.q2Away)")
                    Text("Q3 \(q.q3Home):\(q.q3Away)")
                    Text("Q4 \(q.q4Home):\(q.q4Away)")
                    if let otH = q.otHome, let otA = q.otAway {
                        Text("OT \(otH):\(otA)")
                            .foregroundStyle(palette.euroLeagueOrange)
                    }
                }
                .font(.system(size: 11, weight: .heavy, design: .monospaced))
                .foregroundStyle(palette.secondaryText)
            }
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 18, isSelected: true, accentGlow: Color(hex: game.homeTeam.primaryColorHex))
    }

    private func perGameRemindersQuickStrip(_ game: EuroLeagueGame) -> some View {
        let active = vm.perGameReminders[game.id] ?? []
        let allSelected = active.count == GameReminderType.allCases.count

        return HStack(spacing: 8) {
            Button {
                vm.reminderSheetGame = game
            } label: {
                HStack(spacing: 5) {
                    Image(systemName: active.isEmpty ? "bell" : "bell.badge.fill")
                        .foregroundStyle(active.isEmpty ? palette.secondaryText : palette.euroLeagueOrange)
                    Text(vm.strings.gameRemindersTitle.replacingOccurrences(of: "🔔 ", with: ""))
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(palette.primaryText)
                }
            }
            .buttonStyle(.plain)

            ForEach(GameReminderType.allCases) { type in
                let isOn = active.contains(type)
                let accent = type == .crunchTime ? palette.neonMissedRed : palette.euroLeagueOrange
                Button {
                    vm.toggleSingleReminder(for: game, type: type)
                } label: {
                    Text(type.shortBadge)
                        .font(.system(size: 11, weight: isOn ? .black : .semibold))
                        .foregroundStyle(isOn ? accent : palette.mutedText)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(isOn ? accent.opacity(0.18) : palette.pillSurface, in: Capsule())
                }
                .buttonStyle(.plain)
            }

            Spacer()

            Button {
                vm.updateReminders(
                    for: game,
                    reminders: allSelected ? [] : Set(GameReminderType.allCases)
                )
            } label: {
                Text(allSelected ? vm.strings.clearRemindersLabel : vm.strings.enableAllRemindersLabel)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(allSelected ? palette.neonMadeGreen : palette.secondaryText)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(palette.pillSurface, in: Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .liquidGlassCard(cornerRadius: 14)
    }

    private func courtMomentumPlayByPlayCard(_ game: EuroLeagueGame) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                courtTabButton(0, title: vm.strings.shotChartTab)
                courtTabButton(1, title: vm.strings.momentumTab)
                courtTabButton(2, title: vm.strings.playByPlayTab)

                Spacer()

                if vm.activeCourtTab != 1 {
                    ForEach([(0, "ALL"), (1, "Q1"), (2, "Q2"), (3, "Q3"), (4, "Q4"), (5, "OT")], id: \.0) { (qVal, label) in
                        Button {
                            vm.selectedQuarterFilter = qVal
                        } label: {
                            Text(label)
                                .font(.system(size: 10, weight: vm.selectedQuarterFilter == qVal ? .black : .semibold))
                                .foregroundStyle(vm.selectedQuarterFilter == qVal ? palette.euroLeagueOrange : palette.mutedText)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(vm.selectedQuarterFilter == qVal ? palette.euroLeagueOrange.opacity(0.16) : palette.pillSurface, in: Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }

                if vm.activeCourtTab == 2 {
                    Button {
                        vm.clutchOnlyPbp.toggle()
                    } label: {
                        Text(vm.strings.clutchFilterLabel)
                            .font(.system(size: 10, weight: .black))
                            .foregroundStyle(vm.clutchOnlyPbp ? .white : palette.neonMissedRed)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(vm.clutchOnlyPbp ? palette.neonMissedRed : palette.neonMissedRed.opacity(0.14), in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }

            if vm.activeCourtTab == 0 {
                fibaShotChartCanvas(game)
            } else if vm.activeCourtTab == 1 {
                leadMomentumCanvas(game)
            } else {
                playByPlayListView(game)
            }
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 18)
    }

    private func courtTabButton(_ idx: Int, title: String) -> some View {
        let active = vm.activeCourtTab == idx
        return Button {
            vm.activeCourtTab = idx
        } label: {
            Text(title)
                .font(.system(size: 12, weight: active ? .black : .semibold))
                .foregroundStyle(active ? .white : palette.primaryText)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(active ? palette.euroLeagueOrange : palette.pillSurface, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func fibaShotChartCanvas(_ game: EuroLeagueGame) -> some View {
        let visibleShots = game.shots.filter { vm.selectedQuarterFilter == 0 || $0.quarter == vm.selectedQuarterFilter }
        let madeCount = visibleShots.filter(\.isMade).count

        return VStack(alignment: .leading, spacing: 8) {
            GeometryReader { geo in
                ZStack {
                    Canvas { ctx, size in
                        let lineColor = palette.mode.isLight ? Color.slateLineLight : Color.white.opacity(0.24)
                        let stroke = StrokeStyle(lineWidth: 1.5)

                        // Outer court boundary
                        let border = Path(CGRect(x: 8, y: 8, width: size.width - 16, height: size.height - 16))
                        ctx.stroke(border, with: .color(lineColor), style: stroke)

                        // Paint / Key
                        let keyW = size.width * 0.32
                        let keyH = size.height * 0.42
                        let keyRect = CGRect(x: (size.width - keyW) / 2, y: 8, width: keyW, height: keyH)
                        ctx.stroke(Path(keyRect), with: .color(lineColor), style: stroke)

                        // Free-throw circle
                        let ftRadius = keyW * 0.36
                        let ftCircle = Path(ellipseIn: CGRect(
                            x: size.width / 2 - ftRadius,
                            y: 8 + keyH - ftRadius,
                            width: ftRadius * 2,
                            height: ftRadius * 2
                        ))
                        ctx.stroke(ftCircle, with: .color(lineColor), style: stroke)

                        // Hoop
                        let hoopCenter = CGPoint(x: size.width / 2, y: 28)
                        let hoop = Path(ellipseIn: CGRect(x: hoopCenter.x - 7, y: hoopCenter.y - 7, width: 14, height: 14))
                        ctx.stroke(hoop, with: .color(palette.euroLeagueOrange), style: StrokeStyle(lineWidth: 2.2))

                        // FIBA 3-Point Arc
                        var arc = Path()
                        let arcR = min(size.width * 0.42, size.height * 0.68)
                        arc.addArc(center: hoopCenter, radius: arcR, startAngle: .degrees(12), endAngle: .degrees(168), clockwise: false)
                        ctx.stroke(arc, with: .color(lineColor), style: stroke)

                        // Plot Shots
                        for shot in visibleShots {
                            let pt = CGPoint(
                                x: 14 + CGFloat(shot.courtX) * (size.width - 28),
                                y: 18 + CGFloat(shot.courtY) * (size.height - 32)
                            )
                            let r: CGFloat = shot.isThreePointer ? 6.2 : 5.2
                            let dot = Path(ellipseIn: CGRect(x: pt.x - r, y: pt.y - r, width: r * 2, height: r * 2))
                            if shot.isMade {
                                ctx.fill(dot, with: .color(palette.neonMadeGreen.opacity(0.88)))
                            } else {
                                ctx.stroke(dot, with: .color(palette.neonMissedRed.opacity(0.85)), style: StrokeStyle(lineWidth: 2))
                            }
                        }
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture { loc in
                    let nearest = visibleShots.min(by: { a, b in
                        let ax = 14 + CGFloat(a.courtX) * (geo.size.width - 28)
                        let ay = 18 + CGFloat(a.courtY) * (geo.size.height - 32)
                        let bx = 14 + CGFloat(b.courtX) * (geo.size.width - 28)
                        let by = 18 + CGFloat(b.courtY) * (geo.size.height - 32)
                        return hypot(ax - loc.x, ay - loc.y) < hypot(bx - loc.x, by - loc.y)
                    })
                    vm.inspectedShot = nearest
                }
            }
            .frame(height: 245)
            .background(palette.pillSurface.opacity(0.55), in: RoundedRectangle(cornerRadius: 14, style: .continuous))

            HStack {
                Label("\(madeCount)/\(visibleShots.count) FG", systemImage: "target")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(palette.neonMadeGreen)

                if let s = vm.inspectedShot {
                    Text("• \(s.playerName) (\(s.isThreePointer ? "3PT" : "2PT") \(s.isMade ? "MADE" : "MISS") • Q\(s.quarter))")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(palette.primaryText)
                } else {
                    Text("• Click any shot marker to inspect shooter")
                        .font(.system(size: 11))
                        .foregroundStyle(palette.mutedText)
                }
                Spacer()
            }
        }
    }

    private func leadMomentumCanvas(_ game: EuroLeagueGame) -> some View {
        let summary = CourtsideAnalytics.computeLeadMomentum(game: game)
        let maxAbs = CGFloat(max(8, max(summary.maxHomeLead, summary.maxAwayLead) + 2))
        let homeColor = Color(hex: game.homeTeam.primaryColorHex)
        let awayColor = Color(hex: game.awayTeam.primaryColorHex)

        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 16) {
                Text("\(game.homeTeam.code) Max Lead: +\(summary.maxHomeLead)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(homeColor)
                Text("\(game.awayTeam.code) Max Lead: +\(summary.maxAwayLead)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(awayColor)
                Spacer()
                Text("Lead Changes: \(summary.leadChanges)")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundStyle(palette.euroLeagueOrange)
            }

            Canvas { ctx, size in
                let midY = size.height / 2
                var zeroLine = Path()
                zeroLine.move(to: CGPoint(x: 0, y: midY))
                zeroLine.addLine(to: CGPoint(x: size.width, y: midY))
                ctx.stroke(zeroLine, with: .color(palette.mutedText.opacity(0.4)), style: StrokeStyle(lineWidth: 1, dash: [4, 4]))

                guard summary.points.count >= 2 else { return }
                let maxMin = max(40.0, summary.points.last?.minute ?? 40.0)
                var line = Path()
                for (i, p) in summary.points.enumerated() {
                    let x = CGFloat(p.minute / maxMin) * size.width
                    let y = midY - (CGFloat(p.homeLead) / maxAbs) * (size.height * 0.44)
                    if i == 0 { line.move(to: CGPoint(x: x, y: y)) }
                    else { line.addLine(to: CGPoint(x: x, y: y)) }
                }
                ctx.stroke(line, with: .color(palette.euroLeagueOrange), style: StrokeStyle(lineWidth: 2.6, lineJoin: .round))
            }
            .frame(height: 200)
            .padding(10)
            .background(palette.pillSurface.opacity(0.55), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private func playByPlayListView(_ game: EuroLeagueGame) -> some View {
        let events = game.playByPlay
            .filter { vm.selectedQuarterFilter == 0 || $0.quarter == vm.selectedQuarterFilter }
            .filter { !vm.clutchOnlyPbp || $0.isClutchPlay }
            .reversed()

        return Group {
            if events.isEmpty {
                Text("No Play-by-Play events recorded for this filter yet.")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(palette.mutedText)
                    .padding(.vertical, 24)
                    .frame(maxWidth: .infinity)
            } else {
                VStack(spacing: 6) {
                    ForEach(Array(events.prefix(60))) { ev in
                        HStack(spacing: 10) {
                            Text("Q\(ev.quarter) \(ev.clock)")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundStyle(palette.mutedText)
                                .frame(width: 68, alignment: .leading)

                            Text(ev.category.badgeLabel)
                                .font(.system(size: 9, weight: .black))
                                .foregroundStyle(ev.pointsScored > 0 ? palette.neonMadeGreen : palette.euroLeagueOrange)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(palette.pillSurface, in: Capsule())

                            VStack(alignment: .leading, spacing: 1) {
                                if !ev.playerName.isEmpty {
                                    Text(ev.playerName)
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundStyle(palette.primaryText)
                                }
                                Text(ev.description)
                                    .font(.system(size: 11))
                                    .foregroundStyle(palette.secondaryText)
                            }
                            Spacer()
                            Text("\(ev.homeScore):\(ev.awayScore)")
                                .font(.system(size: 12, weight: .black, design: .monospaced))
                                .foregroundStyle(palette.primaryText)
                        }
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(palette.pillSurface.opacity(0.6), in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                        .onTapGesture {
                            if !ev.playerName.isEmpty,
                               let prof = vm.resolvePlayerProfile(playerId: ev.playerId, playerName: ev.playerName, teamId: ev.teamId) {
                                vm.selectedPlayerProfile = prof
                            }
                        }
                    }
                }
            }
        }
    }

    private func boxscoreCard(_ game: EuroLeagueGame) -> some View {
        let rows = (vm.selectedBoxscoreSide == 0 ? game.boxscoreHome : game.boxscoreAway)
            .sorted { $0.pir > $1.pir }
        let activeTeam = vm.selectedBoxscoreSide == 0 ? game.homeTeam : game.awayTeam

        return VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(vm.strings.playerBoxscoreHeader)
                    .font(.system(size: 15, weight: .black, design: .rounded))
                    .foregroundStyle(palette.primaryText)
                Spacer()
                HStack(spacing: 6) {
                    boxSideButton(0, team: game.homeTeam)
                    boxSideButton(1, team: game.awayTeam)
                }
            }

            if rows.isEmpty {
                Text("Boxscore becomes available at Tip-Off.")
                    .font(.system(size: 12))
                    .foregroundStyle(palette.mutedText)
                    .padding(.vertical, 12)
            } else {
                VStack(spacing: 4) {
                    HStack {
                        Text("PLAYER").frame(maxWidth: .infinity, alignment: .leading)
                        Text("MIN").frame(width: 46)
                        Text("PTS").frame(width: 36)
                        Text("REB").frame(width: 36)
                        Text("AST").frame(width: 36)
                        Text("2P").frame(width: 44)
                        Text("3P").frame(width: 44)
                        Text("FT").frame(width: 44)
                        Text("PIR").frame(width: 42)
                    }
                    .font(.system(size: 10, weight: .black))
                    .foregroundStyle(palette.mutedText)
                    .padding(.horizontal, 8)

                    ForEach(rows) { r in
                        HStack {
                            HStack(spacing: 6) {
                                Text("#\(r.jerseyNumber)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundStyle(palette.euroLeagueOrange)
                                    .frame(width: 24, alignment: .leading)
                                Text(r.playerName)
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundStyle(palette.primaryText)
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)

                            Text(r.minutes).frame(width: 46)
                            Text("\(r.points)").fontWeight(.black).frame(width: 36)
                            Text("\(r.totalRebounds)").frame(width: 36)
                            Text("\(r.assists)").frame(width: 36)
                            Text("\(r.twoPtMade)/\(r.twoPtAttempted)").frame(width: 44)
                            Text("\(r.threePtMade)/\(r.threePtAttempted)").frame(width: 44)
                            Text("\(r.ftMade)/\(r.ftAttempted)").frame(width: 44)
                            Text("\(r.pir)")
                                .font(.system(size: 12, weight: .black))
                                .foregroundStyle(palette.euroLeagueOrange)
                                .frame(width: 42)
                        }
                        .font(.system(size: 11, design: .monospaced))
                        .foregroundStyle(palette.secondaryText)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(palette.pillSurface.opacity(0.6), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                        .onTapGesture {
                            if let prof = vm.resolvePlayerProfile(playerId: r.playerId, playerName: r.playerName, teamId: activeTeam.id) {
                                vm.selectedPlayerProfile = prof
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .liquidGlassCard(cornerRadius: 18)
    }

    private func boxSideButton(_ side: Int, team: EuroLeagueTeam) -> some View {
        let active = vm.selectedBoxscoreSide == side
        return Button {
            vm.selectedBoxscoreSide = side
        } label: {
            HStack(spacing: 6) {
                ClubCrestView(team: team, size: 18)
                Text(team.code)
                    .font(.system(size: 11, weight: .black))
            }
            .foregroundStyle(active ? .white : palette.primaryText)
            .padding(.horizontal, 11)
            .padding(.vertical, 5)
            .background(active ? palette.euroLeagueOrange : palette.pillSurface, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

private extension Color {
    static let slateLineLight = Color(red: 0.35, green: 0.42, blue: 0.54).opacity(0.45)
}
