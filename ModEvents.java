package com.horrormod.event;

import com.horrormod.HorrorMod;
import com.horrormod.entity.SnatcherEntity;
import com.horrormod.registry.ModEntities;
import com.horrormod.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HorrorMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SNATCHER.get(), SnatcherEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.SNATCHER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SnatcherEntity::checkSnatcherSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
    }

    @SubscribeEvent
    public static void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SNATCHER_SPAWN_EGG.get());
        }
    }
}
