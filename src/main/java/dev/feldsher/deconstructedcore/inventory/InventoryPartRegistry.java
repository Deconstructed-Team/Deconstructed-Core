package dev.feldsher.deconstructedcore.inventory;

import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The inventory parts by name, as they're saved, to make them again when the inventory loads. */
public class InventoryPartRegistry {
	private InventoryPartRegistry() {}

	private static final Map<String, IInventoryPartHandler.Factory> INVENTORY_PART_FACTORIES = new HashMap<>();

	static {
		registerFactory(IInventoryPartHandler.Default.NAME, (parent, slotRange, getMemorySettings) -> new IInventoryPartHandler.Default(parent, slotRange.numberOfSlots()));
	}

	public static void registerFactory(String name, IInventoryPartHandler.Factory factory) {
		INVENTORY_PART_FACTORIES.put(name, factory);
	}

	/** A part saved by a mod that's gone, or by an upgrade not loaded yet, falls back to a plain one. */
	public static IInventoryPartHandler instantiatePart(String name, InventoryHandler parent, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings) {
		IInventoryPartHandler.Factory factory = INVENTORY_PART_FACTORIES.get(name);
		if (factory == null) {
			return new IInventoryPartHandler.Default(parent, slotRange.numberOfSlots());
		}
		return factory.create(parent, slotRange, getMemorySettings);
	}
}
