package com.childrenofmu.client;

import com.childrenofmu.entity.folk.FolkEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * A Dog's vocation, on a line of its own beneath their name in the trading window ("Hillfoot Rex" over "Weaver"), so
 * the title doesn't run long. The server tucks it into the title's style (an insertion, never drawn).
 */
public final class VocationLine {
    private VocationLine() {}

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof MerchantScreen screen)) {
            return;
        }
        String tag = screen.getTitle().getStyle().getInsertion();
        if (tag == null || !tag.startsWith(FolkEntity.VOCATION_TAG)) {
            return;
        }
        String vocation = tag.substring(FolkEntity.VOCATION_TAG.length());
        Font font = Minecraft.getInstance().font;
        int w = font.width(vocation);
        // Centred under the title (which sits over the right-hand part of the window), clear of the level bar.
        int x = screen.getGuiLeft() + 49 + screen.getXSize() / 2 - w / 2;
        int y = screen.getGuiTop() + 24;
        event.getGuiGraphics().drawString(font, vocation, x, y, 0x6B4A2B, false);
    }
}
