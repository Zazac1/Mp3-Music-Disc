package fr.zazac1.mp3musicdiscs;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Permission-checked server request to open the client-side personal library. */
public record OpenMusicScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenMusicScreenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Mp3MusicDiscsMod.MOD_ID, "open_music_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMusicScreenPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> { }, buffer -> new OpenMusicScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
