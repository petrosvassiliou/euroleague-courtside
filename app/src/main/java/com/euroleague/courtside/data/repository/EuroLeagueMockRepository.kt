package com.euroleague.courtside.data.repository

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

// --- DOMAIN MODELS ---

data class EuroLeagueTeam(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val arena: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long = 0xFFFFFFFF,
    val wins: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val streak: List<String>,
    val offensiveRating: Float,
    val defensiveRating: Float,
    val pace: Float,
    val threePointPct: Float,
    val twoPointPct: Float,
    val reboundRate: Float,
    val logoUrl: String = "https://media-cdn.cortextech.io/crest/${id.lowercase()}.png",
    val localLogoResName: String = "club_logo_${id.lowercase()}",
    val totalPointsFor: Int = pointsFor,
    val totalPointsAgainst: Int = pointsAgainst
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)
    val accentColor: Color get() = Color(accentColorHex)
    val pointDiff: Int get() = pointsFor - pointsAgainst
    val gamesPlayed: Int get() = wins + losses
    val winPercentage: Double get() = if (gamesPlayed > 0) wins.toDouble() / gamesPlayed.toDouble() else 0.0
    val ppg: Float
        get() = kotlin.math.round((totalPointsFor.toFloat() / gamesPlayed.coerceAtLeast(1)) * 10f) / 10f
    val oppg: Float
        get() = kotlin.math.round((totalPointsAgainst.toFloat() / gamesPlayed.coerceAtLeast(1)) * 10f) / 10f
}

enum class InjuryStatus(val label: String) {
    FIT("Einsatzbereit"),
    GAME_TIME_DECISION("Fraglich (GTD)"),
    QUESTIONABLE("Fraglich (GTD)"),
    OUT("Verletzt")
}

data class PlayerProfile(
    val id: String,
    val teamId: String,
    val number: Int,
    val name: String,
    val position: String,
    val nationality: String,
    val heightCm: Int,
    val ppg: Float,
    val rpg: Float,
    val apg: Float,
    val pir: Float,
    val injuryStatus: InjuryStatus = InjuryStatus.FIT,
    val injuryDetail: String = "",
    val headshotUrl: String = ""
)

enum class GameStatus {
    UPCOMING, LIVE, FINAL
}

data class QuarterScores(
    val q1Home: Int, val q1Away: Int,
    val q2Home: Int, val q2Away: Int,
    val q3Home: Int, val q3Away: Int,
    val q4Home: Int, val q4Away: Int,
    val otHome: Int? = null, val otAway: Int? = null
)

data class PlayerBoxscoreStat(
    val playerId: String,
    val playerName: String,
    val jerseyNumber: Int,
    val teamId: String,
    val minutes: String,
    val points: Int,
    val offRebounds: Int,
    val defRebounds: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val twoPtMade: Int,
    val twoPtAttempted: Int,
    val threePtMade: Int,
    val threePtAttempted: Int,
    val ftMade: Int,
    val ftAttempted: Int,
    val foulsDrawn: Int,
    val foulsCommitted: Int,
    val blocksAgainst: Int = 0,
    val valuationOverride: Int? = null
) {
    val totalRebounds: Int get() = offRebounds + defRebounds
    val twoPtPct: Float get() = if (twoPtAttempted > 0) (twoPtMade * 100f) / twoPtAttempted else 0f
    val threePtPct: Float get() = if (threePtAttempted > 0) (threePtMade * 100f) / threePtAttempted else 0f
    val ftPct: Float get() = if (ftAttempted > 0) (ftMade * 100f) / ftAttempted else 0f

    val pir: Int
        get() {
            if (valuationOverride != null) return valuationOverride
            val missedFg = (twoPtAttempted - twoPtMade) + (threePtAttempted - threePtMade)
            val missedFt = ftAttempted - ftMade
            return (points + totalRebounds + assists + steals + blocks + foulsDrawn) -
                    (missedFg + missedFt + turnovers + blocksAgainst + foulsCommitted)
        }
}

data class ShotAttempt(
    val id: String,
    val playerId: String,
    val playerName: String,
    val teamId: String,
    val courtX: Float,
    val courtY: Float,
    val made: Boolean,
    val isThreePointer: Boolean,
    val quarter: Int
) {
    val isMade: Boolean get() = made
}

data class EuroLeagueGame(
    val id: String,
    val round: Int,
    val homeTeam: EuroLeagueTeam,
    val awayTeam: EuroLeagueTeam,
    val tipOffEpochMillis: Long,
    val status: GameStatus,
    val currentQuarter: Int = 4,
    val remainingClock: String = "00:00",
    val homeScore: Int,
    val awayScore: Int,
    val quarters: QuarterScores,
    val headToHeadHomeWins: Int,
    val headToHeadAwayWins: Int,
    val boxscoreHome: List<PlayerBoxscoreStat>,
    val boxscoreAway: List<PlayerBoxscoreStat>,
    val shots: List<ShotAttempt>,
    val gameCode: Int = 0,
    val seasonCode: String = "E2026"
) {
    val shotChart: List<ShotAttempt> get() = shots

    val isCloseGameFourthQuarter: Boolean
        get() {
            if (status != GameStatus.LIVE || currentQuarter < 4) return false
            val diff = kotlin.math.abs(homeScore - awayScore)
            val minutesLeft = remainingClock.substringBefore(":").toIntOrNull() ?: 99
            return minutesLeft < 3 && diff <= 5
        }
}

data class PlayoffSeries(
    val id: String,
    val stage: String,
    val label: String,
    val homeSeed: Int,
    val awaySeed: Int,
    val homeTeam: EuroLeagueTeam,
    val awayTeam: EuroLeagueTeam,
    val homeWins: Int,
    val awayWins: Int
)

/**
 * Reaktives EuroLeague-Daten-Repository:
 * Hält die aktuellen Teams (inkl. offizieller Logos & Farben), verifizierten Kader (Rosters + Verletztenstatus),
 * Spieltage, Boxscores und Shot-Charts als `StateFlow` bereit und wird laufend durch `EuroLeagueOfflineFirstRepository`
 * mit den offiziellen EuroLeague Live-APIs (`api-live.euroleague.net` & `live.euroleague.net/api`) synchronisiert.
 */
@Singleton
class EuroLeagueMockRepository @Inject constructor() {

    private val _teams = MutableStateFlow(buildInitialTeams())
    val teamsFlow: StateFlow<List<EuroLeagueTeam>> = _teams.asStateFlow()
    val teams: List<EuroLeagueTeam> get() = _teams.value

    private val _players = MutableStateFlow(buildInitialPlayers())
    val playersFlow: StateFlow<List<PlayerProfile>> = _players.asStateFlow()
    val players: List<PlayerProfile> get() = _players.value

    private val _favoriteTeamIds = MutableStateFlow(setOf("MUN", "BAY", "PAN", "PAO"))
    val favoriteTeamIds: StateFlow<Set<String>> = _favoriteTeamIds.asStateFlow()

    private val _selectedTeamId = MutableStateFlow("MUN")
    val selectedTeamId: StateFlow<String> = _selectedTeamId.asStateFlow()

    private val _games = MutableStateFlow(buildInitialGames(_teams.value))
    val games: StateFlow<List<EuroLeagueGame>> = _games.asStateFlow()

    private val _activeRound = MutableStateFlow(11)
    val activeRoundFlow: StateFlow<Int> = _activeRound.asStateFlow()

    private val _seasonCode = MutableStateFlow("E2026")
    val seasonCodeFlow: StateFlow<String> = _seasonCode.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("LIVE API • Saison 2026/27 • Kader & Verletzungen geprüft")
    val lastSyncStatusFlow: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private val _appLanguage = MutableStateFlow(com.euroleague.courtside.i18n.AppLanguage.fromCode(null))
    val appLanguageFlow: StateFlow<com.euroleague.courtside.i18n.AppLanguage> = _appLanguage.asStateFlow()

    private val _activeDestination = MutableStateFlow(com.euroleague.courtside.ui.navigation.CourtsideDestination.START_DESTINATION)
    val activeDestinationFlow: StateFlow<com.euroleague.courtside.ui.navigation.CourtsideDestination> = _activeDestination.asStateFlow()

