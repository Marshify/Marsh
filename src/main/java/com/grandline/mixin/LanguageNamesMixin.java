package com.grandline.mixin;

import com.grandline.names.NamesLanguage;
import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Whatever language the game installs (on start and on every resource reload), Grand Line's renamed names are laid on
 * top of it (see GrandLineNames). Display only: the language's own entries are never changed.
 */
@Mixin(Language.class)
public abstract class LanguageNamesMixin {
	@ModifyVariable(method = "inject", at = @At("HEAD"), argsOnly = true)
	private static Language grandline$withRenamedNames(Language language) {
		return NamesLanguage.wrap(language);
	}
}
