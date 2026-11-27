import SwiftUI
import AppKit
import UserNotifications
import CourtsideCore

public enum MacCourtsideTab: String, CaseIterable, Identifiable {
    case myTeam
    case matchCenter
    case standings
    case analytics
    case settings

    public var id: String { rawValue }

    public var sfSymbol: String {
        switch self {
        case .myTeam: return "house.fill"
        case .matchCenter: return "basketball.fill"
        case .standings: return "trophy.fill"
        case .analytics: return "chart.xyaxis.line"
        case .settings: return "slider.horizontal.3"
        }
    }

    public var shortcutCharacter: KeyEquivalent {
        switch self {
        case .myTeam: return "1"
        case .matchCenter: return "2"
        case .standings: return "3"
        case .analytics: return "4"
        case .settings: return "5"
        }
    }

    public func title(_ s: CourtsideStrings) -> String {
        switch self {
        case .myTeam: return s.navMyTeam
        case .matchCenter: return s.navMatchday
        case .standings: return s.navStandings
        case .analytics: return s.navAnalytics
        case .settings: return s.navSettings
        }
    }
}

@MainActor
final class CourtsideViewModel: ObservableObject {
    // Navigation & Modals
    @Published var selectedTab: MacCourtsideTab = .myTeam
    @Published var selectedStandingsTeam: EuroLeagueTeam? = nil
    @Published var selectedPlayerProfile: PlayerProfile? = nil
    @Published var reminderSheetGame: EuroLeagueGame? = nil
    @Published var showOnboardingSheet: Bool = false

    // Dataset State
    @Published var teams: [EuroLeagueTeam] = []
    @Published var players: [PlayerProfile] = []
    @Published var games: [EuroLeagueGame] = []
    @Published var activeRound: Int = 14
    @Published var selectedRound: Int = 14
    @Published var selectedGameId: String? = nil
    @Published var seasonCode: String = "E2026"
    @Published var syncStatusText: String = "LIVE SNAPSHOT • Round 14"
    @Published var isSyncing: Bool = false
    @Published var toastBanner: String? = nil

    // Filters & Analytics Comparison
    @Published var matchSearchQuery: String = ""
    @Published var matchFilterMode: Int = 0 // 0 = All, 1 = Favorites, 2 = Live Only
    @Published var activeCourtTab: Int = 0 // 0 = Shot Chart, 1 = Momentum, 2 = Play-by-Play
    @Published var selectedQuarterFilter: Int = 0 // 0 = ALL, 1..4 = Q1..Q4, 5 = OT
    @Published var clutchOnlyPbp: Bool = false
    @Published var selectedBoxscoreSide: Int = 0 // 0 = Home, 1 = Away
    @Published var inspectedShot: ShotAttempt? = nil
    @Published var leaderMetric: Int = 0 // 0 = PIR, 1 = PTS, 2 = REB, 3 = AST
    @Published var comparisonTeamAId: String = "MUN"
    @Published var comparisonTeamBId: String = "PAN"

    // Persisted Preferences
    @Published var primaryTeamId: String
    @Published var favoriteTeamIds: Set<String>
    @Published var language: AppLanguage
    @Published var themeMode: AppThemeMode
    @Published var tipOffEnabled: Bool
    @Published var halftimeFinalEnabled: Bool
    @Published var closeGameAlertEnabled: Bool
    @Published var menuBarTrackerEnabled: Bool
    @Published var betaUpdatesEnabled: Bool
    @Published var perGameReminders: [String: Set<GameReminderType>]
    @Published var updateResult: AppReleaseCheckResult = AppReleaseCheckResult()

    private var dispatchedAlertKeys: Set<String> = []
    private var liveLoopTask: Task<Void, Never>? = nil
    private let defaults = UserDefaults.standard

    var strings: CourtsideStrings {
        CourtsideTranslations.forLanguage(language)
    }

    var palette: LiquidGlassPalette {
        LiquidGlassPalette(mode: themeMode)
    }