    fun selectLanguage(language: com.euroleague.courtside.i18n.AppLanguage) {
        _appLanguage.value = language
    }

    fun selectDestination(destination: com.euroleague.courtside.ui.navigation.CourtsideDestination) {
        _activeDestination.value = destination
    }

    /**
     * Stellt sicher, dass bei jedem App-Start (Kaltstart, Prozess-Wiederherstellung,
     * Warmstart aus dem Hintergrund oder Launcher-/Notification-Aufruf) immer "My Team" angezeigt wird.
     */
    fun resetToStartDestination() {
        _activeDestination.value = com.euroleague.courtside.ui.navigation.CourtsideDestination.START_DESTINATION
    }

    fun applyLiveDataset(
        liveTeams: List<EuroLeagueTeam>,
        livePlayers: List<PlayerProfile>,
        liveGames: List<EuroLeagueGame>,
        activeRound: Int,
        seasonCode: String,
        syncStatus: String = "",
        syncLabel: String = ""
    ) {
        val normalizedIncomingTeams = (if (liveTeams.isNotEmpty()) liveTeams else _teams.value).map { team ->
            team.copy(streak = normalizeStreakList(team.streak))
        }
        if (livePlayers.isNotEmpty()) {
            _players.value = livePlayers
        }
        val targetRound = if (activeRound > 0) activeRound else _activeRound.value
        if (activeRound > 0) {
            _activeRound.value = activeRound
        }
        if (liveGames.isNotEmpty()) {
            val resolvedGames = ensureLiveScoresInGames(liveGames, targetRound)
            val finalGamesCount = resolvedGames.count {
                it.status == GameStatus.FINAL && (it.homeScore > 0 || it.awayScore > 0) && it.homeScore != it.awayScore
            }
            if (finalGamesCount >= 20) {
                val recomputedTeams = recomputeOfficialStandingsFromGames(normalizedIncomingTeams, resolvedGames)
                _teams.value = recomputedTeams
                _games.value = refreshGameTeamReferences(resolvedGames, recomputedTeams)
            } else {
                _teams.value = normalizedIncomingTeams
                _games.value = resolvedGames
            }
        } else if (liveTeams.isNotEmpty()) {
            _teams.value = normalizedIncomingTeams
        }
        if (seasonCode.isNotBlank()) {
            _seasonCode.value = seasonCode
        }
        val statusText = syncStatus.ifBlank { syncLabel }
        if (statusText.isNotBlank()) {
            _lastSyncStatus.value = statusText
        }
    }

    /**
     * Sortiert alle Spiele chronologisch nach Spieltag (`round` 1..38) und übernimmt
     * den echten offiziellen Spielstatus (`UPCOMING`, `LIVE`, `FINAL`) sowie die echten
     * Ergebnisse der EuroLeague-API unverfälscht. Wenn ein Live-Spiel im 4. Viertel
     * (oder OT) bei `00:00` beendet ist, wird es als `FINAL` markiert.
     */
    internal fun ensureLiveScoresInGames(
        incomingGames: List<EuroLeagueGame>,
        targetRound: Int
    ): List<EuroLeagueGame> {
        if (incomingGames.isEmpty()) return incomingGames
        val sortedIncoming = incomingGames.sortedWith(
            compareBy<EuroLeagueGame> { it.round }
                .thenBy { it.tipOffEpochMillis }
                .thenBy { it.gameCode }
        )
        return sortedIncoming.map { game ->
            val clockTrimmed = game.remainingClock.trim()
            val isEndedClock = clockTrimmed == "00:00" || clockTrimmed == "0:00"
            if (game.status == GameStatus.LIVE &&
                game.currentQuarter >= 4 &&
                isEndedClock &&
                game.homeScore != game.awayScore &&
                (game.homeScore > 0 || game.awayScore > 0)
            ) {
                game.copy(
                    status = GameStatus.FINAL,
                    remainingClock = "00:00"
                )
            } else {
                game
            }
        }
    }

    fun updateClubRoster(clubId: String, verifiedRoster: List<PlayerProfile>, statusMessage: String? = null) {
        if (verifiedRoster.isEmpty()) return
        _players.update { existing ->
            existing.filterNot { it.teamId.equals(clubId, ignoreCase = true) } + verifiedRoster
        }
        if (statusMessage != null) {
            _lastSyncStatus.value = statusMessage
        }
    }

    fun updateSingleGameDetails(updatedGame: EuroLeagueGame) {
        val nextGames = _games.value.map { game ->
            if (game.id == updatedGame.id) updatedGame else game
        }
        val finalCount = nextGames.count {
            it.status == GameStatus.FINAL && (it.homeScore > 0 || it.awayScore > 0) && it.homeScore != it.awayScore
        }
        if (finalCount >= 20) {
            val recomputedTeams = recomputeOfficialStandingsFromGames(_teams.value, nextGames)
            _teams.value = recomputedTeams
            _games.value = refreshGameTeamReferences(nextGames, recomputedTeams)
        } else {
            _games.value = nextGames
        }
    }

    fun updateSingleGameDetails(
        gameId: String,
        boxHome: List<PlayerBoxscoreStat>,
        boxAway: List<PlayerBoxscoreStat>,
        shots: List<ShotAttempt>,
        homeScore: Int? = null,
        awayScore: Int? = null,
        quarters: QuarterScores? = null,
        status: GameStatus? = null
    ) {
        val current = _games.value.find { it.id == gameId } ?: return
        val merged = current.copy(
            homeScore = homeScore ?: current.homeScore,
            awayScore = awayScore ?: current.awayScore,
            quarters = quarters ?: current.quarters,
            status = status ?: current.status,
            boxscoreHome = if (boxHome.isNotEmpty()) boxHome else current.boxscoreHome,
            boxscoreAway = if (boxAway.isNotEmpty()) boxAway else current.boxscoreAway,
            shots = if (shots.isNotEmpty()) shots else current.shots
        )
        updateSingleGameDetails(merged)
    }

    fun findTeam(idOrCode: String): EuroLeagueTeam? {
        val list = _teams.value
        return list.find {
            it.id.equals(idOrCode, ignoreCase = true) ||
                    it.code.equals(idOrCode, ignoreCase = true) ||
                    (idOrCode == "BAY" && it.id == "MUN") ||
                    (idOrCode == "MUN" && it.id == "BAY") ||
                    (idOrCode == "PAO" && it.id == "PAN") ||
                    (idOrCode == "PAN" && it.id == "PAO") ||
                    (idOrCode == "RMB" && it.id == "MAD") ||
                    (idOrCode == "MAD" && it.id == "RMB") ||
                    (idOrCode == "FCB" && it.id == "BAR") ||
                    (idOrCode == "BAR" && it.id == "FCB") ||
                    (idOrCode == "EFS" && it.id == "IST") ||
                    (idOrCode == "IST" && it.id == "EFS") ||
                    (idOrCode == "FBB" && it.id == "ULK") ||
                    (idOrCode == "ULK" && it.id == "FBB") ||
                    (idOrCode == "ASM" && it.id == "MCO") ||
                    (idOrCode == "MCO" && it.id == "ASM") ||
                    (idOrCode == "CZV" && it.id == "RED") ||
                    (idOrCode == "RED" && it.id == "CZV") ||
                    (idOrCode == "EA7" && it.id == "MIL") ||
                    (idOrCode == "MIL" && it.id == "EA7") ||
                    (idOrCode == "BKN" && it.id == "BAS") ||
                    (idOrCode == "BAS" && it.id == "BKN") ||
                    (idOrCode == "MTA" && it.id == "TEL") ||
                    (idOrCode == "TEL" && it.id == "MTA")
        }
    }

    fun toggleFavoriteTeam(teamId: String) {
        _favoriteTeamIds.update { current ->
            if (current.contains(teamId) && current.size > 1) current - teamId else current + teamId
        }
        if (!_favoriteTeamIds.value.contains(_selectedTeamId.value)) {
            _selectedTeamId.value = _favoriteTeamIds.value.first()
        }
    }

    fun selectPrimaryTeam(teamId: String) {
        _selectedTeamId.value = teamId
        _favoriteTeamIds.update { it + teamId }
    }

