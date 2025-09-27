package com.sakurafuld.useitemwithingui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2ic;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

import java.util.OptionalInt;

@Mod(value = UseItemWithinGui.ID, dist = Dist.CLIENT)
public class UseItemWithinGui {
    public static final String ID = "useitemwithingui";
    public static final Logger LOG = LogUtils.getLogger();

    public UseItemWithinGui(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::useItemWithinGui);
        NeoForge.EVENT_BUS.addListener(this::initUIWGHint);
        NeoForge.EVENT_BUS.addListener(this::closingUIWGHint);
        NeoForge.EVENT_BUS.addListener(this::checkUIWGHint);
        NeoForge.EVENT_BUS.addListener(this::renderUIWGHint);

        modContainer.registerConfig(ModConfig.Type.CLIENT, UIWGConfig.SPEC);
    }

    @OnlyIn(Dist.CLIENT)
    private void useItemWithinGui(ScreenEvent.MouseButtonPressed.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && Screen.hasControlDown() && player != null && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            if (screen instanceof CreativeModeInventoryScreen creative) {
                if (this.handleCreative(player, creative)) event.setCanceled(true);
            } else {
                Slot hovered = screen.getSlotUnderMouse();
                if (hovered == null) {
                    if (UIWGConfig.PRESS_OUTSIDE_TO_USE.get()) {
                        this.useItem(player);
                        LOG.debug("UseItemWithinGui outside");
                        event.setCanceled(true);
                    } else {
                        LOG.debug("UIWG outside cancel");
                    }

                    return;
                }
                if (!hovered.hasItem() || !hovered.mayPickup(player)) {
                    LOG.debug("UIWG Hovered may not be picked up");
                    return;
                }

                AbstractContainerMenu menu = screen.getMenu();
                Inventory inventory = player.getInventory();

                OptionalInt found = menu.findSlot(inventory, inventory.selected);
                if (found.isEmpty()) {
                    LOG.debug("UIWG Selected is not found");
                    return;
                }

                int selectedIndex = found.getAsInt();
                if (selectedIndex == hovered.index) {
                    this.useItem(player);
                } else {
                    Slot selected = menu.getSlot(selectedIndex);
                    if (!selected.mayPickup(player)) {
                        LOG.debug("UIWG Selected may not be picked up");
                        return;
                    }

                    if (ItemStack.isSameItemSameComponents(hovered.getItem(), selected.getItem())) {
                        this.useItem(player);
                        LOG.debug("UseItemWithinGuiSame");
                        event.setCanceled(true);
                        return;
                    }

                    this.pickup(player, menu, hovered.index);
                    this.pickup(player, menu, selectedIndex);
                    this.useItem(player);
                    if (!Screen.hasAltDown()) {
                        LOG.debug("UIWG Non alt pickup");
                        this.pickup(player, menu, selectedIndex);
                    }
                    if (!menu.getCarried().isEmpty()) {
                        this.pickup(player, menu, hovered.index);
                    }
                }

                LOG.debug("UseItemWithinGui");
                event.setCanceled(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void pickup(LocalPlayer player, AbstractContainerMenu menu, int slot) {
        if (slot >= 0 && slot < menu.slots.size() || slot == -999) {
            Minecraft.getInstance().gameMode.handleInventoryMouseClick(menu.containerId, slot, InputConstants.MOUSE_BUTTON_LEFT, ClickType.PICKUP, player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private boolean handleCreative(LocalPlayer player, CreativeModeInventoryScreen screen) {
        Slot hovered = screen.getSlotUnderMouse();

        if (hovered == null) {
            if (UIWGConfig.PRESS_OUTSIDE_TO_USE.get()) {
                this.useItem(player);
                LOG.debug("Creative UseItemWithinGui outside");
                return true;
            } else {
                LOG.debug("Creative UIWG outside cancel");
                return false;
            }
        }

        if (!hovered.hasItem() || !hovered.mayPickup(player)) {
            LOG.debug("Creative UIWG Hovered may not be picked up");
            return false;
        }

        CreativeModeInventoryScreen.ItemPickerMenu menu = screen.getMenu();
        Inventory inventory = player.getInventory();
        int selected;
        boolean infinite;
        if (screen.isInventoryOpen()) {
            selected = 36 + inventory.selected;
            infinite = false;
        } else {
            selected = 45 + inventory.selected;
            infinite = hovered.index < 45;
        }

        if (hovered == menu.getSlot(selected)) {
            this.useItem(player);
            LOG.debug("Creative UseItemWithinGuiDirect");
            return true;
        }

        if (!menu.getSlot(selected).mayPickup(player)) {
            LOG.debug("Creative UIWG Selected may not be picked up");
            return false;
        }

        boolean alt = Screen.hasAltDown();
        ItemStack last = player.getMainHandItem().copy();
        if (infinite && alt) {
            player.drop(true);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, hovered.getItem().copy());
        player.inventoryMenu.broadcastChanges();
        ItemStack before = player.getMainHandItem().copy();
        this.useItem(player);
        if (!alt) {
            ItemStack after = player.getMainHandItem().copy();
            boolean different = !ItemStack.isSameItemSameComponents(before, after);
            if (different) {
                if (infinite) {
                    if (!last.isEmpty()) {
                        player.drop(true);
                    }
                } else {
                    hovered.set(after);
                }
            }

            if (!(infinite && last.isEmpty() && different)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, last);
                Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(last.copy(), 36 + inventory.selected);
            }
        } else if (!infinite) {
            hovered.set(last);
        }
        player.inventoryMenu.broadcastChanges();
        LOG.debug("Creative UseItemWithinGui");
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    private void useItem(LocalPlayer player) {
        boolean shift = Screen.hasShiftDown();
        if (shift) {
            player.input.shiftKeyDown = true;
            player.setShiftKeyDown(true);
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        }
        Minecraft.getInstance().startUseItem();
        if (shift) {
            player.input.shiftKeyDown = false;
            player.setShiftKeyDown(false);
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        }
    }

    private static final Component FIRST_HINT = Component.translatable("hint.useitemwithingui.first").withStyle(ChatFormatting.YELLOW);
    private static final Component SHIFT_HINT = Component.translatable("hint.useitemwithingui.shift").withStyle(ChatFormatting.GOLD);
    private static final Component ALT_HINT = Component.translatable("hint.useitemwithingui.alt").withStyle(ChatFormatting.GOLD);
    private long opened = -1;
    private long tipped = -1;
    private int tipHeight = 0;

    @OnlyIn(Dist.CLIENT)
    private void initUIWGHint(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            this.opened = Util.getMillis();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void closingUIWGHint(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            this.opened = -1;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void checkUIWGHint(RenderTooltipEvent.Pre event) {
        if (!(this.opened > 0 || Screen.hasControlDown())) {
            return;
        }
        this.tipped = Util.getMillis();
        this.tipHeight = event.getComponents().size() == 1 ? -2 : 0;
        for(ClientTooltipComponent component : event.getComponents()) {
            this.tipHeight += component.getHeight();
        }
    }


    @OnlyIn(Dist.CLIENT)
    private void renderUIWGHint(ScreenEvent.Render.Post event) {
        if (!UIWGConfig.SHOW_HINT.get()) {
            return;
        }

        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Font font = mc.font;
        GuiGraphics graphics = event.getGuiGraphics();
        boolean tipping = Util.getMillis() - this.tipped <= 20;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        if (Screen.hasControlDown()) {
            this.opened -= 10;

            int shiftWidth = font.width(SHIFT_HINT);
            int altWidth = font.width(ALT_HINT);
            int shiftYTweak = (tipping ? font.lineHeight + 4 + font.lineHeight + 2 : -4 + font.lineHeight + 2);
            int height = (tipping ? this.tipHeight : font.lineHeight);
            Vector2ic shiftPos = DefaultTooltipPositioner.INSTANCE.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), event.getMouseX(), event.getMouseY(), shiftWidth, height);
            int altYTweak = (tipping ? font.lineHeight + 4 : -4);
            Vector2ic altPos = DefaultTooltipPositioner.INSTANCE.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), event.getMouseX(), event.getMouseY(), altWidth, height);
            int x = altWidth < shiftWidth ? shiftPos.x() : altPos.x();

            graphics.drawString(font, SHIFT_HINT, x, shiftPos.y() - shiftYTweak, 0xFFFFFF);
            graphics.drawString(font, ALT_HINT, x, altPos.y() - altYTweak, 0xFFFFFF);
        } else if (this.opened > 0) {
            long difference = Util.getMillis() - this.opened;
            if (difference <= 2500) {
                int fade;
                if (difference <= 1500) {
                    fade = 0xFF;
                } else {
                    difference -= 1500;
                    fade = (int) (0xFF * (1f - (difference / 1000f)));
                    fade = Math.max(4, fade);
                }

                int height = (tipping ? this.tipHeight : font.lineHeight);
                Vector2ic pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), event.getMouseX(), event.getMouseY(), font.width(FIRST_HINT), height);
                graphics.drawString(font, FIRST_HINT, pos.x(), pos.y() - (tipping ? font.lineHeight + 4 : -4), (fade << 24) | 0xFFFFFF);
            }
        }
        graphics.pose().popPose();
    }
}
