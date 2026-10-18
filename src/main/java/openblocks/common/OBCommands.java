package openblocks.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import openblocks.api.FlimFlamRegistry;
import openblocks.common.flimflam.FlimFlamHandler;
import openblocks.common.flimflam.LuckState;

/**
 * Port of legacy {@code CommandFlimFlam} and {@code CommandLuck} as Brigadier commands; like legacy, for operators of
 * level 4 only.
 */
public class OBCommands {

	private static final SimpleCommandExceptionType NO_FLIM_FLAM = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.command.no_flim_flam"));
	private static final SimpleCommandExceptionType FLIM_FLAM_FAILED = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.command.flim_flam_failed"));

	public static void register(RegisterCommandsEvent event) {
		final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

		dispatcher.register(Commands.literal("flimflam")
				.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> flimFlam(context, null))
						.then(Commands.argument("effect", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(FlimFlamRegistry.getNames(), builder))
								.executes(context -> flimFlam(context, StringArgumentType.getString(context, "effect"))))));

		dispatcher.register(Commands.literal("luck")
				.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(OBCommands::showLuck)
						.then(Commands.argument("amount", IntegerArgumentType.integer())
								.executes(OBCommands::addLuck))));
	}

	private static int flimFlam(CommandContext<CommandSourceStack> context, String effectName) throws CommandSyntaxException {
		final ServerPlayer player = EntityArgument.getPlayer(context, "player");
		final FlimFlamRegistry.Entry entry;
		if (effectName == null) {
			final var flimFlams = FlimFlamRegistry.getFlimFlams();
			entry = flimFlams.get(player.getRandom().nextInt(flimFlams.size()));
		} else {
			entry = FlimFlamRegistry.get(effectName);
			if (entry == null) throw NO_FLIM_FLAM.create();
		}

		if (!FlimFlamHandler.execute(player, entry)) throw FLIM_FLAM_FAILED.create();

		final CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.translatable("openblocks.misc.command.flim_flam_source", player.getDisplayName(), entry.name()), true);
		if (source.getEntity() != player) player.sendSystemMessage(Component.translatable("openblocks.misc.command.flim_flam_target"));
		return 1;
	}

	private static int showLuck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final ServerPlayer player = EntityArgument.getPlayer(context, "player");
		final int luck = LuckState.of(player).getLuck();
		context.getSource().sendSuccess(() -> Component.translatable("openblocks.misc.command.luck_current", player.getDisplayName(), luck), false);
		return luck;
	}

	private static int addLuck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final ServerPlayer player = EntityArgument.getPlayer(context, "player");
		final int luck = LuckState.of(player).modifyLuck(IntegerArgumentType.getInteger(context, "amount"));
		context.getSource().sendSuccess(() -> Component.translatable("openblocks.misc.command.luck_added", player.getDisplayName(), luck), true);
		return luck;
	}
}
