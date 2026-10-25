package dev.feldsher.deconstructedcore.upgrades.jukebox;

import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class JukeboxUpgradeContainer extends UpgradeContainerBase<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> {
	private static final String ACTION_DATA = "action";
	private static final String SHUFFLE_DATA = "shuffle";
	private static final String REPEAT_DATA = "repeat";

	public JukeboxUpgradeContainer(EntityPlayer player, int upgradeContainerId, JukeboxUpgradeWrapper upgradeWrapper, UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		for (int slot = 0; slot < upgradeWrapper.getDiscInventory().getSlots(); slot++) {
			slots.add(new SlotSuppliedHandler(() -> this.upgradeWrapper.getDiscInventory(), slot, -100, -100) {
				/** The disc playing taken out stops it. */
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					if (JukeboxUpgradeContainer.this.upgradeWrapper.isPlaying() && getSlotIndex() == JukeboxUpgradeContainer.this.upgradeWrapper.getDiscSlotActive()) {
						JukeboxUpgradeContainer.this.upgradeWrapper.stop(player);
					}
				}
			});
		}
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(ACTION_DATA)) {
			switch (data.getString(ACTION_DATA)) {
				case "play":
					if (player.openContainer instanceof StorageContainerMenuBase<?>) {
						StorageContainerMenuBase<?> storageContainerMenu = (StorageContainerMenuBase<?>) player.openContainer;
						Optional<BlockPos> blockPosition = storageContainerMenu.getBlockPosition();
						if (blockPosition.isPresent()) {
							upgradeWrapper.play(player.world, blockPosition.get());
						} else {
							upgradeWrapper.play(storageContainerMenu.getEntity().orElse(player));
						}
					}
					break;
				case "stop":
					upgradeWrapper.stop(player);
					break;
				case "next":
					upgradeWrapper.next();
					break;
				case "previous":
					upgradeWrapper.previous();
					break;
				default:
					break;
			}
		}
		if (data.hasKey(SHUFFLE_DATA)) {
			upgradeWrapper.setShuffleEnabled(data.getBoolean(SHUFFLE_DATA));
		}
		if (data.hasKey(REPEAT_DATA)) {
			upgradeWrapper.setRepeatMode(RepeatMode.fromName(data.getString(REPEAT_DATA)));
		}
	}

	private void sendAction(String action) {
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setString(ACTION_DATA, action);
			return data;
		});
	}

	public void play() {
		sendAction("play");
	}

	public void stop() {
		sendAction("stop");
	}

	public void next() {
		sendAction("next");
	}

	public void previous() {
		sendAction("previous");
	}

	public boolean isShuffleEnabled() {
		return upgradeWrapper.isShuffleEnabled();
	}

	public void toggleShuffle() {
		boolean newValue = !upgradeWrapper.isShuffleEnabled();
		upgradeWrapper.setShuffleEnabled(newValue);
		sendBooleanToServer(SHUFFLE_DATA, newValue);
	}

	public RepeatMode getRepeatMode() {
		return upgradeWrapper.getRepeatMode();
	}

	public void toggleRepeat() {
		RepeatMode newValue = upgradeWrapper.getRepeatMode().next();
		upgradeWrapper.setRepeatMode(newValue);
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setString(REPEAT_DATA, newValue.getName());
			return data;
		});
	}

	public Optional<Slot> getDiscSlotActive() {
		int discSlotActive = upgradeWrapper.getDiscSlotActive();
		return discSlotActive > -1 && discSlotActive < slots.size() ? Optional.of(slots.get(discSlotActive)) : Optional.empty();
	}

	public long getDiscFinishTime() {
		return upgradeWrapper.getDiscFinishTime();
	}

	public int getDiscLengthInTicks() {
		return DiscLengths.getLengthInTicks(upgradeWrapper.getDisc());
	}
}
