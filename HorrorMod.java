package com.horrormod;

import com.horrormod.registry.ModEntities;
import com.horrormod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HorrorMod.MODID)
public class HorrorMod {
    public static final String MODID = "horrormod";

    public HorrorMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
    }
}
