package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searches all loaded datapack loot-table resources instead of maintaining a vanilla-only list. */
@Environment(EnvType.CLIENT)
final class LootConfigurationScreen extends Screen {
    private static final int PAD = 12;
    private static final int ROW = 21;
    /** Client resource packs do not expose server data packs, so keep the useful vanilla sources available. */
    private static final List<String> VANILLA_SOURCES = List.of(
            "minecraft:chests/abandoned_mineshaft", "minecraft:chests/ancient_city", "minecraft:chests/ancient_city_ice_box",
            "minecraft:chests/bastion_bridge", "minecraft:chests/bastion_hoglin_stable", "minecraft:chests/bastion_other",
            "minecraft:chests/bastion_treasure", "minecraft:chests/buried_treasure", "minecraft:chests/desert_pyramid",
            "minecraft:chests/end_city_treasure", "minecraft:chests/igloo_chest", "minecraft:chests/jungle_temple",
            "minecraft:chests/nether_bridge", "minecraft:chests/pillager_outpost", "minecraft:chests/ruined_portal",
            "minecraft:chests/shipwreck_map", "minecraft:chests/shipwreck_supply", "minecraft:chests/shipwreck_treasure",
            "minecraft:chests/simple_dungeon", "minecraft:chests/spawn_bonus_chest", "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library", "minecraft:chests/trial_chambers/corridor",
            "minecraft:chests/trial_chambers/intersection", "minecraft:chests/trial_chambers/reward_common",
            "minecraft:chests/trial_chambers/reward_ominous", "minecraft:chests/trial_chambers/reward_rare",
            "minecraft:chests/trial_chambers/reward_unique", "minecraft:chests/underwater_ruin_big",
            "minecraft:chests/underwater_ruin_small", "minecraft:chests/village/village_armorer",
            "minecraft:chests/village/village_butcher", "minecraft:chests/village/village_cartographer",
            "minecraft:chests/village/village_desert_house", "minecraft:chests/village/village_fisher",
            "minecraft:chests/village/village_mason", "minecraft:chests/village/village_plains_house",
            "minecraft:chests/village/village_savanna_house", "minecraft:chests/village/village_snowy_house",
            "minecraft:chests/village/village_taiga_house", "minecraft:chests/village/village_tannery",
            "minecraft:chests/village/village_temple", "minecraft:chests/village/village_toolsmith",
            "minecraft:chests/village/village_weaponsmith", "minecraft:chests/woodland_mansion",
            "minecraft:archaeology/desert_pyramid", "minecraft:archaeology/desert_well",
            "minecraft:archaeology/ocean_ruin_cold", "minecraft:archaeology/ocean_ruin_warm",
            "minecraft:archaeology/trail_ruins_common", "minecraft:archaeology/trail_ruins_rare",
            "minecraft:entities/creeper", "minecraft:entities/drowned", "minecraft:entities/ender_dragon",
            "minecraft:entities/enderman", "minecraft:entities/evoker", "minecraft:entities/husk", "minecraft:entities/piglin",
            "minecraft:entities/pillager", "minecraft:entities/ravager", "minecraft:entities/skeleton",
            "minecraft:entities/stray", "minecraft:entities/vex", "minecraft:entities/vindicator", "minecraft:entities/witch",
            "minecraft:entities/wither", "minecraft:entities/wither_skeleton", "minecraft:entities/zombie", "minecraft:entities/zombie_villager"
    );
    private final Screen parent;
    private final DiscDefinition disc;
    private final List<String> allTables = new ArrayList<>();
    private EditBox search;
    private String searchValue = "";
    private int sourceScroll;
    private int selectedScroll;
    private boolean draggingSourceScrollbar;
    private boolean draggingSelectedScrollbar;

    LootConfigurationScreen(Screen parent, DiscDefinition disc) {
        super(net.minecraft.network.chat.Component.literal("Loot configuration"));
        this.parent = parent;
        this.disc = disc;
    }

