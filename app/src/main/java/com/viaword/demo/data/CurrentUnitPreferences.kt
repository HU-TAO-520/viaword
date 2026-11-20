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

    var eagerLearningEnabled: Boolean
        get() = preferences.getBoolean(EAGER_LEARNING_KEY, false)
        set(value) { preferences.edit().putBoolean(EAGER_LEARNING_KEY, value).apply() }

    private companion object {
        const val KEY = "current_unit_id"
        const val EAGER_LEARNING_KEY = "eager_learning_enabled"
    }
}
