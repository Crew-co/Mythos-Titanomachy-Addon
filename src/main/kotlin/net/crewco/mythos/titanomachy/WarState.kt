package net.crewco.mythos.titanomachy

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** The tally. Ten years, counted in bodies. */
class WarState(private val file: File) {

    private val yaml = if (file.exists()) YamlConfiguration.loadConfiguration(file) else YamlConfiguration()

    val titanKills = AtomicInteger(yaml.getInt("kills.titan", 0))
    val olympianKills = AtomicInteger(yaml.getInt("kills.olympian", 0))

    /** Which of the three gifts have already been made — a Cyclops can't spam them. */
    val forged: MutableSet<String> = yaml.getStringList("forged").toMutableSet()

    fun total() = titanKills.get() + olympianKills.get()

    fun clear() {
        titanKills.set(0)
        olympianKills.set(0)
        forged.clear()
    }

    /** Blocking write — call from the async scheduler. */
    @Synchronized
    fun save() {
        yaml.set("kills.titan", titanKills.get())
        yaml.set("kills.olympian", olympianKills.get())
        yaml.set("forged", forged.toList())
        runCatching { yaml.save(file) }
    }
}
