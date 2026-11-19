package com.horrormod.registry;

import com.horrormod.HorrorMod;
import com.horrormod.entity.SnatcherEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HorrorMod.MODID);

    public static final RegistryObject<EntityType<SnatcherEntity>> SNATCHER = ENTITY_TYPES.register("snatcher",
            () -> EntityType.Builder.of(SnatcherEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(HorrorMod.MODID + ":snatcher"));
}
