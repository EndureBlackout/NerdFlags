package com.michaelelin.NerdFlags;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.sk89q.worldguard.protection.flags.StateFlag;

public class NerdFlagsRegionListener implements Listener {
    private static ScoreboardManager sbm = null;
    private static Scoreboard sb = null;
    private static Team collisionTeam = null;

    private NerdFlagsPlugin plugin;

    NerdFlagsRegionListener(NerdFlagsPlugin plugin) {
        this.plugin = plugin;

        sbm = Bukkit.getScoreboardManager();
        sb = sbm.getMainScoreboard();

        if(sb.getTeam("NoCollision") == null) {
            collisionTeam = sb.registerNewTeam("NoCollision");
        } else {
            collisionTeam = sb.getTeam("NoCollision");
        }

        collisionTeam.setOption(Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerEnteredRegion(PlayerEnterRegionEvent event) {
        Player player = event.getPlayer();
        StateFlag.State weatherState = event.getRegion().getFlag(plugin.WEATHER);
        StateFlag.State separateInvState = event.getRegion().getFlag(plugin.REGION_SEPARATE_INVENTORY);
        StateFlag.State collisionState = event.getRegion().getFlag(plugin.DISABLE_COLLISION);
        
        String regionName = event.getRegion().getId();
        
        if (weatherState == StateFlag.State.ALLOW) {
            setWeather(player, true);
        }

        if(collisionState == StateFlag.State.ALLOW) {
            collisionTeam.addEntry(player.getName());
            player.setCollidable(false);
        }
        
        if(separateInvState == StateFlag.State.ALLOW && (!player.isOp() && !player.hasPermission("nerdflags.admin"))) {
        	PlayerInventory playerInventory = player.getInventory();
        	
        	plugin.PLAYER_INVENTORY.put(regionName + "." + player.getUniqueId() + ".contents", playerInventory.getContents());
        	plugin.PLAYER_INVENTORY.put(regionName + "." + player.getUniqueId() + ".armor", playerInventory.getArmorContents());
        	
        	ItemStack[] emptyInv = {};
        	
        	player.getInventory().setContents(emptyInv);
        	player.updateInventory();
        	
        	player.sendMessage(ChatColor.GREEN + "Your inventory will be given back to you when you leave this region.");
        }

        String entryCommands = event.getRegion().getFlag(plugin.ENTRY_COMMANDS);
        if (entryCommands != null) {
            for (String command : parseCommands(entryCommands)) {
                plugin.getServer().dispatchCommand(player, command);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeftRegion(PlayerLeaveRegionEvent event) {
        Player player = event.getPlayer();
        StateFlag.State weatherState = event.getRegion().getFlag(plugin.WEATHER);
        StateFlag.State separateInvState = event.getRegion().getFlag(plugin.REGION_SEPARATE_INVENTORY);
        StateFlag.State collisionState = event.getRegion().getFlag(plugin.DISABLE_COLLISION);
        
        String regionName = event.getRegion().getId();
        
        boolean storming = player.getWorld().hasStorm();
        if (weatherState == StateFlag.State.ALLOW && !storming) {
            setWeather(player, false);
        }

        if(collisionState == StateFlag.State.ALLOW) {
            collisionTeam.removeEntry(player.getName());
            player.setCollidable(true);
        }
        
        if(separateInvState == StateFlag.State.ALLOW) {
        	if(plugin.PLAYER_INVENTORY.containsKey(regionName + "." + player.getUniqueId() + ".contents")) {
        		ItemStack[] regionContents = plugin.PLAYER_INVENTORY.get(regionName + "." + player.getUniqueId() + ".contents");
        		
        		player.getInventory().setContents(regionContents);
        		player.updateInventory();
        		
        		plugin.PLAYER_INVENTORY.remove(regionName + "." + player.getUniqueId() + ".contents");
        	}
        	
        	if(plugin.PLAYER_INVENTORY.containsKey(regionName + "." + player.getUniqueId() + ".armor")) {
        		ItemStack[] regionArmor = plugin.PLAYER_INVENTORY.get(regionName + "." + player.getUniqueId() + ".armor");
        		
        		player.getInventory().setArmorContents(regionArmor);
        		player.updateInventory();
        		
        		plugin.PLAYER_INVENTORY.remove(regionName + "." + player.getUniqueId() + ".armor");
        	}
        }
    }

    private void setWeather(Player player, boolean weather) {
        PacketContainer weatherPacket = plugin.protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
        weatherPacket.getIntegers().write(0, weather ? 2 : 1);
        weatherPacket.getFloat().write(0, 0F);
        try {
            plugin.protocolManager.sendServerPacket(player, weatherPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseCommands(String commands) {
        List<String> commandList = new LinkedList<>();
        StringBuilder curr = new StringBuilder();
        boolean escape = false;
        for (char c : commands.toCharArray()) {
            if (c == '|' && !escape) {
                commandList.add(curr.toString());
                curr = new StringBuilder();
                escape = false;
            } else if (c == '\\') {
                if (escape) {
                    curr.append('\\');
                }
                escape = !escape;
            } else {
                curr.append(c);
                escape = false;
            }
        }
        commandList.add(curr.toString());
        return commandList;
    }

}
