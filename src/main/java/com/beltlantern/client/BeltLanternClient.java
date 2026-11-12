package com.beltlantern.client;

import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class BeltLanternClient implements ClientModInitializer {
	private static final BeltLanternRenderer RENDERER = new BeltLanternRenderer();

	@Override
	public void onInitializeClient() {
		List<Item> items = List.of(Items.LANTERN, Items.SOUL_LANTERN);

		// The 3D "<item>_worn" models (vanilla lantern block model, unscaled) are loaded separately for rendering on the body.
		ModelLoadingPlugin.register(context -> {
			for (Item item : items) {
				context.addModels(BeltLanternRenderer.wornModelId(new ItemStack(item)));
			}
		});

		// Pose / physics numbers come from assets/beltlantern/pose/*.json so resource packs can change them.
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new LanternPoseManager());

		for (Item item : items) {
			TrinketRendererRegistry.registerRenderer(item, RENDERER);
		}

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RENDERER.clear());
	}
}
