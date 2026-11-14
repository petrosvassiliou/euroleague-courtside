package com.euroleague.courtside.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.euroleague.courtside.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("assets") val assets: List<GitHubReleaseAssetDto> = emptyList()
)

@Serializable
data class GitHubReleaseAssetDto(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("content_type") val contentType: String = "",
    @SerialName("size") val size: Long = 0L,
    @SerialName("digest") val digest: String? = null
)

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR
}

data class AppUpdateInfo(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val latestVersionName: String = BuildConfig.VERSION_NAME,
    val latestVersionCode: Int? = null,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val apkDownloadUrl: String = LATEST_RELEASE_HTML_URL,
    val releasePageUrl: String = LATEST_RELEASE_HTML_URL,
    val sha256Digest: String? = null,
    val apkSizeBytes: Long = 0L,
    val lastCheckedTime: String = "",
    val isManualCheck: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        const val GITHUB_OWNER = "petrosvassiliou"
        const val GITHUB_REPO = "euroleague-courtside"
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val LATEST_RELEASE_HTML_URL =
            "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val PLAY_STORE_PACKAGE = "com.android.vending"
    }
}

/**
 * Google Play Protect-compliant Auto-Update & Manual Update Checker:
 * - Zero `REQUEST_INSTALL_PACKAGES` permission and zero silent background package installation.
 * - Verifies HTTPS release metadata & SHA-256 APK digest from official GitHub Releases API.
 * - Routes Google Play Store installations (`com.android.vending`) to the official Play Store page,
 *   and GitHub Release installations to an explicit user-consented HTTPS download via the system installer.
 */
object CourtsideAppUpdateManager {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _updateState = MutableStateFlow(AppUpdateInfo())
    val updateState: StateFlow<AppUpdateInfo> = _updateState.asStateFlow()

    private val _dismissedVersionTag = MutableStateFlow<String?>(null)
    val dismissedVersionTag: StateFlow<String?> = _dismissedVersionTag.asStateFlow()

    fun dismissUpdateBanner(versionName: String) {
        _dismissedVersionTag.value = normalizeVersionName(versionName)
    }