    fun restoreSavedTeamSelection(primaryTeamId: String, favoriteIds: Set<String> = emptySet()) {
        if (primaryTeamId.isBlank()) return
        val matched = findTeam(primaryTeamId)
        _selectedTeamId.value = primaryTeamId
        val extraIds = setOfNotNull(primaryTeamId, matched?.id, matched?.code)
        _favoriteTeamIds.value = if (favoriteIds.isNotEmpty()) {
            favoriteIds + extraIds
        } else {
            _favoriteTeamIds.value + extraIds
        }
    }

    fun getStandings(): List<EuroLeagueTeam> =
        sortOfficialStandings(
            teams = _teams.value.distinctBy { canonicalClubId(it.id) },
            games = _games.value
        )

    fun getPlayoffBracket(): List<PlayoffSeries> {
        val ranked = getStandings()
        if (ranked.size < 10) return emptyList()
        val playedGames = _games.value.filter { it.status == GameStatus.FINAL && (it.homeScore > 0 || it.awayScore > 0) }

        fun h2hWins(teamA: EuroLeagueTeam, teamB: EuroLeagueTeam): Pair<Int, Int> {
            var winsA = 0
            var winsB = 0
            playedGames.forEach { g ->
                val hMatchA = g.homeTeam.id == teamA.id || g.homeTeam.code == teamA.code
                val aMatchB = g.awayTeam.id == teamB.id || g.awayTeam.code == teamB.code
                val hMatchB = g.homeTeam.id == teamB.id || g.homeTeam.code == teamB.code
                val aMatchA = g.awayTeam.id == teamA.id || g.awayTeam.code == teamA.code
                if (hMatchA && aMatchB) {
                    if (g.homeScore >= g.awayScore) winsA++ else winsB++
                } else if (hMatchB && aMatchA) {
                    if (g.homeScore >= g.awayScore) winsB++ else winsA++
                }
            }
            return winsA to winsB
        }

        val (pi78H, pi78A) = h2hWins(ranked[6], ranked[7])
        val (pi910H, pi910A) = h2hWins(ranked[8], ranked[9])
        val (qf18H, qf18A) = h2hWins(ranked[0], ranked[7])
        val (qf27H, qf27A) = h2hWins(ranked[1], ranked[6])
        val (qf36H, qf36A) = h2hWins(ranked[2], ranked[5])
        val (qf45H, qf45A) = h2hWins(ranked[3], ranked[4])

        return listOf(
            PlayoffSeries("qf_1_8", "PLAYOFF_SEED", "Projektion VF 1 (#1 vs #8) • Saison-H2H", 1, 8, ranked[0], ranked[7], qf18H, qf18A),
            PlayoffSeries("qf_2_7", "PLAYOFF_SEED", "Projektion VF 2 (#2 vs #7) • Saison-H2H", 2, 7, ranked[1], ranked[6], qf27H, qf27A),
            PlayoffSeries("qf_3_6", "PLAYOFF_SEED", "Projektion VF 3 (#3 vs #6) • Saison-H2H", 3, 6, ranked[2], ranked[5], qf36H, qf36A),
            PlayoffSeries("qf_4_5", "PLAYOFF_SEED", "Projektion VF 4 (#4 vs #5) • Saison-H2H", 4, 5, ranked[3], ranked[4], qf45H, qf45A),
            PlayoffSeries("pi_7_8", "PLAY_IN", "Play-In A (#7 vs #8) • Saison-H2H", 7, 8, ranked[6], ranked[7], pi78H, pi78A),
            PlayoffSeries("pi_9_10", "PLAY_IN", "Play-In B (#9 vs #10) • Saison-H2H", 9, 10, ranked[8], ranked[9], pi910H, pi910A)
        )
    }

    fun simulateLiveRefresh() {
        _games.update { list ->
            list.map { game ->
                if (game.status == GameStatus.LIVE) {
                    val rawHome = (0..3).random()
                    val rawAway = (0..3).random()
                    val addHome = if (rawHome == 0 && rawAway == 0) 2 else rawHome
                    val addAway = rawAway

                    val activeQ = game.currentQuarter.coerceIn(1, 4)
                    val updatedQuarters = when (activeQ) {
                        1 -> game.quarters.copy(
                            q1Home = game.quarters.q1Home + addHome,
                            q1Away = game.quarters.q1Away + addAway
                        )
                        2 -> game.quarters.copy(
                            q2Home = game.quarters.q2Home + addHome,
                            q2Away = game.quarters.q2Away + addAway
                        )
                        3 -> game.quarters.copy(
                            q3Home = game.quarters.q3Home + addHome,
                            q3Away = game.quarters.q3Away + addAway
                        )
                        else -> game.quarters.copy(
                            q4Home = game.quarters.q4Home + addHome,
                            q4Away = game.quarters.q4Away + addAway
                        )
                    }

                    val updatedHome = game.homeScore + addHome
                    val updatedAway = game.awayScore + addAway

                    val clockParts = game.remainingClock.split(":")
                    val curMins = clockParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 2
                    val curSecs = clockParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                    val nextTotalSecs = (curMins * 60 + curSecs) - (18..26).random()

                    val isQuarter4Ended = nextTotalSecs <= 0 && activeQ >= 4
                    val finalHomeScore = if (isQuarter4Ended && updatedHome == updatedAway) updatedHome + 2 else updatedHome
                    val finalQuarters = if (isQuarter4Ended && updatedHome == updatedAway) {
                        updatedQuarters.copy(q4Home = updatedQuarters.q4Home + 2)
                    } else {
                        updatedQuarters
                    }

                    val nextStatus = if (isQuarter4Ended) GameStatus.FINAL else GameStatus.LIVE
                    val (nextQuarter, nextClock) = when {
                        nextTotalSecs > 0 -> activeQ to "%02d:%02d".format(nextTotalSecs / 60, nextTotalSecs % 60)
                        activeQ < 4 -> (activeQ + 1) to "09:42"
                        else -> 4 to "00:00"
                    }

                    val updatedBoxHome = applyLivePointsToBoxscore(game.boxscoreHome, addHome)
                    val updatedBoxAway = applyLivePointsToBoxscore(game.boxscoreAway, addAway)
                    val updatedShots = appendLiveShots(
                        existingShots = game.shots,
                        gameCode = game.gameCode,
                        quarter = activeQ,
                        homeTeamId = game.homeTeam.id,
                        awayTeamId = game.awayTeam.id,
                        boxHome = updatedBoxHome,
                        boxAway = updatedBoxAway,
                        addHome = addHome,
                        addAway = addAway
                    )

                    game.copy(
                        status = nextStatus,
                        currentQuarter = nextQuarter,
                        remainingClock = nextClock,
                        homeScore = finalHomeScore,
                        awayScore = updatedAway,
                        quarters = finalQuarters,
                        boxscoreHome = updatedBoxHome,
                        boxscoreAway = updatedBoxAway,
                        shots = updatedShots
                    )
                } else game
            }
        }
    }

    private fun applyLivePointsToBoxscore(
        boxscore: List<PlayerBoxscoreStat>,
        pointsAdded: Int
    ): List<PlayerBoxscoreStat> {
        if (boxscore.isEmpty() || pointsAdded <= 0) return boxscore
        val scorerIdx = (0 until minOf(boxscore.size, 5)).random()
        return boxscore.mapIndexed { idx, p ->
            if (idx != scorerIdx) p
            else when (pointsAdded) {
                1 -> p.copy(
                    points = p.points + 1,
                    ftMade = p.ftMade + 1,
                    ftAttempted = p.ftAttempted + 1,
                    foulsDrawn = p.foulsDrawn + 1
                )
                2 -> p.copy(
                    points = p.points + 2,
                    twoPtMade = p.twoPtMade + 1,
                    twoPtAttempted = p.twoPtAttempted + 1
                )
                else -> p.copy(
                    points = p.points + pointsAdded,
                    threePtMade = p.threePtMade + 1,
                    threePtAttempted = p.threePtAttempted + 1
                )
            }
        }.sortedByDescending { it.pir }
    }

