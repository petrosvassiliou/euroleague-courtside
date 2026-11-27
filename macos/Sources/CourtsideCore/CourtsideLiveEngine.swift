import Foundation

// MARK: - Snapshot & Offline Cache DTOs

public struct BundledSnapshotEnvelope: Codable, Sendable {
    public var seasonCode: String?
    public var activeRound: Int?
    public var teams: [EuroLeagueTeam]
    public var players: [PlayerProfile]
    public var games: [BundledGameDto]

    public struct BundledGameDto: Codable, Sendable {
        public var id: String
        public var gameCode: Int?
        public var seasonCode: String?
        public var round: Int
        public var homeTeamId: String?
        public var awayTeamId: String?
        public var homeTeam: EuroLeagueTeam?
        public var awayTeam: EuroLeagueTeam?
        public var venueName: String?
        public var tipOffEpochMillis: Int64?
        public var status: GameStatus?
        public var currentQuarter: Int?
        public var remainingClock: String?
        public var homeScore: Int?
        public var awayScore: Int?
        public var quarters: QuarterScores?
        public var headToHeadHomeWins: Int?
        public var headToHeadAwayWins: Int?
        public var boxscoreHome: [PlayerBoxscoreStat]?
        public var boxscoreAway: [PlayerBoxscoreStat]?
        public var shotChart: [ShotAttempt]?
        public var shots: [ShotAttempt]?
        public var playByPlay: [PlayByPlayEvent]?
    }
}

public struct OfflineGamecastCacheV2: Codable, Sendable {
    public var version: Int = 2
    public var savedAtEpochMs: Int64
    public var seasonCode: String
    public var activeRound: Int
    public var teams: [EuroLeagueTeam]
    public var players: [PlayerProfile]
    public var games: [EuroLeagueGame]

    public init(
        savedAtEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        seasonCode: String,
        activeRound: Int,
        teams: [EuroLeagueTeam],
        players: [PlayerProfile],
        games: [EuroLeagueGame]
    ) {
        self.version = 2
        self.savedAtEpochMs = savedAtEpochMs
        self.seasonCode = seasonCode
        self.activeRound = activeRound
        self.teams = teams
        self.players = players
        self.games = games
    }
}

// MARK: - Live EuroLeague API DTOs (`live.euroleague.net/api/*` & `api-live.euroleague.net/v2/*`)

public struct LiveHeaderDto: Codable, Sendable {
    public var Live: Bool?
    public var Round: String?
    public var ScoreA: String?
    public var ScoreB: String?
    public var Quarter: String?
    public var RemainingPartialTime: String?
    public var GameTime: String?
    public var ScoreQuarter1A: Int?
    public var ScoreQuarter2A: Int?
    public var ScoreQuarter3A: Int?
    public var ScoreQuarter4A: Int?
    public var ScoreExtraTimeA: Int?
    public var ScoreQuarter1B: Int?
    public var ScoreQuarter2B: Int?
    public var ScoreQuarter3B: Int?
    public var ScoreQuarter4B: Int?
    public var ScoreExtraTimeB: Int?

    public init(
        Live: Bool? = nil, Round: String? = nil, ScoreA: String? = nil, ScoreB: String? = nil,
        Quarter: String? = nil, RemainingPartialTime: String? = nil, GameTime: String? = nil,
        ScoreQuarter1A: Int? = nil, ScoreQuarter2A: Int? = nil, ScoreQuarter3A: Int? = nil, ScoreQuarter4A: Int? = nil, ScoreExtraTimeA: Int? = nil,
        ScoreQuarter1B: Int? = nil, ScoreQuarter2B: Int? = nil, ScoreQuarter3B: Int? = nil, ScoreQuarter4B: Int? = nil, ScoreExtraTimeB: Int? = nil
    ) {
        self.Live = Live; self.Round = Round; self.ScoreA = ScoreA; self.ScoreB = ScoreB
        self.Quarter = Quarter; self.RemainingPartialTime = RemainingPartialTime; self.GameTime = GameTime
        self.ScoreQuarter1A = ScoreQuarter1A; self.ScoreQuarter2A = ScoreQuarter2A; self.ScoreQuarter3A = ScoreQuarter3A; self.ScoreQuarter4A = ScoreQuarter4A; self.ScoreExtraTimeA = ScoreExtraTimeA
        self.ScoreQuarter1B = ScoreQuarter1B; self.ScoreQuarter2B = ScoreQuarter2B; self.ScoreQuarter3B = ScoreQuarter3B; self.ScoreQuarter4B = ScoreQuarter4B; self.ScoreExtraTimeB = ScoreExtraTimeB
    }
}

public struct LiveBoxscoreDto: Codable, Sendable {
    public var Stats: [TeamStatsSection]?

    public struct TeamStatsSection: Codable, Sendable {
        public var Team: String?
        public var PlayersStats: [PlayerRow]?
    }

