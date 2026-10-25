package dev.feldsher.deconstructedcore.settings;

import dev.feldsher.deconstructedcore.DeconstructedCore;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Settings templates from snbt files, which every player can load: what a pack ships in
 * {@code config/deconstructedcore/settings_templates/<source>/}, and what players exported, in the same folders under
 * the world's. 1.12 has no data packs to bring them, so the server reads them when it starts and sends them to the
 * client with the player's own templates.
 */
public final class FileSettingsTemplates {
	public static final String SUFFIX = ".snbt";
	private static final String TEMPLATES_DIRECTORY = DeconstructedCore.MOD_ID + "/settings_templates";
	private static final FileSettingsTemplates SERVER = new FileSettingsTemplates();
	private static final FileSettingsTemplates CLIENT_COPY = new FileSettingsTemplates();

	/** By source, then by template name, both sorted, so that the server and the client list them alike. */
	private final Map<String, Map<String, NBTTagCompound>> templates = new TreeMap<>();

	private FileSettingsTemplates() {}

	public static FileSettingsTemplates get(boolean remote) {
		return remote ? CLIENT_COPY : SERVER;
	}

	public Map<String, Map<String, NBTTagCompound>> getTemplates() {
		return templates;
	}

	public Optional<NBTTagCompound> getTemplateNbt(String source, String templateName) {
		Map<String, NBTTagCompound> sourceTemplates = templates.get(source);
		return sourceTemplates == null ? Optional.empty() : Optional.ofNullable(sourceTemplates.get(templateName));
	}

	/** The file name, with spaces for underscores and its words capitalized, is the template's name. */
	public void putTemplate(String source, String fileName, NBTTagCompound tag) {
		templates.computeIfAbsent(source, s -> new TreeMap<>()).put(toTemplateName(fileName), tag);
	}

	public void setAll(Map<String, Map<String, NBTTagCompound>> allTemplates) {
		templates.clear();
		allTemplates.forEach((source, sourceTemplates) -> templates.put(source, new TreeMap<>(sourceTemplates)));
	}

	private static String toTemplateName(String fileName) {
		StringBuilder builder = new StringBuilder();
		for (String word : fileName.replace('_', ' ').split("\\s+")) {
			if (!word.isEmpty()) {
				builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1)).append(' ');
			}
		}
		return builder.toString().trim();
	}

	/** On server start: the pack's templates, then the world's. */
	public static void loadServerTemplates() {
		SERVER.templates.clear();
		SERVER.loadFrom(new File(Loader.instance().getConfigDir(), TEMPLATES_DIRECTORY).toPath());
		Path worldTemplatesDir = getWorldTemplatesDir();
		if (worldTemplatesDir != null) {
			SERVER.loadFrom(worldTemplatesDir);
		}
	}

	@Nullable
	public static Path getWorldTemplatesDir() {
		File worldDir = DimensionManager.getCurrentSaveRootDirectory();
		return worldDir == null ? null : new File(worldDir, TEMPLATES_DIRECTORY).toPath();
	}

	private void loadFrom(Path templatesDir) {
		if (!Files.isDirectory(templatesDir)) {
			return;
		}
		try (DirectoryStream<Path> sources = Files.newDirectoryStream(templatesDir, Files::isDirectory)) {
			for (Path sourceDir : sources) {
				loadSource(sourceDir.getFileName().toString(), sourceDir);
			}
		} catch (IOException e) {
			DeconstructedCore.LOGGER.error("Couldn't list settings templates in {}", templatesDir, e);
		}
	}

	private void loadSource(String source, Path sourceDir) throws IOException {
		try (DirectoryStream<Path> files = Files.newDirectoryStream(sourceDir, "*" + SUFFIX)) {
			for (Path file : files) {
				String fileName = file.getFileName().toString();
				try {
					NBTTagCompound tag = JsonToNBT.getTagFromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
					putTemplate(source, fileName.substring(0, fileName.length() - SUFFIX.length()), tag);
				} catch (IOException | NBTException e) {
					DeconstructedCore.LOGGER.error("Couldn't parse settings template {}", file, e);
				}
			}
		}
	}
}
