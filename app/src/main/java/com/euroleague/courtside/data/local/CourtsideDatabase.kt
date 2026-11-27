package com.euroleague.courtside.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_teams")
data class CachedTeamEntity(
    @PrimaryKey val id: String,
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
    val totalPointsFor: Int = pointsFor,
    val totalPointsAgainst: Int = pointsAgainst,
    val streakCsv: String,
    val offensiveRating: Float,
    val defensiveRating: Float,
    val pace: Float,
    val threePointPct: Float,
    val twoPointPct: Float,
    val reboundRate: Float,
    val logoUrl: String = "",
    val localLogoResName: String = "club_logo_${id.lowercase()}"
)

@Entity(tableName = "cached_players")
data class CachedPlayerEntity(
    @PrimaryKey val id: String,
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
    val injuryStatusName: String = "FIT",
    val injuryDetail: String = "",
    val headshotUrl: String = ""
)

@Entity(tableName = "cached_games")
data class CachedGameEntity(
    @PrimaryKey val id: String,
    val gameCode: Int = 0,
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
    val q1Home: Int,
    val q1Away: Int,
    val q2Home: Int,
    val q2Away: Int,
    val q3Home: Int,
    val q3Away: Int,
    val q4Home: Int,
    val q4Away: Int,
    val otHome: Int?,
    val otAway: Int?,
    val headToHeadHomeWins: Int,
    val headToHeadAwayWins: Int,
    val boxscoreHomeJson: String = "",
    val boxscoreAwayJson: String = "",
    val shotsJson: String = "",
    val playByPlayJson: String = ""
)

@Dao
interface CourtsideDao {
    @Query("SELECT * FROM cached_teams ORDER BY wins DESC, (pointsFor - pointsAgainst) DESC")
    fun observeStandings(): Flow<List<CachedTeamEntity>>

    @Query("SELECT * FROM cached_games WHERE round = :round ORDER BY tipOffEpochMillis ASC")
    fun observeGamesByRound(round: Int): Flow<List<CachedGameEntity>>

    @Query("SELECT * FROM cached_games ORDER BY tipOffEpochMillis ASC")
    fun observeAllGames(): Flow<List<CachedGameEntity>>

    @Query("SELECT * FROM cached_teams")
    suspend fun getAllTeamsOnce(): List<CachedTeamEntity>

    @Query("SELECT * FROM cached_players")
    suspend fun getAllPlayersOnce(): List<CachedPlayerEntity>

    @Query("SELECT * FROM cached_games ORDER BY round ASC, tipOffEpochMillis ASC")
    suspend fun getAllGamesOnce(): List<CachedGameEntity>

    @Query("SELECT * FROM cached_games WHERE id = :gameId LIMIT 1")
    suspend fun getGameByIdOnce(gameId: String): CachedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTeams(teams: List<CachedTeamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<CachedPlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGames(games: List<CachedGameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingleGame(game: CachedGameEntity)
}

@Database(
    entities = [CachedTeamEntity::class, CachedPlayerEntity::class, CachedGameEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CourtsideDatabase : RoomDatabase() {
    abstract fun courtsideDao(): CourtsideDao
}
