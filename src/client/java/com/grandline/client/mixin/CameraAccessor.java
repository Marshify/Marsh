package com.grandline.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Camera internals the Grand Line camera drives. */
@Mixin(Camera.class)
public interface CameraAccessor {
	@Invoker("setRotation")
	void grandline$setRotation(float yaw, float pitch);

	@Invoker("setPosition")
	void grandline$setPosition(double x, double y, double z);

	@Invoker("move")
	void grandline$move(double forward, double up, double left);

	@Invoker("getMaxZoom")
	double grandline$getMaxZoom(double distance);
}
