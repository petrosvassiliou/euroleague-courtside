import Foundation

// MARK: - Core Enums & Theme / Language Modes

public enum AppLanguage: String, CaseIterable, Codable, Identifiable, Sendable {
    case en = "en"
    case de = "de"
    case fr = "fr"
    case it = "it"
    case es = "es"
    case el = "el"

    public var id: String { rawValue }

    public var flagEmoji: String {
        switch self {
        case .en: return "🇬🇧"
        case .de: return "🇩🇪"
        case .fr: return "🇫🇷"
        case .it: return "🇮🇹"
        case .es: return "🇪🇸"
        case .el: return "🇬🇷"
        }
    }

    public var nativeName: String {
        switch self {
        case .en: return "English"
        case .de: return "Deutsch"
        case .fr: return "Français"
        case .it: return "Italiano"
        case .es: return "Español"
        case .el: return "Ελληνικά"
        }
    }

    public static func fromCode(_ code: String?) -> AppLanguage {
        guard let c = code?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(), !c.isEmpty else {
            return .en
        }
        return AppLanguage.allCases.first { $0.rawValue == c || c.hasPrefix($0.rawValue) } ?? .en
    }
}

public enum AppThemeMode: String, CaseIterable, Codable, Identifiable, Sendable {
    case light = "light"
    case dark = "dark"

    public static let defaultMode: AppThemeMode = .light
    public var id: String { rawValue }
    public var isLight: Bool { self == .light }

    public func toggled() -> AppThemeMode {
        self == .light ? .dark : .light
    }

    public static func fromCode(_ code: String?) -> AppThemeMode {
        guard let c = code?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() else {
            return .light
        }
        return c == "dark" ? .dark : .light
    }
}

public enum GameStatus: String, Codable, CaseIterable, Sendable {
    case live = "LIVE"
    case upcoming = "UPCOMING"
    case final_ = "FINAL"

