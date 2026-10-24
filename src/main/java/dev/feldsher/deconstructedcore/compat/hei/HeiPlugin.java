package dev.feldsher.deconstructedcore.compat.hei;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Rect2i;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;
import net.minecraft.client.gui.inventory.GuiContainer;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HEI keeps its item list off what storage screens draw outside of their main area: the upgrade column, the tabs and
 * the sort buttons. Found by HEI's annotation scan, so nothing loads it without HEI.
 */
@JEIPlugin
public class HeiPlugin implements IModPlugin {
	@Override
	public void register(IModRegistry registry) {
		registry.addAdvancedGuiHandlers(
				new ExtraAreasHandler<>(StorageScreenBase.class, StorageScreenBase::getExtraAreas),
				new ExtraAreasHandler<>(SettingsScreen.class, SettingsScreen::getExtraAreas));
	}

	private static class ExtraAreasHandler<T extends GuiContainer> implements IAdvancedGuiHandler<T> {
		private final Class<T> screenClass;
		private final Function<T, List<Rect2i>> getExtraAreas;

		ExtraAreasHandler(Class<T> screenClass, Function<T, List<Rect2i>> getExtraAreas) {
			this.screenClass = screenClass;
			this.getExtraAreas = getExtraAreas;
		}

		@Override
		public Class<T> getGuiContainerClass() {
			return screenClass;
		}

		@Override
		public List<Rectangle> getGuiExtraAreas(T screen) {
			return getExtraAreas.apply(screen).stream().map(r -> new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight())).collect(Collectors.toList());
		}
	}
}
