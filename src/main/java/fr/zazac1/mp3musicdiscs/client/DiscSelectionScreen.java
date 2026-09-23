package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import fr.zazac1.mp3musicdiscs.DiscStacks;
import fr.zazac1.mp3musicdiscs.DiscSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Turns the held blank disc into one of the locally imported, playable music discs. */
@Environment(EnvType.CLIENT)
final class DiscSelectionScreen extends Screen {
    private static final int ROW = 30;
    private final Screen parent;
    private final Player player;
    private final InteractionHand hand;
    private int scroll;

    DiscSelectionScreen(Screen parent, Player player, InteractionHand hand) {
        super(Component.literal("Choose music for this disc"));
        this.parent = parent;
        this.player = player;
        this.hand = hand;
    }

    @Override
    protected void init() {
        int x = width / 2 - 170;
        addRenderableOnly(this::drawSelection);
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.back"), b -> onClose())
                .bounds(x, height - 28, 340, 20).build());
    }

    private void drawSelection(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 170;
        int bottom = height - 38;
        List<DiscDefinition> discs = DiscLibrary.get();
        MusicScreenStyle.title(graphics, font, "Choose a track", "The blank disc will become this music disc", width);
        MusicScreenStyle.panel(graphics, x, 56, x + 340, bottom);
        MusicScreenStyle.panelHeader(graphics, font, x, 56, x + 340, "IMPORTED MUSIC", discs.size() + (discs.size() == 1 ? " track" : " tracks"));
        if (discs.isEmpty()) {
            graphics.centeredText(font, "No music has been imported yet.", width / 2, 94, MusicScreenStyle.INK);
            graphics.centeredText(font, "Open MP3 Music Discs and use Import MP3 first.", width / 2, 110, MusicScreenStyle.DIM);
            return;
        }
        int visible = Math.max(1, (bottom - 79) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, discs.size() - visible)));
        for (int i = scroll; i < Math.min(discs.size(), scroll + visible); i++) {
            DiscDefinition disc = discs.get(i);
            int y = 79 + (i - scroll) * ROW;
            boolean hovered = mouseX >= x + 7 && mouseX < x + 333 && mouseY >= y && mouseY < y + ROW - 2;
            graphics.fill(x + 7, y, x + 333, y + ROW - 2, hovered ? 0x77506C63 : (i % 2 == 0 ? 0x33212C29 : 0x1A101514));
            DiscPreview.draw(graphics, x + 13, y + 5, 20, disc.appearance);
            graphics.text(font, ellipsis(disc.name, 210), x + 43, y + 6, MusicScreenStyle.INK, false);
            String status = disc.source_mp3.isBlank() ? "MP3 missing" : "Select";
            graphics.text(font, status, x + 276, y + 6, disc.source_mp3.isBlank() ? MusicScreenStyle.DANGER : MusicScreenStyle.TEAL, false);
            if (!disc.description.isBlank()) graphics.text(font, ellipsis(disc.description, 210), x + 43, y + 17, MusicScreenStyle.DIM, false);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        int x = width / 2 - 170;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (mouseX >= x + 7 && mouseX < x + 333 && mouseY >= 79 && mouseY < height - 38) {
            int index = scroll + (mouseY - 79) / ROW;
            List<DiscDefinition> discs = DiscLibrary.get();
            if (index >= 0 && index < discs.size()) {
                DiscDefinition selected = discs.get(index);
                if (!selected.source_mp3.isBlank()) {
                    // The custom-data ID is what DiscAudioPlayer reads when this stack enters a jukebox.
                    player.setItemInHand(hand, DiscStacks.create(selected));
                    ClientPlayNetworking.send(new DiscSelectionPayload(selected.id, hand == InteractionHand.OFF_HAND));
                    minecraft.gui.setScreen(parent);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            scroll -= (int) Math.signum(verticalAmount);
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