    public init(from decoder: Decoder) throws {
        let raw = (try? decoder.singleValueContainer().decode(String.self))?.uppercased() ?? "UPCOMING"
        switch raw {
        case "LIVE": self = .live
        case "FINAL": self = .final_
        default: self = .upcoming
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}

public enum InjuryStatus: String, Codable, CaseIterable, Sendable {
    case fit = "FIT"
    case questionable = "QUESTIONABLE"
    case out = "OUT"

    public init(from decoder: Decoder) throws {
        let raw = (try? decoder.singleValueContainer().decode(String.self))?.uppercased() ?? "FIT"
        switch raw {
        case "QUESTIONABLE", "GTD", "DOUBTFUL": self = .questionable
        case "OUT", "INJURED": self = .out
        default: self = .fit
        }
    }
}

public enum GameReminderType: String, CaseIterable, Codable, Hashable, Identifiable, Sendable {
    case q1Result = "Q1_RESULT"
    case halftimeResult = "HALFTIME_RESULT"
    case q3Result = "Q3_RESULT"
    case crunchTime = "CRUNCH_TIME"
    case finalScore = "FINAL_SCORE"

    public var id: String { rawValue }

    public var shortBadge: String {
        switch self {
        case .q1Result: return "Q1"
        case .halftimeResult: return "HT"
        case .q3Result: return "Q3"
        case .crunchTime: return "🔥 Q4"
        case .finalScore: return "FT"
        }
    }
}

public enum PlayEventCategory: String, Codable, CaseIterable, Sendable {
    case threePt = "THREE_PT"
    case dunk = "DUNK"
    case twoPt = "TWO_PT"
    case freeThrow = "FREE_THROW"
    case miss = "MISS"
    case assistReb = "ASSIST_REB"
    case stealBlk = "STEAL_BLK"
    case turnover = "TURNOVER"
    case foul = "FOUL"
    case timeout = "TIMEOUT"
    case period = "PERIOD"

    public var badgeLabel: String {
        switch self {
        case .threePt: return "3PT"
        case .dunk: return "DUNK"
        case .twoPt: return "2PT"
        case .freeThrow: return "FT"
        case .miss: return "MISS"
        case .assistReb: return "AST/REB"
        case .stealBlk: return "STL/BLK"
        case .turnover: return "TO"
        case .foul: return "FOUL"
        case .timeout: return "TIMEOUT"
        case .period: return "PERIOD"
        }
    }
}

// MARK: - Domain Models

public struct EuroLeagueTeam: Identifiable, Codable, Hashable, Sendable {
    public var id: String
    public var code: String
    public var name: String
    public var city: String
    public var country: String
    public var arena: String
    public var logoUrl: String
    public var localLogoResName: String
    public var primaryColorHex: UInt64
    public var secondaryColorHex: UInt64
    public var accentColorHex: UInt64
    public var wins: Int
    public var losses: Int
    public var pointsFor: Int
    public var pointsAgainst: Int
    public var totalPointsFor: Int?
    public var totalPointsAgainst: Int?
    public var streak: [String]
    public var offensiveRating: Double
    public var defensiveRating: Double
    public var pace: Double
    public var threePointPct: Double
    public var twoPointPct: Double
    public var reboundRate: Double

    public var gamesPlayed: Int { wins + losses }
    public var pointDiff: Int { pointsFor - pointsAgainst }
    public var winPct: Double {
        gamesPlayed > 0 ? Double(wins) / Double(gamesPlayed) : 0.0
    }
    public var ppg: Double {
        let total = totalPointsFor ?? pointsFor
        return gamesPlayed > 0 ? Double(total) / Double(gamesPlayed) : 0.0
    }
    public var oppPpg: Double {
        let total = totalPointsAgainst ?? pointsAgainst
        return gamesPlayed > 0 ? Double(total) / Double(gamesPlayed) : 0.0
    }

    public init(
        id: String, code: String, name: String, city: String, country: String, arena: String,
        logoUrl: String = "", localLogoResName: String = "",
        primaryColorHex: UInt64 = 0xFFFF6600, secondaryColorHex: UInt64 = 0xFF1E293B, accentColorHex: UInt64 = 0xFFFFFFFF,
        wins: Int = 0, losses: Int = 0, pointsFor: Int = 0, pointsAgainst: Int = 0,
        totalPointsFor: Int? = nil, totalPointsAgainst: Int? = nil,
        streak: [String] = [],
        offensiveRating: Double = 114.0, defensiveRating: Double = 112.0, pace: Double = 72.0,
        threePointPct: Double = 36.5, twoPointPct: Double = 54.0, reboundRate: Double = 50.0
    ) {
        self.id = id
        self.code = code
        self.name = name
        self.city = city
        self.country = country
        self.arena = arena
        self.logoUrl = logoUrl
        self.localLogoResName = localLogoResName
        self.primaryColorHex = primaryColorHex
        self.secondaryColorHex = secondaryColorHex
        self.accentColorHex = accentColorHex
        self.wins = wins
        self.losses = losses
        self.pointsFor = pointsFor
        self.pointsAgainst = pointsAgainst
        self.totalPointsFor = totalPointsFor
        self.totalPointsAgainst = totalPointsAgainst
        self.streak = CourtsideAnalytics.normalizeStreak(streak)
        self.offensiveRating = offensiveRating
        self.defensiveRating = defensiveRating
        self.pace = pace
        self.threePointPct = threePointPct
        self.twoPointPct = twoPointPct
        self.reboundRate = reboundRate
    }
}

public struct PlayerProfile: Identifiable, Codable, Hashable, Sendable {
    public var id: String
    public var teamId: String
    public var number: Int
    public var name: String
    public var position: String
    public var nationality: String
    public var heightCm: Int
    public var ppg: Double
    public var rpg: Double
    public var apg: Double
    public var pir: Double
    public var injuryStatus: InjuryStatus
    public var injuryDetail: String?
    public var headshotUrl: String

    public init(
        id: String, teamId: String, number: Int, name: String, position: String,
        nationality: String, heightCm: Int, ppg: Double, rpg: Double, apg: Double, pir: Double,
        injuryStatus: InjuryStatus = .fit, injuryDetail: String? = nil, headshotUrl: String = ""
    ) {
        self.id = id
        self.teamId = teamId
        self.number = number
        self.name = CourtsideAnalytics.cleanPlayerDisplayName(name)
        self.position = position
        self.nationality = nationality
        self.heightCm = heightCm
        self.ppg = ppg
        self.rpg = rpg
        self.apg = apg
        self.pir = pir
        self.injuryStatus = injuryStatus
        self.injuryDetail = injuryDetail
        self.headshotUrl = headshotUrl
    }
}

public struct QuarterScores: Codable, Hashable, Sendable {
    public var q1Home: Int
    public var q1Away: Int
    public var q2Home: Int
    public var q2Away: Int
    public var q3Home: Int
    public var q3Away: Int
    public var q4Home: Int
    public var q4Away: Int
    public var otHome: Int?
    public var otAway: Int?

    public init(
        q1Home: Int = 0, q1Away: Int = 0,
        q2Home: Int = 0, q2Away: Int = 0,
        q3Home: Int = 0, q3Away: Int = 0,
        q4Home: Int = 0, q4Away: Int = 0,
        otHome: Int? = nil, otAway: Int? = nil
    ) {
        self.q1Home = q1Home
        self.q1Away = q1Away
        self.q2Home = q2Home
        self.q2Away = q2Away
        self.q3Home = q3Home
        self.q3Away = q3Away
        self.q4Home = q4Home
        self.q4Away = q4Away
        self.otHome = otHome
        self.otAway = otAway
    }
}

public struct PlayerBoxscoreStat: Identifiable, Codable, Hashable, Sendable {
    public var id: String { "\(teamId)_\(playerId)_\(playerName)" }
    public var playerId: String
    public var playerName: String
    public var jerseyNumber: Int
    public var teamId: String
    public var minutes: String
    public var points: Int
    public var offRebounds: Int
    public var defRebounds: Int
    public var assists: Int
    public var steals: Int
    public var blocks: Int
    public var blocksAgainst: Int
    public var turnovers: Int
    public var twoPtMade: Int
    public var twoPtAttempted: Int
    public var threePtMade: Int
    public var threePtAttempted: Int
    public var ftMade: Int
    public var ftAttempted: Int
    public var foulsDrawn: Int
    public var foulsCommitted: Int
    public var valuationOverride: Int?

    public var totalRebounds: Int { offRebounds + defRebounds }

    /// Official EuroLeague Performance Index Rating (PIR):
    /// `(PTS + REB + AST + STL + BLK + FoulsDrawn) - (MissedFG + MissedFT + TO + FoulsCommitted + BlocksAgainst)`
    public var pir: Int {
        if let overrideVal = valuationOverride {
            return overrideVal
        }
        let missedTwo = max(0, twoPtAttempted - twoPtMade)
        let missedThree = max(0, threePtAttempted - threePtMade)
        let missedFt = max(0, ftAttempted - ftMade)
        let positive = points + totalRebounds + assists + steals + blocks + foulsDrawn
        let negative = missedTwo + missedThree + missedFt + turnovers + foulsCommitted + blocksAgainst
        return positive - negative
    }

    public init(
        playerId: String, playerName: String, jerseyNumber: Int, teamId: String, minutes: String,
        points: Int, offRebounds: Int, defRebounds: Int, assists: Int, steals: Int, blocks: Int,
        blocksAgainst: Int = 0, turnovers: Int, twoPtMade: Int, twoPtAttempted: Int,
        threePtMade: Int, threePtAttempted: Int, ftMade: Int, ftAttempted: Int,
        foulsDrawn: Int, foulsCommitted: Int, valuationOverride: Int? = nil
    ) {
        self.playerId = playerId
        self.playerName = CourtsideAnalytics.cleanPlayerDisplayName(playerName)
        self.jerseyNumber = jerseyNumber
        self.teamId = teamId
        self.minutes = minutes
        self.points = points
        self.offRebounds = offRebounds
        self.defRebounds = defRebounds
        self.assists = assists
        self.steals = steals
        self.blocks = blocks
        self.blocksAgainst = blocksAgainst
        self.turnovers = turnovers
        self.twoPtMade = twoPtMade
        self.twoPtAttempted = twoPtAttempted
        self.threePtMade = threePtMade
        self.threePtAttempted = threePtAttempted
        self.ftMade = ftMade
        self.ftAttempted = ftAttempted
        self.foulsDrawn = foulsDrawn
        self.foulsCommitted = foulsCommitted
        self.valuationOverride = valuationOverride
    }

    enum CodingKeys: String, CodingKey {
        case playerId, playerName, jerseyNumber, teamId, minutes, points
        case offRebounds, defRebounds, assists, steals, blocks, blocksAgainst, turnovers
        case twoPtMade, twoPtAttempted, threePtMade, threePtAttempted
        case ftMade, ftAttempted, foulsDrawn, foulsCommitted, valuationOverride
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.playerId = (try? c.decode(String.self, forKey: .playerId)) ?? ""
        let rawName = (try? c.decode(String.self, forKey: .playerName)) ?? ""
        self.playerName = CourtsideAnalytics.cleanPlayerDisplayName(rawName)
        self.jerseyNumber = (try? c.decode(Int.self, forKey: .jerseyNumber)) ?? 0
        self.teamId = (try? c.decode(String.self, forKey: .teamId)) ?? ""
        self.minutes = (try? c.decode(String.self, forKey: .minutes)) ?? "00:00"
        self.points = (try? c.decode(Int.self, forKey: .points)) ?? 0
        self.offRebounds = (try? c.decode(Int.self, forKey: .offRebounds)) ?? 0
        self.defRebounds = (try? c.decode(Int.self, forKey: .defRebounds)) ?? 0
        self.assists = (try? c.decode(Int.self, forKey: .assists)) ?? 0
        self.steals = (try? c.decode(Int.self, forKey: .steals)) ?? 0
        self.blocks = (try? c.decode(Int.self, forKey: .blocks)) ?? 0
        self.blocksAgainst = (try? c.decode(Int.self, forKey: .blocksAgainst)) ?? 0
        self.turnovers = (try? c.decode(Int.self, forKey: .turnovers)) ?? 0
        self.twoPtMade = (try? c.decode(Int.self, forKey: .twoPtMade)) ?? 0
        self.twoPtAttempted = (try? c.decode(Int.self, forKey: .twoPtAttempted)) ?? 0
        self.threePtMade = (try? c.decode(Int.self, forKey: .threePtMade)) ?? 0
        self.threePtAttempted = (try? c.decode(Int.self, forKey: .threePtAttempted)) ?? 0
        self.ftMade = (try? c.decode(Int.self, forKey: .ftMade)) ?? 0
        self.ftAttempted = (try? c.decode(Int.self, forKey: .ftAttempted)) ?? 0
        self.foulsDrawn = (try? c.decode(Int.self, forKey: .foulsDrawn)) ?? 0
        self.foulsCommitted = (try? c.decode(Int.self, forKey: .foulsCommitted)) ?? 0
        self.valuationOverride = try? c.decodeIfPresent(Int.self, forKey: .valuationOverride)
    }
}

public struct ShotAttempt: Identifiable, Codable, Hashable, Sendable {
    public var id: String
    public var playerId: String
    public var playerName: String
    public var teamId: String
    /// Normalized FIBA half-court X (`0.0 ... 1.0`, `0.5` = center basket)
    public var courtX: Double
    /// Normalized FIBA half-court Y (`0.0 ... 1.0`, `0.0` = baseline, `1.0` = half-court)
    public var courtY: Double
    public var isMade: Bool
    public var isThreePointer: Bool
    public var quarter: Int

    public init(
        id: String, playerId: String, playerName: String, teamId: String,
        courtX: Double, courtY: Double, isMade: Bool, isThreePointer: Bool, quarter: Int
    ) {
        self.id = id
        self.playerId = playerId
        self.playerName = CourtsideAnalytics.cleanPlayerDisplayName(playerName)
        self.teamId = teamId
        self.courtX = min(max(courtX, 0.04), 0.96)
        self.courtY = min(max(courtY, 0.04), 0.95)
        self.isMade = isMade
        self.isThreePointer = isThreePointer
        self.quarter = quarter
    }

    enum CodingKeys: String, CodingKey {
        case id, playerId, playerName, teamId, courtX, courtY, isMade, made, isThreePointer, quarter
    }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.id = (try? c.decode(String.self, forKey: .id)) ?? UUID().uuidString
        self.playerId = (try? c.decode(String.self, forKey: .playerId)) ?? ""
        let rawName = (try? c.decode(String.self, forKey: .playerName)) ?? ""
        self.playerName = CourtsideAnalytics.cleanPlayerDisplayName(rawName)
        self.teamId = (try? c.decode(String.self, forKey: .teamId)) ?? ""
        let rx = (try? c.decode(Double.self, forKey: .courtX)) ?? 0.5
        let ry = (try? c.decode(Double.self, forKey: .courtY)) ?? 0.25
        self.courtX = min(max(rx, 0.04), 0.96)
        self.courtY = min(max(ry, 0.04), 0.95)
        if let m = try? c.decode(Bool.self, forKey: .isMade) {
            self.isMade = m
        } else {
            self.isMade = (try? c.decode(Bool.self, forKey: .made)) ?? false
        }
        self.isThreePointer = (try? c.decode(Bool.self, forKey: .isThreePointer)) ?? false
        self.quarter = (try? c.decode(Int.self, forKey: .quarter)) ?? 1
    }

    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(playerId, forKey: .playerId)
        try c.encode(playerName, forKey: .playerName)
        try c.encode(teamId, forKey: .teamId)
        try c.encode(courtX, forKey: .courtX)
        try c.encode(courtY, forKey: .courtY)
        try c.encode(isMade, forKey: .isMade)
        try c.encode(isThreePointer, forKey: .isThreePointer)
        try c.encode(quarter, forKey: .quarter)
    }
}

