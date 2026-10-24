package dev.feldsher.deconstructedcore.upgrades.filter;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class FilterUpgradeContainer extends UpgradeContainerBase<FilterUpgradeWrapper, FilterUpgradeContainer> {
	public static final UpgradeContainerType<FilterUpgradeWrapper, FilterUpgradeContainer> BASIC_TYPE = new UpgradeContainerType<>(FilterUpgradeContainer::new);
	public static final UpgradeContainerType<FilterUpgradeWrapper, FilterUpgradeContainer> ADVANCED_TYPE = new UpgradeContainerType<>(FilterUpgradeContainer::new);
	private static final String DATA_DIRECTION = "direction";
	private final ContentsFilterLogicContainer filterLogicContainer;

	private FilterUpgradeContainer(EntityPlayer player, int containerId, FilterUpgradeWrapper wrapper, UpgradeContainerType<FilterUpgradeWrapper, FilterUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new ContentsFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	public ContentsFilterLogicContainer getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public Direction getDirection() {
		return upgradeWrapper.getDirection();
	}

	public void setDirection(Direction direction) {
		upgradeWrapper.setDirection(direction);
		sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString(DATA_DIRECTION, direction.getSerializedName());
			return tag;
		});
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (filterLogicContainer.handlePacket(data)) {
			return;
		}
		if (data.hasKey(DATA_DIRECTION)) {
			setDirection(Direction.fromName(data.getString(DATA_DIRECTION)));
		}
	}
}
