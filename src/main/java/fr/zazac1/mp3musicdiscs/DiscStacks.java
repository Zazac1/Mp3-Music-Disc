package fr.zazac1.mp3musicdiscs;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** The ID travels with the stack, allowing one registered item to represent every player-created disc. */
public final class DiscStacks {
    public static final String DISC_ID = "mp3musicdiscs:disc_id";

    private DiscStacks() {}

    public static ItemStack create(DiscDefinition disc) {
        ItemStack stack = new ItemStack(Mp3MusicDiscsMod.CUSTOM_MUSIC_DISC);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(DISC_ID, disc.id));
        applyAppearance(stack, disc);
        return stack;
    }

    /** Refreshes the model data of an existing stack after its source disc is edited. */
    public static void applyAppearance(ItemStack stack, DiscDefinition disc) {
        // Item models use these three tint slots plus the pattern key. This makes the
        // inventory/hand texture mirror the appearance configured in the editor.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(), List.of(), List.of(disc.appearance.pattern),
                List.of(disc.appearance.disc_color, disc.appearance.zone_color, disc.appearance.zone_color, disc.appearance.label_color)));
    }

    public static String id(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        CompoundTag tag = data.copyTag();
        return tag.getString(DISC_ID).orElse("");
    }

    public static DiscDefinition find(ItemStack stack) {
        String id = id(stack);
        return DiscLibrary.get().stream().filter(disc -> disc.id.equals(id)).findFirst().orElse(null);
    }
}
