package com.michaelelin.NerdFlags;

import java.util.Set;

import org.bukkit.Bukkit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.session.MoveType;
import com.sk89q.worldguard.session.Session;
import com.sk89q.worldguard.session.handler.Handler;

public class Entry extends Handler {
	public static final Factory FACTORY = new Factory();

	public static class Factory extends Handler.Factory<Entry> {
		@Override
		public Entry create(Session session) {
			return new Entry(session);
		}
	}

	protected Entry(Session session) {
		super(session);
	}

	@Override
	public boolean onCrossBoundary(LocalPlayer player, Location from, Location to, ApplicableRegionSet toSet,
			Set<ProtectedRegion> entered, Set<ProtectedRegion> exited, MoveType moveType) {
		
		for(ProtectedRegion exitedRegion : exited) {
			PlayerLeaveRegionEvent e = new PlayerLeaveRegionEvent(BukkitAdapter.adapt(player), exitedRegion);
			
			Bukkit.getPluginManager().callEvent(e);
		}
		
		for(ProtectedRegion enteredRegion : entered) {
			PlayerEnterRegionEvent e = new PlayerEnterRegionEvent(BukkitAdapter.adapt(player), enteredRegion);
			
			Bukkit.getPluginManager().callEvent(e);
		}
		
		return true;
	}
}
