package openblocks.common;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Touch buttons on the faces of a building guide; port of the legacy {@code hitboxes/guide_buttons.json}. Boxes are in
 * pixels of the unrotated guide, the {@link GuideOrientation} maps them to the world.
 */
public class GuideButtons {

	public enum Action {
		INCREMENT,
		DECREMENT,
		// copies the size of a half axis to the opposite one
		COPY,
		ROTATE_CW,
		ROTATE_CCW,
		NEXT_SHAPE,
		PREVIOUS_SHAPE
	}

	/**
	 * @param box in block space (0..1) of the unrotated guide
	 * @param halfAxis local half axis changed by {@link Action#INCREMENT}, {@link Action#DECREMENT} and {@link Action#COPY}
	 */
	public record Button(AABB box, Action action, @Nullable Direction halfAxis) {}

	public static final List<Button> BUTTONS = List.of(
			button(1, 15.9, 3, 5, 16.1, 14, "rotate_ccw"),
			button(11, 15.9, 3, 15, 16.1, 14, "rotate_cw"),
			button(11, -0.1, 2, 15, 0.1, 13, "rotate_ccw"),
			button(1, -0.1, 2, 5, 0.1, 13, "rotate_cw"),
			button(5, -0.1, 11, 11, 0.1, 14, "inc_mode"),
			button(5, -0.1, 5, 11, 0.1, 8, "dec_mode"),
			button(5, 15.9, 2, 11, 16.1, 5, "inc_mode"),
			button(5, 15.9, 8, 11, 16.1, 11, "dec_mode"),
			button(-0.1, 12, 9, 0.1, 15, 12, "inc_pos_y"),
			button(-0.1, 12, 4, 0.1, 15, 7, "dec_pos_y"),
			button(-0.1, 1, 9, 0.1, 4, 12, "dec_neg_y"),
			button(-0.1, 1, 4, 0.1, 4, 7, "inc_neg_y"),
			button(15.9, 12, 4, 16.1, 15, 7, "inc_pos_y"),
			button(15.9, 12, 9, 16.1, 15, 12, "dec_pos_y"),
			button(15.9, 1, 9, 16.1, 4, 12, "inc_neg_y"),
			button(15.9, 1, 4, 16.1, 4, 7, "dec_neg_y"),
			button(4, 12, -0.1, 7, 15, 0.1, "inc_pos_y"),
			button(9, 12, -0.1, 12, 15, 0.1, "dec_pos_y"),
			button(9, 1, -0.1, 12, 4, 0.1, "inc_neg_y"),
			button(4, 1, -0.1, 7, 4, 0.1, "dec_neg_y"),
			button(4, 1, 15.9, 7, 4, 16.1, "inc_neg_y"),
			button(9, 1, 15.9, 12, 4, 16.1, "dec_neg_y"),
			button(9, 12, 15.9, 12, 15, 16.1, "inc_pos_y"),
			button(4, 12, 15.9, 7, 15, 16.1, "dec_pos_y"),
			button(-0.1, 10, 6, 0.1, 12, 10, "copy_pos_y"),
			button(-0.1, 4, 6, 0.1, 6, 10, "copy_neg_y"),
			button(15.9, 10, 6, 16.1, 12, 10, "copy_pos_y"),
			button(15.9, 4, 6, 16.1, 6, 10, "copy_neg_y"),
			button(6, 10, -0.1, 10, 12, 0.1, "copy_pos_y"),
			button(6, 4, -0.1, 10, 6, 0.1, "copy_neg_y"),
			button(6, 10, 15.9, 10, 12, 16.1, "copy_pos_y"),
			button(6, 4, 15.9, 10, 6, 16.1, "copy_neg_y"),
			button(-0.1, 9, 12, 0.1, 12, 15, "dec_pos_z"),
			button(-0.1, 4, 12, 0.1, 7, 15, "inc_pos_z"),
			button(-0.1, 6, 10, 0.1, 10, 12, "copy_pos_z"),
			button(-0.1, 9, 1, 0.1, 12, 4, "inc_neg_z"),
			button(-0.1, 4, 1, 0.1, 7, 4, "dec_neg_z"),
			button(-0.1, 6, 4, 0.1, 10, 6, "copy_neg_z"),
			button(15.9, 9, 1, 16.1, 12, 4, "dec_neg_z"),
			button(15.9, 4, 1, 16.1, 7, 4, "inc_neg_z"),
			button(15.9, 6, 4, 16.1, 10, 6, "copy_neg_z"),
			button(15.9, 9, 12, 16.1, 12, 15, "inc_pos_z"),
			button(15.9, 4, 12, 16.1, 7, 15, "dec_pos_z"),
			button(15.9, 6, 10, 16.1, 10, 12, "copy_pos_z"),
			button(1, 9, -0.1, 4, 12, 0.1, "dec_neg_x"),
			button(1, 4, -0.1, 4, 7, 0.1, "inc_neg_x"),
			button(4, 6, -0.1, 6, 10, 0.1, "copy_neg_x"),
			button(12, 9, -0.1, 15, 12, 0.1, "inc_pos_x"),
			button(12, 4, -0.1, 15, 7, 0.1, "dec_pos_x"),
			button(10, 6, -0.1, 12, 10, 0.1, "copy_pos_x"),
			button(12, 9, 15.9, 15, 12, 16.1, "dec_pos_x"),
			button(12, 4, 15.9, 15, 7, 16.1, "inc_pos_x"),
			button(10, 6, 15.9, 12, 10, 16.1, "copy_pos_x"),
			button(1, 9, 15.9, 4, 12, 16.1, "inc_neg_x"),
			button(1, 4, 15.9, 4, 7, 16.1, "dec_neg_x"),
			button(4, 6, 15.9, 6, 10, 16.1, "copy_neg_x"));

	private GuideButtons() {}

	/**
	 * Button at a point of the unrotated guide, in block space (0..1).
	 */
	@Nullable
	public static Button find(Vec3 localHit) {
		for (Button button : BUTTONS)
			if (button.box().contains(localHit)) return button;
		return null;
	}

	// legacy names: inc_pos_x, dec_neg_y, copy_pos_z, rotate_cw, inc_mode, ...
	private static Button button(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String name) {
		final AABB box = new AABB(fromX / 16, fromY / 16, fromZ / 16, toX / 16, toY / 16, toZ / 16);
		return switch (name) {
			case "rotate_cw" -> new Button(box, Action.ROTATE_CW, null);
			case "rotate_ccw" -> new Button(box, Action.ROTATE_CCW, null);
			case "inc_mode" -> new Button(box, Action.NEXT_SHAPE, null);
			case "dec_mode" -> new Button(box, Action.PREVIOUS_SHAPE, null);
			default -> {
				final String[] parts = name.split("_", 2);
				final Action action = switch (parts[0]) {
					case "inc" -> Action.INCREMENT;
					case "dec" -> Action.DECREMENT;
					case "copy" -> Action.COPY;
					default -> throw new IllegalArgumentException(name);
				};
				yield new Button(box, action, halfAxis(parts[1]));
			}
		};
	}

	private static Direction halfAxis(String name) {
		final boolean positive = name.startsWith("pos_");
		final Direction.Axis axis = Direction.Axis.byName(name.substring(4).toLowerCase(Locale.ROOT));
		if (axis == null) throw new IllegalArgumentException(name);
		return Direction.fromAxisAndDirection(axis, positive? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
	}
}
