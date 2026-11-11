package me.codexadrian.tempad.common.registry;

import com.teamresourceful.resourcefullib.common.registry.RegistryEntry;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistries;
import com.teamresourceful.resourcefullib.common.registry.ResourcefulRegistry;
import me.codexadrian.tempad.common.Tempad;
import me.codexadrian.tempad.common.entity.TimedoorEntity;
import me.codexadrian.tempad.common.items.AdvancedTempadItem;
import me.codexadrian.tempad.common.items.ChrononBatteryItem;
import me.codexadrian.tempad.common.items.ChrononCellItem;
import me.codexadrian.tempad.common.items.ChronometerItem;
import me.codexadrian.tempad.common.items.ChrononGeneratorItem;
import me.codexadrian.tempad.common.items.MetronomeItem;
import me.codexadrian.tempad.common.block.MetronomeBlock;
import me.codexadrian.tempad.common.block.MetronomeBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import me.codexadrian.tempad.common.items.CardWalletItem;
import me.codexadrian.tempad.common.items.LocationCard;
import me.codexadrian.tempad.common.items.TempadItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

public class TempadRegistry {
    public static final ResourcefulRegistry<Item> ITEMS = ResourcefulRegistries.create(BuiltInRegistries.ITEM, Tempad.MODID);
    public static final ResourcefulRegistry<Block> BLOCKS = ResourcefulRegistries.create(BuiltInRegistries.BLOCK, Tempad.MODID);
    public static final ResourcefulRegistry<BlockEntityType<?>> BLOCK_ENTITIES = ResourcefulRegistries.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tempad.MODID);
    public static final ResourcefulRegistry<EntityType<?>> ENTITIES = ResourcefulRegistries.create(BuiltInRegistries.ENTITY_TYPE, Tempad.MODID);

    //items
    public static final RegistryEntry<Item> TEMPAD = ITEMS.register("tempad", () -> new TempadItem(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> CREATIVE_TEMPAD = ITEMS.register("he_who_remains_tempad", () -> new AdvancedTempadItem(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> LOCATION_CARD = ITEMS.register("location_card", () -> new LocationCard(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> TIME_STEEL = ITEMS.register("time_steel", () -> new Item(new Item.Properties()));
    public static final RegistryEntry<Item> CHRONON_BATTERY = ITEMS.register("chronon_battery", () -> new ChrononBatteryItem(new Item.Properties(), 1000));
    public static final RegistryEntry<Item> CHRONON_CELL = ITEMS.register("chronon_cell", () -> new ChrononCellItem(new Item.Properties()));
    public static final RegistryEntry<Item> CHRONOMETER = ITEMS.register("chronometer", () -> new ChronometerItem(new Item.Properties()));
    public static final RegistryEntry<Item> CHRONON_GENERATOR = ITEMS.register("chronon_generator", () -> new ChrononGeneratorItem(new Item.Properties()));
    public static final RegistryEntry<Block> METRONOME_BLOCK = BLOCKS.register("metronome", () -> new MetronomeBlock(BlockBehaviour.Properties.of().strength(3.0F, 1200.0F)));
    public static final RegistryEntry<Item> METRONOME = ITEMS.register("metronome", () -> new BlockItem(METRONOME_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<BlockEntityType<MetronomeBlockEntity>> METRONOME_BLOCK_ENTITY = BLOCK_ENTITIES.register("metronome", () -> BlockEntityType.Builder.of(MetronomeBlockEntity::new, METRONOME_BLOCK.get()).build(null));
    public static final RegistryEntry<Item> CARD_WALLET = ITEMS.register("card_wallet", () -> new CardWalletItem(new Item.Properties()));

    //the one entity in this mod
    public static final RegistryEntry<EntityType<TimedoorEntity>> TIMEDOOR_ENTITY = ENTITIES.register("timedoor", () -> EntityType.Builder.of(TimedoorEntity::new, MobCategory.MISC).sized(0.4F, 2.3F).noSave().build("timedoor"));
}
