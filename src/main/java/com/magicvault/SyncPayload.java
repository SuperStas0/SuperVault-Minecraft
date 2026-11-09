package com.magicvault;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: current vault state for the open menu. */
public record SyncPayload(int containerId, StorageSnapshot snapshot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPayload> TYPE = new CustomPacketPayload.Type<>(MagicVault.id("sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SyncPayload::containerId,
        StorageSnapshot.STREAM_CODEC, SyncPayload::snapshot,
        SyncPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
