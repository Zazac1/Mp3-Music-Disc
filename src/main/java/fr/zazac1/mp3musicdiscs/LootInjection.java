package fr.zazac1.mp3musicdiscs;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetCustomDataFunction;

/** Applies exactly the GUI-selected placements; no vanilla table name is hard-coded. */
public final class LootInjection {
    private LootInjection() {}

    public static void initialize() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            Identifier table = key.identifier();
            for (DiscDefinition disc : DiscLibrary.get()) {
                for (DiscDefinition.LootPlacement placement : disc.loot) {
                    if (!placement.enabled || !placement.table.equals(table.toString())) continue;
                    CompoundTag data = new CompoundTag();
                    data.putString(DiscStacks.DISC_ID, disc.id);
                    builder.pool(LootPool.lootPool()
                            .add(LootItem.lootTableItem(Mp3MusicDiscsMod.CUSTOM_MUSIC_DISC)
                                    .setWeight(Math.max(1, placement.weight))
                                    .apply(SetCustomDataFunction.setCustomData(data))).build());
                }
            }
        });
    }
}
