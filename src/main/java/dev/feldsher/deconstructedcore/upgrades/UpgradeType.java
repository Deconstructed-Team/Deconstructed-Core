package dev.feldsher.deconstructedcore.upgrades;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

public class UpgradeType<T extends IUpgradeWrapper> {
	private final IFactory<T> factory;

	public UpgradeType(IFactory<T> factory) {
		this.factory = factory;
	}

	public T create(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		return factory.create(storageWrapper, upgrade, upgradeSaveHandler);
	}

	public interface IFactory<T extends IUpgradeWrapper> {
		T create(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler);
	}
}
