package com.viaword.demo

import android.media.MediaPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viaword.demo.data.AppCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogAudioTest {
    @Test
    fun everyEntryHasItsOwnPlayableResource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ids = AppCatalog.units.flatMap { unit -> unit.entries.map { it.id } }
        assertEquals(ids.size, AppCatalog.audio.size)
        assertEquals(ids.toSet(), AppCatalog.audio.keys)
        assertEquals(ids.size, AppCatalog.audio.values.toSet().size)
        for (id in ids) {
            val resource = AppCatalog.audio.getValue(id)
            assertEquals(id.replace('-', '_'), context.resources.getResourceEntryName(resource))
            val player = requireNotNull(MediaPlayer.create(context, resource)) { "$id cannot open WAV" }
            try {
                assertTrue("$id has zero duration", player.duration > 0)
            } finally {
                player.release()
            }
        }
    }
}
