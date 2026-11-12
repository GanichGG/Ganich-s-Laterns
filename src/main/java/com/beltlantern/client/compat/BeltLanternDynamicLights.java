package com.beltlantern.client.compat;

import com.beltlantern.BeltLanternLight;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.minecraft.world.entity.EntityType;

/**
 * Entrypoint "lambdynlights:initializer". Only ever loaded when LambDynamicLights is installed.
 *
 * Uses the (deprecated, but present in all 1.21.1 builds) DynamicLightHandlers API: it is the only one that
 * lets us compute luminance from code, and LDL merges it with the built-in handlers using max().
 */
@SuppressWarnings({"removal", "deprecation"})
public class BeltLanternDynamicLights implements DynamicLightsInitializer {
	@Override
	public void onInitializeDynamicLights(DynamicLightsContext context) {
		DynamicLightHandlers.registerDynamicLightHandler(
				EntityType.PLAYER,
				DynamicLightHandler.makeHandler(BeltLanternLight::luminanceOf, player -> false)
		);
	}

	@Override
	public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
		// Legacy entrypoint, handled by the DynamicLightsContext overload above.
	}
}
