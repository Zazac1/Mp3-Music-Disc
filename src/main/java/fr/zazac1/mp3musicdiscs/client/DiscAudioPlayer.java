package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscStacks;
import javazoom.jl.player.Player;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local fallback while the generated Ogg is registered with Minecraft's resource audio system. */
@Environment(EnvType.CLIENT)
final class DiscAudioPlayer {
    private static BlockPos pendingPosition;
    private static DiscDefinition pendingDisc;
    private static int pendingTicks;
    private static BlockPos playingPosition;
    private static Player activePlayer;

    private DiscAudioPlayer() {}

    static void queueFromJukebox(ItemStack stack, BlockPos position) {
        DiscDefinition disc = DiscStacks.find(stack);
        if (disc == null || disc.source_mp3.isBlank()) return;
        pendingDisc = disc;
        pendingPosition = position.immutable();
        pendingTicks = 3;
    }

    static void tick(Minecraft client) {
        if (pendingTicks > 0 && --pendingTicks == 0) {
            if (client.level != null && client.level.getBlockState(pendingPosition).hasProperty(JukeboxBlock.HAS_RECORD)
                    && client.level.getBlockState(pendingPosition).getValue(JukeboxBlock.HAS_RECORD)) start(pendingDisc, pendingPosition);
            pendingDisc = null;
            pendingPosition = null;
        }
        if (playingPosition != null && (client.level == null || !client.level.getBlockState(playingPosition).hasProperty(JukeboxBlock.HAS_RECORD)
                || !client.level.getBlockState(playingPosition).getValue(JukeboxBlock.HAS_RECORD))) stop();
    }

    private static void start(DiscDefinition disc, BlockPos position) {
        stop();
        Path source = Path.of(disc.source_mp3);
        if (!Files.isRegularFile(source)) return;
        playingPosition = position;
        Thread playback = new Thread(() -> {
            try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(source))) {
                Player player = new Player(stream);
                synchronized (DiscAudioPlayer.class) { activePlayer = player; }
                player.play();
            } catch (Exception ignored) {
                // A broken audio file simply remains silent; the editor keeps the conversion error/status.
            } finally {
                synchronized (DiscAudioPlayer.class) { activePlayer = null; }
            }
        }, "mp3musicdiscs-playback");
        playback.setDaemon(true);
        playback.start();
    }

    private static synchronized void stop() {
        if (activePlayer != null) activePlayer.close();
        activePlayer = null;
        playingPosition = null;
    }
}
