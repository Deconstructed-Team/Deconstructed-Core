package dev.feldsher.deconstructedcore.storage;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.zip.GZIPOutputStream;

/**
 * The contents of one kind of storage, by UUID; the storage's stack or block carries only the UUID.
 * <p>
 * On the server each storage's contents are a file of their own, {@code data/<folder>/<uuid>.dat} in the world:
 * loaded when first asked for, written when the world saves if they changed, through a temporary file so that a
 * crash mid-write leaves the old one whole. A storage nobody uses is never read, and a change to one rewrites only it.
 * <p>
 * The client has no files: it has the contents the server sent it, of the storages it opened.
 */
@Mod.EventBusSubscriber(modid = DeconstructedCore.MOD_ID)
public final class StorageContentsManager {
	private static final List<StorageContentsManager> MANAGERS = new ArrayList<>();

	private final String folder;
	private final Map<UUID, StorageContents> serverContents = new HashMap<>();
	private final Map<UUID, StorageContents> clientContents = new HashMap<>();
	/** Those of the client's contents the server sent; the others it made up, empty, when asked for them. */
	private final Set<UUID> clientContentsReceived = new HashSet<>();
	@Nullable
	private File storageDir;

	private StorageContentsManager(String folder) {
		this.folder = folder;
	}

	/** @param folder the folder in the world's data folder that the contents are saved in, e.g. the mod's id */
	public static synchronized StorageContentsManager create(String folder) {
		StorageContentsManager manager = new StorageContentsManager(folder);
		MANAGERS.add(manager);
		return manager;
	}

	private Map<UUID, StorageContents> contentsOnSide() {
		return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER ? serverContents : clientContents;
	}

	/** The storage's contents, loaded or new: never null. */
	public StorageContents getOrCreate(UUID uuid) {
		Map<UUID, StorageContents> contents = contentsOnSide();
		StorageContents storageContents = contents.get(uuid);
		if (storageContents == null) {
			storageContents = contents == serverContents && ensureStorageDir() ? load(uuid) : null;
			if (storageContents == null) {
				storageContents = new StorageContents(new NBTTagCompound());
				storageContents.markDirty();
			}
			contents.put(uuid, storageContents);
		}
		return storageContents;
	}

	/** The client's copy of a storage's contents, as the server sent them: they replace what the client had, and its handlers. */
	public void setClientContents(UUID uuid, NBTTagCompound nbt) {
		clientContents.put(uuid, new StorageContents(nbt));
		clientContentsReceived.add(uuid);
	}

	public boolean hasReceivedClientContents(UUID uuid) {
		return clientContentsReceived.contains(uuid);
	}

	/** A storage that's gone: its contents, and on the server their file. */
	public void remove(UUID uuid) {
		Map<UUID, StorageContents> contents = contentsOnSide();
		contents.remove(uuid);
		if (contents == clientContents) {
			clientContentsReceived.remove(uuid);
		}
		if (contents == serverContents && ensureStorageDir()) {
			File file = getFile(uuid);
			if (file.exists() && !file.delete()) {
				DeconstructedCore.LOGGER.error("Failed to delete storage contents {}", file.getAbsolutePath());
			}
		}
	}

	/**
	 * On the server, removes every storage's contents, loaded or only in their file, that the predicate matches.
	 * Files are read only to be tested, not kept loaded.
	 *
	 * @return how many were removed
	 */
	public int removeMatching(BiPredicate<UUID, NBTTagCompound> shouldRemove) {
		Set<UUID> uuids = new HashSet<>(serverContents.keySet());
		File[] files = ensureStorageDir() ? storageDir.listFiles((dir, name) -> name.endsWith(".dat")) : null;
		if (files != null) {
			for (File file : files) {
				try {
					uuids.add(UUID.fromString(file.getName().substring(0, file.getName().length() - ".dat".length())));
				} catch (IllegalArgumentException e) {
					// not one of ours
				}
			}
		}

		int removed = 0;
		for (UUID uuid : uuids) {
			StorageContents contents = serverContents.get(uuid);
			if (contents == null) {
				contents = load(uuid);
			}
			if (contents != null && shouldRemove.test(uuid, contents.getNbt())) {
				remove(uuid);
				removed++;
			}
		}
		return removed;
	}

