package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Music library with a compact, readable management layout. */
@Environment(EnvType.CLIENT)
public final class MusicDiscsScreen extends Screen {
    private static final int PAD = 12;
    private static final int ROW = 24;
    private final Screen parent;
    /** Server menus intentionally cannot alter loot injection from a client. */
    private final boolean allowLootConfiguration;
    private int scroll;

    private MusicDiscsScreen(Screen parent, boolean allowLootConfiguration) {
        super(Component.translatable("mp3musicdiscs.title"));
        this.parent = parent;
        this.allowLootConfiguration = allowLootConfiguration;
    }

    public static MusicDiscsScreen fromModMenu(Screen parent) {
        return new MusicDiscsScreen(parent, true);
    }

    /** Opens the personal library from a permission-checked dedicated-server command. */
    public static MusicDiscsScreen fromServerCommand(Screen parent) {
        return new MusicDiscsScreen(parent, false);
    }

    @Override
    protected void init() {
        int panelX = Math.max(PAD, width / 2 - 245);
        int panelW = Math.min(490, width - PAD * 2);
        int bottom = height - 30;
        int listTop = 82;
        int listBottom = bottom - 8;

        addRenderableOnly((graphics, mouseX, mouseY, delta) -> drawLibrary(graphics, panelX, panelW, listTop, listBottom));
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.import"), button -> minecraft.gui.setScreen(new Mp3ImportScreen(this)))
                .bounds(panelX, 53, 156, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.back"), button -> onClose())
                .bounds(panelX + panelW - 148, 53, 148, 20).build());
        addDiscActionButtons(panelX, panelW, listTop, listBottom);
    }

    private void addDiscActionButtons(int panelX, int panelW, int listTop, int listBottom) {
        List<DiscDefinition> discs = DiscLibrary.get();
        int visible = Math.max(1, (listBottom - listTop - 42) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, discs.size() - visible)));
        int actionX = panelX + panelW - 148;
        for (int i = scroll; i < Math.min(discs.size(), scroll + visible); i++) {
            DiscDefinition disc = discs.get(i);
            int y = listTop + 42 + (i - scroll) * ROW + 2;
            addRenderableWidget(Button.builder(Component.literal("Edit"), b -> minecraft.gui.setScreen(new DiscEditorScreen(this, disc, allowLootConfiguration)))
                    .bounds(actionX, y, 52, 20).build());
            if (allowLootConfiguration) {
                addRenderableWidget(Button.builder(Component.literal("Loot"), b -> minecraft.gui.setScreen(new LootConfigurationScreen(this, disc)))
                        .bounds(actionX + 56, y, 52, 20).build());
                addRenderableWidget(Button.builder(Component.literal("×"), b -> { DiscLibrary.get().remove(disc); DiscLibrary.save(); rebuildWidgets(); })
                        .bounds(actionX + 112, y, 28, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("Delete"), b -> { DiscLibrary.get().remove(disc); DiscLibrary.save(); rebuildWidgets(); })
                        .bounds(actionX + 56, y, 84, 20).build());
            }
        }
    }

    private void drawLibrary(GuiGraphicsExtractor graphics, int x, int panelW, int top, int bottom) {
        List<DiscDefinition> discs = DiscLibrary.get();
        MusicScreenStyle.title(graphics, font, title.getString(), "Your custom soundtrack collection", width);
        MusicScreenStyle.panel(graphics, x, top, x + panelW, bottom);
        MusicScreenStyle.panelHeader(graphics, font, x, top, x + panelW, "MUSIC DISCS", discs.size() + (discs.size() == 1 ? " disc" : " discs"));
        graphics.text(font, "DISC", x + 10, top + 29, MusicScreenStyle.MUTED, false);
        graphics.text(font, "TITLE", x + 45, top + 29, MusicScreenStyle.MUTED, false);
        graphics.verticalLine(x + panelW - 156, top + 24, bottom - 8, 0x6653655F);
        graphics.horizontalLine(x + 7, x + panelW - 8, top + 40, 0x8853655F);
        if (discs.isEmpty()) {
            graphics.centeredText(font, "Your library is ready for its first track.", width / 2, top + 66, MusicScreenStyle.INK);
            graphics.centeredText(font, "Use Import MP3 to create a custom music disc.", width / 2, top + 82, MusicScreenStyle.DIM);
            return;
        }
        int visible = Math.max(1, (bottom - top - 42) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, discs.size() - visible)));
        for (int i = scroll; i < Math.min(discs.size(), scroll + visible); i++) {
            DiscDefinition disc = discs.get(i);
            int y = top + 42 + (i - scroll) * ROW;
            graphics.fill(x + 7, y, x + panelW - 8, y + ROW - 2, i % 2 == 0 ? 0x33212C29 : 0x1A101514);
            DiscPreview.draw(graphics, x + 11, y + 3, 16, disc.appearance);
            // Match the baseline used by the 20px-tall action buttons in the same row.
            graphics.text(font, ellipsis(disc.name, panelW - 212), x + 42, y + 7, MusicScreenStyle.INK, false);
            graphics.horizontalLine(x + 7, x + panelW - 8, y + ROW - 2, 0x334C5E59);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            scroll -= (int) Math.signum(verticalAmount);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private String ellipsis(String value, int maximumPixels) {
        if (font.width(value) <= maximumPixels) return value;
        while (!value.isEmpty() && font.width(value + "…") > maximumPixels) value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
