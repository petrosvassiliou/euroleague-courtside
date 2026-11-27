package com.euroleague.courtside.data.remote

import android.content.Context
import com.euroleague.courtside.data.local.CachedGameEntity
import com.euroleague.courtside.data.local.CachedPlayerEntity
import com.euroleague.courtside.data.local.CachedTeamEntity
import com.euroleague.courtside.data.local.CourtsideDao
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.InjuryStatus
import com.euroleague.courtside.data.repository.PlayByPlayEvent
import com.euroleague.courtside.data.repository.PlayEventCategory
import com.euroleague.courtside.data.repository.PlayerBoxscoreStat
import com.euroleague.courtside.data.repository.PlayerProfile
import com.euroleague.courtside.data.repository.QuarterScores
import com.euroleague.courtside.data.repository.ShotAttempt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// =========================================================================
// 1. OFFIZIELLE EUROLEAGUE LIVE-API DTOs (api-live.euroleague.net v2 / v3)
// =========================================================================

@Serializable
data class EuroLeagueClubsEnvelopeDto(
    @SerialName("data") val data: List<EuroLeagueClubDto> = emptyList()
)

@Serializable
data class EuroLeagueClubDto(
    @SerialName("code") val code: String = "",
    @SerialName("tvCode") val tvCode: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("alias") val alias: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("country") val country: EuroLeagueCountryDto? = null,
    @SerialName("venue") val venue: EuroLeagueVenueDto? = null,
    @SerialName("images") val images: EuroLeagueClubImagesDto? = null
)

