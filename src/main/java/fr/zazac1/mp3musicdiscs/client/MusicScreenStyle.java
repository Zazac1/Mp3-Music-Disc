package fr.zazac1.mp3musicdiscs.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

/** Small shared visual language for the music-disc management screens. */
final class MusicScreenStyle {
    static final int INK = 0xFFF4F1E8;
    static final int MUTED = 0xFFB8C7C1;
    static final int DIM = 0xFF87938F;
    static final int ACCENT = 0xFFFFCE63;
    static final int TEAL = 0xFF7ED6C2;
    static final int DANGER = 0xFFFF8D8D;

    private MusicScreenStyle() { }

    static void title(GuiGraphicsExtractor graphics, Font font, String title, String subtitle, int width) {
        graphics.centeredText(font, title, width / 2, 8, INK);
        if (!subtitle.isBlank()) graphics.centeredText(font, subtitle, width / 2, 21, MUTED);
        graphics.fill(width / 2 - 82, 34, width / 2 + 82, 35, 0xAA7ED6C2);
    }

    static void panel(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom) {
        graphics.fill(x - 1, y - 1, right + 1, bottom + 1, 0xAA080C0C);
        graphics.fill(x, y, right, bottom, 0xD918211F);
        graphics.fill(x + 1, y + 1, right - 1, y + 2, 0xFF46615A);
    }

    static void panelHeader(GuiGraphicsExtractor graphics, Font font, int x, int y, int right, String label, String detail) {
        graphics.fill(x, y, right, y + 22, 0xE52A3633);
        graphics.text(font, label, x + 9, y + 7, INK, false);
        if (!detail.isBlank()) {
            int detailX = right - 9 - font.width(detail);
            graphics.text(font, detail, detailX, y + 7, MUTED, false);
        }
        graphics.horizontalLine(x, right - 1, y + 22, 0xFF53655F);
    }

    static void sectionLabel(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.text(font, text, x, y, MUTED, false);
        graphics.horizontalLine(x, x + 300, y + 12, 0x6653655F);
    }
}
