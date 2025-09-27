package com.sakurafuld.useitemwithingui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.ModConfigSpec;

@OnlyIn(Dist.CLIENT)
public class UIWGConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_HINT;
    public static final ModConfigSpec.BooleanValue PRESS_OUTSIDE_TO_USE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("UseItemWithinGui");

        SHOW_HINT = builder
                .comment("Show operation hints on the Gui")
                .define("showHint", true);
        PRESS_OUTSIDE_TO_USE = builder
                .comment("Press Ctrl+RMB outside slots to use the item in your hand")
                .define("pressOutsideToUse", true);

        builder.pop();

        SPEC = builder.build();
    }
}
