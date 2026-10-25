package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Plays the discs it holds, one after another, from where the storage is: the clients near it play the sound, and
 * say when it has finished, while the storage keeps telling the server it's still there.
 */
public class JukeboxUpgradeWrapper extends UpgradeWrapperBase<JukeboxUpgradeWrapper, JukeboxUpgradeItem> implements ITickableUpgrade {
	private static final int KEEP_ALIVE_SEND_INTERVAL = 5;
	private static final String DISC_INVENTORY_TAG = "discInventory";
	private static final String IS_PLAYING_TAG = "isPlaying";
	private static final String SHUFFLE_TAG = "shuffle";
	private static final String REPEAT_MODE_TAG = "repeatMode";
	private static final String DISC_SLOT_ACTIVE_TAG = "discSlotActive";
	private static final String DISC_FINISH_TIME_TAG = "discFinishTime";

	private final ItemStackHandler discInventory;
	private final boolean[] slotsWithDisc;
	private long lastKeepAliveSendTime = 0;
	private boolean isPlaying;

	private final LinkedList<Integer> playlist = new LinkedList<>();
	private final LinkedList<Integer> history = new LinkedList<>();

	private final Set<Integer> discsRemoved = new HashSet<>();
	private final Set<Integer> discsAdded = new HashSet<>();

	@Nullable
	private Entity entityPlaying = null;
	@Nullable
	private World worldPlaying = null;
	@Nullable
	private BlockPos posPlaying = null;

	private final Runnable onFinishedCallback = this::onDiscFinished;

