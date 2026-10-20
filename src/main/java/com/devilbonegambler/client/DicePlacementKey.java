/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client;

import com.devilbonegambler.network.ModNetwork;
import com.devilbonegambler.network.packet.DiceModifierPacket;
import com.devilbonegambler.network.packet.PlaceDicePacket;
import com.devilbonegambler.registry.ModItems;
import com.devilbonegambler.util.ModConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * The placement modifier key and its mouse hook.
 *
 * While the key is held, a right click with a die never reaches the vanilla use path: the click is cancelled and a
 * placement request goes to the server. The key state is also mirrored to the server, which needs it when a placed die
 * is broken with an empty hand.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DicePlacementKey
{
    public static final KeyMapping KEY = new KeyMapping("key." + ModConstants.MOD_ID + ".place_dice",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, "key.categories." + ModConstants.MOD_ID);
    private static boolean lastHeld;

    private DicePlacementKey()
    {
    }

    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }

        if (Minecraft.getInstance().player == null)
        {
            lastHeld = false;
            return;
        }

        boolean held = KEY.isDown();

        if (held != lastHeld)
        {
            lastHeld = held;
            ModNetwork.sendToServer(new DiceModifierPacket(held));
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || event.getAction() != GLFW.GLFW_PRESS)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.screen != null || !KEY.isDown())
        {
            return;
        }

        boolean mainDice = isDice(minecraft.player.getItemInHand(InteractionHand.MAIN_HAND));
        boolean offDice = isDice(minecraft.player.getItemInHand(InteractionHand.OFF_HAND));

        if (!mainDice && !offDice)
        {
            return;
        }

        if (minecraft.hitResult instanceof BlockHitResult hit)
        {
            ModNetwork.sendToServer(new PlaceDicePacket(hit.getBlockPos(), hit.getDirection(), !mainDice));
        }

        /* Holding the modifier must never fall through to the ordinary dice use. */
        event.setCanceled(true);
    }

    private static boolean isDice(ItemStack stack)
    {
        return stack.is(ModItems.GAMBLERS_DICE.get()) || stack.is(ModItems.DEVILS_DICE.get());
    }
}
