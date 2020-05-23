package com.darkender.plugins.tridenttweaks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
                            if(!platforms.containsKey(trident.getUniqueId()))
                            {
                                // Check if the trident is *about* to be in the void (possible to save it by placing blocks)
                                Location futurePos = trident.getLocation().clone().add(trident.getVelocity());
                                if(futurePos.getBlock().getType().isAir() && futurePos.getBlockY() < 5 && futurePos.getBlockY() >= 0)
                                {
                                    futurePos.getBlock().setType(Material.BARRIER);
                                    platforms.put(trident.getUniqueId(), futurePos.getBlock());
                                    // Remove the platform after 1 second (if it still exists)
                                    getServer().getScheduler().scheduleSyncDelayedTask(TridentTweaks.this, new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            if(platforms.containsKey(trident.getUniqueId()))
                                            {
                                                platforms.get(trident.getUniqueId()).setType(Material.AIR);
                                                platforms.remove(trident.getUniqueId());
                                            }
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
    
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        // Remove any trident saving plaforms if the chunk they're in gets unloaded (so barrier blocks don't get saved)
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
            
            // Check if it was thrown from the offhand
            if(enableOffhandReturn &&
                    p.getInventory().getItemInOffHand().getType() == Material.TRIDENT &&
                    p.getInventory().getItemInMainHand().getType() != Material.TRIDENT)
            {
                event.getEntity().setMetadata("offhand", new FixedMetadataValue(this, true));
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
