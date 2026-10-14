package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.block.entity.GraveBlockEntity;
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
}