    @Override
    protected void init() {
        collectLootTables();
        int left = PAD;
        int tableW = Math.max(130, width / 2 - PAD - 8);
        // The panels are a background layer; row controls must be added afterwards.
        addRenderableOnly(this::drawScreen);
        search = new EditBox(font, left, 57, tableW, 20, net.minecraft.network.chat.Component.translatable("mp3musicdiscs.search"));
        search.setValue(searchValue);
        search.setResponder(value -> { searchValue = value; sourceScroll = 0; });
        search.setHint(net.minecraft.network.chat.Component.translatable("mp3musicdiscs.search"));
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.translatable("mp3musicdiscs.save"), b -> {
            DiscLibrary.save();
            minecraft.gui.setScreen(parent);
        }).bounds(width / 2 - 62, height - 28, 124, 20).build());
        addActiveRemoveButtons();
    }

    private void addActiveRemoveButtons() {
        int right = width / 2 + 8;
        int panelWidth = width - right - PAD;
        int visible = visibleRows(85, height - 60);
        for (int i = selectedScroll; i < Math.min(disc.loot.size(), selectedScroll + visible); i++) {
            DiscDefinition.LootPlacement placement = disc.loot.get(i);
            int rowY = 85 + 23 + (i - selectedScroll) * ROW;
            addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal("Remove"), button -> removePlacement(placement))
                    .bounds(right + panelWidth - 61, rowY + 1, 53, 18).build());
        }
    }

    private void collectLootTables() {
        allTables.clear();
        allTables.addAll(VANILLA_SOURCES);
        try {
            Map<Identifier, ?> resources = minecraft.getResourceManager().listResources("loot_table", path -> path.getPath().endsWith(".json"));
            for (Identifier id : resources.keySet()) {
                String path = id.getPath();
                if (path.startsWith("loot_table/")) path = path.substring("loot_table/".length());
                if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
                String value = id.getNamespace() + ":" + path;
                if (!allTables.contains(value)) allTables.add(value);
            }
        } catch (Exception ignored) {
            // Some multiplayer servers hide datapack resources from the client. Existing configured entries stay editable.
        }
        for (DiscDefinition.LootPlacement placement : disc.loot) if (!allTables.contains(placement.table)) allTables.add(placement.table);
        allTables.sort(Comparator.naturalOrder());
    }

    private void drawScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = PAD;
        int w = Math.max(130, width / 2 - PAD - 8);
        int right = width / 2 + 8;
        int bottom = height - 60;
        MusicScreenStyle.title(graphics, font, "Loot configuration", disc.name, width);
        graphics.text(font, "Search every loaded loot table", left, 47, MusicScreenStyle.MUTED, false);
        drawPanel(graphics, left, 85, w, bottom, "AVAILABLE TABLES", filteredTables(), sourceScroll, false);
        drawPanel(graphics, right, 85, width - right - PAD, bottom, "ACTIVE FOR THIS DISC", selectedTables(), selectedScroll, true);
        graphics.centeredText(font, "Click a table to enable it · click an active entry to edit weight and quantity.", width / 2, height - 49, MusicScreenStyle.DIM);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int bottom, String label,
                           List<?> values, int scroll, boolean selected) {
        MusicScreenStyle.panel(graphics, x, y, x + w, bottom);
        MusicScreenStyle.panelHeader(graphics, font, x, y, x + w, label,
                selected ? "weight · quantity" : values.size() + " entries");
        int visible = visibleRows(y, bottom);
        int maxScroll = Math.max(0, values.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = scroll; i < Math.min(values.size(), scroll + visible); i++) {
            int rowY = y + 23 + (i - scroll) * ROW;
            String value;
            int color = 0xFFFFFFFF;
            if (selected) {
                DiscDefinition.LootPlacement placement = (DiscDefinition.LootPlacement) values.get(i);
                value = placement.table + "  |  weight: " + placement.weight
                        + "  |  quantity: " + placement.min_count + "–" + placement.max_count;
                if (!placement.enabled) color = 0xFF888888;
            } else value = (String) values.get(i);
            graphics.fill(x + 7, rowY, x + w - 8, rowY + ROW - 2, i % 2 == 0 ? 0x33212C29 : 0x1A101514);
            graphics.text(font, ellipsis(value, selected ? w - 70 : w - 12), x + 6, rowY + 6, color, false);
        }
        if (maxScroll > 0) drawScrollbar(graphics, x + w - 4, y + 24, bottom - 2, values.size(), visible, scroll);
        if (values.isEmpty()) graphics.centeredText(font, selected
                        ? net.minecraft.network.chat.Component.literal("No loot sources selected.")
                        : net.minecraft.network.chat.Component.translatable("mp3musicdiscs.no_loot"),
                x + w / 2, y + 38, MusicScreenStyle.DIM);
    }

    private List<String> filteredTables() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) return allTables;
        return allTables.stream().filter(value -> value.toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private List<DiscDefinition.LootPlacement> selectedTables() { return disc.loot; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (clickScrollbar(mouseX, mouseY)) return true;
        int y = 85 + 23;
        if (mouseY >= y && mouseY < height - 60) {
            int index = (mouseY - y) / ROW;
            if (mouseX < width / 2) {
                List<String> matches = filteredTables();
                int actual = sourceScroll + index;
                if (actual >= 0 && actual < matches.size()) toggleTable(matches.get(actual));
            } else {
                int actual = selectedScroll + index;
                if (actual >= 0 && actual < disc.loot.size()) {
                    if (mouseX >= width - PAD - 65) removePlacement(disc.loot.get(actual));
                    else minecraft.gui.setScreen(new LootPlacementScreen(this, disc, disc.loot.get(actual)));
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0 || mouseY < 85 || mouseY >= height - 60) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (mouseX < width / 2) sourceScroll = Math.max(0, Math.min(maxSourceScroll(), sourceScroll - (int) Math.signum(verticalAmount)));
        else {
            selectedScroll = Math.max(0, Math.min(maxSelectedScroll(), selectedScroll - (int) Math.signum(verticalAmount)));
            rebuildWidgets();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingSourceScrollbar) { updateScrollbar(event.y(), false); return true; }
        if (draggingSelectedScrollbar) { updateScrollbar(event.y(), true); return true; }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSourceScrollbar = false;
        draggingSelectedScrollbar = false;
        return super.mouseReleased(event);
    }

    private boolean clickScrollbar(int mouseX, int mouseY) {
        int bottom = height - 60;
        int leftWidth = Math.max(130, width / 2 - PAD - 8);
        int right = width / 2 + 8;
        if (maxSourceScroll() > 0 && mouseX >= PAD + leftWidth - 7 && mouseX < PAD + leftWidth + 3
                && mouseY >= 109 && mouseY < bottom - 2) {
            draggingSourceScrollbar = true;
            updateScrollbar(mouseY, false);
            return true;
        }
        if (maxSelectedScroll() > 0 && mouseX >= width - PAD - 7 && mouseX < width - PAD + 3
                && mouseY >= 109 && mouseY < bottom - 2) {
            draggingSelectedScrollbar = true;
            updateScrollbar(mouseY, true);
            return true;
        }
        return false;
    }

    private void updateScrollbar(double mouseY, boolean selected) {
        int top = 109;
        int bottom = height - 62;
        int count = selected ? disc.loot.size() : filteredTables().size();
        int visible = visibleRows(85, height - 60);
        int maxScroll = Math.max(0, count - visible);
        int track = bottom - top;
        int thumb = Math.max(8, track * visible / count);
        int travel = track - thumb;
        if (travel <= 0) return;
        int next = (int) Math.round((mouseY - top - thumb / 2.0) * maxScroll / travel);
        next = Math.max(0, Math.min(next, maxScroll));
        if (selected && next != selectedScroll) { selectedScroll = next; rebuildWidgets(); }
        if (!selected) sourceScroll = next;
    }

    private int visibleRows(int top, int bottom) { return Math.max(1, (bottom - top - 23) / ROW); }

    private int maxSourceScroll() { return Math.max(0, filteredTables().size() - visibleRows(85, height - 60)); }

    private int maxSelectedScroll() { return Math.max(0, disc.loot.size() - visibleRows(85, height - 60)); }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, int count, int visible, int scroll) {
        int track = bottom - top;
        int thumb = Math.max(8, track * visible / count);
        int range = Math.max(1, count - visible);
        int thumbY = top + (track - thumb) * scroll / range;
        graphics.fill(x, top, x + 2, bottom, 0x6653655F);
        graphics.fill(x, thumbY, x + 2, thumbY + thumb, MusicScreenStyle.TEAL);
    }

    private void toggleTable(String table) {
        for (DiscDefinition.LootPlacement placement : disc.loot) if (placement.table.equals(table)) {
            placement.enabled = !placement.enabled;
            return;
        }
        DiscDefinition.LootPlacement placement = new DiscDefinition.LootPlacement();
        placement.table = table;
        disc.loot.add(placement);
    }

    private void removePlacement(DiscDefinition.LootPlacement placement) {
        disc.loot.remove(placement);
        selectedScroll = Math.min(selectedScroll, maxSelectedScroll());
        rebuildWidgets();
    }

    private String ellipsis(String value, int maximumPixels) {
        if (font.width(value) <= maximumPixels) return value;
        while (!value.isEmpty() && font.width(value + "…") > maximumPixels) value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