public struct PlayByPlayEvent: Identifiable, Codable, Hashable, Sendable {
    public var id: String
    public var playNumber: Int
    public var quarter: Int
    public var clock: String
    public var gameMinute: Double
    public var teamId: String
    public var playerId: String
    public var playerName: String
    public var jerseyNumber: Int?
    public var playType: String
    public var category: PlayEventCategory
    public var description: String
    public var homeScore: Int
    public var awayScore: Int
    public var pointsScored: Int

    public var isClutchPlay: Bool {
        quarter >= 4 && gameMinute >= 37.0 && abs(homeScore - awayScore) <= 5
    }

    public init(
        id: String, playNumber: Int, quarter: Int, clock: String, gameMinute: Double,
        teamId: String, playerId: String, playerName: String, jerseyNumber: Int? = nil,
        playType: String, category: PlayEventCategory, description: String,
        homeScore: Int, awayScore: Int, pointsScored: Int
    ) {
        self.id = id
        self.playNumber = playNumber
        self.quarter = quarter
        self.clock = clock
        self.gameMinute = gameMinute
        self.teamId = teamId
        self.playerId = playerId
        self.playerName = CourtsideAnalytics.cleanPlayerDisplayName(playerName)
        self.jerseyNumber = jerseyNumber
        self.playType = playType
        self.category = category
        self.description = description
        self.homeScore = homeScore
        self.awayScore = awayScore
        self.pointsScored = pointsScored
    }
}

