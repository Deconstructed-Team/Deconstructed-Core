package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ContentsFilterLogicContainer extends FilterLogicContainer<ContentsFilterLogic> {
	private static final String DATA_CONTENTS_FILTER_TYPE = "contentsFilterType";

	public ContentsFilterLogicContainer(Supplier<ContentsFilterLogic> filterLogic, IServerUpdater serverUpdater, Consumer<Slot> addSlot) {
		super(filterLogic, serverUpdater, addSlot);
		if (getFilterType() == ContentsFilterType.STORAGE) {
			getFilterSlots().forEach(s -> s.setEnabled(false));
		}
	}

	public void setFilterType(ContentsFilterType depositFilterType) {
		filterLogic.get().setDepositFilterType(depositFilterType);
		sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString(DATA_CONTENTS_FILTER_TYPE, depositFilterType.getSerializedName());
			return tag;
		});
	}

	@Override
	public boolean handlePacket(NBTTagCompound data) {
		if (isDifferentFilterLogicsData(data)) {
			return false;
		}

		if (data.hasKey(DATA_CONTENTS_FILTER_TYPE)) {
			setFilterType(ContentsFilterType.fromName(data.getString(DATA_CONTENTS_FILTER_TYPE)));
			return true;
		}
		return super.handlePacket(data);
	}

	public ContentsFilterType getFilterType() {
		return filterLogic.get().getFilterType();
	}
}
