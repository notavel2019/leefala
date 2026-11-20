package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.PanelHeightMode
import com.example.model.ScriptDocument
import com.example.model.TeleprompterPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Lightweight local storage for scripts (with manual sentence splits) and user preferences.
 * Uses an atomic JSON file in app-private filesDir and SharedPreferences — no heavy DB required.
 */
class LocalScriptRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scriptsFile = File(appContext.filesDir, "escuta_e_grava_scripts.json")
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadScripts(): List<ScriptDocument> {
        if (!scriptsFile.exists()) return emptyList()
        return try {
            val content = scriptsFile.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            val array = JSONArray(content)
            val result = mutableListOf<ScriptDocument>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val sentencesArray = obj.optJSONArray("sentences")
                val sentences = mutableListOf<String>()
                if (sentencesArray != null) {
                    for (j in 0 until sentencesArray.length()) {
                        val s = sentencesArray.optString(j, "").trim()
                        if (s.isNotEmpty()) sentences.add(s)
                    }
                }
                result.add(
                    ScriptDocument(
                        id = obj.optString("id"),
                        title = obj.optString("title", ""),
                        rawText = obj.optString("rawText", ""),
                        sentences = sentences,
                        hasManualSplitEdits = obj.optBoolean("hasManualSplitEdits", false),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                    )
                )
            }
            result.sortedByDescending { it.updatedAtMillis }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao carregar roteiros locais (${e.javaClass.simpleName})")
            emptyList()
        }
    }

    fun saveScripts(scripts: List<ScriptDocument>): Boolean {
        return try {
            val array = JSONArray()
            scripts.forEach { doc ->
                val obj = JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("rawText", doc.rawText)
                    put("hasManualSplitEdits", doc.hasManualSplitEdits)
                    put("updatedAtMillis", doc.updatedAtMillis)
                    val sArr = JSONArray()
                    doc.sentences.forEach { sArr.put(it) }
                    put("sentences", sArr)
                }
                array.put(obj)
            }
            val tmpFile = File(appContext.filesDir, "escuta_e_grava_scripts.tmp")
            tmpFile.writeText(array.toString(), Charsets.UTF_8)
            if (!tmpFile.renameTo(scriptsFile)) {
                scriptsFile.writeText(array.toString(), Charsets.UTF_8)
                tmpFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao salvar roteiros locais (${e.javaClass.simpleName})")
            false
        }
    }

    fun loadLastScriptId(): String? = prefs.getString(KEY_LAST_SCRIPT_ID, null)

    fun saveLastScriptId(id: String?) {
        prefs.edit().putString(KEY_LAST_SCRIPT_ID, id).apply()
    }

    fun loadPreferences(): TeleprompterPreferences {
        val heightName = prefs.getString(KEY_PANEL_HEIGHT, PanelHeightMode.MEDIUM.name)
        val heightMode = PanelHeightMode.entries.firstOrNull { it.name == heightName }
            ?: PanelHeightMode.MEDIUM
        return TeleprompterPreferences(
            selectedVoiceName = prefs.getString(KEY_VOICE_NAME, null),
            speechRate = prefs.getFloat(KEY_SPEECH_RATE, 0.9f).coerceIn(0.6f, 1.2f),
            pauseSeconds = prefs.getFloat(KEY_PAUSE_SECONDS, 1.0f).coerceIn(0f, 5.0f),
            overlayFontSizeSp = prefs.getFloat(KEY_OVERLAY_FONT_SP, 19f).coerceIn(14f, 28f),
            panelHeightMode = heightMode,
            preferFrontCamera = prefs.getBoolean(KEY_PREFER_FRONT_CAMERA, true),
        )
    }

    fun savePreferences(preferences: TeleprompterPreferences) {
        prefs.edit()
            .putString(KEY_VOICE_NAME, preferences.selectedVoiceName)
            .putFloat(KEY_SPEECH_RATE, preferences.speechRate)
            .putFloat(KEY_PAUSE_SECONDS, preferences.pauseSeconds)
            .putFloat(KEY_OVERLAY_FONT_SP, preferences.overlayFontSizeSp)
            .putString(KEY_PANEL_HEIGHT, preferences.panelHeightMode.name)
            .putBoolean(KEY_PREFER_FRONT_CAMERA, preferences.preferFrontCamera)
            .apply()
    }

    fun loadGithubRepo(): String = prefs.getString(KEY_GITHUB_REPO, "") ?: ""

    fun saveGithubRepo(repoSlug: String) {
        prefs.edit().putString(KEY_GITHUB_REPO, repoSlug.trim()).apply()
    }

    companion object {
        private const val TAG = "LocalScriptRepo"
        private const val PREFS_NAME = "escuta_e_grava_prefs"
        private const val KEY_LAST_SCRIPT_ID = "last_script_id"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_PAUSE_SECONDS = "pause_seconds"
        private const val KEY_OVERLAY_FONT_SP = "overlay_font_sp"
        private const val KEY_PANEL_HEIGHT = "panel_height"
        private const val KEY_PREFER_FRONT_CAMERA = "prefer_front_camera"
        private const val KEY_GITHUB_REPO = "github_repo_slug"
    }
}
