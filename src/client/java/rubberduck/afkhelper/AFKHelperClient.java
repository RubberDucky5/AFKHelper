package rubberduck.afkhelper;

import com.mojang.serialization.Decoder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.impl.networking.client.ClientNetworkingImpl;
import net.fabricmc.fabric.impl.screenhandler.client.ClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.core.jmx.Server;

public class AFKHelperClient implements ClientModInitializer {
	public static SimpleOption<Boolean> autoReconnect = SimpleOption.ofBoolean("Auto Reconnect", SimpleOption.emptyTooltip(), false, value -> {});
	public static SimpleOption<Boolean> autoSwing = SimpleOption.ofBoolean("Auto Swing", SimpleOption.emptyTooltip(), false, value -> {});
	public static SimpleOption<Double> swingSpeed = new SimpleOption<>("Swing Speed", SimpleOption.emptyTooltip(), (optionText, value) -> GameOptions.getGenericValueText(optionText, Text.literal(  String.format("%.2fs", value))), new SimpleOption.ValidatingIntSliderCallbacks(0, 100).withModifier(pv -> (double)pv / 10.0, v -> (int)(v * 10.0)), 2.7, value -> {});

	private static ServerInfo lastServer;

	private int asTimer = 0;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);

	}

	private void tick(MinecraftClient client) {

        if ( autoSwing.getValue() && asTimer <= 0 && client.crosshairTarget != null) {

			switch (client.crosshairTarget.getType()) {
				case ENTITY: {
					client.interactionManager.attackEntity(client.player, ((EntityHitResult)client.crosshairTarget).getEntity());
					break;
				}
				case BLOCK: {
					BlockHitResult blockHitResult = (BlockHitResult)client.crosshairTarget;
					BlockPos blockPos = blockHitResult.getBlockPos();
					if (!client.world.getBlockState(blockPos).isAir()) {
						client.interactionManager.attackBlock(blockPos, blockHitResult.getSide());
						if (!client.world.getBlockState(blockPos).isAir()) break;
						break;
					}
				}
				case MISS: {
					if (client.interactionManager.hasLimitedAttackSpeed()) {
						client.attackCooldown = 10;
					}
					client.player.resetLastAttackedTicks();
				}
			}
			client.player.swingHand(Hand.MAIN_HAND);

            asTimer = (int) (swingSpeed.getValue() * 20);
        }
        asTimer--;

		ClientPlayNetworkHandler nh = client.getNetworkHandler();
        ServerInfo si = nh != null ? nh.getServerInfo() : null;
		if(client.currentScreen instanceof DisconnectedScreen && autoReconnect.getValue()) {
			ConnectScreen.connect(client.currentScreen, client,  ServerAddress.parse(lastServer.address), lastServer, false, null);
		}
		if(si != null) {
			lastServer = si;
		}
	}
}