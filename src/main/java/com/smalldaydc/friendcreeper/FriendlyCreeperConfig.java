package com.smalldaydc.friendcreeper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FriendlyCreeperConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("FriendCreeper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("friendcreeper.json");

    private static FriendlyCreeperConfig instance;

    /** Whether the owner can damage their own tamed Creeper. Default: false */
    public boolean allowOwnerDamage = false;

    /** Whether tamed Creepers follow their owner. Default: true */
    public boolean followOwner = true;

    /** Whether tamed Creepers avenge their owner when attacked. Default: true */
    public boolean revengeOwner = true;

    /** Whether Snow Golems can target tamed Creepers. Default: false */
    public boolean snowGolemAttack = false;

    /** Whether tamed Creepers play a hurt sound when at low health. Client-side only. Default: true */
    public boolean hurtSound = true;

    /** Whether to render the poppy on tamed Creepers' heads. Client-side only. Default: true */
    public boolean renderPoppy = true;

    /** Whether tamed Creepers use custom textures (happy/sad face). Client-side only. Default: true */
    public boolean tamedCreeperTexture = true;

    /** Whether tamed Creepers show a scared face when fleeing from cats. Client-side only. Default: true */
    public boolean scaredFace = true;

    /** Whether tamed Creepers are afraid of cats and ocelots. Default: true */
    public boolean afraidOfCats = true;

    /** Whether tamed Creepers naturally regenerate health over time. Default: true */
    public boolean naturalRegeneration = true;

    /** Whether tamed Creepers show a wither rose instead of a poppy when at low health. Requires renderPoppy. Client-side only. Default: true */
    public boolean witherRoseOnLowHealth = true;

    public static FriendlyCreeperConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                instance = GSON.fromJson(reader, FriendlyCreeperConfig.class);
                if (instance == null) {
                    LOGGER.warn("Empty configuration file; reset to default values.");
                    instance = new FriendlyCreeperConfig();
                } else {
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to load configuration file; reset to default values.");
                instance = new FriendlyCreeperConfig();
            }
        } else {
            instance = new FriendlyCreeperConfig();
        }
        save();
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            LOGGER.warn("Failed to save configuration file.", e);
        }
    }
}