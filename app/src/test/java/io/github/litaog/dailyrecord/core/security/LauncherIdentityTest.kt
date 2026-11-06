package io.github.litaog.dailyrecord.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIdentityTest {
    @Test
    fun unknownOrMissingIdFallsBackToTheDefaultIdentity() {
        assertEquals(LauncherIdentity.Default, LauncherIdentity.fromId(null))
        assertEquals(LauncherIdentity.Default, LauncherIdentity.fromId("calculator"))
        assertEquals(LauncherIdentity.Journal, LauncherIdentity.fromId("journal"))
    }

    @Test
    fun defaultIdentityKeepsTheComponentNameOfEarlierReleases() {
        // Pinned home screen icons point at this component; renaming it would
        // remove them on the update.
        assertEquals("io.github.litaog.dailyrecord.MainActivity", LauncherIdentity.Default.aliasClassName)
        assertEquals("io.github.litaog.dailyrecord.JournalLauncher", LauncherIdentity.Journal.aliasClassName)
        assertTrue(LauncherIdentity.Default.enabledInManifest)
        assertEquals(listOf(LauncherIdentity.Default), LauncherIdentity.entries.filter { it.enabledInManifest })
    }

    @Test
    fun switchingEnablesTheNewEntryBeforeDisablingTheOldOne() {
        assertEquals(
            listOf(
                LauncherAliasChange(LauncherIdentity.Journal, enabled = true),
                LauncherAliasChange(LauncherIdentity.Default, enabled = false),
            ),
            launcherAliasChanges(LauncherIdentity.Journal, setOf(LauncherIdentity.Default)),
        )
        assertEquals(
            listOf(
                LauncherAliasChange(LauncherIdentity.Default, enabled = true),
                LauncherAliasChange(LauncherIdentity.Journal, enabled = false),
            ),
            launcherAliasChanges(LauncherIdentity.Default, setOf(LauncherIdentity.Journal)),
        )
    }

    @Test
    fun matchingStateNeedsNoChange() {
        assertEquals(
            emptyList<LauncherAliasChange>(),
            launcherAliasChanges(LauncherIdentity.Default, setOf(LauncherIdentity.Default)),
        )
        assertEquals(
            emptyList<LauncherAliasChange>(),
            launcherAliasChanges(LauncherIdentity.Journal, setOf(LauncherIdentity.Journal)),
        )
    }

    @Test
    fun interruptedSwitchWithBothEntriesIsRepairedByDisablingTheOtherOne() {
        assertEquals(
            listOf(LauncherAliasChange(LauncherIdentity.Default, enabled = false)),
            launcherAliasChanges(
                LauncherIdentity.Journal,
                setOf(LauncherIdentity.Default, LauncherIdentity.Journal),
            ),
        )
    }

    @Test
    fun noEnabledEntryIsRepairedByEnablingTheTarget() {
        assertEquals(
            listOf(LauncherAliasChange(LauncherIdentity.Default, enabled = true)),
            launcherAliasChanges(LauncherIdentity.Default, emptySet()),
        )
    }

    @Test
    fun everyPlanEndsWithExactlyTheTargetEnabled() {
        val states = listOf(
            emptySet(),
            setOf(LauncherIdentity.Default),
            setOf(LauncherIdentity.Journal),
            LauncherIdentity.entries.toSet(),
        )
        for (target in LauncherIdentity.entries) {
            for (start in states) {
                val enabled = start.toMutableSet()
                for (change in launcherAliasChanges(target, start)) {
                    if (change.enabled) enabled += change.identity else enabled -= change.identity
                    // Never zero launcher entries between two steps.
                    assertTrue("no entry left while moving to $target from $start", enabled.isNotEmpty())
                }
                assertEquals(setOf(target), enabled)
            }
        }
    }
}
