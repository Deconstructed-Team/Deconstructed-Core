package dev.feldsher.deconstructedcore.storage;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * One storage's contents, the one live copy of them: every stack or block with the storage's UUID works on this.
 * Handlers built on the contents are kept here too, so that two stacks with the same UUID share them, and with them
 * what's in the storage at any moment, instead of each holding a copy of its own.
 */
public final class StorageContents {
	private final NBTTagCompound nbt;
	private final Map<String, Object> shared = new HashMap<>();
	private final Map<Object, WeakReference<Runnable>> slotChangeListeners = new WeakHashMap<>();
	private boolean dirty;
	// the manager keeps changed contents strongly till they're saved: it's told as they change
	@Nullable
	private Runnable onDirty = null;

	StorageContents(NBTTagCompound nbt) {
		this.nbt = nbt;
	}

	void setOnDirty(@Nullable Runnable onDirty) {
		this.onDirty = onDirty;
	}

	public NBTTagCompound getNbt() {
		return nbt;
	}

	/**
	 * What the contents' users share: built by the first that asks, as long as these contents are loaded.
	 * Not computeIfAbsent: building one may ask for another, as an inventory asks the upgrades for its slot limit.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getShared(String key, Supplier<T> factory) {
		Object value = shared.get(key);
		if (value == null) {
			value = factory.get();
			Object built = shared.putIfAbsent(key, value);
			if (built != null) {
				value = built;
			}
		}
		return (T) value;
	}

	public void removeShared(String key) {
		shared.remove(key);
	}

	/**
	 * Tells an owner, such as a placed storage that updates comparators, when a slot changes, whichever stack or block
	 * changed it. Held weakly, both: the owner must keep the listener, and a gone owner is dropped.
	 */
	public void addSlotChangeListener(Object owner, Runnable listener) {
		slotChangeListeners.put(owner, new WeakReference<>(listener));
	}

	public void onSlotChanged() {
		markDirty();
		for (WeakReference<Runnable> listener : new ArrayList<>(slotChangeListeners.values())) {
			Runnable runnable = listener.get();
			if (runnable != null) {
				runnable.run();
			}
		}
	}

	public void markDirty() {
		if (!dirty) {
			dirty = true;
			if (onDirty != null) {
				onDirty.run();
			}
		}
	}

	boolean isDirty() {
		return dirty;
	}

	void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
}
