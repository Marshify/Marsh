package com.grandline.treasure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * A treasure chest. Using it opens it for that player only (TreasureService): Beli, then a per-player refill timer.
 * It never holds items and has no menu, so adventure mode's container lock does not apply to it.
 */
public final class TreasureChestBlock extends HorizontalDirectionalBlock {
	private final TreasureTier tier;
	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

	public TreasureChestBlock(TreasureTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		boolean big = tier == TreasureTier.LEGENDARY;
		// The model's footprint (front to the north), turned for each facing.
		double x1 = big ? 0.5 : 1, x2 = big ? 15.5 : 15, z1 = big ? 2 : 2.5, z2 = big ? 14 : 13.5, h = big ? 15.5 : 14.5;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			shapes.put(d, switch (d) {
				case NORTH -> Block.box(x1, 0, z1, x2, h, z2);
				case SOUTH -> Block.box(16 - x2, 0, 16 - z2, 16 - x1, h, 16 - z1);
				case WEST -> Block.box(z1, 0, 16 - x2, z2, h, 16 - x1);
				default -> Block.box(16 - z2, 0, x1, 16 - z1, h, x2);
			});
		}
	}

	public TreasureTier tier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer sp) {
			TreasureService.open(sp, pos, state);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.25f), 0.7f);
	private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.35f, 0.7f, 1.0f), 0.6f);

	/** Legendary chests shed a few golden motes; rare ones a rarer blue glint. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (tier == TreasureTier.LEGENDARY) {
			for (int i = 0; i < 2; i++) {
				level.addParticle(GOLD_DUST, pos.getX() + 0.1 + random.nextDouble() * 0.8, pos.getY() + 0.3 + random.nextDouble() * 0.8,
						pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0, 0.01, 0);
			}
			if (random.nextInt(4) == 0) {
				level.addParticle(ParticleTypes.WAX_OFF, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0,
						pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
			}
		} else if (tier == TreasureTier.RARE && random.nextInt(3) == 0) {
			level.addParticle(BLUE_DUST, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.6 + random.nextDouble() * 0.4,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.01, 0);
		}
	}
}
