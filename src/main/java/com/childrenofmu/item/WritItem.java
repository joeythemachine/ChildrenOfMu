package com.childrenofmu.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/**
 * A Writ of Trade, sealed by the Great Gar of an RCB City: with one in your pocket, the city's folk will trade with you
 * and its bookies will take your bets, until it runs out. Which city and until when are kept on the stack.
 */
public class WritItem extends Item {
    public WritItem(Properties properties) {
        super(properties);
    }

    /** The city (its structure's chunk, packed), its name, and the game time the writ runs out. */
    public static ItemStack make(Item item, long city, String cityName, long expires, int epoch) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putLong("City", city);
        tag.putString("CityName", cityName);
        tag.putLong("Expires", expires);
        tag.putInt("Epoch", epoch);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        if (tag.contains("CityName")) {
            tooltip.add(Component.translatable("item.childrenofmu.writ_of_trade.city", tag.getString("CityName"))
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.childrenofmu.writ_of_trade.until", tag.getLong("Expires") / 24000L + 1)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return data(stack).contains("City");
    }
}
