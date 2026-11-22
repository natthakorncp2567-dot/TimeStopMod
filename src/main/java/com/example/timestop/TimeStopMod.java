package com.example.timestop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class TimeStopMod implements ModInitializer {
    public static final String MOD_ID = "timestop";

    public static final ResourceKey<Item> TIME_CLOCK_KEY = createKey("time_clock");
    public static final Item TIME_CLOCK = new TimeClockItem(new Item.Properties().setId(TIME_CLOCK_KEY).stacksTo(1), false);

    public static final ResourceKey<Item> COPPER_CLOCK_KEY = createKey("copper_clock");
    public static final Item COPPER_CLOCK = new TimeClockItem(new Item.Properties().setId(COPPER_CLOCK_KEY).durability(20), true);

    public static final ResourceKey<Item> DECAY_ITEM_KEY = createKey("decay_item");
    public static final Item DECAY_ITEM = new DecayItem(new Item.Properties().setId(DECAY_ITEM_KEY).stacksTo(1));

    public static final ResourceKey<Item> TIME_REVERSER_KEY = createKey("time_reverser");
    public static final Item TIME_REVERSER = new TimeReverserItem(new Item.Properties().setId(TIME_REVERSER_KEY).stacksTo(1));

    private static ResourceKey<Item> createKey(String name) {
        return ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
    }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, TIME_CLOCK_KEY, TIME_CLOCK);
        Registry.register(BuiltInRegistries.ITEM, COPPER_CLOCK_KEY, COPPER_CLOCK);
        Registry.register(BuiltInRegistries.ITEM, DECAY_ITEM_KEY, DECAY_ITEM);
        Registry.register(BuiltInRegistries.ITEM, TIME_REVERSER_KEY, TIME_REVERSER);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(TIME_CLOCK);
            entries.accept(COPPER_CLOCK);
            entries.accept(DECAY_ITEM);
            entries.accept(TIME_REVERSER);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TimeReverserItem.recordServerTick(server);
            TimeClockItem.tickCopperClockLimit(server);
        });
    }
}