    public struct PlayerRow: Codable, Sendable {
        public var Player_ID: String?
        public var Team: String?
        public var Dorsal: String?
        public var Player: String?
        public var Minutes: String?
        public var Points: Int?
        public var FieldGoalsMade2: Int?
        public var FieldGoalsAttempted2: Int?
        public var FieldGoalsMade3: Int?
        public var FieldGoalsAttempted3: Int?
        public var FreeThrowsMade: Int?
        public var FreeThrowsAttempted: Int?
        public var OffensiveRebounds: Int?
        public var DefensiveRebounds: Int?
        public var Assistances: Int?
        public var Steals: Int?
        public var Turnovers: Int?
        public var BlocksFavour: Int?
        public var BlocksAgainst: Int?
        public var FoulsCommited: Int?
        public var FoulsReceived: Int?
        public var Valuation: Int?
    }
}

public struct LivePointsDto: Codable, Sendable {
    public var Rows: [ShotRow]?

    public struct ShotRow: Codable, Sendable {
        public var NUM_ANOT: Int?
        public var TEAM: String?
        public var ID_PLAYER: String?
        public var PLAYER: String?
        public var ID_ACTION: String?
        public var COORD_X: Double?
        public var COORD_Y: Double?
        public var MINUTE: Int?
    }
}

public struct LivePlayByPlayDto: Codable, Sendable {
    public var Live: Bool?
    public var CodeTeamA: String?
    public var CodeTeamB: String?
    public var ActualQuarter: Int?
    public var FirstQuarter: [PlayRow]?
    public var SecondQuarter: [PlayRow]?
    public var ThirdQuarter: [PlayRow]?
    public var ForthQuarter: [PlayRow]?
    public var FourthQuarter: [PlayRow]?
    public var ExtraTime: [PlayRow]?

    public struct PlayRow: Codable, Sendable {
        public var NUMBEROFPLAY: Int?
        public var CODETEAM: String?
        public var PLAYER_ID: String?
        public var PLAYTYPE: String?
        public var PLAYER: String?
        public var DORSAL: String?
        public var MINUTE: Int?
        public var MARKERTIME: String?
        public var POINTS_A: Int?
        public var POINTS_B: Int?
        public var PLAYINFO: String?
    }
}

// MARK: - Notifications, Reminders & Updater Models

public enum AutoAlertKind: String, Codable, Sendable {
    case tipOff = "TIP_OFF"
    case q1Score = "Q1_SCORE"
    case halftimeScore = "HALFTIME_SCORE"
    case q3Score = "Q3_SCORE"
    case crunchTime = "CRUNCH_TIME"
    case finalScore = "FINAL_SCORE"
}

public struct DispatchedAlertAction: Hashable, Sendable {
    public var dedupeKey: String
    public var kind: AutoAlertKind
    public var gameId: String
    public var round: Int
    public var title: String
    public var body: String
}

public struct AppReleaseCheckResult: Hashable, Sendable {
    public enum Status: String, Sendable {
        case idle, checking, upToDate, updateAvailable, error
    }

    public var status: Status
    public var latestVersionName: String
    public var releaseTitle: String
    public var htmlUrl: String
    public var macDownloadUrl: String
    public var isPreRelease: Bool
    public var lastCheckedTime: String
    public var errorMessage: String?

    public init(
        status: Status = .idle,
        latestVersionName: String = "",
        releaseTitle: String = "",
        htmlUrl: String = "https://github.com/petrosvassiliou/euroleague-courtside/releases",
        macDownloadUrl: String = "",
        isPreRelease: Bool = false,
        lastCheckedTime: String = "",
        errorMessage: String? = nil
    ) {
        self.status = status
        self.latestVersionName = latestVersionName
        self.releaseTitle = releaseTitle
        self.htmlUrl = htmlUrl
        self.macDownloadUrl = macDownloadUrl
        self.isPreRelease = isPreRelease
        self.lastCheckedTime = lastCheckedTime
        self.errorMessage = errorMessage
    }
}

// MARK: - Live Sync, Cloudflare Cache Bypass, Alert & Update Engine

public enum CourtsideLiveEngine {

    public static let currentVersionName = "2.0.0-beta.2"
    public static let currentBuildNumber = 26

