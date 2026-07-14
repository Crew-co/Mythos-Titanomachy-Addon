package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.event.DivineDeathEvent
import net.crewco.mythos.api.event.EraAdvancedEvent
import net.crewco.mythos.api.event.RoleClaimedEvent
import net.crewco.mythos.api.event.RoleReleasedEvent
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.command.CommandContext.Companion.mm
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.ERA
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.IMPRISONED
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.SWALLOWED
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import net.crewco.mythos.api.event.MythosResetEvent
import org.bukkit.event.player.PlayerJoinEvent

class TitanomachyListener(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val content: TitanomachyContent,
    private val places: Places,
    private val war: WarState,
    private val killsToEnd: Int,
    private val announceEvery: Int,
) : Listener {

    // ---- the age begins ------------------------------------------------------

    @EventHandler
    fun onEra(event: EraAdvancedEvent) {
        if (event.to.id != ERA) return

        // Kronos and Rhea were only Titans a moment ago. Now they have work.
        content.grantWarPowers()

        // NOT a broadcast. The era's prologue already told the whole server what has
        // happened, at the right pace, while the world held still. This is the part only
        // two people need — and it arrives after the scene, not on top of it.
        context.schedulers.globalDelayed(40) {
            mythos.roles.holders("rhea").mapNotNull { Bukkit.getPlayer(it) }.forEach { rhea ->
                context.schedulers.entity(rhea) {
                    rhea.sendMessage(mm("<gold>You are going to have six children, and he is going to eat them."))
                    rhea.sendMessage(mm("<white>/power bear <spirit> <olympian> <dark_gray>· <white>/power stone <child> <dark_gray>(once. only once.)"))
                }
            }
            mythos.roles.holders("kronos").mapNotNull { Bukkit.getPlayer(it) }.forEach { kronos ->
                context.schedulers.entity(kronos) {
                    kronos.sendMessage(mm("<gold>You know what your children are for. <white>/power devour"))
                }
            }
        }
    }

    // ---- claiming a role that starts in a hole -------------------------------

    @EventHandler
    fun onClaimed(event: RoleClaimedEvent) {
        // An ally contributed by another addon brought an objective with it. Striking it
        // is as simple as this: complete() no-ops if no such objective exists, so we
        // don't need to know which roles came from where.
        mythos.eras.complete(ERA, "ally_${event.role.id}", "${event.role.displayName} came over")

        val prisoner = (content.cyclopes + content.hekatoncheires).any { it.id == event.role.id }
        if (!prisoner) return
        places.imprison(
            event.player,
            "tartarus",
            IMPRISONED,
            "<dark_red>Uranus put you here before there were gods. <gray>Nobody has been down since.",
        )
    }

    /** Anyone in a hole gets put back in it when they log in. */
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val profile = mythos.profiles.profile(event.player.uniqueId)
        context.schedulers.entityDelayed(event.player, 20, retired = null) {
            when {
                profile.hasFlag(SWALLOWED) -> places.leash(event.player, "stomach", SWALLOWED)
                profile.hasFlag(IMPRISONED) -> places.leash(event.player, "tartarus", IMPRISONED)
            }
        }
    }

    // ---- the disgorging ------------------------------------------------------

    /** Zeus, right-clicking his father with a cup. */
    @EventHandler
    fun onInteract(event: PlayerInteractEntityEvent) {
        val zeus = event.player
        val kronos = event.rightClicked as? Player ?: return
        if (mythos.roles.roleOf(kronos.uniqueId)?.id != "kronos") return
        if (!Regalia.isEmetic(zeus.inventory.itemInMainHand, context)) return
        if (mythos.roles.roleOf(zeus.uniqueId)?.id != "zeus") return

        event.isCancelled = true
        val cup = zeus.inventory.itemInMainHand.clone()
        cup.amount -= 1
        zeus.inventory.setItemInMainHand(if (cup.amount <= 0) null else cup)

        context.schedulers.global { disgorge(kronos) }
    }

    private fun disgorge(kronos: Player) {
        val brought = content.olympians
            .flatMap { mythos.roles.holders(it.id) }
            .mapNotNull { Bukkit.getPlayer(it) }
            .filter { mythos.profiles.profile(it.uniqueId).hasFlag(SWALLOWED) }

        brought.forEach { child ->
            places.release(child, SWALLOWED, "gaia", "<yellow>You come up whole, and adult, and extremely angry.")
        }

        context.schedulers.global {
            listOf(
                "",
                "<gray>Kronos brings up a stone first. Then five gods, in reverse order,",
                "<gray>fully grown, and all of them know exactly whose fault this is.",
                "",
                "<gold><b>THE WAR BEGINS.</b> <gray>Choose a side: <white>/claim titan-sworn <gray>· <white>/claim olympian-sworn",
                "<dark_gray><i>The Cyclopes are still at the bottom of Tartarus. Somebody should go and get them.",
                ""
            ).forEach { Bukkit.broadcast(mm(it)) }
        }


        mythos.eras.complete(ERA, "the_disgorging", "everything he swallowed came back up")

        context.schedulers.entity(kronos) {
            kronos.sendMessage(mm("<gold>Everything you ate is standing in front of you. <dark_gray><i>They have had a long time to think."))
        }
    }

    // ---- ten years -----------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    fun onDeath(event: PlayerDeathEvent) {
        if (mythos.eras.currentId() != ERA) return
        val killer = event.player.killer ?: return
        val victimSide = faction(mythos.roles.roleOf(event.player.uniqueId)) ?: return
        val killerSide = faction(mythos.roles.roleOf(killer.uniqueId)) ?: return
        if (victimSide == killerSide) return

        val counter = if (killerSide == TITAN_SIDE) war.titanKills else war.olympianKills
        counter.incrementAndGet()
        val total = war.total()
        context.schedulers.async { war.save() }

        // Ten years of war is 200 bodies with a hundred players and one body with one.
        val killsToEnd = mythos.dev.threshold(this.killsToEnd)

        if (total % announceEvery == 0 && total < killsToEnd) {
            Bukkit.getServer().sendMessage(
                mm("<dark_gray>» <gray>The war grinds on. <gold>Titans ${war.titanKills.get()} <dark_gray>· <yellow>Olympians ${war.olympianKills.get()} <dark_gray>(of $killsToEnd)"),
            )
        }
        if (total >= killsToEnd && !mythos.eras.isComplete(ERA, "ten_years")) {
            mythos.eras.complete(ERA, "ten_years", "ten years, and no ground left flat")
            Bukkit.getServer().sendMessage(mm("<gold>Ten years. <gray>Kronos can be brought down now — but only by a god holding what the Cyclopes made."))
        }
    }

    // ---- who can kill whom ---------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    fun onDivineDeath(event: DivineDeathEvent) {
        // Inside a stomach, nothing can reach you. Not even mercy.
        if (mythos.profiles.profile(event.victim.uniqueId).hasFlag(SWALLOWED)) {
            event.isCancelled = true
            return
        }
        if (event.victimRole.id != "kronos") return

        val killer = event.killer
        val wieldsRegalia = killer != null && Regalia.OWNERS.keys.any { Regalia.held(killer, it, context) }

        when {
            !mythos.eras.isComplete(ERA, "ten_years") -> {
                event.isCancelled = true
                killer?.sendMessage(mm("<gray>He is <white>time<gray>. You have not spent enough of it yet."))
            }
            !wieldsRegalia -> {
                event.isCancelled = true
                killer?.sendMessage(mm("<gray>Not with that. <dark_gray><i>The Cyclopes made three things. Bring one."))
            }
            else -> {
                event.isCancelled = false
                event.unmakes = true
            }
        }
    }

    /** Kronos falls. The age turns, and something downstream — not this jar — picks it up. */
    @EventHandler
    fun onReleased(event: RoleReleasedEvent) {
        if (event.role.id != "kronos") return

        Bukkit.getServer().sendMessage(mm(""))
        Bukkit.getServer().sendMessage(mm("<gray>They throw him as far down as the sky is high, and put a hundred hands on the gate."))
        Bukkit.getServer().sendMessage(mm("<dark_gray><i>He was a son once. He will have a very long time to think about that."))
        Bukkit.getServer().sendMessage(mm(""))

        context.schedulers.global {
            mythos.roles.seal("kronos", "bound in Tartarus, under a hundred hands")
            mythos.eras.complete(ERA, "kronos_bound", "the harvest was cut down in its turn")
        }
    }

    /**
     * The engine has wiped the world. It doesn't know this addon keeps a kill tally in
     * war.yml — only we know that, so only we can clean it up.
     */
    @EventHandler
    fun onReset(event: MythosResetEvent) {
        if (event.scope == MythosResetEvent.Scope.PLAYER) return // we hold no per-player state of our own
        war.clear()
        context.schedulers.async { war.save() }
        context.logger.info("War tally cleared. The ten years did not happen.")
    }

    // ---- sides ---------------------------------------------------------------

    private fun faction(role: RoleDefinition?): String? = when {
        role == null -> null
        role.id == "titan-sworn" -> TITAN_SIDE
        role.id == "olympian-sworn" -> OLYMPIAN_SIDE
        role.tier == RoleTier.TITAN -> TITAN_SIDE
        role.tier == RoleTier.OLYMPIAN -> OLYMPIAN_SIDE
        // The Cyclopes and the Hundred-Handed fight for whoever let them out — and
        // in every version of the story, that's Zeus.
        role.tier == RoleTier.MONSTER -> OLYMPIAN_SIDE
        else -> null
    }

    companion object {
        const val TITAN_SIDE = "titan"
        const val OLYMPIAN_SIDE = "olympian"
    }
}
