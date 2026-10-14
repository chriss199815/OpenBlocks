package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.block.entity.AutoAnvilBlockEntity;
import openblocks.block.entity.AutoEnchantmentTableBlockEntity;
import openblocks.block.entity.BlockBreakerBlockEntity;
import openblocks.block.entity.BlockPlacerBlockEntity;
import openblocks.block.entity.GraveBlockEntity;
import openblocks.block.entity.ItemDropperBlockEntity;
import openblocks.block.entity.TankBlockEntity;
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
}
