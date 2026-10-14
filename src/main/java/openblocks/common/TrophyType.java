package openblocks.common;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

/**
 * Port of legacy {@code TrophyHandler.Trophy}: the mobs that drop trophies, with the size of the mob on the trophy and what
 * clicking the trophy does. Legacy entity ids are replaced by their 1.21 names (zombie_pigman → zombified_piglin,
 * villager_golem → iron_golem, snowman → snow_golem, evocation_illager → evoker, vindication_illager → vindicator).
 * Wither and ender dragon are skipped, like legacy.
 */
public enum TrophyType implements StringRepresentable {
	WOLF(EntityType.WOLF),
	CHICKEN(EntityType.CHICKEN, TrophyBehaviors.dropItem(10000, Items.EGG, SoundEvents.CHICKEN_EGG)),
	COW(EntityType.COW, TrophyBehaviors.dropItem(20000, Items.LEATHER)),
	CREEPER(EntityType.CREEPER, TrophyBehaviors::creeper),
	SKELETON(EntityType.SKELETON, TrophyBehaviors::skeleton),
	ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, TrophyBehaviors.dropItem(20000, Items.GOLD_NUGGET)),
	BAT(EntityType.BAT, 1.0, -0.3, null),
	ZOMBIE(EntityType.ZOMBIE),
	WITCH(EntityType.WITCH, 0.35, TrophyBehaviors::witch),
	VILLAGER(EntityType.VILLAGER),
	// legacy showed a tamed ocelot, which is a cat since 1.14
	OCELOT(EntityType.OCELOT),
	SHEEP(EntityType.SHEEP),
	BLAZE(EntityType.BLAZE, TrophyBehaviors::blaze),
	SILVERFISH(EntityType.SILVERFISH),
	SPIDER(EntityType.SPIDER),
	CAVE_SPIDER(EntityType.CAVE_SPIDER, TrophyBehaviors::caveSpider),
	SLIME(EntityType.SLIME, 0.6, null),
	GHAST(EntityType.GHAST, 0.1, 0.3, null),
	ENDERMAN(EntityType.ENDERMAN, 0.3, TrophyBehaviors::enderman),
	MAGMA_CUBE(EntityType.MAGMA_CUBE, 0.6, null),
	SQUID(EntityType.SQUID, 0.3, 0.5, TrophyBehaviors::squid),
	MOOSHROOM(EntityType.MOOSHROOM, TrophyBehaviors::mooshroom),
	IRON_GOLEM(EntityType.IRON_GOLEM, 0.3, null),
	SNOW_GOLEM(EntityType.SNOW_GOLEM, TrophyBehaviors::snowGolem),
	PIG(EntityType.PIG, TrophyBehaviors.dropItem(20000, Items.PORKCHOP)),
	ENDERMITE(EntityType.ENDERMITE),
	GUARDIAN(EntityType.GUARDIAN, TrophyBehaviors::guardian),
	RABBIT(EntityType.RABBIT, TrophyBehaviors.dropItem(20000, Items.CARROT)),
	POLAR_BEAR(EntityType.POLAR_BEAR, TrophyBehaviors.dropItem(20000, Items.COD)),
	SHULKER(EntityType.SHULKER, TrophyBehaviors::shulker),
	HORSE(EntityType.HORSE, 0.35, TrophyBehaviors.dropItem(20000, Items.WHEAT)),
	SKELETON_HORSE(EntityType.SKELETON_HORSE, 0.35, TrophyBehaviors.dropItem(20000, Items.BONE)),
	ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, 0.35, TrophyBehaviors.dropItem(20000, Items.ROTTEN_FLESH)),
	DONKEY(EntityType.DONKEY, 0.35, TrophyBehaviors.dropItem(20000, Items.WHEAT)),
	MULE(EntityType.MULE, 0.35, TrophyBehaviors.dropItem(20000, Items.WHEAT)),
	LLAMA(EntityType.LLAMA, 0.35, TrophyBehaviors::llama),
	ELDER_GUARDIAN(EntityType.ELDER_GUARDIAN, 0.2, 0.3, TrophyBehaviors::guardian),
	WITHER_SKELETON(EntityType.WITHER_SKELETON, TrophyBehaviors.dropItem(50000, Items.WITHER_SKELETON_SKULL)),
	STRAY(EntityType.STRAY, TrophyBehaviors::skeleton),
	HUSK(EntityType.HUSK, TrophyBehaviors.dropItem(20000, Items.FEATHER)),
	ZOMBIE_VILLAGER(EntityType.ZOMBIE_VILLAGER),
	EVOKER(EntityType.EVOKER, TrophyBehaviors::evoker),
	VEX(EntityType.VEX),
	VINDICATOR(EntityType.VINDICATOR, TrophyBehaviors.dropItem(20000, Items.IRON_AXE));

	public static final StringRepresentable.EnumCodec<TrophyType> CODEC = StringRepresentable.fromEnum(TrophyType::values);
	public static final StreamCodec<ByteBuf, TrophyType> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], TrophyType::ordinal);

	/**
	 * Legacy {@code PigZombie}, used for trophies without a type (e.g. placed by commands).
	 */
	public static final TrophyType DEFAULT = ZOMBIFIED_PIGLIN;

	private static final Map<EntityType<?>, TrophyType> BY_ENTITY = new HashMap<>();

	static {
		for (TrophyType type : values())
			BY_ENTITY.put(type.entityType, type);
	}

	private final EntityType<?> entityType;
	private final double scale;
	private final double verticalOffset;
	@Nullable
	private final TrophyBehavior behavior;

	TrophyType(EntityType<?> entityType, double scale, double verticalOffset, @Nullable TrophyBehavior behavior) {
		this.entityType = entityType;
		this.scale = scale;
		this.verticalOffset = verticalOffset;
		this.behavior = behavior;
	}

	TrophyType(EntityType<?> entityType, double scale, @Nullable TrophyBehavior behavior) {
		this(entityType, scale, 0.0, behavior);
	}

	TrophyType(EntityType<?> entityType, @Nullable TrophyBehavior behavior) {
		this(entityType, 0.4, behavior);
	}

	TrophyType(EntityType<?> entityType) {
		this(entityType, null);
	}

	@Nullable
	public static TrophyType forEntity(EntityType<?> type) {
		return BY_ENTITY.get(type);
	}

	public EntityType<?> getEntityType() {
		return entityType;
	}

	public double getScale() {
		return scale;
	}

	public double getVerticalOffset() {
		return verticalOffset;
	}

	@Nullable
	public TrophyBehavior getBehavior() {
		return behavior;
	}

	@Override
	public String getSerializedName() {
		return EntityType.getKey(entityType).getPath();
	}
}
