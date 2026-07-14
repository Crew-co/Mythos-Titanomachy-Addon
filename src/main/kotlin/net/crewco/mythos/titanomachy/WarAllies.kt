package net.crewco.mythos.titanomachy

import net.crewco.mythos.api.role.RoleTier

/**
 * **The hole this addon opens in itself.**
 *
 * The war had allies on both sides, and this jar does not know who they were — because
 * some of them haven't been written yet. So instead of hard-coding a list, it opens an
 * extension point and lets any addon, ever, post to it:
 *
 * ```kotlin
 * // In some jar written a year from now — say, a Prometheus addon.
 * // build.gradle.kts:  compileOnly("net.crewco:titanomachy:0.1.0")
 * // addon.yml:         depends: [ Titanomachy ]
 *
 * mythos.extensions.contribute(
 *     WarAlly.POINT,
 *     WarAlly(
 *         id = "styx",
 *         name = "Styx",
 *         side = WarAlly.OLYMPIAN,
 *         lore = "First to come over. Zeus swore every oath on her afterwards, forever.",
 *     ),
 * )
 * ```
 *
 * Titanomachy turns each contribution into a **claimable role** and an **optional
 * objective in its own era** — so a story written years later doesn't just *reference*
 * the war, it changes what's in it. And because the engine's extension registry replays
 * contributions, it works whether that addon loads before or after this one.
 *
 * The contract is a plain data class on purpose: a contributor who only wants to add a
 * name to the war needs no behaviour, and therefore no `depends:` on anything but the
 * type. Behaviour goes in a role's powers, which is what roles are for.
 */
data class WarAlly(
    /** Lowercase and unique — it becomes the role id. */
    val id: String,
    val name: String,
    /** [TITAN] or [OLYMPIAN]. */
    val side: String,
    val lore: String,
    /** Powers this ally grants — register them yourself before contributing. */
    val powers: List<String> = emptyList(),
    val tier: RoleTier = RoleTier.MONSTER,
    /** How many players may be this ally. Rivers and winds can be many; Styx is one. */
    val seats: Int = 1,
) {
    companion object {
        const val POINT = "titanomachy:allies"
        const val TITAN = "titan"
        const val OLYMPIAN = "olympian"
    }
}
