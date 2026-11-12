package com.beltlantern.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

/**
 * Where the worn lantern hangs and how it moves. Defaults here are overridden by
 * {@code assets/beltlantern/pose/default.json} and {@code pose/<item>.json}, so resource packs can change them.
 *
 * Units: pixels (1/16 block) in the player BODY model space: origin = centre of the shoulder line,
 * +x = player's left, +y = down, +z = back. Angles are radians.
 */
final class LanternPose {
	// Placement
	float hookX = 3.7f, hookY = 9.0f, hookZ = 2.0f;
	/** 1.0 = full size vanilla lantern. */
	float scale = 0.6f;
	/** Height of the worn model above its origin in model px (vanilla lantern incl. handle: 11). */
	float modelTopPx = 11.0f;

	// Pendulum
	/** false = the lantern is rigid (resource pack that animates the item itself, or just a static look). */
	boolean physics = true;
	float length = 0.30f;
	float stiffness = 75.0f;
	float damping = 5.5f;
	float accelGain = 0.25f;
	float gaitRoll = 18.0f;
	float gaitPitch = 12.0f;
	/** Idle breathing sway, drive in rad/s^2. */
	float idleSway = 2.5f;

	// Resting tilt and limits
	float restRoll = -0.12f;
	float minPitch = -0.05f, maxPitch = 0.65f;
	float minRoll = -0.65f, maxRoll = 0.30f;

	// Reactions to sharp events
	/** Sway impulses in rad/s. */
	float hurtKick = 3.0f, landKick = 1.5f, sneakKick = 1.2f, swingKick = 0.8f;

	LanternPose copy() {
		LanternPose p = new LanternPose();
		p.hookX = hookX; p.hookY = hookY; p.hookZ = hookZ; p.scale = scale; p.modelTopPx = modelTopPx;
		p.physics = physics; p.length = length; p.stiffness = stiffness; p.damping = damping;
		p.accelGain = accelGain; p.gaitRoll = gaitRoll; p.gaitPitch = gaitPitch; p.idleSway = idleSway;
		p.restRoll = restRoll; p.minPitch = minPitch; p.maxPitch = maxPitch; p.minRoll = minRoll; p.maxRoll = maxRoll;
		p.hurtKick = hurtKick; p.landKick = landKick;
		p.sneakKick = sneakKick; p.swingKick = swingKick;
		return p;
	}

	/** Overrides the fields present in the JSON object. */
	void apply(JsonObject o) {
		if (o.has("hook") && o.get("hook").isJsonArray()) {
			JsonArray a = o.getAsJsonArray("hook");
			if (a.size() == 3) {
				hookX = a.get(0).getAsFloat();
				hookY = a.get(1).getAsFloat();
				hookZ = a.get(2).getAsFloat();
			}
		}
		scale = GsonHelper.getAsFloat(o, "scale", scale);
		modelTopPx = GsonHelper.getAsFloat(o, "model_top_px", modelTopPx);
		physics = GsonHelper.getAsBoolean(o, "physics", physics);
		length = Math.max(0.05f, GsonHelper.getAsFloat(o, "length", length));
		stiffness = GsonHelper.getAsFloat(o, "stiffness", stiffness);
		damping = GsonHelper.getAsFloat(o, "damping", damping);
		accelGain = GsonHelper.getAsFloat(o, "accel_gain", accelGain);
		gaitRoll = GsonHelper.getAsFloat(o, "gait_roll", gaitRoll);
		gaitPitch = GsonHelper.getAsFloat(o, "gait_pitch", gaitPitch);
		idleSway = GsonHelper.getAsFloat(o, "idle_sway", idleSway);
		restRoll = GsonHelper.getAsFloat(o, "rest_roll", restRoll);
		minPitch = GsonHelper.getAsFloat(o, "min_pitch", minPitch);
		maxPitch = GsonHelper.getAsFloat(o, "max_pitch", maxPitch);
		minRoll = GsonHelper.getAsFloat(o, "min_roll", minRoll);
		maxRoll = GsonHelper.getAsFloat(o, "max_roll", maxRoll);
		hurtKick = GsonHelper.getAsFloat(o, "hurt_kick", hurtKick);
		landKick = GsonHelper.getAsFloat(o, "land_kick", landKick);
		sneakKick = GsonHelper.getAsFloat(o, "sneak_kick", sneakKick);
		swingKick = GsonHelper.getAsFloat(o, "swing_kick", swingKick);
	}
}
