package com.example.addon.mixin;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatUtils.class)
public interface ChatUtilsAccessor {


    @Accessor(value = "PREFIX")
    static void setPrefix(Text text) {

    }


}
