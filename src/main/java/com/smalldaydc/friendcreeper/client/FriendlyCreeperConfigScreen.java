package com.smalldaydc.friendcreeper.client;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperConfigScreen {

    public static Screen create(Screen parent) {
        FriendlyCreeperConfig config = FriendlyCreeperConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.friendcreeper.title"))
                .setSavingRunnable(FriendlyCreeperConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.general"));
        ConfigCategory client = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.client"));

        // General
        addBooleanEntry(general, eb, "allowOwnerDamage", false, () -> config.allowOwnerDamage, v -> config.allowOwnerDamage = v);
        addBooleanEntry(general, eb, "followOwner", true, () -> config.followOwner, v -> config.followOwner = v);
        addBooleanEntry(general, eb, "revengeOwner", true, () -> config.revengeOwner, v -> config.revengeOwner = v);
        addBooleanEntry(general, eb, "snowGolemAttack", false, () -> config.snowGolemAttack, v -> config.snowGolemAttack = v);
        addBooleanEntry(general, eb, "afraidOfCats", true, () -> config.afraidOfCats, v -> config.afraidOfCats = v);

        // Client
        addBooleanEntry(client, eb, "hurtSound", true, () -> config.hurtSound, v -> config.hurtSound = v);
        addBooleanEntry(client, eb, "renderPoppy", true, () -> config.renderPoppy, v -> config.renderPoppy = v);
        addBooleanEntry(client, eb, "witherRoseOnLowHealth", true, () -> config.witherRoseOnLowHealth, v -> config.witherRoseOnLowHealth = v);
        addBooleanEntry(client, eb, "tamedCreeperTexture", true, () -> config.tamedCreeperTexture, v -> config.tamedCreeperTexture = v);

        return builder.build();
    }

    private static void addBooleanEntry(ConfigCategory category, ConfigEntryBuilder eb,
                                        String key, boolean defaultValue,
                                        Supplier<Boolean> getter, Consumer<Boolean> setter) {
        category.addEntry(eb.startBooleanToggle(
                Text.translatable("config.friendcreeper." + key), getter.get())
                .setDefaultValue(defaultValue)
                .setTooltip(Text.translatable("config.friendcreeper." + key + ".tooltip"))
                .setSaveConsumer(setter)
                .build());
    }
}
