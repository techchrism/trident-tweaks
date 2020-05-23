package com.darkender.plugins.tridenttweaks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TridentTweaks extends JavaPlugin implements Listener
{
    //TODO make these config options
    private boolean enableOffhandReturn = true;
    private boolean enableVoidSaving = true;
    private boolean disableLoyaltyPortals = true;
    private boolean enableBedrockImpaling = true;
    
    private BukkitTask voidSavingTask = null;
    private HashMap<UUID, Block> platforms;
    
    @Override
    public void onEnable()
    {
        platforms = new HashMap<>();
        
        getServer().getPluginManager().registerEvents(this, this);
        setupVoidSaving();
    }
    
    private void setupVoidSaving()
    {
        if(enableVoidSaving && voidSavingTask == null)
        {
            voidSavingTask = (new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    for(World w : getServer().getWorlds())
                    {
                        for(Trident trident : w.getEntitiesByClass(Trident.class))
                        {
                            if(trident.hasMetadata("loyalty") && !platforms.containsKey(trident.getUniqueId()))
                            {
                                // Check if the trident is *about* to be in the void (possible to save it by placing blocks)
                                Location futurePos = trident.getLocation().clone().add(trident.getVelocity());
                                if(futurePos.getBlock().getType().isAir() && futurePos.getBlockY() < 5 && futurePos.getBlockY() >= 0)
                                {
                                    futurePos.getBlock().setType(Material.BARRIER);
                                    platforms.put(trident.getUniqueId(), futurePos.getBlock());
                                    // Remove the platform after 1 second (if it still exists)
                                    getServer().getScheduler().scheduleSyncDelayedTask(TridentTweaks.this, () ->
                                    {
                                        if(platforms.containsKey(trident.getUniqueId()))
                                        {
                                            platforms.get(trident.getUniqueId()).setType(Material.AIR);
                                            platforms.remove(trident.getUniqueId());
                                        }
                                    }, 20L);
                                }
                                else if(futurePos.getY() < 0)
                                {
                                    // If it's already in the void, shoot it upwards
                                    trident.setVelocity(new Vector(0, 1, 0));
                                }
                            }
                        }
                    }
                }
            }).runTaskTimer(this, 1L, 1L);
        }
        else if(!enableVoidSaving && voidSavingTask != null)
        {
            voidSavingTask.cancel();
            voidSavingTask = null;
        }
    }
    
    private boolean canSeeSky(Entity e)
    {
        Location min = e.getBoundingBox().getMin().toLocation(e.getWorld());
        Location max = e.getBoundingBox().getMax().toLocation(e.getWorld());
        int x = min.getBlockX();
        int z = min.getBlockZ();
        do
        {
            do
            {
                try
                {
                    Block highest = e.getLocation().getWorld().getHighestBlockAt(e.getLocation());
                    if(highest.getY() > e.getLocation().getBlockY())
                    {
                        return false;
                    }
                }
                catch(Exception exception)
                {
                    return true;
                }
                z++;
            }
            while(z <= max.getBlockZ());
            x++;
        }
        while(x <= max.getBlockX());
        return true;
    }
    
    private boolean isInWater(Entity e)
    {
        Location min = e.getBoundingBox().getMin().toLocation(e.getWorld());
        Location max = e.getBoundingBox().getMax().toLocation(e.getWorld());
        int x = min.getBlockX();
        int y = min.getBlockY();
        int z = min.getBlockZ();
        do
        {
            do
            {
                do
                {
                    if(e.getWorld().getBlockAt(x, y, z).getType() == Material.WATER)
                    {
                        return true;
                    }
                    
                    y++;
                }
                while(y <= max.getBlockY());
                z++;
            }
            while(z <= max.getBlockZ());
            x++;
        }
        while(x <= max.getBlockX());
        return false;
    }
    
    public boolean isAquatic(EntityType type)
    {
        return (type == EntityType.DOLPHIN || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN ||
                type == EntityType.SQUID || type == EntityType.TURTLE || type == EntityType.COD ||
                type == EntityType.SALMON || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH);
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if(!enableBedrockImpaling)
        {
            return;
        }
    
        Entity e = event.getEntity();
        int impaling = 0;
        if(event.getDamager().getType() == EntityType.TRIDENT && event.getDamager().hasMetadata("impaling"))
        {
            impaling = event.getDamager().getMetadata("impaling").get(0).asInt();
        }
        else if(event.getDamager().getType() == EntityType.PLAYER)
        {
            impaling = ((Player) event.getDamager()).getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.IMPALING);
        }
        
        if(impaling > 0)
        {
            if(!isAquatic(e.getType()) && ((e.getWorld().hasStorm() && canSeeSky(e)) || isInWater(e)))
            {
                event.setDamage(event.getDamage() + (2.5 * impaling));
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event)
    {
        // Doesn't work with end gateways: https://hub.spigotmc.org/jira/browse/SPIGOT-3838
        if(disableLoyaltyPortals && event.getEntity().hasMetadata("loyalty"))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        // Remove any trident saving platforms if the chunk they're in gets unloaded (so barrier blocks don't get saved)
        Iterator<Map.Entry<UUID, Block>> iterator = platforms.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<UUID, Block> entry = iterator.next();
            if(entry.getValue().getChunk().equals(event.getChunk()))
            {
                entry.getValue().setType(Material.AIR);
                iterator.remove();
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event)
    {
        if(event.getEntityType() == EntityType.TRIDENT && (event.getEntity().getShooter() instanceof Player))
        {
            Player p = (Player) event.getEntity().getShooter();
            ItemStack tridentItem = null;
            boolean fromOffhand = false;
            // If the player is holding two tridents, only the one from the main hand will be thrown
            // (Unless the player starts charging in the offhand then equips a trident in the main hand)
            if(p.getInventory().getItemInMainHand().getType() == Material.TRIDENT)
            {
                tridentItem = p.getInventory().getItemInMainHand();
            }
            else if(p.getInventory().getItemInOffHand().getType() == Material.TRIDENT)
            {
                fromOffhand = true;
                tridentItem = p.getInventory().getItemInOffHand();
            }
            
            // The trident could be thrown "artificially"
            if(tridentItem != null)
            {
                // Check if it was thrown from the offhand
                if(enableOffhandReturn && fromOffhand)
                {
                    event.getEntity().setMetadata("offhand", new FixedMetadataValue(this, true));
                }
                
                if(enableBedrockImpaling && tridentItem.getEnchantmentLevel(Enchantment.IMPALING) != 0)
                {
                    event.getEntity().setMetadata("impaling", new FixedMetadataValue(this,
                            tridentItem.getEnchantmentLevel(Enchantment.IMPALING)));
                }
                
                if(tridentItem.getEnchantmentLevel(Enchantment.LOYALTY) != 0)
                {
                    event.getEntity().setMetadata("loyalty", new FixedMetadataValue(this,
                            tridentItem.getEnchantmentLevel(Enchantment.LOYALTY)));
                }
            }
            
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event)
    {
        // Check if this is a trident that should be in the offhand
        if(enableOffhandReturn &&
               event.getArrow() instanceof Trident &&
               event.getArrow().hasMetadata("offhand") &&
               event.getPlayer().getInventory().getItemInOffHand().getType() == Material.AIR)
        {
            // The itemstack gets modified after the event so it must be cloned for future comparison
            ItemStack item = event.getItem().getItemStack().clone();
            Player p = event.getPlayer();
            
            // The item isn't in the inventory yet so schedule a checker
            getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
            {
                @Override
                public void run()
                {
                    for(ItemStack i : p.getInventory())
                    {
                        if(i != null && i.equals(item))
                        {
                            // If we find the trident, put it in the offhand
                            p.getInventory().remove(i);
                            p.getInventory().setItemInOffHand(i.clone());
                            break;
                        }
                    }
                    p.updateInventory();
                }
            });
        }
    }
}
