package openblocks.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import openblocks.block.entity.TankBlockEntity;
import openblocks.block.entity.VacuumHopperBlockEntity;
import openblocks.block.entity.XpBottlerBlockEntity;

public class OBCapabilities {

	public static void register(RegisterCapabilitiesEvent event) {
		// NeoForge only attaches the bucket wrapper to exact BucketItem instances, not subclasses
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidBucketWrapper(stack), OBItems.XP_BUCKET.get());

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OBBlockEntities.TANK.get(), (tank, side) -> tank.getFluidHandler());
		// only on the sides configured in the GUI; null (no side) gets full automation access
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OBBlockEntities.XP_BOTTLER.get(), XpBottlerBlockEntity::getFluidHandler);
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OBBlockEntities.XP_BOTTLER.get(), XpBottlerBlockEntity::getItemHandler);
		// like legacy, only output sides are accessible (extract/drain only); null (no side) gets the same handlers
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, OBBlockEntities.VACUUM_HOPPER.get(), VacuumHopperBlockEntity::getItemHandler);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, OBBlockEntities.VACUUM_HOPPER.get(), VacuumHopperBlockEntity::getFluidHandler);
		// unlike legacy, only single (unstacked) tank items can be filled or drained
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new FluidHandlerItemStack(OBComponents.TANK_CONTENTS, stack, TankBlockEntity.getTankCapacity()), OBItems.TANK.get());
	}
}
