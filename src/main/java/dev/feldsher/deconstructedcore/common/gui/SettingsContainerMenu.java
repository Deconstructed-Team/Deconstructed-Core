package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncSettingsMessage;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.SettingsContainerBase;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsContainer;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsContainer;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;

/**
 * A storage's settings: its inventory, to look at and pick slots in, and a container per settings category. Clicks on
 * the slots go to the open settings tab, not to the slots; there are no player slots. Settings templates are saved
 * and loaded through its {@link TemplatePersistanceContainer}.
 */
public abstract class SettingsContainerMenu<S extends IStorageWrapper> extends LargeStackSyncContainer implements ISyncedContainer {
	private static final Map<String, ISettingsContainerFactory<?, ?>> SETTINGS_CONTAINER_FACTORIES = new HashMap<>();

	static {
		addFactory(MainSettingsCategory.NAME, MainSettingsContainer::new);
		addFactory(NoSortSettingsCategory.NAME, NoSortSettingsContainer::new);
		addFactory(MemorySettingsCategory.NAME, MemorySettingsContainer::new);
		addFactory(ItemDisplaySettingsCategory.NAME, ItemDisplaySettingsContainer::new);
	}

	protected final EntityPlayer player;
	protected final S storageWrapper;
	private final List<Slot> storageInventorySlots = new ArrayList<>();
	private final Map<String, SettingsContainerBase<?>> settingsContainers = new LinkedHashMap<>();
	private final TemplatePersistanceContainer templatePersistanceContainer;
	@Nullable
	private NBTTagCompound lastSettingsNbt = null;

	protected SettingsContainerMenu(EntityPlayer player, S storageWrapper) {
		this.player = player;
		this.storageWrapper = storageWrapper;
		if (player.world.isRemote) {
			// The server sent the contents just before opening: the client builds from them, not from what it had
			storageWrapper.onContentsNbtUpdated();
		}
		addStorageInventorySlots();
		addSettingsContainers();
		if (player instanceof EntityPlayerMP) {
			TemplatePersistanceContainer.sendTemplatesTo((EntityPlayerMP) player);
		}
		templatePersistanceContainer = new TemplatePersistanceContainer(this);
	}

	public static <C extends ISettingsCategory<?>, T extends SettingsContainerBase<C>> void addFactory(String categoryName, ISettingsContainerFactory<C, T> factory) {
		SETTINGS_CONTAINER_FACTORIES.put(categoryName, factory);
	}

	private void addStorageInventorySlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		for (int slotIndex = 0; slotIndex < inventoryHandler.getSlots(); slotIndex++) {
			storageInventorySlots.add(addSlotToContainer(new ViewOnlyStorageInventorySlot(storageWrapper, inventoryHandler, slotIndex)));
		}
	}

	private void addSettingsContainers() {
		storageWrapper.getSettingsHandler().getSettingsCategories().forEach((name, category) -> {
			ISettingsContainerFactory<?, ?> factory = SETTINGS_CONTAINER_FACTORIES.get(name);
			if (factory != null) {
				settingsContainers.put(name, instantiateContainer(factory, name, category));
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <C extends ISettingsCategory<?>> SettingsContainerBase<?> instantiateContainer(ISettingsContainerFactory<?, ?> factory, String name, ISettingsCategory<?> category) {
		return ((ISettingsContainerFactory<C, ?>) factory).create(this, name, (C) category);
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	public EntityPlayer getPlayer() {
		return player;
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlots();
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
	}

	public List<Slot> getStorageInventorySlots() {
		return storageInventorySlots;
	}

	public void forEachSettingsContainer(BiConsumer<String, ? super SettingsContainerBase<?>> consumer) {
		settingsContainers.forEach(consumer);
	}

	/** The settings the client asked to change come back to it changed, as the server has them. */
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if (!(player instanceof EntityPlayerMP)) {
			return;
		}
		storageWrapper.refreshRenderInfo();
		NBTTagCompound settingsNbt = storageWrapper.getSettingsHandler().getNbt();
		if (!settingsNbt.equals(lastSettingsNbt)) {
			lastSettingsNbt = settingsNbt.copy();
			PacketHandler.INSTANCE.sendTo(new SyncSettingsMessage(windowId, lastSettingsNbt), (EntityPlayerMP) player);
		}
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(TemplatePersistanceContainer.TEMPLATE_PERSISTANCE_TAG, Constants.NBT.TAG_COMPOUND)) {
			templatePersistanceContainer.handlePacket(data.getCompoundTag(TemplatePersistanceContainer.TEMPLATE_PERSISTANCE_TAG));
			return;
		}
		String categoryName = data.getString("categoryName");
		if (settingsContainers.containsKey(categoryName)) {
			settingsContainers.get(categoryName).handlePacket(data);
		}
	}

	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	/** The category as the template that's selected to load has it, to show over the slots. */
	public <T extends ISettingsCategory<?>> Optional<T> getSelectedTemplatesCategory(Class<T> categoryClass) {
		return templatePersistanceContainer.getSelectedTemplate().map(selectedTemplate -> selectedTemplate.getTypeCategory(categoryClass));
	}

	public TemplatePersistanceContainer getTemplatePersistanceContainer() {
		return templatePersistanceContainer;
	}

	public void refreshTemplateSlots() {
		templatePersistanceContainer.refreshTemplateSlots();
	}

	public interface ISettingsContainerFactory<C extends ISettingsCategory<?>, T extends SettingsContainerBase<C>> {
		T create(SettingsContainerMenu<?> settingsContainer, String categoryName, C category);
	}

	/**
	 * Slots whose stacks vanilla doesn't draw, while the settings screen draws them itself, turned: set only around
	 * the screen's drawing, on the client.
	 */
	public static void setStacksHiddenFromDrawing(@Nullable IntPredicate hiddenSlots) {
		ViewOnlyStorageInventorySlot.hiddenSlots = hiddenSlots;
	}

	private static class ViewOnlyStorageInventorySlot extends SlotItemHandler {
		@Nullable
		private static IntPredicate hiddenSlots = null;

		private final IStorageWrapper storageWrapper;

		ViewOnlyStorageInventorySlot(IStorageWrapper storageWrapper, InventoryHandler inventoryHandler, int slotIndex) {
			super(inventoryHandler, slotIndex, 0, 0);
			this.storageWrapper = storageWrapper;
		}

		@Nullable
		@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
		@Override
		public String getSlotTexture() {
			return StorageInventorySlot.getSlotTexture(storageWrapper, getSlotIndex());
		}

		@Override
		public ItemStack getStack() {
			if (hiddenSlots != null && hiddenSlots.test(slotNumber)) {
				return ItemStack.EMPTY;
			}
			return super.getStack();
		}

		@Override
		public boolean canTakeStack(EntityPlayer player) {
			return false;
		}

		@Override
		public boolean isItemValid(@Nonnull ItemStack stack) {
			return false;
		}
	}
}
