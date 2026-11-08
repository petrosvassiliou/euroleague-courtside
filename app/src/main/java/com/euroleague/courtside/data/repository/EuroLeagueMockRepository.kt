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
    val reboundRate: Float
) {
    val primaryColor: Color get() = Color(primaryColorHex)
    val secondaryColor: Color get() = Color(secondaryColorHex)
    val accentColor: Color get() = Color(accentColorHex)
    val pointDiff: Int get() = pointsFor - pointsAgainst
}

enum class InjuryStatus(val label: String) {
    FIT("Einsatzbereit"),
    GAME_TIME_DECISION("Fraglich (GTD)"),
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
    val injuryStatus: InjuryStatus = InjuryStatus.FIT
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
)

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
    val shots: List<ShotAttempt>
) {
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

// --- MOCK REPOSITORY MIT ALLEN 18 OFFIZIELLEN EUROLEAGUE-TEAMS & VEREINSFARBEN ---

@Singleton
class EuroLeagueMockRepository @Inject constructor() {

    val teams: List<EuroLeagueTeam> = listOf(
        EuroLeagueTeam(
            id = "PAO", code = "PAO", name = "Panathinaikos AKTOR Athens",
            city = "Athens", country = "Greece", arena = "OAKA Altion Arena",
            primaryColorHex = 0xFF007A3D, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
            wins = 24, losses = 8, pointsFor = 2740, pointsAgainst = 2515,
            streak = listOf("S", "S", "S", "N", "S"),
            offensiveRating = 119.4f, defensiveRating = 109.8f, pace = 73.5f,
            threePointPct = 39.2f, twoPointPct = 57.4f, reboundRate = 52.8f
        ),
        EuroLeagueTeam(
            id = "RMB", code = "RMB", name = "Real Madrid",
            city = "Madrid", country = "Spain", arena = "WiZink Center",
            primaryColorHex = 0xFF00529F, secondaryColorHex = 0xFFFEBE10, accentColorHex = 0xFFFFFFFF,
            wins = 23, losses = 9, pointsFor = 2785, pointsAgainst = 2560,
            streak = listOf("S", "N", "S", "S", "S"),
            offensiveRating = 120.1f, defensiveRating = 111.2f, pace = 74.8f,
            threePointPct = 38.5f, twoPointPct = 58.9f, reboundRate = 53.5f
        ),
        EuroLeagueTeam(
            id = "OLY", code = "OLY", name = "Olympiacos Piraeus",
            city = "Piraeus", country = "Greece", arena = "Peace and Friendship Stadium",
            primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
            wins = 22, losses = 10, pointsFor = 2610, pointsAgainst = 2430,
            streak = listOf("S", "S", "S", "S", "N"),
            offensiveRating = 117.2f, defensiveRating = 107.1f, pace = 70.4f,
            threePointPct = 37.8f, twoPointPct = 56.5f, reboundRate = 51.9f
        ),
        EuroLeagueTeam(
            id = "FBB", code = "FBB", name = "Fenerbahce Beko Istanbul",
            city = "Istanbul", country = "Turkey", arena = "Ulker Sports and Event Hall",
            primaryColorHex = 0xFF002B5C, secondaryColorHex = 0xFFFFED00, accentColorHex = 0xFF008053,
            wins = 21, losses = 11, pointsFor = 2695, pointsAgainst = 2555,
            streak = listOf("N", "S", "S", "S", "N"),
            offensiveRating = 118.0f, defensiveRating = 112.0f, pace = 72.9f,
            threePointPct = 39.7f, twoPointPct = 55.1f, reboundRate = 51.2f
        ),
        EuroLeagueTeam(
            id = "ASM", code = "ASM", name = "AS Monaco",
            city = "Monaco", country = "Monaco", arena = "Salle Gaston Medecin",
            primaryColorHex = 0xFFCE1126, secondaryColorHex = 0xFFD4AF37, accentColorHex = 0xFFFFFFFF,
            wins = 20, losses = 12, pointsFor = 2670, pointsAgainst = 2575,
            streak = listOf("S", "N", "S", "N", "S"),
            offensiveRating = 116.8f, defensiveRating = 112.5f, pace = 73.1f,
            threePointPct = 36.9f, twoPointPct = 56.0f, reboundRate = 50.8f
        ),
        EuroLeagueTeam(
            id = "FCB", code = "FCB", name = "FC Barcelona",
            city = "Barcelona", country = "Spain", arena = "Palau Blaugrana",
            primaryColorHex = 0xFFA50044, secondaryColorHex = 0xFF004D98, accentColorHex = 0xFFEDBB00,
            wins = 20, losses = 12, pointsFor = 2660, pointsAgainst = 2580,
            streak = listOf("S", "S", "N", "N", "S"),
            offensiveRating = 116.4f, defensiveRating = 112.9f, pace = 73.2f,
            threePointPct = 38.1f, twoPointPct = 55.8f, reboundRate = 51.5f
        ),
        EuroLeagueTeam(
            id = "BAY", code = "BAY", name = "FC Bayern Munich",
            city = "Munich", country = "Germany", arena = "SAP Garden",
            primaryColorHex = 0xFFDC052D, secondaryColorHex = 0xFF0066B2, accentColorHex = 0xFFFFFFFF,
            wins = 19, losses = 13, pointsFor = 2645, pointsAgainst = 2590,
            streak = listOf("S", "S", "S", "N", "S"),
            offensiveRating = 116.2f, defensiveRating = 113.8f, pace = 74.1f,
            threePointPct = 39.4f, twoPointPct = 54.6f, reboundRate = 50.2f
        ),
        EuroLeagueTeam(
            id = "ZAL", code = "ZAL", name = "Zalgiris Kaunas",
            city = "Kaunas", country = "Lithuania", arena = "Zalgirio Arena",
            primaryColorHex = 0xFF006233, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
            wins = 18, losses = 14, pointsFor = 2540, pointsAgainst = 2510,
            streak = listOf("N", "S", "N", "S", "S"),
            offensiveRating = 113.9f, defensiveRating = 111.8f, pace = 70.8f,
            threePointPct = 38.0f, twoPointPct = 53.9f, reboundRate = 50.5f
        ),
        EuroLeagueTeam(
            id = "PAR", code = "PAR", name = "Partizan Mozzart Bet Belgrade",
            city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
            primaryColorHex = 0xFF1A1A1A, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
            wins = 17, losses = 15, pointsFor = 2620, pointsAgainst = 2600,
            streak = listOf("S", "N", "N", "S", "S"),
            offensiveRating = 115.7f, defensiveRating = 114.6f, pace = 72.5f,
            threePointPct = 37.5f, twoPointPct = 55.2f, reboundRate = 49.4f
        ),
        EuroLeagueTeam(
            id = "EFS", code = "EFS", name = "Anadolu Efes Istanbul",
            city = "Istanbul", country = "Turkey", arena = "Basketball Development Center",
            primaryColorHex = 0xFF0033A0, secondaryColorHex = 0xFFE30613, accentColorHex = 0xFFFFFFFF,
            wins = 16, losses = 16, pointsFor = 2635, pointsAgainst = 2630,
            streak = listOf("N", "S", "S", "N", "N"),
            offensiveRating = 115.9f, defensiveRating = 115.4f, pace = 73.0f,
            threePointPct = 37.2f, twoPointPct = 55.0f, reboundRate = 49.8f
        ),
        EuroLeagueTeam(
            id = "CZV", code = "CZV", name = "Crvena Zvezda Meridianbet Belgrade",
            city = "Belgrade", country = "Serbia", arena = "Belgrade Arena",
            primaryColorHex = 0xFFE30613, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF0033A0,
            wins = 16, losses = 16, pointsFor = 2590, pointsAgainst = 2588,
            streak = listOf("S", "S", "N", "S", "N"),
            offensiveRating = 114.8f, defensiveRating = 114.2f, pace = 71.5f,
            threePointPct = 36.9f, twoPointPct = 54.8f, reboundRate = 51.0f
        ),
        EuroLeagueTeam(
            id = "EA7", code = "EA7", name = "EA7 Emporio Armani Milan",
            city = "Milan", country = "Italy", arena = "Unipol Forum",
            primaryColorHex = 0xFFD6001C, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
            wins = 15, losses = 17, pointsFor = 2520, pointsAgainst = 2555,
            streak = listOf("N", "N", "S", "N", "S"),
            offensiveRating = 113.2f, defensiveRating = 114.9f, pace = 70.6f,
            threePointPct = 36.8f, twoPointPct = 53.5f, reboundRate = 49.1f
        ),
        EuroLeagueTeam(
            id = "BKN", code = "BKN", name = "Baskonia Vitoria-Gasteiz",
            city = "Vitoria-Gasteiz", country = "Spain", arena = "Buesa Arena",
            primaryColorHex = 0xFFA6192E, secondaryColorHex = 0xFF00205B, accentColorHex = 0xFFD4AF37,
            wins = 15, losses = 17, pointsFor = 2610, pointsAgainst = 2640,
            streak = listOf("S", "N", "S", "N", "N"),
            offensiveRating = 115.1f, defensiveRating = 116.2f, pace = 74.0f,
            threePointPct = 37.9f, twoPointPct = 54.1f, reboundRate = 50.0f
        ),
        EuroLeagueTeam(
            id = "VIR", code = "VIR", name = "Virtus Segafredo Bologna",
            city = "Bologna", country = "Italy", arena = "Virtus Segafredo Arena",
            primaryColorHex = 0xFF141414, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFFD4AF37,
            wins = 14, losses = 18, pointsFor = 2515, pointsAgainst = 2580,
            streak = listOf("N", "S", "N", "N", "S"),
            offensiveRating = 112.8f, defensiveRating = 115.6f, pace = 71.2f,
            threePointPct = 36.1f, twoPointPct = 53.8f, reboundRate = 49.0f
        ),
        EuroLeagueTeam(
            id = "MTA", code = "MTA", name = "Maccabi Playtika Tel Aviv",
            city = "Tel Aviv", country = "Israel", arena = "Menora Mivtachim Arena",
            primaryColorHex = 0xFFFFD100, secondaryColorHex = 0xFF0033A0, accentColorHex = 0xFFFFFFFF,
            wins = 14, losses = 18, pointsFor = 2665, pointsAgainst = 2710,
            streak = listOf("S", "N", "N", "S", "N"),
            offensiveRating = 116.0f, defensiveRating = 117.8f, pace = 75.1f,
            threePointPct = 36.7f, twoPointPct = 54.9f, reboundRate = 50.4f
        ),
        EuroLeagueTeam(
            id = "PRS", code = "PRS", name = "Paris Basketball",
            city = "Paris", country = "France", arena = "Adidas Arena",
            primaryColorHex = 0xFF111116, secondaryColorHex = 0xFF00B2A9, accentColorHex = 0xFFE30613,
            wins = 13, losses = 19, pointsFor = 2630, pointsAgainst = 2690,
            streak = listOf("N", "S", "S", "N", "N"),
            offensiveRating = 114.5f, defensiveRating = 117.1f, pace = 76.2f,
            threePointPct = 37.0f, twoPointPct = 53.6f, reboundRate = 48.7f
        ),
        EuroLeagueTeam(
            id = "ASV", code = "ASV", name = "LDLC ASVEL Villeurbanne",
            city = "Villeurbanne", country = "France", arena = "Astroballe / LDLC Arena",
            primaryColorHex = 0xFF1E1E24, secondaryColorHex = 0xFFFFFFFF, accentColorHex = 0xFF00A859,
            wins = 11, losses = 21, pointsFor = 2490, pointsAgainst = 2650,
            streak = listOf("N", "N", "S", "N", "N"),
            offensiveRating = 110.9f, defensiveRating = 118.0f, pace = 73.4f,
            threePointPct = 35.4f, twoPointPct = 52.7f, reboundRate = 48.5f
        ),
        EuroLeagueTeam(
            id = "BER", code = "BER", name = "ALBA Berlin",
            city = "Berlin", country = "Germany", arena = "Uber Arena",
            primaryColorHex = 0xFFFFCC00, secondaryColorHex = 0xFF002F6C, accentColorHex = 0xFFFFFFFF,
            wins = 9, losses = 23, pointsFor = 2430, pointsAgainst = 2690,
            streak = listOf("N", "N", "N", "S", "N"),
            offensiveRating = 108.5f, defensiveRating = 119.2f, pace = 75.4f,
            threePointPct = 34.6f, twoPointPct = 51.8f, reboundRate = 47.9f
        )
    )

    val players: List<PlayerProfile> = listOf(
        PlayerProfile("p1", "PAO", 25, "Kendrick Nunn", "SG", "USA", 190, 20.4f, 3.6f, 4.2f, 19.8f, InjuryStatus.FIT),
        PlayerProfile("p2", "PAO", 26, "Mathias Lessort", "C", "FRA", 206, 14.2f, 6.8f, 1.5f, 19.5f, InjuryStatus.FIT),
        PlayerProfile("p3", "PAO", 10, "Kostas Sloukas", "PG", "GRE", 190, 11.8f, 2.7f, 6.1f, 15.9f, InjuryStatus.FIT),
        PlayerProfile("p4", "PAO", 41, "Juancho Hernangomez", "PF", "ESP", 206, 9.4f, 5.9f, 1.2f, 12.6f, InjuryStatus.GAME_TIME_DECISION),
        PlayerProfile("p5", "BAY", 3, "Carsen Edwards", "SG", "USA", 180, 19.9f, 2.4f, 3.3f, 17.6f, InjuryStatus.FIT),
        PlayerProfile("p6", "BAY", 0, "Nick Weiler-Babb", "PG", "GER", 196, 8.9f, 4.5f, 5.4f, 14.1f, InjuryStatus.FIT),
        PlayerProfile("p7", "BAY", 31, "Devin Booker", "C", "USA", 205, 15.1f, 5.6f, 1.8f, 17.2f, InjuryStatus.FIT),
        PlayerProfile("p8", "BAY", 13, "Andreas Obst", "SG", "GER", 191, 11.4f, 1.9f, 1.6f, 10.8f, InjuryStatus.FIT),
        PlayerProfile("p9", "BAY", 7, "Johannes Voigtmann", "C", "GER", 211, 7.8f, 5.2f, 2.4f, 11.2f, InjuryStatus.GAME_TIME_DECISION),
        PlayerProfile("p10", "RMB", 7, "Facundo Campazzo", "PG", "ARG", 180, 13.5f, 3.1f, 7.4f, 19.2f, InjuryStatus.FIT),
        PlayerProfile("p11", "RMB", 22, "Walter Tavares", "C", "CPV", 220, 10.8f, 7.5f, 1.4f, 18.4f, InjuryStatus.FIT),
        PlayerProfile("p12", "RMB", 11, "Mario Hezonja", "SF", "CRO", 203, 15.6f, 5.1f, 1.7f, 16.5f, InjuryStatus.FIT),
        PlayerProfile("p13", "OLY", 14, "Sasha Vezenkov", "PF", "BUL", 206, 18.9f, 6.7f, 1.9f, 22.4f, InjuryStatus.FIT),
        PlayerProfile("p14", "ASM", 55, "Mike James", "PG", "USA", 185, 17.8f, 4.0f, 5.8f, 19.1f, InjuryStatus.FIT)
    )

    private val _favoriteTeamIds = MutableStateFlow(setOf("BAY", "PAO"))
    val favoriteTeamIds: StateFlow<Set<String>> = _favoriteTeamIds.asStateFlow()

    private val _selectedTeamId = MutableStateFlow("BAY")
    val selectedTeamId: StateFlow<String> = _selectedTeamId.asStateFlow()

    private val _games = MutableStateFlow(buildInitialGames())
    val games: StateFlow<List<EuroLeagueGame>> = _games.asStateFlow()

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
        teams.sortedWith(compareByDescending<EuroLeagueTeam> { it.wins }.thenByDescending { it.pointDiff })

    fun getPlayoffBracket(): List<PlayoffSeries> {
        val ranked = getStandings()
        return listOf(
            PlayoffSeries("pi_7_8", "PLAY_IN", "Play-In A (7 vs 8)", 7, 8, ranked[6], ranked[7], 1, 0),
            PlayoffSeries("pi_9_10", "PLAY_IN", "Play-In B (9 vs 10)", 9, 10, ranked[8], ranked[9], 1, 0),
            PlayoffSeries("qf_1_8", "QUARTERFINALS", "Best-of-5 VF 1", 1, 8, ranked[0], ranked[7], 3, 1),
            PlayoffSeries("qf_2_7", "QUARTERFINALS", "Best-of-5 VF 2", 2, 7, ranked[1], ranked[6], 3, 2),
            PlayoffSeries("qf_3_6", "QUARTERFINALS", "Best-of-5 VF 3", 3, 6, ranked[2], ranked[5], 3, 1),
            PlayoffSeries("qf_4_5", "QUARTERFINALS", "Best-of-5 VF 4", 4, 5, ranked[3], ranked[4], 2, 3),
            PlayoffSeries("f4_s1", "FINAL_FOUR_SEMI", "Final Four Halbfinale 1", 1, 5, ranked[0], ranked[4], 1, 0),
            PlayoffSeries("f4_s2", "FINAL_FOUR_SEMI", "Final Four Halbfinale 2", 2, 3, ranked[1], ranked[2], 0, 1)
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

    private fun buildInitialGames(): List<EuroLeagueGame> {
        val now = Instant.now()
        val bay = teams.first { it.id == "BAY" }
        val pao = teams.first { it.id == "PAO" }
        val rmb = teams.first { it.id == "RMB" }
        val oly = teams.first { it.id == "OLY" }
        val fbb = teams.first { it.id == "FBB" }
        val asm = teams.first { it.id == "ASM" }
        val fcb = teams.first { it.id == "FCB" }
        val zal = teams.first { it.id == "ZAL" }

        val bayBox = listOf(
            PlayerBoxscoreStat("p5", "Carsen Edwards", 3, "BAY", "31:42", 26, 1, 3, 5, 2, 0, 2, 5, 9, 4, 9, 4, 4, 5, 2),
            PlayerBoxscoreStat("p7", "Devin Booker", 31, "BAY", "28:15", 18, 3, 6, 2, 1, 1, 1, 6, 10, 1, 2, 3, 4, 4, 3),
            PlayerBoxscoreStat("p8", "Andreas Obst", 13, "BAY", "25:10", 15, 0, 2, 2, 0, 0, 1, 0, 1, 5, 8, 0, 0, 1, 2),
            PlayerBoxscoreStat("p6", "Nick Weiler-Babb", 0, "BAY", "30:05", 11, 2, 5, 8, 3, 1, 1, 2, 4, 2, 4, 1, 2, 3, 2),
            PlayerBoxscoreStat("p9", "Johannes Voigtmann", 7, "BAY", "22:48", 9, 2, 5, 4, 0, 1, 2, 3, 5, 1, 3, 0, 0, 2, 3)
        )

        val paoBox = listOf(
            PlayerBoxscoreStat("p1", "Kendrick Nunn", 25, "PAO", "33:10", 27, 1, 3, 6, 2, 0, 3, 6, 11, 4, 8, 3, 3, 6, 3),
            PlayerBoxscoreStat("p2", "Mathias Lessort", 26, "PAO", "29:50", 19, 4, 6, 2, 1, 2, 2, 7, 10, 0, 0, 5, 7, 7, 3),
            PlayerBoxscoreStat("p3", "Kostas Sloukas", 10, "PAO", "27:30", 14, 0, 3, 9, 1, 0, 2, 2, 4, 2, 5, 4, 4, 4, 1),
            PlayerBoxscoreStat("p4", "Juancho Hernangomez", 41, "PAO", "26:20", 12, 3, 5, 1, 1, 1, 0, 3, 5, 2, 4, 0, 0, 2, 2)
        )

        val sampleShots = listOf(
            ShotAttempt("s1", "p5", "Carsen Edwards", "BAY", 0.18f, 0.72f, true, true, 4),
            ShotAttempt("s2", "p8", "Andreas Obst", "BAY", 0.82f, 0.68f, true, true, 4),
            ShotAttempt("s3", "p7", "Devin Booker", "BAY", 0.48f, 0.22f, true, false, 3),
            ShotAttempt("s4", "p5", "Carsen Edwards", "BAY", 0.50f, 0.81f, false, true, 2),
            ShotAttempt("s5", "p1", "Kendrick Nunn", "PAO", 0.25f, 0.74f, true, true, 4),
            ShotAttempt("s6", "p2", "Mathias Lessort", "PAO", 0.52f, 0.15f, true, false, 4),
            ShotAttempt("s7", "p3", "Kostas Sloukas", "PAO", 0.76f, 0.69f, false, true, 3),
            ShotAttempt("s8", "p6", "Nick Weiler-Babb", "BAY", 0.10f, 0.30f, true, true, 1)
        )

        return listOf(
            EuroLeagueGame(
                id = "g_live_1", round = 33, homeTeam = bay, awayTeam = pao,
                tipOffEpochMillis = now.minus(105, ChronoUnit.MINUTES).toEpochMilli(),
                status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "01:42",
                homeScore = 84, awayScore = 82,
                quarters = QuarterScores(22, 20, 21, 24, 19, 18, 22, 20),
                headToHeadHomeWins = 6, headToHeadAwayWins = 9,
                boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots
            ),
            EuroLeagueGame(
                id = "g_live_2", round = 33, homeTeam = rmb, awayTeam = oly,
                tipOffEpochMillis = now.minus(95, ChronoUnit.MINUTES).toEpochMilli(),
                status = GameStatus.LIVE, currentQuarter = 4, remainingClock = "02:18",
                homeScore = 79, awayScore = 77,
                quarters = QuarterScores(20, 19, 22, 21, 18, 20, 19, 17),
                headToHeadHomeWins = 21, headToHeadAwayWins = 17,
                boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots
            ),
            EuroLeagueGame(
                id = "g_final_1", round = 33, homeTeam = fbb, awayTeam = asm,
                tipOffEpochMillis = now.minus(5, ChronoUnit.HOURS).toEpochMilli(),
                status = GameStatus.FINAL, currentQuarter = 5, remainingClock = "00:00",
                homeScore = 96, awayScore = 92,
                quarters = QuarterScores(24, 21, 19, 22, 20, 18, 21, 23, 12, 8),
                headToHeadHomeWins = 7, headToHeadAwayWins = 5,
                boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots
            ),
            EuroLeagueGame(
                id = "g_upcoming_1", round = 34, homeTeam = bay, awayTeam = fcb,
                tipOffEpochMillis = now.plus(26, ChronoUnit.HOURS).toEpochMilli(),
                status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                homeScore = 0, awayScore = 0,
                quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                headToHeadHomeWins = 5, headToHeadAwayWins = 11,
                boxscoreHome = bayBox, boxscoreAway = paoBox, shots = sampleShots
            ),
            EuroLeagueGame(
                id = "g_upcoming_2", round = 34, homeTeam = pao, awayTeam = zal,
                tipOffEpochMillis = now.plus(50, ChronoUnit.HOURS).toEpochMilli(),
                status = GameStatus.UPCOMING, currentQuarter = 1, remainingClock = "10:00",
                homeScore = 0, awayScore = 0,
                quarters = QuarterScores(0, 0, 0, 0, 0, 0, 0, 0),
                headToHeadHomeWins = 18, headToHeadAwayWins = 8,
                boxscoreHome = paoBox, boxscoreAway = bayBox, shots = sampleShots
            )
        )
    }
}
