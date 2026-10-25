package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A storage's settings, by category, kept in its contents NBT.
 */
public abstract class SettingsHandler {
	protected NBTTagCompound contentsNbt;
	protected final Runnable markContentsDirty;
	protected final Supplier<InventoryHandler> inventoryHandlerSupplier;
	protected final Map<String, ISettingsCategory<?>> settingsCategories = new LinkedHashMap<>();
	private final Map<Class<?>, List<?>> interfaceCategories = new HashMap<>();
	private final Map<Class<? extends ISettingsCategory<?>>, ISettingsCategory<?>> typeCategories = new HashMap<>();

	protected SettingsHandler(NBTTagCompound contentsNbt, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier) {
		this.contentsNbt = contentsNbt;
		this.markContentsDirty = markContentsDirty;
		this.inventoryHandlerSupplier = inventoryHandlerSupplier;
		addSettingsCategories(getSettingsNbtFromContentsNbt(contentsNbt));
	}

	protected abstract NBTTagCompound getSettingsNbtFromContentsNbt(NBTTagCompound contentsNbt);

	protected void addSettingsCategories(NBTTagCompound settingsNbt) {
		addSettingsCategory(settingsNbt, getGlobalSettingsCategoryName(), markContentsDirty, this::instantiateGlobalSettingsCategory);
		addSettingsCategory(settingsNbt, NoSortSettingsCategory.NAME, markContentsDirty, NoSortSettingsCategory::new);
		addSettingsCategory(settingsNbt, MemorySettingsCategory.NAME, markContentsDirty,
				(categoryNbt, saveNbt) -> new MemorySettingsCategory(inventoryHandlerSupplier, categoryNbt, saveNbt));
		addItemDisplayCategory(settingsNbt);
	}

	/** With as many items as the storage shows. */
	protected abstract void addItemDisplayCategory(NBTTagCompound settingsNbt);

	public abstract String getGlobalSettingsCategoryName();

	public abstract ISettingsCategory<?> instantiateGlobalSettingsCategory(NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt);

	public MainSettingsCategory<?> getGlobalSettingsCategory() {
		return (MainSettingsCategory<?>) settingsCategories.get(getGlobalSettingsCategoryName());
	}

	protected void addSettingsCategory(NBTTagCompound settingsNbt, String categoryName, Runnable markContentsDirty,
			BiFunction<NBTTagCompound, Consumer<NBTTagCompound>, ISettingsCategory<?>> instantiateCategory) {
		ISettingsCategory<?> category = instantiateCategory.apply(settingsNbt.getCompoundTag(categoryName), tag -> {
			saveCategoryNbt(settingsNbt, categoryName, tag);
			markContentsDirty.run();
		});
		settingsCategories.put(categoryName, category);
		addTypeCategory(category);
	}

	private <T extends ISettingsCategory<T>> void addTypeCategory(ISettingsCategory<?> category) {
		//noinspection unchecked
		typeCategories.put((Class<T>) category.getClass(), category);
	}

	protected abstract void saveCategoryNbt(NBTTagCompound settingsNbt, String categoryName, NBTTagCompound tag);

	public Map<String, ISettingsCategory<?>> getSettingsCategories() {
		return settingsCategories;
	}

	public <T> List<T> getCategoriesThatImplement(Class<T> categoryClass) {
		//noinspection unchecked
		return (List<T>) interfaceCategories.computeIfAbsent(categoryClass, this::getListOfCategoriesThatImplement);
	}

	public <T extends ISettingsCategory<?>> T getTypeCategory(Class<T> categoryClazz) {
		//noinspection unchecked - only inserted in one place where it's made sure that class is the same as the category instance
		return (T) typeCategories.get(categoryClazz);
	}

	private <T> List<T> getListOfCategoriesThatImplement(Class<T> categoryClass) {
		List<T> ret = new ArrayList<>();
		for (ISettingsCategory<?> category : settingsCategories.values()) {
			if (categoryClass.isInstance(category)) {
				ret.add(categoryClass.cast(category));
			}
		}
		return ret;
	}

	public NBTTagCompound getNbt() {
		return getSettingsNbtFromContentsNbt(contentsNbt);
	}

	/** The settings as the server sent them to the client: into the categories, and into the client's contents. */
	public void reloadSettings(NBTTagCompound settingsNbt) {
		getSettingsCategories().forEach((categoryName, category) -> {
			NBTTagCompound categoryNbt = settingsNbt.getCompoundTag(categoryName);
			category.reloadFrom(categoryNbt);
			saveCategoryNbt(settingsNbt, categoryName, categoryNbt);
		});
	}

	public void reloadFrom(NBTTagCompound contentsNbt) {
		NBTTagCompound settingsNbt = getSettingsNbtFromContentsNbt(contentsNbt);
		getSettingsCategories().forEach((categoryName, category) -> category.reloadFrom(settingsNbt.getCompoundTag(categoryName)));
	}
}
