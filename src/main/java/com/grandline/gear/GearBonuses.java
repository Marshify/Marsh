package com.grandline.gear;

import com.grandline.player.StatType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bonuses from gear: damage fractions per combat category (0.10 = +10%), plus flat max health and max energy. */
public record GearBonuses(double melee, double sword, double gun, double fruit, double health, double energy) {
	public static final GearBonuses NONE = new GearBonuses(0, 0, 0, 0, 0, 0);

	public double damage(StatType category) {
		return switch (category) {
			case MELEE -> melee;
			case SWORD -> sword;
			case GUN -> gun;
			case FRUIT -> fruit;
			case DEFENSE -> 0;
		};
	}

	public GearBonuses plus(GearBonuses o) {
		return new GearBonuses(melee + o.melee, sword + o.sword, gun + o.gun, fruit + o.fruit, health + o.health, energy + o.energy);
	}

	/** Tooltip / Build screen lines, one per non-zero bonus. "All combat" when the four damage bonuses are equal. */
	public List<Component> describe() {
		List<Component> lines = new ArrayList<>();
		if (melee > 0 && melee == sword && sword == gun && gun == fruit) {
			lines.add(line("tooltip.grandline.bonus.all_damage", pct(melee)));
		} else {
			if (melee > 0) lines.add(line("tooltip.grandline.bonus.melee", pct(melee)));
			if (sword > 0) lines.add(line("tooltip.grandline.bonus.sword", pct(sword)));
			if (gun > 0) lines.add(line("tooltip.grandline.bonus.gun", pct(gun)));
			if (fruit > 0) lines.add(line("tooltip.grandline.bonus.fruit", pct(fruit)));
		}
		if (health > 0) lines.add(line("tooltip.grandline.bonus.health", num(health), num(health / 2)));
		if (energy > 0) lines.add(line("tooltip.grandline.bonus.energy", num(energy)));
		return lines;
	}

	private static Component line(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.BLUE);
	}

	private static String pct(double fraction) {
		return num(fraction * 100);
	}

	private static String num(double value) {
		return Math.abs(value - Math.rint(value)) < 1e-9 ? String.valueOf((long) Math.rint(value)) : String.format(Locale.ROOT, "%.1f", value);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeDouble(melee);
		buf.writeDouble(sword);
		buf.writeDouble(gun);
		buf.writeDouble(fruit);
		buf.writeDouble(health);
		buf.writeDouble(energy);
	}

	public static GearBonuses read(FriendlyByteBuf buf) {
		return new GearBonuses(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
	}
}
