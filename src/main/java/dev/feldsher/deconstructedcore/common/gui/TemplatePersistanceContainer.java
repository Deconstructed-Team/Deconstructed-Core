package dev.feldsher.deconstructedcore.common.gui;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import dev.feldsher.deconstructedcore.network.SyncTemplateSettingsMessage;
import dev.feldsher.deconstructedcore.settings.FileSettingsTemplates;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.settings.SettingsTemplateStorage;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Saving a storage's settings as a template, loading one onto it, and exporting them to a file. Both sides run each
 * action, the client first, so that the slots, their order and the selected ones stay the same on both.
 * <p>
 * Save slots are the player's numbered ones, one more numbered slot while there are fewer than ten, the named ones and
 * one to name anew. Load slots are the player's numbered and named ones, then those from files.
 */
public class TemplatePersistanceContainer {
	/** No path separators, so that an export stays in its folder. */
	private static final Pattern EXPORT_FILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9._\\-\\s]+");
	private static final int TOTAL_ORDINAL_SAVE_SLOTS = 10;
	// each player's named templates and exports are kept and sent to clients: so many of them, with names so long, at most
	private static final int MAX_NAME_LENGTH = 32;
	private static final int MAX_NAMED_SAVES = 16;
	private static final int MAX_EXPORTS_PER_PLAYER = 16;
	private static final String ACTION_TAG = "action";
	private static final String SAVE_SLOT_TAG = "saveSlot";
	private static final String LOAD_SLOT_TAG = "loadSlot";
	static final String TEMPLATE_PERSISTANCE_TAG = "templatePersistance";
	private final SettingsContainerMenu<?> settingsContainer;

	private final List<IPersistanceSlot> saveSlots = new ArrayList<>();
	private int saveSlotIndex = 0;
	private final List<IPersistanceSlot> loadSlots = new ArrayList<>();
	private int loadSlotIndex = -1;
	@Nullable
	private TemplateSettingsHandler selectedTemplate;
	private Runnable onSlotsRefreshed = () -> {
	};

	public TemplatePersistanceContainer(SettingsContainerMenu<?> settingsContainer) {
		this.settingsContainer = settingsContainer;
		initSlots();
	}

	/** The player's templates and those from files, which the client lists as the server does. */
	public static void sendTemplatesTo(EntityPlayerMP player) {
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get(player.world);
		PacketHandler.INSTANCE.sendTo(new SyncTemplateSettingsMessage(settingsTemplateStorage.getPlayerTemplates(player),
				settingsTemplateStorage.getPlayerNamedTemplates(player), FileSettingsTemplates.get(false).getTemplates()), player);
	}

	public void setOnSlotsRefreshed(Runnable onSlotsRefreshed) {
		this.onSlotsRefreshed = onSlotsRefreshed;
	}

	private void initSlots() {
		saveSlots.clear();
		int i;
		for (i = 0; i < getNumberOfSaves(); i++) {
			saveSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		if (i < TOTAL_ORDINAL_SAVE_SLOTS - 1) {
			saveSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		getNamedSaves().forEach(name -> saveSlots.add(new NamedPersistanceSlot(name)));
		saveSlots.add(new EditNamePersistanceSlot(""));
		saveSlotIndex = Math.min(saveSlotIndex, saveSlots.size() - 1);

		loadSlots.clear();
		for (i = 0; i < getNumberOfSaves(); i++) {
			loadSlots.add(new OrdinalPersistanceSlot(i + 1));
		}
		getNamedSaves().forEach(name -> loadSlots.add(new NamedPersistanceSlot(name)));
		getFileTemplates().getTemplates().forEach((source, templates) -> templates.keySet().forEach(templateName -> loadSlots.add(new FileSlot(source, templateName))));

		if (loadSlotIndex == -1 && !loadSlots.isEmpty()) {
			loadSlotIndex = 0;
		} else if (loadSlots.isEmpty()) {
			loadSlotIndex = -1;
		} else if (loadSlotIndex >= loadSlots.size()) {
			loadSlotIndex = loadSlots.size() - 1;
		}
		updateSelectedTemplate();
		onSlotsRefreshed.run();
	}

	public void handlePacket(NBTTagCompound data) {
		if (data.hasKey(ACTION_TAG)) {
			switch (data.getString(ACTION_TAG)) {
				case "saveTemplate":
					saveTemplate(data.getString("slotName"));
					break;
				case "loadTemplate":
					loadTemplate();
					break;
				case "exportTemplate":
					exportTemplate(data.getString("fileName"));
					break;
				default:
					break;
			}
		}
		if (data.hasKey(SAVE_SLOT_TAG)) {
			scrollSaveSlot(data.getBoolean(SAVE_SLOT_TAG));
		} else if (data.hasKey(LOAD_SLOT_TAG)) {
			scrollLoadSlot(data.getBoolean(LOAD_SLOT_TAG));
		}
	}

	private boolean isClientSide() {
		return getPlayer().world.isRemote;
	}

	private void sendDataToServer(Supplier<NBTTagCompound> compoundSupplier) {
		if (!isClientSide()) {
			return;
		}
		NBTTagCompound data = new NBTTagCompound();
		data.setTag(TEMPLATE_PERSISTANCE_TAG, compoundSupplier.get());
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	private static NBTTagCompound action(String action) {
		NBTTagCompound data = new NBTTagCompound();
		data.setString(ACTION_TAG, action);
		return data;
	}

	private EntityPlayer getPlayer() {
		return settingsContainer.getPlayer();
	}

	private SettingsTemplateStorage getTemplateStorage() {
		return SettingsTemplateStorage.get(getPlayer().world);
	}

	private FileSettingsTemplates getFileTemplates() {
		return FileSettingsTemplates.get(isClientSide());
	}

	private SettingsHandler getSettingsHandler() {
		return settingsContainer.getStorageWrapper().getSettingsHandler();
	}

	public void loadTemplate() {
		if (selectedTemplate == null) {
			return;
		}
		TemplateSettingsHandler template = selectedTemplate;
		getSettingsHandler().getSettingsCategories().values().forEach(category -> overwriteCategory(category, template.getTypeCategory(categoryClass(category))));

		sendDataToServer(() -> action("loadTemplate"));

		if (isClientSide()) {
			getPlayer().sendStatusMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("load_template"), loadSlots.get(loadSlotIndex).getSlotName()), false);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends ISettingsCategory<?>> Class<T> categoryClass(ISettingsCategory<?> category) {
		return (Class<T>) category.getClass();
	}

	@SuppressWarnings("unchecked")
	private static <T extends ISettingsCategory<T>> void overwriteCategory(ISettingsCategory<?> currentCategory, @Nullable ISettingsCategory<?> otherCategory) {
		if (otherCategory != null) {
			((T) currentCategory).overwriteWith((T) otherCategory);
		}
	}

	public void saveTemplate(String slotName) {
		IPersistanceSlot saveSlot = saveSlots.get(saveSlotIndex);
		if (saveSlot.showsTextbox() && slotName.trim().isEmpty()) {
			return;
		}
		if (saveSlot.showsTextbox() && slotName.trim().length() > MAX_NAME_LENGTH) {
			sendErrorMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("save_template.name_too_long"), MAX_NAME_LENGTH));
			return;
		}
		if (saveSlot.showsTextbox() && !getNamedSaves().contains(slotName.trim()) && getNamedSaves().size() >= MAX_NAMED_SAVES) {
			sendErrorMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("save_template.too_many"), MAX_NAMED_SAVES));
			return;
		}
		saveSlot.setSlotName(slotName.trim());
		saveSlot.persistTo(getPlayer(), getTemplateStorage(), getSettingsHandler().getNbt().copy());

		sendDataToServer(() -> {
			NBTTagCompound data = action("saveTemplate");
			data.setString("slotName", slotName);
			return data;
		});

		initSlots();

		moveSaveSlotIndexTo(saveSlot.getSlotName());

		if (isClientSide()) {
			getPlayer().sendStatusMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("save_template"), saveSlot.getSlotName()), false);
		}
	}

	private void moveSaveSlotIndexTo(String slotName) {
		for (int i = 0; i < saveSlots.size(); i++) {
			if (saveSlots.get(i).getSlotName().equals(slotName)) {
				saveSlotIndex = i;
				break;
			}
		}
	}

	public void scrollSaveSlot(boolean next) {
		saveSlotIndex += next ? 1 : -1;

		if (saveSlotIndex < 0) {
			saveSlotIndex = saveSlots.size() - 1;
		}
		if (saveSlotIndex >= saveSlots.size()) {
			saveSlotIndex = 0;
		}

		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean(SAVE_SLOT_TAG, next);
			return data;
		});
	}

	public void scrollLoadSlot(boolean next) {
		if (loadSlots.isEmpty()) {
			loadSlotIndex = -1;
			return;
		}

		loadSlotIndex += next ? 1 : -1;

		if (loadSlotIndex < 0) {
			loadSlotIndex = loadSlots.size() - 1;
		}
		if (loadSlotIndex >= loadSlots.size()) {
			loadSlotIndex = 0;
		}

		updateSelectedTemplate();
		sendDataToServer(() -> {
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean(LOAD_SLOT_TAG, next);
			return data;
		});
	}

	private void updateSelectedTemplate() {
		if (loadSlotIndex > -1 && loadSlotIndex < loadSlots.size()) {
			NBTTagCompound settingsTag = loadSlots.get(loadSlotIndex).getSettingsNbt(getPlayer(), getTemplateStorage(), getFileTemplates());
			selectedTemplate = new TemplateSettingsHandler(settingsTag, settingsContainer.getStorageWrapper()::getInventoryHandler) {
				@Override
				protected SettingsHandler getCurrentSettingsHandler() {
					return settingsContainer.getStorageWrapper().getSettingsHandler();
				}
			};
		} else {
			selectedTemplate = null;
		}
	}

	public ITextComponent getSaveSlotTooltipName() {
		return saveSlots.get(saveSlotIndex).getSlotTooltipName();
	}

	public ITextComponent getLoadSlotTooltipName() {
		return loadSlots.get(loadSlotIndex).getSlotTooltipName();
	}

	public int getLoadSlot() {
		return loadSlotIndex;
	}

	private int getNumberOfSaves() {
		return getTemplateStorage().getPlayerTemplates(getPlayer()).size();
	}

	private List<String> getNamedSaves() {
		return new ArrayList<>(getTemplateStorage().getPlayerNamedTemplates(getPlayer()).keySet());
	}

	public Optional<TemplateSettingsHandler> getSelectedTemplate() {
		return Optional.ofNullable(selectedTemplate);
	}

	public boolean showsTextbox() {
		return saveSlots.get(saveSlotIndex).showsTextbox();
	}

	public void refreshTemplateSlots() {
		initSlots();
	}

	/** Into the world's templates folder, under the player's name; the client only checks the name. */
	public void exportTemplate(String fileName) {
		if (fileName.trim().isEmpty()) {
			sendErrorMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("export_template.empty_name")));
			return;
		}

		Matcher matcher = EXPORT_FILE_NAME_PATTERN.matcher(fileName);
		if (!matcher.matches() || fileName.startsWith(".")) {
			String invalidCharacters = findNonMatchingCharacters(matcher, fileName);
			sendErrorMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("export_template.invalid_characters"),
					invalidCharacters.isEmpty() ? "." : invalidCharacters));
			return;
		}

		if (fileName.trim().length() > MAX_NAME_LENGTH) {
			sendErrorMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("export_template.name_too_long"), MAX_NAME_LENGTH));
			return;
		}

		String finalFileName = fileName.trim().replace(' ', '_').toLowerCase(Locale.ROOT);
		sendDataToServer(() -> {
			NBTTagCompound data = action("exportTemplate");
			data.setString("fileName", finalFileName);
			return data;
		});

		if (getPlayer() instanceof EntityPlayerMP) {
			EntityPlayerMP serverPlayer = (EntityPlayerMP) getPlayer();
			Path templatesDir = FileSettingsTemplates.getWorldTemplatesDir();
			if (templatesDir == null) {
				return;
			}
			String source = serverPlayer.getName().toLowerCase(Locale.ROOT);
			Path sourceDir = templatesDir.resolve(source);
			Path exportPath = sourceDir.resolve(finalFileName + FileSettingsTemplates.SUFFIX);
			if (!Files.exists(exportPath) && countExports(sourceDir) >= MAX_EXPORTS_PER_PLAYER) {
				serverPlayer.sendStatusMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("export_template.too_many"), MAX_EXPORTS_PER_PLAYER), false);
				return;
			}
			NBTTagCompound settingsNbt = getSettingsHandler().getNbt().copy();
			try {
				Files.createDirectories(sourceDir);
				Files.write(exportPath, settingsNbt.toString().getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				DeconstructedCore.LOGGER.error("Error writing settings template export", e);
				return;
			}

			getFileTemplates().putTemplate(source, finalFileName, settingsNbt);
			sendTemplatesTo(serverPlayer);
			initSlots();

			Path worldDir = DimensionManager.getCurrentSaveRootDirectory().toPath();
			serverPlayer.sendStatusMessage(new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsMessage("export_template"),
					worldDir.relativize(exportPath).toString()), false);
		}
	}

	private static long countExports(Path sourceDir) {
		if (!Files.isDirectory(sourceDir)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(sourceDir)) {
			return files.filter(file -> file.getFileName().toString().endsWith(FileSettingsTemplates.SUFFIX)).count();
		} catch (IOException e) {
			DeconstructedCore.LOGGER.error("Couldn't count settings template exports in {}", sourceDir, e);
			return Long.MAX_VALUE;
		}
	}

	private void sendErrorMessage(TextComponentTranslation message) {
		if (isClientSide()) {
			getPlayer().sendStatusMessage(message.setStyle(new Style().setColor(TextFormatting.RED)), false);
		}
	}

	public static String findNonMatchingCharacters(Matcher matcher, String input) {
		StringBuilder nonMatchingCharacters = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (!matcher.reset(String.valueOf(c)).matches()) {
				nonMatchingCharacters.append(c);
			}
		}

		return nonMatchingCharacters.toString();
	}

	public boolean templateHasTooManySlots() {
		return selectedTemplate != null && selectedTemplate.getSettingsCategories().values().stream()
				.anyMatch(category -> category.isLargerThanNumberOfSlots(settingsContainer.getStorageWrapper().getInventoryHandler().getSlots()));
	}

	public Optional<String> getLoadSlotSource() {
		return loadSlots.get(loadSlotIndex).getSlotSource();
	}

	/** A template's settings, read only, in categories as the storage's own; its item display shows as many items. */
	public abstract static class TemplateSettingsHandler extends SettingsHandler {
		protected TemplateSettingsHandler(NBTTagCompound settingsNbt, Supplier<InventoryHandler> inventoryHandlerSupplier) {
			super(settingsNbt, () -> {
			}, inventoryHandlerSupplier);
		}

		protected abstract SettingsHandler getCurrentSettingsHandler();

		@Override
		protected NBTTagCompound getSettingsNbtFromContentsNbt(NBTTagCompound contentsNbt) {
			return contentsNbt;
		}

		@Override
		protected void addItemDisplayCategory(NBTTagCompound settingsNbt) {
			ItemDisplaySettingsCategory currentCategory = getCurrentSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class);
			if (currentCategory == null) {
				return;
			}
			int itemNumberLimit = currentCategory.getItemNumberLimit();
			addSettingsCategory(settingsNbt, ItemDisplaySettingsCategory.NAME, markContentsDirty, (categoryNbt, saveNbt) ->
					new ItemDisplaySettingsCategory(inventoryHandlerSupplier, categoryNbt, saveNbt, itemNumberLimit, () -> getTypeCategory(MemorySettingsCategory.class)));
		}

		@Override
		public String getGlobalSettingsCategoryName() {
			return getCurrentSettingsHandler().getGlobalSettingsCategoryName();
		}

		@Override
		public ISettingsCategory<?> instantiateGlobalSettingsCategory(NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt) {
			return getCurrentSettingsHandler().instantiateGlobalSettingsCategory(categoryNbt, saveNbt);
		}

		@Override
		protected void saveCategoryNbt(NBTTagCompound settingsNbt, String categoryName, NBTTagCompound tag) {
			//noop
		}
	}

	private interface IPersistanceSlot {
		String getSlotName();

		default void persistTo(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, NBTTagCompound settingsCopy) {
			//noop
		}

		default boolean showsTextbox() {
			return false;
		}

		default void setSlotName(String slotName) {
			//noop
		}

		default ITextComponent getSlotTooltipName() {
			return new TextComponentString(getSlotName());
		}

		NBTTagCompound getSettingsNbt(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, FileSettingsTemplates fileTemplates);

		default Optional<String> getSlotSource() {
			return Optional.empty();
		}
	}

	private static class OrdinalPersistanceSlot implements IPersistanceSlot {
		private final int slot;

		public OrdinalPersistanceSlot(int slot) {
			this.slot = slot;
		}

		@Override
		public String getSlotName() {
			return String.valueOf(slot);
		}

		@Override
		public void persistTo(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, NBTTagCompound settingsCopy) {
			settingsTemplateStorage.putPlayerTemplate(player, slot, settingsCopy);
		}

		@Override
		public NBTTagCompound getSettingsNbt(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, FileSettingsTemplates fileTemplates) {
			return settingsTemplateStorage.getPlayerTemplates(player).getOrDefault(slot, new NBTTagCompound());
		}
	}

	private static class NamedPersistanceSlot implements IPersistanceSlot {
		protected String slotName;

		public NamedPersistanceSlot(String slotName) {
			this.slotName = slotName;
		}

		@Override
		public String getSlotName() {
			return slotName;
		}

		@Override
		public void persistTo(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, NBTTagCompound settingsCopy) {
			settingsTemplateStorage.putPlayerNamedTemplate(player, slotName, settingsCopy);
		}

		@Override
		public NBTTagCompound getSettingsNbt(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, FileSettingsTemplates fileTemplates) {
			return settingsTemplateStorage.getPlayerNamedTemplates(player).getOrDefault(slotName, new NBTTagCompound());
		}
	}

	private static class FileSlot implements IPersistanceSlot {
		private final String source;
		private final String templateName;

		public FileSlot(String source, String templateName) {
			this.source = source;
			this.templateName = templateName;
		}

		@Override
		public String getSlotName() {
			return templateName;
		}

		@Override
		public NBTTagCompound getSettingsNbt(EntityPlayer player, SettingsTemplateStorage settingsTemplateStorage, FileSettingsTemplates fileTemplates) {
			return fileTemplates.getTemplateNbt(source, templateName).orElseGet(NBTTagCompound::new);
		}

		@Override
		public Optional<String> getSlotSource() {
			return Optional.of(source);
		}
	}

	private static class EditNamePersistanceSlot extends NamedPersistanceSlot {
		public EditNamePersistanceSlot(String slotName) {
			super(slotName);
		}

		@Override
		public boolean showsTextbox() {
			return true;
		}

		@Override
		public void setSlotName(String slotName) {
			this.slotName = slotName;
		}

		@Override
		public ITextComponent getSlotTooltipName() {
			return new TextComponentTranslation(TranslationHelper.INSTANCE.translSettingsButton("save_template.custom_name_slot"));
		}
	}
}
