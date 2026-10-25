package dev.feldsher.deconstructedcore.upgrades.feeding;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class FeedingUpgradeContainer extends UpgradeContainerBase<FeedingUpgradeWrapper, FeedingUpgradeContainer> {
	private static final String DATA_HUNGER_LEVEL = "hungerLevel";
	private static final String DATA_FEED_IMMEDIATELY_WHEN_HURT = "feedImmediatelyWhenHurt";

	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public FeedingUpgradeContainer(EntityPlayer player, int containerId, FeedingUpgradeWrapper wrapper, UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_HUNGER_LEVEL)) {
			setFeedAtHungerLevel(HungerLevel.fromName(data.getString(DATA_HUNGER_LEVEL)));
		} else if (data.hasKey(DATA_FEED_IMMEDIATELY_WHEN_HURT)) {
			setFeedImmediatelyWhenHurt(data.getBoolean(DATA_FEED_IMMEDIATELY_WHEN_HURT));
		}
		filterLogicContainer.handlePacket(data);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
		upgradeWrapper.setFeedAtHungerLevel(hungerLevel);
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setString(DATA_HUNGER_LEVEL, hungerLevel.getSerializedName());
			return data;
		});
	}

	public HungerLevel getFeedAtHungerLevel() {
		return upgradeWrapper.getFeedAtHungerLevel();
	}

	public void setFeedImmediatelyWhenHurt(boolean feedImmediatelyWhenHurt) {
		upgradeWrapper.setFeedImmediatelyWhenHurt(feedImmediatelyWhenHurt);
		sendBooleanToServer(DATA_FEED_IMMEDIATELY_WHEN_HURT, feedImmediatelyWhenHurt);
	}

	public boolean shouldFeedImmediatelyWhenHurt() {
		return upgradeWrapper.shouldFeedImmediatelyWhenHurt();
	}
}
