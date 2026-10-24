package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public abstract class UpgradeItemBase<T extends IUpgradeWrapper> extends Item implements IUpgradeItem<T> {
	private final IUpgradeCountLimitConfig upgradeTypeLimitConfig;

	protected UpgradeItemBase(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		this.upgradeTypeLimitConfig = upgradeTypeLimitConfig;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.addAll(TranslationHelper.INSTANCE.getTranslatedLines(getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX, TextFormatting.DARK_GRAY));
	}

	@Override
	public int getUpgradesPerStorage(String storageType) {
		return upgradeTypeLimitConfig.getMaxUpgradesPerStorage(storageType, getRegistryName());
	}

	@Override
	public int getUpgradesInGroupPerStorage(String storageType) {
		if (getUpgradeGroup().isSolo()) {
			return Integer.MAX_VALUE;
		}

		return upgradeTypeLimitConfig.getMaxUpgradesInGroupPerStorage(storageType, getUpgradeGroup());
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return Collections.emptyList();
	}

	@Override
	public ITextComponent getName() {
		return new TextComponentTranslation(getTranslationKey() + ".name");
	}
}
