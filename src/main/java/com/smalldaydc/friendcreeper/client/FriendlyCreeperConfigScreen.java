package com.smalldaydc.friendcreeper.client;

import com.smalldaydc.friendcreeper.FriendlyCreeperConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class FriendlyCreeperConfigScreen {

    public static Screen create(Screen parent) {
        FriendlyCreeperConfig config = FriendlyCreeperConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.friendcreeper.title"))
                .setSavingRunnable(FriendlyCreeperConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.general"));
        ConfigCategory client = builder.getOrCreateCategory(Text.translatable("config.friendcreeper.category.client"));

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.allowOwnerDamage"),
                        config.allowOwnerDamage)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.friendcreeper.allowOwnerDamage.tooltip"))
                .setSaveConsumer(value -> config.allowOwnerDamage = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.followOwner"),
                        config.followOwner)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.followOwner.tooltip"))
                .setSaveConsumer(value -> config.followOwner = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.revengeOwner"),
                        config.revengeOwner)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.revengeOwner.tooltip"))
                .setSaveConsumer(value -> config.revengeOwner = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.snowGolemAttack"),
                        config.snowGolemAttack)
                .setDefaultValue(false)
                .setTooltip(Text.translatable("config.friendcreeper.snowGolemAttack.tooltip"))
                .setSaveConsumer(value -> config.snowGolemAttack = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.afraidOfCats"),
                        config.afraidOfCats)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.afraidOfCats.tooltip"))
                .setSaveConsumer(value -> config.afraidOfCats = value)
                .build());

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.witherRoseOnLowHealth"),
                        config.witherRoseOnLowHealth)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.witherRoseOnLowHealth.tooltip"))
                .setSaveConsumer(value -> config.witherRoseOnLowHealth = value)
                .build());

        client.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.hurtSound"),
                        config.hurtSound)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.hurtSound.tooltip"))
                .setSaveConsumer(value -> config.hurtSound = value)
                .build());

        client.addEntry(entryBuilder
                .startBooleanToggle(
                        Text.translatable("config.friendcreeper.renderPoppy"),
                        config.renderPoppy)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.friendcreeper.renderPoppy.tooltip"))
                .setSaveConsumer(value -> config.renderPoppy = value)
                .build());

        return builder.build();
    }
}
