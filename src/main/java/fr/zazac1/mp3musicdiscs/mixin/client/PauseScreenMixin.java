package fr.zazac1.mp3musicdiscs.mixin.client;

import fr.zazac1.mp3musicdiscs.Mp3MusicDiscsMod;
import fr.zazac1.mp3musicdiscs.client.MusicDiscsScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Local-world shortcut, placed above Custom Recipe's shortcut when that mod is present. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(Mp3MusicDiscsMod.MOD_ID,
            "textures/gui/empty_disc.png");

    protected PauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void mp3musicdiscs$addLibraryShortcut(CallbackInfo ci) {
        if (minecraft == null || minecraft.getSingleplayerServer() == null) return;
        int buttonX = width - 28;
        int buttonY = height - (FabricLoader.getInstance().isModLoaded("customrecipe") ? 52 : 28);
        addRenderableWidget(Button.builder(Component.empty(), button ->
                minecraft.gui.setScreen(MusicDiscsScreen.fromModMenu((Screen) (Object) this)))
                .tooltip(Tooltip.create(Component.literal("MP3 Music Discs")))
                .bounds(buttonX, buttonY, 20, 20).build());
        addRenderableOnly((graphics, mouseX, mouseY, delta) -> graphics.blit(
                RenderPipelines.GUI_TEXTURED, ICON, buttonX + 2, buttonY + 2, 0, 0, 16, 16, 16, 16));
    }
}
