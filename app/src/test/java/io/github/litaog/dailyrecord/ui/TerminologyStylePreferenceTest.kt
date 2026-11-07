package io.github.litaog.dailyrecord.ui

import android.content.SharedPreferences
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TerminologyStylePreferenceTest {
    private lateinit var fakePreferences: FakeSharedPreferences
    private lateinit var preference: TerminologyStylePreference

    @Before
    fun setUp() {
        fakePreferences = FakeSharedPreferences()
        preference = TerminologyStylePreference(fakePreferences)
    }

    @Test
    fun defaultIsElegant() {
        assertEquals(TerminologyStyle.ELEGANT, preference.current)
        assertEquals(TerminologyStyle.ELEGANT, preference.style.value)
    }

    @Test
    fun persistsSelectionAcrossInstances() {
        preference.setStyle(TerminologyStyle.DIRECT)

        assertEquals(TerminologyStyle.DIRECT, preference.current)
        assertEquals(TerminologyStyle.DIRECT, preference.style.value)

        val newInstance = TerminologyStylePreference(fakePreferences)
        assertEquals(TerminologyStyle.DIRECT, newInstance.current)
        assertEquals(TerminologyStyle.DIRECT, newInstance.style.value)
    }

    @Test
    fun settingSameStyleDoesNotOverwriteUnnecessarily() {
        preference.setStyle(TerminologyStyle.ELEGANT)
        assertEquals(TerminologyStyle.ELEGANT, preference.current)
    }

    @Test
    fun flowReflectsSelection() {
        assertEquals(TerminologyStyle.ELEGANT, preference.style.value)
        preference.setStyle(TerminologyStyle.DIRECT)
        assertEquals(TerminologyStyle.DIRECT, preference.style.value)
        preference.setStyle(TerminologyStyle.ELEGANT)
        assertEquals(TerminologyStyle.ELEGANT, preference.style.value)
    }

    @Test
    fun invalidStoredValueFallsBackToElegant() {
        fakePreferences.putString(TerminologyStylePreference.KEY_TERMINOLOGY_STYLE, "INVALID_VALUE")

        val newInstance = TerminologyStylePreference(fakePreferences)
        assertEquals(TerminologyStyle.ELEGANT, newInstance.current)
    }

    @Test
    fun caseInsensitiveStoredValueIsAccepted() {
        fakePreferences.putString(TerminologyStylePreference.KEY_TERMINOLOGY_STYLE, "direct")

        val newInstance = TerminologyStylePreference(fakePreferences)
        assertEquals(TerminologyStyle.DIRECT, newInstance.current)
    }
}

internal class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    fun putString(key: String, value: String) {
        data[key] = value
    }

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        data[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private class FakeEditor(private val backingData: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val staged = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) staged[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) staged[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                backingData.clear()
            }
            removed.forEach { backingData.remove(it) }
            backingData.putAll(staged)
        }
    }
}
