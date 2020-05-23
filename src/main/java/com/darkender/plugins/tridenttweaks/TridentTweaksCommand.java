package com.darkender.plugins.tridenttweaks;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class TridentTweaksCommand implements CommandExecutor, TabCompleter
{
    private final List<String> empty = Collections.singletonList("");
    private final List<String> reload = Collections.singletonList("reload");
    private final TridentTweaks tridentTweaks;
    
    public TridentTweaksCommand(TridentTweaks tridentTweaks)
    {
        this.tridentTweaks = tridentTweaks;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage(ChatColor.GOLD + tridentTweaks.getName() + ChatColor.BLUE + " v" + tridentTweaks.getDescription().getVersion());
            return true;
        }
        else if(args[0].equals("reload"))
        {
            if(sender.hasPermission("tridenttweaks.reload"))
            {
                tridentTweaks.reload();
                sender.sendMessage(ChatColor.GREEN + "Reloaded!");
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            }
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 2)
        {
            return reload;
        }
        return empty;
    }
}
