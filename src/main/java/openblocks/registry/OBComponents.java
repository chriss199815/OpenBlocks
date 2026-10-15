package openblocks.registry;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import openblocks.OpenBlocks;
import openblocks.common.HeightMapData;
import openblocks.common.TrophyType;
import openblocks.entity.CartographerData;
import openblocks.item.CursorTarget;
import openblocks.item.GoldenEyeTarget;
import openblocks.item.Paint;

public class OBComponents {

	public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, OpenBlocks.MODID);

	/**
	 * Fluid stored in a tank item; replaces the legacy {@code tank} NBT tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> TANK_CONTENTS = COMPONENTS.registerComponentType("tank_contents",
			builder -> builder.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

	/**
	 * Color and uses left of a paint can or brush; replaces the legacy {@code color}/{@code amount} NBT tags.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Paint>> PAINT = COMPONENTS.registerComponentType("paint",
			builder -> builder.persistent(Paint.CODEC).networkSynchronized(Paint.STREAM_CODEC));

	/**
	 * Marks luggage struck by lightning, which carries 54 instead of 27 stacks; replaces the legacy inventory size tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LUGGAGE_SPECIAL = COMPONENTS.registerComponentType("luggage_special",
			builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

	/**
	 * Block a cursor is linked to; replaces the legacy {@code dimension}/coordinates/{@code side} NBT tags.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CursorTarget>> CURSOR_TARGET = COMPONENTS.registerComponentType("cursor_target",
			builder -> builder.persistent(CursorTarget.CODEC).networkSynchronized(CursorTarget.STREAM_CODEC));

	/**
	 * Marks a slimalyzer that is in a slime chunk; replaces the legacy {@code Active} tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> SLIMALYZER_ACTIVE = COMPONENTS.registerComponentType("slimalyzer_active",
			builder -> builder.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

	/**
	 * Mob on a trophy; replaces the legacy {@code entity_id} tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<TrophyType>> TROPHY = COMPONENTS.registerComponentType("trophy",
			builder -> builder.persistent(TrophyType.CODEC).networkSynchronized(TrophyType.STREAM_CODEC));

	/**
	 * Ticks until a trophy can run its behaviour again, kept when it is broken; replaces the legacy {@code cooldown} tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TROPHY_COOLDOWN = COMPONENTS.registerComponentType("trophy_cooldown",
			builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/**
	 * Structure a golden eye is locked on; replaces the legacy {@code Structure}/coordinate tags.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<GoldenEyeTarget>> GOLDEN_EYE_TARGET = COMPONENTS.registerComponentType("golden_eye_target",
			builder -> builder.persistent(GoldenEyeTarget.CODEC).networkSynchronized(GoldenEyeTarget.STREAM_CODEC));

	/**
	 * Cannon a pointer has selected; replaces the legacy {@code lastPoint} tag.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> POINTER_TARGET = COMPONENTS.registerComponentType("pointer_target",
			builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

	/**
	 * Scale of an empty or height map, 1 to 2^scale blocks per pixel; replaces the legacy {@code Scale} tag (on height maps
	 * it was only in the map data).
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAP_SCALE = COMPONENTS.registerComponentType("map_scale",
			builder -> builder.persistent(HeightMapData.SCALE_CODEC).networkSynchronized(ByteBufCodecs.VAR_INT));

	/**
	 * Height map shown by the item; replaces the legacy item damage.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEIGHT_MAP_ID = COMPONENTS.registerComponentType("height_map_id",
			builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

	/**
	 * Map and mapping progress of a picked up cartographer; replaces the legacy tags written by {@code EntityCartographer}.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CartographerData>> CARTOGRAPHER = COMPONENTS.registerComponentType("cartographer",
			builder -> builder.persistent(CartographerData.CODEC).networkSynchronized(CartographerData.STREAM_CODEC));
}
