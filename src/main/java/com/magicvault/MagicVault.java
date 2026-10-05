package com.magicvault;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;

public class MagicVault implements ModInitializer {
	public static final String MOD_ID = "magicvault";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static final ResourceKey<Block> VAULT_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("vault"));
	// Very high blast resistance so explosions can't destroy a vault (and silently delete its contents).
	public static final Block VAULT_BLOCK = new VaultBlock(BlockBehaviour.Properties.of()
		.setId(VAULT_BLOCK_KEY)
		.mapColor(MapColor.COLOR_PURPLE)
		.strength(3.0f, 3600000.0f)
		.sound(SoundType.METAL));

	public static final ResourceKey<Item> VAULT_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("vault"));
	public static final Item VAULT_ITEM = new BlockItem(VAULT_BLOCK, new Item.Properties().setId(VAULT_ITEM_KEY));

	public static final BlockEntityType<VaultBlockEntity> VAULT_BE =
		FabricBlockEntityTypeBuilder.create(VaultBlockEntity::new, VAULT_BLOCK).build();

	public static final MenuType<VaultMenu> VAULT_MENU =
		new ExtendedScreenHandlerType<>(VaultMenu::new, BlockPos.STREAM_CODEC.cast());

	public static final DataComponentType<StorageSnapshot> STORAGE_DATA = DataComponentType.<StorageSnapshot>builder()
		.persistent(StorageSnapshot.CODEC)
		.networkSynchronized(StorageSnapshot.STREAM_CODEC)
		.build();

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.BLOCK, VAULT_BLOCK_KEY, VAULT_BLOCK);
		Registry.register(BuiltInRegistries.ITEM, VAULT_ITEM_KEY, VAULT_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("vault"), VAULT_BE);
		Registry.register(BuiltInRegistries.MENU, id("vault"), VAULT_MENU);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("storage"), STORAGE_DATA);

		PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (player.containerMenu instanceof VaultMenu menu && menu.containerId == payload.containerId()) {
				menu.handleAction(payload);
			}
		});

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(VAULT_ITEM));
	}
}
