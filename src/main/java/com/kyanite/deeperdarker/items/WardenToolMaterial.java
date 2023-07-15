package com.kyanite.deeperdarker.items;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class WardenToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 2519;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 10.0f;
    }

    @Override
    public float getAttackDamage() {
        return 5.0f;
    }

    @Override
    public int getMiningLevel() {
        return 5;
    }

    @Override
    public int getEnchantability() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(DeeperDarkerItems.REINFORCED_ECHO_SHARD);
    }
}