package dev.feldsher.deconstructedcore.upgrades.cooking;

import dev.feldsher.deconstructedcore.common.gui.SlotSuppliedHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The cooking slots, and the times the tab draws its progress with, read from the upgrade the client has. */
public class CookingLogicContainer {
	private final Supplier<CookingLogic> supplyCookingLogic;
	private final List<Slot> cookingSlots = new ArrayList<>();

	public CookingLogicContainer(Supplier<CookingLogic> supplyCookingLogic, Consumer<Slot> addSlot) {
		this.supplyCookingLogic = supplyCookingLogic;

		addCookingSlot(addSlot, new CookingSlot(() -> supplyCookingLogic.get().getCookingInventory(), CookingLogic.COOK_INPUT_SLOT));
		addCookingSlot(addSlot, new CookingSlot(() -> supplyCookingLogic.get().getCookingInventory(), CookingLogic.FUEL_SLOT));
		addCookingSlot(addSlot, new SlotSuppliedHandler(() -> supplyCookingLogic.get().getCookingInventory(), CookingLogic.COOK_OUTPUT_SLOT, -100, -100) {
			@Override
			public boolean isItemValid(@Nonnull ItemStack stack) {
				return false;
			}
		});
	}

	private void addCookingSlot(Consumer<Slot> addSlot, Slot slot) {
		addSlot.accept(slot);
		cookingSlots.add(slot);
	}

	public int getBurnTimeTotal() {
		return supplyCookingLogic.get().getBurnTimeTotal();
	}

	public long getBurnTimeFinish() {
		return supplyCookingLogic.get().getBurnTimeFinish();
	}

	public long getCookTimeFinish() {
		return supplyCookingLogic.get().getCookTimeFinish();
	}

	public int getCookTimeTotal() {
		return supplyCookingLogic.get().getCookTimeTotal();
	}

	public boolean isCooking() {
		return supplyCookingLogic.get().isCooking();
	}

	public boolean isBurning(World world) {
		return supplyCookingLogic.get().isBurning(world);
	}

	public List<Slot> getCookingSlots() {
		return cookingSlots;
	}

	private static class CookingSlot extends SlotSuppliedHandler {
		private CookingSlot(Supplier<IItemHandler> itemHandlerSupplier, int slot) {
			super(itemHandlerSupplier, slot, -100, -100);
		}

		/** Forge's limit tries the stack in the emptied slot, which the slot's own check may turn down. */
		@Override
		public int getItemStackLimit(@Nonnull ItemStack stack) {
			return stack.getMaxStackSize();
		}
	}
}
