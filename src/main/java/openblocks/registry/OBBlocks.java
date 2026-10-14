package openblocks.registry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.block.AutoAnvilBlock;
import openblocks.block.AutoEnchantmentTableBlock;
import openblocks.block.BearTrapBlock;
import openblocks.block.BigButtonBlock;
import openblocks.block.CanvasBlock;
import openblocks.block.DonationStationBlock;
import openblocks.block.DrawingTableBlock;
import openblocks.block.PaintCanBlock;
import openblocks.block.PaintMixerBlock;
import openblocks.block.PathBlock;
import openblocks.block.BlockBreakerBlock;
import openblocks.block.BlockPlacerBlock;
import openblocks.block.ElevatorBlock;
import openblocks.block.FanBlock;
import openblocks.block.FlagBlock;
import openblocks.block.GraveBlock;
import openblocks.block.HealBlock;
import openblocks.block.ItemDropperBlock;
import openblocks.block.JadedLadderBlock;
import openblocks.block.RopeLadderBlock;
import openblocks.block.RotatingElevatorBlock;
import openblocks.block.ScaffoldingBlock;
import openblocks.block.SpongeBlock;
import openblocks.block.SprinklerBlock;
import openblocks.block.TankBlock;
import openblocks.block.TargetBlock;
import openblocks.block.VacuumHopperBlock;
import openblocks.block.XpBottlerBlock;
import openblocks.block.XpDrainBlock;
import openblocks.block.XpShowerBlock;
import openblocks.item.FlagItem;

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

	public static final DeferredBlock<XpBottlerBlock> XP_BOTTLER = registerWithItem("xp_bottler", () -> new XpBottlerBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.STONE)));

	public static final DeferredBlock<GraveBlock> GRAVE = registerWithItem("grave", () -> new GraveBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.DIRT)
			.strength(1.0f, 2000.0f)
			.noOcclusion()
			.sound(SoundType.GRAVEL)));

	public static final DeferredBlock<VacuumHopperBlock> VACUUM_HOPPER = registerWithItem("vacuum_hopper", () -> new VacuumHopperBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.sound(SoundType.STONE)));

	public static final DeferredBlock<ItemDropperBlock> ITEM_DROPPER = registerWithItem("item_dropper", () -> new ItemDropperBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.STONE)));

	public static final DeferredBlock<BlockBreakerBlock> BLOCK_BREAKER = registerWithItem("block_breaker", () -> new BlockBreakerBlock(machineProperties()));
	public static final DeferredBlock<BlockPlacerBlock> BLOCK_PLACER = registerWithItem("block_placer", () -> new BlockPlacerBlock(machineProperties()));

	public static final DeferredBlock<AutoAnvilBlock> AUTO_ANVIL = registerWithItem("auto_anvil", () -> new AutoAnvilBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(5.0f, 1200.0f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.sound(SoundType.ANVIL)));

	public static final DeferredBlock<AutoEnchantmentTableBlock> AUTO_ENCHANTMENT_TABLE = registerWithItem("auto_enchantment_table", () -> new AutoEnchantmentTableBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_RED)
			.strength(5.0f, 1200.0f)
			.requiresCorrectToolForDrops()
			.lightLevel(state -> 7)
			.sound(SoundType.STONE)));

	public static final DeferredBlock<DonationStationBlock> DONATION_STATION = registerWithItem("donation_station", () -> new DonationStationBlock(machineProperties().noOcclusion()));

	public static final DeferredBlock<DrawingTableBlock> DRAWING_TABLE = registerWithItem("drawing_table", () -> new DrawingTableBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.5f)
			.ignitedByLava()
			.sound(SoundType.WOOD)));

	public static final DeferredBlock<PaintMixerBlock> PAINT_MIXER = registerWithItem("paint_mixer", () -> new PaintMixerBlock(machineProperties().noOcclusion()));

	// item registered in OBItems, it keeps the paint
	public static final DeferredBlock<PaintCanBlock> PAINT_CAN = BLOCKS.register("paint_can", () -> new PaintCanBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.instabreak()
			.noOcclusion()
			.pushReaction(PushReaction.DESTROY)
			.sound(SoundType.LANTERN)));

	public static final DeferredBlock<CanvasBlock> CANVAS = registerWithItem("canvas", () -> new CanvasBlock(canvasProperties()));
	// only made by painting blocks that do not fill their space, so no item, like legacy
	public static final DeferredBlock<CanvasBlock> CANVAS_GLASS = BLOCKS.register("canvas_glass", () -> new CanvasBlock(canvasProperties()
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)));

	public static final DeferredBlock<FanBlock> FAN = registerWithItem("fan", () -> new FanBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.0f)
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.pushReaction(PushReaction.DESTROY)
			.sound(SoundType.METAL)));

	public static final Map<DyeColor, DeferredBlock<FlagBlock>> FLAGS = registerFlags();

	// no collision, so mobs walk into the trap
	public static final DeferredBlock<BearTrapBlock> BEAR_TRAP = registerWithItem("bear_trap", () -> new BearTrapBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.noCollission()
			.noOcclusion()
			.sound(SoundType.METAL)));

	private static Map<DyeColor, DeferredBlock<FlagBlock>> registerFlags() {
		final Map<DyeColor, DeferredBlock<FlagBlock>> result = new EnumMap<>(DyeColor.class);
		for (DyeColor color : DyeColor.values()) {
			final DeferredBlock<FlagBlock> block = BLOCKS.register(color.getSerializedName() + "_flag", () -> new FlagBlock(color, BlockBehaviour.Properties.of()
					.mapColor(color)
					.instabreak()
					.noCollission()
					.noOcclusion()
					.pushReaction(PushReaction.DESTROY)
					.sound(SoundType.WOOL)));
			OBItems.ITEMS.register(block.getId().getPath(), () -> new FlagItem(block.get(), new Item.Properties()));
			result.put(color, block);
		}
		return Collections.unmodifiableMap(result);
	}

	public static final DeferredBlock<SprinklerBlock> SPRINKLER = registerWithItem("sprinkler", () -> new SprinklerBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.sound(SoundType.METAL)));

	// legacy "ladder", the jaded ladder
	public static final DeferredBlock<JadedLadderBlock> LADDER = registerWithItem("ladder", () -> new JadedLadderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR)));

	public static final DeferredBlock<RopeLadderBlock> ROPE_LADDER = registerWithItem("rope_ladder", () -> new RopeLadderBlock(BlockBehaviour.Properties.of()
			.forceSolidOff()
			.strength(0.4f)
			.noOcclusion()
			.pushReaction(PushReaction.DESTROY)
			.sound(SoundType.LADDER)));

	public static final DeferredBlock<SpongeBlock> SPONGE = registerWithItem("sponge", () -> new SpongeBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW)
			.strength(1.0f)
			.sound(SoundType.WOOL)));

	// item burn time comes from the furnace_fuels data map
	public static final DeferredBlock<ScaffoldingBlock> SCAFFOLDING = registerWithItem("scaffolding", () -> new ScaffoldingBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(0.1f)
			.randomTicks()
			.noOcclusion()
			.ignitedByLava()
			.isValidSpawn(Blocks::never)
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)
			.sound(SoundType.WOOD)));

	public static final DeferredBlock<HealBlock> HEAL = registerWithItem("heal", () -> new HealBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PINK)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.sound(SoundType.STONE)));

	public static final DeferredBlock<BigButtonBlock> BIG_BUTTON = registerWithItem("big_button", () -> new BigButtonBlock(BlockSetType.STONE, bigButtonProperties()));
	public static final DeferredBlock<BigButtonBlock> BIG_BUTTON_WOOD = registerWithItem("big_button_wood", () -> new BigButtonBlock(BlockSetType.OAK, bigButtonProperties()));

	public static final DeferredBlock<TargetBlock> TARGET = registerWithItem("target", () -> new TargetBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.SNOW)
			.strength(1.0f)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.lightLevel(state -> 4)
			.isRedstoneConductor((state, level, pos) -> false)
			.sound(SoundType.STONE)));

	public static final DeferredBlock<PathBlock> PATH = registerWithItem("path", () -> new PathBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.0f)
			.noCollission()
			.noOcclusion()
			.pushReaction(PushReaction.DESTROY)
			.sound(SoundType.STONE)));

	private static BlockBehaviour.Properties bigButtonProperties() {
		return BlockBehaviour.Properties.of()
				.strength(1.0f)
				.noOcclusion()
				.pushReaction(PushReaction.DESTROY);
	}

	private static BlockBehaviour.Properties canvasProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.WOOL)
				.strength(1.0f)
				.ignitedByLava()
				.isRedstoneConductor(CanvasBlock::isRedstoneConductor)
				.sound(SoundType.WOOL);
	}

	private static BlockBehaviour.Properties machineProperties() {
		return BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE)
				.strength(1.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.STONE);
	}

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
