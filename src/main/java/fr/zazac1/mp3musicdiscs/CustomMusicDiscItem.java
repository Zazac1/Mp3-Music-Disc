package fr.zazac1.mp3musicdiscs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

public final class CustomMusicDiscItem extends Item {
    public CustomMusicDiscItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        DiscDefinition disc = DiscStacks.find(stack);
        return disc == null ? Component.translatable("item.mp3musicdiscs.custom_music_disc") : Component.literal(disc.name);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        DiscDefinition disc = DiscStacks.find(stack);
        if (disc != null && !disc.description.isBlank()) lines.accept(Component.literal(disc.description).withStyle(ChatFormatting.GRAY));
        if (disc != null && !disc.audio_status.equals("ready")) lines.accept(Component.literal("Audio: " + disc.audio_status).withStyle(ChatFormatting.YELLOW));
    }
}
