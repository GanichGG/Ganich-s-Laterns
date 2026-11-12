package com.beltlantern;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BeltLanternLight {
	private BeltLanternLight() {
	}

	/** Light level of a worn lantern: same as the vanilla blocks (lantern 15, soul lantern 10). */
	public static int luminance(ItemStack stack) {
		if (stack.is(Items.LANTERN)) {
			return 15;
		}
		if (stack.is(Items.SOUL_LANTERN)) {
			return 10;
		}
		return 0;
	}

	/** Highest luminance among lanterns worn by the entity, 0 if none. */
	public static int luminanceOf(LivingEntity entity) {
		int[] best = {0};
		TrinketsApi.getTrinketComponent(entity).ifPresent(component ->
				component.forEach((slot, stack) -> best[0] = Math.max(best[0], luminance(stack))));
		return best[0];
	}
}