public struct EuroLeagueGame: Identifiable, Codable, Hashable, Sendable {
    public var id: String
    public var gameCode: Int
    public var seasonCode: String
    public var round: Int
    public var homeTeam: EuroLeagueTeam
    public var awayTeam: EuroLeagueTeam
    public var venueName: String
    public var tipOffEpochMillis: Int64
    public var status: GameStatus
    public var currentQuarter: Int
    public var remainingClock: String
    public var homeScore: Int
    public var awayScore: Int
    public var quarters: QuarterScores
    public var headToHeadHomeWins: Int
    public var headToHeadAwayWins: Int
    public var boxscoreHome: [PlayerBoxscoreStat]
    public var boxscoreAway: [PlayerBoxscoreStat]
    public var shots: [ShotAttempt]
    public var playByPlay: [PlayByPlayEvent]

    /// Crunch-Time Close-Game detection: LIVE, Q4/OT (`currentQuarter >= 4`), `<= 3:00` remaining, diff `<= 5`.
    public var isCloseGameFourthQuarter: Bool {
        guard status == .live, currentQuarter >= 4 else { return false }
        let diff = abs(homeScore - awayScore)
        guard diff <= 5 else { return false }
        let parts = remainingClock.split(separator: ":")
        let mins = Int(parts.first ?? "99") ?? 99
        let secs = parts.count > 1 ? (Int(parts[1]) ?? 0) : 0
        let totalSecs = mins * 60 + secs
        return totalSecs <= 180
    }

