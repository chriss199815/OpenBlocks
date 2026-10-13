package openblocks.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.entity.XpOrbTargetingEvent;
import openblocks.registry.OBEntities;

/**
 * Port of legacy {@code EntityXPOrbNoFly}: an XP orb that falls straight down instead of flying to players, so XP showers can feed XP drains.
 * Players still pick it up on contact.
 */
public class NoFlyXpOrb extends ExperienceOrb implements IEntityWithComplexSpawn {

	public NoFlyXpOrb(EntityType<? extends NoFlyXpOrb> type, Level level) {
		super(type, level);
	}

	public NoFlyXpOrb(Level level, double x, double y, double z, int value) {
		this(OBEntities.NO_FLY_XP_ORB.get(), level);
		setPos(x, y, z);
		this.value = value;
		setDeltaMovement(0, -0.1f * random.nextFloat(), 0);
	}

	public static void onTargeting(XpOrbTargetingEvent event) {
		// fires on both sides, so client-side movement prediction matches the server
		if (event.getXpOrb() instanceof NoFlyXpOrb) event.setFollowingPlayer(null);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
		// the vanilla orb packet would spawn a plain ExperienceOrb on the client
		return new ClientboundAddEntityPacket(this, entity);
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(value);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		// the renderer picks the orb icon by value
		value = buffer.readVarInt();
	}
}
