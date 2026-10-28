package dev.feldsher.deconstructedcore.util;

import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How often the server answers a player's request about one thing, as the contents of a storage item: what a client
 * asks for on its own, it asks for seldom, and more is only a way to have the server send the same again and again.
 */
public class RequestThrottle {
	private static final int PRUNE_SIZE = 1024;
	private final long intervalMs;
	private final Map<String, Long> lastAnswered = new HashMap<>();

	public RequestThrottle(long intervalMs) {
		this.intervalMs = intervalMs;
	}

	/** Whether to answer now; if so, the next one about the same thing waits the interval out. */
	public synchronized boolean tryAnswer(EntityPlayer player, UUID about) {
		long now = System.currentTimeMillis();
		if (lastAnswered.size() > PRUNE_SIZE) {
			lastAnswered.values().removeIf(time -> now - time >= intervalMs);
		}
		String key = player.getUniqueID() + "/" + about;
		Long last = lastAnswered.get(key);
		if (last != null && now - last < intervalMs) {
			return false;
		}
		lastAnswered.put(key, now);
		return true;
	}
}
