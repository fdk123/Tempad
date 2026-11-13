package me.codexadrian.tempad.common.config;

import com.teamresourceful.resourcefulconfig.common.annotations.Comment;
import com.teamresourceful.resourcefulconfig.common.annotations.Config;
import com.teamresourceful.resourcefulconfig.common.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.common.annotations.IntRange;
import com.teamresourceful.resourcefulconfig.common.config.EntryType;
import com.teamresourceful.resourcefulconfig.web.annotations.Link;
import com.teamresourceful.resourcefulconfig.web.annotations.WebInfo;
import me.codexadrian.tempad.common.Tempad;

@Config(Tempad.MODID)
@WebInfo(
        icon = "watch",
        color = "#ff6f00",
        title = "Tempad",
        description = "Adds a device that allows you to teleport to any location stored on your Tempad.",
        links = {
                @Link(title = "CurseForge", value = "https://www.curseforge.com/minecraft/mc-mods/tempad", icon = "curseforge"),
                @Link(title = "Modrinth", value = "https://modrinth.com/mod/tempad", icon = "modrinth"),
                @Link(title = "GitHub", value = "https://modrinth.com/mod/tempad", icon = "github"),
                @Link(title = "Discord", value = "https://discord.terrarium.earth", icon = "gamepad-2")
        }
)
public final class TempadConfig {
    @ConfigEntry(id = "requireNewLocationUpgrade", type = EntryType.BOOLEAN, translation = "config.tempad.require_new_location_upgrade")
    @Comment("Require the New Location upgrade to save locations on a Tempad. Disabled by default for existing worlds.")
    public static boolean requireNewLocationUpgrade = false;
    @ConfigEntry(id = "distanceFromPlayer", type = EntryType.INTEGER, translation = "config.tempad.distance_from_player")
    @Comment("The distance from the player that the Tempad is summoned.")
    @IntRange(min = 1, max = 10)
    public static int distanceFromPlayer = 3;

    @ConfigEntry(id = "timedoorWaitTime", type = EntryType.INTEGER, translation = "config.tempad.timedoor_wait_time")
    @Comment("The amount of time in ticks that the timedoor will wait before closing itself after the owner walked through it.")
    public static int timedoorWait = 60;

    @ConfigEntry(id = "timedoorAddWaitTime", type = EntryType.INTEGER, translation = "config.tempad.timedoor_add_wait_time")
    @Comment("The amount of time in ticks that the Tempad will add to the wait time when the player is in the Tempad.")
    public static int timedoorAddWaitTime = 40;

    // allow interdimensional travel
    @ConfigEntry(id = "allowInterdimensionalTravel", type = EntryType.BOOLEAN, translation = "config.tempad.allow_interdimensional_travel")
    @Comment("Whether or not the Tempad should allow interdimensional travel.")
    public static boolean allowInterdimensionalTravel = true;

    // allow exporting of locations
    @ConfigEntry(id = "allowExporting", type = EntryType.BOOLEAN, translation = "config.tempad.allow_exporting")
    @Comment("Whether or not the Tempad should allow exporting of locations onto Location Cards.")
    public static boolean allowExporting = true;

    //consume cooldown for exporting boolean
    @ConfigEntry(id = "consumeCooldown", type = EntryType.BOOLEAN, translation = "config.tempad.consume_cooldown")
    @Comment("Whether or not the Tempad should consume a cooldown when exporting a location.")
    public static boolean consumeCooldown = true;

    //enable waystones compat
    @ConfigEntry(id = "waystonesCompat", type = EntryType.BOOLEAN, translation = "config.tempad.waystones_compat")
    @Comment("Whether or not the Tempad should allow teleporting to waystones.")
    public static boolean waystonesCompat = true;

    //enable fabric waystones compat
    @ConfigEntry(id = "fabricWaystonesCompat", type = EntryType.BOOLEAN, translation = "config.tempad.fabric_waystones_compat")
    @Comment("Whether or not the Tempad should allow teleporting to waystones from the Fabric version of Waystones by LordDeatHunter (fwaystones) (ignore on forge).")
    public static boolean fabricWaystonesCompat = true;

    @ConfigEntry(id = "timedoorFuelAmount", type = EntryType.INTEGER, translation = "config.tempad.timedoor_fuel_amount")
    @Comment("The amount of fuel that the timedoor will consume on opening of the timedoor.")
    public static int tempadFuelConsumptionValue = 180;

    @ConfigEntry(id = "timedoorFuelCapacity", type = EntryType.INTEGER, translation = "config.tempad.timedoor_fuel_capacity")
    @Comment("The amount of fuel that the timedoor can hold.")
    public static int tempadFuelCapacityValue = 1000;

