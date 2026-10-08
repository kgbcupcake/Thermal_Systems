package com.marie.thermalsystems.client.config.hub;

import com.marie.thermalsystems.client.ClientTemperatureUnit;
import com.marie.thermalsystems.client.config.ThermalExportScreen;
import com.marie.thermalsystems.client.config.ThermalImportScreen;
import com.marie.thermalsystems.client.config.ThermalSystemsConfigScreen;
import com.marie.thermalsystems.controller.TemperatureUnit;
import com.marie.thermalsystems.data.config.ThermalClientConfig;
import com.marie.thermalsystems.data.config.ThermalConfigEntries;
import com.marie.thermalsystems.data.config.ThermalConfigEntries.Entry;
import com.marie.thermalsystems.integration.coldsweat.ColdSweatIntegration;
import com.marie.thermalsystems.integration.eclipticseasons.EclipticSeasonsIntegration;
import com.marie.thermalsystems.integration.enderio.EnderIOIntegration;
import com.marie.thermalsystems.integration.enderio.EnderIOUiPersistence;
import com.marie.thermalsystems.integration.lso.LSOIntegration;
import com.marie.thermalsystems.integration.mekanism.MekanismIntegration;
import com.marie.thermalsystems.integration.pneumaticcraft.PneumaticCraftIntegration;
import com.marie.thermalsystems.integration.sereneseasons.SereneSeasonsIntegration;
import com.marie.thermalsystems.integration.toughasnails.ToughAsNailsIntegration;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.api.MarieToolbox;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.hub.HubEntry;
import dev.marie.framework.ui.hub.HubSidebarEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * The Hub config screen's sidebar: one module box per {@code ThermalConfig} section plus a Tools page,
 * each a tabbed {@link MarieToolbox} panel whose rows read and write a {@link ThermalConfigClientModel}.
 * Row labels, ranges and defaults all come from {@link ThermalConfigEntries}.
 *
 * <p>Temperature rows that have a neutral point (where Thermal Systems adds no heat or cold of its own
 * to a player) get an "Ideal" row under them showing that value, which sets it when clicked.
 */
final class ThermalHubPages {

    /** The neutral comfortable temperature both Legendary Survival Overhaul (TemperatureEnum.NORMAL's center) and Cold Sweat assume. */
    static final double COMFORT_CELSIUS = 20.0;
    /** Mekanism's own ambient temperature (HeatAPI.AMBIENT_TEMP). */
    static final double MEKANISM_AMBIENT_KELVIN = 300.0;
    static final double CELSIUS_TO_KELVIN = 273.15;

    private static final String PAGE_PREFIX = "thermalsystems.hub.page.";

    private ThermalHubPages() {
    }

    /** {@code reopen} rebuilds the whole screen, since slider ranges are fixed when a panel is built. */
    static List<HubSidebarEntry> entries(ThermalConfigClientModel model, Screen host, Runnable reopen) {
        PersistenceProvider store = EnderIOUiPersistence.get();
        Entry ambient = ThermalConfigEntries.get("integration", "defaultAmbientTemperature");
        DoubleSupplier ambientValue = () -> model.get(ambient);

        List<ThermalHomePage.Integration> integrations = new ArrayList<>();
        List<HubSidebarEntry> pages = new ArrayList<>();
        pages.add(new HubEntry(PAGE_PREFIX + "home", Component.translatable("gui.thermalsystems.hub.page.home"),
                new ThermalHomePage(model, integrations)));
        pages.add(page("simulation", simulation(model, store, ambientValue)));
        pages.add(page("integration", integration(model, store, ambient)));
        pages.add(page("zones", zones(model, store)));
        addIntegration(pages, integrations, "pneumaticcraft", PneumaticCraftIntegration.PNC_MOD_ID,
                integrationPage(model, store, "pneumaticcraft", PneumaticCraftIntegration.PNC_MOD_ID,
                        new Ideal("referenceTemperatureKelvin", "gui.thermalsystems.hub.ideal.ambient_kelvin",
                                () -> ambientValue.getAsDouble() + CELSIUS_TO_KELVIN)));
        addIntegration(pages, integrations, "mekanism", MekanismIntegration.MEKANISM_MOD_ID,
                integrationPage(model, store, "mekanism", MekanismIntegration.MEKANISM_MOD_ID,
                        new Ideal("referenceTemperatureKelvin", "gui.thermalsystems.hub.ideal.mekanism_ambient",
                                () -> MEKANISM_AMBIENT_KELVIN)));
        addIntegration(pages, integrations, "enderio", EnderIOIntegration.ENDERIO_MOD_ID,
                integrationPage(model, store, "enderio", EnderIOIntegration.ENDERIO_MOD_ID, null));
        addIntegration(pages, integrations, "lso", LSOIntegration.LSO_MOD_ID,
                integrationPage(model, store, "lso", LSOIntegration.LSO_MOD_ID,
                        new Ideal("temperatureOffset", "gui.thermalsystems.hub.ideal.match_ambient", ambientValue)));
        addIntegration(pages, integrations, "coldsweat", ColdSweatIntegration.COLDSWEAT_MOD_ID,
                integrationPage(model, store, "coldsweat", ColdSweatIntegration.COLDSWEAT_MOD_ID,
                        new Ideal("temperatureOffset", "gui.thermalsystems.hub.ideal.match_ambient", ambientValue)));
        addIntegration(pages, integrations, "toughasnails", ToughAsNailsIntegration.TOUGHASNAILS_MOD_ID,
                integrationPage(model, store, "toughasnails", ToughAsNailsIntegration.TOUGHASNAILS_MOD_ID, null));
        addIntegration(pages, integrations, "eclipticseasons", EclipticSeasonsIntegration.MOD_ID,
                integrationPage(model, store, "eclipticseasons", EclipticSeasonsIntegration.MOD_ID, null));
        addIntegration(pages, integrations, "sereneseasons", SereneSeasonsIntegration.MOD_ID,
                integrationPage(model, store, "sereneseasons", SereneSeasonsIntegration.MOD_ID, null));
        pages.add(new HubEntry(PAGE_PREFIX + "display", Component.translatable("gui.thermalsystems.hub.page.display"), display(reopen)));
        pages.add(new HubEntry(PAGE_PREFIX + "tools", Component.translatable("gui.thermalsystems.hub.page.tools"), tools(model, host)));
        return pages;
    }

