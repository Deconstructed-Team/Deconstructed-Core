package dev.feldsher.deconstructedcore.upgrades;

import java.util.EnumMap;
import dev.feldsher.deconstructedcore.renderdata.TankRenderInfo;
import dev.feldsher.deconstructedcore.renderdata.TankPosition;
import dev.feldsher.deconstructedcore.renderdata.BatteryRenderInfo;
import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A storage's upgrade slots, and the wrappers of the upgrades in them: by slot, by type, and by what they implement.
 * The render info of tanks and batteries comes with the upgrades that show them.
 */
public class UpgradeHandler extends ItemStackHandler {
	public static final String UPGRADE_INVENTORY_TAG = "upgradeInventory";
	private final IStorageWrapper storageWrapper;
	private final Runnable contentsSaveHandler;
	private final Runnable onInvalidateUpgradeCaches;
	private final NBTTagCompound contentsNbt;
	@Nullable
	private Runnable refreshCallBack = null;
	private final Map<Integer, IUpgradeWrapper> slotWrappers = new LinkedHashMap<>();
	private final Map<UpgradeType<? extends IUpgradeWrapper>, List<? extends IUpgradeWrapper>> typeWrappers = new HashMap<>();
	@Nullable
	private IUpgradeWrapperAccessor wrapperAccessor = null;
	private boolean justSavingNbtChange = false;
	private boolean wrappersInitialized = false;
	private boolean typeWrappersInitialized = false;
	private boolean persistent = true;
	private final Map<Class<? extends IUpgradeWrapper>, Consumer<? extends IUpgradeWrapper>> upgradeDefaultsHandlers = new HashMap<>();

	public UpgradeHandler(int numberOfUpgradeSlots, IStorageWrapper storageWrapper, NBTTagCompound contentsNbt, Runnable contentsSaveHandler, Runnable onInvalidateUpgradeCaches) {
		super(numberOfUpgradeSlots);
		this.contentsNbt = contentsNbt;
		this.storageWrapper = storageWrapper;
		this.contentsSaveHandler = contentsSaveHandler;
		this.onInvalidateUpgradeCaches = onInvalidateUpgradeCaches;
		deserializeNBT(contentsNbt.getCompoundTag(UPGRADE_INVENTORY_TAG));
	}

	private static boolean isServerSide() {
		return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;
	}

	public <T extends IUpgradeWrapper> void registerUpgradeDefaultsHandler(Class<T> upgradeClass, Consumer<T> defaultsHandler) {
		upgradeDefaultsHandlers.put(upgradeClass, defaultsHandler);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return stack.isEmpty() || stack.getItem() instanceof IUpgradeItem;
	}