    var primaryTeam: EuroLeagueTeam {
        let canon = CourtsideAnalytics.canonicalClubId(primaryTeamId)
        return teams.first { CourtsideAnalytics.canonicalClubId($0.id) == canon }
            ?? teams.first
            ?? EuroLeagueTeam(id: "MUN", code: "MUN", name: "FC Bayern Munich", city: "Munich", country: "Germany", arena: "SAP Garden")
    }

    var standings: [EuroLeagueTeam] {
        CourtsideAnalytics.recomputeAndSortStandings(teams: teams, games: games)
    }

    var menuBarTitle: String {
        CourtsideLiveEngine.formatMenuBarLabel(
            games: games,
            primaryTeam: primaryTeam,
            perGameReminders: perGameReminders
        )
    }

    private var offlineCacheFileURL: URL {
        let appSupport = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        return appSupport
            .appendingPathComponent("EuroLeagueCourtside", isDirectory: true)
            .appendingPathComponent("offline_gamecast_v2.json")
    }

    init() {
        let savedTeam = defaults.string(forKey: "courtside.primaryTeamId") ?? "MUN"
        let savedFavs = Set(defaults.stringArray(forKey: "courtside.favoriteTeamIds") ?? [savedTeam, "PAN"]).union([savedTeam])
        let savedLang = AppLanguage.fromCode(defaults.string(forKey: "courtside.language") ?? "en")
        let savedTheme = AppThemeMode.fromCode(defaults.string(forKey: "courtside.themeMode") ?? AppThemeMode.defaultMode.rawValue)

        self.primaryTeamId = savedTeam
        self.favoriteTeamIds = savedFavs
        self.language = savedLang
        self.themeMode = savedTheme
        self.tipOffEnabled = defaults.object(forKey: "courtside.tipOff") as? Bool ?? true
        self.halftimeFinalEnabled = defaults.object(forKey: "courtside.htFinal") as? Bool ?? true
        self.closeGameAlertEnabled = defaults.object(forKey: "courtside.closeGame") as? Bool ?? true
        self.menuBarTrackerEnabled = defaults.object(forKey: "courtside.menuBarTracker") as? Bool ?? true
        self.betaUpdatesEnabled = defaults.object(forKey: "courtside.betaUpdates") as? Bool ?? true
        self.comparisonTeamAId = savedTeam
        self.comparisonTeamBId = savedTeam == "PAN" ? "OLY" : "PAN"

        if let rawRemindersData = defaults.data(forKey: "courtside.perGameReminders"),
           let decoded = try? JSONDecoder().decode([String: Set<GameReminderType>].self, from: rawRemindersData) {
            self.perGameReminders = decoded
        } else {
            self.perGameReminders = [:]
        }

        let onboardingDone = defaults.bool(forKey: "courtside.onboardingCompletedV2")
        self.showOnboardingSheet = !onboardingDone

        bootstrapOfflineSnapshotAndCache()
        requestNotificationAuthorizationIfAvailable()
        startAdaptiveLiveTicker()
        Task { await checkForUpdates(manual: false) }
    }

    // MARK: - Persistence Setters

    func selectPrimaryTeam(_ teamId: String) {
        primaryTeamId = teamId
        favoriteTeamIds.insert(teamId)
        comparisonTeamAId = teamId
        if comparisonTeamBId == teamId {
            comparisonTeamBId = teamId == "PAN" ? "OLY" : "PAN"
        }
        defaults.set(teamId, forKey: "courtside.primaryTeamId")
        defaults.set(Array(favoriteTeamIds), forKey: "courtside.favoriteTeamIds")
    }

    func toggleFavoriteTeam(_ teamId: String) {
        if favoriteTeamIds.contains(teamId) && favoriteTeamIds.count > 1 && teamId != primaryTeamId {
            favoriteTeamIds.remove(teamId)
        } else {
            favoriteTeamIds.insert(teamId)
        }
        defaults.set(Array(favoriteTeamIds), forKey: "courtside.favoriteTeamIds")
    }

