package com.viaword.demo

import com.viaword.demo.data.sampleUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleWordsTest {
    @Test
    fun sampleVocabularyKeepsUnitAndEntryOrder() {
        assertEquals(listOf("sample-unit-1", "sample-unit-2"), sampleUnits.map { it.id })
        assertEquals(listOf("Unit 1", "Unit 2"), sampleUnits.map { it.title })
        assertEquals(listOf(6, 3), sampleUnits.map { it.entries.size })
        assertEquals(
            listOf("protect", "prepare", "share", "library", "improve", "together", "notice", "useful", "take care of"),
            sampleUnits.flatMap { unit -> unit.entries.map { it.english } },
        )
    }

    @Test
    fun entriesHaveUniqueIdsAndDisplayText() {
        val entries = sampleUnits.flatMap { it.entries }
        assertTrue(entries.all { it.id.isNotBlank() })
        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        assertTrue(entries.all { it.english.isNotBlank() && it.ipa.isNotBlank() && it.meaningZhHans.isNotBlank() })
    }
}
