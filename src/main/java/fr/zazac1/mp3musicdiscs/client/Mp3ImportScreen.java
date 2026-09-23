package fr.zazac1.mp3musicdiscs.client;

import fr.zazac1.mp3musicdiscs.AudioConversion;
import fr.zazac1.mp3musicdiscs.DiscDefinition;
import fr.zazac1.mp3musicdiscs.DiscLibrary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Import stays entirely inside Minecraft; Windows paths can be pasted with Ctrl+V. */
@Environment(EnvType.CLIENT)
final class Mp3ImportScreen extends Screen {
    private final Screen parent;
    private EditBox pathField;
    private String error = "";

    Mp3ImportScreen(Screen parent) {
        super(Component.literal("Import MP3"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 170;
        addRenderableOnly(this::drawScreen);
        pathField = new EditBox(font, x, 119, 340, 20, Component.literal("MP3 file path"));
        pathField.setHint(Component.literal("C:\\Music\\my-song.mp3"));
        pathField.setMaxLength(1024);
        addRenderableWidget(pathField);
        addRenderableWidget(Button.builder(Component.literal("Import"), button -> importFile())
                .bounds(x, 148, 340, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mp3musicdiscs.back"), button -> onClose())
                .bounds(x, height - 28, 340, 20).build());
        setInitialFocus(pathField);
    }

    private void drawScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 170;
        MusicScreenStyle.title(graphics, font, "Import MP3", "Bring your favourite track into Minecraft", width);
        MusicScreenStyle.panel(graphics, x, 76, x + 340, 191);
        MusicScreenStyle.panelHeader(graphics, font, x, 76, x + 340, "AUDIO FILE", "MP3 only");
        graphics.text(font, "Paste a full file path, then customise the disc.", x + 10, 105, MusicScreenStyle.MUTED, false);
        if (!error.isBlank()) graphics.text(font, error, x + 10, 177, MusicScreenStyle.DANGER, false);
    }

    private void importFile() {
        String value = pathField.getValue().trim();
        // Windows Explorer's "Copy as path" wraps the path in quotes.
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
            pathField.setValue(value);
        }
        if (value.isBlank()) {
            error = "Choose or paste an MP3 file path.";
            return;
        }
        try {
            Path selected = Path.of(value).toAbsolutePath().normalize();
            if (!Files.isRegularFile(selected)) {
                error = "File not found.";
                return;
            }
            if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                error = "The selected file must end in .mp3.";
                return;
            }
            DiscDefinition disc = new DiscDefinition();
            String baseName = selected.getFileName().toString().replaceFirst("(?i)\\.mp3$", "");
            disc.name = baseName.isBlank() ? "Untitled Disc" : baseName;
            DiscLibrary.get().add(disc);
            AudioConversion.importAndConvert(disc, selected);
            minecraft.gui.setScreen(new DiscEditorScreen(parent, disc, true));
        } catch (java.nio.file.InvalidPathException exception) {
            error = "Invalid file path.";
        } catch (Exception exception) {
            error = "Unable to import this MP3.";
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
