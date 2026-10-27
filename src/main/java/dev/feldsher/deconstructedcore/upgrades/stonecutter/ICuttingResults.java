package dev.feldsher.deconstructedcore.upgrades.stonecutter;

import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;
import java.util.List;

/** What a cutting upgrade makes of one input item, one for one, and the sound it makes doing it. */
public interface ICuttingResults {
	/** Each a copy, in the order to show them. */
	List<ItemStack> getResults(ItemStack input);

	@Nullable
	SoundEvent getSound(ItemStack input);
}