@Serializable
data class EuroLeagueCountryDto(
    @SerialName("code") val code: String? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class EuroLeagueVenueDto(
    @SerialName("name") val name: String? = null,
    @SerialName("capacity") val capacity: Int? = null,
    @SerialName("address") val address: String? = null
)

@Serializable
data class EuroLeagueClubImagesDto(
    @SerialName("crest") val crest: String? = null
)

@Serializable
data class EuroLeagueClubPersonDto(
    @SerialName("person") val person: EuroLeaguePersonDetailDto? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("typeName") val typeName: String? = null,
    @SerialName("active") val active: Boolean = true,
    @SerialName("dorsal") val dorsal: String? = null,
    @SerialName("positionName") val positionName: String? = null,
    @SerialName("images") val images: EuroLeaguePersonImagesDto? = null
)

@Serializable
data class EuroLeaguePersonDetailDto(
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("passportName") val passportName: String? = null,
    @SerialName("passportSurname") val passportSurname: String? = null,
    @SerialName("jerseyName") val jerseyName: String? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("country") val country: EuroLeagueCountryDto? = null
)

@Serializable
data class EuroLeaguePersonImagesDto(
    @SerialName("headshot") val headshot: String? = null,
    @SerialName("action") val action: String? = null
)

@Serializable
data class EuroLeagueRoundsEnvelopeDto(
    @SerialName("data") val data: List<EuroLeagueRoundDto> = emptyList()
)

@Serializable
data class EuroLeagueRoundDto(
    @SerialName("round") val round: Int = 1,
    @SerialName("phaseTypeCode") val phaseTypeCode: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("minGameStartDate") val minGameStartDate: String? = null,
    @SerialName("maxGameStartDate") val maxGameStartDate: String? = null
)

@Serializable
data class EuroLeagueGamesEnvelopeDto(
    @SerialName("data") val data: List<EuroLeagueRemoteGameDto> = emptyList()
)

@Serializable
data class EuroLeagueRemoteGameDto(
    @SerialName("id") val id: String? = null,
    @SerialName("gameCode") val gameCode: Int = 1,
    @SerialName("round") val round: Int = 1,
    @SerialName("played") val played: Boolean = false,
    @SerialName("utcDate") val utcDate: String? = null,
    @SerialName("gameStatus") val gameStatus: String? = null,
    @SerialName("local") val local: EuroLeagueGameSideDto = EuroLeagueGameSideDto(),
    @SerialName("road") val road: EuroLeagueGameSideDto = EuroLeagueGameSideDto(),
    @SerialName("venue") val venue: EuroLeagueVenueDto? = null
)

@Serializable
data class EuroLeagueGameSideDto(
    @SerialName("club") val club: EuroLeagueClubDto = EuroLeagueClubDto(),
    @SerialName("score") val score: Int? = null,
    @SerialName("partials") val partials: EuroLeaguePartialsDto? = null
)

@Serializable
data class EuroLeaguePartialsDto(
    @SerialName("partials1") val partials1: Int? = null,
    @SerialName("partials2") val partials2: Int? = null,
    @SerialName("partials3") val partials3: Int? = null,
    @SerialName("partials4") val partials4: Int? = null
)

@Serializable
data class EuroLeagueGameBoxscoreDto(
    @SerialName("local") val local: EuroLeagueTeamBoxscoreSectionDto? = null,
    @SerialName("road") val road: EuroLeagueTeamBoxscoreSectionDto? = null
)

@Serializable
data class EuroLeagueTeamBoxscoreSectionDto(
    @SerialName("players") val players: List<EuroLeagueBoxscorePlayerEntryDto> = emptyList()
)

@Serializable
data class EuroLeagueBoxscorePlayerEntryDto(
    @SerialName("player") val player: EuroLeagueBoxscorePlayerInfoDto = EuroLeagueBoxscorePlayerInfoDto(),
    @SerialName("stats") val stats: EuroLeagueBoxscoreStatsDto = EuroLeagueBoxscoreStatsDto()
)

@Serializable
data class EuroLeagueBoxscorePlayerInfoDto(
    @SerialName("dorsal") val dorsal: String? = null,
    @SerialName("positionName") val positionName: String? = null,
    @SerialName("person") val person: EuroLeaguePersonDetailDto = EuroLeaguePersonDetailDto()
)

@Serializable
data class EuroLeagueBoxscoreStatsDto(
    @SerialName("timePlayed") val timePlayed: Double? = null,
    @SerialName("points") val points: Double? = null,
    @SerialName(" valuation") val valuationAlt: Double? = null,
    @SerialName("valuation") val valuation: Double? = null,
    @SerialName("offensiveRebounds") val offensiveRebounds: Double? = null,
    @SerialName("defensiveRebounds") val defensiveRebounds: Double? = null,
    @SerialName("totalRebounds") val totalRebounds: Double? = null,
    @SerialName("assistances") val assistances: Double? = null,
    @SerialName("steals") val steals: Double? = null,
    @SerialName("blocksFavour") val blocksFavour: Double? = null,
    @SerialName("blocksAgainst") val blocksAgainst: Double? = null,
    @SerialName("turnovers") val turnovers: Double? = null,
    @SerialName("fieldGoalsMade2") val fieldGoalsMade2: Double? = null,
    @SerialName("fieldGoalsAttempted2") val fieldGoalsAttempted2: Double? = null,
    @SerialName("fieldGoalsMade3") val fieldGoalsMade3: Double? = null,
    @SerialName("fieldGoalsAttempted3") val fieldGoalsAttempted3: Double? = null,
    @SerialName("freeThrowsMade") val freeThrowsMade: Double? = null,
    @SerialName("freeThrowsAttempted") val freeThrowsAttempted: Double? = null,
    @SerialName("foulsReceived") val foulsReceived: Double? = null,
    @SerialName("foulsCommited") val foulsCommited: Double? = null
)

@Serializable
data class EuroLeaguePlayerStatsEnvelopeDto(
    @SerialName("players") val players: List<EuroLeaguePlayerTraditionalDto> = emptyList()
)

@Serializable
data class EuroLeaguePlayerTraditionalDto(
    @SerialName("player") val player: EuroLeagueStatPlayerMetaDto = EuroLeagueStatPlayerMetaDto(),
    @SerialName("pointsScored") val pointsScored: Double = 0.0,
    @SerialName("totalRebounds") val totalRebounds: Double = 0.0,
    @SerialName("assists") val assists: Double = 0.0,
    @SerialName("pir") val pir: Double = 0.0
)

@Serializable
data class EuroLeagueStatPlayerMetaDto(
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = ""
)

@Serializable
data class EuroLeagueTeamStatsEnvelopeDto(
    @SerialName("teams") val teams: List<EuroLeagueTeamTraditionalDto> = emptyList()
)

@Serializable
data class EuroLeagueTeamTraditionalDto(
    @SerialName("team") val team: EuroLeagueStatTeamMetaDto = EuroLeagueStatTeamMetaDto(),
    @SerialName("pointsScored") val pointsScored: Double = 0.0,
    @SerialName("twoPointersAttempted") val twoPointersAttempted: Double = 0.0,
    @SerialName("twoPointersPercentage") val twoPointersPercentage: String? = null,
    @SerialName("threePointersAttempted") val threePointersAttempted: Double = 0.0,
    @SerialName("threePointersPercentage") val threePointersPercentage: String? = null,
    @SerialName("freeThrowsAttempted") val freeThrowsAttempted: Double = 0.0,
    @SerialName("offensiveRebounds") val offensiveRebounds: Double = 0.0,
    @SerialName("defensiveRebounds") val defensiveRebounds: Double = 0.0,
    @SerialName("totalRebounds") val totalRebounds: Double = 0.0,
    @SerialName("turnovers") val turnovers: Double = 0.0,
    @SerialName("pir") val pir: Double = 0.0
)

@Serializable
data class EuroLeagueStatTeamMetaDto(
    @SerialName("code") val code: String = "",
    @SerialName("tvCodes") val tvCodes: String? = null,
    @SerialName("name") val name: String = ""
)

@Serializable
data class EuroLeagueLiveBoxscoreEnvelopeDto(
    @SerialName("Stats") val stats: List<EuroLeagueLiveTeamBoxscoreDto> = emptyList()
)

@Serializable
data class EuroLeagueLiveTeamBoxscoreDto(
    @SerialName("Team") val team: String = "",
    @SerialName("PlayersStats") val playersStats: List<EuroLeagueLivePlayerStatDto> = emptyList()
)

@Serializable
data class EuroLeagueLivePlayerStatDto(
    @SerialName("Player_ID") val playerId: String = "",
    @SerialName("Team") val team: String = "",
    @SerialName("Dorsal") val dorsal: String = "0",
    @SerialName("Player") val player: String = "",
    @SerialName("Minutes") val minutes: String = "00:00",
    @SerialName("Points") val points: Int = 0,
    @SerialName("FieldGoalsMade2") val fgm2: Int = 0,
    @SerialName("FieldGoalsAttempted2") val fga2: Int = 0,
    @SerialName("FieldGoalsMade3") val fgm3: Int = 0,
    @SerialName("FieldGoalsAttempted3") val fga3: Int = 0,
    @SerialName("FreeThrowsMade") val ftm: Int = 0,
    @SerialName("FreeThrowsAttempted") val fta: Int = 0,
    @SerialName("OffensiveRebounds") val offReb: Int = 0,
    @SerialName("DefensiveRebounds") val defReb: Int = 0,
    @SerialName("Assistances") val assists: Int = 0,
    @SerialName("Steals") val steals: Int = 0,
    @SerialName("Turnovers") val turnovers: Int = 0,
    @SerialName("BlocksFavour") val blocks: Int = 0,
    @SerialName("BlocksAgainst") val blocksAgainst: Int = 0,
    @SerialName("FoulsCommited") val foulsCommitted: Int = 0,
    @SerialName("FoulsReceived") val foulsDrawn: Int = 0,
    @SerialName("Valuation") val valuation: Int? = null
)

@Serializable
data class EuroLeagueLivePointsDto(
    @SerialName("Rows") val rows: List<EuroLeagueShotRowDto> = emptyList()
)

@Serializable
data class EuroLeagueShotRowDto(
    @SerialName("NUM_ANOT") val numAnot: Int = 0,
    @SerialName("TEAM") val team: String = "",
    @SerialName("ID_PLAYER") val playerId: String = "",
    @SerialName("PLAYER") val playerName: String = "",
    @SerialName("ID_ACTION") val actionId: String = "",
    @SerialName("COORD_X") val coordX: Int = 0,
    @SerialName("COORD_Y") val coordY: Int = 0,
    @SerialName("MINUTE") val minute: Int = 1
)

@Serializable
data class EuroLeagueLiveHeaderDto(
    @SerialName("Live") val live: Boolean = false,
    @SerialName("Round") val round: String = "",
    @SerialName("TeamA") val teamA: String = "",
    @SerialName("TeamB") val teamB: String = "",
    @SerialName("CodeTeamA") val codeTeamA: String = "",
    @SerialName("CodeTeamB") val codeTeamB: String = "",
    @SerialName("ScoreA") val scoreA: String = "0",
    @SerialName("ScoreB") val scoreB: String = "0",
    @SerialName("Quarter") val quarter: String = "1",
    @SerialName("RemainingPartialTime") val remainingPartialTime: String = "",
    @SerialName("GameTime") val gameTime: String = "",
    @SerialName("ScoreQuarter1A") val scoreQuarter1A: Int = 0,
    @SerialName("ScoreQuarter2A") val scoreQuarter2A: Int = 0,
    @SerialName("ScoreQuarter3A") val scoreQuarter3A: Int = 0,
    @SerialName("ScoreQuarter4A") val scoreQuarter4A: Int = 0,
    @SerialName("ScoreExtra1A") val scoreExtra1A: Int = 0,
    @SerialName("ScoreExtraTimeA") val scoreExtraTimeA: Int = 0,
    @SerialName("ScoreQuarter1B") val scoreQuarter1B: Int = 0,
    @SerialName("ScoreQuarter2B") val scoreQuarter2B: Int = 0,
    @SerialName("ScoreQuarter3B") val scoreQuarter3B: Int = 0,
    @SerialName("ScoreQuarter4B") val scoreQuarter4B: Int = 0,
    @SerialName("ScoreExtra1B") val scoreExtra1B: Int = 0,
    @SerialName("ScoreExtraTimeB") val scoreExtraTimeB: Int = 0
)

@Serializable
data class EuroLeagueLivePlayByPlayDto(
    @SerialName("Live") val live: Boolean = false,
    @SerialName("CodeTeamA") val codeTeamA: String = "",
    @SerialName("CodeTeamB") val codeTeamB: String = "",
    @SerialName("ActualQuarter") val actualQuarter: Int = 4,
    @SerialName("FirstQuarter") val firstQuarter: List<EuroLeaguePlayByPlayRowDto>? = null,
    @SerialName("SecondQuarter") val secondQuarter: List<EuroLeaguePlayByPlayRowDto>? = null,
    @SerialName("ThirdQuarter") val thirdQuarter: List<EuroLeaguePlayByPlayRowDto>? = null,
    @SerialName("ForthQuarter") val forthQuarter: List<EuroLeaguePlayByPlayRowDto>? = null,
    @SerialName("FourthQuarter") val fourthQuarterAlt: List<EuroLeaguePlayByPlayRowDto>? = null,
    @SerialName("ExtraTime") val extraTime: List<EuroLeaguePlayByPlayRowDto>? = null
)

@Serializable
data class EuroLeaguePlayByPlayRowDto(
    @SerialName("NUMBEROFPLAY") val numberOfPlay: Int = 0,
    @SerialName("CODETEAM") val codeTeam: String? = null,
    @SerialName("PLAYER_ID") val playerId: String? = null,
    @SerialName("PLAYTYPE") val playType: String? = null,
    @SerialName("PLAYER") val player: String? = null,
    @SerialName("DORSAL") val dorsal: String? = null,
    @SerialName("MINUTE") val minute: Int = 1,
    @SerialName("MARKERTIME") val markerTime: String? = null,
    @SerialName("POINTS_A") val pointsA: Int? = null,
    @SerialName("POINTS_B") val pointsB: Int? = null,
    @SerialName("PLAYINFO") val playInfo: String? = null
)

// --- BUNDLED SNAPSHOT DTOs (assets/euroleague_live_snapshot.json) ---

@Serializable
data class BundledLiveSnapshotDto(
    val seasonCode: String = "E2026",
    val syncedAtUtc: String = "",
    val activeRound: Int = 1,
    val teams: List<BundledTeamDto> = emptyList(),
    val players: List<BundledPlayerDto> = emptyList(),
    val games: List<BundledGameDto> = emptyList()
)

@Serializable
data class BundledTeamDto(
    val id: String,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val arena: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long = 0xFFFF6600,
    val wins: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val totalPointsFor: Int? = null,
    val totalPointsAgainst: Int? = null,
    val streak: List<String> = emptyList(),
    val offensiveRating: Float,
    val defensiveRating: Float,
    val pace: Float,
    val threePointPct: Float,
    val twoPointPct: Float,
    val reboundRate: Float,
    val logoUrl: String = "",
    val localLogoResName: String = ""
)

@Serializable
data class BundledPlayerDto(
    val id: String,
    val teamId: String,
    val name: String,
    val number: Int,
    val position: String,
    val heightCm: Int,
    val nationality: String,
    val injuryStatus: String = "FIT",
    val injuryDetail: String = "",
    val ppg: Float = 0f,
    val rpg: Float = 0f,
    val apg: Float = 0f,
    val pir: Float = 0f,
    val headshotUrl: String = ""
)

@Serializable
data class BundledGameDto(
    val id: String,
    val gameCode: Int = 1,
    val seasonCode: String = "E2026",
    val round: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val tipOffEpochMillis: Long,
    val status: String,
    val currentQuarter: Int,
    val remainingClock: String,
    val homeScore: Int,
    val awayScore: Int,
    val quarters: BundledQuartersDto,
    val headToHeadHomeWins: Int = 5,
    val headToHeadAwayWins: Int = 5,
    val isOfficialBoxscore: Boolean = false,
    val boxscoreHome: List<BundledBoxscoreStatDto> = emptyList(),
    val boxscoreAway: List<BundledBoxscoreStatDto> = emptyList(),
    val shotChart: List<BundledShotAttemptDto> = emptyList()
)

@Serializable
data class BundledQuartersDto(
    val q1Home: Int = 0,
    val q1Away: Int = 0,
    val q2Home: Int = 0,
    val q2Away: Int = 0,
    val q3Home: Int = 0,
    val q3Away: Int = 0,
    val q4Home: Int = 0,
    val q4Away: Int = 0,
    val otHome: Int? = null,
    val otAway: Int? = null
)

@Serializable
data class BundledBoxscoreStatDto(
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
    val blocksAgainst: Int = 0,
    val turnovers: Int,
    val twoPtMade: Int,
    val twoPtAttempted: Int,
    val threePtMade: Int,
    val threePtAttempted: Int,
    val ftMade: Int,
    val ftAttempted: Int,
    val foulsDrawn: Int,
    val foulsCommitted: Int,
    val valuationOverride: Int? = null
)

@Serializable
data class BundledShotAttemptDto(
    val id: String,
    val playerId: String,
    val playerName: String,
    val teamId: String,
    val courtX: Float,
    val courtY: Float,
    val isMade: Boolean,
    val isThreePointer: Boolean,
    val quarter: Int
)

/**
 * Offizielles Retrofit-Interface für die EuroLeague Live-Feeds (`api-live.euroleague.net` v2/v3
 * sowie `live.euroleague.net/api/Boxscore` & `Points` für Live-Boxscores und echte Wurf-Koordinaten).
 */
interface EuroLeagueApiService {

    @GET("v2/competitions/E/seasons/{seasonCode}/clubs")
    suspend fun fetchSeasonClubs(
        @Path("seasonCode") seasonCode: String = "E2026",
        @Query("limit") limit: Int = 50
    ): EuroLeagueClubsEnvelopeDto

    @GET("v2/competitions/E/seasons/{seasonCode}/clubs/{clubCode}/people")
    suspend fun fetchClubRosterPeople(
        @Path("seasonCode") seasonCode: String = "E2026",
        @Path("clubCode") clubCode: String
    ): List<EuroLeagueClubPersonDto>

    @GET("v2/competitions/E/seasons/{seasonCode}/rounds")
    suspend fun fetchSeasonRounds(
        @Path("seasonCode") seasonCode: String = "E2026"
    ): EuroLeagueRoundsEnvelopeDto

    @GET("v2/competitions/E/seasons/{seasonCode}/games")
    suspend fun fetchSeasonGames(
        @Path("seasonCode") seasonCode: String = "E2026",
        @Query("roundNumber") round: Int? = null,
        @Query("limit") limit: Int = 400
    ): EuroLeagueGamesEnvelopeDto

    @GET("v2/competitions/E/seasons/{seasonCode}/games/{gameCode}/stats")
    suspend fun fetchGameBoxscoreStats(
        @Path("seasonCode") seasonCode: String = "E2026",
        @Path("gameCode") gameCode: Int
    ): EuroLeagueGameBoxscoreDto

    @GET("v3/competitions/E/statistics/players/traditional")
    suspend fun fetchPlayerTraditionalStats(
        @Query("seasonMode") seasonMode: String = "Single",
        @Query("seasonCode") seasonCode: String = "E2026",
        @Query("statisticMode") statisticMode: String = "PerGame",
        @Query("limit") limit: Int = 500
    ): EuroLeaguePlayerStatsEnvelopeDto

    @GET("v3/competitions/E/statistics/teams/traditional")
    suspend fun fetchTeamTraditionalStats(
        @Query("seasonMode") seasonMode: String = "Single",
        @Query("seasonCode") seasonCode: String = "E2026",
        @Query("statisticMode") statisticMode: String = "PerGame",
        @Query("limit") limit: Int = 50
    ): EuroLeagueTeamStatsEnvelopeDto

    @GET
    suspend fun fetchLiveGameBoxscore(
        @Url fullUrl: String
    ): EuroLeagueLiveBoxscoreEnvelopeDto

    @GET
    suspend fun fetchLiveGameShotPoints(
        @Url fullUrl: String
    ): EuroLeagueLivePointsDto

    @GET
    suspend fun fetchLiveGameHeader(
        @Url fullUrl: String
    ): EuroLeagueLiveHeaderDto

    @GET
    suspend fun fetchLiveGamePlayByPlay(
        @Url fullUrl: String
    ): EuroLeagueLivePlayByPlayDto
}

/**
 * Live-First & Offline-First Synchronisations-Repository:
 * 1. Lädt beim Start sofort den gebündelten Live-Snapshot (`assets/euroleague_live_snapshot.json`),
 *    sodass echte Logos, Kader und Spieltage ohne Verzögerung bereitstehen.
 * 2. Synchronisiert anschließend live gegen `api-live.euroleague.net/v2`, `v3`, `live.euroleague.net/api`
 *    und den täglichen EuroLeague-Injury-Report, prüft alle aktiven Kader (`type == "J"`, `active == true`),
 *    berechnet die tagesaktuelle Tabelle und speichert alles im Room-Cache (`CourtsideDao`).
 */
@Singleton
class EuroLeagueOfflineFirstRepository @Inject constructor(
    private val apiService: EuroLeagueApiService,
    private val dao: CourtsideDao,
    private val fallbackMockRepository: EuroLeagueMockRepository,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val httpClient = OkHttpClient()
    private val apiSemaphore = kotlinx.coroutines.sync.Semaphore(6)
    private val liveTickMutex = kotlinx.coroutines.sync.Mutex()
    @Volatile private var lastScheduleSyncMillis: Long = 0L
    @Volatile private var lastRoomPersistMillis: Long = 0L

    private val officialPalette: Map<String, Triple<Long, Long, Long>> = mapOf(
        "MUN" to Triple(0xFFDC052D, 0xFF0066B2, 0xFFFFFFFF),
        "BAY" to Triple(0xFFDC052D, 0xFF0066B2, 0xFFFFFFFF),
        "PAN" to Triple(0xFF007A3D, 0xFFD4AF37, 0xFFFFFFFF),
        "PAO" to Triple(0xFF007A3D, 0xFFD4AF37, 0xFFFFFFFF),
        "MAD" to Triple(0xFFFEBE10, 0xFF00529F, 0xFFFFFFFF),
        "RMB" to Triple(0xFFFEBE10, 0xFF00529F, 0xFFFFFFFF),
        "OLY" to Triple(0xFFE30613, 0xFFFFFFFF, 0xFFD4AF37),
        "BAR" to Triple(0xFFA50044, 0xFF004D98, 0xFFEDBB00),
        "FCB" to Triple(0xFFA50044, 0xFF004D98, 0xFFEDBB00),
        "ULK" to Triple(0xFFFFED00, 0xFF002B5C, 0xFFFFFFFF),
        "FBB" to Triple(0xFFFFED00, 0xFF002B5C, 0xFFFFFFFF),
        "MCO" to Triple(0xFFE4002B, 0xFFD4AF37, 0xFFFFFFFF),
        "ASM" to Triple(0xFFE4002B, 0xFFD4AF37, 0xFFFFFFFF),
        "ZAL" to Triple(0xFF006837, 0xFFFFFFFF, 0xFFD4AF37),
        "PAR" to Triple(0xFFE5E7EB, 0xFFD4AF37, 0xFF1F2937),
        "IST" to Triple(0xFF0033A0, 0xFFFF6600, 0xFFFFFFFF),
        "EFS" to Triple(0xFF0033A0, 0xFFFF6600, 0xFFFFFFFF),
        "BER" to Triple(0xFFFFCC00, 0xFF003876, 0xFFFFFFFF),
        "ALB" to Triple(0xFFFFCC00, 0xFF003876, 0xFFFFFFFF),
        "EA7" to Triple(0xFFD9001B, 0xFFFFFFFF, 0xFFD4AF37),
        "MIL" to Triple(0xFFD9001B, 0xFFFFFFFF, 0xFFD4AF37),
        "TEL" to Triple(0xFFFCE300, 0xFF003893, 0xFFFFFFFF),
        "MAC" to Triple(0xFFFCE300, 0xFF003893, 0xFFFFFFFF),
        "BKN" to Triple(0xFF9E1B32, 0xFF002D62, 0xFFFFFFFF),
        "BAS" to Triple(0xFF9E1B32, 0xFF002D62, 0xFFFFFFFF),
        "VIR" to Triple(0xFFE5E7EB, 0xFFD4AF37, 0xFF111827),
        "RED" to Triple(0xFFE30613, 0xFFFFFFFF, 0xFFD4AF37),
        "CZV" to Triple(0xFFE30613, 0xFFFFFFFF, 0xFFD4AF37),
        "PRS" to Triple(0xFF1E3A8A, 0xFFE11D48, 0xFFFFFFFF),
        "ASV" to Triple(0xFF9CA3AF, 0xFFFFFFFF, 0xFF111827),
        "DUB" to Triple(0xFFD4AF37, 0xFF111827, 0xFFFFFFFF),
        "HTA" to Triple(0xFFE30613, 0xFF111827, 0xFFFFFFFF),
        "PAM" to Triple(0xFFFF6600, 0xFF111827, 0xFFFFFFFF),
        "VAL" to Triple(0xFFFF6600, 0xFF111827, 0xFFFFFFFF)
    )

    /**
     * Lädt den vorab synchronisierten Live-Snapshot aus `assets/euroleague_live_snapshot.json`,
     * falls vorhanden. Dadurch hat die App sofort nach dem Öffnen aktuelle Kader, echte Club-Logos
     * und den aktuellen Spieltag, noch während der Live-API-Call im Hintergrund läuft.
     */
    suspend fun loadBundledLiveSnapshotFromAssets(context: Context): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val rawJson = context.assets.open("euroleague_live_snapshot.json")
                    .bufferedReader()
                    .use { it.readText() }
                applySnapshotJsonString(rawJson, isLiveNetwork = false)
            }
        }

    fun applySnapshotJsonString(rawJson: String, isLiveNetwork: Boolean = false) {
        val snapshot = json.decodeFromString<BundledLiveSnapshotDto>(rawJson)
        if (snapshot.teams.isEmpty()) return

        val mappedTeams = snapshot.teams.map { dto ->
            EuroLeagueTeam(
                id = dto.id,
                code = dto.code,
                name = dto.name,
                city = dto.city,
                country = dto.country,
                arena = dto.arena,
                primaryColorHex = dto.primaryColorHex,
                secondaryColorHex = dto.secondaryColorHex,
                accentColorHex = dto.accentColorHex,
                wins = dto.wins,
                losses = dto.losses,
                pointsFor = dto.pointsFor,
                pointsAgainst = dto.pointsAgainst,
                totalPointsFor = dto.totalPointsFor ?: dto.pointsFor,
                totalPointsAgainst = dto.totalPointsAgainst ?: dto.pointsAgainst,
                streak = EuroLeagueMockRepository.normalizeStreakList(dto.streak),
                offensiveRating = dto.offensiveRating,
                defensiveRating = dto.defensiveRating,
                pace = dto.pace,
                threePointPct = dto.threePointPct,
                twoPointPct = dto.twoPointPct,
                reboundRate = dto.reboundRate,
                logoUrl = dto.logoUrl,
                localLogoResName = dto.localLogoResName
            )
        }
        val teamById = mappedTeams.associateBy { it.id }

        val mappedPlayers = snapshot.players.map { p ->
            PlayerProfile(
                id = p.id,
                teamId = p.teamId,
                name = p.name,
                number = p.number,
                position = p.position,
                heightCm = p.heightCm,
                nationality = p.nationality,
                injuryStatus = runCatching { InjuryStatus.valueOf(p.injuryStatus) }.getOrDefault(InjuryStatus.FIT),
                injuryDetail = p.injuryDetail,
                ppg = p.ppg,
                rpg = p.rpg,
                apg = p.apg,
                pir = p.pir,
                headshotUrl = p.headshotUrl
            )
        }

        val mappedGames = snapshot.games.mapNotNull { g ->
            val home = teamById[g.homeTeamId] ?: mappedTeams.find { it.code == g.homeTeamId } ?: return@mapNotNull null
            val away = teamById[g.awayTeamId] ?: mappedTeams.find { it.code == g.awayTeamId } ?: return@mapNotNull null
            EuroLeagueGame(
                id = g.id,
                gameCode = g.gameCode,
                seasonCode = g.seasonCode,
                round = g.round,
                homeTeam = home,
                awayTeam = away,
                tipOffEpochMillis = g.tipOffEpochMillis,
                status = runCatching { GameStatus.valueOf(g.status) }.getOrDefault(GameStatus.UPCOMING),
                currentQuarter = g.currentQuarter,
                remainingClock = g.remainingClock,
                homeScore = g.homeScore,
                awayScore = g.awayScore,
                quarters = QuarterScores(
                    q1Home = g.quarters.q1Home,
                    q1Away = g.quarters.q1Away,
                    q2Home = g.quarters.q2Home,
                    q2Away = g.quarters.q2Away,
                    q3Home = g.quarters.q3Home,
                    q3Away = g.quarters.q3Away,
                    q4Home = g.quarters.q4Home,
                    q4Away = g.quarters.q4Away,
                    otHome = g.quarters.otHome,
                    otAway = g.quarters.otAway
                ),
                headToHeadHomeWins = g.headToHeadHomeWins,
                headToHeadAwayWins = g.headToHeadAwayWins,
                boxscoreHome = g.boxscoreHome.map { b ->
                    PlayerBoxscoreStat(
                        playerId = b.playerId,
                        playerName = b.playerName,
                        jerseyNumber = b.jerseyNumber,
                        teamId = b.teamId,
                        minutes = b.minutes,
                        points = b.points,
                        offRebounds = b.offRebounds,
                        defRebounds = b.defRebounds,
                        assists = b.assists,
                        steals = b.steals,
                        blocks = b.blocks,
                        blocksAgainst = b.blocksAgainst,
                        turnovers = b.turnovers,
                        twoPtMade = b.twoPtMade,
                        twoPtAttempted = b.twoPtAttempted,
                        threePtMade = b.threePtMade,
                        threePtAttempted = b.threePtAttempted,
                        ftMade = b.ftMade,
                        ftAttempted = b.ftAttempted,
                        foulsDrawn = b.foulsDrawn,
                        foulsCommitted = b.foulsCommitted,
                        valuationOverride = b.valuationOverride
                    )
                },
                boxscoreAway = g.boxscoreAway.map { b ->
                    PlayerBoxscoreStat(
                        playerId = b.playerId,
                        playerName = b.playerName,
                        jerseyNumber = b.jerseyNumber,
                        teamId = b.teamId,
                        minutes = b.minutes,
                        points = b.points,
                        offRebounds = b.offRebounds,
                        defRebounds = b.defRebounds,
                        assists = b.assists,
                        steals = b.steals,
                        blocks = b.blocks,
                        blocksAgainst = b.blocksAgainst,
                        turnovers = b.turnovers,
                        twoPtMade = b.twoPtMade,
                        twoPtAttempted = b.twoPtAttempted,
                        threePtMade = b.threePtMade,
                        threePtAttempted = b.threePtAttempted,
                        ftMade = b.ftMade,
                        ftAttempted = b.ftAttempted,
                        foulsDrawn = b.foulsDrawn,
                        foulsCommitted = b.foulsCommitted,
                        valuationOverride = b.valuationOverride
                    )
                },
                shots = g.shotChart.map { s ->
                    ShotAttempt(
                        id = s.id,
                        playerId = s.playerId,
                        playerName = s.playerName,
                        teamId = s.teamId,
                        courtX = s.courtX,
                        courtY = s.courtY,
                        made = s.isMade,
                        isThreePointer = s.isThreePointer,
                        quarter = s.quarter
                    )
                }
            )
        }

        val syncLabel = if (isLiveNetwork) {
            "LIVE API • ${snapshot.seasonCode} • Round ${snapshot.activeRound} • Kader & Logos aktuell"
        } else {
            "LIVE SNAPSHOT • ${snapshot.seasonCode} • Round ${snapshot.activeRound} • ${mappedTeams.size} Clubs & ${mappedPlayers.size} Spieler"
        }

        fallbackMockRepository.applyLiveDataset(
            liveTeams = mappedTeams,
            livePlayers = mappedPlayers,
            liveGames = mappedGames,
            activeRound = snapshot.activeRound,
            seasonCode = snapshot.seasonCode,
            syncLabel = syncLabel
        )
    }

    /**
     * Synchronisiert alle Clubs (inkl. offizieller `images.crest`-Logo-URLs), die tagesaktuelle Runde,
     * alle Spiele, die berechnete Tabelle, die verifizierten Kader aller Clubs (`/people`) sowie den
     * aktuellen Verletzten-Report direkt von den EuroLeague Live-Endpunkten.
     */
    suspend fun syncSeasonDataFromRemoteOrSeedCache(preferredSeasonCode: String = "E2026"): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val (seasonCode, clubs) = fetchActiveSeasonClubs(preferredSeasonCode)
                val rounds = runCatching { apiService.fetchSeasonRounds(seasonCode).data }.getOrDefault(emptyList())
                val allGamesDto = apiService.fetchSeasonGames(seasonCode = seasonCode, round = null, limit = 400).data
                val activeRound = resolveCurrentActiveRound(rounds, allGamesDto)
                val playerStatsMap = fetchPlayerTraditionalStatsMap(seasonCode)
                val teamStatsMap = fetchTeamTraditionalStatsMap(seasonCode)
                val injuryMap = fetchLatestInjuryReportMap()

                val existingTeamsById = fallbackMockRepository.teams.associateBy { it.id }
                val existingGamesById = fallbackMockRepository.games.value.associateBy { it.id }
                val existingPlayersByTeam = fallbackMockRepository.players.groupBy { it.teamId }

                // 1. Berechne aktuelle Tabellenstände (exkl. Overtime-Punkte gemäß EuroLeague-Bylaws) & echte Formkurven ("W"/"L")
                data class MutableStanding(
                    var wins: Int = 0,
                    var losses: Int = 0,
                    var regPf: Int = 0,
                    var regPa: Int = 0,
                    var totalPf: Int = 0,
                    var totalPa: Int = 0,
                    val streak: MutableList<String> = mutableListOf()
                )
                val standingsMap = clubs.associate { it.code to MutableStanding() }.toMutableMap()

                allGamesDto
                    .filter { it.played && it.local.score != null && it.road.score != null }
                    .sortedWith(
                        compareBy<EuroLeagueRemoteGameDto> { it.round }
                            .thenBy { it.utcDate ?: "" }
                            .thenBy { it.gameCode }
                    )
                    .forEach { g ->
                        val hCode = g.local.club.code
                        val aCode = g.road.club.code
                        val hScore = g.local.score ?: 0
                        val aScore = g.road.score ?: 0
                        if (hScore == 0 && aScore == 0) return@forEach

                        val hReg = (g.local.partials?.partials1 ?: 0) +
                            (g.local.partials?.partials2 ?: 0) +
                            (g.local.partials?.partials3 ?: 0) +
                            (g.local.partials?.partials4 ?: 0)
                        val aReg = (g.road.partials?.partials1 ?: 0) +
                            (g.road.partials?.partials2 ?: 0) +
                            (g.road.partials?.partials3 ?: 0) +
                            (g.road.partials?.partials4 ?: 0)
                        val isOvertime = hReg > 0 && aReg > 0 && hReg == aReg && (hScore > hReg || aScore > aReg)
                        val standingsH = if (isOvertime) hReg else hScore
                        val standingsA = if (isOvertime) aReg else aScore

                        val hRec = standingsMap.getOrPut(hCode) { MutableStanding() }
                        val aRec = standingsMap.getOrPut(aCode) { MutableStanding() }
                        hRec.regPf += standingsH
                        hRec.regPa += standingsA
                        hRec.totalPf += hScore
                        hRec.totalPa += aScore
                        aRec.regPf += standingsA
                        aRec.regPa += standingsH
                        aRec.totalPf += aScore
                        aRec.totalPa += hScore
                        if (hScore > aScore) {
                            hRec.wins += 1
                            aRec.losses += 1
                            hRec.streak.add("W")
                            aRec.streak.add("L")
                        } else if (aScore > hScore) {
                            aRec.wins += 1
                            hRec.losses += 1
                            aRec.streak.add("W")
                            hRec.streak.add("L")
                        }
                    }

                val liveTeams = clubs.map { c ->
                    val code = c.code
                    val tvCode = c.tvCode ?: code
                    val st = standingsMap[code] ?: MutableStanding()
                    val existing = existingTeamsById[code] ?: existingTeamsById[tvCode]
                    val tStat = teamStatsMap[code] ?: teamStatsMap[tvCode]
                    val gp = (st.wins + st.losses).coerceAtLeast(1)
                    val ppg = when {
                        (tStat?.pointsScored ?: 0.0) > 0.0 -> tStat!!.pointsScored.toFloat()
                        st.totalPf > 0 -> st.totalPf.toFloat() / gp
                        existing != null -> existing.ppg
                        else -> 82f
                    }
                    val oppg = when {
                        st.totalPa > 0 -> st.totalPa.toFloat() / gp
                        existing != null -> existing.oppg
                        else -> 82f
                    }
                    val parsedTwoPct = parsePercentageString(tStat?.twoPointersPercentage)
                    val parsedThreePct = parsePercentageString(tStat?.threePointersPercentage)

                    val fgaPerGame = (tStat?.twoPointersAttempted ?: 0.0) + (tStat?.threePointersAttempted ?: 0.0)
                    val possPerGame = if (fgaPerGame > 0.0) {
                        (fgaPerGame - (tStat?.offensiveRebounds ?: 0.0) +
                            (tStat?.turnovers ?: 0.0) +
                            0.44 * (tStat?.freeThrowsAttempted ?: 0.0)).toFloat().coerceAtLeast(55f)
                    } else {
                        null
                    }
                    val computedOffRtg = if (possPerGame != null) {
                        (((100f * ppg / possPerGame) * 10f).roundToInt() / 10f)
                    } else {
                        existing?.offensiveRating ?: 112.0f
                    }
                    val computedDefRtg = if (possPerGame != null) {
                        (((100f * oppg / possPerGame) * 10f).roundToInt() / 10f)
                    } else {
                        existing?.defensiveRating ?: 112.0f
                    }
                    val computedPace = if (possPerGame != null) {
                        ((possPerGame * 10f).roundToInt() / 10f)
                    } else {
                        existing?.pace ?: 72.0f
                    }
                    val leagueAvgReb = teamStatsMap.values
                        .map { it.totalRebounds }
                        .filter { it > 0.0 }
                        .average()
                        .takeIf { !it.isNaN() && it > 0.0 } ?: 34.5
                    val computedRebRate = when {
                        existing != null && existing.reboundRate in 35f..65f -> existing.reboundRate
                        (tStat?.totalRebounds ?: 0.0) > 0.0 -> {
                            val reb = tStat!!.totalRebounds
                            (((100.0 * reb / (reb + leagueAvgReb)) * 10.0).roundToInt() / 10f)
                        }
                        else -> 50.0f
                    }
                    val colors = officialPalette[code]
                        ?: officialPalette[tvCode]
                        ?: Triple(0xFFFF6600L, 0xFFFFFFFFL, 0xFFFF6600L)

                    EuroLeagueTeam(
                        id = code,
                        code = tvCode,
                        name = c.name.ifBlank { c.alias ?: code },
                        city = c.city ?: existing?.city ?: extractCityFromAddress(c.venue?.address),
                        country = c.country?.name ?: existing?.country ?: "",
                        arena = c.venue?.name ?: existing?.arena ?: "EuroLeague Arena",
                        primaryColorHex = colors.first,
                        secondaryColorHex = colors.second,
                        accentColorHex = colors.third,
                        wins = st.wins,
                        losses = st.losses,
                        pointsFor = st.regPf,
                        pointsAgainst = st.regPa,
                        totalPointsFor = st.totalPf,
                        totalPointsAgainst = st.totalPa,
                        streak = if (st.wins + st.losses > 0) {
                            EuroLeagueMockRepository.normalizeStreakList(st.streak.takeLast(5))
                        } else {
                            EuroLeagueMockRepository.normalizeStreakList(existing?.streak ?: emptyList())
                        },
                        offensiveRating = computedOffRtg,
                        defensiveRating = computedDefRtg,
                        pace = computedPace,
                        threePointPct = parsedThreePct ?: existing?.threePointPct ?: 35.5f,
                        twoPointPct = parsedTwoPct ?: existing?.twoPointPct ?: 54.0f,
                        reboundRate = computedRebRate,
                        logoUrl = c.images?.crest ?: existing?.logoUrl ?: "",
                        localLogoResName = "club_logo_${code.lowercase()}"
                    )
                }

                val teamById = liveTeams.associateBy { it.id }

                // 2. Lade und verifiziere gedrosselt (Semaphore(4)) die aktuellen Kader (Rosters) aller EuroLeague-Clubs
                val allVerifiedPlayers = coroutineScope {
                    liveTeams.map { team ->
                        async {
                            val fetchedRoster = apiSemaphore.withPermit {
                                fetchAndVerifySingleClubRoster(
                                    seasonCode = seasonCode,
                                    clubCode = team.id,
                                    playerStatsMap = playerStatsMap,
                                    injuryMap = injuryMap
                                )
                            }
                            if (fetchedRoster.isNotEmpty()) {
                                fetchedRoster
                            } else {
                                // Falls Einzelabruf gedrosselt/offline: Bestehenden Kader mit frischen Single-Season-Stats aktualisieren
                                (existingPlayersByTeam[team.id] ?: emptyList()).map { p ->
                                    val rawCode = p.id.removePrefix("${team.id}_")
                                    val st = playerStatsMap[rawCode]
                                    val inj = matchPlayerInjury(p.name, p.name.substringAfterLast(" "), injuryMap)
                                    p.copy(
                                        ppg = if (st != null) ((st.pointsScored * 10.0).roundToInt() / 10f) else p.ppg,
                                        rpg = if (st != null) ((st.totalRebounds * 10.0).roundToInt() / 10f) else p.rpg,
                                        apg = if (st != null) ((st.assists * 10.0).roundToInt() / 10f) else p.apg,
                                        pir = if (st != null) ((st.pir * 10.0).roundToInt() / 10f) else p.pir,
                                        injuryStatus = inj?.first ?: p.injuryStatus,
                                        injuryDetail = inj?.second ?: p.injuryDetail
                                    )
                                }
                            }
                        }
                    }.awaitAll().flatten()
                }

                // 3. Behalte ALLE Spiele aller 38 Spieltage (R1..R38) inkl. aller Boxscores & Shot-Charts
                val sortedAllGamesDto = allGamesDto.sortedWith(
                    compareBy<EuroLeagueRemoteGameDto> { it.round }
                        .thenBy { it.utcDate ?: "" }
                        .thenBy { it.gameCode }
                )

                val latestCachedById = fallbackMockRepository.games.value.associateBy { it.id }
                val latestCachedByCode = fallbackMockRepository.games.value.associateBy { it.gameCode }
                val nowMillis = System.currentTimeMillis()

                val liveGames = coroutineScope {
                    sortedAllGamesDto.mapNotNull { g ->
                        val homeTeam = teamById[g.local.club.code] ?: return@mapNotNull null
                        val awayTeam = teamById[g.road.club.code] ?: return@mapNotNull null
                        val gameId = "${seasonCode}_${g.gameCode}"
                        val cachedGame = latestCachedById[gameId]
                            ?: existingGamesById[gameId]
                            ?: latestCachedByCode[g.gameCode]
                        async {
                            val candidateHeader = if (shouldQueryLiveHeader(g, activeRound, nowMillis)) {
                                apiSemaphore.withPermit {
                                    fetchGameLiveHeader(seasonCode, g.gameCode)
                                }
                            } else null
                            val headerHasScore = (candidateHeader?.scoreA?.trim()?.toIntOrNull() ?: 0) > 0 ||
                                (candidateHeader?.scoreB?.trim()?.toIntOrNull() ?: 0) > 0
                            val isHeaderLiveOrPlayed = candidateHeader != null && (candidateHeader.live || headerHasScore)
                            val isEffectivelyPlayedOrLive = g.played || isHeaderLiveOrPlayed
                            val isActiveOrCurrentRound = g.round == activeRound || g.round == activeRound + 1 || isHeaderLiveOrPlayed

                            val (fetchedBoxHome, fetchedBoxAway) = if (isActiveOrCurrentRound && isEffectivelyPlayedOrLive) {
                                apiSemaphore.withPermit {
                                    fetchGameBoxscores(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                                }
                            } else {
                                emptyList<PlayerBoxscoreStat>() to emptyList()
                            }
                            val fetchedShots = if (isActiveOrCurrentRound && isEffectivelyPlayedOrLive) {
                                apiSemaphore.withPermit {
                                    fetchGameShotChart(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                                }
                            } else {
                                emptyList()
                            }

                            val finalBoxHome = when {
                                !isEffectivelyPlayedOrLive -> emptyList()
                                fetchedBoxHome.isNotEmpty() -> fetchedBoxHome
                                cachedGame != null && cachedGame.boxscoreHome.isNotEmpty() -> cachedGame.boxscoreHome
                                else -> buildFallbackBoxscoreFromRoster(homeTeam.id, allVerifiedPlayers)
                            }
                            val finalBoxAway = when {
                                !isEffectivelyPlayedOrLive -> emptyList()
                                fetchedBoxAway.isNotEmpty() -> fetchedBoxAway
                                cachedGame != null && cachedGame.boxscoreAway.isNotEmpty() -> cachedGame.boxscoreAway
                                else -> buildFallbackBoxscoreFromRoster(awayTeam.id, allVerifiedPlayers)
                            }
                            val finalShots = when {
                                !isEffectivelyPlayedOrLive -> emptyList()
                                fetchedShots.isNotEmpty() -> fetchedShots
                                cachedGame != null && cachedGame.shots.isNotEmpty() -> cachedGame.shots
                                else -> buildFallbackShotChartFromBoxscores(
                                    gameCode = g.gameCode,
                                    homeTeamId = homeTeam.id,
                                    awayTeamId = awayTeam.id,
                                    boxHome = finalBoxHome,
                                    boxAway = finalBoxAway
                                )
                            }

                            val mapped = mapRemoteGameToDomain(
                                g = g,
                                seasonCode = seasonCode,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                boxHome = finalBoxHome,
                                boxAway = finalBoxAway,
                                shots = finalShots
                            )

                            when {
                                isHeaderLiveOrPlayed && candidateHeader != null ->
                                    applyLiveHeaderToGame(mapped, candidateHeader)
                                !g.played && cachedGame != null && cachedGame.status != GameStatus.UPCOMING ->
                                    cachedGame
                                else -> mapped
                            }
                        }
                    }.awaitAll()
                }

                val resolvedActiveRound = liveGames
                    .filter { it.status == GameStatus.LIVE || it.status == GameStatus.FINAL }
                    .maxOfOrNull { it.round }
                    ?.let { maxOf(activeRound, it) }
                    ?: activeRound

                val syncedTeams = EuroLeagueMockRepository.recomputeOfficialStandingsFromGames(
                    teams = liveTeams,
                    games = liveGames
                )

                fallbackMockRepository.applyLiveDataset(
                    liveTeams = syncedTeams,
                    livePlayers = allVerifiedPlayers,
                    liveGames = liveGames,
                    activeRound = resolvedActiveRound,
                    seasonCode = seasonCode,
                    syncLabel = "$seasonCode • Round $resolvedActiveRound"
                )

                val doneMillis = System.currentTimeMillis()
                lastScheduleSyncMillis = doneMillis
                lastRoomPersistMillis = doneMillis
                persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
            }.recoverCatching {
                // Offline-Fallback: Speichere aktuellen Stand aus dem Mock/Snapshot-Repository in Room
                persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
            }
        }

    /**
     * Ultra-schneller Echtzeit-Live-Ticker (<200ms Latenz):
     * - Umgeht den 60s-Cloudflare-Cache auf `live.euroleague.net/api/Header` via 3s-Cache-Bucket (`&_t=...`).
     * - Lädt im 5s-Schnelltakt ausschließlich die winzigen `Header`-Payloads (~0,9 KB) aktiv laufender oder
     *   unmittelbar startender Partien parallel, ohne bei jedem Tick die 730-KB-Gesamtsaisonliste neu zu laden
     *   (der komplette Spielplan wird nur beim Kaltstart oder alle 5 Minuten abgeglichen).
     */
    suspend fun refreshLiveMatchdayTick(forceScheduleSync: Boolean = false): Result<Unit> =
        withContext(ioDispatcher) {
            if (!forceScheduleSync) {
                if (!liveTickMutex.tryLock()) return@withContext Result.success(Unit)
            } else {
                liveTickMutex.lock()
            }
            try {
                runCatching {
                    val seasonCode = fallbackMockRepository.seasonCodeFlow.value
                    val nowMillis = System.currentTimeMillis()
                    val existingGames = fallbackMockRepository.games.value
                    val shouldFetchSchedule = forceScheduleSync ||
                        existingGames.isEmpty() ||
                        (nowMillis - lastScheduleSyncMillis >= 300_000L)

                    val allGamesDto = if (shouldFetchSchedule) {
                        runCatching {
                            apiService.fetchSeasonGames(seasonCode = seasonCode, round = null, limit = 400).data
                        }.getOrDefault(emptyList()).also { fetched ->
                            if (fetched.isNotEmpty()) {
                                lastScheduleSyncMillis = nowMillis
                            }
                        }
                    } else {
                        emptyList()
                    }

                    val tickClock = runCatching {
                        java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    }.getOrDefault("")

                    if (allGamesDto.isNotEmpty()) {
                        val sortedAllGamesDto = allGamesDto.sortedWith(
                            compareBy<EuroLeagueRemoteGameDto> { it.round }
                                .thenBy { it.utcDate ?: "" }
                                .thenBy { it.gameCode }
                        )
                        val activeRound = resolveCurrentActiveRound(emptyList(), sortedAllGamesDto, nowMillis)

                        val teamById = fallbackMockRepository.teams.associateBy { it.id }
                        val cachedById = existingGames.associateBy { it.id }
                        var anyTransitionedFinal = false

                        val updatedGames = coroutineScope {
                            sortedAllGamesDto.mapNotNull { g ->
                                val homeTeam = teamById[g.local.club.code] ?: return@mapNotNull null
                                val awayTeam = teamById[g.road.club.code] ?: return@mapNotNull null
                                val gameId = "${seasonCode}_${g.gameCode}"
                                val cached = cachedById[gameId]
                                async {
                                    val statusRaw = (g.gameStatus ?: "").lowercase()
                                    val isStatusLive = "live" in statusRaw || "playing" in statusRaw || "progress" in statusRaw
                                    val candidateHeader = if (shouldQueryLiveHeader(g, activeRound, nowMillis)) {
                                        apiSemaphore.withPermit {
                                            fetchGameLiveHeader(seasonCode, g.gameCode)
                                        }
                                    } else null
                                    val headerHasScore = (candidateHeader?.scoreA?.trim()?.toIntOrNull() ?: 0) > 0 ||
                                        (candidateHeader?.scoreB?.trim()?.toIntOrNull() ?: 0) > 0
                                    val isLive = isStatusLive || (candidateHeader?.live == true && headerHasScore)
                                    val justFinished = (g.played || (candidateHeader != null && !candidateHeader.live && headerHasScore)) &&
                                        cached?.status != GameStatus.FINAL
                                    if (justFinished) anyTransitionedFinal = true

                                    val needInitialOrFinalBox = justFinished || (isLive && cached?.boxscoreHome.isNullOrEmpty())
                                    val (boxH, boxA) = if (needInitialOrFinalBox) {
                                        apiSemaphore.withPermit {
                                            fetchGameBoxscores(
                                                seasonCode = seasonCode,
                                                gameCode = g.gameCode,
                                                homeTeamId = homeTeam.id,
                                                awayTeamId = awayTeam.id,
                                                preferLiveEndpoint = isLive || candidateHeader != null
                                            )
                                        }
                                    } else {
                                        (cached?.boxscoreHome ?: emptyList()) to (cached?.boxscoreAway ?: emptyList())
                                    }
                                    val shots = if (justFinished || (isLive && cached?.shots.isNullOrEmpty())) {
                                        apiSemaphore.withPermit {
                                            fetchGameShotChart(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                                        }
                                    } else {
                                        cached?.shots ?: emptyList()
                                    }

                                    val mapped = mapRemoteGameToDomain(
                                        g = g,
                                        seasonCode = seasonCode,
                                        homeTeam = homeTeam,
                                        awayTeam = awayTeam,
                                        boxHome = boxH.ifEmpty { cached?.boxscoreHome ?: emptyList() },
                                        boxAway = boxA.ifEmpty { cached?.boxscoreAway ?: emptyList() },
                                        shots = shots.ifEmpty { cached?.shots ?: emptyList() }
                                    ).let { base ->
                                        val prevPbp = cached?.playByPlay.orEmpty()
                                        if (prevPbp.isNotEmpty()) base.copy(playByPlay = prevPbp) else base
                                    }

                                    when {
                                        candidateHeader != null && (isLive || candidateHeader.live || headerHasScore) ->
                                            applyLiveHeaderToGame(mapped, candidateHeader)
                                        !g.played && cached != null && cached.status != GameStatus.UPCOMING ->
                                            cached
                                        else -> mapped
                                    }
                                }
                            }.awaitAll()
                        }

                        if (updatedGames.isNotEmpty()) {
                            val resolvedActiveRound = updatedGames
                                .filter { it.status == GameStatus.LIVE || it.status == GameStatus.FINAL }
                                .maxOfOrNull { it.round }
                                ?.let { maxOf(activeRound, it) }
                                ?: activeRound
                            val hasLiveNow = updatedGames.any { it.status == GameStatus.LIVE }
                            val label = if (hasLiveNow && tickClock.isNotEmpty()) {
                                "$seasonCode • Round $resolvedActiveRound • $tickClock"
                            } else {
                                "$seasonCode • Round $resolvedActiveRound"
                            }
                            fallbackMockRepository.applyLiveDataset(
                                liveTeams = fallbackMockRepository.teams,
                                livePlayers = fallbackMockRepository.players,
                                liveGames = updatedGames,
                                activeRound = resolvedActiveRound,
                                seasonCode = seasonCode,
                                syncLabel = label
                            )
                            if (anyTransitionedFinal || nowMillis - lastRoomPersistMillis >= 60_000L) {
                                lastRoomPersistMillis = nowMillis
                                persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
                            }
                        }
                    } else {
                        if (existingGames.isEmpty()) return@runCatching
                        val activeRound = fallbackMockRepository.activeRoundFlow.value
                        var anyChanged = false
                        var anyTransitionedFinal = false

                        val updatedGames = coroutineScope {
                            existingGames.map { game ->
                                async {
                                    val isFastLiveCandidate = game.status == GameStatus.LIVE ||
                                        (game.status != GameStatus.FINAL &&
                                            game.tipOffEpochMillis in (nowMillis - 6 * 3_600_000L)..(nowMillis + 45 * 60_000L))
                                    if (!isFastLiveCandidate || game.gameCode <= 0) {
                                        return@async game
                                    }
                                    val candidateHeader = apiSemaphore.withPermit {
                                        fetchGameLiveHeader(game.seasonCode, game.gameCode)
                                    } ?: return@async game
                                    val headerHasScore = (candidateHeader.scoreA.trim().toIntOrNull() ?: 0) > 0 ||
                                        (candidateHeader.scoreB.trim().toIntOrNull() ?: 0) > 0
                                    if (!candidateHeader.live && !headerHasScore) {
                                        return@async game
                                    }
                                    val headerApplied = applyLiveHeaderToGame(game, candidateHeader)
                                    val justFinished = headerApplied.status == GameStatus.FINAL && game.status != GameStatus.FINAL
                                    if (justFinished) anyTransitionedFinal = true

                                    val needInitialOrFinalStats = justFinished ||
                                        (headerApplied.status == GameStatus.LIVE && game.boxscoreHome.isEmpty())
                                    val (boxH, boxA) = if (needInitialOrFinalStats) {
                                        apiSemaphore.withPermit {
                                            fetchGameBoxscores(
                                                seasonCode = game.seasonCode,
                                                gameCode = game.gameCode,
                                                homeTeamId = game.homeTeam.id,
                                                awayTeamId = game.awayTeam.id,
                                                preferLiveEndpoint = true
                                            )
                                        }
                                    } else {
                                        game.boxscoreHome to game.boxscoreAway
                                    }
                                    val shots = if (justFinished || (headerApplied.status == GameStatus.LIVE && game.shots.isEmpty())) {
                                        apiSemaphore.withPermit {
                                            fetchGameShotChart(game.seasonCode, game.gameCode, game.homeTeam.id, game.awayTeam.id)
                                        }
                                    } else {
                                        game.shots
                                    }
                                    val nextGame = headerApplied.copy(
                                        boxscoreHome = boxH.ifEmpty { game.boxscoreHome },
                                        boxscoreAway = boxA.ifEmpty { game.boxscoreAway },
                                        shots = shots.ifEmpty { game.shots },
                                        playByPlay = game.playByPlay
                                    )
                                    if (nextGame != game) {
                                        anyChanged = true
                                    }
                                    nextGame
                                }
                            }.awaitAll()
                        }

                        val hasLiveNow = updatedGames.any { it.status == GameStatus.LIVE }
                        if (anyChanged || hasLiveNow) {
                            val resolvedActiveRound = updatedGames
                                .filter { it.status == GameStatus.LIVE || it.status == GameStatus.FINAL }
                                .maxOfOrNull { it.round }
                                ?.let { maxOf(activeRound, it) }
                                ?.coerceAtLeast(1)
                                ?: activeRound
                            val label = if (hasLiveNow && tickClock.isNotEmpty()) {
                                "$seasonCode • Round $resolvedActiveRound • $tickClock"
                            } else {
                                "$seasonCode • Round $resolvedActiveRound"
                            }
                            fallbackMockRepository.applyLiveDataset(
                                liveTeams = fallbackMockRepository.teams,
                                livePlayers = fallbackMockRepository.players,
                                liveGames = updatedGames,
                                activeRound = resolvedActiveRound,
                                seasonCode = seasonCode,
                                syncLabel = label
                            )
                        }
                        if (anyTransitionedFinal || (anyChanged && nowMillis - lastRoomPersistMillis >= 60_000L)) {
                            lastRoomPersistMillis = nowMillis
                            persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
                        }
                    }
                }
            } finally {
                liveTickMutex.unlock()
            }
        }

    /**
     * Prüft und aktualisiert gezielt den Kader (`type == "J"`, `active == true`) sowie den
     * Verletztenstatus eines einzelnen Vereins (z. B. beim Wechsel des Lieblingsteams im Team-Hub).
     */
    suspend fun verifyAndSyncClubRoster(clubId: String): Result<List<PlayerProfile>> =
        withContext(ioDispatcher) {
            runCatching {
                val seasonCode = fallbackMockRepository.seasonCodeFlow.value
                val statsMap = fetchPlayerTraditionalStatsMap(seasonCode)
                val injuryMap = fetchLatestInjuryReportMap()
                val verifiedRoster = fetchAndVerifySingleClubRoster(
                    seasonCode = seasonCode,
                    clubCode = clubId,
                    playerStatsMap = statsMap,
                    injuryMap = injuryMap
                )
                if (verifiedRoster.isNotEmpty()) {
                    fallbackMockRepository.updateClubRoster(clubId, verifiedRoster)
                }
                verifiedRoster
            }
        }

    /**
     * Lädt Live-Header, Live-Boxscore, alle Shot-Chart-Würfe (Q1–Q4/OT) und Live-Play-by-Play
     * **parallel** (`coroutineScope { async { ... } }`) in ~180–220ms statt sequentiell (~1.000ms)
     * und nutzt immer den aktuellsten Repository-Spielstand als Merge-Basis.
     */
    suspend fun refreshSelectedGameLiveStats(game: EuroLeagueGame): Result<EuroLeagueGame> =
        withContext(ioDispatcher) {
            runCatching {
                val currentGame = fallbackMockRepository.games.value.find { it.id == game.id } ?: game
                if (currentGame.gameCode <= 0) return@runCatching currentGame

                val nowMillis = System.currentTimeMillis()
                val shouldCheckHeader = currentGame.status == GameStatus.LIVE ||
                    (currentGame.status == GameStatus.UPCOMING && currentGame.tipOffEpochMillis <= nowMillis + 45 * 60_000L)

                if (currentGame.status == GameStatus.UPCOMING && !shouldCheckHeader) {
                    return@runCatching currentGame
                }

                // Für noch als UPCOMING markierte Spiele zuerst kurz Header prüfen, bevor Boxscore/PBP geladen werden
                val preCheckedHeader = if (currentGame.status == GameStatus.UPCOMING && shouldCheckHeader) {
                    fetchGameLiveHeader(currentGame.seasonCode, currentGame.gameCode)
                } else null

                if (currentGame.status == GameStatus.UPCOMING) {
                    val preHasScore = (preCheckedHeader?.scoreA?.trim()?.toIntOrNull() ?: 0) > 0 ||
                        (preCheckedHeader?.scoreB?.trim()?.toIntOrNull() ?: 0) > 0
                    if (preCheckedHeader?.live != true && !preHasScore) {
                        return@runCatching currentGame
                    }
                }

                val isLiveGame = currentGame.status == GameStatus.LIVE || preCheckedHeader?.live == true

                val (liveHeader, boxPair, shotsAndPbp) = coroutineScope {
                    val headerDef = async {
                        preCheckedHeader ?: if (shouldCheckHeader) {
                            fetchGameLiveHeader(currentGame.seasonCode, currentGame.gameCode)
                        } else null
                    }
                    val boxDef = async {
                        fetchGameBoxscores(
                            seasonCode = currentGame.seasonCode,
                            gameCode = currentGame.gameCode,
                            homeTeamId = currentGame.homeTeam.id,
                            awayTeamId = currentGame.awayTeam.id,
                            preferLiveEndpoint = isLiveGame
                        )
                    }
                    val shotsDef = async {
                        fetchGameShotChart(
                            seasonCode = currentGame.seasonCode,
                            gameCode = currentGame.gameCode,
                            homeTeamId = currentGame.homeTeam.id,
                            awayTeamId = currentGame.awayTeam.id
                        )
                    }
                    val pbpDef = async {
                        fetchGamePlayByPlay(
                            seasonCode = currentGame.seasonCode,
                            gameCode = currentGame.gameCode,
                            homeTeamId = currentGame.homeTeam.id,
                            awayTeamId = currentGame.awayTeam.id
                        )
                    }
                    Triple(headerDef.await(), boxDef.await(), shotsDef.await() to pbpDef.await())
                }

                val (boxHome, boxAway) = boxPair
                val (shots, pbpEvents) = shotsAndPbp
                val freshestBase = fallbackMockRepository.games.value.find { it.id == currentGame.id } ?: currentGame

                val resolvedHome = boxHome.ifEmpty {
                    freshestBase.boxscoreHome.ifEmpty {
                        buildFallbackBoxscoreFromRoster(freshestBase.homeTeam.id, fallbackMockRepository.players)
                    }
                }
                val resolvedAway = boxAway.ifEmpty {
                    freshestBase.boxscoreAway.ifEmpty {
                        buildFallbackBoxscoreFromRoster(freshestBase.awayTeam.id, fallbackMockRepository.players)
                    }
                }
                val resolvedShots = shots.ifEmpty {
                    freshestBase.shots.ifEmpty {
                        buildFallbackShotChartFromBoxscores(
                            gameCode = freshestBase.gameCode,
                            homeTeamId = freshestBase.homeTeam.id,
                            awayTeamId = freshestBase.awayTeam.id,
                            boxHome = resolvedHome,
                            boxAway = resolvedAway
                        )
                    }
                }
                val baseUpdated = freshestBase.copy(
                    boxscoreHome = resolvedHome,
                    boxscoreAway = resolvedAway,
                    shots = resolvedShots,
                    playByPlay = pbpEvents.ifEmpty { freshestBase.playByPlay }
                )
                val headerHasScore = (liveHeader?.scoreA?.trim()?.toIntOrNull() ?: 0) > 0 ||
                    (liveHeader?.scoreB?.trim()?.toIntOrNull() ?: 0) > 0
                val headerMerged = if (liveHeader != null && (liveHeader.live || headerHasScore)) {
                    applyLiveHeaderToGame(baseUpdated, liveHeader)
                } else {
                    baseUpdated
                }
                val updated = if (headerMerged.playByPlay.isEmpty()) {
                    headerMerged.copy(
                        playByPlay = EuroLeagueMockRepository.buildFallbackPlayByPlayForGame(headerMerged)
                    )
                } else {
                    headerMerged
                }
                fallbackMockRepository.updateSingleGameDetails(updated)
                runCatching { dao.upsertSingleGame(toCachedGameEntity(updated)) }
                updated
            }
        }

    private suspend fun fetchGameLiveHeader(
        seasonCode: String,
        gameCode: Int
    ): EuroLeagueLiveHeaderDto? = runCatching {
        val url = buildLiveApiUrl("Header", gameCode, seasonCode)
        apiService.fetchLiveGameHeader(url)
    }.getOrNull()

    private suspend fun fetchGamePlayByPlay(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ): List<PlayByPlayEvent> = runCatching {
        if (gameCode <= 0) return@runCatching emptyList()
        val url = buildLiveApiUrl("PlaybyPlay", gameCode, seasonCode)
        val dto = apiService.fetchLiveGamePlayByPlay(url)
        parseLivePlayByPlayStatic(
            dto = dto,
            gameCode = gameCode,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId
        )
    }.getOrDefault(emptyList())

    internal fun shouldQueryLiveHeader(
        g: EuroLeagueRemoteGameDto,
        activeRound: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val statusRaw = (g.gameStatus ?: "").lowercase()
        if ("live" in statusRaw || "playing" in statusRaw || "progress" in statusRaw) {
            return true
        }
        val tipOffMillis = runCatching {
            Instant.parse(g.utcDate ?: "").toEpochMilli()
        }.getOrNull()
        val isMatchdayWindow = tipOffMillis != null &&
            tipOffMillis in (nowMillis - 18 * 3_600_000L)..(nowMillis + 2 * 3_600_000L)
        if (!g.played && isMatchdayWindow) {
            return true
        }
        if (!g.played && (g.round == activeRound || g.round == activeRound + 1)) {
            return tipOffMillis == null || tipOffMillis <= nowMillis + 6 * 3_600_000L
        }
        return false
    }

    internal fun applyLiveHeaderToGame(
        game: EuroLeagueGame,
        header: EuroLeagueLiveHeaderDto
    ): EuroLeagueGame = applyLiveHeaderToGameStatic(game, header)

    // --- HELPER METHODS ---

    private suspend fun fetchActiveSeasonClubs(preferredSeason: String): Pair<String, List<EuroLeagueClubDto>> {
        val candidates = listOf(preferredSeason, "E2026", "E2025").distinct()
        for (season in candidates) {
            val clubs = runCatching { apiService.fetchSeasonClubs(season).data }.getOrDefault(emptyList())
            if (clubs.size >= 16) {
                return season to clubs
            }
        }
        error("Keine aktiven EuroLeague-Clubs über api-live.euroleague.net erreichbar")
    }

    internal fun resolveCurrentActiveRound(
        rounds: List<EuroLeagueRoundDto>,
        allGamesDto: List<EuroLeagueRemoteGameDto>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val playedOrLiveGames = allGamesDto.filter { g ->
            val st = (g.gameStatus ?: "").lowercase()
            g.played || (g.local.score ?: 0) > 0 || (g.road.score ?: 0) > 0 ||
                "live" in st || "playing" in st || "progress" in st
        }
        val playedRounds = playedOrLiveGames
            .map { it.round }
            .filter { it in 1..38 }
        if (playedRounds.isNotEmpty()) {
            val maxPlayedRound = playedRounds.maxOrNull() ?: 11
            val gamesInMaxRound = allGamesDto.filter { it.round == maxPlayedRound }
            val allInMaxPlayed = gamesInMaxRound.isNotEmpty() && gamesInMaxRound.all { g ->
                g.played || (g.local.score ?: 0) > 0 || (g.road.score ?: 0) > 0
            }
            if (allInMaxPlayed && maxPlayedRound < 38) {
                val nextRound = maxPlayedRound + 1
                val nextRoundTipOffs = allGamesDto
                    .filter { it.round == nextRound }
                    .mapNotNull { g ->
                        runCatching { Instant.parse(g.utcDate ?: "").toEpochMilli() }.getOrNull()
                    }
                val minNextTipOff = nextRoundTipOffs.minOrNull()
                val maxNextTipOff = nextRoundTipOffs.maxOrNull()
                if (minNextTipOff != null && maxNextTipOff != null &&
                    nowMillis >= minNextTipOff - 36 * 3_600_000L &&
                    nowMillis <= maxNextTipOff + 72 * 3_600_000L
                ) {
                    return nextRound
                }
            }
            return maxPlayedRound
        }
        if (rounds.isEmpty()) return fallbackMockRepository.activeRoundFlow.value.coerceIn(1, 38)
        val nowIso = Instant.ofEpochMilli(nowMillis).toString()
        val sortedRounds = rounds.filter { it.round in 1..38 }.sortedBy { it.round }
        for (r in sortedRounds) {
            val maxDate = r.maxGameStartDate ?: ""
            if (maxDate.isNotEmpty() && maxDate >= nowIso) {
                return r.round
            }
        }
        return fallbackMockRepository.activeRoundFlow.value.coerceIn(1, 38)
    }

    private suspend fun fetchPlayerTraditionalStatsMap(seasonCode: String): Map<String, EuroLeaguePlayerTraditionalDto> =
        runCatching {
            apiService.fetchPlayerTraditionalStats(seasonMode = "Single", seasonCode = seasonCode)
                .players
                .associateBy { it.player.code }
        }.getOrDefault(emptyMap())

    private suspend fun fetchTeamTraditionalStatsMap(seasonCode: String): Map<String, EuroLeagueTeamTraditionalDto> =
        runCatching {
            apiService.fetchTeamTraditionalStats(seasonMode = "Single", seasonCode = seasonCode)
                .teams
                .associateBy { it.team.code }
        }.getOrDefault(emptyMap())

    private fun parsePercentageString(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace("%", "").trim().toFloatOrNull() ?: return null
        val pct = if (cleaned <= 1.0f && cleaned > 0f) cleaned * 100f else cleaned
        return (pct * 10f).roundToInt() / 10f
    }

    private fun fetchLatestInjuryReportMap(): Map<String, Pair<InjuryStatus, String>> =
        runCatching {
            val today = LocalDate.now(ZoneOffset.UTC)
            for (daysBack in 0..10) {
                val dateStr = today.minusDays(daysBack.toLong())
                    .format(DateTimeFormatter.ofPattern("yyyy_MM_dd"))
                val url = "https://raw.githubusercontent.com/IvoVillanueva/INJURYREPORT-EUROLEAGE_2026/main/data/euroleage_injuryreport_$dateStr.csv"
                val req = Request.Builder().url(url).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val csvText = resp.body?.string().orEmpty()
                        val parsed = parseInjuryCsv(csvText)
                        if (parsed.isNotEmpty()) return@runCatching parsed
                    }
                }
            }
            emptyMap()
        }.getOrDefault(emptyMap())

    internal fun parseInjuryCsv(csvText: String): Map<String, Pair<InjuryStatus, String>> {
        val result = mutableMapOf<String, Pair<InjuryStatus, String>>()
        csvText.lineSequence().drop(1).forEach { line ->
            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (cols.size >= 9) {
                val playerName = cols[3].uppercase()
                val injuryType = cols[7]
                val statusRaw = cols[8].uppercase()
                val status = when {
                    "OUT" in statusRaw -> InjuryStatus.OUT
                    "GTD" in statusRaw || "DECISION" in statusRaw || "QUESTIONABLE" in statusRaw || "DOUBTFUL" in statusRaw ->
                        InjuryStatus.QUESTIONABLE
                    else -> InjuryStatus.FIT
                }
                result[playerName] = status to "${cols[8]} • $injuryType".trim(' ', '•')
            }
        }
        return result
    }

    private suspend fun fetchAndVerifySingleClubRoster(
        seasonCode: String,
        clubCode: String,
        playerStatsMap: Map<String, EuroLeaguePlayerTraditionalDto>,
        injuryMap: Map<String, Pair<InjuryStatus, String>>
    ): List<PlayerProfile> = runCatching {
        val people = apiService.fetchClubRosterPeople(seasonCode = seasonCode, clubCode = clubCode)
        people.filter { entry ->
            entry.active && (entry.type == "J" || entry.typeName?.equals("Player", ignoreCase = true) == true)
        }.mapNotNull { entry ->
            val person = entry.person ?: return@mapNotNull null
            val pCode = person.code.ifBlank { return@mapNotNull null }
            val fullName = formatPlayerDisplayName(person)
            val stat = playerStatsMap[pCode]
            val injuryMatch = matchPlayerInjury(fullName, person.passportSurname.orEmpty(), injuryMap)

            val rawHeight = person.height ?: 198
            val heightCm = if (rawHeight in 1..2) (rawHeight * 100) else if (rawHeight < 100) 198 else rawHeight

            PlayerProfile(
                id = "${clubCode}_$pCode",
                teamId = clubCode,
                name = fullName,
                number = entry.dorsal?.trim()?.toIntOrNull() ?: 0,
                position = mapPositionCode(entry.positionName),
                heightCm = heightCm,
                nationality = person.country?.code ?: "EU",
                injuryStatus = injuryMatch?.first ?: InjuryStatus.FIT,
                injuryDetail = injuryMatch?.second ?: "",
                ppg = ((stat?.pointsScored ?: 0.0) * 10.0).roundToInt() / 10f,
                rpg = ((stat?.totalRebounds ?: 0.0) * 10.0).roundToInt() / 10f,
                apg = ((stat?.assists ?: 0.0) * 10.0).roundToInt() / 10f,
                pir = ((stat?.pir ?: 0.0) * 10.0).roundToInt() / 10f,
                headshotUrl = entry.images?.headshot ?: ""
            )
        }.sortedWith(compareByDescending<PlayerProfile> { it.pir }.thenByDescending { it.ppg })
    }.getOrDefault(emptyList())

    /**
     * Dual-Source Live-Boxscore-Abruf:
     * - Während laufender Live-Spiele (`preferLiveEndpoint = true`) wird direkt `live.euroleague.net/api/Boxscore`
     *   mit Cache-Buster abgefragt (da `v2/.../stats` während Live-Spielen 0 Punkte mit `max-age=7200` liefert).
     * - Für abgeschlossene Spiele dient `v2/.../stats` als Primärquelle mit Live-Endpoint-Fallback.
     */
    private suspend fun fetchGameBoxscores(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String,
        preferLiveEndpoint: Boolean = false
    ): Pair<List<PlayerBoxscoreStat>, List<PlayerBoxscoreStat>> {
        if (preferLiveEndpoint) {
            val livePrimary = runCatching {
                val url = buildLiveApiUrl("Boxscore", gameCode, seasonCode)
                val liveDto = apiService.fetchLiveGameBoxscore(url)
                val homeStats = parseLiveBoxscorePlayers(liveDto.stats.getOrNull(0)?.playersStats.orEmpty(), homeTeamId)
                val awayStats = parseLiveBoxscorePlayers(liveDto.stats.getOrNull(1)?.playersStats.orEmpty(), awayTeamId)
                homeStats to awayStats
            }.getOrDefault(emptyList<PlayerBoxscoreStat>() to emptyList())

            if (livePrimary.first.isNotEmpty() || livePrimary.second.isNotEmpty()) {
                return livePrimary
            }
        }

        val primary = runCatching {
            val dto = apiService.fetchGameBoxscoreStats(seasonCode = seasonCode, gameCode = gameCode)
            val homeStats = parseBoxscoreSection(dto.local, homeTeamId)
            val awayStats = parseBoxscoreSection(dto.road, awayTeamId)
            homeStats to awayStats
        }.getOrDefault(emptyList<PlayerBoxscoreStat>() to emptyList())

        if (primary.first.isNotEmpty() && primary.second.isNotEmpty()) {
            return primary
        }

        return runCatching {
            val url = buildLiveApiUrl("Boxscore", gameCode, seasonCode)
            val liveDto = apiService.fetchLiveGameBoxscore(url)
            val homeStats = parseLiveBoxscorePlayers(liveDto.stats.getOrNull(0)?.playersStats.orEmpty(), homeTeamId)
            val awayStats = parseLiveBoxscorePlayers(liveDto.stats.getOrNull(1)?.playersStats.orEmpty(), awayTeamId)
            homeStats to awayStats
        }.getOrDefault(primary)
    }

    private fun parseLiveBoxscorePlayers(
        rows: List<EuroLeagueLivePlayerStatDto>,
        teamId: String
    ): List<PlayerBoxscoreStat> {
        return rows.mapNotNull { r ->
            val minsClean = r.minutes.trim()
            if ((minsClean.isEmpty() || minsClean == "00:00" || minsClean == "DNP") && r.points <= 0) {
                return@mapNotNull null
            }
            val codeClean = r.playerId.trim().removePrefix("P")
            val formattedName = if ("," in r.player) {
                val parts = r.player.split(",", limit = 2).map { it.trim() }
                "${parts[1]} ${parts[0]}".lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            } else {
                r.player.trim().lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            }
            PlayerBoxscoreStat(
                playerId = "${teamId}_$codeClean",
                playerName = formattedName,
                jerseyNumber = r.dorsal.trim().toIntOrNull() ?: 0,
                teamId = teamId,
                minutes = minsClean.ifEmpty { "00:00" },
                points = r.points,
                offRebounds = r.offReb,
                defRebounds = r.defReb,
                assists = r.assists,
                steals = r.steals,
                blocks = r.blocks,
                blocksAgainst = r.blocksAgainst,
                turnovers = r.turnovers,
                twoPtMade = r.fgm2,
                twoPtAttempted = r.fga2,
                threePtMade = r.fgm3,
                threePtAttempted = r.fga3,
                ftMade = r.ftm,
                ftAttempted = r.fta,
                foulsDrawn = r.foulsDrawn,
                foulsCommitted = r.foulsCommitted,
                valuationOverride = r.valuation
            )
        }.sortedByDescending { it.pir }
    }

    private fun parseBoxscoreSection(
        section: EuroLeagueTeamBoxscoreSectionDto?,
        teamId: String
    ): List<PlayerBoxscoreStat> {
        val entries = section?.players ?: return emptyList()
        return entries.mapNotNull { item ->
            val st = item.stats
            val secs = (st.timePlayed ?: 0.0).toInt()
            val pts = (st.points ?: 0.0).roundToInt()
            if (secs <= 0 && pts <= 0) return@mapNotNull null

            val mins = "%02d:%02d".format(secs / 60, secs % 60)
            val rawVal = st.valuation ?: st.valuationAlt
            PlayerBoxscoreStat(
                playerId = "${teamId}_${item.player.person.code}",
                playerName = formatPlayerDisplayName(item.player.person),
                jerseyNumber = item.player.dorsal?.trim()?.toIntOrNull() ?: 0,
                teamId = teamId,
                minutes = mins,
                points = pts,
                offRebounds = (st.offensiveRebounds ?: 0.0).roundToInt(),
                defRebounds = (st.defensiveRebounds ?: 0.0).roundToInt(),
                assists = (st.assistances ?: 0.0).roundToInt(),
                steals = (st.steals ?: 0.0).roundToInt(),
                blocks = (st.blocksFavour ?: 0.0).roundToInt(),
                blocksAgainst = (st.blocksAgainst ?: 0.0).roundToInt(),
                turnovers = (st.turnovers ?: 0.0).roundToInt(),
                twoPtMade = (st.fieldGoalsMade2 ?: 0.0).roundToInt(),
                twoPtAttempted = (st.fieldGoalsAttempted2 ?: 0.0).roundToInt(),
                threePtMade = (st.fieldGoalsMade3 ?: 0.0).roundToInt(),
                threePtAttempted = (st.fieldGoalsAttempted3 ?: 0.0).roundToInt(),
                ftMade = (st.freeThrowsMade ?: 0.0).roundToInt(),
                ftAttempted = (st.freeThrowsAttempted ?: 0.0).roundToInt(),
                foulsDrawn = (st.foulsReceived ?: 0.0).roundToInt(),
                foulsCommitted = (st.foulsCommited ?: 0.0).roundToInt(),
                valuationOverride = rawVal?.roundToInt()
            )
        }.sortedByDescending { it.pir }
    }

    private suspend fun fetchGameShotChart(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ): List<ShotAttempt> = runCatching {
        val url = buildLiveApiUrl("Points", gameCode, seasonCode)
        val rows = apiService.fetchLiveGameShotPoints(url).rows
        rows.mapIndexedNotNull { idx, row ->
            val act = row.actionId.trim().uppercase()
            if (act !in setOf("2FGM", "2FGA", "3FGM", "3FGA", "DUNK", "LAYUPATT", "LAYUPMD")) return@mapIndexedNotNull null
            val normX = ((row.coordX + 750f) / 1500f).coerceIn(0.06f, 0.94f)
            val normY = ((row.coordY + 150f) / 1400f).coerceIn(0.08f, 0.92f)
            val teamRaw = row.team.trim().uppercase()
            val mappedTeamId = if (teamRaw == homeTeamId || teamRaw.startsWith(homeTeamId.take(3))) homeTeamId else awayTeamId
            val minute = row.minute.coerceAtLeast(1)
            val quarter = (((minute - 1) / 10) + 1).coerceIn(1, 5)

            ShotAttempt(
                id = "shot_${gameCode}_${row.numAnot}_$idx",
                playerId = row.playerId.trim(),
                playerName = row.playerName.trim(),
                teamId = mappedTeamId,
                courtX = normX,
                courtY = normY,
                made = act in setOf("2FGM", "3FGM", "DUNK", "LAYUPMD"),
                isThreePointer = "3FG" in act,
                quarter = quarter.coerceAtMost(4)
            )
        }
    }.getOrDefault(emptyList())

    private fun mapRemoteGameToDomain(
        g: EuroLeagueRemoteGameDto,
        seasonCode: String,
        homeTeam: EuroLeagueTeam,
        awayTeam: EuroLeagueTeam,
        boxHome: List<PlayerBoxscoreStat>,
        boxAway: List<PlayerBoxscoreStat>,
        shots: List<ShotAttempt>
    ): EuroLeagueGame {
        val tipOffMillis = runCatching {
            Instant.parse(g.utcDate ?: "2026-10-30T19:30:00Z").toEpochMilli()
        }.getOrDefault(System.currentTimeMillis() + 86_400_000L)

        val statusRaw = (g.gameStatus ?: "").lowercase()
        val isLive = "live" in statusRaw || "playing" in statusRaw || "progress" in statusRaw
        val status = when {
            isLive -> GameStatus.LIVE
            g.played -> GameStatus.FINAL
            else -> GameStatus.UPCOMING
        }

        val hp = g.local.partials ?: EuroLeaguePartialsDto()
        val ap = g.road.partials ?: EuroLeaguePartialsDto()
        val q1H = hp.partials1 ?: 0
        val q2H = hp.partials2 ?: 0
        val q3H = hp.partials3 ?: 0
        val q4H = hp.partials4 ?: 0
        val q1A = ap.partials1 ?: 0
        val q2A = ap.partials2 ?: 0
        val q3A = ap.partials3 ?: 0
        val q4A = ap.partials4 ?: 0

        val hScore = g.local.score ?: (q1H + q2H + q3H + q4H)
        val aScore = g.road.score ?: (q1A + q2A + q3A + q4A)
        val sumRegH = q1H + q2H + q3H + q4H
        val sumRegA = q1A + q2A + q3A + q4A

        return EuroLeagueGame(
            id = "${seasonCode}_${g.gameCode}",
            gameCode = g.gameCode,
            seasonCode = seasonCode,
            round = g.round,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            tipOffEpochMillis = tipOffMillis,
            status = status,
            currentQuarter = when {
                status == GameStatus.UPCOMING -> 1
                g.played && hScore > sumRegH && sumRegH > 0 -> 5
                else -> 4
            },
            remainingClock = when (status) {
                GameStatus.FINAL -> "00:00"
                GameStatus.LIVE -> "02:15"
                GameStatus.UPCOMING -> "10:00"
            },
            homeScore = hScore,
            awayScore = aScore,
            quarters = QuarterScores(
                q1Home = q1H,
                q1Away = q1A,
                q2Home = q2H,
                q2Away = q2A,
                q3Home = q3H,
                q3Away = q3A,
                q4Home = q4H,
                q4Away = q4A,
                otHome = if (g.played && hScore > sumRegH && sumRegH > 0) hScore - sumRegH else null,
                otAway = if (g.played && aScore > sumRegA && sumRegA > 0) aScore - sumRegA else null
            ),
            headToHeadHomeWins = 6,
            headToHeadAwayWins = 5,
            boxscoreHome = boxHome,
            boxscoreAway = boxAway,
            shots = shots
        )
    }

    private fun buildFallbackBoxscoreFromRoster(
        teamId: String,
        players: List<PlayerProfile>
    ): List<PlayerBoxscoreStat> {
        return players.filter { it.teamId == teamId }.take(8).map { p ->
            val pts = p.ppg.roundToInt().coerceAtLeast(4)
            val reb = p.rpg.roundToInt().coerceAtLeast(2)
            val ast = p.apg.roundToInt().coerceAtLeast(1)
            val twoM = (pts / 3).coerceAtLeast(1)
            val threeM = ((pts - twoM * 2) / 3).coerceAtLeast(0)
            val ftM = (pts - twoM * 2 - threeM * 3).coerceAtLeast(0)
            PlayerBoxscoreStat(
                playerId = p.id,
                playerName = p.name,
                jerseyNumber = p.number,
                teamId = teamId,
                minutes = "24:10",
                points = pts,
                offRebounds = reb / 3,
                defRebounds = reb - (reb / 3),
                assists = ast,
                steals = 1,
                blocks = 0,
                turnovers = 1,
                twoPtMade = twoM,
                twoPtAttempted = twoM + 2,
                threePtMade = threeM,
                threePtAttempted = threeM + 2,
                ftMade = ftM,
                ftAttempted = ftM + 1,
                foulsDrawn = 3,
                foulsCommitted = 2
            )
        }
    }

    private fun buildFallbackShotChartFromBoxscores(
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String,
        boxHome: List<PlayerBoxscoreStat>,
        boxAway: List<PlayerBoxscoreStat>
    ): List<ShotAttempt> {
        val spots = listOf(
            Triple(0.11f, 0.20f, true),
            Triple(0.89f, 0.20f, true),
            Triple(0.20f, 0.68f, true),
            Triple(0.80f, 0.68f, true),
            Triple(0.50f, 0.80f, true),
            Triple(0.36f, 0.76f, true),
            Triple(0.64f, 0.76f, true),
            Triple(0.49f, 0.16f, false),
            Triple(0.45f, 0.22f, false),
            Triple(0.55f, 0.22f, false),
            Triple(0.35f, 0.36f, false),
            Triple(0.65f, 0.36f, false),
            Triple(0.50f, 0.45f, false),
            Triple(0.28f, 0.28f, false),
            Triple(0.72f, 0.28f, false),
            Triple(0.42f, 0.54f, false),
            Triple(0.58f, 0.54f, false),
            Triple(0.50f, 0.30f, false)
        )
        val combined = mutableListOf<Pair<String, PlayerBoxscoreStat>>()
        val maxLen = maxOf(boxHome.size, boxAway.size, 1)
        for (i in 0 until maxLen) {
            if (i < boxHome.size) combined.add(homeTeamId to boxHome[i])
            if (i < boxAway.size) combined.add(awayTeamId to boxAway[i])
        }
        if (combined.isEmpty()) return emptyList()
        return (0 until 36).map { idx ->
            val (teamId, p) = combined[idx % combined.size]
            val spot = spots[(idx + gameCode) % spots.size]
            val shiftX = (((gameCode * 13 + idx * 7) % 9) - 4) * 0.008f
            val shiftY = (((gameCode * 17 + idx * 11) % 9) - 4) * 0.008f
            ShotAttempt(
                id = "shot_${gameCode}_fb_$idx",
                playerId = p.playerId,
                playerName = p.playerName,
                teamId = teamId,
                courtX = (spot.first + shiftX).coerceIn(0.07f, 0.93f),
                courtY = (spot.second + shiftY).coerceIn(0.10f, 0.90f),
                made = ((idx + gameCode) % 10) < (if (spot.third) 4 else 6),
                isThreePointer = spot.third,
                quarter = (idx % 4) + 1
            )
        }
    }

    private fun formatPlayerDisplayName(person: EuroLeaguePersonDetailDto): String {
        val given = person.passportName?.substringBefore(",")?.trim().orEmpty()
        val surname = person.passportSurname?.substringBefore(",")?.trim().orEmpty()
        if (given.isNotEmpty() && surname.isNotEmpty()) {
            return "$given $surname".lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
        val raw = person.name.trim()
        if ("," in raw) {
            val parts = raw.split(",", limit = 2).map { it.trim() }
            val firstPart = parts[1].substringBefore(",").trim()
            return "$firstPart ${parts[0]}".lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
        return raw.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    private fun matchPlayerInjury(
        fullName: String,
        surname: String,
        injuryMap: Map<String, Pair<InjuryStatus, String>>
    ): Pair<InjuryStatus, String>? {
        val upperFull = fullName.uppercase()
        injuryMap[upperFull]?.let { return it }
        val upperSurname = surname.trim().uppercase()
        if (upperSurname.length >= 4) {
            injuryMap.entries.firstOrNull { (k, _) -> upperSurname in k }?.let { return it.value }
        }
        return null
    }

    private fun mapPositionCode(posName: String?): String {
        val p = posName.orEmpty().lowercase()
        return when {
            "point" in p -> "PG"
            "shooting" in p -> "SG"
            "guard" in p -> "G"
            "small" in p -> "SF"
            "power" in p -> "PF"
            "forward" in p -> "F"
            "center" in p -> "C"
            else -> "G/F"
        }
    }

    private fun extractCityFromAddress(address: String?): String {
        if (address.isNullOrBlank()) return ""
        return address.split(",").lastOrNull()?.trim().orEmpty()
    }

    /**
     * v2.0 Full Offline Gamecast Restore:
     * Stellt beim Kaltstart oder im Flugmodus alle lokal in Room gespeicherten Spielstände,
     * Boxscores, Half-Court Shot-Charts, Play-by-Play-Feeds und Spieler-Kader wieder her.
     */
    suspend fun restoreFromRoomCacheIfAvailable(): Boolean = withContext(ioDispatcher) {
        runCatching {
            val cachedTeams = dao.getAllTeamsOnce()
            val cachedGames = dao.getAllGamesOnce()
            val cachedPlayers = dao.getAllPlayersOnce()
            if (cachedGames.isEmpty()) return@runCatching false

            val existingTeamsById = fallbackMockRepository.teams.associateBy { it.id }
            val restoredTeams = if (cachedTeams.size >= 16) {
                cachedTeams.map { t ->
                    val existing = existingTeamsById[t.id]
                    EuroLeagueTeam(
                        id = t.id,
                        code = t.code,
                        name = t.name,
                        city = t.city,
                        country = t.country,
                        arena = t.arena,
                        primaryColorHex = t.primaryColorHex,
                        secondaryColorHex = t.secondaryColorHex,
                        accentColorHex = if (t.accentColorHex != 0L) t.accentColorHex else (existing?.accentColorHex ?: 0xFFFFFFFF),
                        wins = t.wins,
                        losses = t.losses,
                        pointsFor = t.pointsFor,
                        pointsAgainst = t.pointsAgainst,
                        totalPointsFor = if (t.totalPointsFor > 0) t.totalPointsFor else t.pointsFor,
                        totalPointsAgainst = if (t.totalPointsAgainst > 0) t.totalPointsAgainst else t.pointsAgainst,
                        streak = t.streakCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        offensiveRating = t.offensiveRating,
                        defensiveRating = t.defensiveRating,
                        pace = t.pace,
                        threePointPct = t.threePointPct,
                        twoPointPct = t.twoPointPct,
                        reboundRate = t.reboundRate,
                        logoUrl = t.logoUrl.ifBlank { existing?.logoUrl.orEmpty() },
                        localLogoResName = t.localLogoResName.ifBlank { "club_logo_${t.id.lowercase()}" }
                    )
                }
            } else {
                fallbackMockRepository.teams
            }

            val teamMap = restoredTeams.associateBy { it.id }
            val existingGamesById = fallbackMockRepository.games.value.associateBy { it.id }

            val restoredPlayers = if (cachedPlayers.isNotEmpty()) {
                cachedPlayers.map { p ->
                    PlayerProfile(
                        id = p.id,
                        teamId = p.teamId,
                        number = p.number,
                        name = p.name,
                        position = p.position,
                        nationality = p.nationality,
                        heightCm = p.heightCm,
                        ppg = p.ppg,
                        rpg = p.rpg,
                        apg = p.apg,
                        pir = p.pir,
                        injuryStatus = runCatching { InjuryStatus.valueOf(p.injuryStatusName) }.getOrDefault(InjuryStatus.FIT),
                        injuryDetail = p.injuryDetail,
                        headshotUrl = p.headshotUrl
                    )
                }
            } else {
                fallbackMockRepository.players
            }

            val cachedById = cachedGames.associateBy { it.id }
            val baseGames = fallbackMockRepository.games.value
            val baseIds = baseGames.map { it.id }.toSet()
            val mergedGames = if (baseGames.isNotEmpty()) {
                val updatedBase = baseGames.map { base ->
                    val cg = cachedById[base.id] ?: return@map base
                    fromCachedGameEntity(cg, teamMap, base)
                }
                val extraFromRoom = cachedGames
                    .filter { it.id !in baseIds }
                    .mapNotNull { cg -> fromCachedGameEntityOrNull(cg, teamMap, existingGamesById[cg.id]) }
                updatedBase + extraFromRoom
            } else {
                cachedGames.mapNotNull { cg ->
                    val fallback = existingGamesById[cg.id]
                    fromCachedGameEntityOrNull(cg, teamMap, fallback)
                }
            }

            val activeRound = mergedGames
                .filter { it.status == GameStatus.LIVE || (it.status == GameStatus.FINAL && (it.homeScore > 0 || it.awayScore > 0)) }
                .maxOfOrNull { it.round }
                ?: fallbackMockRepository.activeRoundFlow.value

            fallbackMockRepository.applyLiveDataset(
                liveTeams = restoredTeams,
                livePlayers = restoredPlayers,
                liveGames = mergedGames,
                activeRound = activeRound,
                seasonCode = mergedGames.firstOrNull()?.seasonCode ?: fallbackMockRepository.seasonCodeFlow.value,
                syncLabel = "OFFLINE CACHE • Round $activeRound"
            )
            true
        }.getOrDefault(false)
    }

    private fun fromCachedGameEntity(
        cg: CachedGameEntity,
        teamMap: Map<String, EuroLeagueTeam>,
        base: EuroLeagueGame
    ): EuroLeagueGame {
        val roomStatus = runCatching { GameStatus.valueOf(cg.status) }.getOrDefault(base.status)
        val useRoomScore = roomStatus == GameStatus.FINAL || roomStatus == GameStatus.LIVE ||
            (cg.homeScore + cg.awayScore) > (base.homeScore + base.awayScore)

        val decodedBoxH = decodeJsonListOrEmpty<PlayerBoxscoreStat>(cg.boxscoreHomeJson)
        val decodedBoxA = decodeJsonListOrEmpty<PlayerBoxscoreStat>(cg.boxscoreAwayJson)
        val decodedShots = decodeJsonListOrEmpty<ShotAttempt>(cg.shotsJson)
        val decodedPbp = decodeJsonListOrEmpty<PlayByPlayEvent>(cg.playByPlayJson)

        return base.copy(
            homeTeam = teamMap[cg.homeTeamId] ?: base.homeTeam,
            awayTeam = teamMap[cg.awayTeamId] ?: base.awayTeam,
            status = if (useRoomScore) roomStatus else base.status,
            currentQuarter = if (useRoomScore) cg.currentQuarter else base.currentQuarter,
            remainingClock = if (useRoomScore) cg.remainingClock else base.remainingClock,
            homeScore = if (useRoomScore) cg.homeScore else base.homeScore,
            awayScore = if (useRoomScore) cg.awayScore else base.awayScore,
            quarters = if (useRoomScore) {
                QuarterScores(
                    q1Home = cg.q1Home,
                    q1Away = cg.q1Away,
                    q2Home = cg.q2Home,
                    q2Away = cg.q2Away,
                    q3Home = cg.q3Home,
                    q3Away = cg.q3Away,
                    q4Home = cg.q4Home,
                    q4Away = cg.q4Away,
                    otHome = cg.otHome,
                    otAway = cg.otAway
                )
            } else {
                base.quarters
            },
            boxscoreHome = decodedBoxH.ifEmpty { base.boxscoreHome },
            boxscoreAway = decodedBoxA.ifEmpty { base.boxscoreAway },
            shots = decodedShots.ifEmpty { base.shots },
            playByPlay = decodedPbp.ifEmpty { base.playByPlay }
        )
    }

    private fun fromCachedGameEntityOrNull(
        cg: CachedGameEntity,
        teamMap: Map<String, EuroLeagueTeam>,
        fallback: EuroLeagueGame?
    ): EuroLeagueGame? {
        val home = teamMap[cg.homeTeamId] ?: fallback?.homeTeam ?: return null
        val away = teamMap[cg.awayTeamId] ?: fallback?.awayTeam ?: return null
        return EuroLeagueGame(
            id = cg.id,
            gameCode = if (cg.gameCode > 0) cg.gameCode else (cg.id.substringAfterLast("_").toIntOrNull() ?: 0),
            seasonCode = cg.seasonCode.ifBlank { "E2026" },
            round = cg.round,
            homeTeam = home,
            awayTeam = away,
            tipOffEpochMillis = cg.tipOffEpochMillis,
            status = runCatching { GameStatus.valueOf(cg.status) }.getOrDefault(GameStatus.UPCOMING),
            currentQuarter = cg.currentQuarter,
            remainingClock = cg.remainingClock,
            homeScore = cg.homeScore,
            awayScore = cg.awayScore,
            quarters = QuarterScores(
                q1Home = cg.q1Home,
                q1Away = cg.q1Away,
                q2Home = cg.q2Home,
                q2Away = cg.q2Away,
                q3Home = cg.q3Home,
                q3Away = cg.q3Away,
                q4Home = cg.q4Home,
                q4Away = cg.q4Away,
                otHome = cg.otHome,
                otAway = cg.otAway
            ),
            headToHeadHomeWins = cg.headToHeadHomeWins,
            headToHeadAwayWins = cg.headToHeadAwayWins,
            boxscoreHome = decodeJsonListOrEmpty(cg.boxscoreHomeJson),
            boxscoreAway = decodeJsonListOrEmpty(cg.boxscoreAwayJson),
            shots = decodeJsonListOrEmpty(cg.shotsJson),
            playByPlay = decodeJsonListOrEmpty(cg.playByPlayJson)
        )
    }

    private inline fun <reified T> decodeJsonListOrEmpty(raw: String): List<T> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
    }

    private fun toCachedGameEntity(g: EuroLeagueGame): CachedGameEntity = CachedGameEntity(
        id = g.id,
        gameCode = g.gameCode,
        seasonCode = g.seasonCode,
        round = g.round,
        homeTeamId = g.homeTeam.id,
        awayTeamId = g.awayTeam.id,
        tipOffEpochMillis = g.tipOffEpochMillis,
        status = g.status.name,
        currentQuarter = g.currentQuarter,
        remainingClock = g.remainingClock,
        homeScore = g.homeScore,
        awayScore = g.awayScore,
        q1Home = g.quarters.q1Home,
        q1Away = g.quarters.q1Away,
        q2Home = g.quarters.q2Home,
        q2Away = g.quarters.q2Away,
        q3Home = g.quarters.q3Home,
        q3Away = g.quarters.q3Away,
        q4Home = g.quarters.q4Home,
        q4Away = g.quarters.q4Away,
        otHome = g.quarters.otHome,
        otAway = g.quarters.otAway,
        headToHeadHomeWins = g.headToHeadHomeWins,
        headToHeadAwayWins = g.headToHeadAwayWins,
        boxscoreHomeJson = if (g.boxscoreHome.isNotEmpty()) runCatching { json.encodeToString(g.boxscoreHome) }.getOrDefault("") else "",
        boxscoreAwayJson = if (g.boxscoreAway.isNotEmpty()) runCatching { json.encodeToString(g.boxscoreAway) }.getOrDefault("") else "",
        shotsJson = if (g.shots.isNotEmpty()) runCatching { json.encodeToString(g.shots) }.getOrDefault("") else "",
        playByPlayJson = if (g.playByPlay.isNotEmpty()) runCatching { json.encodeToString(g.playByPlay) }.getOrDefault("") else ""
    )

    internal suspend fun persistToRoomCache(
        teams: List<EuroLeagueTeam>,
        games: List<EuroLeagueGame>,
        players: List<PlayerProfile> = fallbackMockRepository.players
    ) {
        val teamEntities = teams.map { team ->
            CachedTeamEntity(
                id = team.id,
                code = team.code,
                name = team.name,
                city = team.city,
                country = team.country,
                arena = team.arena,
                primaryColorHex = team.primaryColorHex,
                secondaryColorHex = team.secondaryColorHex,
                accentColorHex = team.accentColorHex,
                wins = team.wins,
                losses = team.losses,
                pointsFor = team.pointsFor,
                pointsAgainst = team.pointsAgainst,
                totalPointsFor = team.totalPointsFor,
                totalPointsAgainst = team.totalPointsAgainst,
                streakCsv = team.streak.joinToString(","),
                offensiveRating = team.offensiveRating,
                defensiveRating = team.defensiveRating,
                pace = team.pace,
                threePointPct = team.threePointPct,
                twoPointPct = team.twoPointPct,
                reboundRate = team.reboundRate,
                logoUrl = team.logoUrl,
                localLogoResName = team.localLogoResName
            )
        }
        val playerEntities = players.map { p ->
            CachedPlayerEntity(
                id = p.id,
                teamId = p.teamId,
                number = p.number,
                name = p.name,
                position = p.position,
                nationality = p.nationality,
                heightCm = p.heightCm,
                ppg = p.ppg,
                rpg = p.rpg,
                apg = p.apg,
                pir = p.pir,
                injuryStatusName = p.injuryStatus.name,
                injuryDetail = p.injuryDetail,
                headshotUrl = p.headshotUrl
            )
        }
        val gameEntities = games.map { g -> toCachedGameEntity(g) }
        dao.upsertTeams(teamEntities)
        if (playerEntities.isNotEmpty()) {
            dao.upsertPlayers(playerEntities)
        }
        dao.upsertGames(gameEntities)
    }

    companion object {
        internal const val LIVE_CACHE_BUCKET_MS: Long = 3_000L

        internal fun liveCacheBustBucket(nowMillis: Long = System.currentTimeMillis()): Long =
            nowMillis / LIVE_CACHE_BUCKET_MS

        internal fun buildLiveApiUrl(
            endpoint: String,
            gameCode: Int,
            seasonCode: String,
            nowMillis: Long = System.currentTimeMillis()
        ): String =
            "https://live.euroleague.net/api/$endpoint?gamecode=$gameCode&seasoncode=$seasonCode&_t=${liveCacheBustBucket(nowMillis)}"
        internal fun applyLiveHeaderToGameStatic(
            game: EuroLeagueGame,
            header: EuroLeagueLiveHeaderDto
        ): EuroLeagueGame {
            val parsedScoreA = header.scoreA.trim().toIntOrNull()
            val parsedScoreB = header.scoreB.trim().toIntOrNull()
            val hasScore = (parsedScoreA ?: 0) > 0 || (parsedScoreB ?: 0) > 0
            val gameMinutes = header.gameTime.substringBefore(":").trim().toIntOrNull() ?: 0
            val qRaw = header.quarter.trim().uppercase()
            val inferredQuarterFromTime = if (gameMinutes >= 1) {
                ((gameMinutes - 1) / 10 + 1).coerceIn(1, 5)
            } else {
                game.currentQuarter
            }
            val parsedQuarter = when {
                "OT" in qRaw || "E" in qRaw -> 5
                else -> qRaw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 5) ?: inferredQuarterFromTime
            }
            val clockClean = header.remainingPartialTime.trim().ifEmpty {
                if (header.live) "00:00" else game.remainingClock
            }
            val isClockZero = clockClean == "00:00" || clockClean == "0:00" || clockClean.isEmpty()
            val isEndedInHeader = hasScore && (
                !header.live ||
                    (gameMinutes >= 40 && isClockZero && qRaw.isEmpty() && parsedScoreA != parsedScoreB)
                )
            val resolvedStatus = when {
                isEndedInHeader -> GameStatus.FINAL
                header.live && hasScore -> GameStatus.LIVE
                game.status == GameStatus.FINAL -> GameStatus.FINAL
                else -> game.status
            }
            val extraRawA = maxOf(header.scoreExtraTimeA, header.scoreExtra1A)
            val extraRawB = maxOf(header.scoreExtraTimeB, header.scoreExtra1B)
            val hasHeaderPartials = (header.scoreQuarter1A + header.scoreQuarter1B + header.scoreQuarter2A + header.scoreQuarter2B) > 0
            val updatedQuarters = if (hasHeaderPartials) {
                val isCumulativeA = header.scoreQuarter2A == 0 || (header.scoreQuarter2A >= header.scoreQuarter1A && header.scoreQuarter1A > 0)
                val isCumulativeB = header.scoreQuarter2B == 0 || (header.scoreQuarter2B >= header.scoreQuarter1B && header.scoreQuarter1B > 0)
                val q1H = header.scoreQuarter1A.coerceAtLeast(0)
                val q2H = if (header.scoreQuarter2A <= 0) 0 else if (isCumulativeA) (header.scoreQuarter2A - header.scoreQuarter1A).coerceAtLeast(0) else header.scoreQuarter2A
                val q3H = if (header.scoreQuarter3A <= 0) 0 else if (isCumulativeA && header.scoreQuarter3A >= header.scoreQuarter2A) (header.scoreQuarter3A - header.scoreQuarter2A).coerceAtLeast(0) else header.scoreQuarter3A.coerceAtLeast(0)
                val q4H = if (header.scoreQuarter4A <= 0) 0 else if (isCumulativeA && header.scoreQuarter4A >= header.scoreQuarter3A) (header.scoreQuarter4A - header.scoreQuarter3A).coerceAtLeast(0) else header.scoreQuarter4A.coerceAtLeast(0)
                val q1A = header.scoreQuarter1B.coerceAtLeast(0)
                val q2A = if (header.scoreQuarter2B <= 0) 0 else if (isCumulativeB) (header.scoreQuarter2B - header.scoreQuarter1B).coerceAtLeast(0) else header.scoreQuarter2B
                val q3A = if (header.scoreQuarter3B <= 0) 0 else if (isCumulativeB && header.scoreQuarter3B >= header.scoreQuarter2B) (header.scoreQuarter3B - header.scoreQuarter2B).coerceAtLeast(0) else header.scoreQuarter3B.coerceAtLeast(0)
                val q4A = if (header.scoreQuarter4B <= 0) 0 else if (isCumulativeB && header.scoreQuarter4B >= header.scoreQuarter3B) (header.scoreQuarter4B - header.scoreQuarter3B).coerceAtLeast(0) else header.scoreQuarter4B.coerceAtLeast(0)
                val regH = q1H + q2H + q3H + q4H
                val regA = q1A + q2A + q3A + q4A
                val totalH = parsedScoreA ?: game.homeScore
                val totalA = parsedScoreB ?: game.awayScore
                val otH = when {
                    isCumulativeA && extraRawA > header.scoreQuarter4A && header.scoreQuarter4A > 0 -> extraRawA - header.scoreQuarter4A
                    extraRawA > 0 && !isCumulativeA -> extraRawA
                    (resolvedStatus == GameStatus.FINAL || parsedQuarter >= 5) && totalH > regH && regH > 0 -> totalH - regH
                    else -> null
                }
                val otA = when {
                    isCumulativeB && extraRawB > header.scoreQuarter4B && header.scoreQuarter4B > 0 -> extraRawB - header.scoreQuarter4B
                    extraRawB > 0 && !isCumulativeB -> extraRawB
                    (resolvedStatus == GameStatus.FINAL || parsedQuarter >= 5) && totalA > regA && regA > 0 -> totalA - regA
                    else -> null
                }
                QuarterScores(
                    q1Home = q1H,
                    q1Away = q1A,
                    q2Home = q2H,
                    q2Away = q2A,
                    q3Home = q3H,
                    q3Away = q3A,
                    q4Home = q4H,
                    q4Away = q4A,
                    otHome = otH,
                    otAway = otA
                )
            } else {
                game.quarters
            }
            return game.copy(
                status = resolvedStatus,
                currentQuarter = if (resolvedStatus == GameStatus.FINAL && updatedQuarters.otHome != null) 5 else parsedQuarter,
                remainingClock = if (resolvedStatus == GameStatus.FINAL) "00:00" else clockClean,
                homeScore = parsedScoreA ?: game.homeScore,
                awayScore = parsedScoreB ?: game.awayScore,
                quarters = updatedQuarters
            )
        }

        internal fun parseLivePlayByPlayStatic(
            dto: EuroLeagueLivePlayByPlayDto,
            gameCode: Int,
            homeTeamId: String,
            awayTeamId: String
        ): List<PlayByPlayEvent> {
            val quarterBuckets: List<Pair<Int, List<EuroLeaguePlayByPlayRowDto>>> = listOf(
                1 to dto.firstQuarter.orEmpty(),
                2 to dto.secondQuarter.orEmpty(),
                3 to dto.thirdQuarter.orEmpty(),
                4 to (dto.forthQuarter ?: dto.fourthQuarterAlt).orEmpty(),
                5 to dto.extraTime.orEmpty()
            )
            val codeHomeCanon = EuroLeagueMockRepository.canonicalClubId(
                dto.codeTeamA.trim().ifEmpty { homeTeamId }
            )
            var runningHome = 0
            var runningAway = 0
            val out = mutableListOf<PlayByPlayEvent>()

            for ((q, rows) in quarterBuckets) {
                val qDuration = if (q >= 5) 5f else 10f
                val qStart = if (q <= 4) (q - 1) * 10f else 40f

                for ((idx, row) in rows.withIndex()) {
                    val playType = row.playType?.trim()?.uppercase().orEmpty()
                    // Skip silent substitution IN/OUT rows to keep the broadcast feed crisp
                    if (playType == "IN" || playType == "OUT" || playType.isEmpty()) continue

                    val prevH = runningHome
                    val prevA = runningAway
                    if (row.pointsA != null) runningHome = row.pointsA
                    if (row.pointsB != null) runningAway = row.pointsB
                    val ptsAdded = ((runningHome - prevH) + (runningAway - prevA)).coerceAtLeast(0)

                    val rawTeam = row.codeTeam?.trim().orEmpty()
                    val resolvedTeamId = when {
                        rawTeam.isEmpty() -> ""
                        EuroLeagueMockRepository.canonicalClubId(rawTeam) == codeHomeCanon ||
                            EuroLeagueMockRepository.canonicalClubId(rawTeam) == EuroLeagueMockRepository.canonicalClubId(homeTeamId) -> homeTeamId
                        else -> awayTeamId
                    }

                    val clockRaw = row.markerTime?.trim().orEmpty()
                    val clock = if (clockRaw.contains(":")) {
                        clockRaw
                    } else {
                        val minInQ = (row.minute - (q - 1) * 10).coerceIn(0, qDuration.toInt())
                        "%02d:00".format((qDuration.toInt() - minInQ).coerceAtLeast(0))
                    }
                    val cParts = clock.split(":")
                    val remMins = cParts.getOrNull(0)?.toFloatOrNull() ?: 0f
                    val remSecs = cParts.getOrNull(1)?.toFloatOrNull() ?: 0f
                    val elapsedInQ = (qDuration - (remMins + remSecs / 60f)).coerceIn(0f, qDuration)
                    val gameMin = qStart + elapsedInQ

                    val formattedPlayer = formatPlayByPlayPlayerName(row.player)
                    val rawPlayerCode = row.playerId?.trim()?.removePrefix("P").orEmpty()
                    val fullPlayerId = if (resolvedTeamId.isNotEmpty() && rawPlayerCode.isNotEmpty()) {
                        "${resolvedTeamId}_$rawPlayerCode"
                    } else ""

                    val category = when (playType) {
                        "3FGM" -> PlayEventCategory.THREE_PT
                        "DUNK" -> PlayEventCategory.DUNK
                        "2FGM", "LAYUPMD" -> PlayEventCategory.TWO_PT
                        "FTM" -> PlayEventCategory.FREE_THROW
                        "2FGA", "3FGA", "FTA", "LAYUPATT" -> PlayEventCategory.MISS
                        "AS", "D", "O" -> PlayEventCategory.ASSIST_REB
                        "ST", "FV", "AG" -> PlayEventCategory.STEAL_BLK
                        "TO" -> PlayEventCategory.TURNOVER
                        "CM", "CMU", "CMT", "CMD", "OF", "RV" -> PlayEventCategory.FOUL
                        "TVO", "TOUT", "TOUT_TV" -> PlayEventCategory.TIMEOUT
                        else -> PlayEventCategory.PERIOD
                    }

                    val desc = row.playInfo?.trim()?.takeIf { it.isNotEmpty() } ?: category.badgeLabel

                    out.add(
                        PlayByPlayEvent(
                            id = "pbp_${gameCode}_${q}_${row.numberOfPlay}_$idx",
                            playNumber = if (row.numberOfPlay > 0) row.numberOfPlay else idx + 1,
                            quarter = q,
                            clock = clock,
                            gameMinute = gameMin,
                            teamId = resolvedTeamId,
                            playerId = fullPlayerId,
                            playerName = formattedPlayer,
                            jerseyNumber = row.dorsal?.trim()?.toIntOrNull(),
                            playType = playType,
                            category = category,
                            description = desc,
                            homeScore = runningHome,
                            awayScore = runningAway,
                            pointsScored = ptsAdded
                        )
                    )
                }
            }
            return out
        }

        private fun formatPlayByPlayPlayerName(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return ""
            return if ("," in trimmed) {
                val parts = trimmed.split(",", limit = 2).map { it.trim() }
                "${parts[1]} ${parts[0]}".lowercase().split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            } else {
                trimmed.lowercase().split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            }
        }
    }
}
