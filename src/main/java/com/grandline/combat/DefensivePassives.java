package com.grandline.combat;

import com.grandline.player.PlayerProfileService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Damage-source passives of a fruit (reusable): while a player's fruit is {@code fruitId}, incoming damage matching the
 * passive's test is cancelled on the server before it lands (so no damage, knockback or hit stagger), and the passive's
 * reaction runs (visuals, sound). Checked every hit from the player's saved fruit, so it applies whatever they hold
 * and ends the moment the fruit is lost or replaced.
 *
 * <p>Also classifies attacks: {@link #swordAttack} and {@link #auraEnhanced}.
 */
public final class DefensivePassives {
	public record Passive(Predicate<DamageSource> blocks, BiConsumer<ServerPlayer, DamageSource> onBlock) {
	}

	private static final Map<String, Passive> PASSIVES = new HashMap<>();

	private DefensivePassives() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || !blocked(player, source));
	}

	public static void passive(String fruitId, Passive passive) {
		PASSIVES.put(fruitId, passive);
	}

	/** Whether {@code player}'s fruit passive blocks this damage (and if so, runs its reaction). */
	public static boolean blocked(ServerPlayer player, DamageSource source) {
		Passive p = PASSIVES.get(PlayerProfileService.get(player).powers().fruit());
		if (p == null || !p.blocks().test(source)) {
			return false;
		}
		p.onBlock().accept(player, source);
		return true;
	}

	/**
	 * A sword attack: a sword move or sword combo finisher (damage dealt inside a Sword-scaling power's move), or a plain
	 * melee hit by a living attacker holding a sword (a vanilla SwordItem or a Grand Line sword). Fruit, fighting-style
	 * and gun moves, projectiles and the environment are not.
	 */
	public static boolean swordAttack(DamageSource source) {
		if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
			return false;
		}
		com.grandline.combat.ability.Power power = com.grandline.technique.CombatContext.currentPower();
		if (power != null) {
			return power.scalingStat() == com.grandline.player.StatType.SWORD;
		}
		if (!(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
			return false;
		}
		ItemStack held = attacker.getMainHandItem();
		return held.getItem() instanceof SwordItem || com.grandline.gear.sword.Swords.spec(held.getItem()) != null;
	}

	/** Whether the attacker behind {@code source} is using Aura (a player's Aura, or an Aura-coated enemy). */
	public static boolean auraEnhanced(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer p) {
			return com.grandline.technique.TechniqueService.auraOn(p);
		}
		return source.getEntity() instanceof com.grandline.entity.GrandLineEnemy enemy && enemy.aura();
	}
}
