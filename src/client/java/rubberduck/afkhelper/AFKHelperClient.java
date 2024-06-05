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
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.core.jmx.Server;

public class AFKHelperClient implements ClientModInitializer {
	public static SimpleOption<Boolean> autoReconnect = SimpleOption.ofBoolean("Auto Reconnect", SimpleOption.emptyTooltip(), false, value -> {});
	public static SimpleOption<Boolean> autoSwing = SimpleOption.ofBoolean("Auto Swing", SimpleOption.emptyTooltip(), false, value -> {});
	public static SimpleOption<Double> swingSpeed = new SimpleOption<>("Swing Speed", SimpleOption.emptyTooltip(), (optionText, value) -> GameOptions.getGenericValueText(optionText, Text.literal(  String.format("%.2fs", value))), new SimpleOption.ValidatingIntSliderCallbacks(0, 100).withModifier(pv -> (double)pv / 10.0, v -> (int)(v * 10.0)), 2.7, value -> {});
	public static SimpleOption<Boolean> holdRightClick = SimpleOption.ofBoolean("Hold Right Click", SimpleOption.emptyTooltip(), false, value -> {});

	private static ServerInfo lastServer;

	private int asTimer = 0;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);

	}

	private void tick(MinecraftClient client) {

		if ( holdRightClick.getValue() ) {
			client.itemUseCooldown = 4;
			if (client.crosshairTarget == null) {

			}
			for (Hand hand : Hand.values()) {
				ActionResult actionResult3;
				ItemStack itemStack = client.player.getStackInHand(hand);
				if (!itemStack.isItemEnabled(client.world.getEnabledFeatures())) {
					return;
				}
				if (client.crosshairTarget != null) {
					switch (client.crosshairTarget.getType()) {
						case ENTITY: {
							EntityHitResult entityHitResult = (EntityHitResult)client.crosshairTarget;
							Entity entity = entityHitResult.getEntity();
							if (!client.world.getWorldBorder().contains(entity.getBlockPos())) {
								return;
							}
							ActionResult actionResult = client.interactionManager.interactEntityAtLocation(client.player, entity, entityHitResult, hand);
							if (!actionResult.isAccepted()) {
								actionResult = client.interactionManager.interactEntity(client.player, entity, hand);
							}
							if (!actionResult.isAccepted()) break;
							if (actionResult.shouldSwingHand()) {
								client.player.swingHand(hand);
							}
							return;
						}
						case BLOCK: {
							BlockHitResult blockHitResult = (BlockHitResult)client.crosshairTarget;
							int i = itemStack.getCount();
							ActionResult actionResult2 = client.interactionManager.interactBlock(client.player, hand, blockHitResult);
							if (actionResult2.isAccepted()) {
								if (actionResult2.shouldSwingHand()) {
									client.player.swingHand(hand);
									if (!itemStack.isEmpty() && (itemStack.getCount() != i || client.interactionManager.hasCreativeInventory())) {
										client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
									}
								}
								return;
							}
							if (actionResult2 != ActionResult.FAIL) break;
							return;
						}
					}
				}
				if (itemStack.isEmpty() || !(actionResult3 = client.interactionManager.interactItem(client.player, hand)).isAccepted()) continue;
				if (actionResult3.shouldSwingHand()) {
					client.player.swingHand(hand);
				}
				client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
				return;
			}
		}

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