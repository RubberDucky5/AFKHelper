package rubberduck.afkhelper.screens;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import rubberduck.afkhelper.AFKHelperClient;

import java.util.function.Supplier;

public class AFKMenuScreen extends GameOptionsScreen {
    private OptionListWidget optionButtons;

    public AFKMenuScreen() {
        super(new GameMenuScreen(true), null, Text.literal("AFK Menu"));

    }

    @Override
    public void init () {
        SimpleOption<?>[] autoSwing = new SimpleOption[]{AFKHelperClient.autoSwing, AFKHelperClient.swingSpeed};

        optionButtons = addDrawableChild(new OptionListWidget(this.client, this.width, this.height, this));
        optionButtons.addSingleOptionEntry(AFKHelperClient.autoReconnect);
        optionButtons.addAll(autoSwing);

        super.init();
    }
}
