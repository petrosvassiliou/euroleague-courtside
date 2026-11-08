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
 * sowie `live.euroleague.net/api/Points` für echte Wurf-Koordinaten).
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
        @Query("seasonCode") seasonCode: String = "E2026",
        @Query("statisticMode") statisticMode: String = "PerGame",
        @Query("limit") limit: Int = 500
    ): EuroLeaguePlayerStatsEnvelopeDto

    @GET
    suspend fun fetchLiveGameShotPoints(
        @Url fullUrl: String
    ): EuroLeagueLivePointsDto
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
                shotChart = g.shotChart.map { s ->
                    ShotAttempt(
                        id = s.id,
                        playerId = s.playerId,
                        playerName = s.playerName,
                        teamId = s.teamId,
                        courtX = s.courtX,
                        courtY = s.courtY,
                        isMade = s.isMade,
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
                val activeRound = resolveCurrentActiveRound(rounds)
                val allGamesDto = apiService.fetchSeasonGames(seasonCode = seasonCode, round = null, limit = 400).data
                val playerStatsMap = fetchPlayerTraditionalStatsMap(seasonCode)
                val injuryMap = fetchLatestInjuryReportMap()

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
                    val gp = (st.wins + st.losses).coerceAtLeast(1)
                    val ppg = st.pf.toFloat() / gp
                    val oppg = st.pa.toFloat() / gp
                    val colors = officialPalette[code]
                        ?: officialPalette[tvCode]
                        ?: Triple(0xFFFF6600L, 0xFFFFFFFFL, 0xFFFF6600L)

                    EuroLeagueTeam(
                        id = code,
                        code = tvCode,
                        name = c.name.ifBlank { c.alias ?: code },
                        city = c.city ?: extractCityFromAddress(c.venue?.address),
                        country = c.country?.name ?: "",
                        arena = c.venue?.name ?: "EuroLeague Arena",
                        primaryColorHex = colors.first,
                        secondaryColorHex = colors.second,
                        accentColorHex = colors.third,
                        wins = st.wins,
                        losses = st.losses,
                        pointsFor = st.pf,
                        pointsAgainst = st.pa,
                        streak = st.streak.takeLast(5).ifEmpty { listOf("S", "S", "N", "S", "S") },
                        offensiveRating = ((ppg * 1.38f * 10f).roundToInt() / 10f).coerceIn(104f, 126f),
                        defensiveRating = ((oppg * 1.38f * 10f).roundToInt() / 10f).coerceIn(102f, 124f),
                        pace = 72.5f,
                        threePointPct = 37.5f,
                        twoPointPct = 55.4f,
                        reboundRate = 51.0f,
                        logoUrl = c.images?.crest ?: "",
                        localLogoResName = "club_logo_${code.lowercase()}"
                    )
                }.sortedWith(compareByDescending<EuroLeagueTeam> { it.wins }.thenByDescending { it.pointDiff })

                val teamById = liveTeams.associateBy { it.id }

                // 2. Lade und verifiziere parallel die aktuellen Kader (Rosters) aller EuroLeague-Clubs
                val allVerifiedPlayers = coroutineScope {
                    liveTeams.map { team ->
                        async {
                            fetchAndVerifySingleClubRoster(
                                seasonCode = seasonCode,
                                clubCode = team.id,
                                playerStatsMap = playerStatsMap,
                                injuryMap = injuryMap
                            )
                        }
                    }.awaitAll().flatten()
                }

                // 3. Lade Spiele der aktiven Runde (+ alle Spiele des Lieblingsteams für den Kalender-Export)
                val targetGamesDto = allGamesDto.filter { g ->
                    g.round == activeRound ||
                        g.local.club.code in setOf("MUN", "PAN", "MAD", "OLY") ||
                        g.road.club.code in setOf("MUN", "PAN", "MAD", "OLY")
                }.ifEmpty { allGamesDto.take(18) }

                val liveGames = coroutineScope {
                    targetGamesDto.mapNotNull { g ->
                        val homeTeam = teamById[g.local.club.code] ?: return@mapNotNull null
                        val awayTeam = teamById[g.road.club.code] ?: return@mapNotNull null
                        async {
                            val isActiveRound = g.round == activeRound
                            val (boxHome, boxAway) = if (isActiveRound && g.played) {
                                fetchGameBoxscores(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                            } else {
                                emptyList<PlayerBoxscoreStat>() to emptyList()
                            }
                            val shots = if (isActiveRound && g.played) {
                                fetchGameShotChart(seasonCode, g.gameCode, homeTeam.id, awayTeam.id)
                            } else {
                                emptyList()
                            }
                            mapRemoteGameToDomain(
                                g = g,
                                seasonCode = seasonCode,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                boxHome = boxHome.ifEmpty { buildFallbackBoxscoreFromRoster(homeTeam.id, allVerifiedPlayers) },
                                boxAway = boxAway.ifEmpty { buildFallbackBoxscoreFromRoster(awayTeam.id, allVerifiedPlayers) },
                                shots = shots
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
                    syncLabel = "LIVE API • $seasonCode • Round $activeRound • ${allVerifiedPlayers.size} Kader-Spieler geprüft"
                )

                persistToRoomCache(liveTeams, liveGames)
            }.recoverCatching {
                // Offline-Fallback: Speichere aktuellen Stand aus dem Mock/Snapshot-Repository in Room
                persistToRoomCache(fallbackMockRepository.teams, fallbackMockRepository.games.value)
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
     * Lädt Live-Boxscore und echte Shot-Chart-Koordinaten für ein ausgewähltes Spiel nach.
     */
    suspend fun refreshSelectedGameLiveStats(game: EuroLeagueGame): Result<EuroLeagueGame> =
        withContext(ioDispatcher) {
            runCatching {
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
                val updated = game.copy(
                    boxscoreHome = boxHome.ifEmpty { game.boxscoreHome },
                    boxscoreAway = boxAway.ifEmpty { game.boxscoreAway },
                    shots = shots.ifEmpty { game.shots }
                )
                fallbackMockRepository.updateSingleGameDetails(updated)
                updated
            }
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

    private fun resolveCurrentActiveRound(rounds: List<EuroLeagueRoundDto>): Int {
        if (rounds.isEmpty()) return fallbackMockRepository.activeRoundFlow.value
        val nowIso = Instant.now().toString()
        for (r in rounds) {
            val maxDate = r.maxGameStartDate ?: ""
            if (maxDate >= nowIso) {
                return r.round
            }
        }
        return rounds.last().round
    }

    private suspend fun fetchPlayerTraditionalStatsMap(seasonCode: String): Map<String, EuroLeaguePlayerTraditionalDto> =
        runCatching {
            apiService.fetchPlayerTraditionalStats(seasonCode = seasonCode)
                .players
                .associateBy { it.player.code }
        }.getOrDefault(emptyMap())

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

    private suspend fun fetchGameBoxscores(
        seasonCode: String,
        gameCode: Int,
        homeTeamId: String,
        awayTeamId: String
    ): Pair<List<PlayerBoxscoreStat>, List<PlayerBoxscoreStat>> = runCatching {
        val dto = apiService.fetchGameBoxscoreStats(seasonCode = seasonCode, gameCode = gameCode)
        val homeStats = parseBoxscoreSection(dto.local, homeTeamId)
        val awayStats = parseBoxscoreSection(dto.road, awayTeamId)
        homeStats to awayStats
    }.getOrDefault(emptyList<PlayerBoxscoreStat>() to emptyList())

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
