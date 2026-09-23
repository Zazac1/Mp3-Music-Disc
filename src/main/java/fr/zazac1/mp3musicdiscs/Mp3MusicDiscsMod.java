package fr.zazac1.mp3musicdiscs;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A blank disc becomes a data-carrying playable disc after a track is chosen. */
public final class Mp3MusicDiscsMod implements ModInitializer {
    public static final String MOD_ID = "mp3musicdiscs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final SoundEvent PLACEHOLDER_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(MOD_ID, "music_disc_placeholder"), SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(MOD_ID, "music_disc_placeholder")));
    public static final Item CUSTOM_MUSIC_DISC = RegistryHolder.registerDisc();
    public static final Item EMPTY_MUSIC_DISC = RegistryHolder.registerEmptyDisc();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(DiscSelectionPayload.TYPE, DiscSelectionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenMusicScreenPayload.TYPE, OpenMusicScreenPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DiscSelectionPayload.TYPE, (payload, context) -> {
            InteractionHand hand = payload.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!context.player().getItemInHand(hand).is(EMPTY_MUSIC_DISC)) return;
            DiscLibrary.get().stream().filter(disc -> disc.id.equals(payload.discId())).findFirst()
                    .ifPresent(disc -> context.player().setItemInHand(hand, DiscStacks.create(disc)));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(EMPTY_MUSIC_DISC);
            for (DiscDefinition disc : DiscLibrary.get()) entries.accept(DiscStacks.create(disc));
        });
        LootInjection.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("mp3musicdiscs")
                        // Local worlds are owned by their player; dedicated servers require a gamemaster.
                        .requires(source -> environment != Commands.CommandSelection.DEDICATED
                                || source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(context -> openMusicScreen(context.getSource()))
        ));
        LOGGER.info("MP3 Music Discs initialized.");
    }

    private static int openMusicScreen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServerPlayNetworking.canSend(player, OpenMusicScreenPayload.TYPE)) {
            source.sendFailure(Component.literal("MP3 Music Discs must also be installed on your client."));
            return 0;
        }
        ServerPlayNetworking.send(player, new OpenMusicScreenPayload());
        source.sendSuccess(() -> Component.literal("Opening MP3 Music Discs…"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static final class RegistryHolder {
        private static Item registerDisc() {
            Identifier itemId = Identifier.fromNamespaceAndPath(MOD_ID, "custom_music_disc");
            return Registry.register(BuiltInRegistries.ITEM,
                    itemId,
                    new CustomMusicDiscItem(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, itemId))
                            .stacksTo(1)
                            .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,
                                    Identifier.fromNamespaceAndPath(MOD_ID, "custom_disc")))));
        }

        private static Item registerEmptyDisc() {
            Identifier itemId = Identifier.fromNamespaceAndPath(MOD_ID, "empty_music_disc");
            return Registry.register(BuiltInRegistries.ITEM, itemId,
                    new BlankMusicDiscItem(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, itemId))
                            .stacksTo(1)));
        }
    }
}
