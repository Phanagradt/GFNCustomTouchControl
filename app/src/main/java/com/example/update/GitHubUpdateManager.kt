package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.coroutines.coroutineContext

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val publishedAt: String
)

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class UpToDate(
        val currentVersion: String,
        val latestTag: String,
        val checkedAtMs: Long
    ) : UpdateUiState()

    data class UpdateAvailable(
        val currentVersion: String,
        val release: GitHubReleaseInfo
    ) : UpdateUiState()

    data class Downloading(
        val release: GitHubReleaseInfo,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateUiState() {
        val progress: Float
            get() = if (totalBytes > 0L) {
                (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
    }

    data class ReadyToInstall(
        val release: GitHubReleaseInfo,
        val apkFile: File
    ) : UpdateUiState()

    data class Error(val message: String) : UpdateUiState()
}

/**
 * Checks GitHub Releases API for newer NexusPad APKs, downloads the signed release APK
 * over HTTPS with strict GitHub host validation, and launches Android's native Package
 * Installer for seamless in-place upgrades (preserving all layouts, filters, and gallery data).
 */
object GitHubUpdateManager {

    private val REPO_SLUG_REGEX = Regex("^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$")
    private val ALLOWED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "api.github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    )

    fun currentVersionName(): String = BuildConfig.VERSION_NAME

    fun currentVersionCode(): Int = BuildConfig.VERSION_CODE

    fun defaultBuiltInRepo(): String = normalizeRepoSlug(BuildConfig.GITHUB_REPO) ?: ""

    /**
     * Accepts "owner/repo", "https://github.com/owner/repo", or "github.com/owner/repo.git"
     * and returns a clean "owner/repo" slug, or null if invalid.
     */
    fun normalizeRepoSlug(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .removeSuffix("/")
            .removeSuffix(".git")
            .trim()
        val parts = trimmed.split("/").filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val candidate = "${parts[0]}/${parts[1]}"
        return if (REPO_SLUG_REGEX.matches(candidate)) candidate else null
    }

    fun isAllowedGitHubDownloadUrl(rawUrl: String): Boolean {
        return runCatching {
            val uri = URI(rawUrl)
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            if (scheme != "https") return false
            host in ALLOWED_DOWNLOAD_HOSTS || host.endsWith(".githubusercontent.com")
        }.getOrDefault(false)
    }

    /**
     * Compares semantic/dotted versions (e.g., "1.0" vs "v1.0.5", "1.0.9" vs "1.0.10").
     * Returns > 0 if [latest] is newer than [current], 0 if equal, < 0 if older.
     */
    fun compareVersions(current: String, latest: String): Int {
        val curParts = parseVersionParts(current)
        val latParts = parseVersionParts(latest)
        val maxLen = maxOf(curParts.size, latParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l != c) return l.compareTo(c)
        }
        return 0
    }

    fun isNewerVersion(current: String, latest: String): Boolean =
        compareVersions(current, latest) > 0

    private fun parseVersionParts(raw: String): List<Int> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        return cleaned.split(".")
            .map { seg -> seg.filter { it.isDigit() }.toIntOrNull() ?: 0 }
            .ifEmpty { listOf(0) }
    }

    @JsonClass(generateAdapter = true)
    internal data class GitHubAssetDto(
        val name: String = "",
        @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
        val size: Long = 0L
    )

    @JsonClass(generateAdapter = true)
    internal data class GitHubReleaseDto(
        @Json(name = "tag_name") val tagName: String = "",
        val name: String = "",
        val body: String = "",
        @Json(name = "html_url") val htmlUrl: String = "",
        @Json(name = "published_at") val publishedAt: String = "",
        val assets: List<GitHubAssetDto> = emptyList()
    )

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val releaseAdapter = moshi.adapter(GitHubReleaseDto::class.java)

    fun parseReleaseJson(jsonText: String): GitHubReleaseInfo {
        val dto = releaseAdapter.fromJson(jsonText)
            ?: throw IllegalStateException("Empty GitHub release JSON")
        val tagName = dto.tagName.trim()
        val title = dto.name.trim().ifBlank { tagName }
        val body = dto.body.trim()
        val htmlUrl = dto.htmlUrl.trim()
        val publishedAt = dto.publishedAt.trim()

        var bestApkUrl: String? = null
        var bestApkName: String? = null
        var bestApkSize = 0L

        for (asset in dto.assets) {
            val name = asset.name.trim()
            val downloadUrl = asset.browserDownloadUrl.trim()
            val size = asset.size
            if (name.endsWith(".apk", ignoreCase = true) && isAllowedGitHubDownloadUrl(downloadUrl)) {
                val preferThis = bestApkUrl == null ||
                    name.startsWith("NexusPad", ignoreCase = true)
                if (preferThis) {
                    bestApkUrl = downloadUrl
                    bestApkName = name
                    bestApkSize = size
                }
            }
        }

        return GitHubReleaseInfo(
            tagName = tagName,
            versionName = tagName.removePrefix("v").removePrefix("V"),
            title = title,
            releaseNotes = body,
            htmlUrl = htmlUrl,
            apkDownloadUrl = bestApkUrl,
            apkFileName = bestApkName,
            apkSizeBytes = bestApkSize,
            publishedAt = publishedAt
        )
    }

    suspend fun fetchLatestRelease(repoInput: String): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(repoInput)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Enter a valid GitHub repository (e.g. username/NexusPad)")
            )

        val apiUrl = "https://api.github.com/repos/$slug/releases/latest"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "NexusPad-Android/${currentVersionName()}")
            }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext Result.failure(
                    IllegalStateException("No published GitHub Releases found for $slug yet")
                )
            }
            if (code == HttpURLConnection.HTTP_FORBIDDEN) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub API rate limit reached — please try again in a few minutes")
                )
            }
            if (code !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("GitHub returned HTTP $code")
                )
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val release = parseReleaseJson(body)
            if (release.tagName.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Invalid release metadata from GitHub"))
            }
            Result.success(release)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadReleaseApk(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val initialUrl = release.apkDownloadUrl
            ?: return@withContext Result.failure(
                IllegalStateException("This GitHub release does not have an attached APK file")
            )
        if (!isAllowedGitHubDownloadUrl(initialUrl)) {
            return@withContext Result.failure(
                SecurityException("Blocked non-GitHub download URL")
            )
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean previous cached APKs so cache stays lean
        updatesDir.listFiles()?.forEach { old ->
            if (old.isFile && old.extension.equals("apk", ignoreCase = true)) {
                old.delete()
            }
        }
        val safeName = (release.apkFileName ?: "NexusPad-${release.tagName}.apk")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
        val targetFile = File(updatesDir, safeName)
        val tempFile = File(updatesDir, "$safeName.part")

        var currentUrl = initialUrl
        var connection: HttpURLConnection? = null
        try {
            // Follow up to 5 HTTPS GitHub redirects safely (GitHub redirects release downloads to objects.githubusercontent.com)
            var redirects = 0
            while (redirects < 5) {
                coroutineContext.ensureActive()
                if (!isAllowedGitHubDownloadUrl(currentUrl)) {
                    return@withContext Result.failure(SecurityException("Blocked redirect to untrusted host"))
                }
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "NexusPad-Android/${currentVersionName()}")
                    setRequestProperty("Accept", "application/octet-stream")
                }
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = null
                    if (loc.isNullOrBlank()) {
                        return@withContext Result.failure(IllegalStateException("Broken download redirect"))
                    }
                    currentUrl = loc
                    redirects++
                } else {
                    break
                }
            }

            val conn = connection ?: return@withContext Result.failure(IllegalStateException("Too many redirects"))
            if (conn.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("APK download failed (HTTP ${conn.responseCode})"))
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: release.apkSizeBytes
            var downloaded = 0L
            var lastNotifyMs = 0L

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastNotifyMs >= 80L || (totalBytes > 0L && downloaded >= totalBytes)) {
                            lastNotifyMs = now
                            onProgress(downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.length() < 100_000L) {
                tempFile.delete()
                return@withContext Result.failure(IllegalStateException("Downloaded APK file appears incomplete"))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            onProgress(targetFile.length(), targetFile.length())
            Result.success(targetFile)
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    /**
     * Launches Android's system Package Installer against the downloaded APK using FileProvider.
     * Because all NexusPad GitHub releases share the same keystore signature and monotonic
     * versionCode, Android updates the app in-place without requiring uninstall or losing data.
     */
    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> {
        if (!apkFile.exists()) {
            return Result.failure(IllegalStateException("Downloaded APK file was cleared — please re-download"))
        }
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return Result.failure(
                IllegalStateException("Allow \"Install unknown apps\" for NexusPad, then tap Install Update again")
            )
        }
        return runCatching {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }

    fun openReleaseInBrowser(context: Context, url: String) {
        if (!isAllowedGitHubDownloadUrl(url)) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
