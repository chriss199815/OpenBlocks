package openblocks.common;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import openblocks.OpenBlocks;
import openblocks.api.FlimFlamRegistry;
import openblocks.api.InventoryEvent.SubInventory;
import openblocks.common.PlayerInventoryStore.LoadedInventories;
import openblocks.common.flimflam.FlimFlamHandler;
import openblocks.common.flimflam.LuckState;

/**
 * Port of legacy {@code CommandFlimFlam}, {@code CommandLuck} and {@code CommandInventory} as Brigadier commands; like legacy,
 * for operators of level 4 only.
 */
public class OBCommands {

	private static final SimpleCommandExceptionType NO_FLIM_FLAM = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.command.no_flim_flam"));
	private static final SimpleCommandExceptionType FLIM_FLAM_FAILED = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.command.flim_flam_failed"));
	private static final DynamicCommandExceptionType CANT_RESTORE_PLAYER = new DynamicCommandExceptionType(player -> Component.translatable("openblocks.misc.cant_restore_player", player));
	private static final DynamicCommandExceptionType CANT_STORE = new DynamicCommandExceptionType(player -> Component.translatable("openblocks.misc.cant_store", player));
	private static final SimpleCommandExceptionType CANT_RESTORE_INVENTORY = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.cant_restore_inventory"));
	private static final DynamicCommandExceptionType INVALID_SUB_INVENTORY = new DynamicCommandExceptionType(id -> Component.translatable("openblocks.misc.invalid_sub_inventory", id));
	private static final SimpleCommandExceptionType INVALID_INDEX = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.invalid_index"));
	private static final SimpleCommandExceptionType EMPTY_SLOT = new SimpleCommandExceptionType(Component.translatable("openblocks.misc.empty_slot"));

	private static final String ID_MAIN_INVENTORY = "main";

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

		dispatcher.register(Commands.literal("ob_inventory")
				.requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.literal("store")
						.then(Commands.argument("player", EntityArgument.player())
								.executes(OBCommands::storeInventory)))
				.then(Commands.literal("restore")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests(OBCommands::suggestDumps)
										.executes(OBCommands::restoreInventory))))
				.then(Commands.literal("spawn")
						.then(Commands.argument("id", StringArgumentType.word())
								.suggests(OBCommands::suggestDumps)
								.executes(context -> spawnInventory(context, ID_MAIN_INVENTORY, null))
								.then(Commands.argument("inventory", StringArgumentType.word())
										.suggests(OBCommands::suggestSubInventories)
										.executes(context -> spawnInventory(context, StringArgumentType.getString(context, "inventory"), null))
										.then(Commands.argument("index", IntegerArgumentType.integer(0))
												.executes(context -> spawnInventory(context, StringArgumentType.getString(context, "inventory"), IntegerArgumentType.getInteger(context, "index"))))))));
	}

	private static CompletableFuture<Suggestions> suggestDumps(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(PlayerInventoryStore.getMatchedDumps(context.getSource().getServer(), builder.getRemaining()), builder);
	}

	private static CompletableFuture<Suggestions> suggestSubInventories(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		final List<String> result = new ArrayList<>();
		result.add(ID_MAIN_INVENTORY);
		try {
			final LoadedInventories inventories = PlayerInventoryStore.loadInventories(context.getSource().getServer(), StringArgumentType.getString(context, "id"));
			if (inventories != null) result.addAll(inventories.subInventories().keySet());
		} catch (Exception e) {
			// just ignore, don't spam
		}
		return SharedSuggestionProvider.suggest(result, builder);
	}

	private static int storeInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final ServerPlayer player = EntityArgument.getPlayer(context, "player");
		final Path result;
		try {
			result = PlayerInventoryStore.storePlayerInventory(player, "command");
		} catch (Exception e) {
			OpenBlocks.LOG.warn("Failed to store inventory, player {}", player.getGameProfile().getName(), e);
			throw CANT_STORE.create(player.getDisplayName());
		}
		context.getSource().sendSuccess(() -> Component.translatable("openblocks.misc.stored_inventory", result.toAbsolutePath().toString()), true);
		return 1;
	}

	private static int restoreInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final ServerPlayer player = EntityArgument.getPlayer(context, "player");
		final String id = StringArgumentType.getString(context, "id");
		final boolean success;
		try {
			success = PlayerInventoryStore.restoreInventory(player, id);
		} catch (Exception e) {
			OpenBlocks.LOG.warn("Failed to restore inventory, player {}, file {}", player.getGameProfile().getName(), id, e);
			throw CANT_RESTORE_PLAYER.create(player.getDisplayName());
		}

		if (!success) throw CANT_RESTORE_PLAYER.create(player.getDisplayName());
		context.getSource().sendSuccess(() -> Component.translatable("openblocks.misc.restored_inventory", player.getDisplayName()), true);
		return 1;
	}

	private static int spawnInventory(CommandContext<CommandSourceStack> context, String target, Integer index) throws CommandSyntaxException {
		final String id = StringArgumentType.getString(context, "id");
		final LoadedInventories inventories;
		try {
			inventories = PlayerInventoryStore.loadInventories(context.getSource().getServer(), id);
		} catch (Exception e) {
			OpenBlocks.LOG.warn("Failed to restore inventory, file {}", id, e);
			throw CANT_RESTORE_INVENTORY.create();
		}
		if (inventories == null) throw CANT_RESTORE_INVENTORY.create();

		final List<ItemStack> toRestore;
		if (ID_MAIN_INVENTORY.equals(target)) {
			final List<ItemStack> inventory = inventories.mainInventory();
			if (inventory == null) throw CANT_RESTORE_INVENTORY.create();

			if (index != null) {
				if (index >= inventory.size()) throw INVALID_INDEX.create();
				toRestore = List.of(nonEmpty(inventory.get(index)));
			} else {
				toRestore = inventory;
			}
		} else {
			final SubInventory inventory = inventories.subInventories().get(target);
			if (inventory == null) throw INVALID_SUB_INVENTORY.create(target);

			if (index != null) toRestore = List.of(nonEmpty(inventory.getItemStack(index)));
			else toRestore = List.copyOf(inventory.asMap().values());
		}

		final ServerLevel level = context.getSource().getLevel();
		final Vec3 pos = context.getSource().getPosition();
		int count = 0;
		for (ItemStack stack : toRestore) {
			if (stack.isEmpty()) continue;
			final ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
			drop.setDefaultPickUpDelay();
			level.addFreshEntity(drop);
			count++;
		}
		return count;
	}

	private static ItemStack nonEmpty(ItemStack stack) throws CommandSyntaxException {
		if (stack.isEmpty()) throw EMPTY_SLOT.create();
		return stack;
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