    private fun appendLiveShots(
        existingShots: List<ShotAttempt>,
        gameCode: Int,
        quarter: Int,
        homeTeamId: String,
        awayTeamId: String,
        boxHome: List<PlayerBoxscoreStat>,
        boxAway: List<PlayerBoxscoreStat>,
        addHome: Int,
        addAway: Int
    ): List<ShotAttempt> {
        val newShots = mutableListOf<ShotAttempt>()
        if (addHome >= 2 && boxHome.isNotEmpty()) {
            val scorer = boxHome.first()
            val isThree = addHome == 3
            newShots.add(
                ShotAttempt(
                    id = "live_${gameCode}_h_${System.nanoTime()}",
                    playerId = scorer.playerId,
                    playerName = scorer.playerName,
                    teamId = homeTeamId,
                    courtX = if (isThree) (15..85).random() / 100f else (32..68).random() / 100f,
                    courtY = if (isThree) (66..82).random() / 100f else (18..48).random() / 100f,
                    made = true,
                    isThreePointer = isThree,
                    quarter = quarter
                )
            )
        }
        if (addAway >= 2 && boxAway.isNotEmpty()) {
            val scorer = boxAway.first()
            val isThree = addAway == 3
            newShots.add(
                ShotAttempt(
                    id = "live_${gameCode}_a_${System.nanoTime()}",
                    playerId = scorer.playerId,
                    playerName = scorer.playerName,
                    teamId = awayTeamId,
                    courtX = if (isThree) (15..85).random() / 100f else (32..68).random() / 100f,
                    courtY = if (isThree) (66..82).random() / 100f else (18..48).random() / 100f,
                    made = true,
                    isThreePointer = isThree,
                    quarter = quarter
                )
            )
        }
        if (newShots.isEmpty()) return existingShots
        return (existingShots + newShots).takeLast(48)
    }

