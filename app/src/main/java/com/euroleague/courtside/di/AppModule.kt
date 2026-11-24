package com.euroleague.courtside.di

import android.content.Context
import androidx.room.Room
import com.euroleague.courtside.data.local.CourtsideDao
import com.euroleague.courtside.data.local.CourtsideDatabase
import com.euroleague.courtside.data.remote.EuroLeagueApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideCourtsideDatabase(
        @ApplicationContext context: Context
    ): CourtsideDatabase = Room.databaseBuilder(
        context,
        CourtsideDatabase::class.java,
        "euroleague_courtside.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideCourtsideDao(database: CourtsideDatabase): CourtsideDao =
        database.courtsideDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val host = original.url.host
            val reqBuilder = original.newBuilder()
                .header("User-Agent", "EuroLeagueCourtside/1.8 (Android; CourtsideLiveTicker)")
            if (host.contains("live.euroleague.net", ignoreCase = true)) {
                reqBuilder
                    .header("Cache-Control", "no-cache, no-store, max-age=0")
                    .header("Pragma", "no-cache")
            }
            chain.proceed(reqBuilder.build())
        }
        .build()

    @Provides
    @Singleton
    fun provideEuroLeagueApiService(
        json: Json,
        okHttpClient: OkHttpClient
    ): EuroLeagueApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api-live.euroleague.net/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(EuroLeagueApiService::class.java)
    }

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
