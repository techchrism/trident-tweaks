package me.techchrism.tridenttweaks

import org.bukkit.entity.Trident
import org.bukkit.scheduler.BukkitRunnable

class TridentHallMonitorRunnable(private val trident: Trident) : BukkitRunnable() {
    override fun run() {
        if(!trident.isValid) {
            cancel()
            return
        }
        if(trident.y <= trident.world.minHeight) {
            trident.velocity.y = 0.0
            trident.setHasDealtDamage(true)
            cancel()
        }
    }
}