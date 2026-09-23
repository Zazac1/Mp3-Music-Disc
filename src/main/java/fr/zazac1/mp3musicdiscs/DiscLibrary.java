package fr.zazac1.mp3musicdiscs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One readable config file is managed exclusively by the GUI. */
public final class DiscLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mp3musicdiscs.json");
    private static final Path AUDIO_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("mp3musicdiscs").resolve("audio");
    private static List<DiscDefinition> discs;

    private DiscLibrary() {}

    public static List<DiscDefinition> get() {
        if (discs == null) discs = load();
        return discs;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(get()));
        } catch (IOException error) {
            Mp3MusicDiscsMod.LOGGER.error("Could not save music disc library", error);
        }
    }

    public static Path audioDirectory() {
        try { Files.createDirectories(AUDIO_DIRECTORY); }
        catch (IOException error) { Mp3MusicDiscsMod.LOGGER.error("Could not create music disc audio cache", error); }
        return AUDIO_DIRECTORY;
    }

    private static List<DiscDefinition> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try {
            DiscDefinition[] values = GSON.fromJson(Files.readString(FILE), DiscDefinition[].class);
            List<DiscDefinition> result = new ArrayList<>();
            if (values != null) for (DiscDefinition value : values) if (value != null) result.add(normalize(value));
            return result;
        } catch (Exception error) {
            Mp3MusicDiscsMod.LOGGER.warn("Could not read music disc library; using an empty library", error);
            return new ArrayList<>();
        }
    }

    public static DiscDefinition normalize(DiscDefinition disc) {
        if (disc.id == null || disc.id.isBlank()) disc.id = java.util.UUID.randomUUID().toString();
        if (disc.name == null) disc.name = "Untitled Disc";
        if (disc.description == null) disc.description = "";
        if (disc.source_mp3 == null) disc.source_mp3 = "";
        if (disc.audio_ogg == null) disc.audio_ogg = "";
        if (disc.audio_status == null) disc.audio_status = "not_converted";
        if (disc.audio_error == null) disc.audio_error = "";
        if (disc.appearance == null) disc.appearance = new DiscDefinition.Appearance();
        if (disc.appearance.pattern == null) disc.appearance.pattern = "rings";
        if (disc.loot == null) disc.loot = new ArrayList<>();
        return disc;
    }
}
