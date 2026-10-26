package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The discs storages play, by dimension: a storage that stops telling it's still there, as one put away in a chest,
 * has its disc stopped for the players near where it last was. A player who comes to see an entity playing one,
 * as a mob first in their view, is sent its disc then.
 */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID)
public final class ServerStorageSoundHandler {
	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final int SOUND_RANGE = 128;
	private static final Map<Integer, Long> lastWorldCheck = new HashMap<>();
	private static final Map<Integer, Map<UUID, KeepAliveInfo>> worldStorageSoundKeepAlive = new HashMap<>();

	private ServerStorageSoundHandler() {}

	@SubscribeEvent
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)) {
			return;
		}
		World world = event.world;
		int dim = world.provider.getDimension();
		long time = world.getTotalWorldTime();
		if (lastWorldCheck.computeIfAbsent(dim, key -> time) > time - KEEP_ALIVE_CHECK_INTERVAL || !worldStorageSoundKeepAlive.containsKey(dim)) {
			return;
		}
		lastWorldCheck.put(dim, time);

		worldStorageSoundKeepAlive.get(dim).entrySet().removeIf(entry -> {
			if (entry.getValue().getLastKeepAliveTime() < time - KEEP_ALIVE_CHECK_INTERVAL) {
				sendStopMessage(world, entry.getValue().getLastPosition(), entry.getKey());
				return true;
			}
			return false;
		});
	}

	public static void updateKeepAlive(UUID storageUuid, World world, Vec3d position, Runnable onNoLongerRunning) {
		int dim = world.provider.getDimension();
		if (!worldStorageSoundKeepAlive.containsKey(dim) || !worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		worldStorageSoundKeepAlive.get(dim).get(storageUuid).update(world.getTotalWorldTime(), position);
	}

	public static void onSoundFinished(World world, UUID storageUuid) {
		removeKeepAliveInfo(world, storageUuid, true);
	}

	public static void startPlayingDisc(WorldServer world, BlockPos position, UUID storageUuid, ItemStack disc, Runnable onFinishedHandler) {
		Vec3d pos = new Vec3d(position).add(0.5, 0.5, 0.5);
		sendNear(world, pos, new PlayDiscMessage(storageUuid, disc, position));
		putKeepAliveInfo(world, storageUuid, onFinishedHandler, pos, disc, -1);
	}

	public static void startPlayingDisc(WorldServer world, Vec3d position, UUID storageUuid, int entityId, ItemStack disc, Runnable onFinishedHandler) {
		sendNear(world, position, new PlayDiscMessage(storageUuid, disc, entityId));
		putKeepAliveInfo(world, storageUuid, onFinishedHandler, position, disc, entityId);
	}

	private static void putKeepAliveInfo(World world, UUID storageUuid, Runnable onFinishedHandler, Vec3d pos, ItemStack disc, int entityId) {
		worldStorageSoundKeepAlive.computeIfAbsent(world.provider.getDimension(), dim -> new HashMap<>())
				.put(storageUuid, new KeepAliveInfo(onFinishedHandler, world.getTotalWorldTime(), pos, disc, entityId));
	}

	/**
	 * The players near an entity as it started playing knew it only if they saw it then; one that sees it later, as a
	 * mob that spawned far away, gets its disc now, from the start.
	 */
	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		Entity target = event.getTarget();
		if (!(target.world instanceof WorldServer) || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
			return;
		}
		Map<UUID, KeepAliveInfo> keepAlive = worldStorageSoundKeepAlive.get(target.world.provider.getDimension());
		if (keepAlive == null) {
			return;
		}
		keepAlive.forEach((storageUuid, info) -> {
			if (info.entityId == target.getEntityId()) {
				PacketHandler.INSTANCE.sendTo(new PlayDiscMessage(storageUuid, info.disc, info.entityId), (EntityPlayerMP) event.getEntityPlayer());
			}
		});
	}

	public static void stopPlayingDisc(World world, Vec3d position, UUID storageUuid) {
		removeKeepAliveInfo(world, storageUuid, false);
		sendStopMessage(world, position, storageUuid);
	}

	private static void removeKeepAliveInfo(World world, UUID storageUuid, boolean finished) {
		Map<UUID, KeepAliveInfo> keepAlive = worldStorageSoundKeepAlive.get(world.provider.getDimension());
		if (keepAlive != null && keepAlive.containsKey(storageUuid)) {
			KeepAliveInfo keepAliveInfo = keepAlive.remove(storageUuid);
			if (finished) {
				keepAliveInfo.runOnFinished();
			}
		}
	}

	private static void sendStopMessage(World world, Vec3d position, UUID storageUuid) {
		if (world instanceof WorldServer) {
			sendNear(world, position, new StopDiscPlaybackMessage(storageUuid));
		}
	}

	private static void sendNear(World world, Vec3d pos, IMessage message) {
		PacketHandler.INSTANCE.sendToAllAround(message, new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.x, pos.y, pos.z, SOUND_RANGE));
	}

	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload event) {
		if (!(event.getWorld() instanceof WorldServer)) {
			return;
		}
		int dim = event.getWorld().provider.getDimension();
		worldStorageSoundKeepAlive.remove(dim);
		lastWorldCheck.remove(dim);
	}

	private static class KeepAliveInfo {
		private final WeakReference<Runnable> onFinishedHandler;
		private long lastKeepAliveTime;
		private Vec3d lastPosition;
		private final ItemStack disc;
		/** The entity it plays from, -1 when it plays from a block. */
		private final int entityId;

		private KeepAliveInfo(Runnable onFinishedHandler, long lastKeepAliveTime, Vec3d lastPosition, ItemStack disc, int entityId) {
			this.onFinishedHandler = new WeakReference<>(onFinishedHandler);
			this.lastKeepAliveTime = lastKeepAliveTime;
			this.lastPosition = lastPosition;
			this.disc = disc.copy();
			this.entityId = entityId;
		}

		private long getLastKeepAliveTime() {
			return lastKeepAliveTime;
		}

		private Vec3d getLastPosition() {
			return lastPosition;
		}

		private void update(long gameTime, Vec3d position) {
			lastKeepAliveTime = gameTime;
			lastPosition = position;
		}

		private void runOnFinished() {
			Runnable handler = onFinishedHandler.get();
			if (handler != null) {
				handler.run();
			}
		}
	}
}
