package com.marie.thermalsystems.client.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.marie.framework.client.config.importexport.ImportExportManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes the current Thermal Systems config to {@code config/thermalsystems/exports/}
 * or a share code. {@link ImportExportManager#buildShareCode(JsonObject)} /
 * {@link ImportExportManager#parseShareCode(String)} are reused as-is (pure
 * JSON&lt;-&gt;string helpers, not tied to any particular mod's data), but the
 * file/JSON shape itself comes from {@link ThermalConfigIO} since Marie's
 * Lib's own export manager only knows how to serialize its own scanner/debug
 * settings.
 */
public final class ThermalExportScreen extends Screen {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Screen returnTo;
    private EditBox shareBox;
    private Component status = Component.empty();

    public ThermalExportScreen(Screen returnTo) {
        super(Component.translatable(configKey("importExport.export.title")));
        this.returnTo = returnTo;
    }

    private static String configKey(String suffix) {
        return "config.thermalsystems." + suffix;
    }

    private static Path exportsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve("thermalsystems").resolve("exports");
    }

    @Override
    protected void init() {
        int y = 40;
        shareBox = new EditBox(this.font, this.width / 2 - 150, y, 300, 20,
                Component.translatable(configKey("importExport.export.shareCode")));
        shareBox.setMaxLength(8192);
        addRenderableWidget(shareBox);

        addRenderableWidget(Button.builder(Component.translatable(configKey("importExport.export.toFile")), b -> exportFile())
                .bounds(this.width / 2 - 154, y + 30, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable(configKey("importExport.export.toShareCode")), b -> exportShare())
                .bounds(this.width / 2 + 4, y + 30, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(returnTo);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (!status.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 44, 0xA0A0A0);
        }
    }

    private void exportFile() {
        try {
            JsonObject root = ThermalConfigIO.buildRoot();
            Files.createDirectories(exportsDirectory());
            String stem = "thermalsystems-config-" + LocalDateTime.now().format(FILE_TS);
            Path file = exportsDirectory().resolve(stem + ".json");
            try (Writer w = Files.newBufferedWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
            status = Component.translatable(configKey("importExport.export.saved"), file.getFileName().toString());
        } catch (IOException e) {
            status = Component.translatable(configKey("importExport.export.failed"), e.getMessage());
        }
    }

    private void exportShare() {
        try {
            JsonObject root = ThermalConfigIO.buildRoot();
            shareBox.setValue(ImportExportManager.buildShareCode(root));
            status = Component.translatable(configKey("importExport.export.shareReady"));
        } catch (IOException e) {
            status = Component.translatable(configKey("importExport.export.failed"), e.getMessage());
        }
    }
}