    companion object {
        internal fun canonicalClubId(idOrCode: String): String = when (idOrCode.trim().uppercase()) {
            "BAY", "MUN" -> "MUN"
            "PAO", "PAN" -> "PAN"
            "RMB", "MAD" -> "MAD"
            "FCB", "BAR" -> "BAR"
            "EFS", "IST" -> "IST"
            "FBB", "FBT", "ULK" -> "ULK"
            "ASM", "MCO" -> "MCO"
            "CZV", "RED" -> "RED"
            "EA7", "MIL" -> "MIL"
            "BKN", "KBA", "BAS" -> "BAS"
            "MTA", "MAC", "TEL" -> "TEL"
            "PBB", "PRS" -> "PRS"
            "VBC", "VAL", "PAM" -> "PAM"
            "BJK", "BES" -> "BES"
            "ALB", "BER" -> "BER"
            else -> idOrCode.trim().uppercase()
        }

        internal fun normalizeStreakList(streak: List<String>): List<String> =
            streak.mapNotNull { raw ->
                when (raw.trim().uppercase()) {
                    "W", "S" -> "W"
                    "L", "N" -> "L"
                    else -> null
                }
            }.takeLast(5)

        /**
         * Official EuroLeague Regular Season Bylaws rule:
         * Points scored in overtime periods (`OT`) do NOT count toward `PF` (`PTS+`), `PA` (`PTS-`), or `+/-`
         * in the Regular Season Standings table.
         */
        internal fun regulationStandingsScores(game: EuroLeagueGame): Pair<Int, Int> {
            val q = game.quarters
            val regHome = q.q1Home + q.q2Home + q.q3Home + q.q4Home
            val regAway = q.q1Away + q.q2Away + q.q3Away + q.q4Away
            val wentToOvertime = (q.otHome != null || q.otAway != null) ||
                (regHome > 0 && regAway > 0 && regHome == regAway && (game.homeScore > regHome || game.awayScore > regAway))
            return if (wentToOvertime && regHome > 0 && regAway > 0) {
                regHome to regAway
            } else {
                game.homeScore to game.awayScore
            }
        }

        private fun roundOneDecimal(value: Double): Float =
            (kotlin.math.round(value * 10.0) / 10.0).toFloat()

        private fun parseBoxscoreMinutesToTotal(boxscore: List<PlayerBoxscoreStat>): Double {
            var totalMins = 0.0
            for (p in boxscore) {
                val raw = p.minutes.trim()
                if (":" in raw) {
                    val parts = raw.split(":", limit = 2)
                    val mm = parts[0].trim().toIntOrNull() ?: 0
                    val ss = parts[1].trim().toIntOrNull() ?: 0
                    totalMins += mm + (ss / 60.0)
                } else {
                    totalMins += raw.toDoubleOrNull() ?: 0.0
                }
            }
            return totalMins
        }

        internal fun recomputeOfficialStandingsFromGames(
            teams: List<EuroLeagueTeam>,
            games: List<EuroLeagueGame>
        ): List<EuroLeagueTeam> {
            if (teams.isEmpty()) return teams

            class ClubAccumulator {
                var wins = 0
                var losses = 0
                var regPf = 0
                var regPa = 0
                var totalPf = 0
                var totalPa = 0
                val results = mutableListOf<String>()

                var boxGames = 0
                var gameMinutesEquiv = 0.0
                var boxPts = 0
                var boxOppPts = 0
                var twoM = 0
                var twoA = 0
                var threeM = 0
                var threeA = 0
                var ftA = 0
                var oreb = 0
                var dreb = 0
                var tov = 0
                var oppTwoA = 0
                var oppThreeA = 0
                var oppFtA = 0
                var oppOreb = 0
                var oppDreb = 0
                var oppTov = 0
            }

            val accByClub = mutableMapOf<String, ClubAccumulator>()
            teams.forEach { t ->
                accByClub.getOrPut(canonicalClubId(t.id)) { ClubAccumulator() }
            }

            val finalGames = games.filter {
                it.status == GameStatus.FINAL &&
                    (it.homeScore > 0 || it.awayScore > 0) &&
                    it.homeScore != it.awayScore
            }.sortedWith(
                compareBy<EuroLeagueGame> { it.round }
                    .thenBy { it.tipOffEpochMillis }
                    .thenBy { it.gameCode }
            )

            for (g in finalGames) {
                val homeKey = canonicalClubId(g.homeTeam.id)
                val awayKey = canonicalClubId(g.awayTeam.id)
                val homeAcc = accByClub.getOrPut(homeKey) { ClubAccumulator() }
                val awayAcc = accByClub.getOrPut(awayKey) { ClubAccumulator() }

                val (regH, regA) = regulationStandingsScores(g)
                homeAcc.regPf += regH
                homeAcc.regPa += regA
                awayAcc.regPf += regA
                awayAcc.regPa += regH

                homeAcc.totalPf += g.homeScore
                homeAcc.totalPa += g.awayScore
                awayAcc.totalPf += g.awayScore
                awayAcc.totalPa += g.homeScore

                if (g.homeScore > g.awayScore) {
                    homeAcc.wins += 1
                    awayAcc.losses += 1
                    homeAcc.results.add("W")
                    awayAcc.results.add("L")
                } else {
                    awayAcc.wins += 1
                    homeAcc.losses += 1
                    awayAcc.results.add("W")
                    homeAcc.results.add("L")
                }

                if (g.boxscoreHome.isNotEmpty() && g.boxscoreAway.isNotEmpty()) {
                    val homePlayerMins = parseBoxscoreMinutesToTotal(g.boxscoreHome)
                    val resolvedGameMins = if (homePlayerMins >= 180.0) {
                        maxOf(40.0, kotlin.math.round(homePlayerMins / 5.0))
                    } else if (g.quarters.otHome != null || g.homeScore > regH) {
                        45.0
                    } else {
                        40.0
                    }

                    val h2M = g.boxscoreHome.sumOf { it.twoPtMade }
                    val h2A = g.boxscoreHome.sumOf { it.twoPtAttempted }
                    val h3M = g.boxscoreHome.sumOf { it.threePtMade }
                    val h3A = g.boxscoreHome.sumOf { it.threePtAttempted }
                    val hFtA = g.boxscoreHome.sumOf { it.ftAttempted }
                    val hOreb = g.boxscoreHome.sumOf { it.offRebounds }
                    val hDreb = g.boxscoreHome.sumOf { it.defRebounds }
                    val hTov = g.boxscoreHome.sumOf { it.turnovers }

                    val a2M = g.boxscoreAway.sumOf { it.twoPtMade }
                    val a2A = g.boxscoreAway.sumOf { it.twoPtAttempted }
                    val a3M = g.boxscoreAway.sumOf { it.threePtMade }
                    val a3A = g.boxscoreAway.sumOf { it.threePtAttempted }
                    val aFtA = g.boxscoreAway.sumOf { it.ftAttempted }
                    val aOreb = g.boxscoreAway.sumOf { it.offRebounds }
                    val aDreb = g.boxscoreAway.sumOf { it.defRebounds }
                    val aTov = g.boxscoreAway.sumOf { it.turnovers }

                    homeAcc.boxGames += 1
                    homeAcc.gameMinutesEquiv += resolvedGameMins
                    homeAcc.boxPts += g.homeScore
                    homeAcc.boxOppPts += g.awayScore
                    homeAcc.twoM += h2M
                    homeAcc.twoA += h2A
                    homeAcc.threeM += h3M
                    homeAcc.threeA += h3A
                    homeAcc.ftA += hFtA
                    homeAcc.oreb += hOreb
                    homeAcc.dreb += hDreb
                    homeAcc.tov += hTov
                    homeAcc.oppTwoA += a2A
                    homeAcc.oppThreeA += a3A
                    homeAcc.oppFtA += aFtA
                    homeAcc.oppOreb += aOreb
                    homeAcc.oppDreb += aDreb
                    homeAcc.oppTov += aTov

                    awayAcc.boxGames += 1
                    awayAcc.gameMinutesEquiv += resolvedGameMins
                    awayAcc.boxPts += g.awayScore
                    awayAcc.boxOppPts += g.homeScore
                    awayAcc.twoM += a2M
                    awayAcc.twoA += a2A
                    awayAcc.threeM += a3M
                    awayAcc.threeA += a3A
                    awayAcc.ftA += aFtA
                    awayAcc.oreb += aOreb
                    awayAcc.dreb += aDreb
                    awayAcc.tov += aTov
                    awayAcc.oppTwoA += h2A
                    awayAcc.oppThreeA += h3A
                    awayAcc.oppFtA += hFtA
                    awayAcc.oppOreb += hOreb
                    awayAcc.oppDreb += hDreb
                    awayAcc.oppTov += hTov
                }
            }

            val updatedTeams = teams.map { team ->
                val acc = accByClub[canonicalClubId(team.id)]
                val gp = (acc?.wins ?: 0) + (acc?.losses ?: 0)
                if (acc == null || gp == 0) {
                    team.copy(streak = normalizeStreakList(team.streak))
                } else {
                    val fga = acc.twoA + acc.threeA
                    val oppFga = acc.oppTwoA + acc.oppThreeA
                    val possOwn = fga - acc.oreb + acc.tov + 0.44 * acc.ftA
                    val possOpp = oppFga - acc.oppOreb + acc.oppTov + 0.44 * acc.oppFtA
                    val poss = 0.5 * (possOwn + possOpp)
                    val treb = acc.oreb + acc.dreb
                    val oppTreb = acc.oppOreb + acc.oppDreb

                    val hasBoxStats = acc.boxGames > 0 && poss > 10.0 && acc.gameMinutesEquiv >= 40.0
                    team.copy(
                        wins = acc.wins,
                        losses = acc.losses,
                        pointsFor = acc.regPf,
                        pointsAgainst = acc.regPa,
                        totalPointsFor = acc.totalPf,
                        totalPointsAgainst = acc.totalPa,
                        streak = acc.results.takeLast(5),
                        offensiveRating = if (hasBoxStats) roundOneDecimal((acc.boxPts / poss) * 100.0) else team.offensiveRating,
                        defensiveRating = if (hasBoxStats) roundOneDecimal((acc.boxOppPts / poss) * 100.0) else team.defensiveRating,
                        pace = if (hasBoxStats) roundOneDecimal(40.0 * (poss / acc.gameMinutesEquiv)) else team.pace,
                        twoPointPct = if (hasBoxStats && acc.twoA > 0) roundOneDecimal((acc.twoM.toDouble() / acc.twoA) * 100.0) else team.twoPointPct,
                        threePointPct = if (hasBoxStats && acc.threeA > 0) roundOneDecimal((acc.threeM.toDouble() / acc.threeA) * 100.0) else team.threePointPct,
                        reboundRate = if (hasBoxStats && (treb + oppTreb) > 0) roundOneDecimal((treb.toDouble() / (treb + oppTreb)) * 100.0) else team.reboundRate
                    )
                }
            }

            return sortOfficialStandings(updatedTeams, finalGames)
        }

        internal fun sortOfficialStandings(
            teams: List<EuroLeagueTeam>,
            games: List<EuroLeagueGame>
        ): List<EuroLeagueTeam> {
            if (teams.size <= 1) return teams
            val finalGames = games.filter {
                it.status == GameStatus.FINAL &&
                    (it.homeScore > 0 || it.awayScore > 0) &&
                    it.homeScore != it.awayScore
            }

            // Group teams by Win% (rounded to 1e-6) so mid-round unequal GP (e.g. 7-4 vs 7-5) ranks by Win% per official EuroLeague table
            val winPctGroups = teams.groupBy { t ->
                kotlin.math.round(t.winPercentage * 1_000_000.0).toLong()
            }.toSortedMap(compareByDescending { it })

            val rankedResult = mutableListOf<EuroLeagueTeam>()
            for ((_, group) in winPctGroups) {
                if (group.size == 1) {
                    rankedResult.addAll(group)
                    continue
                }

                val clubKeys = group.map { canonicalClubId(it.id) }.toSet()
                val h2hGames = finalGames.filter { g ->
                    canonicalClubId(g.homeTeam.id) in clubKeys && canonicalClubId(g.awayTeam.id) in clubKeys
                }

                // Official EuroLeague Bylaws: Head-to-Head tiebreaker applies once all tied teams have played each other home and away
                var allPairsComplete = clubKeys.size >= 2
                val clubKeyList = clubKeys.toList()
                for (i in clubKeyList.indices) {
                    for (j in i + 1 until clubKeyList.size) {
                        val a = clubKeyList[i]
                        val b = clubKeyList[j]
                        val pairCount = h2hGames.count { g ->
                            val hk = canonicalClubId(g.homeTeam.id)
                            val ak = canonicalClubId(g.awayTeam.id)
                            (hk == a && ak == b) || (hk == b && ak == a)
                        }
                        if (pairCount < 2) {
                            allPairsComplete = false
                            break
                        }
                    }
                    if (!allPairsComplete) break
                }

                if (allPairsComplete) {
                    val h2hWins = mutableMapOf<String, Int>()
                    val h2hRegDiff = mutableMapOf<String, Int>()
                    for (g in h2hGames) {
                        val hk = canonicalClubId(g.homeTeam.id)
                        val ak = canonicalClubId(g.awayTeam.id)
                        val (regH, regA) = regulationStandingsScores(g)
                        if (g.homeScore > g.awayScore) {
                            h2hWins[hk] = (h2hWins[hk] ?: 0) + 1
                        } else {
                            h2hWins[ak] = (h2hWins[ak] ?: 0) + 1
                        }
                        h2hRegDiff[hk] = (h2hRegDiff[hk] ?: 0) + (regH - regA)
                        h2hRegDiff[ak] = (h2hRegDiff[ak] ?: 0) + (regA - regH)
                    }
                    rankedResult.addAll(
                        group.sortedWith(
                            compareByDescending<EuroLeagueTeam> { h2hWins[canonicalClubId(it.id)] ?: 0 }
                                .thenByDescending { h2hRegDiff[canonicalClubId(it.id)] ?: 0 }
                                .thenByDescending { it.pointDiff }
                                .thenByDescending { it.pointsFor }
                                .thenByDescending { it.wins }
                                .thenBy { it.name }
                        )
                    )
                } else {
                    rankedResult.addAll(
                        group.sortedWith(
                            compareByDescending<EuroLeagueTeam> { it.pointDiff }
                                .thenByDescending { it.pointsFor }
                                .thenByDescending { it.wins }
                                .thenBy { it.name }
                        )
                    )
                }
            }
            return rankedResult
        }

        internal fun refreshGameTeamReferences(
            games: List<EuroLeagueGame>,
            teams: List<EuroLeagueTeam>
        ): List<EuroLeagueGame> {
            val exactById = teams.associateBy { it.id.uppercase() }
            val byCanonical = teams.associateBy { canonicalClubId(it.id) }
            val finalGames = games.filter {
                it.status == GameStatus.FINAL && (it.homeScore > 0 || it.awayScore > 0) && it.homeScore != it.awayScore
            }

            return games.map { g ->
                val resolvedHome = exactById[g.homeTeam.id.uppercase()]
                    ?: byCanonical[canonicalClubId(g.homeTeam.id)]
                    ?: g.homeTeam
                val resolvedAway = exactById[g.awayTeam.id.uppercase()]
                    ?: byCanonical[canonicalClubId(g.awayTeam.id)]
                    ?: g.awayTeam
                val homeKey = canonicalClubId(resolvedHome.id)
                val awayKey = canonicalClubId(resolvedAway.id)

                var hWins = 0
                var aWins = 0
                for (fg in finalGames) {
                    val fhk = canonicalClubId(fg.homeTeam.id)
                    val fak = canonicalClubId(fg.awayTeam.id)
                    if (fhk == homeKey && fak == awayKey) {
                        if (fg.homeScore > fg.awayScore) hWins++ else aWins++
                    } else if (fhk == awayKey && fak == homeKey) {
                        if (fg.homeScore > fg.awayScore) aWins++ else hWins++
                    }
                }

                g.copy(
                    homeTeam = resolvedHome,
                    awayTeam = resolvedAway,
                    headToHeadHomeWins = hWins,
                    headToHeadAwayWins = aWins
                )
            }
        }

        fun buildInitialTeams(): List<EuroLeagueTeam> = listOf(
            EuroLeagueTeam(
                id = "PAN", code = "PAO", name = "Panathinaikos AKTOR Athens",
                city = "Athens", country = "Greece", arena = "OAKA Altion Arena",
                primaryColorHex = 0xFF007A3D, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 684, pointsAgainst = 632,
                streak = listOf("W", "W", "W", "L", "W"),
                offensiveRating = 113.6f, defensiveRating = 104.5f, pace = 74.7f,
                threePointPct = 34.8f, twoPointPct = 57.4f, reboundRate = 51.6f,
                localLogoResName = "club_logo_pan"
            ),
            EuroLeagueTeam(
                id = "MAD", code = "RMB", name = "Real Madrid",
                city = "Madrid", country = "Spain", arena = "Movistar Arena",
                primaryColorHex = 0xFF00529F, secondaryColorHex = 0xFFFEBE10, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 695, pointsAgainst = 640,
                streak = listOf("W", "L", "W", "W", "W"),
                offensiveRating = 121.5f, defensiveRating = 112.3f, pace = 78.9f,
                threePointPct = 39.7f, twoPointPct = 56.0f, reboundRate = 51.6f,
                localLogoResName = "club_logo_mad"
            ),
            EuroLeagueTeam(
                id = "OLY", code = "OLY", name = "Olympiacos Piraeus",
                city = "Piraeus", country = "Greece", arena = "Peace and Friendship Stadium",
                primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 5, losses = 3, pointsFor = 652, pointsAgainst = 612,
                streak = listOf("W", "W", "W", "W", "L"),
                offensiveRating = 116.6f, defensiveRating = 107.1f, pace = 74.1f,
                threePointPct = 33.1f, twoPointPct = 59.8f, reboundRate = 51.4f,
                localLogoResName = "club_logo_oly"
            ),
            EuroLeagueTeam(
                id = "ULK", code = "FBB", name = "Fenerbahce Beko Istanbul",
                city = "Istanbul", country = "Turkiye", arena = "Ulker Sports and Event Hall",
                primaryColorHex = 0xFF002B5C, secondaryColorHex = 0xFFFFED00, accentColorHex = 0xFF008053,
                wins = 6, losses = 2, pointsFor = 668, pointsAgainst = 628,
                streak = listOf("L", "W", "W", "W", "W"),
                offensiveRating = 113.1f, defensiveRating = 104.2f, pace = 74.9f,
                threePointPct = 34.6f, twoPointPct = 57.1f, reboundRate = 50.9f,
                localLogoResName = "club_logo_ulk"
            ),
            EuroLeagueTeam(
                id = "BAR", code = "FCB", name = "FC Barcelona",
                city = "Barcelona", country = "Spain", arena = "Palau Blaugrana",
                primaryColorHex = 0xFFA50044, secondaryColorHex = 0xFF004D98, accentColorHex = 0xFFEDBB00,
                wins = 6, losses = 2, pointsFor = 674, pointsAgainst = 638,
                streak = listOf("W", "W", "L", "W", "W"),
                offensiveRating = 116.6f, defensiveRating = 114.2f, pace = 75.8f,
                threePointPct = 36.8f, twoPointPct = 57.6f, reboundRate = 51.5f,
                localLogoResName = "club_logo_bar"
            ),
            EuroLeagueTeam(
                id = "MUN", code = "BAY", name = "FC Bayern Munich",
                city = "Munich", country = "Germany", arena = "SAP Garden",
                primaryColorHex = 0xFFDC052D, secondaryColorHex = 0xFF0066B2, accentColorHex = 0xFFFFFFFF,
                wins = 5, losses = 3, pointsFor = 678, pointsAgainst = 664,
                streak = listOf("W", "W", "W", "L", "W"),
                offensiveRating = 113.8f, defensiveRating = 123.9f, pace = 77.5f,
                threePointPct = 36.7f, twoPointPct = 57.9f, reboundRate = 50.1f,
                localLogoResName = "club_logo_mun"
            ),
            EuroLeagueTeam(
                id = "BAY", code = "BAY", name = "FC Bayern Munich",
                city = "Munich", country = "Germany", arena = "SAP Garden",
                primaryColorHex = 0xFFDC052D, secondaryColorHex = 0xFF0066B2, accentColorHex = 0xFFFFFFFF,
                wins = 5, losses = 3, pointsFor = 678, pointsAgainst = 664,
                streak = listOf("W", "W", "W", "L", "W"),
                offensiveRating = 113.8f, defensiveRating = 123.9f, pace = 77.5f,
                threePointPct = 36.7f, twoPointPct = 57.9f, reboundRate = 50.1f,
                localLogoResName = "club_logo_mun"
            ),
            EuroLeagueTeam(
                id = "PAO", code = "PAO", name = "Panathinaikos AKTOR Athens",
                city = "Athens", country = "Greece", arena = "OAKA Altion Arena",
                primaryColorHex = 0xFF007A3D, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 684, pointsAgainst = 632,
                streak = listOf("W", "W", "W", "L", "W"),
                offensiveRating = 113.6f, defensiveRating = 104.5f, pace = 74.7f,
                threePointPct = 34.8f, twoPointPct = 57.4f, reboundRate = 51.6f,
                localLogoResName = "club_logo_pan"
            ),
            EuroLeagueTeam(
                id = "ZAL", code = "ZAL", name = "Zalgiris Kaunas",
                city = "Kaunas", country = "Lithuania", arena = "Zalgirio Arena",
                primaryColorHex = 0xFF006233, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 6, losses = 2, pointsFor = 645, pointsAgainst = 608,
                streak = listOf("L", "W", "W", "W", "W"),
                offensiveRating = 113.9f, defensiveRating = 109.7f, pace = 76.7f,
                threePointPct = 39.4f, twoPointPct = 50.9f, reboundRate = 55.0f,
                localLogoResName = "club_logo_zal"
            ),
            EuroLeagueTeam(
                id = "PAR", code = "PAR", name = "Partizan Mozzart Bet Belgrade",
                city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
                primaryColorHex = 0xFF1A1A1A, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 4, losses = 4, pointsFor = 655, pointsAgainst = 648,
                streak = listOf("W", "L", "L", "W", "W"),
                offensiveRating = 105.7f, defensiveRating = 104.9f, pace = 73.2f,
                threePointPct = 29.8f, twoPointPct = 53.9f, reboundRate = 47.6f,
                localLogoResName = "club_logo_par"
            ),
            EuroLeagueTeam(
                id = "IST", code = "EFS", name = "Anadolu Efes Istanbul",
                city = "Istanbul", country = "Turkiye", arena = "Basketball Development Center",
                primaryColorHex = 0xFF0033A0, secondaryColorHex = 0xFFE30613, accentColorHex = 0xFFFFFFFF,
                wins = 4, losses = 4, pointsFor = 660, pointsAgainst = 652,
                streak = listOf("L", "W", "W", "L", "W"),
                offensiveRating = 109.6f, defensiveRating = 112.0f, pace = 77.6f,
                threePointPct = 35.4f, twoPointPct = 52.7f, reboundRate = 48.1f,
                localLogoResName = "club_logo_ist"
            ),
            EuroLeagueTeam(
                id = "RED", code = "CZV", name = "Crvena Zvezda Meridianbet Belgrade",
                city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
                primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF0033A0,
                wins = 4, losses = 4, pointsFor = 648, pointsAgainst = 642,
                streak = listOf("W", "W", "L", "W", "L"),
                offensiveRating = 109.2f, defensiveRating = 104.0f, pace = 75.0f,
                threePointPct = 36.6f, twoPointPct = 52.3f, reboundRate = 49.7f,
                localLogoResName = "club_logo_red"
            ),
            EuroLeagueTeam(
                id = "MIL", code = "EA7", name = "EA7 Emporio Armani Milan",
                city = "Milan", country = "Italy", arena = "Unipol Forum",
                primaryColorHex = 0xFFD6001C, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 3, losses = 5, pointsFor = 630, pointsAgainst = 655,
                streak = listOf("L", "L", "W", "L", "W"),
                offensiveRating = 108.9f, defensiveRating = 117.1f, pace = 75.4f,
                threePointPct = 35.0f, twoPointPct = 51.6f, reboundRate = 48.2f,
                localLogoResName = "club_logo_mil"
            ),
            EuroLeagueTeam(
                id = "BAS", code = "BKN", name = "Baskonia Vitoria-Gasteiz",
                city = "Vitoria-Gasteiz", country = "Spain", arena = "Buesa Arena",
                primaryColorHex = 0xFFA6192E, secondaryColorHex = 0xFF00205B, accentColorHex = 0xFFD4AF37,
                wins = 4, losses = 4, pointsFor = 640, pointsAgainst = 645,
                streak = listOf("W", "L", "W", "L", "L"),
                offensiveRating = 103.9f, defensiveRating = 118.5f, pace = 77.0f,
                threePointPct = 29.2f, twoPointPct = 55.2f, reboundRate = 48.4f,
                localLogoResName = "club_logo_bas"
            ),
            EuroLeagueTeam(
                id = "VIR", code = "VIR", name = "Virtus Segafredo Bologna",
                city = "Bologna", country = "Italy", arena = "Virtus Segafredo Arena",
                primaryColorHex = 0xFF141414, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 2, losses = 6, pointsFor = 620, pointsAgainst = 660,
                streak = listOf("L", "W", "L", "L", "L"),
                offensiveRating = 110.0f, defensiveRating = 114.9f, pace = 78.1f,
                threePointPct = 31.2f, twoPointPct = 53.6f, reboundRate = 50.4f,
                localLogoResName = "club_logo_vir"
            ),
            EuroLeagueTeam(
                id = "TEL", code = "MTA", name = "Maccabi Rapyd Tel Aviv",
                city = "Tel Aviv", country = "Israel", arena = "Menora Mivtachim Arena",
                primaryColorHex = 0xFFFFD100, secondaryColorHex = 0xFF0033A0, accentColorHex = 0xFFFFFFFF,
                wins = 3, losses = 5, pointsFor = 650, pointsAgainst = 680,
                streak = listOf("W", "L", "L", "W", "L"),
                offensiveRating = 110.4f, defensiveRating = 119.9f, pace = 76.4f,
                threePointPct = 34.2f, twoPointPct = 56.6f, reboundRate = 51.5f,
                localLogoResName = "club_logo_tel"
            ),
            EuroLeagueTeam(
                id = "PRS", code = "PRS", name = "Paris Basketball",
                city = "Paris", country = "France", arena = "Adidas Arena",
                primaryColorHex = 0xFF111116, secondaryColorHex = 0xFF00B2A9, accentColorHex = 0xFFE30613,
                wins = 5, losses = 3, pointsFor = 670, pointsAgainst = 655,
                streak = listOf("L", "W", "W", "W", "L"),
                offensiveRating = 112.6f, defensiveRating = 115.9f, pace = 79.8f,
                threePointPct = 34.4f, twoPointPct = 58.7f, reboundRate = 48.1f,
                localLogoResName = "club_logo_prs"
            ),
            EuroLeagueTeam(
                id = "ASV", code = "ASV", name = "LDLC ASVEL Villeurbanne",
                city = "Villeurbanne", country = "France", arena = "LDLC Arena",
                primaryColorHex = 0xFF1E1E24, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF00A859,
                wins = 2, losses = 6, pointsFor = 615, pointsAgainst = 670,
                streak = listOf("L", "L", "W", "L", "L"),
                offensiveRating = 112.5f, defensiveRating = 121.5f, pace = 78.2f,
                threePointPct = 41.0f, twoPointPct = 54.8f, reboundRate = 44.1f,
                localLogoResName = "club_logo_asv"
            ),
            EuroLeagueTeam(
                id = "PAM", code = "VBC", name = "Valencia Basket",
                city = "Valencia", country = "Spain", arena = "Roig Arena / Fuente de San Luis",
                primaryColorHex = 0xFFFF6600, secondaryColorHex = 0xFF1A1A1A, accentColorHex = 0xFFFFFFFF,
                wins = 7, losses = 2, pointsFor = 835, pointsAgainst = 768,
                streak = listOf("W", "L", "W", "W", "W"),
                offensiveRating = 119.6f, defensiveRating = 109.7f, pace = 78.4f,
                threePointPct = 39.2f, twoPointPct = 53.3f, reboundRate = 52.6f,
                localLogoResName = "club_logo_pam"
            ),
            EuroLeagueTeam(
                id = "DUB", code = "DUB", name = "Dubai Basketball",
                city = "Dubai", country = "UAE", arena = "Coca-Cola Arena",
                primaryColorHex = 0xFFC59B27, secondaryColorHex = 0xFF121216, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 3, pointsFor = 774, pointsAgainst = 760,
                streak = listOf("L", "W", "W", "L", "W"),
                offensiveRating = 116.6f, defensiveRating = 114.9f, pace = 73.8f,
                threePointPct = 36.3f, twoPointPct = 54.7f, reboundRate = 52.5f,
                localLogoResName = "club_logo_dub"
            ),
            EuroLeagueTeam(
                id = "HTA", code = "HTA", name = "Hapoel IBI Tel Aviv",
                city = "Tel Aviv", country = "Israel", arena = "Shlomo Group Arena",
                primaryColorHex = 0xFFD9001B, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF1A1A1A,
                wins = 5, losses = 4, pointsFor = 805, pointsAgainst = 780,
                streak = listOf("W", "L", "L", "L", "W"),
                offensiveRating = 116.8f, defensiveRating = 112.7f, pace = 77.0f,
                threePointPct = 38.1f, twoPointPct = 56.3f, reboundRate = 48.6f,
                localLogoResName = "club_logo_hta"
            ),
            EuroLeagueTeam(
                id = "MCO", code = "ASM", name = "AS Monaco",
                city = "Monaco", country = "Monaco", arena = "Salle Gaston Medecin",
                primaryColorHex = 0xFFE4002B, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 6, pointsFor = 1054, pointsAgainst = 1005,
                streak = listOf("W", "W", "L", "L", "L"),
                offensiveRating = 114.6f, defensiveRating = 109.7f, pace = 76.2f,
                threePointPct = 36.1f, twoPointPct = 56.1f, reboundRate = 48.0f,
                localLogoResName = "club_logo_mco"
            )
        )

        fun buildInitialPlayers(): List<PlayerProfile> = listOf(
            PlayerProfile("012774", "PAN", 25, "Kendrick Nunn", "G", "USA", 190, 20.4f, 3.6f, 4.2f, 19.8f, InjuryStatus.FIT),
            PlayerProfile("009266", "PAN", 26, "Mathias Lessort", "C", "FRA", 206, 14.2f, 6.8f, 1.5f, 19.5f, InjuryStatus.FIT),
            PlayerProfile("001926", "PAN", 10, "Kostas Sloukas", "G", "GRE", 190, 11.8f, 2.7f, 6.1f, 15.9f, InjuryStatus.FIT),
            PlayerProfile("004088", "PAN", 16, "Cedi Osman", "F", "TUR", 204, 12.9f, 3.8f, 1.6f, 13.4f, InjuryStatus.FIT),
            PlayerProfile("011988", "MUN", 3, "Carsen Edwards", "G", "USA", 180, 19.9f, 2.4f, 3.3f, 17.6f, InjuryStatus.FIT),
            PlayerProfile("011988_bay", "BAY", 3, "Carsen Edwards", "G", "USA", 180, 19.9f, 2.4f, 3.3f, 17.6f, InjuryStatus.FIT),
            PlayerProfile("009862", "MUN", 0, "Nick Weiler-Babb", "G", "GER", 196, 8.9f, 4.5f, 5.4f, 14.1f, InjuryStatus.FIT),
            PlayerProfile("007032", "MUN", 13, "Andreas Obst", "G", "GER", 191, 11.4f, 1.9f, 1.6f, 10.8f, InjuryStatus.FIT),
            PlayerProfile("007432", "MUN", 7, "Johannes Voigtmann", "C", "GER", 211, 7.8f, 5.2f, 2.4f, 11.2f, InjuryStatus.FIT),
            PlayerProfile("005982", "MAD", 7, "Facundo Campazzo", "G", "ARG", 180, 13.5f, 3.1f, 7.4f, 19.2f, InjuryStatus.OUT, "Hamstring"),
            PlayerProfile("007982", "MAD", 22, "Walter Tavares", "C", "CPV", 220, 10.8f, 7.5f, 1.4f, 18.4f, InjuryStatus.FIT),
            PlayerProfile("006540", "OLY", 14, "Sasha Vezenkov", "F", "BUL", 206, 18.9f, 6.7f, 1.9f, 22.4f, InjuryStatus.FIT),
            PlayerProfile("005985", "IST", 55, "Mike James", "G", "USA", 185, 17.8f, 4.0f, 5.8f, 19.1f, InjuryStatus.OUT, "Achilles")
        )

        fun buildInitialGames(teams: List<EuroLeagueTeam>): List<EuroLeagueGame> {
            val now = Instant.now()
            val bay = teams.find { it.id == "BAY" } ?: teams.find { it.id == "MUN" } ?: teams.first()
            val pao = teams.find { it.id == "PAO" } ?: teams.find { it.id == "PAN" } ?: teams[1]
            val rmb = teams.find { it.id == "MAD" || it.id == "RMB" } ?: teams[1]
            val oly = teams.find { it.id == "OLY" } ?: teams[2]
            val fbb = teams.find { it.id == "ULK" || it.id == "FBB" } ?: teams[3]
            val bar = teams.find { it.id == "BAR" || it.id == "FCB" } ?: teams[4]
            val zal = teams.find { it.id == "ZAL" } ?: teams.last()

            val bayBox = listOf(
                PlayerBoxscoreStat("p5", "Carsen Edwards", 3, bay.id, "31:42", 26, 1, 3, 5, 2, 0, 2, 5, 9, 4, 9, 4, 4, 5, 2),
                PlayerBoxscoreStat("p8", "Andreas Obst", 13, bay.id, "25:10", 15, 0, 2, 2, 0, 0, 1, 0, 1, 5, 8, 0, 0, 1, 2),
                PlayerBoxscoreStat("p6", "Nick Weiler-Babb", 0, bay.id, "30:05", 11, 2, 5, 8, 3, 1, 1, 2, 4, 2, 4, 1, 2, 3, 2),
                PlayerBoxscoreStat("p9", "Johannes Voigtmann", 7, bay.id, "22:48", 9, 2, 5, 4, 0, 1, 2, 3, 5, 1, 3, 0, 0, 2, 3)
            )

            val paoBox = listOf(
                PlayerBoxscoreStat("p1", "Kendrick Nunn", 25, pao.id, "33:10", 27, 1, 3, 6, 2, 0, 3, 6, 11, 4, 8, 3, 3, 6, 3),
                PlayerBoxscoreStat("p2", "Mathias Lessort", 26, pao.id, "29:50", 19, 4, 6, 2, 1, 2, 2, 7, 10, 0, 0, 5, 7, 7, 3),
                PlayerBoxscoreStat("p3", "Kostas Sloukas", 10, pao.id, "27:30", 14, 0, 3, 9, 1, 0, 2, 2, 4, 2, 5, 4, 4, 4, 1),
                PlayerBoxscoreStat("p4", "Cedi Osman", 16, pao.id, "26:20", 12, 3, 5, 1, 1, 1, 0, 3, 5, 2, 4, 0, 0, 2, 2)
            )

            val sampleShots = listOf(
                ShotAttempt("s1", "p5", "Carsen Edwards", bay.id, 0.18f, 0.72f, true, true, 4),
                ShotAttempt("s2", "p8", "Andreas Obst", bay.id, 0.82f, 0.68f, true, true, 4),
                ShotAttempt("s4", "p5", "Carsen Edwards", bay.id, 0.50f, 0.81f, false, true, 2),
                ShotAttempt("s5", "p1", "Kendrick Nunn", pao.id, 0.25f, 0.74f, true, true, 4),
                ShotAttempt("s6", "p2", "Mathias Lessort", pao.id, 0.52f, 0.15f, true, false, 4),
                ShotAttempt("s7", "p3", "Kostas Sloukas", pao.id, 0.76f, 0.69f, false, true, 3),
                ShotAttempt("s8", "p6", "Nick Weiler-Babb", bay.id, 0.10f, 0.30f, true, true, 1)
            )

            return listOf(
                EuroLeagueGame(
                    id = "g_live_1", round = 11, homeTeam = bay, awayTeam = pao,
                    tipOffEpochMillis = now.minus(105, ChronoUnit.MINUTES).toEpochMilli(),
                    status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "01:42",
                    homeScore = 84, awayScore = 82,
                    quarters = QuarterScores(22, 20, 21, 24, 19, 18, 22, 20),
                    headToHeadHomeWins = 6, headToHeadAwayWins = 9,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 105, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_live_2", round = 11, homeTeam = rmb, awayTeam = oly,
                    tipOffEpochMillis = now.minus(95, ChronoUnit.MINUTES).toEpochMilli(),
                    status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "02:18",
                    homeScore = 79, awayScore = 77,
                    quarters = QuarterScores(20, 19, 22, 21, 18, 20, 19, 17),
                    headToHeadHomeWins = 21, headToHeadAwayWins = 17,
                    boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots,
                    gameCode = 107, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_final_1", round = 11, homeTeam = fbb, awayTeam = bar,
                    tipOffEpochMillis = now.minus(5, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.FINAL, currentQuarter = 5, remainingClock = "00:00",
                    homeScore = 96, awayScore = 92,
                    quarters = QuarterScores(24, 21, 19, 22, 20, 18, 21, 23, 12, 8),
                    headToHeadHomeWins = 7, headToHeadAwayWins = 5,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 110, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_upcoming_1", round = 12, homeTeam = bay, awayTeam = bar,
                    tipOffEpochMillis = now.plus(26, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                    homeScore = 0, awayScore = 0,
                    quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                    headToHeadHomeWins = 5, headToHeadAwayWins = 11,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 112, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_upcoming_2", round = 12, homeTeam = pao, awayTeam = zal,
                    tipOffEpochMillis = now.plus(50, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                    homeScore = 0, awayScore = 0,
                    quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                    headToHeadHomeWins = 18, headToHeadAwayWins = 8,
                    boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots,
                    gameCode = 119, seasonCode = "E2026"
                )
            )
        }
    }
}