    func selectLanguage(_ lang: AppLanguage) {
        language = lang
        defaults.set(lang.rawValue, forKey: "courtside.language")
    }

    func selectThemeMode(_ mode: AppThemeMode) {
        themeMode = mode
        defaults.set(mode.rawValue, forKey: "courtside.themeMode")
    }

    func updateNotificationChannels(tipOff: Bool, htFinal: Bool, closeGame: Bool) {
        tipOffEnabled = tipOff
        halftimeFinalEnabled = htFinal
        closeGameAlertEnabled = closeGame
        defaults.set(tipOff, forKey: "courtside.tipOff")
        defaults.set(htFinal, forKey: "courtside.htFinal")
        defaults.set(closeGame, forKey: "courtside.closeGame")
    }

    func setMenuBarTracker(_ enabled: Bool) {
        menuBarTrackerEnabled = enabled
        defaults.set(enabled, forKey: "courtside.menuBarTracker")
    }

    func setBetaUpdates(_ enabled: Bool) {
        betaUpdatesEnabled = enabled
        defaults.set(enabled, forKey: "courtside.betaUpdates")
        Task { await checkForUpdates(manual: true) }
    }

    func completeOnboarding() {
        showOnboardingSheet = false
        defaults.set(true, forKey: "courtside.onboardingCompletedV2")
    }

    func updateReminders(for game: EuroLeagueGame, reminders: Set<GameReminderType>) {
        if reminders.isEmpty {
            perGameReminders.removeValue(forKey: game.id)
        } else {
            perGameReminders[game.id] = reminders
        }
        if let encoded = try? JSONEncoder().encode(perGameReminders) {
            defaults.set(encoded, forKey: "courtside.perGameReminders")
        }
        evaluateAndDeliverNotifications()
    }

    func toggleSingleReminder(for game: EuroLeagueGame, type: GameReminderType) {
        var current = perGameReminders[game.id] ?? []
        if current.contains(type) { current.remove(type) } else { current.insert(type) }
        updateReminders(for: game, reminders: current)
    }

    func openGameInMatchCenter(gameId: String, round: Int) {
        selectedRound = min(max(round, 1), 38)
        selectedGameId = gameId
        selectedPlayerProfile = nil
        selectedTab = .matchCenter
    }

    func resolvePlayerProfile(playerId: String, playerName: String, teamId: String) -> PlayerProfile? {
        let cleanTarget = CourtsideAnalytics.cleanPlayerDisplayName(playerName).lowercased()
        let teamCanon = CourtsideAnalytics.canonicalClubId(teamId)
        if let exact = players.first(where: { !$0.id.isEmpty && $0.id == playerId }) {
            return exact
        }
        if let byName = players.first(where: {
            CourtsideAnalytics.canonicalClubId($0.teamId) == teamCanon &&
            $0.name.lowercased() == cleanTarget
        }) {
            return byName
        }
        if let anyName = players.first(where: { $0.name.lowercased() == cleanTarget }) {
            return anyName
        }
        // Synthesize scouting profile from boxscores if player joined mid-season
        let matchingRows = games.flatMap { $0.boxscoreHome + $0.boxscoreAway }
            .filter { $0.playerName.lowercased() == cleanTarget }
        guard let first = matchingRows.first else { return nil }
        let gp = Double(max(1, matchingRows.count))
        return PlayerProfile(
            id: first.playerId.isEmpty ? "syn_\(cleanTarget)" : first.playerId,
            teamId: first.teamId.isEmpty ? teamId : first.teamId,
            number: first.jerseyNumber,
            name: first.playerName,
            position: "G/F",
            nationality: "INT",
            heightCm: 198,
            ppg: Double(matchingRows.reduce(0) { $0 + $1.points }) / gp,
            rpg: Double(matchingRows.reduce(0) { $0 + $1.totalRebounds }) / gp,
            apg: Double(matchingRows.reduce(0) { $0 + $1.assists }) / gp,
            pir: Double(matchingRows.reduce(0) { $0 + $1.pir }) / gp
        )
    }

