package me.codexadrian.tempad.common.recipe;

import com.google.gson.JsonObject;
import me.codexadrian.tempad.common.registry.TempadRegistry;
import me.codexadrian.tempad.common.items.TempadUpgrades;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;

/** Workstation download recipe from Tempad 3.0, adapted to 1.20.1 recipe APIs. */
public record TempadUpgradeRecipe(ResourceLocation id, Ingredient item, int downloadTime, ResourceLocation upgrade) implements Recipe<Container> {
    @Override public boolean matches(Container container, Level level) { return item.test(container.getItem(0)); }
    @Override public ItemStack assemble(Container container, RegistryAccess registries) { ItemStack result = container.getItem(1).copy(); TempadUpgrades.install(result, upgrade); return result; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { ItemStack result = new ItemStack(TempadRegistry.TEMPAD.get()); TempadUpgrades.install(result, upgrade); return result; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return TempadRegistry.UPGRADE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TempadRegistry.UPGRADE_TYPE.get(); }

    public static final class Serializer implements RecipeSerializer<TempadUpgradeRecipe> {
        @Override public TempadUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient item = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "item"));
            int time = GsonHelper.getAsInt(json, "download_time");
            if (time < 1 || time > 72000) throw new IllegalArgumentException("Invalid Tempad upgrade download time: " + time);
            return new TempadUpgradeRecipe(id, item, time, new ResourceLocation(GsonHelper.getAsString(json, "upgrade")));
        }
        @Override public @Nullable TempadUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new TempadUpgradeRecipe(id, Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readResourceLocation());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, TempadUpgradeRecipe recipe) {
            recipe.item().toNetwork(buffer);
            buffer.writeVarInt(recipe.downloadTime());
            buffer.writeResourceLocation(recipe.upgrade());
        }
    }
}
