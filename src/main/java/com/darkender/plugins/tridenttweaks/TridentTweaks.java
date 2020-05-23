package com.darkender.plugins.tridenttweaks;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class TridentTweaks extends JavaPlugin implements Listener
{
    //TODO make this a config option
    private boolean enableOffhandReturn = true;
    
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler
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
                        // If we find the trident, put it in the offhand
                        if(i != null && i.equals(item))
                        {
                            p.getInventory().setItemInOffHand(i.clone());
                            i.setAmount(0);
                            p.updateInventory();
                            break;
                        }
                    }
                }
            });
        }
    }
}