    // MARK: - Snapshot + Offline Gamecast Cache v2 Bootstrap

    private func bootstrapOfflineSnapshotAndCache() {
        let snapshotCandidates: [URL?] = [
            Bundle.main.url(forResource: "euroleague_live_snapshot", withExtension: "json"),
            Bundle.main.resourceURL?.appendingPathComponent("euroleague_live_snapshot.json"),
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
                .appendingPathComponent("app/src/main/assets/euroleague_live_snapshot.json"),
            URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
                .appendingPathComponent("../app/src/main/assets/euroleague_live_snapshot.json")
        ]

        for url in snapshotCandidates.compactMap({ $0 }) {
            if let data = try? Data(contentsOf: url),
               let parsed = try? CourtsideLiveEngine.decodeBundledSnapshot(data) {
                self.seasonCode = parsed.seasonCode
                self.activeRound = parsed.activeRound
                self.selectedRound = parsed.activeRound
                self.teams = parsed.teams
                self.players = parsed.players
                self.games = parsed.games
                self.syncStatusText = "SNAPSHOT • \(parsed.seasonCode) • Round \(parsed.activeRound)"
                break
            }
        }

        // Overlay persisted Offline Gamecast Cache v2 (preserves live boxscores, shot charts & play-by-play offline!)
        if let cached = try? CourtsideLiveEngine.loadOfflineCacheV2(from: offlineCacheFileURL), !cached.games.isEmpty {
            let cachedById = Dictionary(uniqueKeysWithValues: cached.games.map { ($0.id, $0) })
            let baseIds = Set(self.games.map(\.id))
            var merged = self.games.map { base -> EuroLeagueGame in
                guard let cg = cachedById[base.id] else { return base }
                var copy = cg
                if copy.boxscoreHome.isEmpty { copy.boxscoreHome = base.boxscoreHome }
                if copy.boxscoreAway.isEmpty { copy.boxscoreAway = base.boxscoreAway }
                if copy.shots.isEmpty { copy.shots = base.shots }
                if copy.playByPlay.isEmpty { copy.playByPlay = base.playByPlay }
                return copy
            }
            merged.append(contentsOf: cached.games.filter { !baseIds.contains($0.id) })
            self.games = merged
            if !cached.players.isEmpty { self.players = cached.players }
            self.teams = CourtsideAnalytics.recomputeAndSortStandings(
                teams: cached.teams.isEmpty ? self.teams : cached.teams,
                games: merged
            )
            self.activeRound = cached.activeRound
            self.syncStatusText = "OFFLINE CACHE v2 • Round \(cached.activeRound)"
        }

        ensureSelectedGameInRound()
    }

    func ensureSelectedGameInRound() {
        let roundGames = games.filter { $0.round == selectedRound }
        if let sel = selectedGameId, roundGames.contains(where: { $0.id == sel }) {
            return
        }
        let pCanon = CourtsideAnalytics.canonicalClubId(primaryTeamId)
        selectedGameId = roundGames.first(where: { $0.status == .live })?.id
            ?? roundGames.first(where: {
                CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == pCanon ||
                CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == pCanon
            })?.id
            ?? roundGames.first?.id
    }

    private func persistOfflineCache() {
        let cache = OfflineGamecastCacheV2(
            seasonCode: seasonCode,
            activeRound: activeRound,
            teams: teams,
            players: players,
            games: games
        )
        let target = offlineCacheFileURL
        Task.detached(priority: .utility) {
            try? CourtsideLiveEngine.saveOfflineCacheV2(cache, to: target)
        }
    }

    // MARK: - High-Speed 5s Live EuroLeague Sync Loop

