package openblocks.registry;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import openblocks.OpenBlocks;
import openblocks.entity.Cartographer;
import openblocks.entity.GoldenEye;
import openblocks.entity.Luggage;
import openblocks.entity.Magnet;
import openblocks.entity.MiniMe;
import openblocks.entity.MountedBlock;
import openblocks.entity.NoFlyXpOrb;

public class OBEntities {

	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, OpenBlocks.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<NoFlyXpOrb>> NO_FLY_XP_ORB = ENTITIES.register("xp_orb_no_fly",
			() -> EntityType.Builder.<NoFlyXpOrb>of(NoFlyXpOrb::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(6)
					.updateInterval(20)
					.build("xp_orb_no_fly"));

	public static final DeferredHolder<EntityType<?>, EntityType<Luggage>> LUGGAGE = ENTITIES.register("luggage",
			() -> EntityType.Builder.of(Luggage::new, MobCategory.MISC)
					.sized(0.5f, 0.5f)
					.build("luggage"));

	public static final DeferredHolder<EntityType<?>, EntityType<MiniMe>> MINI_ME = ENTITIES.register("mini_me",
			() -> EntityType.Builder.of(MiniMe::new, MobCategory.MISC)
					.sized(0.6f, 0.95f)
					.build("mini_me"));

	public static final DeferredHolder<EntityType<?>, EntityType<GoldenEye>> GOLDEN_EYE = ENTITIES.register("golden_eye",
			() -> EntityType.Builder.<GoldenEye>of(GoldenEye::new, MobCategory.MISC)
					.sized(0.25f, 0.25f)
					.clientTrackingRange(4)
					.updateInterval(4)
					.build("golden_eye"));

	public static final DeferredHolder<EntityType<?>, EntityType<Cartographer>> CARTOGRAPHER = ENTITIES.register("cartographer",
			() -> EntityType.Builder.<Cartographer>of(Cartographer::new, MobCategory.MISC)
					.sized(0.4f, 0.4f)
					.fireImmune()
					.clientTrackingRange(4)
					.updateInterval(5)
					.build("cartographer"));

	// synced every tick, like legacy
	public static final DeferredHolder<EntityType<?>, EntityType<Magnet>> MAGNET = ENTITIES.register("magnet",
			() -> EntityType.Builder.<Magnet>of(Magnet::new, MobCategory.MISC)
					.sized(Magnet.SIZE, Magnet.SIZE)
					.fireImmune()
					.clientTrackingRange(4)
					.updateInterval(1)
					.build("magnet"));

	public static final DeferredHolder<EntityType<?>, EntityType<MountedBlock>> MOUNTED_BLOCK = ENTITIES.register("mounted_block",
			() -> EntityType.Builder.<MountedBlock>of(MountedBlock::new, MobCategory.MISC)
					.sized(0.98f, 0.98f)
					.clientTrackingRange(4)
					.updateInterval(1)
					.build("mounted_block"));

	public static final DeferredRegister<EntityDataSerializer<?>> DATA_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, OpenBlocks.MODID);

	// the Mini Me owner, with its skin
	public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<ResolvableProfile>>> OPTIONAL_PROFILE = DATA_SERIALIZERS.register("optional_profile",
			() -> EntityDataSerializer.forValueType(ByteBufCodecs.optional(ResolvableProfile.STREAM_CODEC)));

	public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Cartographer.Progress>> CARTOGRAPHER_PROGRESS = DATA_SERIALIZERS.register("cartographer_progress",
			() -> EntityDataSerializer.forValueType(Cartographer.Progress.STREAM_CODEC));

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(LUGGAGE.get(), Luggage.createAttributes().build());
		event.put(MINI_ME.get(), MiniMe.createAttributes().build());
	}
}