	/**
	 * A manager made after the overworld loaded, as one made when its mod's class is first used, missed the load event:
	 * it finds the world's folder from the running server.
	 */
	private boolean ensureStorageDir() {
		if (storageDir == null) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			World overworld = server == null ? null : server.getWorld(0);
			if (overworld != null) {
				storageDir = getStorageDir(overworld);
			}
		}
		return storageDir != null;
	}

	private File getStorageDir(World overworld) {
		return new File(new File(overworld.getSaveHandler().getWorldDirectory(), "data"), folder);
	}

	@Nullable
	private StorageContents load(UUID uuid) {
		if (storageDir == null) {
			return null;
		}
		File file = getFile(uuid);
		if (!file.exists()) {
			return null;
		}
		try (InputStream stream = new FileInputStream(file)) {
			return new StorageContents(CompressedStreamTools.readCompressed(stream));
		} catch (IOException e) {
			DeconstructedCore.LOGGER.error("Failed to load storage contents {}", file.getAbsolutePath(), e);
			return null;
		}
	}

	private void saveAll() {
		if (!ensureStorageDir()) {
			return;
		}
		if (!storageDir.exists() && !storageDir.mkdirs()) {
			DeconstructedCore.LOGGER.error("Failed to create storage contents folder {}", storageDir.getAbsolutePath());
			return;
		}
		for (Map.Entry<UUID, StorageContents> entry : serverContents.entrySet()) {
			StorageContents contents = entry.getValue();
			if (contents.isDirty() && save(entry.getKey(), contents)) {
				contents.setDirty(false);
			}
		}
	}

	private boolean save(UUID uuid, StorageContents contents) {
		Path target = getFile(uuid).toPath();
		Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
		try {
			writeSynced(contents.getNbt(), tmp.toFile());
			try {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			DeconstructedCore.LOGGER.error("Failed to save storage contents {}", target, e);
			try {
				Files.deleteIfExists(tmp);
			} catch (IOException deleteException) {
				DeconstructedCore.LOGGER.error("Failed to delete {}", tmp, deleteException);
			}
			return false;
		}
	}

	/**
	 * As CompressedStreamTools.writeCompressed, but on the disk before it returns: renamed over the old file unsynced,
	 * the new one could point at blocks a power cut never wrote. writeCompressed closes the stream, so the gzip is
	 * written here, and synced before the close.
	 */
	private static void writeSynced(NBTTagCompound nbt, File file) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			GZIPOutputStream zip = new GZIPOutputStream(out);
			DataOutputStream data = new DataOutputStream(new BufferedOutputStream(zip));
			try {
				CompressedStreamTools.write(nbt, data);
				data.flush();
				zip.finish();
				out.getFD().sync();
			} finally {
				// releases the deflater's native memory
				data.close();
			}
		}
	}

	private File getFile(UUID uuid) {
		return new File(storageDir, uuid + ".dat");
	}

	@SubscribeEvent
	public static void onWorldLoad(WorldEvent.Load event) {
		World world = event.getWorld();
		synchronized (StorageContentsManager.class) {
			for (StorageContentsManager manager : MANAGERS) {
				if (world.isRemote) {
					manager.clientContents.clear();
					manager.clientContentsReceived.clear();
				} else if (world.provider.getDimension() == 0) {
					manager.serverContents.clear();
					manager.storageDir = manager.getStorageDir(world);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onWorldSave(WorldEvent.Save event) {
		if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
			synchronized (StorageContentsManager.class) {
				MANAGERS.forEach(StorageContentsManager::saveAll);
			}
		}
	}

	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload event) {
		if (!event.getWorld().isRemote && event.getWorld().provider.getDimension() == 0) {
			synchronized (StorageContentsManager.class) {
				for (StorageContentsManager manager : MANAGERS) {
					manager.saveAll();
					manager.serverContents.clear();
					manager.storageDir = null;
				}
			}
		}
	}
}
