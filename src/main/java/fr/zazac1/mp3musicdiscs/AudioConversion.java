package fr.zazac1.mp3musicdiscs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Keeps the source MP3 safely in the mod cache and produces Ogg Vorbis for Minecraft's native sound pipeline. */
public final class AudioConversion {
    private AudioConversion() {}

    public static void importAndConvert(DiscDefinition disc, Path selectedMp3) {
        try {
            Path cachedMp3 = DiscLibrary.audioDirectory().resolve(disc.id + ".mp3");
            Files.copy(selectedMp3, cachedMp3, StandardCopyOption.REPLACE_EXISTING);
            disc.source_mp3 = cachedMp3.toAbsolutePath().toString();
            disc.audio_ogg = DiscLibrary.audioDirectory().resolve(disc.id + ".ogg").toAbsolutePath().toString();
            disc.audio_status = "converting";
            disc.audio_error = "";
            DiscLibrary.save();
            Thread converter = new Thread(() -> convert(disc), "mp3musicdiscs-ogg-converter");
            converter.setDaemon(true);
            converter.start();
        } catch (IOException error) {
            disc.audio_status = "import_failed";
            disc.audio_error = error.getMessage() == null ? "Could not copy MP3" : error.getMessage();
            DiscLibrary.save();
        }
    }

    private static void convert(DiscDefinition disc) {
        try {
            Process process = new ProcessBuilder(List.of("ffmpeg", "-y", "-v", "error", "-i", disc.source_mp3,
                    "-map_metadata", "-1", "-vn", "-c:a", "libvorbis", "-q:a", "5", disc.audio_ogg))
                    .redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() == 0 && Files.isRegularFile(Path.of(disc.audio_ogg))) {
                disc.audio_status = "ready";
                disc.audio_error = "";
            } else {
                disc.audio_status = "mp3_fallback";
                disc.audio_error = output.isBlank() ? "Ogg encoder unavailable; using local MP3 playback." : output.strip();
            }
        } catch (Exception error) {
            // The embedded Java decoder still lets an imported MP3 play while the release encoder is unavailable.
            disc.audio_status = "mp3_fallback";
            disc.audio_error = "Ogg encoder unavailable; using local MP3 playback.";
        }
        DiscLibrary.save();
    }
}
