package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalModePreferenceTest {
    @Test
    fun explicitLocalModeChoiceSurvivesNewPreferenceInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preference = LocalModePreference(context)
        preference.setEnabled(false)

        preference.setEnabled(true)
        assertTrue(LocalModePreference(context).isEnabled)

        preference.setEnabled(false)
        assertFalse(LocalModePreference(context).isEnabled)
    }
}