    public init(
        id: String, gameCode: Int, seasonCode: String = "E2026", round: Int,
        homeTeam: EuroLeagueTeam, awayTeam: EuroLeagueTeam, venueName: String = "",
        tipOffEpochMillis: Int64, status: GameStatus, currentQuarter: Int = 1,
        remainingClock: String = "10:00", homeScore: Int = 0, awayScore: Int = 0,
        quarters: QuarterScores = QuarterScores(),
        headToHeadHomeWins: Int = 0, headToHeadAwayWins: Int = 0,
        boxscoreHome: [PlayerBoxscoreStat] = [], boxscoreAway: [PlayerBoxscoreStat] = [],
        shots: [ShotAttempt] = [], playByPlay: [PlayByPlayEvent] = []
    ) {
        self.id = id
        self.gameCode = gameCode
        self.seasonCode = seasonCode
        self.round = round
        self.homeTeam = homeTeam
        self.awayTeam = awayTeam
        self.venueName = venueName.isEmpty ? homeTeam.arena : venueName
        self.tipOffEpochMillis = tipOffEpochMillis
        self.status = status
        self.currentQuarter = currentQuarter
        self.remainingClock = remainingClock
        self.homeScore = homeScore
        self.awayScore = awayScore
        self.quarters = quarters
        self.headToHeadHomeWins = headToHeadHomeWins
        self.headToHeadAwayWins = headToHeadAwayWins
        self.boxscoreHome = boxscoreHome
        self.boxscoreAway = boxscoreAway
        self.shots = shots
        self.playByPlay = playByPlay
    }
}

public struct LeadMomentumPoint: Identifiable, Hashable, Sendable {
    public var id: Int
    public var minute: Double
    /// Positive = home team leads; Negative = away team leads
    public var homeLead: Int
}

public struct LeadMomentumSummary: Hashable, Sendable {
    public var points: [LeadMomentumPoint]
    public var maxHomeLead: Int
    public var maxAwayLead: Int
    public var leadChanges: Int
}

public struct PlayerGameLogEntry: Identifiable, Hashable, Sendable {
    public var id: String { gameId }
    public var gameId: String
    public var round: Int
    public var opponentCode: String
    public var isHome: Bool
    public var teamWon: Bool
    public var scoreText: String
    public var minutes: String
    public var points: Int
    public var rebounds: Int
    public var assists: Int
    public var pir: Int
    public var twoPtMade: Int
    public var twoPtAttempted: Int
    public var threePtMade: Int
    public var threePtAttempted: Int
    public var ftMade: Int
    public var ftAttempted: Int
}

public struct PlayerScoutingSummary: Hashable, Sendable {
    public var gamesLogged: Int
    public var twoPtPct: Double
    public var threePtPct: Double
    public var ftPct: Double
    public var avgMinutes: Double
    public var gameLog: [PlayerGameLogEntry]
}

public struct PlayoffSeriesProjection: Identifiable, Hashable, Sendable {
    public var id: String
    public var seriesLabel: String
    public var highSeedRank: Int
    public var lowSeedRank: Int
    public var homeTeam: EuroLeagueTeam
    public var awayTeam: EuroLeagueTeam
    public var homeWins: Int
    public var awayWins: Int
}

// MARK: - Analytics, Standings, Scouting & RFC 5545 .ics Helpers

public enum CourtsideAnalytics {

    public static func canonicalClubId(_ raw: String) -> String {
        let up = raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        switch up {
        case "BAY", "MUN": return "MUN"
        case "PAO", "PAN": return "PAN"
        case "RMB", "MAD": return "MAD"
        case "FCB", "BAR": return "BAR"
        case "FBB", "ULK": return "ULK"
        case "ASM", "MCO": return "MCO"
        case "EFS", "IST": return "IST"
        case "ALB", "BER": return "BER"
        case "EA7", "MIL": return "MIL"
        case "MAC", "TEL": return "TEL"
        case "BKN", "BAS": return "BAS"
        case "CZV", "RED": return "RED"
        case "VAL", "PAM": return "PAM"
        default: return up
        }
    }

