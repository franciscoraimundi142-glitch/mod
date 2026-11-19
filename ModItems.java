package com.horrormod.registry;

import com.horrormod.HorrorMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HorrorMod.MODID);

    public static final RegistryObject<Item> SNATCHER_SPAWN_EGG = ITEMS.register("snatcher_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SNATCHER, 0xC9B8B0, 0x7A0E0E, new Item.Properties()));
}
