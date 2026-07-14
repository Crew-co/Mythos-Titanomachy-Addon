package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.power.Power
import net.crewco.mythos.api.power.PowerContext
import net.crewco.mythos.command.CommandContext.Companion.mm
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.ERA
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.HIDDEN
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.IMPRISONED
import net.crewco.mythos.titanomachy.TitanomachyContent.Companion.SWALLOWED
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

/** Rhea reaches into the spirit world, exactly as her mother did. */
class BearPower(private val mythos: Mythos, private val context: AddonContext, private val content: TitanomachyContent) : Power {
    override val id = "bear"
    override val displayName = "Bear a Child"
    override val description = "Give one of the waiting dead a name, and a father who will eat it. /power bear <spirit> <olympian>"
    override val cooldownSeconds = 90

    override fun use(ctx: PowerContext): Boolean {
        val rhea = ctx.player
        if (ctx.args.size < 2) {
            val free = content.olympians.filter { mythos.roles.holders(it.id).isEmpty() }
            rhea.sendMessage(mm("<red>/power bear <spirit> <olympian>"))
            rhea.sendMessage(mm("<gray>Unborn: <white>${free.joinToString { it.id }}"))
            return false
        }
        val target = Bukkit.getPlayerExact(ctx.args[0]) ?: return false.also { rhea.sendMessage(mm("<red>No such spirit is here.")) }
        val childId = ctx.args[1].lowercase()
        if (content.olympians.none { it.id == childId }) {
            rhea.sendMessage(mm("<red>That is not one of yours.")); return false
        }
        if (!mythos.spirits.isSpirit(target.uniqueId)) {
            rhea.sendMessage(mm("<red>${target.name} already exists.")); return false
        }
        if (mythos.roles.holders(childId).isNotEmpty()) {
            rhea.sendMessage(mm("<red>You have already borne that one.")); return false
        }

        context.schedulers.global {
            mythos.roles.assign(target.uniqueId, childId, "borne by Rhea")
            Bukkit.getServer().sendMessage(mm("<dark_gray>» <gold>Rhea <gray>bears a child. <dark_gray><i>Somewhere, a father is hungry."))
            mythos.roles.holders("kronos").mapNotNull { Bukkit.getPlayer(it) }.forEach { kronos ->
                context.schedulers.entity(kronos) {
                    kronos.sendMessage(mm("<gold>Another one. <white>/power devour ${target.name}"))
                }
            }
        }
        return true
    }
}

/** Kronos, doing the only thing he knows how to do. */
class DevourPower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val content: TitanomachyContent,
    private val places: Places,
) : Power {
    override val id = "devour"
    override val displayName = "Devour"
    override val description = "Swallow one of your children whole. They are not dead. That is the horror of it. /power devour <child>"
    override val cooldownSeconds = 30

    override fun use(ctx: PowerContext): Boolean {
        val kronos = ctx.player
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) }
            ?: return false.also { kronos.sendMessage(mm("<red>/power devour <player>")) }

        val role = mythos.roles.roleOf(target.uniqueId)
        if (role == null || content.olympians.none { it.id == role.id }) {
            kronos.sendMessage(mm("<red>Only your own children go down easily.")); return false
        }
        val profile = mythos.profiles.profile(target.uniqueId)
        if (profile.hasFlag(SWALLOWED)) {
            kronos.sendMessage(mm("<red>That one is already inside you.")); return false
        }

        // The lie. Rhea marked this child, and put a wrapped rock in his hands.
        if (profile.hasFlag(HIDDEN)) {
            val stone = kronos.inventory.contents.firstOrNull { Regalia.isStone(it, context) }
            if (stone != null) {
                kronos.inventory.removeItem(stone)
                kronos.sendMessage(mm("<gray>You swallow it without chewing. <dark_gray><i>It is heavier than a baby. You do not think about that."))
                Bukkit.getServer().sendMessage(mm("<dark_gray>» <gold>Kronos <gray>swallows something. <dark_gray><i>Nobody says anything."))
                mythos.eras.complete(ERA, "the_stone", "a stone, swaddled, went down instead")
                return true
            }
            // No stone in hand — the trick fails, and the child is eaten after all.
            profile.setFlag(HIDDEN, null)
        }

        // Not a hole at bedrock. A world, with the others already in it.
        places.imprison(
            target,
            "stomach",
            SWALLOWED,
            "<dark_red>You can hear your siblings breathing. <gray>They are in here with you.",
        )
        Bukkit.getServer().sendMessage(mm("<dark_gray>» <gold>Kronos <gray>swallows <yellow>${role.displayName}<gray>, whole."))

        val swallowed = content.olympians.count { olympian ->
            mythos.roles.holders(olympian.id).any { mythos.profiles.profile(it).hasFlag(SWALLOWED) }
        }
        if (swallowed >= mythos.dev.threshold(5)) {
            mythos.eras.complete(ERA, "the_swallowing", "five children, and a stomach that never fills")
        }
        return true
    }
}

