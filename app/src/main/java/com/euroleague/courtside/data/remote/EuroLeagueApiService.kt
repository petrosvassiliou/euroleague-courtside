package com.euroleague.courtside.data.remote

import android.content.Context
import com.euroleague.courtside.data.local.CachedGameEntity
import com.euroleague.courtside.data.local.CachedTeamEntity
import com.euroleague.courtside.data.local.CourtsideDao
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.InjuryStatus
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
    @SerialName("twoPointersPercentage") val twoPointersPercentage: String? = null,
    @SerialName("threePointersPercentage") val threePointersPercentage: String? = null,
    @SerialName("totalRebounds") val totalRebounds: Double = 0.0,
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
    @SerialName("FoulsCommited") val foulsCommitted: Int = 0,
    @SerialName("FoulsReceived") val foulsDrawn: Int = 0
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
    @SerialName("ScoreQuarter1B") val scoreQuarter1B: Int = 0,
    @SerialName("ScoreQuarter2B") val scoreQuarter2B: Int = 0,
    @SerialName("ScoreQuarter3B") val scoreQuarter3B: Int = 0,
    @SerialName("ScoreQuarter4B") val scoreQuarter4B: Int = 0,
    @SerialName("ScoreExtra1B") val scoreExtra1B: Int = 0
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
    val turnovers: Int,
    val twoPtMade: Int,
    val twoPtAttempted: Int,
    val threePtMade: Int,
    val threePtAttempted: Int,
    val ftMade: Int,
    val ftAttempted: Int,
    val foulsDrawn: Int,
    val foulsCommitted: Int
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
    private val apiSemaphore = kotlinx.coroutines.sync.Semaphore(4)

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
                streak = dto.streak,
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
                        turnovers = b.turnovers,
                        twoPtMade = b.twoPtMade,
                        twoPtAttempted = b.twoPtAttempted,
                        threePtMade = b.threePtMade,
                        threePtAttempted = b.threePtAttempted,
                        ftMade = b.ftMade,
                        ftAttempted = b.ftAttempted,
                        foulsDrawn = b.foulsDrawn,
                        foulsCommitted = b.foulsCommitted
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
                        turnovers = b.turnovers,
                        twoPtMade = b.twoPtMade,
                        twoPtAttempted = b.twoPtAttempted,
                        threePtMade = b.threePtMade,
                        threePtAttempted = b.threePtAttempted,
                        ftMade = b.ftMade,
                        ftAttempted = b.ftAttempted,
                        foulsDrawn = b.foulsDrawn,
                        foulsCommitted = b.foulsCommitted
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

                // 1. Berechne aktuelle Tabellenstände & Formkurven aus allen gespielten Spielen
                data class MutableStanding(
                    var wins: Int = 0,
                    var losses: Int = 0,
                    var pf: Int = 0,
                    var pa: Int = 0,
                    val streak: MutableList<String> = mutableListOf()
                )
                val standingsMap = clubs.associate { it.code to MutableStanding() }.toMutableMap()

                allGamesDto
                    .filter { it.played && it.local.score != null && it.road.score != null }
                    .sortedBy { it.utcDate ?: "" }
                    .forEach { g ->
                        val hCode = g.local.club.code
                        val aCode = g.road.club.code
                        val hScore = g.local.score ?: 0
                        val aScore = g.road.score ?: 0
                        if (hScore == 0 && aScore == 0) return@forEach
                        val hRec = standingsMap.getOrPut(hCode) { MutableStanding() }
                        val aRec = standingsMap.getOrPut(aCode) { MutableStanding() }
                        hRec.pf += hScore
                        hRec.pa += aScore
                        aRec.pf += aScore
                        aRec.pa += hScore
                        if (hScore >= aScore) {
                            hRec.wins += 1
                            aRec.losses += 1
                            hRec.streak.add("S")
                            aRec.streak.add("N")
                        } else {
                            aRec.wins += 1
                            hRec.losses += 1
                            aRec.streak.add("S")
                            hRec.streak.add("N")
                        }
                    }

                val liveTeams = clubs.map { c ->
                    val code = c.code
                    val tvCode = c.tvCode ?: code
                    val st = standingsMap[code] ?: MutableStanding()
                    val existing = existingTeamsById[code] ?: existingTeamsById[tvCode]
                    val tStat = teamStatsMap[code] ?: teamStatsMap[tvCode]
                    val gp = (st.wins + st.losses).coerceAtLeast(1)
                    val ppg = if ((tStat?.pointsScored ?: 0.0) > 0.0) {
                        tStat!!.pointsScored.toFloat()
                    } else if (st.pf > 0) {
                        st.pf.toFloat() / gp
                    } else {
                        82f
                    }
                    val oppg = if (st.pa > 0) st.pa.toFloat() / gp else 80f
                    val parsedTwoPct = parsePercentageString(tStat?.twoPointersPercentage)
                    val parsedThreePct = parsePercentageString(tStat?.threePointersPercentage)
                    val parsedRebRate = if ((tStat?.totalRebounds ?: 0.0) > 0.0) {
                        ((tStat!!.totalRebounds.toFloat() * 1.42f * 10f).roundToInt() / 10f).coerceIn(44.5f, 57.5f)
                    } else {
                        null
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
                        pointsFor = st.pf,
                        pointsAgainst = st.pa,
                        streak = st.streak.takeLast(5).ifEmpty { existing?.streak ?: listOf("S", "N", "S", "S", "N") },
                        offensiveRating = ((ppg * 1.32f * 10f).roundToInt() / 10f),
                        defensiveRating = ((oppg * 1.32f * 10f).roundToInt() / 10f),
                        pace = (((70f + (ppg - 78f) * 0.35f) * 10f).roundToInt() / 10f),
                        threePointPct = parsedThreePct ?: existing?.threePointPct ?: (((34.5f + (ppg - 78f) * 0.32f) * 10f).roundToInt() / 10f),
                        twoPointPct = parsedTwoPct ?: existing?.twoPointPct ?: (((52.5f + (ppg - 78f) * 0.38f) * 10f).roundToInt() / 10f),
                        reboundRate = parsedRebRate ?: existing?.reboundRate ?: (((48.5f + (st.wins - st.losses) * 0.45f) * 10f).roundToInt() / 10f),
                        logoUrl = c.images?.crest ?: existing?.logoUrl ?: "",
                        localLogoResName = "club_logo_${code.lowercase()}"
                    )
                }.sortedWith(compareByDescending<EuroLeagueTeam> { it.wins }.thenByDescending { it.pointDiff })

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

                val liveGames = coroutineScope {
                    sortedAllGamesDto.mapNotNull { g ->
                        val homeTeam = teamById[g.local.club.code] ?: return@mapNotNull null
                        val awayTeam = teamById[g.road.club.code] ?: return@mapNotNull null
                        val gameId = "${seasonCode}_${g.gameCode}"
                        val cachedGame = latestCachedById[gameId]
                            ?: existingGamesById[gameId]
                            ?: latestCachedByCode[g.gameCode]
                        async {
                            val isActiveRound = g.round == activeRound
                            val (fetchedBoxHome, fetchedBoxAway) = if (isActiveRound && g.played) {
                                apiSemaphore.withPermit {
                                    fetchGameBoxscores(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                                }
                            } else {
                                emptyList<PlayerBoxscoreStat>() to emptyList()
                            }
                            val fetchedShots = if (isActiveRound && g.played) {
                                apiSemaphore.withPermit {
                                    fetchGameShotChart(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                                }
                            } else {
                                emptyList()
                            }

                            val finalBoxHome = when {
                                !g.played -> emptyList()
                                fetchedBoxHome.isNotEmpty() -> fetchedBoxHome
                                cachedGame != null && cachedGame.boxscoreHome.isNotEmpty() -> cachedGame.boxscoreHome
                                else -> buildFallbackBoxscoreFromRoster(homeTeam.id, allVerifiedPlayers)
                            }
                            val finalBoxAway = when {
                                !g.played -> emptyList()
                                fetchedBoxAway.isNotEmpty() -> fetchedBoxAway
                                cachedGame != null && cachedGame.boxscoreAway.isNotEmpty() -> cachedGame.boxscoreAway
                                else -> buildFallbackBoxscoreFromRoster(awayTeam.id, allVerifiedPlayers)
                            }
                            val finalShots = when {
                                !g.played -> emptyList()
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

                            mapRemoteGameToDomain(
                                g = g,
                                seasonCode = seasonCode,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                boxHome = finalBoxHome,
                                boxAway = finalBoxAway,
                                shots = finalShots
                            )
                        }
                    }.awaitAll()
                }

                fallbackMockRepository.applyLiveDataset(
                    liveTeams = liveTeams,
                    livePlayers = allVerifiedPlayers,
                    liveGames = liveGames,
                    activeRound = activeRound,
                    seasonCode = seasonCode,
                    syncLabel = "$seasonCode • Round $activeRound"
                )

                persistToRoomCache(liveTeams, liveGames)
            }.recoverCatching {
                // Offline-Fallback: Speichere aktuellen Stand aus dem Mock/Snapshot-Repository in Room
                persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
            }
        }

    /**
     * Schneller, Rate-Limit-schonender Live-Ticker (1 Request für aktuelle Spielstände/Viertel +
     * `live.euroleague.net/api/Header` & Live-Boxscore-Update für laufende Partien), damit die App
     * im Dauerbetrieb niemals HTTP 429 auslöst und gleichzeitig Live-Scores in Echtzeit aktualisiert.
     */
    suspend fun refreshLiveMatchdayTick(): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val seasonCode = fallbackMockRepository.seasonCodeFlow.value
                val allGamesDto = apiService.fetchSeasonGames(seasonCode = seasonCode, round = null, limit = 400).data
                if (allGamesDto.isEmpty()) {
                    return@runCatching
                }

                val sortedAllGamesDto = allGamesDto.sortedWith(
                    compareBy<EuroLeagueRemoteGameDto> { it.round }
                        .thenBy { it.utcDate ?: "" }
                        .thenBy { it.gameCode }
                )
                val activeRound = resolveCurrentActiveRound(emptyList(), sortedAllGamesDto)

                val teamById = fallbackMockRepository.teams.associateBy { it.id }
                val cachedById = fallbackMockRepository.games.value.associateBy { it.id }

                val updatedGames = sortedAllGamesDto.mapNotNull { g ->
                    val homeTeam = teamById[g.local.club.code] ?: return@mapNotNull null
                    val awayTeam = teamById[g.road.club.code] ?: return@mapNotNull null
                    val gameId = "${seasonCode}_${g.gameCode}"
                    val cached = cachedById[gameId]
                    val statusRaw = (g.gameStatus ?: "").lowercase()
                    val isStatusLive = "live" in statusRaw || "playing" in statusRaw || "progress" in statusRaw
                    val candidateHeader = if (isStatusLive || (!g.played && g.round == activeRound)) {
                        fetchGameLiveHeader(seasonCode, g.gameCode)
                    } else null
                    val headerHasScore = (candidateHeader?.scoreA?.trim()?.toIntOrNull() ?: 0) > 0 ||
                        (candidateHeader?.scoreB?.trim()?.toIntOrNull() ?: 0) > 0
                    val isLive = isStatusLive || (candidateHeader?.live == true && headerHasScore)
                    val justFinished = (g.played || (candidateHeader != null && !candidateHeader.live && headerHasScore)) &&
                        cached?.status != GameStatus.FINAL

                    val (boxH, boxA) = if (isLive || justFinished) {
                        fetchGameBoxscores(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                    } else {
                        (cached?.boxscoreHome ?: emptyList()) to (cached?.boxscoreAway ?: emptyList())
                    }
                    val shots = if (isLive || justFinished) {
                        fetchGameShotChart(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
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
                    )

                    if (candidateHeader != null && (isLive || candidateHeader.live || headerHasScore)) {
                        applyLiveHeaderToGame(mapped, candidateHeader)
                    } else {
                        mapped
                    }
                }

                if (updatedGames.isNotEmpty()) {
                    fallbackMockRepository.applyLiveDataset(
                        liveTeams = fallbackMockRepository.teams,
                        livePlayers = fallbackMockRepository.players,
                        liveGames = updatedGames,
                        activeRound = activeRound,
                        seasonCode = seasonCode,
                        syncLabel = "$seasonCode • Round $activeRound"
                    )
                }
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
     * Lädt Live-Boxscore, echte Shot-Chart-Koordinaten und (falls live) den aktuellen
     * `live.euroleague.net/api/Header` Spielstand für ein ausgewähltes Spiel nach.
     */
    suspend fun refreshSelectedGameLiveStats(game: EuroLeagueGame): Result<EuroLeagueGame> =
        withContext(ioDispatcher) {
            runCatching {
                if (game.status == GameStatus.UPCOMING) {
                    return@runCatching game
                }
                val liveHeader = if (game.status == GameStatus.LIVE) {
                    fetchGameLiveHeader(game.seasonCode, game.gameCode)
                } else null
                val (boxHome, boxAway) = fetchGameBoxscores(
                    seasonCode = game.seasonCode,
                    gameCode = game.gameCode,
                    homeTeamId = game.homeTeam.id,
                    awayTeamId = game.awayTeam.id
                )
                val shots = fetchGameShotChart(
                    seasonCode = game.seasonCode,
                    gameCode = game.gameCode,
                    homeTeamId = game.homeTeam.id,
                    awayTeamId = game.awayTeam.id
                )
                val resolvedHome = boxHome.ifEmpty {
                    game.boxscoreHome.ifEmpty {
                        buildFallbackBoxscoreFromRoster(game.homeTeam.id, fallbackMockRepository.players)
                    }
                }
                val resolvedAway = boxAway.ifEmpty {
                    game.boxscoreAway.ifEmpty {
                        buildFallbackBoxscoreFromRoster(game.awayTeam.id, fallbackMockRepository.players)
                    }
                }
                val resolvedShots = shots.ifEmpty {
                    game.shots.ifEmpty {
                        buildFallbackShotChartFromBoxscores(
                            gameCode = game.gameCode,
                            homeTeamId = game.homeTeam.id,
                            awayTeamId = game.awayTeam.id,
                            boxHome = resolvedHome,
                            boxAway = resolvedAway
                        )
                    }
                }
                val baseUpdated = game.copy(
                    boxscoreHome = resolvedHome,
                    boxscoreAway = resolvedAway,
                    shots = resolvedShots
                )
                val updated = if (liveHeader != null) {
                    applyLiveHeaderToGame(baseUpdated, liveHeader)
                } else {
                    baseUpdated
                }
                fallbackMockRepository.updateSingleGameDetails(updated)
                updated
            }
        }

    private suspend fun fetchGameLiveHeader(
        seasonCode: String,
        gameCode: Int
    ): EuroLeagueLiveHeaderDto? = runCatching {
        val url = "https://live.euroleague.net/api/Header?gamecode=$gameCode&seasoncode=$seasonCode"
        apiService.fetchLiveGameHeader(url)
    }.getOrNull()

    internal fun applyLiveHeaderToGame(
        game: EuroLeagueGame,
        header: EuroLeagueLiveHeaderDto
    ): EuroLeagueGame {
        val parsedScoreA = header.scoreA.trim().toIntOrNull()
        val parsedScoreB = header.scoreB.trim().toIntOrNull()
        val hasScore = (parsedScoreA ?: 0) > 0 || (parsedScoreB ?: 0) > 0
        val qRaw = header.quarter.trim().uppercase()
        val parsedQuarter = when {
            "OT" in qRaw || "E" in qRaw -> 5
            else -> qRaw.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 5) ?: game.currentQuarter
        }
        val clockClean = header.remainingPartialTime.trim().ifEmpty { game.remainingClock }
        val gameMinutes = header.gameTime.substringBefore(":").trim().toIntOrNull() ?: 0
        val isClockZero = clockClean == "00:00" || clockClean == "0:00" || clockClean.isEmpty()
        val isEndedInHeader = hasScore && (
            !header.live ||
                (gameMinutes >= 40 && isClockZero && qRaw.isEmpty() && parsedScoreA != parsedScoreB)
            )
        val resolvedStatus = when {
            game.status == GameStatus.FINAL || isEndedInHeader -> GameStatus.FINAL
            header.live && hasScore -> GameStatus.LIVE
            else -> game.status
        }
        val hasHeaderPartials = (header.scoreQuarter1A + header.scoreQuarter1B + header.scoreQuarter2A + header.scoreQuarter2B) > 0
        val updatedQuarters = if (hasHeaderPartials) {
            val isCumulativeA = header.scoreQuarter2A >= header.scoreQuarter1A && header.scoreQuarter1A > 0
            val isCumulativeB = header.scoreQuarter2B >= header.scoreQuarter1B && header.scoreQuarter1B > 0
            val q1H = header.scoreQuarter1A
            val q2H = if (isCumulativeA) (header.scoreQuarter2A - header.scoreQuarter1A).coerceAtLeast(0) else header.scoreQuarter2A
            val q3H = if (isCumulativeA && header.scoreQuarter3A >= header.scoreQuarter2A) (header.scoreQuarter3A - header.scoreQuarter2A).coerceAtLeast(0) else header.scoreQuarter3A.coerceAtLeast(0)
            val q4H = if (isCumulativeA && header.scoreQuarter4A >= header.scoreQuarter3A) (header.scoreQuarter4A - header.scoreQuarter3A).coerceAtLeast(0) else header.scoreQuarter4A.coerceAtLeast(0)
            val q1A = header.scoreQuarter1B
            val q2A = if (isCumulativeB) (header.scoreQuarter2B - header.scoreQuarter1B).coerceAtLeast(0) else header.scoreQuarter2B
            val q3A = if (isCumulativeB && header.scoreQuarter3B >= header.scoreQuarter2B) (header.scoreQuarter3B - header.scoreQuarter2B).coerceAtLeast(0) else header.scoreQuarter3B.coerceAtLeast(0)
            val q4A = if (isCumulativeB && header.scoreQuarter4B >= header.scoreQuarter3B) (header.scoreQuarter4B - header.scoreQuarter3B).coerceAtLeast(0) else header.scoreQuarter4B.coerceAtLeast(0)
            val regH = q1H + q2H + q3H + q4H
            val regA = q1A + q2A + q3A + q4A
            val totalH = parsedScoreA ?: game.homeScore
            val totalA = parsedScoreB ?: game.awayScore
            val otH = when {
                isCumulativeA && header.scoreExtra1A > header.scoreQuarter4A -> header.scoreExtra1A - header.scoreQuarter4A
                header.scoreExtra1A > 0 && !isCumulativeA -> header.scoreExtra1A
                totalH > regH && regH > 0 -> totalH - regH
                else -> null
            }
            val otA = when {
                isCumulativeB && header.scoreExtra1B > header.scoreQuarter4B -> header.scoreExtra1B - header.scoreQuarter4B
                header.scoreExtra1B > 0 && !isCumulativeB -> header.scoreExtra1B
                totalA > regA && regA > 0 -> totalA - regA
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

    private fun resolveCurrentActiveRound(
        rounds: List<EuroLeagueRoundDto>,
        allGamesDto: List<EuroLeagueRemoteGameDto>
    ): Int {
        val playedRounds = allGamesDto
            .filter { it.played || (it.local.score ?: 0) > 0 || (it.road.score ?: 0) > 0 }
            .map { it.round }
            .filter { it in 1..38 }
        if (playedRounds.isNotEmpty()) {
            return playedRounds.maxOrNull() ?: 11
        }
        if (rounds.isEmpty()) return fallbackMockRepository.activeRoundFlow.value.coerceIn(1, 38)
        val nowIso = Instant.now().toString()
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
     * 1. Primär: `api-live.euroleague.net/v2/competitions/E/seasons/{seasonCode}/games/{gameCode}/stats`
     * 2. Sekundär (Fallback bei Rate-Limit): `live.euroleague.net/api/Boxscore?gamecode={gameCode}&seasoncode={seasonCode}`
     */
    private suspend fun fetchGameBoxscores(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ): Pair<List<PlayerBoxscoreStat>, List<PlayerBoxscoreStat>> {
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
            val url = "https://live.euroleague.net/api/Boxscore?gamecode=$gameCode&seasoncode=$seasonCode"
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
                turnovers = r.turnovers,
                twoPtMade = r.fgm2,
                twoPtAttempted = r.fga2,
                threePtMade = r.fgm3,
                threePtAttempted = r.fga3,
                ftMade = r.ftm,
                ftAttempted = r.fta,
                foulsDrawn = r.foulsDrawn,
                foulsCommitted = r.foulsCommitted
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
                turnovers = (st.turnovers ?: 0.0).roundToInt(),
                twoPtMade = (st.fieldGoalsMade2 ?: 0.0).roundToInt(),
                twoPtAttempted = (st.fieldGoalsAttempted2 ?: 0.0).roundToInt(),
                threePtMade = (st.fieldGoalsMade3 ?: 0.0).roundToInt(),
                threePtAttempted = (st.fieldGoalsAttempted3 ?: 0.0).roundToInt(),
                ftMade = (st.freeThrowsMade ?: 0.0).roundToInt(),
                ftAttempted = (st.freeThrowsAttempted ?: 0.0).roundToInt(),
                foulsDrawn = (st.foulsReceived ?: 0.0).roundToInt(),
                foulsCommitted = (st.foulsCommited ?: 0.0).roundToInt()
            )
        }.sortedByDescending { it.pir }
    }

    private suspend fun fetchGameShotChart(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ): List<ShotAttempt> = runCatching {
        val url = "https://live.euroleague.net/api/Points?gamecode=$gameCode&seasoncode=$seasonCode"
        val rows = apiService.fetchLiveGameShotPoints(url).rows
        rows.mapIndexedNotNull { idx, row ->
            val act = row.actionId.trim().uppercase()
            if (act !in setOf("2FGM", "2FGA", "3FGM", "3FGA", "DUNK", "LAYUPATT")) return@mapIndexedNotNull null
            val normX = ((row.coordX + 750f) / 1500f).coerceIn(0.06f, 0.94f)
            val normY = ((row.coordY + 150f) / 1400f).coerceIn(0.08f, 0.92f)
            val teamRaw = row.team.trim().uppercase()
            val mappedTeamId = if (teamRaw == homeTeamId || teamRaw.startsWith(homeTeamId.take(3))) homeTeamId else awayTeamId
            val minute = row.minute.coerceAtLeast(1)
            val quarter = (((minute - 1) / 10) + 1).coerceIn(1, 4)

            ShotAttempt(
                id = "shot_${gameCode}_${row.numAnot}_$idx",
                playerId = row.playerId.trim(),
                playerName = row.playerName.trim(),
                teamId = mappedTeamId,
                courtX = normX,
                courtY = normY,
                made = act in setOf("2FGM", "3FGM", "DUNK"),
                isThreePointer = "3FG" in act,
                quarter = quarter
            )
        }.take(45)
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
            currentQuarter = if (status == GameStatus.UPCOMING) 1 else 4,
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
        val given = person.passportName?.trim().orEmpty()
        val surname = person.passportSurname?.trim().orEmpty()
        if (given.isNotEmpty() && surname.isNotEmpty()) {
            return "$given $surname".lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
        val raw = person.name.trim()
        if ("," in raw) {
            val parts = raw.split(",", limit = 2).map { it.trim() }
            return "${parts[1]} ${parts[0]}".lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        }
        return raw.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
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

    private suspend fun persistToRoomCache(
        teams: List<EuroLeagueTeam>,
        games: List<EuroLeagueGame>
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
                wins = team.wins,
                losses = team.losses,
                pointsFor = team.pointsFor,
                pointsAgainst = team.pointsAgainst,
                streakCsv = team.streak.joinToString(","),
                offensiveRating = team.offensiveRating,
                defensiveRating = team.defensiveRating,
                pace = team.pace,
                threePointPct = team.threePointPct,
                twoPointPct = team.twoPointPct,
                reboundRate = team.reboundRate
            )
        }
        val gameEntities = games.map { g ->
            CachedGameEntity(
                id = g.id,
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
                headToHeadAwayWins = g.headToHeadAwayWins
            )
        }
        dao.upsertTeams(teamEntities)
        dao.upsertGames(gameEntities)
    }
}
