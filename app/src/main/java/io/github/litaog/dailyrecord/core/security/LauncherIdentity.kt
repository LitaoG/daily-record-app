package io.github.litaog.dailyrecord.core.security

/**
 * The entry the launcher shows for this app (ADR-024). Each identity is an
 * `activity-alias` in the manifest that opens the same `ui.MainActivity`.
 *
 * The default identity keeps the component name `.MainActivity` of earlier
 * releases, so home screen icons the user pinned before survive the update.
 */
enum class LauncherIdentity(
    val id: String,
    /** Full component class name of the alias; must match AndroidManifest.xml. */
    val aliasClassName: String,
    /** The alias's `android:enabled` value in the manifest. */
    val enabledInManifest: Boolean,
) {
    Default("default", "$NAMESPACE.MainActivity", enabledInManifest = true),
    Journal("journal", "$NAMESPACE.JournalLauncher", enabledInManifest = false),
    ;

    companion object {
        /** Persisted ids never change; an unknown id falls back to the default. */
        fun fromId(id: String?): LauncherIdentity = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** The Gradle `namespace`, which relative manifest names such as `.MainActivity` resolve against. */
private const val NAMESPACE = "io.github.litaog.dailyrecord"

/** One enable or disable step for a launcher alias. */
data class LauncherAliasChange(
    val identity: LauncherIdentity,
    val enabled: Boolean,
)

/**
 * Steps that leave [target] as the only enabled launcher entry, given which
 * aliases are enabled now. The new entry is always enabled before any other is
 * disabled, so the app never has zero launcher entries, even if the process
 * dies between two steps. An empty list means nothing has to change.
 */
fun launcherAliasChanges(
    target: LauncherIdentity,
    enabledNow: Set<LauncherIdentity>,
): List<LauncherAliasChange> = buildList {
    if (target !in enabledNow) add(LauncherAliasChange(target, enabled = true))
    LauncherIdentity.entries
        .filter { it != target && it in enabledNow }
        .forEach { add(LauncherAliasChange(it, enabled = false)) }
}
