package com.darkender.plugins.tridenttweaks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class TridentTweaks extends JavaPlugin implements Listener
{
    private boolean enableOffhandReturn = true;
    private boolean enableVoidSaving = true;
    private boolean disableLoyaltyPortals = true;
    private boolean enableBedrockImpaling = true;
    private boolean enableBedrockDropping = true;
    
    private ReflectionUtils reflectionUtils;
    
    private final Random random = new Random();
    
    @Override
    public void onEnable()
    {
        TridentTweaksCommand tridentTweaksCommand = new TridentTweaksCommand(this);
        getCommand("tridenttweaks").setExecutor(tridentTweaksCommand);
        getCommand("tridenttweaks").setTabCompleter(tridentTweaksCommand);
        
        getServer().getPluginManager().registerEvents(this, this);
        reload();
    }
    
    public void reload()
    {
        saveDefaultConfig();
        reloadConfig();
        
        enableBedrockImpaling = getConfig().getBoolean("enable-bedrock-impaling");
        enableVoidSaving = getConfig().getBoolean("enable-void-saving");
        enableOffhandReturn = getConfig().getBoolean("enable-offhand-return");
        disableLoyaltyPortals = getConfig().getBoolean("disable-loyalty-portals");
        enableBedrockDropping = getConfig().getBoolean("enable-bedrock-dropping");
        if(enableVoidSaving)
        {
            reflectionUtils = new ReflectionUtils();
            if(!reflectionUtils.isReady())
            {
                getLogger().severe("Reflection failed, disabling void saving!");
                enableVoidSaving = false;
            }
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
    
    private boolean isAquatic(EntityType type)
    {
        // I hate this as much as you do - Spigot API should really implement a better way to do this..
        return (type == EntityType.DOLPHIN || type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN ||
                type == EntityType.SQUID || type == EntityType.TURTLE || type == EntityType.COD ||
                type == EntityType.SALMON || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH);
    }
    
    private void displayEnchantedHit(Entity entity)
    {
        for(int i = 0; i < 16*3; i++)
        {
            double d = (this.random.nextFloat() * 2.0F - 1.0F);
            double e = (this.random.nextFloat() * 2.0F - 1.0F);
            double f = (this.random.nextFloat() * 2.0F - 1.0F);
            if(d * d + e * e + f * f <= 1.0D)
            {
                Location loc = entity.getLocation().clone();
                loc.add((entity.getWidth() * (d / 4.0D)),
                        (entity.getHeight() * (0.5D + e / 4.0D)),
                        (entity.getWidth() * (f / 4.0D)));
                loc.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc, 0, d, e + 0.2D, f);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event)
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
                displayEnchantedHit(e);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onEntityPortal(EntityPortalEvent event)
    {
        // Doesn't work with end gateways: https://hub.spigotmc.org/jira/browse/SPIGOT-3838
        if(disableLoyaltyPortals && event.getEntity().hasMetadata("loyalty"))
        {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onProjectileLaunch(ProjectileLaunchEvent event)
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
                    
                    if(enableVoidSaving)
                    {
                        LoyaltyTridentTrackerTask trackerTask = new LoyaltyTridentTrackerTask((Trident) event.getEntity(), reflectionUtils);
                        trackerTask.runTaskTimer(this, 0, 1);
                    }
                }
            }
            
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void onPlayerPickupArrow(PlayerPickupArrowEvent event)
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
            getServer().getScheduler().scheduleSyncDelayedTask(this, () ->
            {
                // Double-check to ensure offhand item is still empty
                if(event.getPlayer().getInventory().getItemInOffHand().getType() != Material.AIR)
                {
                    return;
                }
                
                // Start from end of inventory to get the most recently added trident in case duplicates exist
                ItemStack[] contents = p.getInventory().getContents();
                for(int i = contents.length - 1; i >= 0; i--)
                {
                    ItemStack current = contents[i];
                    if(current != null && current.equals(item))
                    {
                        // If we find the trident and the offhand is clear, put it in the offhand
                        p.getInventory().setItemInOffHand(current.clone());
                        current.setAmount(current.getAmount() - 1);
                        break;
                    }
                }
                p.updateInventory();
            });
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    private void DeathEvent(EntityDeathEvent event)
    {
        if(!enableBedrockDropping)
        {
            return;
        }
        if(event.getEntity() instanceof Drowned)
        {
            Drowned drowned = (Drowned) event.getEntity();
            if(!drowned.getWorld().getGameRuleValue(GameRule.DO_MOB_LOOT) ||
                    drowned.getEquipment().getItemInMainHand().getType() == Material.TRIDENT)
            {
                return;
            }
            int num = random.nextInt(100);
            if(num <= 8)
            {
                ItemStack trident = new ItemStack(Material.TRIDENT);
                Damageable meta = (Damageable) trident.getItemMeta();
                meta.setDamage(random.nextInt(248) + 1);
                trident.setItemMeta((ItemMeta) meta);
                event.getDrops().add(trident);
            }
        }
    }
}