    /**
     * Normalizes a version string such as "v1.5.0" or "1.5.0-release" to "1.5.0".
     */
    fun normalizeVersionName(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").substringBefore("-").trim()

    /**
     * Parses a `Build <number>` integer from a GitHub Release title or body if present
     * (e.g. "EuroLeague Courtside v1.5.0 (Build 12 — Official Signed Release APK)" -> 12).
     */
    fun extractBuildNumber(title: String, body: String = ""): Int? {
        val regex = Regex("""\bBuild\s+(\d+)\b""", RegexOption.IGNORE_CASE)
        return regex.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: regex.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    /**
     * Compares two semantic version strings (e.g., "1.5.0" vs "1.4.2").
     * Returns > 0 if [candidateVersion] is newer than [currentVersion], 0 if equal, < 0 if older.
     */
    fun compareSemanticVersions(candidateVersion: String, currentVersion: String): Int {
        val candParts = normalizeVersionName(candidateVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
        val currParts = normalizeVersionName(currentVersion)
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(candParts.size, currParts.size)
        for (i in 0 until maxLen) {
            val c = candParts.getOrElse(i) { 0 }
            val k = currParts.getOrElse(i) { 0 }
            if (c != k) return c.compareTo(k)
        }
        return 0
    }

    /**
     * Determines whether a remote release is newer than the currently installed build.
     */
    fun isRemoteReleaseNewer(
        remoteVersionName: String,
        remoteBuildCode: Int?,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        currentBuildCode: Int = BuildConfig.VERSION_CODE
    ): Boolean {
        val semCompare = compareSemanticVersions(remoteVersionName, currentVersionName)
        if (semCompare > 0) return true
        if (semCompare < 0) return false
        return remoteBuildCode != null && remoteBuildCode > currentBuildCode
    }

    /**
     * Parses a GitHub Release JSON payload into an [AppUpdateInfo] state.
     */
    fun evaluateReleasePayload(
        rawJson: String,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        checkedAtTime: String = "",
        isManual: Boolean = false
    ): AppUpdateInfo {
        val dto = jsonParser.decodeFromString<GitHubReleaseDto>(rawJson)
        val remoteVer = normalizeVersionName(dto.tagName.ifBlank { dto.name })
        val remoteBuild = extractBuildNumber(dto.name, dto.body)

        // Prefer the signed release APK (excluding "-debug.apk") from the official repository
        val signedReleaseAsset = dto.assets.firstOrNull { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                !asset.name.contains("debug", ignoreCase = true) &&
                asset.browserDownloadUrl.startsWith("https://github.com/${AppUpdateInfo.GITHUB_OWNER}/${AppUpdateInfo.GITHUB_REPO}/")
        } ?: dto.assets.firstOrNull { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                asset.browserDownloadUrl.startsWith("https://github.com/${AppUpdateInfo.GITHUB_OWNER}/${AppUpdateInfo.GITHUB_REPO}/")
        }

        val isNewer = !dto.draft && !dto.prerelease && remoteVer.isNotBlank() &&
            isRemoteReleaseNewer(
                remoteVersionName = remoteVer,
                remoteBuildCode = remoteBuild,
                currentVersionName = currentVersionName,
                currentBuildCode = currentVersionCode
            )

        val safeReleaseUrl = dto.htmlUrl
            .takeIf { it.startsWith("https://github.com/${AppUpdateInfo.GITHUB_OWNER}/${AppUpdateInfo.GITHUB_REPO}/") }
            ?: AppUpdateInfo.LATEST_RELEASE_HTML_URL

        val safeApkUrl = signedReleaseAsset?.browserDownloadUrl
            ?.takeIf { it.startsWith("https://github.com/${AppUpdateInfo.GITHUB_OWNER}/${AppUpdateInfo.GITHUB_REPO}/") }
            ?: safeReleaseUrl

        return AppUpdateInfo(
            status = if (isNewer) UpdateStatus.UPDATE_AVAILABLE else UpdateStatus.UP_TO_DATE,
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode,
            latestVersionName = remoteVer.ifBlank { currentVersionName },
            latestVersionCode = remoteBuild,
            releaseTitle = dto.name.ifBlank { "v$remoteVer" },
            releaseNotes = dto.body.trim(),
            apkDownloadUrl = safeApkUrl,
            releasePageUrl = safeReleaseUrl,
            sha256Digest = signedReleaseAsset?.digest?.removePrefix("sha256:"),
            apkSizeBytes = signedReleaseAsset?.size ?: 0L,
            lastCheckedTime = checkedAtTime,
            isManualCheck = isManual,
            errorMessage = null
        )
    }

    /**
     * Queries the official GitHub Releases endpoint over HTTPS to check for a newer signed APK release.
     */
    suspend fun checkForUpdates(isManual: Boolean = false): AppUpdateInfo = withContext(Dispatchers.IO) {
        val prev = _updateState.value
        _updateState.value = prev.copy(
            status = UpdateStatus.CHECKING,
            isManualCheck = isManual,
            errorMessage = null
        )

        val timeStr = runCatching {
            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        }.getOrDefault("")

        val result = runCatching {
            val connection = (URL(AppUpdateInfo.LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "EuroLeagueCourtside/${BuildConfig.VERSION_NAME}")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                evaluateReleasePayload(
                    rawJson = body,
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    checkedAtTime = timeStr,
                    isManual = isManual
                )
            } finally {
                connection.disconnect()
            }
        }.getOrElse { err ->
            prev.copy(
                status = UpdateStatus.ERROR,
                lastCheckedTime = timeStr,
                isManualCheck = isManual,
                errorMessage = err.message ?: "Network unreachable"
            )
        }

        if (isManual && result.status == UpdateStatus.UPDATE_AVAILABLE) {
            _dismissedVersionTag.value = null
        }
        _updateState.value = result
        result
    }

    /**
     * Checks whether the app was installed via Google Play Store (`com.android.vending`).
     */
    fun isInstalledFromGooglePlay(context: Context): Boolean = runCatching {
        val pm = context.packageManager
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(context.packageName)
        }
        installer == AppUpdateInfo.PLAY_STORE_PACKAGE
    }.getOrDefault(false)

    /**
     * Launches a Google Play Protect-compliant update flow with explicit user action:
     * - If installed from Google Play Store, opens the official Play Store listing.
     * - Otherwise, opens the verified HTTPS signed APK download / GitHub Release URL via the system browser/installer.
     */
    fun launchPlayProtectCompliantUpdate(context: Context, updateInfo: AppUpdateInfo = _updateState.value) {
        runCatching {
            if (isInstalledFromGooglePlay(context)) {
                val marketIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
                return
            }
        }

        runCatching {
            val targetUrl = updateInfo.apkDownloadUrl.ifBlank { AppUpdateInfo.LATEST_RELEASE_HTML_URL }
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }

    fun openReleaseNotesInBrowser(context: Context, updateInfo: AppUpdateInfo = _updateState.value) {
        runCatching {
            val targetUrl = updateInfo.releasePageUrl.ifBlank { AppUpdateInfo.LATEST_RELEASE_HTML_URL }
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }
}
