package com.grandline.gear;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A gun: right-click fires a shot straight where you aim, no ammo needed, with a per-gun reload time. The shot is
 * created on the server only (the client just plays the hand swing), so damage and cooldowns are server-authoritative.
 */
public class GunItem extends Item {
	private final double damage;
	private final float velocity;
	private final int reloadTicks;
	private final int knockback;
	private final SoundEvent sound;

	public GunItem(Properties properties, double damage, float velocity, int reloadTicks, int knockback, SoundEvent sound) {
		super(properties);
		this.damage = damage;
		this.velocity = velocity;
		this.reloadTicks = reloadTicks;
		this.knockback = knockback;
		this.sound = sound;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide) {
			Arrow shot = new Arrow(level, player);
			shot.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 0.5F);
			double gear = player instanceof net.minecraft.server.level.ServerPlayer sp
					? GearService.damageMultiplier(sp, com.grandline.player.StatType.GUN) : 1.0;
			shot.setBaseDamage(damage * gear / velocity); // arrow damage scales with speed; this keeps the listed damage
			shot.setKnockback(knockback);
			shot.pickup = AbstractArrow.Pickup.DISALLOWED;
			shot.setCritArrow(false);
			level.addFreshEntity(shot);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		player.getCooldowns().addCooldown(this, reloadTicks);
		player.swing(hand);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.grandline.gun", String.format(java.util.Locale.ROOT, "%.0f", damage),
				String.format(java.util.Locale.ROOT, "%.1f", reloadTicks / 20.0)).withStyle(net.minecraft.ChatFormatting.GRAY));
		WeaponTooltips.moves(this, tooltip);
	}
}
