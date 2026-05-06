package com.smalldaydc.friendcreeper.client;

import com.smalldaydc.friendcreeper.FriendCreeperConfig;
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
public class FriendCreeperConfigScreen {

    public static Screen create(Screen parent) {
        FriendCreeperConfig config = FriendCreeperConfig.get();
        FriendCreeperConfig defaults = new FriendCreeperConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.friendcreeper.title"))
                .setSavingRunnable(FriendCreeperConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.general"));
        ConfigCategory client = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.client"));

        // General
        addBooleanEntry(general, eb, "allowOwnerDamage", defaults.allowOwnerDamage, () -> config.allowOwnerDamage, v -> config.allowOwnerDamage = v);
        addBooleanEntry(general, eb, "followOwner", defaults.followOwner, () -> config.followOwner, v -> config.followOwner = v);
        addBooleanEntry(general, eb, "revengeOwner", defaults.revengeOwner, () -> config.revengeOwner, v -> config.revengeOwner = v);
        addBooleanEntry(general, eb, "snowGolemAttack", defaults.snowGolemAttack, () -> config.snowGolemAttack, v -> config.snowGolemAttack = v);
        addBooleanEntry(general, eb, "afraidOfCats", defaults.afraidOfCats, () -> config.afraidOfCats, v -> config.afraidOfCats = v);
        addBooleanEntry(general, eb, "naturalRegeneration", defaults.naturalRegeneration, () -> config.naturalRegeneration, v -> config.naturalRegeneration = v);
        addBooleanEntry(general, eb, "feedOwnerCat", defaults.feedOwnerCat, () -> config.feedOwnerCat, v -> config.feedOwnerCat = v);

        // Client
        addBooleanEntry(client, eb, "hurtSound", defaults.hurtSound, () -> config.hurtSound, v -> config.hurtSound = v);
        addBooleanEntry(client, eb, "renderPoppy", defaults.renderPoppy, () -> config.renderPoppy, v -> config.renderPoppy = v);
        addBooleanEntry(client, eb, "witherRoseOnLowHealth", defaults.witherRoseOnLowHealth, () -> config.witherRoseOnLowHealth, v -> config.witherRoseOnLowHealth = v);
        addBooleanEntry(client, eb, "tamedCreeperTexture", defaults.tamedCreeperTexture, () -> config.tamedCreeperTexture, v -> config.tamedCreeperTexture = v);
        addBooleanEntry(client, eb, "scaredFace", defaults.scaredFace, () -> config.scaredFace, v -> config.scaredFace = v);

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