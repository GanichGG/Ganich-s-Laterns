package com.beltlantern;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class BeltLantern implements ModInitializer {
	public static final String MOD_ID = "beltlantern";

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Vanilla lanterns can be equipped into the "legs/lantern" slot (which items fit is the item tag
		// data/trinkets/tags/item/legs/lantern.json). Registering them as trinkets also enables right-click equip.
		TrinketsApi.registerTrinket(Items.LANTERN, new Trinket() {
		});
		TrinketsApi.registerTrinket(Items.SOUL_LANTERN, new Trinket() {
		});
	}
}
