package io.github.litaog.dailyrecord.ui

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class IntimacyOnboardingPreferenceTest {
    private lateinit var fakePreferences: FakeSharedPreferences
    private lateinit var preference: IntimacyOnboardingPreference

    @Before
    fun setUp() {
        fakePreferences = FakeSharedPreferences()
        preference = IntimacyOnboardingPreference(fakePreferences)
    }

    @Test
    fun defaultsToNotDismissed() {
        assertFalse(preference.isDismissed)
        assertFalse(preference.dismissed.value)
    }

    @Test
    fun dismissingPersistsAndUpdatesFlow() {
        preference.dismiss()
        assertTrue(preference.isDismissed)
        assertTrue(preference.dismissed.value)

        val newInstance = IntimacyOnboardingPreference(fakePreferences)
        assertTrue(newInstance.isDismissed)
        assertTrue(newInstance.dismissed.value)
    }

    @Test
    fun repeatedDismissDoesNotFail() {
        preference.dismiss()
        preference.dismiss()
        assertTrue(preference.isDismissed)
    }
}