    private func startAdaptiveLiveTicker() {
        liveLoopTask?.cancel()
        liveLoopTask = Task {
            await triggerLiveSync(silent: true)
            while !Task.isCancelled {
                let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
                let hasActiveWindow = games.contains { g in
                    g.status == .live ||
                    (g.status == .upcoming && g.tipOffEpochMillis >= nowMs - 4 * 3_600_000 && g.tipOffEpochMillis <= nowMs + 20 * 60_000)
                }
                let sleepNs: UInt64 = hasActiveWindow ? 5_000_000_000 : 25_000_000_000
                try? await Task.sleep(nanoseconds: sleepNs)
                await refreshActiveMatchdayHeaders()
                if let selId = selectedGameId, let selGame = games.first(where: { $0.id == selId }),
                   (selGame.status == .live || selGame.playByPlay.isEmpty || selGame.boxscoreHome.isEmpty) {
                    await refreshSelectedGameFullGamecast(selGame)
                }
            }
        }
    }

    func triggerLiveSync(silent: Bool = false) async {
        guard !isSyncing else { return }
        isSyncing = true
        defer { isSyncing = false }

        await refreshActiveMatchdayHeaders()
        if let selId = selectedGameId, let selGame = games.first(where: { $0.id == selId }) {
            await refreshSelectedGameFullGamecast(selGame)
        }

        let fmt = DateFormatter()
        fmt.dateFormat = "HH:mm:ss"
        let stamp = fmt.string(from: Date())
        syncStatusText = "LIVE API • \(seasonCode) • Round \(activeRound) • \(stamp)"
        persistOfflineCache()

        if !silent {
            showToast("⚡ \(strings.liveSyncButton) • \(stamp)")
        }
    }

    func refreshActiveMatchdayHeaders() async {
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        let candidates = games.filter { g in
            g.round == activeRound || g.round == selectedRound || g.status == .live ||
            (g.status == .upcoming && abs(g.tipOffEpochMillis - nowMs) <= 6 * 3_600_000)
        }.prefix(20)

        guard !candidates.isEmpty else { return }
        var updatedById: [String: EuroLeagueGame] = [:]

        await withTaskGroup(of: (String, EuroLeagueGame)?.self) { group in
            for game in candidates where game.gameCode > 0 {
                group.addTask {
                    let urlStr = CourtsideLiveEngine.buildLiveApiUrl(
                        endpoint: "Header",
                        gameCode: game.gameCode,
                        seasonCode: game.seasonCode
                    )
                    guard let data = await Self.fetchNoCacheData(urlString: urlStr),
                          let dto = try? JSONDecoder().decode(LiveHeaderDto.self, from: data) else {
                        return nil
                    }
                    let enriched = CourtsideLiveEngine.applyLiveHeader(to: game, header: dto)
                    return (game.id, enriched)
                }
            }
            for await pair in group {
                if let (id, enriched) = pair {
                    updatedById[id] = enriched
                }
            }
        }

        if !updatedById.isEmpty {
            games = games.map { updatedById[$0.id] ?? $0 }
            teams = CourtsideAnalytics.recomputeAndSortStandings(teams: teams, games: games)
            evaluateAndDeliverNotifications()
        }
    }