	@Override
	protected void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		if (persistent) {
			saveInventory();
			contentsSaveHandler.run();
		}
		if (!justSavingNbtChange) {
			refreshUpgradeWrappers();
		}
	}

	@Override
	public void setSize(int size) {
		super.setSize(stacks.size());
	}

	public void saveInventory() {
		contentsNbt.setTag(UPGRADE_INVENTORY_TAG, serializeNBT());
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	public void setRefreshCallBack(Runnable refreshCallBack) {
		this.refreshCallBack = refreshCallBack;
	}

	public void removeRefreshCallback() {
		refreshCallBack = null;
	}

	private void initializeWrappers() {
		if (wrappersInitialized) {
			return;
		}
		wrappersInitialized = true;
		slotWrappers.clear();
		typeWrappers.clear();
		if (wrapperAccessor != null) {
			wrapperAccessor.clearCache();
		}

		InventoryHelper.iterate(this, (slot, upgrade) -> {
			if (upgrade.isEmpty() || !(upgrade.getItem() instanceof IUpgradeItem<?>)) {
				return;
			}
			UpgradeType<?> type = ((IUpgradeItem<?>) upgrade.getItem()).getType();
			IUpgradeWrapper wrapper = type.create(storageWrapper, upgrade, upgradeStack -> {
				justSavingNbtChange = true;
				setStackInSlot(slot, upgradeStack);
				justSavingNbtChange = false;
			});
			setUpgradeDefaults(wrapper);
			slotWrappers.put(slot, wrapper);
		});
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		ItemStack result = super.insertItem(slot, stack, simulate);
		if (!simulate && isServerSide() && result.isEmpty() && !stack.isEmpty()) {
			onUpgradeAdded(slot);
		}
		return result;
	}

	private void onUpgradeAdded(int slot) {
		Map<Integer, IUpgradeWrapper> wrappers = getSlotWrappers();
		if (wrappers.containsKey(slot)) {
			wrappers.get(slot).onAdded();
		}
	}

	private void setUpgradeDefaults(IUpgradeWrapper wrapper) {
		getUpgradeDefaultsHandler(wrapper).accept(wrapper);
	}

	private <T extends IUpgradeWrapper> Consumer<T> getUpgradeDefaultsHandler(T wrapper) {
		//noinspection unchecked
		return (Consumer<T>) upgradeDefaultsHandlers.getOrDefault(wrapper.getClass(), w -> {
		});
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		ItemStack originalStack = getStackInSlot(slot);
		Map<Integer, IUpgradeWrapper> wrappers = getSlotWrappers();
		boolean itemsDiffer = !ItemStack.areItemsEqual(originalStack, stack) || !ItemStack.areItemStackTagsEqual(originalStack, stack);
		if (isServerSide() && itemsDiffer && wrappers.containsKey(slot)) {
			wrappers.get(slot).onBeforeRemoved();
		}

		super.setStackInSlot(slot, stack);

		if (isServerSide() && itemsDiffer) {
			onUpgradeAdded(slot);
		}
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (!simulate && isServerSide()) {
			ItemStack slotStack = getStackInSlot(slot);
			if (persistent && !slotStack.isEmpty() && amount == 1) {
				Map<Integer, IUpgradeWrapper> wrappers = getSlotWrappers();
				if (wrappers.containsKey(slot)) {
					wrappers.get(slot).onBeforeRemoved();
				}
			}
		}
		return super.extractItem(slot, amount, simulate);
	}

	private void initializeTypeWrappers() {
		if (typeWrappersInitialized) {
			return;
		}
		initializeWrappers();
		typeWrappersInitialized = true;

		typeWrappers.clear();
		slotWrappers.values().forEach(wrapper -> {
			if (wrapper.getUpgradeStack().getItem() instanceof IUpgradeItem<?> && wrapper.isEnabled()) {
				addTypeWrapper(((IUpgradeItem<?>) wrapper.getUpgradeStack().getItem()).getType(), wrapper);
			}
		});
	}

	private <T extends IUpgradeWrapper> void addTypeWrapper(UpgradeType<?> type, T wrapper) {
		//noinspection unchecked
		((List<T>) typeWrappers.computeIfAbsent(type, t -> new ArrayList<>())).add(wrapper);
	}

	public <T extends IUpgradeWrapper> List<T> getTypeWrappers(UpgradeType<T> type) {
		initializeTypeWrappers();
		//noinspection unchecked
		return (List<T>) typeWrappers.getOrDefault(type, Collections.emptyList());
	}

	public <T extends IUpgradeWrapper> boolean hasUpgrade(UpgradeType<T> type) {
		return !getTypeWrappers(type).isEmpty();
	}

	/** The enabled upgrades that implement it, as the accessor finds them: with inception, also those of the storages inside. */
	public <T> List<T> getWrappersThatImplement(Class<T> upgradeClass) {
		initializeWrappers();
		return getWrapperAccessor().getWrappersThatImplement(upgradeClass);
	}

	private IUpgradeWrapperAccessor getWrapperAccessor() {
		if (wrapperAccessor == null) {
			IUpgradeWrapperAccessor accessor = new Accessor(this);
			for (IUpgradeAccessModifier upgrade : getListOfWrappersThatImplement(IUpgradeAccessModifier.class)) {
				accessor = upgrade.wrapAccessor(accessor);
			}
			wrapperAccessor = accessor;
		}
		return wrapperAccessor;
	}

	/** Only this storage's own upgrades, even with inception. */
	public <T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass) {
		initializeWrappers();
		return getWrapperAccessor().getWrappersThatImplementFromMainStorage(upgradeClass);
	}

	public <T> List<T> getListOfWrappersThatImplement(Class<T> uc) {
		List<T> ret = new ArrayList<>();
		for (IUpgradeWrapper wrapper : slotWrappers.values()) {
			if (wrapper.isEnabled() && uc.isInstance(wrapper)) {
				ret.add(uc.cast(wrapper));
			}
		}
		return ret;
	}

	public Map<Integer, IUpgradeWrapper> getSlotWrappers() {
		initializeWrappers();
		return slotWrappers;
	}

	public void copyTo(UpgradeHandler otherHandler) {
		InventoryHelper.copyTo(this, otherHandler);
	}

	/**
	 * The storage's render info with what its upgrades show: tanks left then right, or a single tank on the right when
	 * it's in the second half of the upgrade slots, as in 1.21; the battery; what working upgrades show.
	 */
	public RenderInfo buildRenderInfo(RenderInfo.ItemDisplayRenderInfo itemDisplay) {
		Map<Integer, IUpgradeWrapper> wrappers = getSlotWrappers();
		List<IRenderedTankUpgrade> tanks = new ArrayList<>();
		boolean singleTankRight = false;
		BatteryRenderInfo battery = null;
		Map<String, NBTTagCompound> upgradeRenderData = new HashMap<>();
		for (Map.Entry<Integer, IUpgradeWrapper> entry : wrappers.entrySet()) {
			IUpgradeWrapper wrapper = entry.getValue();
			if (wrapper instanceof IRenderedTankUpgrade) {
				tanks.add((IRenderedTankUpgrade) wrapper);
				singleTankRight |= entry.getKey() >= getSlots() / 2;
			}
			if (wrapper instanceof IRenderedBatteryUpgrade && battery == null) {
				battery = ((IRenderedBatteryUpgrade) wrapper).getBatteryRenderInfo();
			}
			if (wrapper instanceof IRenderDataUpgrade && wrapper.isEnabled()) {
				((IRenderDataUpgrade) wrapper).addRenderData(upgradeRenderData);
			}
		}
		Map<TankPosition, TankRenderInfo> tankRenderInfos = new EnumMap<>(TankPosition.class);
		TankPosition position = tanks.size() == 1 && singleTankRight ? TankPosition.RIGHT : TankPosition.LEFT;
		for (IRenderedTankUpgrade tank : tanks) {
			if (!tankRenderInfos.containsKey(position)) {
				tankRenderInfos.put(position, tank.getTankRenderInfo());
			}
			position = TankPosition.RIGHT;
		}
		return new RenderInfo(itemDisplay, tankRenderInfos, battery, upgradeRenderData);
	}

	public void refreshWrappersThatImplementAndTypeWrappers() {
		typeWrappersInitialized = false;
		if (wrapperAccessor != null) {
			wrapperAccessor.clearCache();
		}
		if (refreshCallBack != null) {
			refreshCallBack.run();
		}
	}

	public void refreshUpgradeWrappers() {
		wrappersInitialized = false;
		typeWrappersInitialized = false;
		if (wrapperAccessor != null) {
			wrapperAccessor.onBeforeDeconstruct();
			wrapperAccessor = null;
		}
		if (refreshCallBack != null) {
			refreshCallBack.run();
		}
		onInvalidateUpgradeCaches.run();
	}

	public void increaseSize(int diff) {
		NonNullList<ItemStack> previousStacks = stacks;
		stacks = NonNullList.withSize(previousStacks.size() + diff, ItemStack.EMPTY);
		for (int slot = 0; slot < previousStacks.size(); slot++) {
			stacks.set(slot, previousStacks.get(slot));
		}
		saveInventory();
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	private static class Accessor implements IUpgradeWrapperAccessor {
		private final Map<Class<?>, List<?>> interfaceWrappers = new HashMap<>();
		private final UpgradeHandler upgradeHandler;

		private Accessor(UpgradeHandler upgradeHandler) {
			this.upgradeHandler = upgradeHandler;
		}

		@Override
		public <T> List<T> getWrappersThatImplement(Class<T> upgradeClass) {
			//noinspection unchecked
			return (List<T>) interfaceWrappers.computeIfAbsent(upgradeClass, upgradeHandler::getListOfWrappersThatImplement);
		}

		@Override
		public <T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass) {
			return getWrappersThatImplement(upgradeClass);
		}

		@Override
		public void clearCache() {
			interfaceWrappers.clear();
		}
	}
}