/** Rhea's one good idea. */
class StonePower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val content: TitanomachyContent,
    private val places: Places,
) : Power {
    override val id = "stone"
    override val displayName = "The Swaddled Stone"
    override val description = "Hide one child, and hand your husband a rock in a blanket. /power stone <child>"
    override val cooldownSeconds = 0

    override fun use(ctx: PowerContext): Boolean {
        val rhea = ctx.player
        if (mythos.eras.isComplete(ERA, "the_stone")) {
            rhea.sendMessage(mm("<red>You get to do this once. He is stupid, not that stupid.")); return false
        }
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) }
            ?: return false.also { rhea.sendMessage(mm("<red>/power stone <child>")) }
        val role = mythos.roles.roleOf(target.uniqueId)
        if (role == null || content.olympians.none { it.id == role.id }) {
            rhea.sendMessage(mm("<red>That is not your child.")); return false
        }

        mythos.profiles.profile(target.uniqueId).setFlag(HIDDEN, true)

        val kronos = mythos.roles.holders("kronos").firstNotNullOfOrNull { Bukkit.getPlayer(it) }
        if (kronos == null) {
            rhea.sendMessage(mm("<red>He isn't here to be lied to.")); return false
        }
        context.schedulers.entity(kronos) {
            kronos.inventory.addItem(Regalia.stone(context))
            kronos.sendMessage(mm("<gray>Rhea hands you the child, wrapped tight. <dark_gray><i>She is not crying. You don't wonder why."))
        }

        val hiding = places.crete() ?: return false.also { rhea.sendMessage(mm("<red>There is nowhere to hide him.")) }
        target.teleportAsync(hiding).thenRun {
            context.schedulers.entity(target) {
                target.sendMessage(mm("<green>A cave. A goat. Noise, constantly, so he can't hear you cry."))
                target.sendMessage(mm("<dark_gray><i>Grow up. Come back. You know what to do."))
            }
        }
        rhea.sendMessage(mm("<gold>It's done. <gray>Now the child has to survive long enough to matter."))
        return true
    }
}

/** Zeus, grown, with a plan and a cup. */
class DraughtPower(private val mythos: Mythos, private val context: AddonContext) : Power {
    override val id = "draught"
    override val displayName = "The Emetic Draught"
    override val description = "Metis mixes it. Right-click your father with it. /power draught"
    override val cooldownSeconds = 0

    override fun use(ctx: PowerContext): Boolean {
        val zeus = ctx.player
        if (!mythos.eras.isComplete(ERA, "the_swallowing")) {
            zeus.sendMessage(mm("<red>There is nothing in him to bring up. Not yet.")); return false
        }
        if (mythos.eras.isComplete(ERA, "the_disgorging")) {
            zeus.sendMessage(mm("<red>They're already out. Look around.")); return false
        }
        zeus.inventory.addItem(Regalia.emetic(context))
        zeus.sendMessage(mm("<dark_green>Metis gives you the cup, and does not explain how she made it."))
        zeus.sendMessage(mm("<gray>Get close to Kronos. <white>Right-click him with it."))
        return true
    }
}

