package openblocks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import openblocks.OpenBlocks;

public class OBFluids {

	public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, OpenBlocks.MODID);
	public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, OpenBlocks.MODID);

	// keeps the legacy translation key, so existing translations still apply
	public static final DeferredHolder<FluidType, FluidType> XP_JUICE_TYPE = FLUID_TYPES.register("xp_juice", () -> new FluidType(FluidType.Properties.create()
			.descriptionId("fluid.openblocks.xp_juice")
			.lightLevel(10)
			.density(800)
			.viscosity(1500)
			.sound(SoundActions.BUCKET_FILL, SoundEvents.EXPERIENCE_ORB_PICKUP)
			.sound(SoundActions.BUCKET_EMPTY, SoundEvents.PLAYER_LEVELUP)));

	public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> XP_JUICE = FLUIDS.register("xp_juice", () -> new BaseFlowingFluid.Source(xpJuiceProperties()));
	public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_XP_JUICE = FLUIDS.register("flowing_xp_juice", () -> new BaseFlowingFluid.Flowing(xpJuiceProperties()));

	private static BaseFlowingFluid.Properties xpJuiceProperties() {
		return new BaseFlowingFluid.Properties(XP_JUICE_TYPE, XP_JUICE, FLOWING_XP_JUICE)
				.bucket(OBItems.XP_BUCKET)
				.block(OBBlocks.XP_JUICE);
	}
}