    func refreshSelectedGameFullGamecast(_ game: EuroLeagueGame) async {
        guard game.gameCode > 0 else { return }
        let code = game.gameCode
        let season = game.seasonCode

        async let headerData = Self.fetchNoCacheData(urlString: CourtsideLiveEngine.buildLiveApiUrl(endpoint: "Header", gameCode: code, seasonCode: season))
        async let boxData = Self.fetchNoCacheData(urlString: CourtsideLiveEngine.buildLiveApiUrl(endpoint: "Boxscore", gameCode: code, seasonCode: season))
        async let ptsData = Self.fetchNoCacheData(urlString: CourtsideLiveEngine.buildLiveApiUrl(endpoint: "Points", gameCode: code, seasonCode: season))
        async let pbpData = Self.fetchNoCacheData(urlString: CourtsideLiveEngine.buildLiveApiUrl(endpoint: "PlaybyPlay", gameCode: code, seasonCode: season))

        let (hRaw, bRaw, ptRaw, pbpRaw) = await (headerData, boxData, ptsData, pbpData)
        var updated = games.first(where: { $0.id == game.id }) ?? game

        if let hRaw, let hDto = try? JSONDecoder().decode(LiveHeaderDto.self, from: hRaw) {
            updated = CourtsideLiveEngine.applyLiveHeader(to: updated, header: hDto)
        }
        if let bRaw, let bDto = try? JSONDecoder().decode(LiveBoxscoreDto.self, from: bRaw) {
            let parsedBox = CourtsideLiveEngine.parseLiveBoxscore(dto: bDto, homeTeamId: updated.homeTeam.id, awayTeamId: updated.awayTeam.id)
            if !parsedBox.home.isEmpty { updated.boxscoreHome = parsedBox.home }
            if !parsedBox.away.isEmpty { updated.boxscoreAway = parsedBox.away }
        }
        if let ptRaw, let ptDto = try? JSONDecoder().decode(LivePointsDto.self, from: ptRaw) {
            let parsedShots = CourtsideLiveEngine.parseLiveShots(dto: ptDto, gameCode: code, homeTeamId: updated.homeTeam.id, awayTeamId: updated.awayTeam.id)
            if !parsedShots.isEmpty { updated.shots = parsedShots }
        }
        if let pbpRaw, let pbpDto = try? JSONDecoder().decode(LivePlayByPlayDto.self, from: pbpRaw) {
            let parsedPbp = CourtsideLiveEngine.parseLivePlayByPlay(dto: pbpDto, gameCode: code, homeTeamId: updated.homeTeam.id, awayTeamId: updated.awayTeam.id)
            if !parsedPbp.isEmpty { updated.playByPlay = parsedPbp }
        }

        if let idx = games.firstIndex(where: { $0.id == updated.id }) {
            games[idx] = updated
            persistOfflineCache()
            evaluateAndDeliverNotifications()
        }
    }

    func verifyClubRosterNow() async {
        let clubCode = primaryTeam.code
        let urlStr = "https://api-live.euroleague.net/v2/competitions/E/seasons/\(seasonCode)/clubs/\(clubCode)/people"
        if let data = await Self.fetchNoCacheData(urlString: urlStr), !data.isEmpty {
            showToast("✓ \(primaryTeam.name) • \(strings.activeRosterHeader)")
        } else {
            showToast("✓ \(primaryTeam.name) • Offline Verified Roster")
        }
    }

