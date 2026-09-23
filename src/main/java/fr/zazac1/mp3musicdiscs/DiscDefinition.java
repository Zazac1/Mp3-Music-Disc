package fr.zazac1.mp3musicdiscs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JSON-serializable, player-authored disc data. Audio always remains a local imported file until synced. */
public final class DiscDefinition {
    public String id = UUID.randomUUID().toString();
    public String name = "Untitled Disc";
    public String description = "A custom MP3 music disc";
    public String source_mp3 = "";
    /** Cached Ogg Vorbis file. Minecraft resource playback uses this, never the original MP3. */
    public String audio_ogg = "";
    public String audio_status = "not_converted";
    public String audio_error = "";
    public Appearance appearance = new Appearance();
    public List<LootPlacement> loot = new ArrayList<>();

    public static final class Appearance {
        public int disc_color = 0xFF42505B;
        public int label_color = 0xFFE7D2A0;
        public int zone_color = 0xFF65B8B0;
        public String pattern = "rings";
        public boolean overlay = true;
    }

    public static final class LootPlacement {
        public String table = "";
        public boolean enabled = true;
        public int weight = 1;
        public int min_count = 1;
        public int max_count = 1;
    }
}
