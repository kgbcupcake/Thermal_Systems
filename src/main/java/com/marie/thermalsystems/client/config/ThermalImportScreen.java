package com.marie.thermalsystems.client.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.marie.framework.client.config.importexport.ImportExportManager;
import dev.marie.framework.client.config.importexport.ImportExportToast;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Reads a Thermal Systems config export (share code or the newest file in
 * {@code config/thermalsystems/exports/}) and applies it via
 * {@link ThermalConfigIO#applyRoot(JsonObject)}.
 */
public final class ThermalImportScreen extends Screen {

    private final Screen returnTo;
    private EditBox shareBox;
    private Component status = Component.empty();
    private JsonObject pendingRoot;

    public ThermalImportScreen(Screen returnTo) {
        super(Component.translatable(configKey("importExport.import.title")));
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
        shareBox = new EditBox(this.font, this.width / 2 - 150, 36, 300, 20,
                Component.translatable(configKey("importExport.import.pasteShareCode")));
        shareBox.setMaxLength(8192);
        addRenderableWidget(shareBox);

        addRenderableWidget(Button.builder(Component.translatable(configKey("importExport.import.parseShareCode")), b -> parseShare())
                .bounds(this.width / 2 - 154, 60, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable(configKey("importExport.import.pickFile")), b -> pickLatestFile())
                .bounds(this.width / 2 + 4, 60, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable(configKey("importExport.import.apply")), b -> applyPending())
                .bounds(this.width / 2 - 154, this.height - 52, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(this.width / 2 + 4, this.height - 52, 100, 20)
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
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 68, 0xA0A0A0);
        }
    }

    private void parseShare() {
        try {
            pendingRoot = ImportExportManager.parseShareCode(shareBox.getValue());
            status = Component.translatable(configKey("importExport.import.parsed"));
        } catch (IOException e) {
            status = Component.translatable(configKey("importExport.import.failed"), e.getMessage());
        }
    }

    private void pickLatestFile() {
        try {
            Path dir = exportsDirectory();
            if (!Files.isDirectory(dir)) {
                status = Component.translatable(configKey("importExport.import.noFiles"));
                return;
            }
            List<Path> files;
            try (Stream<Path> stream = Files.list(dir)) {
                files = stream
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .sorted()
                        .toList();
            }
            if (files.isEmpty()) {
                status = Component.translatable(configKey("importExport.import.noFiles"));
                return;
            }
            try (Reader r = Files.newBufferedReader(files.get(files.size() - 1))) {
                pendingRoot = new Gson().fromJson(r, JsonObject.class);
            }
            status = Component.translatable(configKey("importExport.import.parsed"));
        } catch (IOException e) {
            status = Component.translatable(configKey("importExport.import.failed"), e.getMessage());
        }
    }

    private void applyPending() {
        if (pendingRoot == null) {
            status = Component.translatable(configKey("importExport.import.nothingToApply"));
            return;
        }
        try {
            ThermalConfigIO.applyRoot(pendingRoot);
            status = Component.translatable(configKey("importExport.import.applied"));
            ImportExportToast.show(Component.translatable(configKey("importExport.import.applied")));
        } catch (Exception e) {
            status = Component.translatable(configKey("importExport.import.failed"), e.getMessage());
        }
    }
}