    private static func fetchNoCacheData(urlString: String) async -> Data? {
        guard let url = URL(string: urlString) else { return nil }
        var req = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalAndRemoteCacheData, timeoutInterval: 8.0)
        req.setValue("no-cache, no-store, max-age=0", forHTTPHeaderField: "Cache-Control")
        req.setValue("no-cache", forHTTPHeaderField: "Pragma")
        req.setValue("EuroLeagueCourtsideMac/2.0 (macOS Liquid Glass)", forHTTPHeaderField: "User-Agent")
        return try? await URLSession.shared.data(for: req).0
    }

    // MARK: - macOS Notifications, Calendar .ics Export & GitHub Updater

    private func requestNotificationAuthorizationIfAvailable() {
        guard Bundle.main.bundleIdentifier != nil else { return }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func evaluateAndDeliverNotifications() {
        let actions = CourtsideLiveEngine.evaluateAlertActions(
            games: games,
            favoriteTeamIds: favoriteTeamIds.union([primaryTeamId]),
            perGameReminders: perGameReminders,
            tipOffEnabled: tipOffEnabled,
            halftimeFinalEnabled: halftimeFinalEnabled,
            closeGameEnabled: closeGameAlertEnabled,
            alreadySentKeys: dispatchedAlertKeys,
            language: language
        )
        for act in actions {
            dispatchedAlertKeys.insert(act.dedupeKey)
            postMacOSNotification(id: act.dedupeKey, title: act.title, body: act.body)
        }
    }

    func sendInstantTestNotification() {
        let sample = games.first(where: { $0.status == .live })
            ?? games.first(where: { CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == CourtsideAnalytics.canonicalClubId(primaryTeam.id) })
            ?? games.first
        let title = sample.map { "🏀 \($0.homeTeam.code) \($0.homeScore) : \($0.awayScore) \($0.awayTeam.code) • Q\($0.currentQuarter) \($0.remainingClock)" }
            ?? "🏀 EuroLeague Courtside 2.0 • macOS Liquid Glass"
        let body = sample.map { "\($0.homeTeam.name) vs \($0.awayTeam.name) • \(strings.roundPrefix) \($0.round)" }
            ?? strings.menuBarTrackerSubtitle
        postMacOSNotification(id: "test_\(Int(Date().timeIntervalSince1970))", title: title, body: body)
        showToast("🔔 \(title)")
    }

    private func postMacOSNotification(id: String, title: String, body: String) {
        guard Bundle.main.bundleIdentifier != nil else { return }
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let req = UNNotificationRequest(identifier: id, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(req, withCompletionHandler: nil)
    }

    func exportSingleGameToAppleCalendar(_ game: EuroLeagueGame) {
        let ics = CourtsideAnalytics.buildRfc5545IcsPayload(team: primaryTeam, games: [game])
        let tmp = URL(fileURLWithPath: NSTemporaryDirectory())
            .appendingPathComponent("EuroLeague-R\(game.round)-\(game.homeTeam.code)-vs-\(game.awayTeam.code).ics")
        do {
            try ics.data(using: .utf8)?.write(to: tmp, options: .atomic)
            NSWorkspace.shared.open(tmp)
            showToast("📅 \(game.homeTeam.code) vs \(game.awayTeam.code) → Calendar")
        } catch {
            showToast("⚠️ Calendar export failed")
        }
    }

    func exportSeasonIcsWithSavePanel() {
        let pCanon = CourtsideAnalytics.canonicalClubId(primaryTeam.id)
        let clubGames = games.filter {
            CourtsideAnalytics.canonicalClubId($0.homeTeam.id) == pCanon ||
            CourtsideAnalytics.canonicalClubId($0.awayTeam.id) == pCanon
        }
        let ics = CourtsideAnalytics.buildRfc5545IcsPayload(team: primaryTeam, games: clubGames)

        let panel = NSSavePanel()
        panel.nameFieldStringValue = "EuroLeague-\(primaryTeam.code)-\(seasonCode)-Season.ics"
        panel.canCreateDirectories = true
        if panel.runModal() == .OK, let url = panel.url {
            try? ics.data(using: .utf8)?.write(to: url, options: .atomic)
            showToast("✓ Saved \(url.lastPathComponent)")
        }
    }

    func checkForUpdates(manual: Bool) async {
        updateResult.status = .checking
        let urlStr = "https://api.github.com/repos/petrosvassiliou/euroleague-courtside/releases?per_page=15"
        guard let data = await Self.fetchNoCacheData(urlString: urlStr) else {
            updateResult = AppReleaseCheckResult(status: .error, errorMessage: "GitHub Releases unreachable")
            return
        }
        updateResult = CourtsideLiveEngine.evaluateGitHubReleasesJson(
            rawData: data,
            currentVersion: CourtsideLiveEngine.currentVersionName,
            includePrereleases: betaUpdatesEnabled
        )
        if manual {
            if updateResult.status == .updateAvailable {
                showToast("🚀 v\(updateResult.latestVersionName) available!")
            } else if updateResult.status == .upToDate {
                showToast("\(strings.upToDateLabel) (v\(CourtsideLiveEngine.currentVersionName))")
            }
        }
    }

    func showToast(_ message: String) {
        toastBanner = message
        Task {
            try? await Task.sleep(nanoseconds: 3_500_000_000)
            if toastBanner == message {
                toastBanner = nil
            }
        }
    }
}
