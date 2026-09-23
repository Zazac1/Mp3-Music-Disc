package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
final class LootPlacementScreen extends Screen {
    private final Screen parent;
    private final DiscDefinition disc;
    private final DiscDefinition.LootPlacement placement;

    LootPlacementScreen(Screen parent, DiscDefinition disc, DiscDefinition.LootPlacement placement) {
        super(Component.literal("Loot source settings"));
        this.parent = parent;
        this.disc = disc;
        this.placement = placement;
    }

    @Override
    protected void init() {
        int x = width / 2 - 150;
        addRenderableWidget(Button.builder(Component.literal("Enabled: " + (placement.enabled ? "ON" : "OFF")), b -> { placement.enabled = !placement.enabled; rebuildWidgets(); })
                .bounds(x, 122, 300, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Weight: " + placement.weight + "  −"), b -> { placement.weight = Math.max(1, placement.weight - 1); rebuildWidgets(); })
                .bounds(x, 156, 146, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Weight: " + placement.weight + "  +"), b -> { placement.weight++; rebuildWidgets(); })
                .bounds(x + 154, 156, 146, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Minimum: " + placement.min_count + "  −"), b -> { placement.min_count = Math.max(1, placement.min_count - 1); placement.max_count = Math.max(placement.min_count, placement.max_count); rebuildWidgets(); })
                .bounds(x, 186, 146, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Maximum: " + placement.max_count + "  +"), b -> { placement.max_count++; rebuildWidgets(); })
                .bounds(x + 154, 186, 146, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Remove from this disc"), b -> { disc.loot.remove(placement); DiscLibrary.save(); minecraft.gui.setScreen(parent); })
                .bounds(x, 227, 300, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.back"), b -> { DiscLibrary.save(); minecraft.gui.setScreen(parent); })
                .bounds(x, height - 28, 300, 20).build());
        addRenderableOnly(this::drawSettings);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 150;
        MusicScreenStyle.title(graphics, font, "Loot source", "Fine tune this disc's drop settings", width);
        MusicScreenStyle.panel(graphics, x, 72, x + 300, 257);
        MusicScreenStyle.panelHeader(graphics, font, x, 72, x + 300, "LOOT TABLE", "active entry");
        graphics.centeredText(font, placement.table, width / 2, 105, MusicScreenStyle.MUTED);
        graphics.centeredText(font, "Weight controls how often this disc is selected within this loot table.", width / 2, 272, MusicScreenStyle.DIM);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
