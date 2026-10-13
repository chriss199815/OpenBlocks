package openblocks.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import openblocks.particle.LiquidSprayOptions;
import openblocks.registry.OBParticleTypes;

/**
 * Port of legacy {@code FXLiquidSpray}: a small droplet showing a random quarter of the fluid's still texture.
 */
public class LiquidSprayParticle extends TextureSheetParticle {

	private final float uo;
	private final float vo;

	private LiquidSprayParticle(ClientLevel level, LiquidSprayOptions options, double x, double y, double z, double xd, double yd, double zd) {
		super(level, x, y, z);
		// the velocity constructor would add random motion
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.gravity = options.gravity();
		this.lifetime = 50;
		this.quadSize = 0.1f * options.scale();
		this.hasPhysics = true;
		setSize(0.2f, 0.2f);

		final IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(options.fluid().getFluid());
		setSprite(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(options.fluid())));
		final int color = extensions.getTintColor(options.fluid());
		setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);

		this.uo = random.nextFloat() * 3.0f;
		this.vo = random.nextFloat() * 3.0f;
	}

	public static void register(RegisterParticleProvidersEvent event) {
		event.registerSpecial(OBParticleTypes.LIQUID_SPRAY.get(), (options, level, x, y, z, xd, yd, zd) -> new LiquidSprayParticle(level, options, x, y, z, xd, yd, zd));
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.TERRAIN_SHEET;
	}

	@Override
	protected float getU0() {
		return sprite.getU((uo + 1.0f) / 4.0f);
	}

	@Override
	protected float getU1() {
		return sprite.getU(uo / 4.0f);
	}

	@Override
	protected float getV0() {
		return sprite.getV(vo / 4.0f);
	}

	@Override
	protected float getV1() {
		return sprite.getV((vo + 1.0f) / 4.0f);
	}
}