    /** Sidebar page and Home row, only when that mod is actually installed. */
    private static void addIntegration(List<HubSidebarEntry> pages, List<ThermalHomePage.Integration> integrations,
                                       String section, String modId, MarieComponent content) {
        if (!ModList.get().isLoaded(modId)) {
            return;
        }
        integrations.add(new ThermalHomePage.Integration(section, modId));
        pages.add(page(section, content));
    }

    private static HubEntry page(String section, MarieComponent content) {
        return new HubEntry(PAGE_PREFIX + section, Component.translatable("config.thermalsystems.category." + section), content);
    }

    private static MarieComponent simulation(ThermalConfigClientModel m, PersistenceProvider store, DoubleSupplier ambient) {
        String id = PAGE_PREFIX + "simulation";
        Entry logging = entry("simulation", "loggingEnabled");
        BooleanSupplier loggingOn = () -> m.getBool(logging);
        Entry target = entry("simulation", "defaultTargetTemperature");

        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("config.thermalsystems.category.simulation"))
                .tab(text("gui.thermalsystems.hub.tab.general"));
        resetModule(p, m, store, id);
        row(p, m, entry("simulation", "systemEnabled"));
        row(p, m, entry("simulation", "simulationTickInterval"));
        row(p, m, entry("simulation", "heatTransferCoefficient"));
        row(p, m, entry("simulation", "temperatureConvergenceRate"));
        resetTab(p, m);

        p.tab(text("gui.thermalsystems.hub.tab.temperatures"));
        row(p, m, target);
        ideal(p, m, target, "gui.thermalsystems.hub.ideal.match_ambient", ambient, () -> true);
        row(p, m, entry("simulation", "minimumTemperature"));
        row(p, m, entry("simulation", "maximumTemperature"));
        resetTab(p, m);

