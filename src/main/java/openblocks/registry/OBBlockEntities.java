package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;
import openblocks.block.entity.BearTrapBlockEntity;
import openblocks.block.entity.BigButtonBlockEntity;
import openblocks.block.entity.CanvasBlockEntity;
import openblocks.block.entity.DonationStationBlockEntity;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.block.entity.FanBlockEntity;
import openblocks.block.entity.PaintCanBlockEntity;
import openblocks.block.entity.PaintMixerBlockEntity;
import openblocks.block.entity.SprinklerBlockEntity;
import openblocks.block.entity.BlockBreakerBlockEntity;
import openblocks.block.entity.BlockPlacerBlockEntity;
import openblocks.block.entity.GraveBlockEntity;
import openblocks.block.entity.HealBlockEntity;
import openblocks.block.entity.SkyBlockEntity;
import openblocks.block.entity.ItemDropperBlockEntity;
import openblocks.block.entity.TankBlockEntity;
import openblocks.block.entity.TrophyBlockEntity;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.block.entity.XpBottlerBlockEntity;
import openblocks.block.entity.XpDrainBlockEntity;
import openblocks.block.entity.XpShowerBlockEntity;

public class OBBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenBlocks.MODID);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK = BLOCK_ENTITIES.register("tank",
			() -> BlockEntityType.Builder.of(TankBlockEntity::new, OBBlocks.TANK.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XpDrainBlockEntity>> XP_DRAIN = BLOCK_ENTITIES.register("xp_drain",
			() -> BlockEntityType.Builder.of(XpDrainBlockEntity::new, OBBlocks.XP_DRAIN.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XpShowerBlockEntity>> XP_SHOWER = BLOCK_ENTITIES.register("xp_shower",
			() -> BlockEntityType.Builder.of(XpShowerBlockEntity::new, OBBlocks.XP_SHOWER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE = BLOCK_ENTITIES.register("grave",
			() -> BlockEntityType.Builder.of(GraveBlockEntity::new, OBBlocks.GRAVE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XpBottlerBlockEntity>> XP_BOTTLER = BLOCK_ENTITIES.register("xp_bottler",
			() -> BlockEntityType.Builder.of(XpBottlerBlockEntity::new, OBBlocks.XP_BOTTLER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VacuumHopperBlockEntity>> VACUUM_HOPPER = BLOCK_ENTITIES.register("vacuum_hopper",
			() -> BlockEntityType.Builder.of(VacuumHopperBlockEntity::new, OBBlocks.VACUUM_HOPPER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemDropperBlockEntity>> ITEM_DROPPER = BLOCK_ENTITIES.register("item_dropper",
			() -> BlockEntityType.Builder.of(ItemDropperBlockEntity::new, OBBlocks.ITEM_DROPPER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockBreakerBlockEntity>> BLOCK_BREAKER = BLOCK_ENTITIES.register("block_breaker",
			() -> BlockEntityType.Builder.of(BlockBreakerBlockEntity::new, OBBlocks.BLOCK_BREAKER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlockPlacerBlockEntity>> BLOCK_PLACER = BLOCK_ENTITIES.register("block_placer",
			() -> BlockEntityType.Builder.of(BlockPlacerBlockEntity::new, OBBlocks.BLOCK_PLACER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoAnvilBlockEntity>> AUTO_ANVIL = BLOCK_ENTITIES.register("auto_anvil",
			() -> BlockEntityType.Builder.of(AutoAnvilBlockEntity::new, OBBlocks.AUTO_ANVIL.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoEnchantmentTableBlockEntity>> AUTO_ENCHANTMENT_TABLE = BLOCK_ENTITIES.register("auto_enchantment_table",
			() -> BlockEntityType.Builder.of(AutoEnchantmentTableBlockEntity::new, OBBlocks.AUTO_ENCHANTMENT_TABLE.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DonationStationBlockEntity>> DONATION_STATION = BLOCK_ENTITIES.register("donation_station",
			() -> BlockEntityType.Builder.of(DonationStationBlockEntity::new, OBBlocks.DONATION_STATION.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrawingTableBlockEntity>> DRAWING_TABLE = BLOCK_ENTITIES.register("drawing_table",
			() -> BlockEntityType.Builder.of(DrawingTableBlockEntity::new, OBBlocks.DRAWING_TABLE.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintMixerBlockEntity>> PAINT_MIXER = BLOCK_ENTITIES.register("paint_mixer",
			() -> BlockEntityType.Builder.of(PaintMixerBlockEntity::new, OBBlocks.PAINT_MIXER.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintCanBlockEntity>> PAINT_CAN = BLOCK_ENTITIES.register("paint_can",
			() -> BlockEntityType.Builder.of(PaintCanBlockEntity::new, OBBlocks.PAINT_CAN.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CanvasBlockEntity>> CANVAS = BLOCK_ENTITIES.register("canvas",
			() -> BlockEntityType.Builder.of(CanvasBlockEntity::new, OBBlocks.CANVAS.get(), OBBlocks.CANVAS_GLASS.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FanBlockEntity>> FAN = BLOCK_ENTITIES.register("fan",
			() -> BlockEntityType.Builder.of(FanBlockEntity::new, OBBlocks.FAN.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BearTrapBlockEntity>> BEAR_TRAP = BLOCK_ENTITIES.register("bear_trap",
			() -> BlockEntityType.Builder.of(BearTrapBlockEntity::new, OBBlocks.BEAR_TRAP.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER = BLOCK_ENTITIES.register("sprinkler",
			() -> BlockEntityType.Builder.of(SprinklerBlockEntity::new, OBBlocks.SPRINKLER.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HealBlockEntity>> HEAL = BLOCK_ENTITIES.register("heal",
			() -> BlockEntityType.Builder.of(HealBlockEntity::new, OBBlocks.HEAL.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigButtonBlockEntity>> BIG_BUTTON = BLOCK_ENTITIES.register("big_button",
			() -> BlockEntityType.Builder.of(BigButtonBlockEntity::new, OBBlocks.BIG_BUTTON.get(), OBBlocks.BIG_BUTTON_WOOD.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyBlockEntity>> TROPHY = BLOCK_ENTITIES.register("trophy",
			() -> BlockEntityType.Builder.of(TrophyBlockEntity::new, OBBlocks.TROPHY.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SkyBlockEntity>> SKY = BLOCK_ENTITIES.register("sky",
			() -> BlockEntityType.Builder.of(SkyBlockEntity::new, OBBlocks.SKY.get(), OBBlocks.SKY_INVERTED.get()).build(null));
}
