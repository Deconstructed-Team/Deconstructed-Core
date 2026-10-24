package dev.feldsher.deconstructedcore.inventory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import net.minecraft.item.ItemStack;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * One wrapper per storage stack. ItemStack has no equals, so the cache goes by identity.
 */
public class StorageWrapperRepository {
	private static final Cache<ItemStack, IStorageWrapper> stackStorageWrappers = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(10L, TimeUnit.MINUTES).build();

	private StorageWrapperRepository() {}

	public static <T extends IStorageWrapper> Optional<T> getExistingStorageWrapper(ItemStack stack, Class<T> wrapperClass) {
		IStorageWrapper storageWrapper = stackStorageWrappers.getIfPresent(stack);
		if (wrapperClass.isInstance(storageWrapper)) {
			return Optional.of(wrapperClass.cast(storageWrapper));
		}
		return Optional.empty();
	}

	public static <T extends IStorageWrapper> T getStorageWrapper(ItemStack stack, Class<T> wrapperClass, Function<ItemStack, T> factory) {
		IStorageWrapper storageWrapper = stackStorageWrappers.getIfPresent(stack);
		if (storageWrapper == null) {
			storageWrapper = factory.apply(stack);
			stackStorageWrappers.put(stack, storageWrapper);
		} else if (!wrapperClass.isInstance(storageWrapper)) {
			DeconstructedCore.LOGGER.error("StorageWrapperRepository: Wrapper with ItemStack {} is not an instance of {}. Replacing with new instance...", stack, wrapperClass);
			stackStorageWrappers.invalidate(stack);
			storageWrapper = factory.apply(stack);
			stackStorageWrappers.put(stack, storageWrapper);
		}
		return wrapperClass.cast(storageWrapper);
	}

	public static void clearCache() {
		stackStorageWrappers.invalidateAll();
	}
}
