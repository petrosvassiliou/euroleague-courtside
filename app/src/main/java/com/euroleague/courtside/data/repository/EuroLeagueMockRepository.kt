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
    val localLogoResName: String = "club_logo_${id.lowercase()}"
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)
    val accentColor: Color get() = Color(accentColorHex)
    val pointDiff: Int get() = pointsFor - pointsAgainst
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
    val foulsCommitted: Int
) {
    val totalRebounds: Int get() = offRebounds + defRebounds
    val twoPtPct: Float get() = if (twoPtAttempted > 0) (twoPtMade * 100f) / twoPtAttempted else 0f
    val threePtPct: Float get() = if (threePtAttempted > 0) (threePtMade * 100f) / threePtAttempted else 0f
    val ftPct: Float get() = if (ftAttempted > 0) (ftMade * 100f) / ftAttempted else 0f

    val pir: Int
        get() {
            val missedFg = (twoPtAttempted - twoPtMade) + (threePtAttempted - threePtMade)
            val missedFt = ftAttempted - ftMade
            return (points + totalRebounds + assists + steals + blocks + foulsDrawn) -
                    (missedFg + missedFt + turnovers + foulsCommitted)
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

    private val _activeRound = MutableStateFlow(8)
    val activeRoundFlow: StateFlow<Int> = _activeRound.asStateFlow()

    private val _seasonCode = MutableStateFlow("E2026")
    val seasonCodeFlow: StateFlow<String> = _seasonCode.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("LIVE API • Saison 2026/27 • Kader & Verletzungen geprüft")
    val lastSyncStatusFlow: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private val _appLanguage = MutableStateFlow(com.euroleague.courtside.i18n.AppLanguage.fromCode(null))
    val appLanguageFlow: StateFlow<com.euroleague.courtside.i18n.AppLanguage> = _appLanguage.asStateFlow()

    fun selectLanguage(language: com.euroleague.courtside.i18n.AppLanguage) {
        _appLanguage.value = language
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
        if (liveTeams.isNotEmpty()) {
            _teams.value = liveTeams
        }
        if (livePlayers.isNotEmpty()) {
            _players.value = livePlayers
        }
        if (liveGames.isNotEmpty()) {
            _games.value = liveGames
        }
        if (activeRound > 0) {
            _activeRound.value = activeRound
        }
        if (seasonCode.isNotBlank()) {
            _seasonCode.value = seasonCode
        }
        val statusText = syncStatus.ifBlank { syncLabel }
        if (statusText.isNotBlank()) {
            _lastSyncStatus.value = statusText
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
        _games.update { currentList ->
            currentList.map { game ->
                if (game.id == updatedGame.id) updatedGame else game
            }
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
        _games.update { currentList ->
            currentList.map { game ->
                if (game.id == gameId) {
                    game.copy(
                        homeScore = homeScore ?: game.homeScore,
                        awayScore = awayScore ?: game.awayScore,
                        quarters = quarters ?: game.quarters,
                        status = status ?: game.status,
                        boxscoreHome = if (boxHome.isNotEmpty()) boxHome else game.boxscoreHome,
                        boxscoreAway = if (boxAway.isNotEmpty()) boxAway else game.boxscoreAway,
                        shots = if (shots.isNotEmpty()) shots else game.shots
                    )
                } else game
            }
        }
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

    fun getStandings(): List<EuroLeagueTeam> =
        _teams.value.sortedWith(compareByDescending<EuroLeagueTeam> { it.wins }.thenByDescending { it.pointDiff })

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
                    val addHome = (0..3).random()
                    val addAway = (0..3).random()
                    val updatedHome = game.homeScore + addHome
                    val updatedAway = game.awayScore + addAway
                    game.copy(
                        homeScore = updatedHome,
                        awayScore = updatedAway,
                        quarters = game.quarters.copy(
                            q4Home = game.quarters.q4Home + addHome,
                            q4Away = game.quarters.q4Away + addAway
                        )
                    )
                } else game
            }
        }
    }

    companion object {
        fun buildInitialTeams(): List<EuroLeagueTeam> = listOf(
            EuroLeagueTeam(
                id = "PAN", code = "PAO", name = "Panathinaikos AKTOR Athens",
                city = "Athens", country = "Greece", arena = "OAKA Altion Arena",
                primaryColorHex = 0xFF007A3D, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 684, pointsAgainst = 632,
                streak = listOf("S", "S", "S", "N", "S"),
                offensiveRating = 119.4f, defensiveRating = 109.8f, pace = 73.5f,
                threePointPct = 39.2f, twoPointPct = 57.4f, reboundRate = 52.8f,
                localLogoResName = "club_logo_pan"
            ),
            EuroLeagueTeam(
                id = "MAD", code = "RMB", name = "Real Madrid",
                city = "Madrid", country = "Spain", arena = "Movistar Arena",
                primaryColorHex = 0xFF00529F, secondaryColorHex = 0xFFFEBE10, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 695, pointsAgainst = 640,
                streak = listOf("S", "N", "S", "S", "S"),
                offensiveRating = 120.1f, defensiveRating = 111.2f, pace = 74.8f,
                threePointPct = 38.5f, twoPointPct = 58.9f, reboundRate = 53.5f,
                localLogoResName = "club_logo_mad"
            ),
            EuroLeagueTeam(
                id = "OLY", code = "OLY", name = "Olympiacos Piraeus",
                city = "Piraeus", country = "Greece", arena = "Peace and Friendship Stadium",
                primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 5, losses = 3, pointsFor = 652, pointsAgainst = 612,
                streak = listOf("S", "S", "S", "S", "N"),
                offensiveRating = 117.2f, defensiveRating = 107.1f, pace = 70.4f,
                threePointPct = 37.8f, twoPointPct = 56.5f, reboundRate = 51.9f,
                localLogoResName = "club_logo_oly"
            ),
            EuroLeagueTeam(
                id = "ULK", code = "FBB", name = "Fenerbahce Beko Istanbul",
                city = "Istanbul", country = "Turkiye", arena = "Ulker Sports and Event Hall",
                primaryColorHex = 0xFF002B5C, secondaryColorHex = 0xFFFFED00, accentColorHex = 0xFF008053,
                wins = 6, losses = 2, pointsFor = 668, pointsAgainst = 628,
                streak = listOf("N", "S", "S", "S", "S"),
                offensiveRating = 118.0f, defensiveRating = 112.0f, pace = 72.9f,
                threePointPct = 39.7f, twoPointPct = 55.1f, reboundRate = 51.2f,
                localLogoResName = "club_logo_ulk"
            ),
            EuroLeagueTeam(
                id = "BAR", code = "FCB", name = "FC Barcelona",
                city = "Barcelona", country = "Spain", arena = "Palau Blaugrana",
                primaryColorHex = 0xFFA50044, secondaryColorHex = 0xFF004D98, accentColorHex = 0xFFEDBB00,
                wins = 6, losses = 2, pointsFor = 674, pointsAgainst = 638,
                streak = listOf("S", "S", "N", "S", "S"),
                offensiveRating = 116.4f, defensiveRating = 112.9f, pace = 73.2f,
                threePointPct = 38.1f, twoPointPct = 55.8f, reboundRate = 51.5f,
                localLogoResName = "club_logo_bar"
            ),
            EuroLeagueTeam(
                id = "MUN", code = "BAY", name = "FC Bayern Munich",
                city = "Munich", country = "Germany", arena = "SAP Garden",
                primaryColorHex = 0xFFDC052D, secondaryColorHex = 0xFF0066B2, accentColorHex = 0xFFFFFFFF,
                wins = 5, losses = 3, pointsFor = 678, pointsAgainst = 664,
                streak = listOf("S", "S", "S", "N", "S"),
                offensiveRating = 116.2f, defensiveRating = 113.8f, pace = 74.1f,
                threePointPct = 39.4f, twoPointPct = 54.6f, reboundRate = 50.2f,
                localLogoResName = "club_logo_mun"
            ),
            EuroLeagueTeam(
                id = "BAY", code = "BAY", name = "FC Bayern Munich",
                city = "Munich", country = "Germany", arena = "SAP Garden",
                primaryColorHex = 0xFFDC052D, secondaryColorHex = 0xFF0066B2, accentColorHex = 0xFFFFFFFF,
                wins = 5, losses = 3, pointsFor = 678, pointsAgainst = 664,
                streak = listOf("S", "S", "S", "N", "S"),
                offensiveRating = 116.2f, defensiveRating = 113.8f, pace = 74.1f,
                threePointPct = 39.4f, twoPointPct = 54.6f, reboundRate = 50.2f,
                localLogoResName = "club_logo_mun"
            ),
            EuroLeagueTeam(
                id = "PAO", code = "PAO", name = "Panathinaikos AKTOR Athens",
                city = "Athens", country = "Greece", arena = "OAKA Altion Arena",
                primaryColorHex = 0xFF007A3D, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
                wins = 6, losses = 2, pointsFor = 684, pointsAgainst = 632,
                streak = listOf("S", "S", "S", "N", "S"),
                offensiveRating = 119.4f, defensiveRating = 109.8f, pace = 73.5f,
                threePointPct = 39.2f, twoPointPct = 57.4f, reboundRate = 52.8f,
                localLogoResName = "club_logo_pan"
            ),
            EuroLeagueTeam(
                id = "ZAL", code = "ZAL", name = "Zalgiris Kaunas",
                city = "Kaunas", country = "Lithuania", arena = "Zalgirio Arena",
                primaryColorHex = 0xFF006233, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 6, losses = 2, pointsFor = 645, pointsAgainst = 608,
                streak = listOf("N", "S", "S", "S", "S"),
                offensiveRating = 113.9f, defensiveRating = 111.8f, pace = 70.8f,
                threePointPct = 38.0f, twoPointPct = 53.9f, reboundRate = 50.5f,
                localLogoResName = "club_logo_zal"
            ),
            EuroLeagueTeam(
                id = "PAR", code = "PAR", name = "Partizan Mozzart Bet Belgrade",
                city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
                primaryColorHex = 0xFF1A1A1A, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 4, losses = 4, pointsFor = 655, pointsAgainst = 648,
                streak = listOf("S", "N", "N", "S", "S"),
                offensiveRating = 115.7f, defensiveRating = 114.6f, pace = 72.5f,
                threePointPct = 37.5f, twoPointPct = 55.2f, reboundRate = 49.4f,
                localLogoResName = "club_logo_par"
            ),
            EuroLeagueTeam(
                id = "IST", code = "EFS", name = "Anadolu Efes Istanbul",
                city = "Istanbul", country = "Turkiye", arena = "Basketball Development Center",
                primaryColorHex = 0xFF0033A0, secondaryColorHex = 0xFFE30613, accentColorHex = 0xFFFFFFFF,
                wins = 4, losses = 4, pointsFor = 660, pointsAgainst = 652,
                streak = listOf("N", "S", "S", "N", "S"),
                offensiveRating = 115.9f, defensiveRating = 115.4f, pace = 73.0f,
                threePointPct = 37.2f, twoPointPct = 55.0f, reboundRate = 49.8f,
                localLogoResName = "club_logo_ist"
            ),
            EuroLeagueTeam(
                id = "RED", code = "CZV", name = "Crvena Zvezda Meridianbet Belgrade",
                city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
                primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF0033A0,
                wins = 4, losses = 4, pointsFor = 648, pointsAgainst = 642,
                streak = listOf("S", "S", "N", "S", "N"),
                offensiveRating = 114.8f, defensiveRating = 114.2f, pace = 71.5f,
                threePointPct = 36.9f, twoPointPct = 54.8f, reboundRate = 51.0f,
                localLogoResName = "club_logo_red"
            ),
            EuroLeagueTeam(
                id = "MIL", code = "EA7", name = "EA7 Emporio Armani Milan",
                city = "Milan", country = "Italy", arena = "Unipol Forum",
                primaryColorHex = 0xFFD6001C, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 3, losses = 5, pointsFor = 630, pointsAgainst = 655,
                streak = listOf("N", "N", "S", "N", "S"),
                offensiveRating = 113.2f, defensiveRating = 114.9f, pace = 70.6f,
                threePointPct = 36.8f, twoPointPct = 53.5f, reboundRate = 49.1f,
                localLogoResName = "club_logo_mil"
            ),
            EuroLeagueTeam(
                id = "BAS", code = "BKN", name = "Baskonia Vitoria-Gasteiz",
                city = "Vitoria-Gasteiz", country = "Spain", arena = "Buesa Arena",
                primaryColorHex = 0xFFA6192E, secondaryColorHex = 0xFF00205B, accentColorHex = 0xFFD4AF37,
                wins = 4, losses = 4, pointsFor = 640, pointsAgainst = 645,
                streak = listOf("S", "N", "S", "N", "N"),
                offensiveRating = 115.1f, defensiveRating = 116.2f, pace = 74.0f,
                threePointPct = 37.9f, twoPointPct = 54.1f, reboundRate = 50.0f,
                localLogoResName = "club_logo_bas"
            ),
            EuroLeagueTeam(
                id = "VIR", code = "VIR", name = "Virtus Segafredo Bologna",
                city = "Bologna", country = "Italy", arena = "Virtus Segafredo Arena",
                primaryColorHex = 0xFF141414, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
                wins = 2, losses = 6, pointsFor = 620, pointsAgainst = 660,
                streak = listOf("N", "S", "N", "N", "N"),
                offensiveRating = 112.8f, defensiveRating = 115.6f, pace = 71.2f,
                threePointPct = 36.1f, twoPointPct = 53.8f, reboundRate = 49.0f,
                localLogoResName = "club_logo_vir"
            ),
            EuroLeagueTeam(
                id = "TEL", code = "MTA", name = "Maccabi Rapyd Tel Aviv",
                city = "Tel Aviv", country = "Israel", arena = "Menora Mivtachim Arena",
                primaryColorHex = 0xFFFFD100, secondaryColorHex = 0xFF0033A0, accentColorHex = 0xFFFFFFFF,
                wins = 3, losses = 5, pointsFor = 650, pointsAgainst = 680,
                streak = listOf("S", "N", "N", "S", "N"),
                offensiveRating = 116.0f, defensiveRating = 117.8f, pace = 75.1f,
                threePointPct = 36.7f, twoPointPct = 54.9f, reboundRate = 50.4f,
                localLogoResName = "club_logo_tel"
            ),
            EuroLeagueTeam(
                id = "PRS", code = "PRS", name = "Paris Basketball",
                city = "Paris", country = "France", arena = "Adidas Arena",
                primaryColorHex = 0xFF111116, secondaryColorHex = 0xFF00B2A9, accentColorHex = 0xFFE30613,
                wins = 5, losses = 3, pointsFor = 670, pointsAgainst = 655,
                streak = listOf("N", "S", "S", "S", "N"),
                offensiveRating = 114.5f, defensiveRating = 117.1f, pace = 76.2f,
                threePointPct = 37.0f, twoPointPct = 53.6f, reboundRate = 48.7f,
                localLogoResName = "club_logo_prs"
            ),
            EuroLeagueTeam(
                id = "ASV", code = "ASV", name = "LDLC ASVEL Villeurbanne",
                city = "Villeurbanne", country = "France", arena = "LDLC Arena",
                primaryColorHex = 0xFF1E1E24, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF00A859,
                wins = 2, losses = 6, pointsFor = 615, pointsAgainst = 670,
                streak = listOf("N", "N", "S", "N", "N"),
                offensiveRating = 110.9f, defensiveRating = 118.0f, pace = 73.4f,
                threePointPct = 35.4f, twoPointPct = 52.7f, reboundRate = 48.5f,
                localLogoResName = "club_logo_asv"
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
                    id = "g_live_1", round = 8, homeTeam = bay, awayTeam = pao,
                    tipOffEpochMillis = now.minus(105, ChronoUnit.MINUTES).toEpochMilli(),
                    status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "01:42",
                    homeScore = 84, awayScore = 82,
                    quarters = QuarterScores(22, 20, 21, 24, 19, 18, 22, 20),
                    headToHeadHomeWins = 6, headToHeadAwayWins = 9,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 65, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_live_2", round = 8, homeTeam = rmb, awayTeam = oly,
                    tipOffEpochMillis = now.minus(95, ChronoUnit.MINUTES).toEpochMilli(),
                    status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "02:18",
                    homeScore = 79, awayScore = 77,
                    quarters = QuarterScores(20, 19, 22, 21, 18, 20, 19, 17),
                    headToHeadHomeWins = 21, headToHeadAwayWins = 17,
                    boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots,
                    gameCode = 68, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_final_1", round = 8, homeTeam = fbb, awayTeam = bar,
                    tipOffEpochMillis = now.minus(5, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.FINAL, currentQuarter = 5, remainingClock = "00:00",
                    homeScore = 96, awayScore = 92,
                    quarters = QuarterScores(24, 21, 19, 22, 20, 18, 21, 23, 12, 8),
                    headToHeadHomeWins = 7, headToHeadAwayWins = 5,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 70, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_upcoming_1", round = 9, homeTeam = bay, awayTeam = bar,
                    tipOffEpochMillis = now.plus(26, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                    homeScore = 0, awayScore = 0,
                    quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                    headToHeadHomeWins = 5, headToHeadAwayWins = 11,
                    boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots,
                    gameCode = 75, seasonCode = "E2026"
                ),
                EuroLeagueGame(
                    id = "g_upcoming_2", round = 9, homeTeam = pao, awayTeam = zal,
                    tipOffEpochMillis = now.plus(50, ChronoUnit.HOURS).toEpochMilli(),
                    status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                    homeScore = 0, awayScore = 0,
                    quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                    headToHeadHomeWins = 18, headToHeadAwayWins = 8,
                    boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots,
                    gameCode = 78, seasonCode = "E2026"
                )
            )
        }
    }
}
