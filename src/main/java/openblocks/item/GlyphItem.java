package openblocks.item;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import openblocks.entity.Glyph;
import openblocks.registry.OBComponents;
import openblocks.registry.OBItems;

/**
 * Port of legacy {@code ItemGlyph}: a letter that hangs on walls, eight pixels big and placed on a one pixel grid
 * centered on the clicked point. The character is a data component instead of the item damage.
 */
public class GlyphItem extends Item {

	/**
	 * The characters of the legacy font sheet, without the empty cells; the same set as legacy.
	 */
	public static final String CHARS = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
			+ "ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■";

	/**
	 * Characters in the creative tab; all of {@link #CHARS} are in the search tab if enabled in the config.
	 */
	public static final String DISPLAY_CHARS = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	public static final char DEFAULT_CHAR = '?';

	public GlyphItem(Properties properties) {
		super(properties.component(OBComponents.GLYPH, (int)DEFAULT_CHAR));
	}

	public static boolean isValid(int ch) {
		return CHARS.indexOf(ch) >= 0;
	}

	public static ItemStack create(int ch, int count) {
		final ItemStack stack = new ItemStack(OBItems.GLYPH.get(), count);
		stack.set(OBComponents.GLYPH, isValid(ch)? ch : DEFAULT_CHAR);
		return stack;
	}

	public static char getChar(ItemStack stack) {
		final int ch = stack.getOrDefault(OBComponents.GLYPH, (int)DEFAULT_CHAR);
		return isValid(ch)? (char)ch : DEFAULT_CHAR;
	}

	/**
	 * The next or previous character of {@link #CHARS}, wrapping around.
	 */
	public static char cycle(char ch, int delta) {
		return CHARS.charAt(Math.floorMod(CHARS.indexOf(ch) + delta, CHARS.length()));
	}

	@Override
	public Component getName(ItemStack stack) {
		final char ch = getChar(stack);
		return ch == DEFAULT_CHAR? super.getName(stack) : Component.translatable("item.openblocks.glyph.with_char", String.valueOf(ch));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		final char ch = getChar(stack);
		final String name = Character.getName(ch);
		tooltip.add(Component.literal(String.format(Locale.ROOT, "%04X (%s)", (int)ch, name != null? name : "?")).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Direction side = context.getClickedFace();
		final BlockPos pos = context.getClickedPos().relative(side);
		final Player player = context.getPlayer();
		final ItemStack stack = context.getItemInHand();
		if (side.getAxis().isVertical() || (player != null && !player.mayUseItemAt(pos, side, stack))) return InteractionResult.FAIL;

		// the clicked point on the face in pixels, from the bottom left corner as seen from the front
		final Vec3 hit = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
		final Direction right = side.getCounterClockWise();
		final double along = right.getAxis().choose(hit.x, hit.y, hit.z);
		final double u = right.getAxisDirection() == Direction.AxisDirection.POSITIVE? along : 1 - along;
		final Glyph glyph = new Glyph(context.getLevel(), pos, side, getChar(stack), pixel(u), pixel(hit.y));
		if (!glyph.survives()) return InteractionResult.FAIL;

		final Level level = context.getLevel();
		if (!level.isClientSide) {
			glyph.playPlacementSound();
			level.gameEvent(player, GameEvent.ENTITY_PLACE, glyph.position());
			level.addFreshEntity(glyph);
		}
		stack.shrink(1);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static int pixel(double coordinate) {
		return Math.min(15, Math.max(0, (int)Math.floor(coordinate * 16)));
	}
}
