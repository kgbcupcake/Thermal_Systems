package com.marie.thermalsystems.client;

import com.marie.thermalsystems.client.render.WallThermostatRenderer;
import com.marie.thermalsystems.client.screen.ThermostatScreen;
import com.marie.thermalsystems.client.screen.ZoneEditorScreen;
import com.marie.thermalsystems.client.screen.ZoneGadgetScreen;
import com.marie.thermalsystems.client.zone.ClientZones;
import com.marie.thermalsystems.client.zone.ZoneSelectionRenderer;
import com.marie.thermalsystems.controller.ThermalScreens;
import com.marie.thermalsystems.registry.ThermalRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Client side of the zone tools: the gadget preview, the tablet/gadget screens, the wall display and the display unit. */
public final class ThermalClientContent {

    private ThermalClientContent() {
    }

    public static void init(IEventBus modEventBus) {
        ClientZones.init();
        ClientTemperatureUnit.init();
        ZoneSelectionRenderer.init(modEventBus);
        ThermalScreens.setOpener(new ThermalScreens.Opener() {
            @Override
            public void openThermostat(@Nullable UUID zoneId, @Nullable BlockPos wallPos) {
                Minecraft.getInstance().setScreen(new ThermostatScreen(zoneId, wallPos));
            }

            @Override
            public void openGadget() {
                Minecraft.getInstance().setScreen(new ZoneGadgetScreen());
            }

            @Override
            public void openZoneEditor(BlockPos first, BlockPos second, @Nullable UUID editing, String name) {
                Minecraft.getInstance().setScreen(new ZoneEditorScreen(first, second, editing, name));
            }
        });
        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(ThermalRegistries.WALL_THERMOSTAT_ENTITY.get(), WallThermostatRenderer::new));
    }
}
