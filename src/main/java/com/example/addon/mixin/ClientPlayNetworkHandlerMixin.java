package com.example.addon.mixin;

import com.example.addon.modules.SnowFetch;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler; // This import now works!
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin { // Match the filename here
    @Inject(method = "onCommandSuggestions", at = @At("TAIL"))
    private void onOnCommandSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        SnowFetch snowFetch = Modules.get().get(SnowFetch.class);
        if (snowFetch != null && snowFetch.isActive()) {
            snowFetch.onReceiveSuggestions(packet);
        }
    }
}