        p.tab(text("gui.thermalsystems.hub.tab.logging"));
        row(p, m, logging);
        row(p, m, entry("simulation", "radiationLoggingEnabled"), loggingOn);
        row(p, m, entry("simulation", "bindingLoggingEnabled"), loggingOn);
        row(p, m, entry("simulation", "radiationDebugEnabled"));
        row(p, m, entry("simulation", "radiationChangeEpsilon"));
        resetTab(p, m);
        return p.build();
    }

    private static MarieComponent integration(ThermalConfigClientModel m, PersistenceProvider store, Entry ambient) {
        String id = PAGE_PREFIX + "integration";
        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("config.thermalsystems.category.integration"))
                .tab(text("gui.thermalsystems.hub.tab.general"));
        resetModule(p, m, store, id);
        row(p, m, ambient);
        ideal(p, m, ambient, "gui.thermalsystems.hub.ideal.comfort", () -> COMFORT_CELSIUS, () -> true);
        row(p, m, entry("integration", "playerBridgeInterval"));
        row(p, m, entry("integration", "hoverTooltipsEnabled"));
        resetTab(p, m);

        p.tab(text("gui.thermalsystems.hub.tab.sources"));
        row(p, m, entry("integration", "sourceBindingRadius"));
        row(p, m, entry("integration", "sourceScanInterval"));
        row(p, m, entry("integration", "sourceRadiationRadius"));
        row(p, m, entry("integration", "sourceRadiationInterval"));
        row(p, m, entry("integration", "sourceTrackingReverifyInterval"));
        resetTab(p, m);
        return p.build();
    }

    private static MarieComponent zones(ThermalConfigClientModel m, PersistenceProvider store) {
        String id = PAGE_PREFIX + "zones";
        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("config.thermalsystems.category.zones"))
                .tab(text("gui.thermalsystems.hub.tab.general"));
        resetModule(p, m, store, id);
        for (Entry e : ThermalConfigEntries.section("zones")) {
            row(p, m, e);
        }
        resetTab(p, m);
        return p.build();
    }

    /** {@code key} of the temperature row that gets an "Ideal" row, why it's ideal, and its value. */
    private record Ideal(String key, String reasonKey, DoubleSupplier value) {
    }

    /**
     * An optional integration's page: whether its mod is installed, its Enabled toggle, then every other
     * value in its section, greyed out while the integration is disabled.
     */
    private static MarieComponent integrationPage(ThermalConfigClientModel m, PersistenceProvider store,
                                                  String section, String modId, Ideal ideal) {
        String id = PAGE_PREFIX + section;
        boolean installed = ModList.get().isLoaded(modId);
        Entry enabled = entry(section, "enabled");
        BooleanSupplier on = () -> m.getBool(enabled);

        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("config.thermalsystems.category." + section))
                .tab(text("gui.thermalsystems.hub.tab.general"));
        resetModule(p, m, store, id);
        p.button(text("gui.thermalsystems.hub.status"),
                () -> text(!installed ? "gui.thermalsystems.hub.status.not_installed"
                        : on.getAsBoolean() ? "gui.thermalsystems.hub.status.on" : "gui.thermalsystems.hub.status.off"),
                () -> {}, () -> {});
        p.toggle(text("gui.thermalsystems.hub.enabled_restart"), on, v -> m.set(enabled, v ? 1 : 0), m::commit)
                .defaultValue(enabled.defaultValue() >= 0.5)
                .enabledWhen(m::canEdit);
        for (Entry e : ThermalConfigEntries.section(section)) {
            if (e == enabled) {
                continue;
            }
            row(p, m, e, on);
            if (ideal != null && ideal.key().equals(e.key())) {
                ideal(p, m, e, ideal.reasonKey(), ideal.value(), on);
            }
        }
        resetTab(p, m);
        return p.build();
    }

    /** This client's own display preferences - always editable, never sent to the server's config. */
    private static MarieComponent display(Runnable reopen) {
        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("gui.thermalsystems.hub.page.display"))
                .tab(text("gui.thermalsystems.hub.tab.general"));
        p.button(text("gui.thermalsystems.hub.display.unit"),
                () -> text("gui.thermalsystems.hub.display.unit." + ClientTemperatureUnit.get().name().toLowerCase(Locale.ROOT)),
                () -> {
                    ClientTemperatureUnit.toggle();
                    reopen.run();
                }, () -> {});
        return p.build();
    }

    /**
     * Export, Import and the classic Cloth screen all read/write this client's own config file, so they
     * only make sense where that file is the server's too: the main menu, or a singleplayer world.
     */
    private static MarieComponent tools(ThermalConfigClientModel m, Screen host) {
        BooleanSupplier sharesConfig = () -> !m.isRemote() || Minecraft.getInstance().hasSingleplayerServer();
        String open = text("gui.thermalsystems.hub.open");
        MarieToolbox.PanelBuilder p = MarieToolbox.panel(text("gui.thermalsystems.hub.page.tools"))
                .tab(text("gui.thermalsystems.hub.tab.tools"));
        p.button(text("gui.thermalsystems.hub.tools.export"), open,
                () -> Minecraft.getInstance().setScreen(new ThermalExportScreen(host)), () -> {})
                .enabledWhen(sharesConfig);
        p.button(text("gui.thermalsystems.hub.tools.import"), open,
                () -> Minecraft.getInstance().setScreen(new ThermalImportScreen(host)), () -> {})
                .enabledWhen(() -> sharesConfig.getAsBoolean() && m.canEdit());
        p.button(text("gui.thermalsystems.hub.tools.classic"), open,
                () -> Minecraft.getInstance().setScreen(ThermalSystemsConfigScreen.create(host)), () -> {})
                .enabledWhen(() -> sharesConfig.getAsBoolean() && m.canEdit());
        p.button(text("gui.thermalsystems.hub.tools.reload"), text("gui.thermalsystems.hub.reload"), m::reload, () -> {});
        p.button(text("gui.thermalsystems.hub.tools.access"),
                () -> text(m.canEdit() ? "gui.thermalsystems.hub.access.edit" : "gui.thermalsystems.hub.access.read_only"),
                () -> {}, () -> {});
        return p.build();
    }

    private static void row(MarieToolbox.PanelBuilder p, ThermalConfigClientModel m, Entry e) {
        row(p, m, e, () -> true);
    }

    private static void row(MarieToolbox.PanelBuilder p, ThermalConfigClientModel m, Entry e, BooleanSupplier enabled) {
        String label = text(e.langKey());
        switch (e.kind()) {
            case BOOL -> p.toggle(label, () -> m.getBool(e), v -> m.set(e, v ? 1 : 0), m::commit)
                    .defaultValue(e.defaultValue() >= 0.5);
            case INT -> p.intSlider(label, () -> m.getInt(e), v -> m.set(e, v),
                            (int) e.guiMin(), (int) e.guiMax(), (int) e.step(), e.unit(), m::commit)
                    .defaultValue(e.defaultValue());
            case DOUBLE -> p.decimalSlider(label, () -> toDisplay(e, m.get(e)), v -> m.set(e, fromDisplay(e, v)),
                            toDisplay(e, e.guiMin()), toDisplay(e, e.guiMax()), displayStep(e), e.decimals(),
                            displayUnit(e), m::commit)
                    .defaultValue(toDisplay(e, e.defaultValue()));
        }
        p.enabledWhen(() -> m.canEdit() && enabled.getAsBoolean());
    }

    /** A stored Celsius value as shown in the player's display unit; non-temperature values pass through. */
    private static double toDisplay(Entry e, double value) {
        TemperatureUnit unit = ThermalClientConfig.unit();
        return switch (e.temperature()) {
            case NONE -> value;
            case ABSOLUTE -> unit.fromCelsius(value);
            case DELTA -> unit.deltaFromCelsius(value);
        };
    }

    private static double fromDisplay(Entry e, double value) {
        TemperatureUnit unit = ThermalClientConfig.unit();
        return switch (e.temperature()) {
            case NONE -> value;
            case ABSOLUTE -> unit.toCelsius(value);
            case DELTA -> unit.deltaToCelsius(value);
        };
    }

    /** Doubled rather than scaled by 1.8 in Fahrenheit, so a slider still moves in round steps. */
    private static double displayStep(Entry e) {
        boolean converted = e.temperature() != ThermalConfigEntries.Temperature.NONE
                && ThermalClientConfig.unit() != TemperatureUnit.CELSIUS;
        return converted ? e.step() * 2 : e.step();
    }

    private static String displayUnit(Entry e) {
        return e.temperature() == ThermalConfigEntries.Temperature.NONE ? e.unit() : ThermalClientConfig.unit().symbol();
    }

    /** "Ideal: why" on the left; "USE 20.0 °C" on the right, or "20.0 °C ✓" once the row already matches. */
    private static void ideal(MarieToolbox.PanelBuilder p, ThermalConfigClientModel m, Entry e,
                              String reasonKey, DoubleSupplier ideal, BooleanSupplier enabled) {
        p.button(text(reasonKey), () -> {
            double target = ideal.getAsDouble();
            String formatted = format(e, target);
            return Math.abs(m.get(e) - target) < 1e-6
                    ? formatted + " \u2713"
                    : Component.translatable("gui.thermalsystems.hub.ideal.use", formatted).getString();
        }, () -> m.set(e, ideal.getAsDouble()), m::commit);
        p.enabledWhen(() -> m.canEdit() && enabled.getAsBoolean());
    }

    private static void resetModule(MarieToolbox.PanelBuilder p, ThermalConfigClientModel m, PersistenceProvider store, String id) {
        p.resetEverything(store, id, () -> {}).enabledWhen(m::canEdit);
    }

    private static void resetTab(MarieToolbox.PanelBuilder p, ThermalConfigClientModel m) {
        p.resetTab().enabledWhen(m::canEdit);
    }

    static String format(Entry e, double value) {
        String number = String.format(Locale.ROOT, "%." + e.decimals() + "f", toDisplay(e, value));
        String unit = displayUnit(e);
        return unit.isEmpty() ? number : number + " " + unit;
    }

    private static Entry entry(String section, String key) {
        return ThermalConfigEntries.get(section, key);
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }
}
