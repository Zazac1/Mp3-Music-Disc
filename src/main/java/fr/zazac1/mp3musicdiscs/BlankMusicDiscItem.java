package fr.zazac1.mp3musicdiscs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A reusable blank which is turned into a saved custom disc from the client selection screen. */
public final class BlankMusicDiscItem extends Item {
    public BlankMusicDiscItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal("Right-click in the air to choose a track.").withStyle(ChatFormatting.GRAY));
    }
}