/** Somebody has to go down and get them. */
class FreePower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val content: TitanomachyContent,
    private val places: Places,
) : Power {
    override val id = "free"
    override val displayName = "Open the Pit"
    override val description = "Go to the bottom of Tartarus and let out what Uranus put there. /power free"
    override val cooldownSeconds = 10

    override fun use(ctx: PowerContext): Boolean {
        val god = ctx.player

        // getNearbyEntities only ever returns entities owned by OUR region, so this
        // is the one Folia-legal way to ask "who is standing next to me". Reading
        // another player's Location across regions is exactly the thing that throws.
        val prisonerIds = (content.cyclopes + content.hekatoncheires).map { it.id }.toSet()
        val prisoners = god.getNearbyEntities(20.0, 20.0, 20.0)
            .filterIsInstance<Player>()
            .filter { mythos.roles.roleOf(it.uniqueId)?.id in prisonerIds }
            .filter { mythos.profiles.profile(it.uniqueId).hasFlag(IMPRISONED) }

        if (prisoners.isEmpty()) {
            god.sendMessage(mm("<red>Nothing down here but rock. <gray>They're at the bottom of Tartarus, and it is a long way down."))
            return false
        }

        prisoners.forEach { prisoner ->
            places.release(
                prisoner,
                IMPRISONED,
                "gaia",
                "<gold>Light. You have not seen it since before there was a sky to hang it in.",
            )
            context.schedulers.entity(prisoner) {
                prisoner.sendMessage(mm("<gray>An Olympian came down for you. <dark_gray><i>Nobody has ever come down for you."))
                prisoner.sendMessage(mm("<white>/power forge <god> <gray>— give them something worth the trip."))
            }
        }
        Bukkit.getServer().sendMessage(
            mm("<dark_gray>» <yellow>${god.name} <gray>opens the pit. <dark_gray><i>The things inside it remember who did that."),
        )
        val freed = (content.cyclopes + content.hekatoncheires)
            .flatMap { mythos.roles.holders(it.id) }
            .count { !mythos.profiles.profile(it).hasFlag(IMPRISONED) }
        if (freed >= mythos.dev.threshold(3)) {
            mythos.eras.complete(ERA, "the_cyclopes", "the smiths were let out, and they were grateful")
        }
        return true
    }
}

/** The gift that decides the war. */
class ForgePower(
    private val mythos: Mythos,
    private val context: AddonContext,
    private val war: WarState,
) : Power {
    override val id = "forge"
    override val displayName = "Forge a Gift"
    override val description = "Make the thunderbolt, the trident, or the helm — for the god who let you out. /power forge <zeus|poseidon|hades>"
    override val cooldownSeconds = 60

    override fun use(ctx: PowerContext): Boolean {
        val smith = ctx.player
        if (mythos.profiles.profile(smith.uniqueId).hasFlag(IMPRISONED)) {
            smith.sendMessage(mm("<red>You have no forge down here. You have a hole.")); return false
        }
        val godId = ctx.args.firstOrNull()?.lowercase()
            ?: return false.also { smith.sendMessage(mm("<red>/power forge <zeus|poseidon|hades>")) }

        val kind = Regalia.OWNERS.entries.firstOrNull { it.value == godId }?.key
            ?: return false.also { smith.sendMessage(mm("<red>You have nothing to give that one.")) }

        if (!war.forged.add(kind)) {
            smith.sendMessage(mm("<red>Your brothers already made that one.")); return false
        }
        val god = mythos.roles.holders(godId).mapNotNull { Bukkit.getPlayer(it) }.firstOrNull()
        if (god == null) {
            war.forged.remove(kind)
            smith.sendMessage(mm("<red>They aren't here to take it.")); return false
        }

        context.schedulers.entity(god) {
            god.inventory.addItem(Regalia.item(kind, context))
            god.sendMessage(mm("<gold>The Cyclopes give you a gift. <gray>It is the reason you win."))
        }
        context.schedulers.global { Bukkit.getServer().sendMessage(mm("<dark_gray>» <dark_aqua>${smith.name} <gray>forges something for <yellow>$godId<gray>."))
        }
        context.schedulers.async { war.save() }
        return true
    }
}

