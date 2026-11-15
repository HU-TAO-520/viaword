package com.viaword.demo.data

import android.content.Context

class CurrentUnitPreferences(context: Context, private val units: List<WordUnit>) {
    private val preferences = context.getSharedPreferences("viaword_preferences", Context.MODE_PRIVATE)

    fun readId(): String {
        val storedId = preferences.getString(KEY, null)
        return units.firstOrNull { it.id == storedId }?.id ?: units.first().id
    }

    fun save(unit: WordUnit) {
        preferences.edit().putString(KEY, unit.id).apply()
    }

    private companion object {
        const val KEY = "current_unit_id"
    }
}
