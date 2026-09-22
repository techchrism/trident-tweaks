package me.techchrism.tridenttweaks

import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

@Suppress("unused")
class TridentTweaks : JavaPlugin(), Listener {
    // Bedrock edition has a 25.0% drop chance, Java edition has an 8.5% drop chance
    private val EXTRA_DROP_CHANCE = (25.0 - 8.5) / 100.0

    private lateinit var offhandKey: NamespacedKey
    private val random = Random()
    private var enableOffhandReturn = true
    private var enableVoidSaving = true
    private var disableLoyaltyPortals = true
    private var enableBedrockImpaling = true
    private var enableBedrockDropping = true
    private var enableRainLightningRod = true

    override fun onEnable() {
        offhandKey = NamespacedKey(this, "thrown-from-offhand")
        Bukkit.getPluginManager().registerEvents(this, this)
        reload()
    }

    private fun reload() {
        saveDefaultConfig()
        reloadConfig()

        enableBedrockImpaling = getConfig().getBoolean("enable-bedrock-impaling")
        enableVoidSaving = getConfig().getBoolean("enable-void-saving")
        enableOffhandReturn = getConfig().getBoolean("enable-offhand-return")
        disableLoyaltyPortals = getConfig().getBoolean("disable-loyalty-portals")
        enableBedrockDropping = getConfig().getBoolean("enable-bedrock-dropping")
        enableRainLightningRod = getConfig().getBoolean("enable-rain-lightning-rod")
    }

    private fun displayEnchantedHit(entity: Entity) {
        for (i in 0..<16 * 3) {
            val d = (this.random.nextDouble() * 2.0 - 1.0)
            val e = (this.random.nextDouble() * 2.0 - 1.0)
            val f = (this.random.nextDouble() * 2.0 - 1.0)
            if (d * d + e * e + f * f <= 1.0) {
                val loc = entity.location.clone()
                loc.add(
                    (entity.width * (d / 4.0)),
                    (entity.height * (0.5 + e / 4.0)),
                    (entity.width * (f / 4.0))
                )
                loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 0, d, e + 0.2, f)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        // Don't add extra damage for aquatic mobs which the enchantment already applies to
        if(!enableBedrockImpaling || Tag.ENTITY_TYPES_AQUATIC.isTagged(event.entityType)) return

        val impaling: Int = when (val damager = event.damager) {
            is Trident -> damager.itemStack.enchantments[Enchantment.IMPALING] ?: return
            is LivingEntity -> damager.equipment?.itemInMainHand?.enchantments[Enchantment.IMPALING] ?: return
            else -> return
        }

        if(event.entity.isInRain || event.entity.isInWater) {
            event.damage += (impaling * 2.5)
            displayEnchantedHit(event.entity)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onTridentThrow(event: ProjectileLaunchEvent) {
        val entity = event.entity
        if(entity !is Trident) return

        if(enableVoidSaving && entity.loyaltyLevel > 0) {
            TridentHallMonitorRunnable(entity).runTaskTimer(this, 0L, 1L)
        }
        if(enableOffhandReturn) {
            val thrower = entity.shooter
            if(
                thrower is Player &&
                thrower.inventory.itemInMainHand.type != Material.TRIDENT &&
                thrower.inventory.itemInOffHand.type == Material.TRIDENT
            ) {
                entity.persistentDataContainer.set(offhandKey, PersistentDataType.BOOLEAN, true)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onEntityPickupItem(event: PlayerPickupArrowEvent) {
        if(!enableOffhandReturn) return
        val trident = event.arrow
        val player = event.player
        if(
            trident !is Trident ||
            !(trident.persistentDataContainer.get(offhandKey, PersistentDataType.BOOLEAN) ?: false)
        ) return

        // Flag the item being picked up
        event.item.itemStack.editPersistentDataContainer { it.set(offhandKey, PersistentDataType.BOOLEAN, true) }

        // In one tick (because the item isn't picked up yet), search for the flagged item and move it to the offhand
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, Runnable {
            if(!player.isValid || player.inventory.itemInOffHand.type != Material.AIR) return@Runnable
            run {
                player.inventory.all(Material.TRIDENT).forEach { (_, stack) ->
                    if(!(stack.persistentDataContainer.get(offhandKey, PersistentDataType.BOOLEAN) ?: false)) return@forEach
                    stack.editPersistentDataContainer { it.remove(offhandKey) }
                    player.inventory.setItemInOffHand(stack.clone())
                    player.inventory.remove(stack)
                    return@run
                }
            }
        })
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private fun onEntityPortalEnter(event: EntityPortalEnterEvent) {
        if(!disableLoyaltyPortals) return
        val trident = event.entity
        if(trident is Trident && trident.loyaltyLevel > 0) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    private fun onEntityDeath(event: EntityDeathEvent) {
        if(!enableBedrockDropping) return
        val drowned = event.entity
        if(
            drowned !is Drowned ||
            !drowned.world.getGameRuleValue(GameRules.MOB_DROPS) ||
            drowned.equipment.itemInMainHand.type != Material.TRIDENT ||
            event.drops.find { it.type == Material.TRIDENT } != null ||
            random.nextDouble() > EXTRA_DROP_CHANCE
        ) return

        val item = drowned.equipment.itemInMainHand
        item.editMeta { (it as Damageable).damage = random.nextInt(248) + 1 }
        event.drops.add(item)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onProjectileHit(event: ProjectileHitEvent) {
        if(!enableRainLightningRod) return
        val block = event.hitBlock
        val trident = event.entity
        if(
            trident !is Trident ||
            block == null ||
            !Tag.LIGHTNING_RODS.isTagged(block.type) ||
            !(trident.world.hasStorm() && !trident.world.isThundering) ||
            trident.itemStack.enchantments[Enchantment.CHANNELING] == null ||
            block.world.getHighestBlockAt(block.location) != block
        ) return

        trident.world.strikeLightning(block.location.add(0.0, 1.0, 0.0))
        trident.world.playSound(trident.location, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f)
    }
}