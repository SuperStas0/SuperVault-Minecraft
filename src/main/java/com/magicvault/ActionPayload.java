package com.magicvault;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server: a click on a vault cell (index = cell index, flags bit0 = right button, bit1 = shift,
 * proto = expected item for validation) or an upgrade request (index = upgrade flag).
 */
public record ActionPayload(int containerId, int action, int index, int flags, ItemStack proto) implements CustomPacketPayload {
	public static final int CELL = 0;
	public static final int UPGRADE = 1;

	public static final CustomPacketPayload.Type<ActionPayload> TYPE = new CustomPacketPayload.Type<>(MagicVault.id("action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ActionPayload::containerId,
		ByteBufCodecs.VAR_INT, ActionPayload::action,
		ByteBufCodecs.VAR_INT, ActionPayload::index,
		ByteBufCodecs.VAR_INT, ActionPayload::flags,
		ItemStack.OPTIONAL_STREAM_CODEC, ActionPayload::proto,
		ActionPayload::new
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
