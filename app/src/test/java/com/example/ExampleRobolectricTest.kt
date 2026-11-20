package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalScriptRepository
import com.example.model.ScriptDocument
import com.example.model.TeleprompterPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Escuta e Grava", appName)
    }

    @Test
    fun `scripts and manual sentence splits persist locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalScriptRepository(context)
        val doc = ScriptDocument(
            id = "test-1",
            title = "Reflexão Jo 3:16",
            rawText = "Porque Deus amou o mundo. Em Jo 3:16 lemos isso.",
            sentences = listOf("Porque Deus amou o mundo.", "Em Jo 3:16", "lemos isso."),
            hasManualSplitEdits = true,
        )

        assertTrue(repo.saveScripts(listOf(doc)))
        val reloaded = repo.loadScripts()
        assertEquals(1, reloaded.size)
        assertEquals("Reflexão Jo 3:16", reloaded[0].title)
        assertEquals(3, reloaded[0].sentences.size)
        assertTrue(reloaded[0].hasManualSplitEdits)

        val prefs = TeleprompterPreferences(
            selectedVoiceName = "pt-br-x-afs-local",
            speechRate = 0.85f,
            pauseSeconds = 1.5f,
        )
        repo.savePreferences(prefs)
        val loadedPrefs = repo.loadPreferences()
        assertEquals("pt-br-x-afs-local", loadedPrefs.selectedVoiceName)
        assertEquals(0.85f, loadedPrefs.speechRate, 0.001f)
        assertEquals(1.5f, loadedPrefs.pauseSeconds, 0.001f)
    }
}
