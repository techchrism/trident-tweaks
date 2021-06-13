package com.darkender.plugins.tridenttweaks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Trident;

import java.lang.reflect.Field;

public class ReflectionUtils
{
    private Field dealtDamageField = null;
    
    public ReflectionUtils()
    {
        try
        {
            Class<?> thrownTridentClass;
            try
            {
                thrownTridentClass = getNmsClass("EntityThrownTrident");
            }
            catch(Exception ignored)
            {
                // 1.17+ Mojang mappings
                thrownTridentClass = Class.forName("net.minecraft.world.entity.projectile.EntityThrownTrident");
            }
            
            int count = 0;
            for(Field field : thrownTridentClass.getDeclaredFields())
            {
                if(field.getType() == boolean.class)
                {
                    dealtDamageField = field;
                    dealtDamageField.setAccessible(true);
                    count++;
                }
            }
            
            // Ensure this is the only boolean field
            if(count != 1)
            {
                Bukkit.getLogger().severe("Found more than one boolean trident field!");
                dealtDamageField = null;
            }
        }
        catch(Exception e)
        {
            Bukkit.getLogger().severe("Error while loading trident reflection:");
            e.printStackTrace();
        }
    }
    
    public boolean isReady()
    {
        return (dealtDamageField != null);
    }
    
    public void setDealtDamage(Trident trident, boolean value)
    {
        try
        {
            Object entityTrident = trident.getClass().getMethod("getHandle").invoke(trident);
            dealtDamageField.set(entityTrident, value);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public boolean hasDealtDamage(Trident trident)
    {
        try
        {
            Object entityTrident = trident.getClass().getMethod("getHandle").invoke(trident);
            return (boolean) dealtDamageField.get(entityTrident);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    private Class<?> getNmsClass(String nmsClassName) throws ClassNotFoundException
    {
        return Class.forName("net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + "." + nmsClassName);
    }
}
