package com.grandline.gear;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.grandline.GrandLineMod;
import com.grandline.combat.fx.FxEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.Vec3;

/**
 * The Saber: its melee damage and attack speed come from the config ({@code saberAttackDamage}, {@code saberAttackSpeed}),
 * so they can be tuned without touching the item. Hits land with a bright slash arc, a ring of sparks and a crisp ring.
 * The Sword stat and gear bonuses add on top exactly as for other swords.
 */
public class SaberItem extends GearSwordItem {
	public SaberItem(Properties properties) {
		super(Tiers.DIAMOND, 3, -2.3F, properties);
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND) {
			return super.getDefaultAttributeModifiers(slot);
		}
		var config = GrandLineMod.config();
		// The player's base attack is 1 damage and 4 attacks per second; the modifiers move them to the configured values.
		return ImmutableMultimap.<Attribute, AttributeModifier>builder()
				.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
						config.saberAttackDamage - 1.0, AttributeModifier.Operation.ADDITION))
				.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
						config.saberAttackSpeed - 4.0, AttributeModifier.Operation.ADDITION))
				.build();
	}

	@Override
	public float getDamage() {
		return (float) GrandLineMod.config().saberAttackDamage - 1.0F;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker.level() instanceof ServerLevel level) {
			Vec3 hit = target.position().add(0, target.getBbHeight() * 0.6, 0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 10, 0.25, 0.3, 0.25, 0.35);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.35F);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.15F, 2.0F);
			if (attacker instanceof ServerPlayer player) {
				Vec3 dir = target.position().subtract(attacker.position()).multiply(1, 0, 1);
				FxEvents.send(player, "saber_hit", hit, dir.lengthSqr() < 1e-6 ? attacker.getLookAngle() : dir.normalize(), 0, 0, 1.0);
			}
		}
		return super.hurtEnemy(stack, target, attacker);
	}
}
