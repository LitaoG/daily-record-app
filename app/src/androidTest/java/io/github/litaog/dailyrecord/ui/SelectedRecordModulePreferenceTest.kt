package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectedRecordModulePreferenceTest {
    @Test
    fun selectionSurvivesNewPreferenceInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preference = SelectedRecordModulePreference(context)
        preference.setSelectedModule(RecordModule.Sex)

        assertEquals(
            RecordModule.Sex,
            SelectedRecordModulePreference(context).selectedModule,
        )

        preference.setSelectedModule(RecordModule.HandBrew)
    }
}
