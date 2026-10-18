package openblocks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import openblocks.OpenBlocks;
import openblocks.block.entity.DrawingTableBlockEntity;
import openblocks.menu.DrawingTableMenu;

/**
 * Client to server: print the text as glyphs on the drawing table the player has open. Replaces the legacy RPC
 * {@code IStencilCrafter.printGlyphs}.
 */
public record DrawingTablePrintPayload(String text) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DrawingTablePrintPayload> TYPE = new CustomPacketPayload.Type<>(OpenBlocks.location("drawing_table_print"));

	public static final StreamCodec<ByteBuf, DrawingTablePrintPayload> STREAM_CODEC = ByteBufCodecs.stringUtf8(DrawingTableBlockEntity.MAX_PRINT_SIZE)
			.map(DrawingTablePrintPayload::new, DrawingTablePrintPayload::text);

	@Override
	public CustomPacketPayload.Type<DrawingTablePrintPayload> type() {
		return TYPE;
	}

	public static void handle(DrawingTablePrintPayload payload, IPayloadContext context) {
		if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof DrawingTableMenu menu && menu.stillValid(player))
			menu.print(payload.text());
	}
}