	public JukeboxUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		discInventory = new ItemStackHandler(upgradeItem.getNumberOfSlots()) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				StorageNbt.getOrCreateTag(upgrade).setTag(DISC_INVENTORY_TAG, serializeNBT());
				save();
				boolean hasDisc = !getStackInSlot(slot).isEmpty();
				if (!slotsWithDisc[slot] && hasDisc) {
					discsAdded.add(slot);
					discsRemoved.remove(slot);
				} else if (slotsWithDisc[slot] && !hasDisc) {
					discsRemoved.add(slot);
					discsAdded.remove(slot);
				}
				slotsWithDisc[slot] = hasDisc;
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return stack.isEmpty() || stack.getItem() instanceof ItemRecord;
			}

			@Nonnull
			@Override
			public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
				return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
			}
		};
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null && tag.hasKey(DISC_INVENTORY_TAG)) {
			NBTTagCompound inventoryTag = tag.getCompoundTag(DISC_INVENTORY_TAG).copy();
			// The slot count follows the config, not what was saved
			inventoryTag.removeTag("Size");
			discInventory.deserializeNBT(inventoryTag);
		}
		slotsWithDisc = new boolean[discInventory.getSlots()];
		for (int slot = 0; slot < slotsWithDisc.length; slot++) {
			slotsWithDisc[slot] = !discInventory.getStackInSlot(slot).isEmpty();
		}
		isPlaying = getTag().getBoolean(IS_PLAYING_TAG);
	}

	private NBTTagCompound getTag() {
		NBTTagCompound tag = upgrade.getTagCompound();
		return tag == null ? new NBTTagCompound() : tag;
	}

	public boolean isShuffleEnabled() {
		return getTag().getBoolean(SHUFFLE_TAG);
	}

	public void setShuffleEnabled(boolean shuffleEnabled) {
		StorageNbt.getOrCreateTag(upgrade).setBoolean(SHUFFLE_TAG, shuffleEnabled);
		save();
		initPlaylist(true);
	}

	public RepeatMode getRepeatMode() {
		return RepeatMode.fromName(getTag().getString(REPEAT_MODE_TAG));
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		StorageNbt.getOrCreateTag(upgrade).setString(REPEAT_MODE_TAG, repeatMode.getName());
		save();
	}

	public ItemStack getDisc() {
		return getDiscSlotActive() > -1 ? discInventory.getStackInSlot(getDiscSlotActive()) : ItemStack.EMPTY;
	}

	public int getDiscSlotActive() {
		NBTTagCompound tag = getTag();
		return tag.hasKey(DISC_SLOT_ACTIVE_TAG) ? tag.getInteger(DISC_SLOT_ACTIVE_TAG) : -1;
	}

	private void setDiscSlotActive(int discSlotActive) {
		StorageNbt.getOrCreateTag(upgrade).setInteger(DISC_SLOT_ACTIVE_TAG, discSlotActive);
		save();
	}

	public void play(World world, BlockPos pos) {
		if (isPlaying) {
			return;
		}
		worldPlaying = world;
		posPlaying = pos;
		playNext();
	}

	public void play(Entity entity) {
		if (isPlaying) {
			return;
		}
		entityPlaying = entity;
		playNext();
	}

	private void playDisc() {
		World world = entityPlaying != null ? entityPlaying.world : worldPlaying;
		if (!(world instanceof WorldServer) || (posPlaying == null && entityPlaying == null)) {
			return;
		}
		ItemStack disc = getDisc();
		if (disc.isEmpty()) {
			return;
		}

		storageWrapper.getContentsUuid().ifPresent(storageUuid -> {
			if (entityPlaying != null) {
				ServerStorageSoundHandler.startPlayingDisc((WorldServer) world, entityPlaying.getPositionVector(), storageUuid, entityPlaying.getEntityId(), disc, onFinishedCallback);
			} else {
				ServerStorageSoundHandler.startPlayingDisc((WorldServer) world, posPlaying, storageUuid, disc, onFinishedCallback);
			}
			StorageNbt.getOrCreateTag(upgrade).setLong(DISC_FINISH_TIME_TAG, world.getTotalWorldTime() + DiscLengths.getLengthInTicks(disc));
		});
		setIsPlaying(true);
	}

	private void onDiscFinished() {
		if (getRepeatMode() == RepeatMode.ONE) {
			playDisc();
		} else if (getRepeatMode() == RepeatMode.ALL) {
			playNext();
		} else {
			playNext(false);
		}
	}

	private void setIsPlaying(boolean playing) {
		isPlaying = playing;
		StorageNbt.getOrCreateTag(upgrade).setBoolean(IS_PLAYING_TAG, playing);
		if (!isPlaying) {
			setDiscSlotActive(-1);
		}
		save();
	}

	public void stop(Entity entity) {
		if (!(entity.world instanceof WorldServer)) {
			return;
		}
		storageWrapper.getContentsUuid().ifPresent(storageUuid ->
				ServerStorageSoundHandler.stopPlayingDisc(entity.world, entity.getPositionVector(), storageUuid));
		setIsPlaying(false);
		StorageNbt.getOrCreateTag(upgrade).removeTag(DISC_FINISH_TIME_TAG);
		setDiscSlotActive(-1);
		playlist.clear();
		history.clear();
	}

	public IItemHandler getDiscInventory() {
		return discInventory;
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		if (!world.isRemote) {
			if (!discsRemoved.isEmpty()) {
				discsRemoved.forEach(index -> {
					playlist.remove(index);
					history.remove(index);
				});
				discsRemoved.clear();
			}
			if (!discsAdded.isEmpty()) {
				playlist.addAll(discsAdded);
				discsAdded.clear();
			}
		}

		if (isPlaying && lastKeepAliveSendTime < world.getTotalWorldTime() - KEEP_ALIVE_SEND_INTERVAL) {
			storageWrapper.getContentsUuid().ifPresent(storageUuid ->
					ServerStorageSoundHandler.updateKeepAlive(storageUuid, world, entity != null ? entity.getPositionVector() : new Vec3d(pos).add(0.5, 0.5, 0.5),
							() -> setIsPlaying(false)));
			lastKeepAliveSendTime = world.getTotalWorldTime();
		}
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void next() {
		if (!isPlaying) {
			return;
		}
		playNext();
	}

	public void playNext() {
		playNext(true);
	}

	public void playNext(boolean startOverIfAtTheEnd) {
		if (playlist.isEmpty() && startOverIfAtTheEnd) {
			initPlaylist(false);
		}
		if (playlist.isEmpty()) {
			return;
		}
		if (getDiscSlotActive() != -1) {
			history.add(getDiscSlotActive());
			if (history.size() > discInventory.getSlots()) {
				history.poll();
			}
		}
		Integer discIndex = playlist.poll();
		if (discIndex == null) {
			return;
		}
		setDiscSlotActive(discIndex);
		playDisc();
	}

	private void initPlaylist(boolean excludeActive) {
		playlist.clear();
		for (int i = 0; i < discInventory.getSlots(); i++) {
			if (!discInventory.getStackInSlot(i).isEmpty() && (!excludeActive || !isPlaying || i != getDiscSlotActive())) {
				playlist.add(i);
			}
		}
		if (isShuffleEnabled()) {
			Collections.shuffle(playlist);
		}
	}

	public void previous() {
		if (!isPlaying) {
			return;
		}
		playPrevious();
	}

	public void playPrevious() {
		if (history.isEmpty()) {
			return;
		}
		playlist.addFirst(getDiscSlotActive());
		Integer discIndex = history.pollLast();
		if (discIndex == null) {
			return;
		}
		setDiscSlotActive(discIndex);
		playDisc();
	}

	public long getDiscFinishTime() {
		return getTag().getLong(DISC_FINISH_TIME_TAG);
	}
}