    /// Cloudflare 3-second cache-busting bucket (`&_t=`) matching Android's high-speed 5s ticker.
    public static func buildLiveApiUrl(
        endpoint: String,
        gameCode: Int,
        seasonCode: String = "E2026",
        nowMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> String {
        let bucket = nowMillis / 3_000
        return "https://live.euroleague.net/api/\(endpoint)?gamecode=\(gameCode)&seasoncode=\(seasonCode)&_t=\(bucket)"
    }

    public static func decodeBundledSnapshot(_ jsonData: Data) throws -> (
        seasonCode: String,
        activeRound: Int,
        teams: [EuroLeagueTeam],
        players: [PlayerProfile],
        games: [EuroLeagueGame]
    ) {
        let decoder = JSONDecoder()
        let snap = try decoder.decode(BundledSnapshotEnvelope.self, from: jsonData)
        let byId = Dictionary(uniqueKeysWithValues: snap.teams.map { ($0.id, $0) })
        let byCode = Dictionary(uniqueKeysWithValues: snap.teams.map { ($0.code, $0) })

        let mappedGames: [EuroLeagueGame] = snap.games.compactMap { g in
            let home = g.homeTeam
                ?? g.homeTeamId.flatMap { byId[$0] ?? byCode[$0] }
                ?? snap.teams.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(g.homeTeamId ?? "") }
            let away = g.awayTeam
                ?? g.awayTeamId.flatMap { byId[$0] ?? byCode[$0] }
                ?? snap.teams.first { CourtsideAnalytics.canonicalClubId($0.id) == CourtsideAnalytics.canonicalClubId(g.awayTeamId ?? "") }
            guard let resolvedHome = home, let resolvedAway = away else { return nil }
            let code = g.gameCode ?? Int(g.id.split(separator: "_").last ?? "0") ?? 0
            return EuroLeagueGame(
                id: g.id,
                gameCode: code,
                seasonCode: g.seasonCode ?? snap.seasonCode ?? "E2026",
                round: g.round,
                homeTeam: resolvedHome,
                awayTeam: resolvedAway,
                venueName: g.venueName ?? resolvedHome.arena,
                tipOffEpochMillis: g.tipOffEpochMillis ?? 0,
                status: g.status ?? .upcoming,
                currentQuarter: g.currentQuarter ?? 1,
                remainingClock: g.remainingClock ?? "10:00",
                homeScore: g.homeScore ?? 0,
                awayScore: g.awayScore ?? 0,
                quarters: g.quarters ?? QuarterScores(),
                headToHeadHomeWins: g.headToHeadHomeWins ?? 0,
                headToHeadAwayWins: g.headToHeadAwayWins ?? 0,
                boxscoreHome: g.boxscoreHome ?? [],
                boxscoreAway: g.boxscoreAway ?? [],
                shots: g.shotChart ?? g.shots ?? [],
                playByPlay: g.playByPlay ?? []
            )
        }.sorted { ($0.round, $0.tipOffEpochMillis) < ($1.round, $1.tipOffEpochMillis) }

        let recomputedTeams = CourtsideAnalytics.recomputeAndSortStandings(teams: snap.teams, games: mappedGames)
        return (
            seasonCode: snap.seasonCode ?? "E2026",
            activeRound: snap.activeRound ?? 14,
            teams: recomputedTeams,
            players: snap.players,
            games: mappedGames
        )
    }

    public static func saveOfflineCacheV2(_ cache: OfflineGamecastCacheV2, to url: URL) throws {
        let dir = url.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(cache)
        try data.write(to: url, options: .atomic)
    }

    public static func loadOfflineCacheV2(from url: URL) throws -> OfflineGamecastCacheV2 {
        let data = try Data(contentsOf: url)
        return try JSONDecoder().decode(OfflineGamecastCacheV2.self, from: data)
    }

    // MARK: Live Header / Boxscore / Points / PlayByPlay Parsers

