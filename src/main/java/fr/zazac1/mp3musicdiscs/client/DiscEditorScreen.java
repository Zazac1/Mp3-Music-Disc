package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
final class DiscEditorScreen extends Screen {
    /** Vanilla dye palette: white, orange, magenta, light blue, yellow, lime, pink, gray, light gray, cyan, purple, blue, brown, green, red and black. */
    private static final int[] COLORS = {
            0xFFF9FFFE, 0xFFF9801D, 0xFFC74EBD, 0xFF3AB3DA,
            0xFFFED83D, 0xFF80C71F, 0xFFF38BAA, 0xFF474F52,
            0xFF9D9D97, 0xFF169C9C, 0xFF8932B8, 0xFF3C44AA,
            0xFF835432, 0xFF5E7C16, 0xFFB02E26, 0xFF1D1D21
    };
    private static final String[] PATTERNS = {"rings", "stripes", "dots", "checker", "gradient"};
    private final Screen parent;
    private final DiscDefinition disc;
    private final boolean allowLootConfiguration;
    private EditBox name;
    private EditBox description;

    DiscEditorScreen(Screen parent, DiscDefinition disc, boolean allowLootConfiguration) {
        super(Component.literal("Edit music disc"));
        this.parent = parent;
        this.disc = DiscLibrary.normalize(disc);
        this.allowLootConfiguration = allowLootConfiguration;
    }

    @Override
    protected void init() {
        int x = width / 2 - 170;
        name = new EditBox(font, x, 71, 340, 20, Component.literal("Disc name"));
        name.setValue(disc.name);
        // A visible limit prevents a successful-looking save followed by a silent rollback.
        name.setMaxLength(256);
        description = new EditBox(font, x, 109, 340, 20, Component.literal("Description"));
        description.setValue(disc.description);
        description.setMaxLength(120);
        addRenderableWidget(name);
        addRenderableWidget(description);
        addRenderableWidget(Button.builder(Component.literal("Edge colour"), b -> { disc.appearance.disc_color = next(disc.appearance.disc_color); refreshInventoryAppearance(); })
                .bounds(x, 157, 108, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Centre colour"), b -> { disc.appearance.label_color = next(disc.appearance.label_color); refreshInventoryAppearance(); })
                .bounds(x + 116, 157, 108, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Material colour"), b -> { disc.appearance.zone_color = next(disc.appearance.zone_color); refreshInventoryAppearance(); })
                .bounds(x + 232, 157, 108, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Pattern: " + disc.appearance.pattern), b -> {
            disc.appearance.pattern = PATTERNS[(indexOfPattern() + 1) % PATTERNS.length];
            refreshInventoryAppearance();
            rebuildWidgets();
        }).bounds(x, 185, 340, 20).build());
        if (allowLootConfiguration) addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.loot"), b -> saveThenOpenLoot())
                .bounds(x, height - 28, 166, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.save"), b -> saveAndClose())
                .bounds(allowLootConfiguration ? x + 174 : x, height - 28, allowLootConfiguration ? 166 : 340, 20).build());
        addRenderableOnly(this::drawEditor);
    }

    private void drawEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 170;
        MusicScreenStyle.title(graphics, font, "Disc appearance", "Shape the look of your custom record", width);
        int previewTop = 218;
        int previewBottom = height - 36;
        // The preview owns the whole panel body and scales down only when the window is short.
        int previewBodyTop = previewTop + 23;
        int previewBodyHeight = previewBottom - previewBodyTop;
        int previewSize = Math.max(16, Math.min(96, previewBodyHeight - 4));
        int previewY = previewBodyTop + Math.max(0, (previewBodyHeight - previewSize) / 2);
        MusicScreenStyle.sectionLabel(graphics, font, "DISC DETAILS", x, 45);
        graphics.text(font, "Name (up to 256 characters)", x, 59, MusicScreenStyle.MUTED, false);
        graphics.text(font, "Description", x, 97, MusicScreenStyle.MUTED, false);
        MusicScreenStyle.sectionLabel(graphics, font, "COLOURS & PATTERN", x, 137);
        MusicScreenStyle.panel(graphics, x, previewTop, x + 340, previewBottom);
        MusicScreenStyle.panelHeader(graphics, font, x, previewTop, x + 340, "LIVE PREVIEW", "updates instantly");
        DiscPreview.draw(graphics, width / 2 - previewSize / 2, previewY, previewSize, disc.appearance);
    }

    private int next(int current) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == current) return COLORS[(i + 1) % COLORS.length];
        return COLORS[0];
    }

    private int indexOfPattern() {
        for (int i = 0; i < PATTERNS.length; i++) if (PATTERNS[i].equals(disc.appearance.pattern)) return i;
        return 0;
    }

    private void applyFields() {
        disc.name = name.getValue().trim().isBlank() ? "Untitled Disc" : name.getValue().trim();
        disc.description = description.getValue().trim();
        refreshInventoryAppearance();
    }

    private void refreshInventoryAppearance() { Mp3MusicDiscsClient.refreshInventoryAppearance(disc); }

    private void saveThenOpenLoot() {
        applyFields();
        DiscLibrary.save();
        minecraft.gui.setScreen(new LootConfigurationScreen(parent, disc));
    }

    private void saveAndClose() {
        applyFields();
        DiscLibrary.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