// ---- the regalia, in use ---------------------------------------------------

class ThunderboltPower(private val mythos: Mythos, private val context: AddonContext) : Power {
    override val id = "thunderbolt"
    override val displayName = "The Thunderbolt"
    override val description = "Requires the Thunderbolt in hand. /power thunderbolt <player>"
    override val cooldownSeconds = 45

    override fun use(ctx: PowerContext): Boolean {
        val zeus = ctx.player
        if (!Regalia.held(zeus, Regalia.THUNDERBOLT, context)) {
            zeus.sendMessage(mm("<red>Your hand is empty. <gray>The Cyclopes have not made it yet.")); return false
        }
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) }
            ?: return false.also { zeus.sendMessage(mm("<red>/power thunderbolt <player>")) }

        // Cross-region: everything about the target happens on the target's thread.
        context.schedulers.entity(target) {
            target.world.strikeLightning(target.location)
            target.damage(30.0, zeus)

            // THE WORLD IS DIFFERENT AFTERWARDS. A thunderbolt leaves a scar you can walk
            // back to a year later, which is the whole difference between a power and a
            // status effect.
            val ground = target.location
            for (x in -2..2) for (z in -2..2) {
                if (x * x + z * z > 5) continue
                val block = ground.clone().add(x.toDouble(), -1.0, z.toDouble()).block
                if (block.type.isSolid) block.type = org.bukkit.Material.BLACKSTONE
            }
            ground.clone().add(0.0, 0.0, 0.0).block.let { if (it.type.isAir) it.type = org.bukkit.Material.FIRE }
        }
        return true
    }
}

class QuakePower(private val context: AddonContext) : Power {
    override val id = "quake"
    override val displayName = "Earth-shaker"
    override val description = "Requires the Trident. Throws everything near you into the air. /power quake"
    override val cooldownSeconds = 60

    override fun use(ctx: PowerContext): Boolean {
        val poseidon = ctx.player
        if (!Regalia.held(poseidon, Regalia.TRIDENT, context)) {
            poseidon.sendMessage(mm("<red>Without the Trident you are just a large angry man.")); return false
        }
        // Same region as us, by definition of getNearbyEntities — safe to touch.
        poseidon.getNearbyEntities(12.0, 6.0, 12.0)
            .filterIsInstance<Player>()
            .forEach { victim ->
                victim.velocity = Vector(victim.velocity.x, 1.4, victim.velocity.z)
                victim.damage(8.0, poseidon)
            }

        // The Earth-shaker shakes the earth. Fissures open, permanently, and the ground he
        // broke stays broken — a river will find them eventually, and that's someone's problem.
        val centre = poseidon.location
        repeat(6) {
            val angle = Math.random() * Math.PI * 2
            val length = 3 + (Math.random() * 6).toInt()
            for (step in 1..length) {
                val spot = centre.clone().add(Math.cos(angle) * step, -1.0, Math.sin(angle) * step)
                for (depth in 0..2) {
                    val block = spot.clone().add(0.0, -depth.toDouble(), 0.0).block
                    if (block.type.isSolid) block.type = org.bukkit.Material.AIR
                }
            }
        }
        poseidon.world.createExplosion(centre, 0f, false, false)
        return true
    }
}

class UnseenPower(private val context: AddonContext) : Power {
    override val id = "unseen"
    override val displayName = "The Helm of Darkness"
    override val description = "Requires the Helm. You are not invisible. You are not there. /power unseen"
    override val cooldownSeconds = 120

    override fun use(ctx: PowerContext): Boolean {
        val hades = ctx.player
        if (!Regalia.held(hades, Regalia.HELM, context)) {
            hades.sendMessage(mm("<red>You need the Helm.")); return false
        }
        hades.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 1200, 0, false, false))
        hades.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 1200, 0, false, false))
        hades.sendMessage(mm("<dark_gray><i>Nobody looks for you. Nobody can."))
        return true
    }
}

