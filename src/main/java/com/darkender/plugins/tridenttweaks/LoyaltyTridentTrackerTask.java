package com.darkender.plugins.tridenttweaks;

import org.bukkit.entity.Trident;
import org.bukkit.scheduler.BukkitRunnable;

public class LoyaltyTridentTrackerTask extends BukkitRunnable
{
    private Trident trident;
    private ReflectionUtils reflectionUtils;
    
    public LoyaltyTridentTrackerTask(Trident trident, ReflectionUtils reflectionUtils)
    {
        this.trident = trident;
        this.reflectionUtils = reflectionUtils;
    }
    
    @Override
    public void run()
    {
        if(!trident.isValid())
        {
            cancel();
            return;
        }
        
        if(trident.getLocation().getY() < 0)
        {
            reflectionUtils.setDealtDamage(trident, true);
            cancel();
        }
    }
}