    public static func applyLiveHeader(to game: EuroLeagueGame, header: LiveHeaderDto) -> EuroLeagueGame {
        let scoreA = Int(header.ScoreA?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "")
        let scoreB = Int(header.ScoreB?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "")
        let hasScore = (scoreA ?? 0) > 0 || (scoreB ?? 0) > 0
        let isLiveFlag = header.Live ?? false
        let gameMins = Int(header.GameTime?.split(separator: ":").first ?? "0") ?? 0
        let qRaw = (header.Quarter ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let inferredQ = gameMins >= 1 ? min(max((gameMins - 1) / 10 + 1, 1), 5) : game.currentQuarter
        let parsedQ: Int = {
            if qRaw.contains("OT") || qRaw.contains("E") { return 5 }
            let digits = qRaw.filter(\.isNumber)
            if let q = Int(digits), (1...5).contains(q) { return q }
            return inferredQ
        }()

        let clockRaw = (header.RemainingPartialTime ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        let clock = clockRaw.isEmpty ? (isLiveFlag ? "00:00" : game.remainingClock) : clockRaw
        let isZeroClock = clock == "00:00" || clock == "0:00"
        let isEnded = hasScore && (!isLiveFlag || (gameMins >= 40 && isZeroClock && qRaw.isEmpty && scoreA != scoreB))

        let resolvedStatus: GameStatus = {
            if isEnded { return .final_ }
            if isLiveFlag && hasScore { return .live }
            if game.status == .final_ { return .final_ }
            return game.status
        }()

        let s1A = header.ScoreQuarter1A ?? 0
        let s2A = header.ScoreQuarter2A ?? 0
        let s3A = header.ScoreQuarter3A ?? 0
        let s4A = header.ScoreQuarter4A ?? 0
        let exA = header.ScoreExtraTimeA ?? 0
        let s1B = header.ScoreQuarter1B ?? 0
        let s2B = header.ScoreQuarter2B ?? 0
        let s3B = header.ScoreQuarter3B ?? 0
        let s4B = header.ScoreQuarter4B ?? 0
        let exB = header.ScoreExtraTimeB ?? 0

        let hasPartials = (s1A + s1B + s2A + s2B) > 0
        let updatedQuarters: QuarterScores
        if hasPartials {
            let cumA = s2A == 0 || (s2A >= s1A && s1A > 0)
            let cumB = s2B == 0 || (s2B >= s1B && s1B > 0)
            let q1H = max(0, s1A)
            let q2H = s2A <= 0 ? 0 : (cumA ? max(0, s2A - s1A) : s2A)
            let q3H = s3A <= 0 ? 0 : (cumA && s3A >= s2A ? max(0, s3A - s2A) : max(0, s3A))
            let q4H = s4A <= 0 ? 0 : (cumA && s4A >= s3A ? max(0, s4A - s3A) : max(0, s4A))
            let q1Aw = max(0, s1B)
            let q2Aw = s2B <= 0 ? 0 : (cumB ? max(0, s2B - s1B) : s2B)
            let q3Aw = s3B <= 0 ? 0 : (cumB && s3B >= s2B ? max(0, s3B - s2B) : max(0, s3B))
            let q4Aw = s4B <= 0 ? 0 : (cumB && s4B >= s3B ? max(0, s4B - s3B) : max(0, s4B))
            let regH = q1H + q2H + q3H + q4H
            let regAw = q1Aw + q2Aw + q3Aw + q4Aw
            let totH = scoreA ?? game.homeScore
            let totAw = scoreB ?? game.awayScore
            let otH: Int? = {
                if cumA && exA > s4A && s4A > 0 { return exA - s4A }
                if exA > 0 && !cumA { return exA }
                if (resolvedStatus == .final_ || parsedQ >= 5) && totH > regH && regH > 0 { return totH - regH }
                return nil
            }()
            let otAw: Int? = {
                if cumB && exB > s4B && s4B > 0 { return exB - s4B }
                if exB > 0 && !cumB { return exB }
                if (resolvedStatus == .final_ || parsedQ >= 5) && totAw > regAw && regAw > 0 { return totAw - regAw }
                return nil
            }()
            updatedQuarters = QuarterScores(
                q1Home: q1H, q1Away: q1Aw,
                q2Home: q2H, q2Away: q2Aw,
                q3Home: q3H, q3Away: q3Aw,
                q4Home: q4H, q4Away: q4Aw,
                otHome: otH, otAway: otAw
            )
        } else {
            updatedQuarters = game.quarters
        }

        var updated = game
        updated.status = resolvedStatus
        updated.currentQuarter = (resolvedStatus == .final_ && updatedQuarters.otHome != nil) ? 5 : parsedQ
        updated.remainingClock = resolvedStatus == .final_ ? "00:00" : clock
        updated.homeScore = scoreA ?? game.homeScore
        updated.awayScore = scoreB ?? game.awayScore
        updated.quarters = updatedQuarters
        return updated
    }

    public static func parseLivePlayByPlay(
        dto: LivePlayByPlayDto,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ) -> [PlayByPlayEvent] {
        let buckets: [(Int, [LivePlayByPlayDto.PlayRow])] = [
            (1, dto.FirstQuarter ?? []),
            (2, dto.SecondQuarter ?? []),
            (3, dto.ThirdQuarter ?? []),
            (4, dto.ForthQuarter ?? dto.FourthQuarter ?? []),
            (5, dto.ExtraTime ?? [])
        ]
        let homeCanon = CourtsideAnalytics.canonicalClubId(
            (dto.CodeTeamA ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? homeTeamId : (dto.CodeTeamA ?? homeTeamId)
        )
        var runH = 0
        var runA = 0
        var out: [PlayByPlayEvent] = []

        for (q, rows) in buckets {
            let qDur = q >= 5 ? 5.0 : 10.0
            let qStart = q <= 4 ? Double(q - 1) * 10.0 : 40.0
            for (idx, row) in rows.enumerated() {
                let pType = (row.PLAYTYPE ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
                if pType.isEmpty || pType == "IN" || pType == "OUT" { continue }

                let prevH = runH, prevA = runA
                if let pA = row.POINTS_A { runH = pA }
                if let pB = row.POINTS_B { runA = pB }
                let added = max(0, (runH - prevH) + (runA - prevA))

                let rawTeam = (row.CODETEAM ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let resolvedTeamId: String = {
                    if rawTeam.isEmpty { return "" }
                    let c = CourtsideAnalytics.canonicalClubId(rawTeam)
                    return (c == homeCanon || c == CourtsideAnalytics.canonicalClubId(homeTeamId)) ? homeTeamId : awayTeamId
                }()

                let mTime = (row.MARKERTIME ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let clock: String
                if mTime.contains(":") {
                    clock = mTime
                } else {
                    let minInQ = min(max((row.MINUTE ?? 0) - (q - 1) * 10, 0), Int(qDur))
                    clock = String(format: "%02d:00", max(0, Int(qDur) - minInQ))
                }
                let cParts = clock.split(separator: ":")
                let remM = Double(cParts.first ?? "0") ?? 0
                let remS = cParts.count > 1 ? (Double(cParts[1]) ?? 0) : 0
                let elapsed = min(max(qDur - (remM + remS / 60.0), 0), qDur)

                let category: PlayEventCategory = {
                    switch pType {
                    case "3FGM": return .threePt
                    case "DUNK": return .dunk
                    case "2FGM", "LAYUPMD": return .twoPt
                    case "FTM": return .freeThrow
                    case "2FGA", "3FGA", "FTA", "LAYUPATT": return .miss
                    case "AS", "D", "O": return .assistReb
                    case "ST", "FV", "AG": return .stealBlk
                    case "TO": return .turnover
                    case "CM", "CMU", "CMT", "CMD", "OF", "RV": return .foul
                    case "TVO", "TOUT", "TOUT_TV": return .timeout
                    default: return .period
                    }
                }()

                let formattedName = formatCommaLastFirstName(row.PLAYER)
                let pCode = (row.PLAYER_ID ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                let pId = (!resolvedTeamId.isEmpty && !pCode.isEmpty) ? "\(resolvedTeamId)_\(pCode.replacingOccurrences(of: "P", with: ""))" : ""
                let desc = (row.PLAYINFO ?? "").trimmingCharacters(in: .whitespacesAndNewlines)

                out.append(
                    PlayByPlayEvent(
                        id: "pbp_\(gameCode)_\(q)_\(row.NUMBEROFPLAY ?? idx)_\(idx)",
                        playNumber: (row.NUMBEROFPLAY ?? 0) > 0 ? (row.NUMBEROFPLAY ?? idx + 1) : (idx + 1),
                        quarter: q,
                        clock: clock,
                        gameMinute: qStart + elapsed,
                        teamId: resolvedTeamId,
                        playerId: pId,
                        playerName: formattedName,
                        jerseyNumber: Int((row.DORSAL ?? "").trimmingCharacters(in: .whitespacesAndNewlines)),
                        playType: pType,
                        category: category,
                        description: desc.isEmpty ? category.badgeLabel : desc,
                        homeScore: runH,
                        awayScore: runA,
                        pointsScored: added
                    )
                )
            }
        }
        return out
    }

    public static func parseLiveBoxscore(
        dto: LiveBoxscoreDto,
        homeTeamId: String,
        awayTeamId: String
    ) -> (home: [PlayerBoxscoreStat], away: [PlayerBoxscoreStat]) {
        guard let sections = dto.Stats, !sections.isEmpty else { return ([], []) }
        func mapRows(_ rows: [LiveBoxscoreDto.PlayerRow], teamId: String) -> [PlayerBoxscoreStat] {
            rows.compactMap { r in
                let mins = (r.Minutes ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                if mins.uppercased() == "DNP" || mins.isEmpty { return nil }
                let cleanCode = (r.Player_ID ?? "").trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "P", with: "")
                return PlayerBoxscoreStat(
                    playerId: cleanCode.isEmpty ? "" : "\(teamId)_\(cleanCode)",
                    playerName: formatCommaLastFirstName(r.Player),
                    jerseyNumber: Int((r.Dorsal ?? "").trimmingCharacters(in: .whitespacesAndNewlines)) ?? 0,
                    teamId: teamId,
                    minutes: mins,
                    points: r.Points ?? 0,
                    offRebounds: r.OffensiveRebounds ?? 0,
                    defRebounds: r.DefensiveRebounds ?? 0,
                    assists: r.Assistances ?? 0,
                    steals: r.Steals ?? 0,
                    blocks: r.BlocksFavour ?? 0,
                    blocksAgainst: r.BlocksAgainst ?? 0,
                    turnovers: r.Turnovers ?? 0,
                    twoPtMade: r.FieldGoalsMade2 ?? 0,
                    twoPtAttempted: r.FieldGoalsAttempted2 ?? 0,
                    threePtMade: r.FieldGoalsMade3 ?? 0,
                    threePtAttempted: r.FieldGoalsAttempted3 ?? 0,
                    ftMade: r.FreeThrowsMade ?? 0,
                    ftAttempted: r.FreeThrowsAttempted ?? 0,
                    foulsDrawn: r.FoulsReceived ?? 0,
                    foulsCommitted: r.FoulsCommited ?? 0,
                    valuationOverride: r.Valuation
                )
            }
        }
        let homeRows = mapRows(sections.first?.PlayersStats ?? [], teamId: homeTeamId)
        let awayRows = sections.count > 1 ? mapRows(sections[1].PlayersStats ?? [], teamId: awayTeamId) : []
        return (homeRows, awayRows)
    }

    public static func parseLiveShots(
        dto: LivePointsDto,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ) -> [ShotAttempt] {
        guard let rows = dto.Rows else { return [] }
        let homeCanon = CourtsideAnalytics.canonicalClubId(homeTeamId)
        return rows.enumerated().compactMap { (idx, r) in
            let action = (r.ID_ACTION ?? "").trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            guard ["2FGM", "2FGA", "3FGM", "3FGA", "DUNK", "LAYUPMD", "LAYUPATT"].contains(action) else { return nil }
            let made = action == "2FGM" || action == "3FGM" || action == "DUNK" || action == "LAYUPMD"
            let is3 = action.hasPrefix("3")
            let rawTeam = (r.TEAM ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let tId = CourtsideAnalytics.canonicalClubId(rawTeam) == homeCanon ? homeTeamId : awayTeamId
            // EuroLeague live Points COORD_X is roughly -750..+750 cm, COORD_Y is -100..+1300 cm from hoop
            let nx = ((r.COORD_X ?? 0) + 750.0) / 1500.0
            let ny = ((r.COORD_Y ?? 150.0) + 157.5) / 1400.0
            let minVal = max(1, r.MINUTE ?? 1)
            let q = minVal > 40 ? 5 : min(max((minVal - 1) / 10 + 1, 1), 4)
            let pCode = (r.ID_PLAYER ?? "").trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "P", with: "")
            return ShotAttempt(
                id: "shot_\(gameCode)_\(r.NUM_ANOT ?? idx)_\(idx)",
                playerId: pCode.isEmpty ? "" : "\(tId)_\(pCode)",
                playerName: formatCommaLastFirstName(r.PLAYER),
                teamId: tId,
                courtX: nx,
                courtY: ny,
                isMade: made,
                isThreePointer: is3,
                quarter: q
            )
        }
    }

    private static func formatCommaLastFirstName(_ raw: String?) -> String {
        let trimmed = (raw ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }
        let ordered: String
        if trimmed.contains(",") {
            let parts = trimmed.split(separator: ",", maxSplits: 1).map { $0.trimmingCharacters(in: .whitespaces) }
            ordered = parts.count == 2 ? "\(parts[1]) \(parts[0])" : trimmed
        } else {
            ordered = trimmed
        }
        let titled = ordered
            .lowercased()
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
        return CourtsideAnalytics.cleanPlayerDisplayName(titled)
    }

    // MARK: - Alert & Menu Bar Scoreboard Evaluation

    public static func evaluateAlertActions(
        games: [EuroLeagueGame],
        favoriteTeamIds: Set<String>,
        perGameReminders: [String: Set<GameReminderType>],
        tipOffEnabled: Bool,
        halftimeFinalEnabled: Bool,
        closeGameEnabled: Bool,
        alreadySentKeys: Set<String>,
        language: AppLanguage,
        nowMillis: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) -> [DispatchedAlertAction] {
        let favCanon = Set(favoriteTeamIds.map { CourtsideAnalytics.canonicalClubId($0) })
        let strings = CourtsideTranslations.forLanguage(language)
        var actions: [DispatchedAlertAction] = []

        for g in games {
            let isFav = favCanon.contains(CourtsideAnalytics.canonicalClubId(g.homeTeam.id)) ||
                favCanon.contains(CourtsideAnalytics.canonicalClubId(g.awayTeam.id))
            let reminders = perGameReminders[g.id] ?? []
            if !isFav && reminders.isEmpty { continue }

            let scoreLine = "\(g.homeTeam.code) \(g.homeScore) : \(g.awayScore) \(g.awayTeam.code)"

            // Tip-Off (upcoming within 4 hours for favorite club)
            if isFav && tipOffEnabled && g.status == .upcoming {
                let delta = g.tipOffEpochMillis - nowMillis
                if delta > 0 && delta <= 4 * 3_600_000 {
                    let key = "tipoff_\(g.id)"
                    if !alreadySentKeys.contains(key) {
                        let mins = max(1, delta / 60_000)
                        actions.append(DispatchedAlertAction(
                            dedupeKey: key, kind: .tipOff, gameId: g.id, round: g.round,
                            title: "🏀 \(strings.nextTipOffBadge) (\(mins)m) • \(g.homeTeam.code) vs \(g.awayTeam.code)",
                            body: "\(g.homeTeam.name) vs \(g.awayTeam.name) • \(g.venueName)"
                        ))
                    }
                }
            }

            // End of Q1
            if reminders.contains(.q1Result) && g.status == .live &&
                ((g.currentQuarter == 1 && (g.remainingClock == "00:00" || g.remainingClock == "0:00")) || g.currentQuarter == 2) &&
                (g.quarters.q1Home + g.quarters.q1Away) > 0 {
                let key = "q1_\(g.id)"
                if !alreadySentKeys.contains(key) {
                    actions.append(DispatchedAlertAction(
                        dedupeKey: key, kind: .q1Score, gameId: g.id, round: g.round,
                        title: "🔔 \(strings.reminderQ1Label) • \(g.homeTeam.code) \(g.quarters.q1Home) : \(g.quarters.q1Away) \(g.awayTeam.code)",
                        body: "\(g.homeTeam.name) vs \(g.awayTeam.name) • \(strings.roundPrefix) \(g.round)"
                    ))
                }
            }

            // Halftime
            let wantHt = reminders.contains(.halftimeResult) || (isFav && halftimeFinalEnabled)
            if wantHt && g.status == .live && g.currentQuarter == 2 && (g.remainingClock == "00:00" || g.remainingClock == "0:00") {
                let key = "ht_\(g.id)"
                if !alreadySentKeys.contains(key) {
                    actions.append(DispatchedAlertAction(
                        dedupeKey: key, kind: .halftimeScore, gameId: g.id, round: g.round,
                        title: "⏸️ \(strings.reminderHalftimeLabel) • \(scoreLine)",
                        body: "\(g.homeTeam.name) vs \(g.awayTeam.name) • Q1 \(g.quarters.q1Home):\(g.quarters.q1Away) • Q2 \(g.quarters.q2Home):\(g.quarters.q2Away)"
                    ))
                }
            }

            // End of Q3
            if reminders.contains(.q3Result) && g.status == .live &&
                ((g.currentQuarter == 3 && (g.remainingClock == "00:00" || g.remainingClock == "0:00")) || g.currentQuarter == 4) &&
                (g.quarters.q3Home + g.quarters.q3Away) > 0 {
                let key = "q3_\(g.id)"
                if !alreadySentKeys.contains(key) {
                    actions.append(DispatchedAlertAction(
                        dedupeKey: key, kind: .q3Score, gameId: g.id, round: g.round,
                        title: "🔔 \(strings.reminderQ3Label) • \(scoreLine)",
                        body: "\(g.homeTeam.name) vs \(g.awayTeam.name) • Q3 \(g.quarters.q3Home):\(g.quarters.q3Away)"
                    ))
                }
            }

            // Crunch Time
            let wantCrunch = reminders.contains(.crunchTime) || (isFav && closeGameEnabled)
            if wantCrunch && g.isCloseGameFourthQuarter {
                let key = "crunch_\(g.id)"
                if !alreadySentKeys.contains(key) {
                    actions.append(DispatchedAlertAction(
                        dedupeKey: key, kind: .crunchTime, gameId: g.id, round: g.round,
                        title: "\(strings.crunchTimeBadge) • Q\(g.currentQuarter) \(g.remainingClock) • \(scoreLine)",
                        body: "\(g.homeTeam.name) vs \(g.awayTeam.name)"
                    ))
                }
            }

            // Final Score (recent within 18 hours so historical Round 1 games don't fire on boot)
            let wantFinal = reminders.contains(.finalScore) || (isFav && halftimeFinalEnabled)
            let isRecent = g.tipOffEpochMillis == 0 || abs(nowMillis - g.tipOffEpochMillis) <= 18 * 3_600_000
            if wantFinal && g.status == .final_ && isRecent {
                let key = "final_\(g.id)"
                if !alreadySentKeys.contains(key) {
                    actions.append(DispatchedAlertAction(
                        dedupeKey: key, kind: .finalScore, gameId: g.id, round: g.round,
                        title: "🏁 \(strings.statusFinal) • \(scoreLine)",
                        body: "\(g.homeTeam.name) vs \(g.awayTeam.name) • \(strings.roundPrefix) \(g.round)"
                    ))
                }
            }
        }
        return actions
    }

    public static func formatMenuBarLabel(
        games: [EuroLeagueGame],
        primaryTeam: EuroLeagueTeam,
        perGameReminders: [String: Set<GameReminderType>]
    ) -> String {
        let pCanon = CourtsideAnalytics.canonicalClubId(primaryTeam.id)
        let liveTracked = games.first {
            $0.status == .live && (
                !(perGameReminders[$0.id] ?? []).isEmpty ||
                CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == pCanon ||
                CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == pCanon
            )
        } ?? games.first { $0.status == .live }

        if let live = liveTracked {
            let qLabel = live.currentQuarter >= 5 ? "OT" : "Q\(live.currentQuarter)"
            return "🔴 \(live.homeTeam.code) \(live.homeScore):\(live.awayScore) \(live.awayTeam.code) (\(qLabel) \(live.remainingClock))"
        }

        if let nextClubGame = games.first(where: {
            $0.status == .upcoming && (
                CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == pCanon ||
                CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == pCanon
            )
        }) {
            return "🏀 \(nextClubGame.homeTeam.code) vs \(nextClubGame.awayTeam.code) • R\(nextClubGame.round)"
        }

        return "🏀 \(primaryTeam.code) \(primaryTeam.wins)-\(primaryTeam.losses)"
    }

    // MARK: - GitHub Releases Semantic Version Evaluator

    public static func compareSemanticVersions(_ lhs: String, _ rhs: String) -> Int {
        func parse(_ raw: String) -> (core: [Int], isPre: Bool, preNum: Int) {
            let clean = raw.trimmingCharacters(in: .whitespacesAndNewlines)
                .replacingOccurrences(of: "^[vV]", with: "", options: .regularExpression)
            let parts = clean.split(separator: "-", maxSplits: 1)
            let nums = parts.first?.split(separator: ".").map { Int($0) ?? 0 } ?? [0, 0, 0]
            let padded = nums + Array(repeating: 0, count: max(0, 3 - nums.count))
            if parts.count == 1 {
                return (padded, false, Int.max)
            }
            let preStr = String(parts[1])
            let trailingDigits = preStr.split(whereSeparator: { !$0.isNumber }).last.flatMap { Int($0) } ?? 0
            return (padded, true, trailingDigits)
        }

        let a = parse(lhs)
        let b = parse(rhs)
        for i in 0..<3 {
            if a.core[i] != b.core[i] { return a.core[i] > b.core[i] ? 1 : -1 }
        }
        if a.isPre != b.isPre {
            return a.isPre ? -1 : 1
        }
        if a.preNum != b.preNum {
            return a.preNum > b.preNum ? 1 : -1
        }
        return 0
    }

    public static func evaluateGitHubReleasesJson(
        rawData: Data,
        currentVersion: String = currentVersionName,
        includePrereleases: Bool
    ) -> AppReleaseCheckResult {
        struct GHAsset: Decodable {
            var name: String?
            var browser_download_url: String?
        }
        struct GHRelease: Decodable {
            var tag_name: String?
            var name: String?
            var html_url: String?
            var draft: Bool?
            var prerelease: Bool?
            var assets: [GHAsset]?
        }

        let timeFormatter = DateFormatter()
        timeFormatter.dateFormat = "HH:mm:ss"
        let checkedAt = timeFormatter.string(from: Date())

        let releases: [GHRelease]
        if let list = try? JSONDecoder().decode([GHRelease].self, from: rawData) {
            releases = list
        } else if let single = try? JSONDecoder().decode(GHRelease.self, from: rawData) {
            releases = [single]
        } else {
            return AppReleaseCheckResult(status: .error, lastCheckedTime: checkedAt, errorMessage: "Invalid release payload")
        }

        let candidates = releases.filter { rel in
            guard rel.draft != true else { return false }
            let isPre = (rel.prerelease == true) || ((rel.tag_name ?? "").contains("-"))
            return includePrereleases || !isPre
        }

        guard let best = candidates.max(by: {
            compareSemanticVersions($0.tag_name ?? "0.0.0", $1.tag_name ?? "0.0.0") < 0
        }) else {
            return AppReleaseCheckResult(status: .upToDate, latestVersionName: currentVersion, lastCheckedTime: checkedAt)
        }

        let tag = best.tag_name ?? currentVersion
        let cleanLatest = tag.replacingOccurrences(of: "^[vV]", with: "", options: .regularExpression)
        let macAsset = best.assets?.first {
            let n = ($0.name ?? "").lowercased()
            return n.hasSuffix(".dmg") || (n.contains("macos") && n.hasSuffix(".zip"))
        } ?? best.assets?.first

        if compareSemanticVersions(cleanLatest, currentVersion) > 0 {
            return AppReleaseCheckResult(
                status: .updateAvailable,
                latestVersionName: cleanLatest,
                releaseTitle: best.name ?? "EuroLeague Courtside \(tag)",
                htmlUrl: best.html_url ?? "https://github.com/petrosvassiliou/euroleague-courtside/releases",
                macDownloadUrl: macAsset?.browser_download_url ?? (best.html_url ?? ""),
                isPreRelease: (best.prerelease == true) || cleanLatest.contains("-"),
                lastCheckedTime: checkedAt
            )
        } else {
            return AppReleaseCheckResult(
                status: .upToDate,
                latestVersionName: currentVersion,
                releaseTitle: best.name ?? "EuroLeague Courtside v\(currentVersion)",
                htmlUrl: best.html_url ?? "https://github.com/petrosvassiliou/euroleague-courtside/releases",
                macDownloadUrl: macAsset?.browser_download_url ?? "",
                isPreRelease: cleanLatest.contains("-"),
                lastCheckedTime: checkedAt
            )
        }
    }
}