/** A hundred hands, throwing mountains. This is what actually ends the war. */
class HundredfoldPower(private val context: AddonContext) : Power {
    override val id = "hundredfold"
    override val displayName = "Hundredfold"
    override val description = "Throw a mountain at everything in front of you. /power hundredfold"
    override val cooldownSeconds = 90

    override fun use(ctx: PowerContext): Boolean {
        val giant = ctx.player
        val hit = giant.getNearbyEntities(16.0, 8.0, 16.0).filterIsInstance<Player>()
        if (hit.isEmpty()) {
            giant.sendMessage(mm("<gray>Nothing within reach, and you have a great deal of reach.")); return false
        }
        hit.forEach { victim ->
            victim.velocity = victim.location.toVector().subtract(giant.location.toVector()).normalize().multiply(2.0).setY(0.8)
            victim.damage(12.0, giant)

            // He throws MOUNTAINS. Real falling blocks, torn out of the ground under him and
            // dropped on them from above — the world is visibly worse afterwards, which is the
            // entire difference between a power and a potion effect.
            repeat(4) {
                val above = victim.location.clone().add(
                    (-2..2).random().toDouble(), 12.0, (-2..2).random().toDouble(),
                )
                val rock = giant.world.spawnFallingBlock(above, org.bukkit.Material.STONE.createBlockData())
                rock.dropItem = false
                rock.setHurtEntities(true)
            }
        }
        giant.world.createExplosion(giant.location, 0f, false, true)
        giant.sendMessage(mm("<dark_aqua>Fifty heads. A hundred hands. <gray>They did not think this through."))
        return true
    }
}

/**
 * Gaia's power — and Gaia belongs to a *different addon*.
 *
 * EraOfCreation registered her with `birth`, `sickle` and nothing else. This addon
 * called `mythos.roles.extend("gaia")` and gave her one more, because in this age she is
 * no longer the mother — she's the grandmother who has seen this exact thing happen
 * before and cannot stop watching it happen again.
 *
 * That is the whole worldbuilding argument for extension: a character *changes* as the
 * story moves, and the addon that changes her doesn't have to be the addon that made her.
 */
class ProphesyPower(private val mythos: Mythos, private val context: AddonContext) : Power {
    override val id = "prophesy"
    override val displayName = "Tell Them How It Ends"
    override val description = "You have watched this before. Say so, out loud, to someone who won't listen. /power prophesy <player>"
    override val cooldownSeconds = 600

    override fun use(ctx: PowerContext): Boolean {
        val gaia = ctx.player
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) }
            ?: return false.also { gaia.sendMessage(mm("<red>/power prophesy <player>")) }

        val role = mythos.roles.roleOf(target.uniqueId)
        val prophecy = when (role?.id) {
            "kronos" ->
                "<gold>Kronos<gray>. You will be put down by your own son, exactly as you put down your father. " +
                    "<i>You already know this. That is why you are so frightened."
            "zeus" ->
                "<yellow>Zeus<gray>. You will win, and then you will spend eternity afraid of a child you haven't had yet. " +
                    "<i>It runs in the family."
            null -> "<gray>You are nobody, and so nothing happens to you. <i>Enjoy it while it lasts."
            else ->
                "<gray>${role.displayName}<gray>. The Earth has seen your kind before, and buried them, " +
                    "<i>and grown grass on top."
        }

        context.schedulers.global {
            Bukkit.getServer().sendMessage(mm("<dark_gray>» <green>Gaia <gray>speaks, and the ground speaks with her:"))
            Bukkit.getServer().sendMessage(mm("<dark_gray>   <i>$prophecy"))
        }
        mythos.chronicle.record("story", "<green>Gaia <gray>prophesied to <white>${target.name}<gray>, who did not listen.")

        context.schedulers.entity(target) {
            target.sendMessage(mm("<dark_gray><i>The ground under you does not feel entirely solid any more."))
        }
        return true
    }
}
