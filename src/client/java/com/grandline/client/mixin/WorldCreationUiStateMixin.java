package com.grandline.client.mixin;

import com.grandline.GrandLineMod;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;

/** Makes "Grand Line (ocean & islands)" the default world type on the Create New World screen. */
@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
	private static final ResourceKey<WorldPreset> GRAND_LINE =
			ResourceKey.create(Registries.WORLD_PRESET, GrandLineMod.id("grand_line"));

	@Inject(method = "<init>", at = @At("RETURN"))
	private void grandline$defaultToGrandLine(Path path, WorldCreationContext context, Optional<ResourceKey<WorldPreset>> preset,
			OptionalLong seed, CallbackInfo ci) {
		// Only replace the plain default; keep any explicitly requested preset (e.g. from a server or modpack).
		if (preset.isPresent() && !preset.get().equals(WorldPresets.NORMAL)) {
			return;
		}
		WorldCreationUiState self = (WorldCreationUiState) (Object) this;
		context.worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder(GRAND_LINE)
				.ifPresent(holder -> self.setWorldType(new WorldCreationUiState.WorldTypeEntry(holder)));
	}
}
