package openblocks.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.block.ElevatorBlock;
import openblocks.block.RotatingElevatorBlock;
import openblocks.block.TankBlock;
import openblocks.block.XpDrainBlock;
import openblocks.block.XpShowerBlock;

public class OBBlocks {

	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OpenBlocks.MODID);

	public static final Map<DyeColor, DeferredBlock<ElevatorBlock>> ELEVATORS = registerColored("elevator", ElevatorBlock::new);
	public static final Map<DyeColor, DeferredBlock<RotatingElevatorBlock>> ROTATING_ELEVATORS = registerColored("elevator_rotating", RotatingElevatorBlock::new);

	// fluids are registered before blocks, so the fluid instance is already available here
	public static final DeferredBlock<LiquidBlock> XP_JUICE = BLOCKS.register("xp_juice", () -> new LiquidBlock(OBFluids.XP_JUICE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
			.mapColor(MapColor.COLOR_LIGHT_GREEN)
			.lightLevel(state -> 10)));

	// item is registered in OBItems, since it needs a custom class
	public static final DeferredBlock<TankBlock> TANK = BLOCKS.register("tank", () -> new TankBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isRedstoneConductor((state, level, pos) -> false)
			.lightLevel(state -> state.getValue(TankBlock.LIGHT))
			.sound(SoundType.STONE)));

	public static final DeferredBlock<XpDrainBlock> XP_DRAIN = registerWithItem("xp_drain", () -> new XpDrainBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.0f)
			.noOcclusion()
			.sound(SoundType.METAL)));

	public static final DeferredBlock<XpShowerBlock> XP_SHOWER = registerWithItem("xp_shower", () -> new XpShowerBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.noOcclusion()
			.sound(SoundType.STONE)));

	private static <T extends Block> DeferredBlock<T> registerWithItem(String id, Supplier<T> factory) {
		final DeferredBlock<T> block = BLOCKS.register(id, factory);
		OBItems.ITEMS.registerSimpleBlockItem(block);
		return block;
	}

	private static <T extends Block> Map<DyeColor, DeferredBlock<T>> registerColored(String id, BiFunction<DyeColor, BlockBehaviour.Properties, T> factory) {
		final Map<DyeColor, DeferredBlock<T>> result = new EnumMap<>(DyeColor.class);
		for (DyeColor color : DyeColor.values())
			result.put(color, registerWithItem(color.getSerializedName() + "_" + id, () -> factory.apply(color, elevatorProperties(color))));
		return Collections.unmodifiableMap(result);
	}

	private static BlockBehaviour.Properties elevatorProperties(DyeColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(1.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.STONE);
	}
}
