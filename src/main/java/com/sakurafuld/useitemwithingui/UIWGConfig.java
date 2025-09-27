package com.sakurafuld.useitemwithingui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

@OnlyIn(Dist.CLIENT)
public class UIWGConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_HINT;
    public static final ForgeConfigSpec.BooleanValue PRESS_OUTSIDE_TO_USE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("UseItemWithinGui");

        SHOW_HINT = builder
                .comment("Show operation hints on the Gui")
                .define("showHint", true);
        PRESS_OUTSIDE_TO_USE = builder
                .comment("Press Ctrl+RMB outside the Gui to use the item in your hand")
                .define("pressOutsideToUse", true);

        builder.pop();

        SPEC = builder.build();
    }
}
