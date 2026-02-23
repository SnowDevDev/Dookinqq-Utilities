package com.example.addon.mixin;

import com.example.addon.modules.SnowNameTags;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("HEAD")
    )
    private void dookinqq$hideVanillaNametags(
        Entity entity,
        Text text,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        if (!(entity instanceof PlayerEntity)) return;
        if (text == null) return;

        SnowNameTags mod = Modules.get().get(SnowNameTags.class);
        if (mod == null || !mod.isActive() || !mod.hideVanilla.get()) return;

        // Replace label text instead of cancelling render
        ((EntityRendererAccessor) this).dookinqq$setLabel(Text.empty());
    }
}
