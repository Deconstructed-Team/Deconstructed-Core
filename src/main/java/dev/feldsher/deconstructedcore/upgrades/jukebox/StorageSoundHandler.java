package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The storages' discs this client plays, and telling the server when one has finished. */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID, value = Side.CLIENT)
public final class StorageSoundHandler {
	private static final int SOUND_STOP_CHECK_INTERVAL = 10;
	/** A disc that never got to play, as with records muted, counts as finished after this. */
	private static final int START_TIMEOUT = 100;

	private static final Map<UUID, PlayingSound> storageSounds = new ConcurrentHashMap<>();
	private static long lastPlaybackChecked = 0;

	private StorageSoundHandler() {}

	private static void playStorageSound(UUID storageUuid, ISound sound) {
		stopStorageSound(storageUuid);
		World world = Minecraft.getMinecraft().world;
		storageSounds.put(storageUuid, new PlayingSound(sound, world == null ? 0 : world.getTotalWorldTime()));
		Minecraft.getMinecraft().getSoundHandler().playSound(sound);
	}

	public static void stopStorageSound(UUID storageUuid) {
		PlayingSound playingSound = storageSounds.remove(storageUuid);
		if (playingSound != null) {
			Minecraft.getMinecraft().getSoundHandler().stopSound(playingSound.sound);
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		World world = Minecraft.getMinecraft().world;
		if (event.phase != TickEvent.Phase.END || world == null || storageSounds.isEmpty() || lastPlaybackChecked >= world.getTotalWorldTime() - SOUND_STOP_CHECK_INTERVAL) {
			return;
		}
		long time = world.getTotalWorldTime();
		lastPlaybackChecked = time;
		storageSounds.entrySet().removeIf(entry -> {
			PlayingSound playingSound = entry.getValue();
			if (Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(playingSound.sound)) {
				playingSound.started = true;
				return false;
			}
			// a mob gone from this client's view plays on for those who see it; the server sends it again if it comes back
			if (playingSound.sound instanceof EntityRecordSound && ((EntityRecordSound) playingSound.sound).isEntityOutOfView()) {
				return true;
			}
			if (playingSound.started || time - playingSound.startTime > START_TIMEOUT) {
				PacketHandler.INSTANCE.sendToServer(new SoundFinishedNotificationMessage(entry.getKey()));
				return true;
			}
			return false;
		});
	}

	public static void playStorageSound(ItemStack disc, UUID storageUuid, BlockPos pos) {
		if (disc.getItem() instanceof ItemRecord) {
			playStorageSound(storageUuid, PositionedSoundRecord.getRecordSoundRecord(((ItemRecord) disc.getItem()).getSound(), pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F));
		}
	}

	public static void playStorageSound(ItemStack disc, UUID storageUuid, int entityId) {
		World world = Minecraft.getMinecraft().world;
		if (world == null || !(disc.getItem() instanceof ItemRecord)) {
			return;
		}
		Entity entity = world.getEntityByID(entityId);
		if (entity == null) {
			stopStorageSound(storageUuid);
			return;
		}
		playStorageSound(storageUuid, new EntityRecordSound(((ItemRecord) disc.getItem()).getSound(), entity));
	}

	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload event) {
		if (event.getWorld().isRemote) {
			storageSounds.clear();
			lastPlaybackChecked = 0;
		}
	}

	private static class PlayingSound {
		private final ISound sound;
		private final long startTime;
		private boolean started = false;

		private PlayingSound(ISound sound, long startTime) {
			this.sound = sound;
			this.startTime = startTime;
		}
	}

	/** Follows the entity; for a player it's just in front of their eyes, as they hear it the same whichever way they turn. */
	private static class EntityRecordSound extends MovingSound {
		private final Entity entity;

		private EntityRecordSound(SoundEvent soundEvent, Entity entity) {
			super(soundEvent, SoundCategory.RECORDS);
			this.entity = entity;
			volume = 2.0F;
			attenuationType = ISound.AttenuationType.LINEAR;
			update();
		}

		/** Gone from this client but for a player, whose death ends the disc as its end does. */
		private boolean isEntityOutOfView() {
			return entity.isDead && !(entity instanceof EntityPlayer);
		}

		@Override
		public void update() {
			if (entity.isDead) {
				donePlaying = true;
				return;
			}
			if (entity instanceof EntityPlayer) {
				Vec3d look = entity.getLookVec();
				xPosF = (float) (entity.posX + look.x);
				yPosF = (float) (entity.posY + entity.getEyeHeight() + look.y);
				zPosF = (float) (entity.posZ + look.z);
			} else {
				xPosF = (float) entity.posX;
				yPosF = (float) entity.posY;
				zPosF = (float) entity.posZ;
			}
		}
	}
}
