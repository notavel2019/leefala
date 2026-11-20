package com.example.update

import com.example.BuildConfig
import com.example.model.UpdateCheckState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks the user's GitHub repository "latest" release for a newer APK build.
 * Opens the download/release in the system browser (no REQUEST_INSTALL_PACKAGES or DCL).
 */
object GitHubUpdateChecker {

    suspend fun checkLatestRelease(repoSlugRaw: String): UpdateCheckState = withContext(Dispatchers.IO) {
        val repo = sanitizeRepoSlug(repoSlugRaw)
        if (repo == null) {
            return@withContext UpdateCheckState.NeedsBrowserAccess(
                message = "Informe seu repositório no formato usuario/repositorio (ex.: notavel2019/escuta-e-grava).",
                releasesUrl = "https://github.com",
                actionsUrl = "https://github.com",
            )
        }

        val releasesPage = "https://github.com/$repo/releases"
        val actionsPage = "https://github.com/$repo/actions"
        val apiUrl = "https://api.github.com/repos/$repo/releases/tags/latest"

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "EscutaEGrava-Android/${BuildConfig.VERSION_NAME}")
            }

            val code = connection.responseCode
            if (code == 404 || code == 403 || code == 401) {
                return@withContext UpdateCheckState.NeedsBrowserAccess(
                    message = "Se o repositório for privado (ou ainda não tiver a release 'latest'), abra pelo navegador onde você já está logado no GitHub para baixar o EscutaEGrava.apk.",
                    releasesUrl = releasesPage,
                    actionsUrl = actionsPage,
                )
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckState.NeedsBrowserAccess(
                    message = "Não foi possível consultar o GitHub agora (HTTP $code). Você pode abrir a página de Releases direto no navegador.",
                    releasesUrl = releasesPage,
                    actionsUrl = actionsPage,
                )
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val title = json.optString("name", "")
            val notes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", releasesPage)

            val assets = json.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }

            val remoteVersion = extractVersion(title).ifEmpty { extractVersion(notes) }.ifEmpty { "nova" }
            val currentVersion = BuildConfig.VERSION_NAME

            if (apkDownloadUrl == null) {
                return@withContext UpdateCheckState.NeedsBrowserAccess(
                    message = "A release ainda não possui um arquivo .apk anexado. Aguarde o término do workflow no GitHub Actions.",
                    releasesUrl = htmlUrl,
                    actionsUrl = actionsPage,
                )
            }

            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateCheckState.UpdateAvailable(
                    latestVersion = remoteVersion,
                    currentVersion = currentVersion,
                    downloadUrl = apkDownloadUrl,
                    releasePageUrl = htmlUrl,
                    notes = notes.ifBlank { "Nova compilação pronta no GitHub." },
                )
            } else {
                UpdateCheckState.UpToDate(
                    currentVersion = currentVersion,
                    releasePageUrl = htmlUrl,
                )
            }
        } catch (_: Exception) {
            UpdateCheckState.NeedsBrowserAccess(
                message = "Sem conexão ou falha ao consultar a API do GitHub. Você pode abrir o GitHub direto pelo navegador.",
                releasesUrl = releasesPage,
                actionsUrl = actionsPage,
            )
        } finally {
            connection?.disconnect()
        }
    }

    fun sanitizeRepoSlug(raw: String): String? {
        val cleaned = raw.trim()
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removePrefix("github.com/")
            .trim('/')
            .removeSuffix(".git")
        val parts = cleaned.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null
        return "${parts[0]}/${parts[1]}"
    }

    private fun extractVersion(text: String): String {
        val match = Regex("""v?(\d+\.\d+(?:\.\d+)?)""").find(text)
        return match?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split('.').mapNotNull { it.toIntOrNull() }
        val cParts = current.split('.').mapNotNull { it.toIntOrNull() }
        if (rParts.isEmpty() || cParts.isEmpty()) return remote != current
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }
}
