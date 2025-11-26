package com.alterSMP.legendaryweapons.altar;

import com.alterSMP.legendaryweapons.LegendaryWeaponsPlugin;
import com.alterSMP.legendaryweapons.items.LegendaryType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AltarRecipe {

    private final LegendaryType result;
    private final RecipeIngredient[][] ingredients;

    public AltarRecipe(LegendaryType result, RecipeIngredient[][] ingredients) {
        this.result = result;
        this.ingredients = ingredients;
    }

    public LegendaryType getResult() {
        return result;
    }

    public RecipeIngredient[][] getIngredients() {
        return ingredients;
    }

    public boolean matches(ItemStack[] gridItems) {
        if (gridItems.length != 25) {
            return false;
        }

        // Check each position in 5x5 grid
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                RecipeIngredient expected = ingredients[row][col];
                ItemStack actual = gridItems[index];

                if (!expected.matches(actual)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Factory method to create all recipes from config
    public static Map<String, AltarRecipe> createAllRecipes() {
        Map<String, AltarRecipe> recipes = new HashMap<>();
        LegendaryWeaponsPlugin plugin = LegendaryWeaponsPlugin.getInstance();

        // Load all recipes from config
        for (LegendaryType type : LegendaryType.values()) {
            RecipeIngredient[][] ingredients = plugin.getConfigManager().getRecipeIngredients(type.getId());
            recipes.put(type.getId(), new AltarRecipe(type, ingredients));
        }

        return recipes;
    }

}
