package dev.feldsher.deconstructedcore.client.render;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import dev.feldsher.deconstructedcore.util.CountAbbreviator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A storage item's tooltip, with shift held, shows what it holds: its upgrades, then its items, the same ones together
 * and the most first. 1.12 has no images in tooltips: the rows of items get blank lines of their width, which the items
 * are drawn over once the tooltip's text is.
 */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID, value = Side.CLIENT)
public final class StorageContentsTooltip {
	private static final String STORAGE_ITEM = "storage";
	// formatting codes that aren't any: drawn as nothing, they mark the lines the items go over
	private static final String UPGRADES_MARKER = "§u§p";
	private static final String INVENTORY_MARKER = "§u§i";
	private static final TextureBlitData UPGRADE_ON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(4, 128), Dimension.RECTANGLE_4_10);
	private static final TextureBlitData UPGRADE_OFF = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(0, 128), Dimension.RECTANGLE_4_10);
	private static final int MAX_STACKS_ON_LINE = 9;
	private static final int DEFAULT_STACK_WIDTH = 18;
	private static final int COUNT_PADDING = 2;
	private static final long REFRESH_INTERVAL_MS = 1000;
	private static final List<IContentsTooltipSource> SOURCES = new ArrayList<>();
	@Nullable
	private static UUID lastRequestedUuid = null;
	private static long lastRequestTime = 0;
	@Nullable
	private static Shown shown = null;

	private StorageContentsTooltip() {}

	/** Which items show their contents so, and how the client gets them. */
	public interface IContentsTooltipSource {
		/** The stack's storage, if it's one of these. */
		Optional<IStorageWrapper> getStorageWrapper(ItemStack stack);

		boolean hasClientContents(UUID uuid);

		/** Asks the server for the contents, as they may have changed on it. */
		void requestContents(UUID uuid);
	}

	public static void registerSource(IContentsTooltipSource source) {
		SOURCES.add(source);
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		// also asked for every item as the creative search is built, with no player yet
		if (event.getEntityPlayer() == null) {
			return;
		}
		ItemStack stack = event.getItemStack();
		for (IContentsTooltipSource source : SOURCES) {
			Optional<IStorageWrapper> wrapper = source.getStorageWrapper(stack);
			if (wrapper.isPresent()) {
				addContentsLines(event.getToolTip(), stack, wrapper.get(), source);
				return;
			}
		}
	}

	private static void addContentsLines(List<String> tooltip, ItemStack stack, IStorageWrapper wrapper, IContentsTooltipSource source) {
		String tooltipKey = TranslationHelper.INSTANCE.translItemTooltip(STORAGE_ITEM);
		if (!GuiScreen.isShiftKeyDown()) {
			tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".press_for_contents", TextFormatting.AQUA + I18n.format(tooltipKey + ".shift") + TextFormatting.GRAY));
			return;
		}
		Optional<UUID> uuid = wrapper.getContentsUuid();
		if (uuid.isPresent()) {
			requestContents(source, uuid.get());
			if (!source.hasClientContents(uuid.get())) {
				return;
			}
		}
		Collection<IUpgradeWrapper> upgrades = uuid.isPresent() ? wrapper.getUpgradeHandler().getSlotWrappers().values() : new ArrayList<>();
		List<ItemStack> contents = uuid.isPresent() ? getCompactedStacksSortedByCount(wrapper.getInventoryHandler()) : new ArrayList<>();
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		if (uuid.isPresent()) {
			addMultiplierLine(tooltip, wrapper, tooltipKey);
			addFluidLines(tooltip, wrapper, tooltipKey);
			wrapper.getEnergyStorage().ifPresent(energyStorage -> tooltip.add(TextFormatting.RED + I18n.format(tooltipKey + ".energy",
					TextFormatting.WHITE + CountAbbreviator.abbreviate(energyStorage.getEnergyStored()) + TextFormatting.RED)));
		}
		if (upgrades.isEmpty() && contents.isEmpty()) {
			tooltip.add(TextFormatting.YELLOW + I18n.format(tooltipKey + ".empty"));
			shown = null;
			return;
		}
		if (!upgrades.isEmpty()) {
			tooltip.add(TextFormatting.YELLOW + I18n.format(tooltipKey + ".upgrades"));
			int width = 0;
			for (IUpgradeWrapper upgradeWrapper : upgrades) {
				width += (upgradeWrapper.canBeDisabled() ? 4 : 0) + DEFAULT_STACK_WIDTH;
			}
			addRowLines(tooltip, UPGRADES_MARKER, width, font);
		}
		if (!contents.isEmpty()) {
			tooltip.add(TextFormatting.YELLOW + I18n.format(tooltipKey + ".inventory"));
			for (int rowStart = 0; rowStart < contents.size(); rowStart += MAX_STACKS_ON_LINE) {
				int width = 0;
				for (int i = rowStart; i < contents.size() && i < rowStart + MAX_STACKS_ON_LINE; i++) {
					width += getStackWidth(font, contents.get(i));
				}
				addRowLines(tooltip, rowStart == 0 ? INVENTORY_MARKER : "", width, font);
			}
		}
		shown = new Shown(stack, new ArrayList<>(upgrades), contents);
	}

	/** While shown, the contents are asked for again each second, as they may change, as a worn backpack's picking items up. */
	private static void requestContents(IContentsTooltipSource source, UUID uuid) {
		long now = System.currentTimeMillis();
		if (!uuid.equals(lastRequestedUuid) || now - lastRequestTime > REFRESH_INTERVAL_MS) {
			lastRequestedUuid = uuid;
			lastRequestTime = now;
			source.requestContents(uuid);
		}
	}

	/** A row of items takes two lines, the first as wide as the row. */
	private static void addRowLines(List<String> tooltip, String marker, int width, FontRenderer font) {
		StringBuilder spaces = new StringBuilder(marker);
		int spaceWidth = Math.max(1, font.getCharWidth(' '));
		for (int i = 0; i < (width + spaceWidth - 1) / spaceWidth; i++) {
			spaces.append(' ');
		}
		tooltip.add(spaces.toString());
		tooltip.add("");
	}

	private static void addMultiplierLine(List<String> tooltip, IStorageWrapper wrapper, String tooltipKey) {
		double multiplier = StackUpgradeItem.getInventorySlotLimit(wrapper) / 64D;
		if (multiplier > 1) {
			tooltip.add(TextFormatting.GREEN + I18n.format(tooltipKey + ".stack_multiplier", TextFormatting.WHITE + new DecimalFormat("0.###").format(multiplier)));
		}
	}

	private static void addFluidLines(List<String> tooltip, IStorageWrapper wrapper, String tooltipKey) {
		wrapper.getFluidHandler().ifPresent(fluidHandler -> {
			for (IFluidTankProperties tank : fluidHandler.getTankProperties()) {
				FluidStack fluid = tank.getContents();
				if (fluid == null || fluid.amount <= 0) {
					tooltip.add(TextFormatting.BLUE + I18n.format(tooltipKey + ".fluid_empty"));
				} else {
					tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".fluid", TextFormatting.WHITE + CountAbbreviator.abbreviate(fluid.amount) + TextFormatting.GRAY,
							TextFormatting.BLUE + fluid.getLocalizedName()));
				}
			}
		});
	}

	/** The same items, of any slots, as one stack each, those of the most first. */
	private static List<ItemStack> getCompactedStacksSortedByCount(InventoryHandler inventory) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}
			ItemStack same = null;
			for (ItemStack stack : stacks) {
				if (ItemHandlerHelper.canItemStacksStack(stack, slotStack)) {
					same = stack;
					break;
				}
			}
			if (same == null) {
				stacks.add(slotStack.copy());
			} else {
				same.setCount((int) Math.min(Integer.MAX_VALUE, (long) same.getCount() + slotStack.getCount()));
			}
		}
		stacks.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
		return stacks;
	}

	private static int getStackWidth(FontRenderer font, ItemStack stack) {
		return Math.max(font.getStringWidth(CountAbbreviator.abbreviate(stack.getCount())) + COUNT_PADDING, DEFAULT_STACK_WIDTH);
	}

	@SubscribeEvent
	public static void onTooltipText(RenderTooltipEvent.PostText event) {
		Shown current = shown;
		if (current == null || event.getStack() != current.stack) {
			return;
		}
		List<String> lines = event.getLines();
		for (int i = 0; i < lines.size(); i++) {
			// as vanilla lays the lines out: two more below the first
			int lineY = event.getY() + i * 10 + (i > 0 ? 2 : 0);
			if (lines.get(i).startsWith(UPGRADES_MARKER)) {
				renderUpgrades(current.upgrades, event.getX(), lineY);
			} else if (lines.get(i).startsWith(INVENTORY_MARKER)) {
				renderContents(current.contents, event.getX(), lineY);
			}
		}
	}

	private static void renderUpgrades(List<IUpgradeWrapper> upgrades, int leftX, int topY) {
		int x = leftX;
		RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
		for (IUpgradeWrapper upgradeWrapper : upgrades) {
			if (upgradeWrapper.canBeDisabled()) {
				GuiHelper.blit(x, topY + 3, upgradeWrapper.isEnabled() ? UPGRADE_ON : UPGRADE_OFF);
				x += 4;
			}
			renderStack(renderItem, upgradeWrapper.getUpgradeStack(), x, topY, null);
			x += DEFAULT_STACK_WIDTH;
		}
	}

	private static void renderContents(List<ItemStack> contents, int leftX, int topY) {
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
		int x = leftX;
		for (int i = 0; i < contents.size(); i++) {
			if (i % MAX_STACKS_ON_LINE == 0) {
				x = leftX;
			}
			int y = topY + i / MAX_STACKS_ON_LINE * 20;
			ItemStack stack = contents.get(i);
			int stackWidth = getStackWidth(font, stack);
			renderStack(renderItem, stack, x + stackWidth - DEFAULT_STACK_WIDTH, y, CountAbbreviator.abbreviate(stack.getCount()));
			x += stackWidth;
		}
	}

	/** Over the tooltip's background, which is drawn without depth at the height vanilla gives it. */
	private static void renderStack(RenderItem renderItem, ItemStack stack, int x, int y, @Nullable String count) {
		float zLevel = renderItem.zLevel;
		renderItem.zLevel = 300;
		GlStateManager.enableDepth();
		RenderHelper.enableGUIStandardItemLighting();
		renderItem.renderItemAndEffectIntoGUI(stack, x, y);
		RenderHelper.disableStandardItemLighting();
		ItemStack shown = stack;
		if (count != null && stack.getCount() == 1) {
			count = null;
		}
		if (count != null) {
			shown = stack.copy();
			shown.setCount(1);
		}
		renderItem.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRenderer, shown, x, y, count);
		GlStateManager.disableDepth();
		renderItem.zLevel = zLevel;
	}

	private static final class Shown {
		private final ItemStack stack;
		private final List<IUpgradeWrapper> upgrades;
		private final List<ItemStack> contents;

		private Shown(ItemStack stack, List<IUpgradeWrapper> upgrades, List<ItemStack> contents) {
			this.stack = stack;
			this.upgrades = upgrades;
			this.contents = contents;
		}
	}
}