    public static func cleanPlayerDisplayName(_ raw: String) -> String {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return "" }
        // Remove stray passport middle-name commas like "Evan , Mehdi Fournier" -> "Evan Fournier"
        if trimmed.contains(" , ") {
            let parts = trimmed.components(separatedBy: " , ")
            let first = parts.first?.trimmingCharacters(in: .whitespaces) ?? ""
            let restWords = (parts.dropFirst().joined(separator: " ")).split(separator: " ")
            if let last = restWords.last {
                return "\(first) \(last)"
            }
        }
        return trimmed.replacingOccurrences(of: "  ", with: " ")
    }

    public static func normalizeStreak(_ raw: [String]) -> [String] {
        raw.compactMap { token in
            let u = token.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            if u == "W" || u == "S" { return "W" }
            if u == "L" || u == "N" { return "L" }
            return nil
        }
    }

    /// Official EuroLeague Standings:
    /// Excludes overtime points from regulation `PF`/`PA` and `Diff`, while keeping OT in total `PPG`,
    /// and sorts by Win%, Head-to-Head record (when all H2H games played), and regulation point differential.
    public static func recomputeAndSortStandings(
        teams: [EuroLeagueTeam],
        games: [EuroLeagueGame]
    ) -> [EuroLeagueTeam] {
        let finalGames = games
            .filter { $0.status == .final_ && ($0.homeScore > 0 || $0.awayScore > 0) }
            .sorted { ($0.round, $0.tipOffEpochMillis) < ($1.round, $1.tipOffEpochMillis) }

        guard !finalGames.isEmpty else {
            return teams.sorted {
                if abs($0.winPct - $1.winPct) > 0.0001 { return $0.winPct > $1.winPct }
                if $0.pointDiff != $1.pointDiff { return $0.pointDiff > $1.pointDiff }
                return $0.pointsFor > $1.pointsFor
            }
        }

        var updated: [EuroLeagueTeam] = teams.map { team in
            let canon = canonicalClubId(team.id)
            let clubGames = finalGames.filter {
                canonicalClubId($0.homeTeam.id) == canon || canonicalClubId($0.awayTeam.id) == canon
            }
            guard !clubGames.isEmpty else { return team }
            var w = 0, l = 0, regPF = 0, regPA = 0, totPF = 0, totPA = 0
            var results: [String] = []

            for g in clubGames {
                let isHome = canonicalClubId(g.homeTeam.id) == canon
                let myTotal = isHome ? g.homeScore : g.awayScore
                let oppTotal = isHome ? g.awayScore : g.homeScore
                let otH = g.quarters.otHome ?? 0
                let otA = g.quarters.otAway ?? 0
                let hasOt = (otH + otA) > 0 || g.currentQuarter >= 5
                let regH: Int
                let regA: Int
                if hasOt && (g.quarters.q1Home + g.quarters.q2Home + g.quarters.q3Home + g.quarters.q4Home) > 0 {
                    regH = g.quarters.q1Home + g.quarters.q2Home + g.quarters.q3Home + g.quarters.q4Home
                    regA = g.quarters.q1Away + g.quarters.q2Away + g.quarters.q3Away + g.quarters.q4Away
                } else if hasOt && (otH + otA) > 0 {
                    regH = max(0, g.homeScore - otH)
                    regA = max(0, g.awayScore - otA)
                } else {
                    regH = g.homeScore
                    regA = g.awayScore
                }
                let myReg = isHome ? regH : regA
                let oppReg = isHome ? regA : regH

                if myTotal > oppTotal {
                    w += 1
                    results.append("W")
                } else {
                    l += 1
                    results.append("L")
                }
                regPF += myReg
                regPA += oppReg
                totPF += myTotal
                totPA += oppTotal
            }

            var copy = team
            copy.wins = w
            copy.losses = l
            copy.pointsFor = regPF
            copy.pointsAgainst = regPA
            copy.totalPointsFor = totPF
            copy.totalPointsAgainst = totPA
            copy.streak = Array(results.suffix(5))
            return copy
        }

        updated.sort { a, b in
            if abs(a.winPct - b.winPct) > 0.0001 {
                return a.winPct > b.winPct
            }
            if a.wins != b.wins {
                return a.wins > b.wins
            }
            if a.pointDiff != b.pointDiff {
                return a.pointDiff > b.pointDiff
            }
            return a.pointsFor > b.pointsFor
        }
        return updated
    }

    public static func computeLeadMomentum(game: EuroLeagueGame) -> LeadMomentumSummary {
        if !game.playByPlay.isEmpty {
            var pts: [LeadMomentumPoint] = [LeadMomentumPoint(id: 0, minute: 0, homeLead: 0)]
            var maxH = 0
            var maxA = 0
            var changes = 0
            var prevSign = 0
            for (idx, ev) in game.playByPlay.enumerated() {
                let lead = ev.homeScore - ev.awayScore
                maxH = max(maxH, lead)
                maxA = max(maxA, -lead)
                let sign = lead > 0 ? 1 : (lead < 0 ? -1 : 0)
                if sign != 0 && prevSign != 0 && sign != prevSign {
                    changes += 1
                }
                if sign != 0 { prevSign = sign }
                pts.append(LeadMomentumPoint(id: idx + 1, minute: ev.gameMinute, homeLead: lead))
            }
            return LeadMomentumSummary(points: pts, maxHomeLead: maxH, maxAwayLead: maxA, leadChanges: changes)
        }

        // Synthesize from quarter partials when full play-by-play is not yet loaded
        let q = game.quarters
        let c1 = (q.q1Home - q.q1Away)
        let c2 = c1 + (q.q2Home - q.q2Away)
        let c3 = c2 + (q.q3Home - q.q3Away)
        let c4 = game.homeScore - game.awayScore
        let rawLeads = [(0.0, 0), (10.0, c1), (20.0, c2), (30.0, c3), (40.0, c4)]
        var pts: [LeadMomentumPoint] = []
        var maxH = 0, maxA = 0, changes = 0, prevSign = 0
        for (i, item) in rawLeads.enumerated() {
            let lead = item.1
            maxH = max(maxH, lead)
            maxA = max(maxA, -lead)
            let sign = lead > 0 ? 1 : (lead < 0 ? -1 : 0)
            if sign != 0 && prevSign != 0 && sign != prevSign { changes += 1 }
            if sign != 0 { prevSign = sign }
            pts.append(LeadMomentumPoint(id: i, minute: item.0, homeLead: lead))
        }
        return LeadMomentumSummary(points: pts, maxHomeLead: maxH, maxAwayLead: maxA, leadChanges: changes)
    }

    public static func buildPlayerScoutingSummary(
        player: PlayerProfile,
        games: [EuroLeagueGame]
    ) -> PlayerScoutingSummary {
        let pCanonTeam = canonicalClubId(player.teamId)
        let normTargetName = player.name.lowercased()
        var entries: [PlayerGameLogEntry] = []
        var m2 = 0, a2 = 0, m3 = 0, a3 = 0, mFt = 0, aFt = 0
        var totalMins = 0.0

        for g in games where (g.status == .final_ || g.status == .live) {
            let isHome = canonicalClubId(g.homeTeam.id) == pCanonTeam
            let isAway = canonicalClubId(g.awayTeam.id) == pCanonTeam
            guard isHome || isAway else { continue }
            let box = isHome ? g.boxscoreHome : g.boxscoreAway
            guard let row = box.first(where: { candidate in
                (!candidate.playerId.isEmpty && candidate.playerId == player.id) ||
                candidate.playerName.lowercased() == normTargetName ||
                (candidate.jerseyNumber == player.number && normTargetName.split(separator: " ").last.map { $0.count > 2 && rowMatchesLastName(candidate.playerName, String($0)) } == true)
            }) else { continue }

            m2 += row.twoPtMade; a2 += row.twoPtAttempted
            m3 += row.threePtMade; a3 += row.threePtAttempted
            mFt += row.ftMade; aFt += row.ftAttempted
            let minParts = row.minutes.split(separator: ":")
            let mins = Double(minParts.first ?? "0") ?? 0
            let secs = minParts.count > 1 ? (Double(minParts[1]) ?? 0) : 0
            totalMins += mins + secs / 60.0

            let opp = isHome ? g.awayTeam.code : g.homeTeam.code
            let won = isHome ? (g.homeScore >= g.awayScore) : (g.awayScore >= g.homeScore)
            entries.append(
                PlayerGameLogEntry(
                    gameId: g.id,
                    round: g.round,
                    opponentCode: opp,
                    isHome: isHome,
                    teamWon: won,
                    scoreText: "\(g.homeScore):\(g.awayScore)",
                    minutes: row.minutes,
                    points: row.points,
                    rebounds: row.totalRebounds,
                    assists: row.assists,
                    pir: row.pir,
                    twoPtMade: row.twoPtMade,
                    twoPtAttempted: row.twoPtAttempted,
                    threePtMade: row.threePtMade,
                    threePtAttempted: row.threePtAttempted,
                    ftMade: row.ftMade,
                    ftAttempted: row.ftAttempted
                )
            )
        }

        entries.sort { $0.round > $1.round }
        let twoPct = a2 > 0 ? (Double(m2) * 100.0 / Double(a2)) : 54.0
        let threePct = a3 > 0 ? (Double(m3) * 100.0 / Double(a3)) : 37.0
        let ftPct = aFt > 0 ? (Double(mFt) * 100.0 / Double(aFt)) : 82.0
        let avgMin = entries.isEmpty ? 22.5 : (totalMins / Double(entries.count))

        return PlayerScoutingSummary(
            gamesLogged: entries.count,
            twoPtPct: twoPct,
            threePtPct: threePct,
            ftPct: ftPct,
            avgMinutes: avgMin,
            gameLog: entries
        )
    }

    private static func rowMatchesLastName(_ candidate: String, _ lastName: String) -> Bool {
        candidate.lowercased().contains(lastName.lowercased())
    }

    public static func resolveTeamLastAndUpcomingGames(
        team: EuroLeagueTeam,
        games: [EuroLeagueGame]
    ) -> (last3: [EuroLeagueGame], next3: [EuroLeagueGame]) {
        let target = canonicalClubId(team.id)
        let teamGames = games.filter {
            canonicalClubId($0.homeTeam.id) == target || canonicalClubId($0.awayTeam.id) == target
        }
        let last3 = teamGames
            .filter { $0.status == .final_ }
            .sorted { $0.round > $1.round }
            .prefix(3)
        let next3 = teamGames
            .filter { $0.status == .live || $0.status == .upcoming }
            .sorted { $0.round < $1.round }
            .prefix(3)
        return (Array(last3), Array(next3))
    }

    public static func buildPlayoffBracket(
        standings: [EuroLeagueTeam],
        games: [EuroLeagueGame]
    ) -> [PlayoffSeriesProjection] {
        guard standings.count >= 8 else { return [] }
        let pairs = [(0, 7, "QF 1"), (1, 6, "QF 2"), (2, 5, "QF 3"), (3, 4, "QF 4")]
        return pairs.map { (hiIdx, loIdx, label) in
            let hi = standings[hiIdx]
            let lo = standings[loIdx]
            let hiCanon = canonicalClubId(hi.id)
            let loCanon = canonicalClubId(lo.id)
            var hiW = 0, loW = 0
            for g in games where g.status == .final_ {
                let h = canonicalClubId(g.homeTeam.id)
                let a = canonicalClubId(g.awayTeam.id)
                if (h == hiCanon && a == loCanon) || (h == loCanon && a == hiCanon) {
                    let hiWon = (h == hiCanon && g.homeScore > g.awayScore) || (a == hiCanon && g.awayScore > g.homeScore)
                    if hiWon { hiW += 1 } else { loW += 1 }
                }
            }
            return PlayoffSeriesProjection(
                id: "\(hi.code)_\(lo.code)",
                seriesLabel: label,
                highSeedRank: hiIdx + 1,
                lowSeedRank: loIdx + 1,
                homeTeam: hi,
                awayTeam: lo,
                homeWins: min(hiW, 2),
                awayWins: min(loW, 2)
            )
        }
    }

    /// Generates an RFC 5545 compliant `.ics` calendar file with CRLF (`\r\n`) line endings and a 30-minute `VALARM`.
    public static func buildRfc5545IcsPayload(
        team: EuroLeagueTeam,
        games: [EuroLeagueGame]
    ) -> String {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = TimeZone(secondsFromGMT: 0)
        fmt.dateFormat = "yyyyMMdd'T'HHmmss'Z'"

        let stamp = fmt.string(from: Date())
        var lines: [String] = [
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//EuroLeague Courtside 2.0//macOS Liquid Glass//EN",
            "CALSCALE:GREGORIAN",
            "METHOD:PUBLISH",
            "X-WR-CALNAME:EuroLeague • \(escapeIcs(team.name))"
        ]

        let sorted = games.sorted { $0.round < $1.round }
        for g in sorted {
            let start = Date(timeIntervalSince1970: TimeInterval(g.tipOffEpochMillis) / 1000.0)
            let end = start.addingTimeInterval(2 * 3600 + 15 * 60)
            let loc = "\(g.homeTeam.arena), \(g.homeTeam.city)"
            let summary = "🏀 R\(g.round): \(g.homeTeam.name) vs \(g.awayTeam.name)"
            let desc = g.status == .final_
                ? "Final Score: \(g.homeTeam.code) \(g.homeScore) : \(g.awayScore) \(g.awayTeam.code)"
                : "EuroLeague Round \(g.round) • Courtside 2.0"

            lines.append("BEGIN:VEVENT")
            lines.append("UID:\(g.id)@courtside.euroleague.app")
            lines.append("DTSTAMP:\(stamp)")
            lines.append("DTSTART:\(fmt.string(from: start))")
            lines.append("DTEND:\(fmt.string(from: end))")
            lines.append("SUMMARY:\(escapeIcs(summary))")
            lines.append("LOCATION:\(escapeIcs(loc))")
            lines.append("DESCRIPTION:\(escapeIcs(desc))")
            lines.append("BEGIN:VALARM")
            lines.append("TRIGGER:-PT30M")
            lines.append("ACTION:DISPLAY")
            lines.append("DESCRIPTION:Tip-Off in 30 minutes: \(escapeIcs(g.homeTeam.code)) vs \(escapeIcs(g.awayTeam.code))")
            lines.append("END:VALARM")
            lines.append("END:VEVENT")
        }

        lines.append("END:VCALENDAR")
        return lines.joined(separator: "\r\n") + "\r\n"
    }

    private static func escapeIcs(_ raw: String) -> String {
        raw.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: ";", with: "\\;")
            .replacingOccurrences(of: ",", with: "\\,")
            .replacingOccurrences(of: "\n", with: "\\n")
    }
}
