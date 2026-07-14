package net.crewco.mythos.titanomachy

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.entity.Player

/**
 * Two prisons, and both of them are worlds now.
 *
 * This file used to compute a y-coordinate near bedrock and leash people to it with a
 * distance check. It is a great deal shorter, and a great deal more *true*, now that the
 * stomach of Kronos is a place you can actually be.
 *
 * The leash still exists, because a realm's access rules stop you getting IN, not OUT — and
 * "things go into Tartarus and do not come out" is a property of the prison, not of the door.
 */
class Places(
    private val context: AddonContext,
    private val mythos: Mythos,
) {

    /** Put them in a world, and set the flag that keeps them there. */
    fun imprison(player: Player, realmId: String, flag: String, message: String) {
        mythos.profiles.profile(player.uniqueId).setFlag(flag, true)
        mythos.realms.send(player, realmId, message)
        leash(player, realmId, flag)
    }

    /**
     * While the flag is set, they belong to that world. Wander out — by death, by command,
     * by a portal nobody thought about — and the world takes them back.
     */
    fun leash(player: Player, realmId: String, flag: String) {
        context.schedulers.entityRepeating(player, 60, 60, retired = null) { task ->
            val profile = mythos.profiles.profile(player.uniqueId)
            if (!profile.hasFlag(flag) || !player.isOnline) {
                task.cancel()
                return@entityRepeating
            }
            if (mythos.realms.realmOf(player)?.id != realmId) {
                mythos.realms.send(player, realmId, "There is no way out that way.")
            }
        }
    }

    fun release(player: Player, flag: String, toRealm: String, message: String) {
        mythos.profiles.profile(player.uniqueId).setFlag(flag, null)
        mythos.realms.send(player, toRealm, message)
        context.schedulers.entity(player) {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS)
            player.sendMessage(mm(message))
        }
    }

    /** Somewhere very far away, where a father won't look. Crete is still just a place in Gaia. */
    fun crete(): org.bukkit.Location? {
        val world = mythos.realms.world("gaia") ?: return null
        val angle = Math.random() * Math.PI * 2
        val distance = 1500 + Math.random() * 1500
        val x = world.spawnLocation.x + Math.cos(angle) * distance
        val z = world.spawnLocation.z + Math.sin(angle) * distance
        return org.bukkit.Location(world, x, world.getHighestBlockYAt(x.toInt(), z.toInt()) + 1.0, z)
    }
}
