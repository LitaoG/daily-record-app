package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.litaog.dailyrecord.core.common.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LanguagePreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference: LanguagePreference
        get() = LanguagePreference(context)

    @Before
    fun clearStoredLanguage() {
        context.getSharedPreferences("daily_record_language", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun defaultsToChinese() {
        assertEquals(AppLanguage.ZH, preference.current)
    }

    @Test
    fun persistsSelectionAcrossInstances() {
        preference.setLanguage(AppLanguage.EN)

        assertEquals(AppLanguage.EN, LanguagePreference(context).current)
    }

    @Test
    fun flowReflectsSelection() {
        preference.setLanguage(AppLanguage.EN)

        assertEquals(AppLanguage.EN, preference.language.value)
    }

    @Test
    fun invalidStoredValueFallsBackToChinese() {
        context.getSharedPreferences("daily_record_language", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", "FR")
            .commit()

        assertEquals(AppLanguage.ZH, preference.current)
    }
}
