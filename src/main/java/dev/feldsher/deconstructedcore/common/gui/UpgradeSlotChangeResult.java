package dev.feldsher.deconstructedcore.common.gui;

import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public final class UpgradeSlotChangeResult {
	private static final UpgradeSlotChangeResult SUCCESS = new UpgradeSlotChangeResult(true, null, Collections.emptySet(), Collections.emptySet());

	private final boolean successful;
	@Nullable
	private final ITextComponent errorMessage;
	private final Set<Integer> errorUpgradeSlots;
	private final Set<Integer> errorInventorySlots;

	private UpgradeSlotChangeResult(boolean successful, @Nullable ITextComponent errorMessage, Set<Integer> errorUpgradeSlots, Set<Integer> errorInventorySlots) {
		this.successful = successful;
		this.errorMessage = errorMessage;
		this.errorUpgradeSlots = errorUpgradeSlots;
		this.errorInventorySlots = errorInventorySlots;
	}

	public boolean successful() {
		return successful;
	}

	public Optional<ITextComponent> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}

	public Set<Integer> errorUpgradeSlots() {
		return errorUpgradeSlots;
	}

	public Set<Integer> errorInventorySlots() {
		return errorInventorySlots;
	}

	public static UpgradeSlotChangeResult fail(ITextComponent errorMessage, Set<Integer> errorUpgradeSlots, Set<Integer> errorInventorySlots) {
		return new UpgradeSlotChangeResult(false, errorMessage, errorUpgradeSlots, errorInventorySlots);
	}

	public static UpgradeSlotChangeResult success() {
		return SUCCESS;
	}
}
