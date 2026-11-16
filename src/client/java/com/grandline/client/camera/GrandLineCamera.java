package com.grandline.client.camera;

import com.grandline.client.settings.ClientSettings;
import com.grandline.gear.GunItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;

/**
 * Grand Line's third-person action camera (client only).
 *
 * <p>In the third-person back view the camera has its own yaw and pitch, turned by the mouse with no smoothing (no input
 * delay). Two modes:
 * <ul>
 *   <li><b>Exploration</b>: the camera orbits freely; the character turns to face where it moves (W runs away from the
 *   camera, S runs towards it).</li>
 *   <li><b>Shift Lock</b> (toggle, Left Shift by default): the character faces the camera's direction, movement strafes
 *   relative to the camera, and the view sits over the right shoulder with a reticle.</li>
 * </ul>
 * Aim always follows the camera: any attack, item use or ability first turns the character to the camera's direction and
 * sends that rotation to the server at once (before the attack packet), so the reticle, the attack and the server's check
 * agree; facing then stays locked to the camera for a moment ({@link #COMBAT_TICKS}) so multi-hit moves keep their aim.
 * Picking (what the reticle points at) uses the camera's direction too.
 *
 * <p>Distance eases out smoothly but pulls in at once when terrain is in the way (no clipping). Small responses to
 * sprinting, dashing, swimming, ships, gun aiming, held charges and nearby bosses adjust distance and FOV; reduced
 * motion turns those off. Everything is time-based, so it behaves the same at any frame rate.
 */
public final class GrandLineCamera {
	static final float MAX_PITCH = 80f;
	static final int COMBAT_TICKS = 30;
	private static final double[] DISTANCES = {5.5, 7.5, 9.5};
	private static final double[] SENSITIVITY = {0.75, 1.0, 1.3};

	private static float camYaw, camPitch;
	private static boolean initialised;
	private static boolean shiftLock;
	private static int combatTicks;
	private static double zoom = 1.0;          // = / - keys
	private static double distance = -1;       // the camera's current distance (smoothed)
	private static double shoulder;            // 0 = centred, 1 = over the right shoulder (smoothed)
	private static double extraTarget, extra;  // movement responses: blocks added to the distance
	private static double fovTarget = 1, fov = 1;
	private static double pulse;               // a short burst (dash): decays to 0
	private static double camY = Double.NaN;   // smoothed pivot height (jump/land lag)
	private static long lastNanos;
	private static boolean bossNear;
	private static double camX, camYPos, camZ;
	private static boolean hasCam;
	private static final double AIM_RANGE = 96;
	private static boolean charging;

	private GrandLineCamera() {
	}

	// ============================================================================================== state

