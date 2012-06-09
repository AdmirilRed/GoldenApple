package com.bendude56.goldenapple.area;

import org.bukkit.Location;

public class SafetyArea extends ParentArea {
	boolean allowPvp;
	boolean hostileMobs;
	boolean regenHealth;
	
	public SafetyArea(Location corner1, Location corner2, boolean ignoreY) {
		this.setCorner1(corner1);
		this.setCorner2(corner2);
		this.ignoreY(ignoreY);
	}
	
	public boolean allowPvp() {
		return allowPvp; 
	}
	
	public void allowPvp(boolean allowPvp) {
		this.allowPvp = allowPvp;
	}
	
	public boolean hostileMobs() {
		return hostileMobs;
	}
	
	public void hostileMobs(boolean hostileMobs) {
		this.hostileMobs = hostileMobs;
	}
	
	public boolean regenHealth() {
		return regenHealth;
	}
	
	public void regenHealth(boolean regenHealth) {
		this.regenHealth = regenHealth;
	}
}