    @ConfigEntry(id = "advancedTimedoorFuelAmount", type = EntryType.INTEGER, translation = "config.tempad.advanced_timedoor_fuel_amount")
    @Comment("The amount of fuel that the advanced timedoor will consume on opening of the timedoor.")
    public static int advancedTempadfuelConsumptionValue = 1;

    @ConfigEntry(id = "advancedTimedoorFuelCapacity", type = EntryType.INTEGER, translation = "config.tempad.advanced_timedoor_fuel_capacity")
    @Comment("The amount of fuel that the advanced timedoor can hold.")
    public static int advancedTempadfuelCapacityValue = 1000;

    @ConfigEntry(id = "chronometerGenerationInterval", type = EntryType.INTEGER, translation = "config.tempad.chronometer_generation_interval")
    @Comment("The number of ticks between Chronometer generation cycles.")
    @IntRange(min = 1, max = 72000)
    public static int chronometerGenerationInterval = 48;

    @ConfigEntry(id = "chronometerGenerationAmount", type = EntryType.INTEGER, translation = "config.tempad.chronometer_generation_amount")
    @Comment("The number of chronons generated by a Chronometer per cycle.")
    @IntRange(min = 1, max = 1000)
    public static int chronometerGenerationAmount = 1;

    @ConfigEntry(id = "chrononGeneratorCapacity", type = EntryType.INTEGER, translation = "config.tempad.chronon_generator_capacity")
    @Comment("The amount of chronons a Chronon Generator can store.")
    @IntRange(min = 1, max = 100000)
    public static int chrononGeneratorCapacity = 1000;

    @ConfigEntry(id = "chrononGeneratorGenerationInterval", type = EntryType.INTEGER, translation = "config.tempad.chronon_generator_generation_interval")
    @Comment("The number of ticks between Chronon Generator cycles.")
    @IntRange(min = 1, max = 72000)
    public static int chrononGeneratorGenerationInterval = 48;

    @ConfigEntry(id = "chrononGeneratorGenerationAmount", type = EntryType.INTEGER, translation = "config.tempad.chronon_generator_generation_amount")
    @Comment("The number of chronons generated by a Chronon Generator per cycle.")
    @IntRange(min = 1, max = 1000)
    public static int chrononGeneratorGenerationAmount = 1;

    @ConfigEntry(id = "metronomeCapacity", type = EntryType.INTEGER, translation = "config.tempad.metronome_capacity")
    @Comment("The amount of chronons a Metronome can store.")
    @IntRange(min = 1, max = 100000)
    public static int metronomeCapacity = 6000;

    @ConfigEntry(id = "metronomeGenerationInterval", type = EntryType.INTEGER, translation = "config.tempad.metronome_generation_interval")
    @Comment("The number of ticks between Metronome generation cycles.")
    @IntRange(min = 1, max = 72000)
    public static int metronomeGenerationInterval = 24;

    @ConfigEntry(id = "metronomeGenerationAmount", type = EntryType.INTEGER, translation = "config.tempad.metronome_generation_amount")
    @Comment("The number of chronons generated by a Metronome per cycle.")
    @IntRange(min = 1, max = 1000)
    public static int metronomeGenerationAmount = 1;

    @ConfigEntry(id = "metronomeDistributionRange", type = EntryType.INTEGER, translation = "config.tempad.metronome_distribution_range")
    @Comment("The radius in blocks in which a Metronome can charge its owner.")
    @IntRange(min = 1, max = 64)
    public static int metronomeDistributionRange = 8;

    @ConfigEntry(id = "metronomeTransferRate", type = EntryType.INTEGER, translation = "config.tempad.metronome_transfer_rate")
    @Comment("Maximum chronons transferred per tick to each adjacent Workstation.")
    @IntRange(min = 1, max = 1000)
    public static int metronomeTransferRate = 20;

    @ConfigEntry(id = "metronomeRedstonePause", type = EntryType.BOOLEAN, translation = "config.tempad.metronome_redstone_pause")
    @Comment("Whether a redstone signal pauses Metronome generation.")
    public static boolean metronomeRedstonePause = true;

    @ConfigEntry(id = "workstationPortalSustainCost", type = EntryType.INTEGER, translation = "config.tempad.workstation_portal_sustain_cost")
    @Comment("Chronons consumed from the installed Tempad every second while a Workstation holds a Timedoor open.")
    @IntRange(min = 0, max = 1000)
    public static int workstationPortalSustainCost = 1;

    @ConfigEntry(id = "timedoorFuelType", type = EntryType.STRING, translation = "config.tempad.timedoor_fuel_type")
    @Comment("The type of fuel that the timedoor will consume.")
    public static String tempadFuelType = "tempad:timer";

    @ConfigEntry(id = "advancedTimedoorFuelType", type = EntryType.STRING, translation = "config.tempad.advanced_timedoor_fuel_type")
    @Comment("The type of fuel that the advanced timedoor will consume.")
    public static String advancedTempadFuelType = "tempad:unlimited";
}
