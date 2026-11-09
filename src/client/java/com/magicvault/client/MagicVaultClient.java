package com.magicvault.client;

import com.magicvault.MagicVault;
import com.magicvault.SyncPayload;
import com.magicvault.VaultMenu;

import net.minecraft.client.gui.screens.MenuScreens;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MagicVaultClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(MagicVault.VAULT_MENU, VaultScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            if (context.player() != null
                && context.player().containerMenu instanceof VaultMenu menu
                && menu.containerId == payload.containerId()) {
                menu.view = payload.snapshot();
            }
        });
    }
}
