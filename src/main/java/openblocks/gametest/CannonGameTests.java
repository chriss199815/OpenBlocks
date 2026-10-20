package openblocks.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import openblocks.OpenBlocks;
import openblocks.block.entity.CannonBlockEntity;
import openblocks.common.CannonBallistics;
import openblocks.data.OBGameTestStructureProvider;
import openblocks.registry.OBBlocks;

@GameTestHolder(OpenBlocks.MODID)
@PrefixGameTestTemplate(false)
public class CannonGameTests {

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void trajectoryReachesTarget(GameTestHelper helper) {
		for (Vec3 delta : List.of(new Vec3(5, 0.5, 0), new Vec3(-20, 3, 12), new Vec3(40, -10, -30), new Vec3(0, 8, 2), new Vec3(60, 0, 0))) {
			final Vec3 velocity = CannonBallistics.velocityTo(delta);
			boolean hit = false;
			for (int ticks = 1; ticks <= 100 && !hit; ticks++)
				hit = CannonBallistics.positionAfter(velocity, ticks).distanceTo(delta) < 0.01;
			helper.assertTrue(hit, "Expected the shot to reach " + delta + " with " + velocity);
			helper.assertTrue(Math.abs(velocity.x) <= 3.9 && Math.abs(velocity.y) <= 3.9 && Math.abs(velocity.z) <= 3.9, "Too fast to sync: " + velocity);
		}
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_3X8X3)
	public static void aimByAnglesRoundTrips(GameTestHelper helper) {
		final BlockPos cannonPos = new BlockPos(1, 1, 1);
		helper.setBlock(cannonPos, OBBlocks.CANNON.get());
		final CannonBlockEntity cannon = (CannonBlockEntity)helper.getBlockEntity(cannonPos);

		cannon.setAim(0, 0, 2);
		final Vec3 south = cannon.getVelocity();
		helper.assertTrue(south.distanceTo(new Vec3(0, 0, 2)) < 1e-6, "Expected yaw 0 to shoot south, got " + south);

		cannon.setAim(-120, 35, 1.5);
		helper.assertTrue(Math.abs(cannon.getAimYaw() + 120) < 1e-6, "yaw " + cannon.getAimYaw());
		helper.assertTrue(Math.abs(cannon.getAimPitch() - 35) < 1e-6, "pitch " + cannon.getAimPitch());
		helper.assertTrue(Math.abs(cannon.getSpeed() - 1.5) < 1e-6, "speed " + cannon.getSpeed());
		helper.succeed();
	}

	@GameTest(template = OBGameTestStructureProvider.EMPTY_16X8X16, timeoutTicks = 200)
	public static void poweredCannonShootsItemsAtTarget(GameTestHelper helper) {
		final BlockPos cannonPos = new BlockPos(2, 2, 2);
		final BlockPos target = new BlockPos(12, 1, 9);
		helper.setBlock(cannonPos.below(), Blocks.STONE);
		helper.setBlock(target, Blocks.STONE);
		helper.setBlock(cannonPos, OBBlocks.CANNON.get());
		helper.setBlock(cannonPos.west(), Blocks.CHEST);
		((ChestBlockEntity)helper.getBlockEntity(cannonPos.west())).setItem(0, new ItemStack(Items.COBBLESTONE, 16));

		final CannonBlockEntity cannon = (CannonBlockEntity)helper.getBlockEntity(cannonPos);
		cannon.setTarget(helper.absolutePos(target));
		helper.setBlock(cannonPos.north(), Blocks.REDSTONE_BLOCK);

		// lands on top of the target block, or right next to it after bouncing
		final AABB landing = new AABB(helper.absolutePos(target.above())).inflate(1.5, 0.5, 1.5);
		helper.succeedWhen(() -> {
			final List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, landing, item -> item.onGround() && item.getItem().is(Items.COBBLESTONE));
			helper.assertFalse(items.isEmpty(), "Expected the cobblestone on the target");
			helper.assertValueEqual(items.getFirst().getItem().getCount(), 16, "shot stack size");
		});
	}
}
