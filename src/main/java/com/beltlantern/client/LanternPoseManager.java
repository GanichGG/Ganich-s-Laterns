package com.beltlantern.client;

import com.beltlantern.BeltLantern;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code beltlantern:pose/default.json} and {@code beltlantern:pose/<item>.json} from ALL resource packs,
 * lowest priority first, so packs can override single fields.
 */
public final class LanternPoseManager implements SimpleSynchronousResourceReloadListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("beltlantern");

	private static volatile LanternPose defaultPose = new LanternPose();
	private static volatile Map<String, LanternPose> poses = Map.of();

	static LanternPose forStack(ItemStack stack) {
		LanternPose pose = poses.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
		return pose != null ? pose : defaultPose;
	}

	@Override
	public ResourceLocation getFabricId() {
		return BeltLantern.id("pose");
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		LanternPose base = new LanternPose();
		applyAll(manager, BeltLantern.id("pose/default.json"), base);

		Map<String, LanternPose> map = new HashMap<>();
		for (String item : new String[]{"lantern", "soul_lantern"}) {
			LanternPose pose = base.copy();
			applyAll(manager, BeltLantern.id("pose/" + item + ".json"), pose);
			map.put(item, pose);
		}
		defaultPose = base;
		poses = map;
	}

	private static void applyAll(ResourceManager manager, ResourceLocation id, LanternPose target) {
		for (Resource resource : manager.getResourceStack(id)) {
			try (Reader reader = resource.openAsReader()) {
				JsonObject json = GsonHelper.parse(reader);
				target.apply(json);
			} catch (Exception e) {
				LOGGER.warn("Ignoring broken lantern pose {} from pack {}: {}", id, resource.sourcePackId(), e.toString());
			}
		}
	}
}
