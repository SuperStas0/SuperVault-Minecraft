package com.magicvault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Where a Vault Link points: the dimension and position of a vault block. */
public record VaultLinkData(ResourceKey<Level> dimension, BlockPos pos) {
    public static final Codec<VaultLinkData> CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(VaultLinkData::dimension),
        BlockPos.CODEC.fieldOf("pos").forGetter(VaultLinkData::pos)
    ).apply(i, VaultLinkData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, VaultLinkData> STREAM_CODEC = StreamCodec.composite(
        ResourceKey.streamCodec(Registries.DIMENSION), VaultLinkData::dimension,
        BlockPos.STREAM_CODEC, VaultLinkData::pos,
        VaultLinkData::new
    );

    public String dimensionName() {
        if (dimension.equals(Level.OVERWORLD)) {
            return "Overworld";
        }

        if (dimension.equals(Level.NETHER)) {
            return "The Nether";
        }

        if (dimension.equals(Level.END)) {
            return "The End";
        }

        return "another dimension";
    }
}
