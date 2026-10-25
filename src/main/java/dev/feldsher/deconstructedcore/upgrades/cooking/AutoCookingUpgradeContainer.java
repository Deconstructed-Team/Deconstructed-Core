package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class AutoCookingUpgradeContainer extends UpgradeContainerBase<AutoCookingUpgradeWrapper, AutoCookingUpgradeContainer> {
	private final FilterLogicContainer<FilterLogic> inputFilterLogicContainer;
	private final FilterLogicContainer<FilterLogic> fuelFilterLogicContainer;
	private final CookingLogicContainer cookingLogicContainer;

	public AutoCookingUpgradeContainer(EntityPlayer player, int containerId, AutoCookingUpgradeWrapper wrapper, UpgradeContainerType<AutoCookingUpgradeWrapper, AutoCookingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		inputFilterLogicContainer = new FilterLogicContainer<>(() -> upgradeWrapper.getInputFilterLogic(), this, slots::add);
		fuelFilterLogicContainer = new FilterLogicContainer<>(() -> upgradeWrapper.getFuelFilterLogic(), this, slots::add);
		cookingLogicContainer = new CookingLogicContainer(() -> upgradeWrapper.getCookingLogic(), slots::add);
	}

	/** Only the input filter has settings: the fuel filter is a basic one. */
	@Override
	public void handlePacket(NBTTagCompound data) {
		inputFilterLogicContainer.handlePacket(data);
	}

	public CookingLogicContainer getCookingLogicContainer() {
		return cookingLogicContainer;
	}

	public FilterLogicContainer<FilterLogic> getInputFilterLogicContainer() {
		return inputFilterLogicContainer;
	}

	public FilterLogicContainer<FilterLogic> getFuelFilterLogicContainer() {
		return fuelFilterLogicContainer;
	}
}
