package com.beltlantern.client;

import com.beltlantern.BeltLanternLight;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the worn lantern as a 3D model hanging from a hook on the wearer's body and swinging like a pendulum.
 *
 * It is attached to {@code HumanoidModel.body} AFTER the model has been posed, so whatever changes the body pose
 * (sneaking, riding, crawling, animation mods / packs that move the torso) is followed automatically.
 */
public final class BeltLanternRenderer implements TrinketRenderer {
	private static final float PX = 1.0f / 16.0f;

	private final Map<Integer, Swing> swings = new HashMap<>();

	/** Model id of the 3D "worn" model of an item: {@code beltlantern:item/<path>_worn} (replaceable by packs). */
	static ResourceLocation wornModelId(ItemStack stack) {
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return ResourceLocation.fromNamespaceAndPath("beltlantern", "item/" + key.getPath() + "_worn");
	}

	public void clear() {
		this.swings.clear();
	}

	@Override
	public void render(ItemStack stack, SlotReference slot, EntityModel<? extends LivingEntity> contextModel,
					   PoseStack poseStack, MultiBufferSource buffers, int light, LivingEntity entity,
					   float limbAngle, float limbDistance, float tickDelta, float animationProgress,
					   float headYaw, float headPitch) {
		if (!(contextModel instanceof HumanoidModel<?> humanoid) || entity.isInvisible()) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		LanternPose pose = LanternPoseManager.forStack(stack);
		BakedModel model = mc.getModelManager().getModel(wornModelId(stack));

		// The lantern is a light source: keep it bright regardless of where the player stands.
		int emissive = BeltLanternLight.luminance(stack);
		int packed = LightTexture.pack(Math.max(LightTexture.block(light), emissive), LightTexture.sky(light));

		Swing swing = pose.physics
				? this.step(entity, pose, limbAngle, limbDistance, tickDelta)
				: Swing.rest(pose);

		poseStack.pushPose();
		// Follows everything the torso does: sneaking lean, body yaw, riding pose, animation mods.
		humanoid.body.translateAndRotate(poseStack);

		// Pendulum pivot = hook. Model space is y-down: positive X rotation swings the bottom of the lantern
		// backwards, positive Z rotation swings it inwards (towards the body's centre line for a left hook).
		poseStack.translate(pose.hookX * PX, pose.hookY * PX, pose.hookZ * PX);
		poseStack.mulPose(Axis.XP.rotation(swing.pitch));
		poseStack.mulPose(Axis.ZP.rotation(swing.roll));

		// Item models are y-up; here y points down. 180 degrees around X makes the lantern stand upright
		// (the lantern is symmetric so the z flip does not matter).
		poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
		poseStack.scale(pose.scale, pose.scale, pose.scale);
		// ItemRenderer centres the model on (0.5, 0.5, 0.5) of its 16px cube: move the top of the model (handle)
		// to the pivot.
		poseStack.translate(0.0f, -(pose.modelTopPx - 8.0f) * PX, 0.0f);

		mc.getItemRenderer().render(
				stack, ItemDisplayContext.FIXED, false, poseStack, buffers, packed, OverlayTexture.NO_OVERLAY, model);

		poseStack.popPose();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Pendulum simulation (client only, per entity, frame-rate independent).
	// ------------------------------------------------------------------------------------------------------------

	private static final class Swing {
		/** Swing angles (rad). The lantern is nailed to its hook: it only sways, it never moves up or down. */
		float pitch, pitchVel, roll, rollVel;
		/** Smoothed world velocity (blocks/s) and body yaw rate (rad/s, counter-clockwise from above = +). */
		float vx, vy, vz, omega;
		double lastX, lastY, lastZ;
		float lastYaw;
		boolean wasOnGround, wasCrouching, wasSwinging;
		int lastHurt;
		long lastNanos;
		boolean initialised;

		static Swing rest(LanternPose pose) {
			Swing s = new Swing();
			s.roll = pose.restRoll;
			return s;
		}
	}

	private Swing step(LivingEntity e, LanternPose p, float limbAngle, float limbDistance, float tickDelta) {
		long now = Util.getNanos();
		if (this.swings.size() > 32) {
			this.swings.values().removeIf(s -> now - s.lastNanos > 5_000_000_000L);
		}
		Swing s = this.swings.computeIfAbsent(e.getId(), id -> new Swing());

		double x = Mth.lerp(tickDelta, e.xo, e.getX());
		double y = Mth.lerp(tickDelta, e.yo, e.getY());
		double z = Mth.lerp(tickDelta, e.zo, e.getZ());
		float yaw = Mth.rotLerp(tickDelta, e.yBodyRotO, e.yBodyRot);

		if (!s.initialised) {
			s.initialised = true;
			s.lastX = x;
			s.lastY = y;
			s.lastZ = z;
			s.lastYaw = yaw;
			s.lastNanos = now;
			s.wasOnGround = e.onGround();
			s.wasCrouching = e.isCrouching();
			s.wasSwinging = e.swinging;
			s.lastHurt = e.hurtTime;
			s.roll = p.restRoll;
			return s;
		}

		float dtReal = (now - s.lastNanos) * 1.0e-9f;
		if (dtReal < 1.0e-4f) {
			return s; // already simulated for this frame (several render passes)
		}
		s.lastNanos = now;
		if (dtReal > 0.25f) { // pause, teleport, lag spike: do not fling the lantern
			s.lastX = x;
			s.lastY = y;
			s.lastZ = z;
			s.lastYaw = yaw;
			s.vx = s.vy = s.vz = s.omega = 0.0f;
			return s;
		}
		float dt = Math.min(dtReal, 0.05f);

		// ---- Motion of the wearer ---------------------------------------------------------------------------
		// World velocity (low-passed, position deltas are noisy) and its derivative, the world acceleration.
		float k = Math.min(1.0f, dt * 18.0f);
		float nvx = s.vx + ((float) ((x - s.lastX) / dtReal) - s.vx) * k;
		float nvy = s.vy + ((float) ((y - s.lastY) / dtReal) - s.vy) * k;
		float nvz = s.vz + ((float) ((z - s.lastZ) / dtReal) - s.vz) * k;
		float ax = (nvx - s.vx) / dt;
		float ay = (nvy - s.vy) / dt;
		float az = (nvz - s.vz) / dt;
		float vyBefore = s.vy;
		s.vx = nvx;
		s.vy = nvy;
		s.vz = nvz;
		s.lastX = x;
		s.lastY = y;
		s.lastZ = z;

		// Body yaw rate: Minecraft yaw grows clockwise, physics wants counter-clockwise.
		float yawRate = Mth.wrapDegrees(yaw - s.lastYaw) * Mth.DEG_TO_RAD / dtReal;
		s.lastYaw = yaw;
		float nOmega = Mth.clamp(s.omega + (-yawRate - s.omega) * k, -25.0f, 25.0f);
		float alpha = Mth.clamp((nOmega - s.omega) / dt, -150.0f, 150.0f);
		s.omega = nOmega;

		// World acceleration -> body frame (F = forward, L = left).
		float rad = yaw * Mth.DEG_TO_RAD;
		float sin = Mth.sin(rad);
		float cos = Mth.cos(rad);
		float accelForward = ax * -sin + az * cos;
		float accelLeft = ax * cos + az * sin;

		// The hook is not on the turning axis: turning the body whips it around (tangential + centripetal).
		float rForward = -p.hookZ * PX;
		float rLeft = p.hookX * PX;
		accelForward += -alpha * rLeft - s.omega * s.omega * rForward;
		accelLeft += alpha * rForward - s.omega * s.omega * rLeft;
		accelForward = Mth.clamp(accelForward, -60.0f, 60.0f);
		accelLeft = Mth.clamp(accelLeft, -60.0f, 60.0f);

		// ---- Sharp events -> impulses --------------------------------------------------------------------------
		float sign = ((now >> 12) & 1L) == 0L ? 1.0f : -1.0f;
		boolean onGround = e.onGround();
		if (!s.wasOnGround && onGround) { // landing
			float impact = Mth.clamp(-vyBefore, 0.0f, 25.0f) / 25.0f;
			s.pitchVel += impact * p.landKick;
			s.rollVel += sign * impact * p.landKick * 0.5f;
		}
		s.wasOnGround = onGround;

		if (e.hurtTime > 0 && s.lastHurt == 0) { // got hit
			s.pitchVel += p.hurtKick;
			s.rollVel += sign * p.hurtKick;
		}
		s.lastHurt = e.hurtTime;

		if (e.swinging && !s.wasSwinging) { // swung an arm: the torso twists a little
			s.pitchVel += p.swingKick;
			s.rollVel -= p.swingKick * 0.5f;
		}
		s.wasSwinging = e.swinging;

		boolean crouching = e.isCrouching();
		if (crouching != s.wasCrouching) { // the whole torso tips over
			s.pitchVel += (crouching ? 1.0f : -1.0f) * p.sneakKick;
			s.rollVel += sign * p.sneakKick * 0.4f;
		}
		s.wasCrouching = crouching;

		// ---- Continuous drives ---------------------------------------------------------------------------------
		// Walking rhythm: hips sway sideways once per step and bounce twice per stride.
		float phase = limbAngle * 0.6662f;
		float gaitRoll = Mth.cos(phase) * limbDistance * p.gaitRoll;
		float gaitPitch = Mth.sin(phase * 2.0f) * limbDistance * p.gaitPitch;

		// Idle breathing sway, out of phase per entity so a crowd does not swing in unison.
		float t = (e.tickCount + tickDelta) / 20.0f;
		float seed = e.getId() * 0.7f;
		float idlePitch = Mth.sin(t * 1.1f + seed) * p.idleSway;
		float idleRoll = Mth.sin(t * 0.75f + seed * 1.3f) * p.idleSway * 0.8f;

		// Pivot accelerates forward -> the bottom lags behind (pitch +, i.e. backwards).
		// Pivot accelerates to the left (+x) -> the bottom lags to the right (roll +).
		float drivePitch = accelForward / p.length * p.accelGain + gaitPitch + idlePitch;
		float driveRoll = accelLeft / p.length * p.accelGain + gaitRoll + idleRoll;
		// Vertical acceleration changes the effective gravity: heavier on take-off, weightless while falling.
		float gravity = Mth.clamp(1.0f + ay / 25.0f, 0.15f, 2.2f);
		float stiffness = p.stiffness * gravity;

		int steps = Math.max(1, Mth.ceil(dt / 0.008f));
		float h = dt / steps;
		for (int i = 0; i < steps; i++) {
			s.pitchVel += (drivePitch - stiffness * s.pitch - p.damping * s.pitchVel) * h;
			s.pitch += s.pitchVel * h;
			s.rollVel += (driveRoll - stiffness * (s.roll - p.restRoll) - p.damping * s.rollVel) * h;
			s.roll += s.rollVel * h;

			// Limits: the lantern rests against the body / thigh instead of going through it.
			if (s.pitch < p.minPitch) {
				s.pitch = p.minPitch;
				s.pitchVel = s.pitchVel < 0.0f ? s.pitchVel * -0.2f : s.pitchVel;
			} else if (s.pitch > p.maxPitch) {
				s.pitch = p.maxPitch;
				s.pitchVel = Math.min(s.pitchVel, 0.0f);
			}
			if (s.roll > p.maxRoll) {
				s.roll = p.maxRoll;
				s.rollVel = s.rollVel > 0.0f ? s.rollVel * -0.2f : s.rollVel;
			} else if (s.roll < p.minRoll) {
				s.roll = p.minRoll;
				s.rollVel = Math.max(s.rollVel, 0.0f);
			}
		}
		return s;
	}
}
