package dev.feldsher.deconstructedcore.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Tabs in a column, of which one at a time is open over the others. */
public abstract class SettingsTabControl<C extends GuiContainer, T extends SettingsTabBase<C>> extends CompositeWidgetBase<Tab> {
	private static final int VERTICAL_SPACE = 1;
	@Nullable
	private T openTab = null;

	protected SettingsTabControl(Position position) {
		super(position, new Dimension(0, 0));
	}

	protected <U extends T> U addSettingsTab(Runnable onTabOpenContainerAction, Runnable onTabCloseContainerAction, U tab) {
		U settingsTab = addChild(tab);
		settingsTab.setHandlers(() -> {
					if (openTab != null && openTab != settingsTab) {
						openTab.close();
					}
					openTab = settingsTab;
					onTabOpenContainerAction.run();
				},
				() -> {
					if (openTab == settingsTab) {
						openTab = null;
						onTabCloseContainerAction.run();
					}
				},
				() -> openTab == null || openTab == settingsTab || isNotCovered(openTab, settingsTab, true),
				() -> openTab == null || isNotCovered(openTab, settingsTab, false)
		);
		return settingsTab;
	}

	@Override
	protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
		new ArrayList<>(children).forEach(child -> {
			if (child != openTab) {
				child.render(mouseX, mouseY, partialTicks);
			}
		});
		if (openTab != null) {
			// The other tabs' icons are items, drawn with depth above the flat tab: without it they'd show through
			GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
			openTab.render(mouseX, mouseY, partialTicks);
		}
	}

	/** The open tab is on top: it gets the click first. */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (openTab != null && openTab.isMouseOver(mouseX, mouseY)) {
			return openTab.mouseClicked(mouseX, mouseY, button);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean isNotCovered(T open, Tab t, boolean checkFullyCovered) {
		if (checkFullyCovered) {
			return open.getBottomY() < t.getBottomY() || open.getTopY() > t.getTopY();
		} else {
			return open.getBottomY() < t.getTopY() || open.getTopY() > t.getTopY();
		}
	}

	public Optional<T> getOpenTab() {
		return Optional.ofNullable(openTab);
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	@Override
	public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
		new ArrayList<>(children).forEach(tab -> tab.renderTooltip(screen, mouseX, mouseY));
	}

	protected int getTopY() {
		return y + children.size() * (Tab.DEFAULT_HEIGHT + VERTICAL_SPACE);
	}

	@Override
	public int getHeight() {
		int maxY = y;
		for (Tab tab : children) {
			maxY = Math.max(maxY, tab.getBottomY());
		}
		return maxY - y;
	}

	@Override
	public int getWidth() {
		int maxWidth = 0;
		for (Tab tab : children) {
			maxWidth = Math.max(maxWidth, tab.getWidth());
		}
		return maxWidth;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		for (Tab tab : children) {
			if (tab.getTabRectangle().contains(mouseX, mouseY)) {
				return true;
			}
		}
		return false;
	}

	public List<Rect2i> getTabRectangles() {
		List<Rect2i> ret = new ArrayList<>();
		children.forEach(child -> ret.add(child.getTabRectangle()));
		return ret;
	}
}