	/** Whether the Grand Line camera is driving the view (third-person back view, setting on, a player present). */
	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && ClientSettings.get().customCamera && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
	}

	/** Whether the character's facing is locked to the camera right now (Shift Lock, combat, gun aiming). */
	public static boolean facingLocked() {
		return shiftLock || combatTicks > 0 || aimingGun();
	}

	public static boolean shiftLock() {
		return shiftLock;
	}

	/** Where the camera was placed last frame (CameraRig). */
	static void cameraAt(double x, double y, double z) {
		camX = x;
		camYPos = y;
		camZ = z;
		hasCam = true;
	}

	public static boolean hasCameraPosition() {
		return hasCam;
	}

	public static net.minecraft.world.phys.Vec3 cameraPosition() {
		return new net.minecraft.world.phys.Vec3(camX, camYPos, camZ);
	}

	public static net.minecraft.world.phys.Vec3 lookVector() {
		return net.minecraft.world.phys.Vec3.directionFromRotation(camPitch, camYaw);
	}

	/**
	 * The rotation that points the character's eyes at what the reticle is on, so the server (which aims from the eyes)
	 * hits the same spot the reticle shows, whatever the camera's distance or shoulder offset. {yaw, pitch}.
	 */
	static float[] aimRotation(LocalPlayer p) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return new float[]{camYaw, camPitch};
		}
		net.minecraft.world.phys.Vec3 target = CrosshairPick.aimPoint(mc, AIM_RANGE);
		net.minecraft.world.phys.Vec3 eye = p.getEyePosition();
		double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat < 1.0e-4 && Math.abs(dy) < 1.0e-4) {
			return new float[]{camYaw, camPitch};
		}
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
		// Keep it continuous with the camera's yaw (no 360 jumps) and within the pitch limits.
		yaw = camYaw + Mth.wrapDegrees(yaw - camYaw);
		return new float[]{yaw, Mth.clamp(pitch, -90f, 90f)};
	}

	public static float yaw() {
		return camYaw;
	}

	public static float pitch() {
		return camPitch;
	}

	private static boolean survivalLike(LocalPlayer p) {
		var mode = Minecraft.getInstance().gameMode == null ? null : Minecraft.getInstance().gameMode.getPlayerMode();
		return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
	}

	static boolean aimingGun() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.options.keyUse.isDown() && mc.player.getMainHandItem().getItem() instanceof GunItem;
	}

	/** Shift Lock key pressed. Riding and creative/spectator flying keep Shift for vanilla use, so it does not toggle there. */
	public static void toggleShiftLock() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || p.isPassenger() || !survivalLike(p)) {
			return;
		}
		shiftLock = !shiftLock;
		if (shiftLock && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK && ClientSettings.get().customCamera) {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		if (shiftLock) {
			faceCamera(false);
		}
		p.displayClientMessage(net.minecraft.network.chat.Component.translatable(shiftLock ? "message.grandline.shift_lock.on"
				: "message.grandline.shift_lock.off").withStyle(net.minecraft.ChatFormatting.GRAY), true);
	}

	public static void zoom(int dir) {
		zoom = Mth.clamp(zoom + dir * 0.1, 0.6, 1.5);
	}

	/** Called on join: the action camera is the default view. */
	public static void onJoin() {
		Minecraft mc = Minecraft.getInstance();
		initialised = false;
		shiftLock = false;
		combatTicks = 0;
		distance = -1;
		camY = Double.NaN;
		if (ClientSettings.get().customCamera && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	private static void ensureInit(LocalPlayer p) {
		if (!initialised) {
			camYaw = p.getYRot();
			camPitch = Mth.clamp(p.getXRot(), -MAX_PITCH, MAX_PITCH);
			initialised = true;
		}
	}

	// ============================================================================================== input

	/** Mouse movement while the Grand Line camera is active (replaces the player's own turn). */
	public static void turn(LocalPlayer p, double dx, double dy) {
		ensureInit(p);
		double s = SENSITIVITY[Mth.clamp(ClientSettings.get().cameraSensitivity, 0, 2)];
		float oldYaw = camYaw, oldPitch = camPitch;
		camYaw += (float) (dx * 0.15 * s);
		camPitch = Mth.clamp(camPitch + (float) (dy * 0.15 * s), -MAX_PITCH, MAX_PITCH);
		if (facingLocked() && !p.isPassenger()) {
			// The character follows the camera's turn this frame (Entity.turn style: the previous rotation moves too, so
			// nothing lags); each tick then re-aims it exactly at the reticle's point.
			float dYaw = camYaw - oldYaw, dPitch = camPitch - oldPitch;
			p.setYRot(p.getYRot() + dYaw);
			p.setXRot(Mth.clamp(p.getXRot() + dPitch, -90f, 90f));
			p.yRotO += dYaw;
			p.xRotO += dPitch;
		}
	}

	/**
	 * Turns the character to the camera's direction and tells the server now, before an attack, item use or ability
	 * request goes out, so the server aims where the reticle is. Keeps facing locked for {@link #COMBAT_TICKS}.
	 */
	public static void faceCamera(boolean combat) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || !active() || p.isPassenger()) {
			return;
		}
		ensureInit(p);
		if (combat) {
			combatTicks = COMBAT_TICKS;
		}
		float[] aim = aimRotation(p);
		p.setYRot(aim[0]);
		p.setXRot(aim[1]);
		p.yRotO = aim[0];
		p.xRotO = aim[1];
		p.setYHeadRot(aim[0]);
		p.yBodyRot = aim[0];
		if (p.connection != null) {
			p.connection.send(new ServerboundMovePlayerPacket.Rot(aim[0], aim[1], p.onGround()));
		}
	}

	/** A move key is being held (a charge): keep facing locked and give the view a little more room. */
	public static void holdCombat(boolean charge) {
		if (combatTicks < COMBAT_TICKS) {
			combatTicks = COMBAT_TICKS;
		}
		charging |= charge;
	}

	/**
	 * Exploration movement, after the keyboard is read: the character turns towards the camera-relative input direction
	 * (quickly, not instantly) and moves exactly in that direction while it turns. In Shift Lock, combat, riding or
	 * other views the input is left as it is (vanilla strafing).
	 */
	public static void steer(net.minecraft.client.player.Input input) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || !active() || p.isPassenger() || facingLocked()) {
			return;
		}
		ensureInit(p);
		float fwd = input.forwardImpulse, left = input.leftImpulse;
		double mag = Math.min(1.0, Math.sqrt(fwd * fwd + left * left));
		// Head follows the camera's pitch gently while exploring.
		p.setXRot(p.getXRot() + (camPitch * 0.4f - p.getXRot()) * 0.3f);
		if (mag < 1.0e-3) {
			return;
		}
		float target = camYaw - (float) Math.toDegrees(Math.atan2(left, fwd));
		float diff = Mth.wrapDegrees(target - p.getYRot());
		float step = Mth.clamp(diff * 0.45f, -40f, 40f);
		if (Math.abs(diff) < 1f) {
			step = diff;
		}
		p.setYRot(p.getYRot() + step);
		double delta = Math.toRadians(Mth.wrapDegrees(target - p.getYRot()));
		input.forwardImpulse = (float) (mag * Math.cos(delta));
		input.leftImpulse = (float) (mag * Math.sin(delta));
	}

	// ============================================================================================== per tick

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null) {
			initialised = false;
			return;
		}
		if (!active()) {
			initialised = false; // re-sync from the player when the view comes back
			return;
		}
		ensureInit(p);
		if (combatTicks > 0) {
			combatTicks--;
		}
		if (facingLocked() && !p.isPassenger()) {
			float[] aim = aimRotation(p);
			p.setYRot(aim[0]);
			p.setXRot(aim[1]);
			p.yBodyRot = Mth.rotLerp(0.6f, p.yBodyRot, aim[0]); // the body turns with the camera, smoothly
		}
		boolean reduced = ClientSettings.get().reducedMotion;
		// Movement responses (distance in blocks, FOV as a multiplier).
		double add = 0;
		double f = 1.0;
		if (p.isPassenger()) {
			add += 3.5; // ships are big
		} else if (p.isSwimming() || p.isInWater()) {
			add -= 1.5;
		} else if (p.isSprinting()) {
			add += 0.6;
		}
		if (bossNear) {
			add += 1.5;
		}
		if (charging) {
			add += 1.5;
			f *= 1.04;
		}
		charging = false;
		if (aimingGun()) {
			add -= 2.5;
			f *= 0.88;
		}
		double horizontal = Math.hypot(p.getDeltaMovement().x, p.getDeltaMovement().z);
		if (horizontal > 0.75) {
			pulse = 1.0; // a dash or flash step
		}
		pulse = Math.max(0, pulse - 0.08);
		if (reduced) {
			add = p.isPassenger() ? 3.5 : 0;
			f = 1.0;
			pulse = 0;
		}
		extraTarget = add + pulse * 1.2;
		fovTarget = f * (1 + pulse * 0.05);
		if (p.tickCount % 10 == 0) {
			bossNear = !p.level().getEntitiesOfClass(com.grandline.entity.SaberBoss.class, p.getBoundingBox().inflate(28)).isEmpty();
		}
	}

	// ============================================================================================== per frame (camera)

	/** Seconds since the last frame (clamped), for frame-rate independent smoothing. */
	static double frameSeconds() {
		long now = System.nanoTime();
		double dt = lastNanos == 0 ? 0.016 : (now - lastNanos) / 1.0e9;
		lastNanos = now;
		return Mth.clamp(dt, 0.0, 0.1);
	}

	/** 1 - e^(-rate * dt): the fraction to move this frame for an exponential ease at {@code rate} per second. */
	static double ease(double rate, double dt) {
		return 1 - Math.exp(-rate * dt);
	}

	static double baseDistance() {
		return DISTANCES[Mth.clamp(ClientSettings.get().cameraDistance, 0, 2)] * zoom;
	}

	/** Advances the smoothed values for this frame; returns the wanted distance before collision. */
	static double frame(double dt) {
		extra += (extraTarget - extra) * ease(4, dt);
		fov += (fovTarget - fov) * ease(6, dt);
		shoulder += ((shiftLock ? 1 : 0) - shoulder) * ease(8, dt); // only the Shift Lock toggle moves the view
		return Math.max(2.0, baseDistance() + extra);
	}

	/** The camera's distance after collision: pulls in at once, eases back out. */
	static double settle(double allowed, double dt) {
		if (distance < 0 || allowed < distance) {
			distance = allowed;
		} else {
			distance += (allowed - distance) * ease(5, dt);
		}
		return distance;
	}

	static double shoulderAmount() {
		return shoulder;
	}

	/** Smoothed pivot height: a slight lag on jumps and landings (none with reduced motion). */
	static double pivotY(double target, double dt) {
		if (Double.isNaN(camY) || ClientSettings.get().reducedMotion || Math.abs(target - camY) > 4) {
			camY = target;
		} else {
			camY += (target - camY) * ease(14, dt);
		}
		return camY;
	}

	/** FOV multiplier for the current responses. */
	public static double fovScale() {
		return active() ? fov : 1.0;
	}
}
