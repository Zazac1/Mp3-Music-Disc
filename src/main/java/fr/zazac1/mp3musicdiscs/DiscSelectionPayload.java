package fr.zazac1.mp3musicdiscs;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to imprint the blank disc held in either hand. */
public record DiscSelectionPayload(String discId, boolean offHand) implements CustomPacketPayload {
    public static final Type<DiscSelectionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Mp3MusicDiscsMod.MOD_ID, "select_disc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DiscSelectionPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.discId, 64);
                buffer.writeBoolean(payload.offHand);
            },
            buffer -> new DiscSelectionPayload(buffer.readUtf(64), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
