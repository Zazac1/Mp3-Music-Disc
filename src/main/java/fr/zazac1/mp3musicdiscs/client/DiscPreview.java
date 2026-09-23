package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Procedural preview: a base disc plus independent label, zone and pattern layers. */
final class DiscPreview {
    /** User-authored 16x16 zone map: cyan=edge, magenta=material, green=center. */
    private static final int EDGE = 0x00FFF6;
    private static final int MATERIAL = 0xFF00FF;
    private static final int CENTER = 0x00FF00;
    private static final BufferedImage ZONE_MASK = loadMask();
    private DiscPreview() {}

    static void draw(GuiGraphicsExtractor graphics, int x, int y, int size, DiscDefinition.Appearance look) {
        if (ZONE_MASK != null) {
            drawMask(graphics, x, y, size, look);
            return;
        }
        drawFallback(graphics, x, y, size, look);
    }

    private static void drawMask(GuiGraphicsExtractor graphics, int x, int y, int size, DiscDefinition.Appearance look) {
        int maskW = ZONE_MASK.getWidth();
        int maskH = ZONE_MASK.getHeight();
        for (int py = 0; py < maskH; py++) for (int px = 0; px < maskW; px++) {
            int argb = ZONE_MASK.getRGB(px, py);
            if ((argb >>> 24) == 0) continue;
            int rgb = argb & 0xFFFFFF;
            int color = switch (rgb) {
                case EDGE -> shade(look.disc_color, edgeShade(px, py));
                case MATERIAL -> materialColor(look, px, py, maskW, maskH);
                case CENTER -> shade(look.label_color, .92f);
                default -> 0;
            };
            if (color == 0) continue;
            int left = x + px * size / maskW;
            int top = y + py * size / maskH;
            int right = x + (px + 1) * size / maskW;
            int bottom = y + (py + 1) * size / maskH;
            graphics.fill(left, top, Math.max(left + 1, right), Math.max(top + 1, bottom), color);
        }
    }

    private static int materialColor(DiscDefinition.Appearance look, int px, int py, int width, int height) {
        boolean accent = switch (look.pattern == null ? "rings" : look.pattern) {
            case "stripes" -> py % 4 == 0;
            case "dots" -> px % 4 == 1 && py % 4 == 1;
            case "checker" -> ((px / 3) + (py / 3) & 1) == 0;
            case "gradient" -> py < height / 2;
            default -> {
                int dx = px * 2 - width + 1;
                int dy = py * 2 - height + 1;
                int distance = dx * dx + dy * dy;
                yield distance > 34 && distance < 58;
            }
        };
        return shade(look.zone_color, accent ? 1.14f : .72f);
    }

    private static float edgeShade(int px, int py) {
        return (px + py) % 3 == 0 ? .72f : .92f;
    }

    private static void drawFallback(GuiGraphicsExtractor graphics, int x, int y, int size, DiscDefinition.Appearance look) {
        int dark = shade(look.disc_color, 0.48f);
        int edge = shade(look.disc_color, 0.72f);
        for (int row = 0; row < size; row++) {
            float dy = (row + .5f - size / 2f) / (size / 2f);
            int inset = Math.max(0, (int) (Math.abs(dy) * Math.abs(dy) * size * .22f));
            graphics.fill(x + inset, y + row, x + size - inset, y + row + 1, edge);
        }
        graphics.fill(x + 1, y + 1, x + size - 1, y + 2, dark);
        pattern(graphics, x, y, size, look);
        int label = Math.max(4, size / 3);
        int labelX = x + (size - label) / 2;
        int labelY = y + (size - label) / 2;
        graphics.fill(labelX, labelY, labelX + label, labelY + label, look.label_color);
    }

    private static BufferedImage loadMask() {
        try (InputStream stream = DiscPreview.class.getResourceAsStream(
                "/assets/mp3musicdiscs/textures/gui/disc_zone_mask.png")) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void pattern(GuiGraphicsExtractor graphics, int x, int y, int size, DiscDefinition.Appearance look) {
        String pattern = look.pattern == null ? "rings" : look.pattern;
        int c = look.zone_color;
        if (pattern.equals("stripes")) {
            for (int i = 2; i < size - 2; i += Math.max(2, size / 6)) graphics.fill(x + 2, y + i, x + size - 2, y + i + 1, c);
        } else if (pattern.equals("dots")) {
            for (int py = 3; py < size - 2; py += Math.max(3, size / 4)) for (int px = 3; px < size - 2; px += Math.max(3, size / 4)) graphics.fill(x + px, y + py, x + px + 1, y + py + 1, c);
        } else if (pattern.equals("checker")) {
            int cell = Math.max(2, size / 4);
            for (int py = 1; py < size - 1; py += cell) for (int px = 1; px < size - 1; px += cell) if (((px + py) / cell & 1) == 0) graphics.fill(x + px, y + py, x + px + cell, y + py + cell, c);
        } else if (pattern.equals("gradient")) {
            graphics.fillGradient(x + 2, y + 2, x + size - 2, y + size - 2, c, shade(c, .35f));
        } else {
            int ring = Math.max(2, size / 5);
            graphics.fill(x + ring, y + ring, x + size - ring, y + ring + 1, c);
            graphics.fill(x + ring, y + size - ring - 1, x + size - ring, y + size - ring, c);
            graphics.fill(x + ring, y + ring, x + ring + 1, y + size - ring, c);
            graphics.fill(x + size - ring - 1, y + ring, x + size - ring, y + size - ring, c);
        }
    }

    private static int shade(int argb, float factor) {
        int a = argb >>> 24;
        int r = Math.min(255, (int) (((argb >>> 16) & 255) * factor));
        int g = Math.min(255, (int) (((argb >>> 8) & 255) * factor));
        int b = Math.min(255, (int) ((argb & 255) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
