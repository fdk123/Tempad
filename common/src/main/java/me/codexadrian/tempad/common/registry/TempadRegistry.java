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
import me.codexadrian.tempad.common.block.WorkstationBlock;
import me.codexadrian.tempad.common.block.WorkstationBlockEntity;
import me.codexadrian.tempad.common.block.WorkstationChildBlock;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlock;
import me.codexadrian.tempad.common.block.TimedoorProjectorBlockEntity;
import me.codexadrian.tempad.common.items.TimedoorProjectorItem;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import me.codexadrian.tempad.common.recipe.TempadUpgradeRecipe;

public class TempadRegistry {
    public static final ResourcefulRegistry<Item> ITEMS = ResourcefulRegistries.create(BuiltInRegistries.ITEM, Tempad.MODID);
    public static final ResourcefulRegistry<Block> BLOCKS = ResourcefulRegistries.create(BuiltInRegistries.BLOCK, Tempad.MODID);
    public static final ResourcefulRegistry<BlockEntityType<?>> BLOCK_ENTITIES = ResourcefulRegistries.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tempad.MODID);
    public static final ResourcefulRegistry<RecipeSerializer<?>> RECIPE_SERIALIZERS = ResourcefulRegistries.create(BuiltInRegistries.RECIPE_SERIALIZER, Tempad.MODID);
    public static final ResourcefulRegistry<RecipeType<?>> RECIPE_TYPES = ResourcefulRegistries.create(BuiltInRegistries.RECIPE_TYPE, Tempad.MODID);
    public static final RegistryEntry<RecipeSerializer<TempadUpgradeRecipe>> UPGRADE_SERIALIZER = RECIPE_SERIALIZERS.register("upgrade", TempadUpgradeRecipe.Serializer::new);
    public static final RegistryEntry<RecipeType<TempadUpgradeRecipe>> UPGRADE_TYPE = RECIPE_TYPES.register("upgrade", () -> new RecipeType<>() {});
    public static final RegistryEntry<Block> WORKSTATION_BLOCK = BLOCKS.register("workstation", () -> new WorkstationBlock(BlockBehaviour.Properties.of().strength(3.0F, 1200.0F)));
    public static final RegistryEntry<Block> WORKSTATION_CHILD_BLOCK = BLOCKS.register("workstation_child", () -> new WorkstationChildBlock(BlockBehaviour.Properties.of().strength(3.0F, 1200.0F).noOcclusion()));
    public static final RegistryEntry<Item> WORKSTATION = ITEMS.register("workstation", () -> new BlockItem(WORKSTATION_BLOCK.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<BlockEntityType<WorkstationBlockEntity>> WORKSTATION_BLOCK_ENTITY = BLOCK_ENTITIES.register("workstation", () -> BlockEntityType.Builder.of(WorkstationBlockEntity::new, WORKSTATION_BLOCK.get()).build(null));
    public static final RegistryEntry<Block> TIMEDOOR_PROJECTOR_BLOCK = BLOCKS.register("timedoor_projector", () -> new TimedoorProjectorBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).noOcclusion()));
    public static final RegistryEntry<Item> TIMEDOOR_PROJECTOR = ITEMS.register("timedoor_projector", () -> new TimedoorProjectorItem((TimedoorProjectorBlock) TIMEDOOR_PROJECTOR_BLOCK.get(), new Item.Properties()));
    public static final RegistryEntry<BlockEntityType<TimedoorProjectorBlockEntity>> TIMEDOOR_PROJECTOR_BLOCK_ENTITY = BLOCK_ENTITIES.register("timedoor_projector", () -> BlockEntityType.Builder.of(TimedoorProjectorBlockEntity::new, TIMEDOOR_PROJECTOR_BLOCK.get()).build(null));
    public static final ResourcefulRegistry<EntityType<?>> ENTITIES = ResourcefulRegistries.create(BuiltInRegistries.ENTITY_TYPE, Tempad.MODID);

    //items
    public static final RegistryEntry<Item> TEMPAD = ITEMS.register("tempad", () -> new TempadItem(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> CREATIVE_TEMPAD = ITEMS.register("he_who_remains_tempad", () -> new AdvancedTempadItem(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> LOCATION_CARD = ITEMS.register("location_card", () -> new LocationCard(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> TIME_STEEL = ITEMS.register("time_steel", () -> new Item(new Item.Properties()));
    public static final RegistryEntry<Item> NEW_LOCATION_UPGRADE = ITEMS.register("new_location_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryEntry<Item> KNOWLEDGE_REPOSITORY_UPGRADE = ITEMS.register("knowledge_repository_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
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
