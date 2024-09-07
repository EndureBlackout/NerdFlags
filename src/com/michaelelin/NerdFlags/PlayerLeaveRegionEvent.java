package com.michaelelin.NerdFlags;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class PlayerLeaveRegionEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	private Player player;
	private ProtectedRegion region;
	private boolean cancelled;
	
	public PlayerLeaveRegionEvent(Player p, ProtectedRegion region) {
		this.player = p;
		this.region = region;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public ProtectedRegion getRegion() {
		return region;
	}
	
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
}
