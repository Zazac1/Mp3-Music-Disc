package fr.zazac1.mp3musicdiscs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import fr.zazac1.mp3musicdiscs.Mp3MusicDiscsMod;
import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscStacks;

@Environment(EnvType.CLIENT)
public final class Mp3MusicDiscsClient implements ClientModInitializer {
    static void refreshInventoryAppearance(DiscDefinition disc) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        for (net.minecraft.world.item.ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (DiscStacks.id(stack).equals(disc.id)) DiscStacks.applyAppearance(stack, disc);
        }
    }

    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() && player.getItemInHand(hand).is(Mp3MusicDiscsMod.EMPTY_MUSIC_DISC)) {
                Minecraft client = Minecraft.getInstance();
                client.gui.setScreen(new DiscSelectionScreen(null, player, hand));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ClientPlayNetworking.registerGlobalReceiver(fr.zazac1.mp3musicdiscs.OpenMusicScreenPayload.TYPE,
                (payload, context) -> context.client().gui.setScreen(
                        MusicDiscsScreen.fromServerCommand(context.client().gui.screen())));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() && player.getItemInHand(hand).is(Mp3MusicDiscsMod.CUSTOM_MUSIC_DISC))
                DiscAudioPlayer.queueFromJukebox(player.getItemInHand(hand), hit.getBlockPos());
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(DiscAudioPlayer::tick);
    }
}
