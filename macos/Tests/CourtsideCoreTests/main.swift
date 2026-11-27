import Foundation
import CourtsideCore

@main
struct CourtsideMacTestsRunner {
    static func check(_ condition: @autoclosure () -> Bool, _ message: String, file: String = #fileID, line: Int = #line) {
        if !condition() {
            fputs("❌ Test failure at \(file):\(line) — \(message)\n", stderr)
            exit(1)
        }
    }

    static func main() throws {
        print("🧪 Running EuroLeague Courtside 2.0 macOS Liquid Glass Test Suite…")

        // 1. Official EuroLeague PIR calculation
        let edwards = PlayerBoxscoreStat(
            playerId: "p5", playerName: "Carsen Edwards", jerseyNumber: 3, teamId: "MUN",
            minutes: "31:42", points: 26, offRebounds: 1, defRebounds: 3, assists: 5,
            steals: 2, blocks: 0, turnovers: 2, twoPtMade: 5, twoPtAttempted: 9,
            threePtMade: 4, threePtAttempted: 9, ftMade: 4, ftAttempted: 4,
            foulsDrawn: 5, foulsCommitted: 2
        )
        check(edwards.pir == 29, "Expected PIR 29, got \(edwards.pir)")
        check(edwards.totalRebounds == 4, "Expected 4 total rebounds")

        // 2. Passport comma cleanup & Light Mode default
        check(CourtsideAnalytics.cleanPlayerDisplayName("Evan , Mehdi Fournier") == "Evan Fournier", "Name comma cleanup failed")
        check(AppThemeMode.defaultMode == .light, "Light Liquid Glass must be default")

        // 3. Bundled Snapshot Decoding (20 clubs, 344 players, 380 games)
        let repoSnapshot = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
            .appendingPathComponent("app/src/main/assets/euroleague_live_snapshot.json")
        let fallbackSnapshot = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
            .appendingPathComponent("../app/src/main/assets/euroleague_live_snapshot.json")
        let snapUrl = FileManager.default.fileExists(atPath: repoSnapshot.path) ? repoSnapshot : fallbackSnapshot
        let snapData = try Data(contentsOf: snapUrl)
        let decoded = try CourtsideLiveEngine.decodeBundledSnapshot(snapData)
        check(decoded.teams.count == 20, "Expected 20 EuroLeague clubs, got \(decoded.teams.count)")
        check(decoded.players.count >= 300, "Expected >=300 verified players, got \(decoded.players.count)")
        check(decoded.games.count >= 300, "Expected full season games, got \(decoded.games.count)")
        check(decoded.teams.reduce(0) { $0 + $1.pointsFor } == decoded.teams.reduce(0) { $0 + $1.pointsAgainst }, "Sum of regulation PF must equal sum of PA")

        // 4. RFC 5545 .ics Season Exporter
        let bayern = decoded.teams.first { CourtsideAnalytics.canonicalClubId($0.id) == "MUN" } ?? decoded.teams[0]
        let bayernGames = decoded.games.filter {
            CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == "MUN" ||
            CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == "MUN"
        }
        let ics = CourtsideAnalytics.buildRfc5545IcsPayload(team: bayern, games: bayernGames)
        check(ics.hasPrefix("BEGIN:VCALENDAR\r\n"), "Invalid VCALENDAR header")
        check(ics.contains("TRIGGER:-PT30M\r\n"), "Missing 30m VALARM")
        check(ics.hasSuffix("END:VCALENDAR\r\n"), "Invalid VCALENDAR footer")

        // 5. Cloudflare 3s Cache-Busting + Live Header OT + Play-by-Play + Momentum
        let u0 = CourtsideLiveEngine.buildLiveApiUrl(endpoint: "Header", gameCode: 140, seasonCode: "E2026", nowMillis: 1_700_000_000_000)
        let u4 = CourtsideLiveEngine.buildLiveApiUrl(endpoint: "Header", gameCode: 140, seasonCode: "E2026", nowMillis: 1_700_000_004_000)
        check(u0 != u4 && u0.contains("&_t="), "Cloudflare 3s cache-busting bucket must advance")

        let otHeader = LiveHeaderDto(
            Live: false, Round: "14", ScoreA: "117", ScoreB: "114",
            Quarter: "", RemainingPartialTime: "00:00", GameTime: "45:00",
            ScoreQuarter1A: 19, ScoreQuarter2A: 50, ScoreQuarter3A: 78, ScoreQuarter4A: 101, ScoreExtraTimeA: 117,
            ScoreQuarter1B: 25, ScoreQuarter2B: 57, ScoreQuarter3B: 79, ScoreQuarter4B: 101, ScoreExtraTimeB: 114
        )
        let otEnriched = CourtsideLiveEngine.applyLiveHeader(to: decoded.games[0], header: otHeader)
        check(otEnriched.status == .final_ && otEnriched.quarters.otHome == 16 && otEnriched.quarters.otAway == 13, "OT Header parsing failed")

        // 6. Per-Game Reminders (Q1, HT, Q3, Crunch Time, Final) & Menu Bar Formatter
        var crunchGame = decoded.games[0]
        crunchGame.id = "E2026_999"
        crunchGame.status = .live
        crunchGame.currentQuarter = 4
        crunchGame.remainingClock = "01:24"
        crunchGame.homeScore = 84
        crunchGame.awayScore = 82
        check(crunchGame.isCloseGameFourthQuarter, "Crunch time detection failed")

        let alerts = CourtsideLiveEngine.evaluateAlertActions(
            games: [crunchGame],
            favoriteTeamIds: [],
            perGameReminders: ["E2026_999": [.crunchTime, .finalScore]],
            tipOffEnabled: false,
            halftimeFinalEnabled: false,
            closeGameEnabled: false,
            alreadySentKeys: [],
            language: .en
        )
        check(alerts.count == 1 && alerts[0].kind == .crunchTime, "Per-game crunch-time reminder failed")

        let mbLabel = CourtsideLiveEngine.formatMenuBarLabel(
            games: [crunchGame],
            primaryTeam: bayern,
            perGameReminders: ["E2026_999": [.crunchTime]]
        )
        check(mbLabel.contains("84:82") && mbLabel.contains("Q4"), "Menu bar live label formatting failed: \(mbLabel)")

        // 7. Offline Gamecast Cache v2 Save & Reload
        let tmpCacheUrl = URL(fileURLWithPath: NSTemporaryDirectory())
            .appendingPathComponent("courtside_test_cache_\(UUID().uuidString).json")
        defer { try? FileManager.default.removeItem(at: tmpCacheUrl) }
        crunchGame.playByPlay = [
            PlayByPlayEvent(
                id: "pbp_1", playNumber: 1, quarter: 4, clock: "01:24", gameMinute: 38.6,
                teamId: crunchGame.homeTeam.id, playerId: "p5", playerName: "Carsen Edwards",
                playType: "3FGM", category: .threePt, description: "Clutch 3PT",
                homeScore: 84, awayScore: 82, pointsScored: 3
            )
        ]
        let cacheIn = OfflineGamecastCacheV2(
            seasonCode: "E2026", activeRound: 14,
            teams: decoded.teams, players: decoded.players, games: [crunchGame]
        )
        try CourtsideLiveEngine.saveOfflineCacheV2(cacheIn, to: tmpCacheUrl)
        let cacheOut = try CourtsideLiveEngine.loadOfflineCacheV2(from: tmpCacheUrl)
        check(cacheOut.version == 2 && cacheOut.games.first?.playByPlay.first?.description == "Clutch 3PT", "Offline cache v2 round-trip failed")

        // 8. 6-Language Localization & GitHub Semantic Version Comparison
        for lang in AppLanguage.allCases {
            let s = CourtsideTranslations.forLanguage(lang)
            check(!s.navMyTeam.isEmpty && !s.menuBarTrackerTitle.isEmpty && !s.onboardingWelcomeTitle.isEmpty, "Missing localization for \(lang)")
        }
        check(CourtsideLiveEngine.compareSemanticVersions("v2.0.0-beta.2", "v2.0.0-beta.1") > 0, "SemVer beta comparison failed")
        check(CourtsideLiveEngine.compareSemanticVersions("v2.0.0", "v2.0.0-beta.2") > 0, "SemVer final vs beta failed")

        print("✅ All 8 EuroLeague Courtside 2.0 macOS test suites PASSED (\(decoded.teams.count) clubs, \(decoded.players.count) players, \(decoded.games.count) games verified)!")
    }
}
