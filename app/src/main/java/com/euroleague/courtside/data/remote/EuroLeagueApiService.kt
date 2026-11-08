package com.euroleague.courtside.data.remote

import com.euroleague.courtside.data.local.CachedGameEntity
import com.euroleague.courtside.data.local.CachedTeamEntity
import com.euroleague.courtside.data.local.CourtsideDao
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// --- RETROFIT DTOs ---

@Serializable
data class EuroLeagueGameDto(
    @SerialName("gameCode") val gameId: String,
    @SerialName("round") val round: Int,
    @SerialName("homeCode") val homeTeamId: String,
    @SerialName("awayCode") val awayTeamId: String,
    @SerialName("utcDateEpoch") val tipOffEpochMillis: Long,
    @SerialName("status") val status: String,
    @SerialName("quarter") val currentQuarter: Int,
    @SerialName("clock") val remainingClock: String,
    @SerialName("homeScore") val homeScore: Int,
    @SerialName("awayScore") val awayScore: Int,
    @SerialName("q1Home") val q1Home: Int,
    @SerialName("q1Away") val q1Away: Int,
    @SerialName("q2Home") val q2Home: Int,
    @SerialName("q2Away") val q2Away: Int,
    @SerialName("q3Home") val q3Home: Int,
    @SerialName("q3Away") val q3Away: Int,
    @SerialName("q4Home") val q4Home: Int,
    @SerialName("q4Away") val q4Away: Int,
    @SerialName("otHome") val otHome: Int? = null,
    @SerialName("otAway") val otAway: Int? = null
)

@Serializable
data class EuroLeagueStandingDto(
    @SerialName("clubCode") val teamId: String,
    @SerialName("clubName") val name: String,
    @SerialName("city") val city: String,
    @SerialName("country") val country: String,
    @SerialName("arena") val arena: String,
    @SerialName("wins") val wins: Int,
    @SerialName("losses") val losses: Int,
    @SerialName("pointsFor") val pointsFor: Int,
    @SerialName("pointsAgainst") val pointsAgainst: Int,
    @SerialName("streak") val streak: List<String>,
    @SerialName("offRtg") val offensiveRating: Float,
    @SerialName("defRtg") val defensiveRating: Float,
    @SerialName("pace") val pace: Float,
    @SerialName("threePct") val threePointPct: Float,
    @SerialName("twoPct") val twoPointPct: Float,
    @SerialName("rebRate") val reboundRate: Float
)

/**
 * Offizielles Retrofit-Interface für die EuroLeague Live-Feeds (v2 API).
 */
interface EuroLeagueApiService {

    @GET("v2/competitions/E/seasons/{seasonCode}/games")
    suspend fun fetchGamesByRound(
        @Path("seasonCode") seasonCode: String = "E2026",
        @Query("roundNumber") round: Int? = null
    ): List<EuroLeagueGameDto>

    @GET("v2/competitions/E/seasons/{seasonCode}/standings")
    suspend fun fetchSeasonStandings(
        @Path("seasonCode") seasonCode: String = "E2026"
    ): List<EuroLeagueStandingDto>
}

/**
 * Offline-First Synchronisations-Repository:
 * Lädt Live-Daten über `EuroLeagueApiService`, persistiert sie in `CourtsideDao` (Room)
 * und fällt im Offline-Modus automatisch auf den lokalen Room-Cache bzw. `EuroLeagueMockRepository` zurück.
 */
@Singleton
class EuroLeagueOfflineFirstRepository @Inject constructor(
    private val apiService: EuroLeagueApiService,
    private val dao: CourtsideDao,
    private val fallbackMockRepository: EuroLeagueMockRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun syncSeasonDataFromRemoteOrSeedCache(seasonCode: String = "E2026"): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val remoteStandings = apiService.fetchSeasonStandings(seasonCode)
                val remoteGames = apiService.fetchGamesByRound(seasonCode)

                val teamEntities = remoteStandings.map { dto ->
                    val fallbackTeam = fallbackMockRepository.teams.find { it.id == dto.teamId }
                    CachedTeamEntity(
                        id = dto.teamId,
                        code = dto.teamId,
                        name = dto.name,
                        city = dto.city,
                        country = dto.country,
                        arena = dto.arena,
                        primaryColorHex = fallbackTeam?.primaryColorHex ?: 0xFFFF6600,
                        secondaryColorHex = fallbackTeam?.secondaryColorHex ?: 0xFFFFFFFF,
                        wins = dto.wins,
                        losses = dto.losses,
                        pointsFor = dto.pointsFor,
                        pointsAgainst = dto.pointsAgainst,
                        streakCsv = dto.streak.joinToString(","),
                        offensiveRating = dto.offensiveRating,
                        defensiveRating = dto.defensiveRating,
                        pace = dto.pace,
                        threePointPct = dto.threePointPct,
                        twoPointPct = dto.twoPointPct,
                        reboundRate = dto.reboundRate
                    )
                }

                val gameEntities = remoteGames.map { dto ->
                    CachedGameEntity(
                        id = dto.gameId,
                        round = dto.round,
                        homeTeamId = dto.homeTeamId,
                        awayTeamId = dto.awayTeamId,
                        tipOffEpochMillis = dto.tipOffEpochMillis,
                        status = dto.status,
                        currentQuarter = dto.currentQuarter,
                        remainingClock = dto.remainingClock,
                        homeScore = dto.homeScore,
                        awayScore = dto.awayScore,
                        q1Home = dto.q1Home,
                        q1Away = dto.q1Away,
                        q2Home = dto.q2Home,
                        q2Away = dto.q2Away,
                        q3Home = dto.q3Home,
                        q3Away = dto.q3Away,
                        q4Home = dto.q4Home,
                        q4Away = dto.q4Away,
                        otHome = dto.otHome,
                        otAway = dto.otAway,
                        headToHeadHomeWins = 5,
                        headToHeadAwayWins = 5
                    )
                }

                dao.upsertTeams(teamEntities)
                dao.upsertGames(gameEntities)
            }.recoverCatching {
                // Offline-Seed: Befülle Room-Cache aus EuroLeagueMockRepository, falls keine Netzwerkverbindung besteht
                val seededTeams = fallbackMockRepository.teams.map { team ->
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
                val seededGames = fallbackMockRepository.games.value.map { g ->
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
                dao.upsertTeams(seededTeams)
                dao.upsertGames(seededGames)
            }
        }
}
