package com.magicvault;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Opens a linked, fully upgraded vault from anywhere, in any dimension. */
public class VaultLinkItem extends Item {
	public VaultLinkItem(Properties properties) {
		super(properties);
	}

	/** Called by the vault block when this item is used on a fully upgraded vault. */
	public static void bind(ItemStack stack, Level level, BlockPos pos) {
		VaultLinkData data = new VaultLinkData(level.dimension(), pos.immutable());
		String text = "Linked to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + data.dimensionName() + ")";
		stack.set(MagicVault.LINK_DATA, data);
		stack.set(DataComponents.LORE, new ItemLore(List.<Component>of(Component.literal(text).withStyle(ChatFormatting.GRAY))));
	}

	/** Keeps the chunk of a remotely opened vault loaded. The ticket expires on its own once refreshing stops. */
	public static void keepLoaded(ServerLevel level, BlockPos pos) {
		level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), 0);
	}

	private static void say(Player player, String text) {
		player.displayClientMessage(Component.literal(text), true);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		VaultLinkData link = player.getItemInHand(hand).get(MagicVault.LINK_DATA);

		if (link == null) {
			say(player, "Not linked. Use it on a fully upgraded vault.");
			return InteractionResult.SUCCESS;
		}

		ServerLevel target = serverLevel.getServer().getLevel(link.dimension());

		if (target == null) {
			say(player, "The linked dimension is unavailable.");
			return InteractionResult.SUCCESS;
		}

		// ticket first, so the chunk stays loaded after the lookup below loads it
		keepLoaded(target, link.pos());

		if (!(target.getBlockEntity(link.pos()) instanceof VaultBlockEntity vault)) {
			say(player, "The linked vault no longer exists.");
			return InteractionResult.SUCCESS;
		}

		if (!vault.storage().isMaxTier()) {
			say(player, "The linked vault is not fully upgraded.");
			return InteractionResult.SUCCESS;
		}

		serverPlayer.openMenu(vault.remoteFactory());
		return InteractionResult.SUCCESS;
	}
}
