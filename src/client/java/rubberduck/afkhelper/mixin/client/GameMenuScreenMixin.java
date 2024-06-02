package rubberduck.afkhelper.mixin.client;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rubberduck.afkhelper.screens.AFKMenuScreen;


@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    GameMenuScreenMixin (Text title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "initWidgets")
    public void onInitWidgets (CallbackInfo ci) {
        addDrawableChild(ButtonWidget.builder(Text.literal("AFK Menu"), b -> client.setScreen(new AFKMenuScreen()))
                .dimensions(10, 10, 70, 20).build());
    }
}
