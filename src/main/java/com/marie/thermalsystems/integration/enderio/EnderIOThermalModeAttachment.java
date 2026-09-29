package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Persists a Stirling Generator's {@link ThermalMode} on its own
 * {@link BlockEntity} via a NeoForge data attachment, replacing the removed
 * {@code EnderIOAdapterModeRegistry} - mode now survives a server restart the
 * same way any other block entity state does.
 */
public final class EnderIOThermalModeAttachment {

    public static final String ATTACHMENT_ID = "enderio_thermal_mode";

    public static Supplier<AttachmentType<ThermalMode>> THERMAL_MODE;

    private EnderIOThermalModeAttachment() {
    }

    public static void register(IEventBus modEventBus) {
        DeferredRegister<AttachmentType<?>> attachmentTypes =
                DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ThermalSystemsMod.MOD_ID);
        THERMAL_MODE = attachmentTypes.register(ATTACHMENT_ID, () ->
                AttachmentType.builder(() -> ThermalMode.HEAT)
                        .serialize(ThermalMode.CODEC)
                        .build()
        );
        attachmentTypes.register(modEventBus);
    }

    static ThermalMode get(BlockEntity blockEntity) {
        return blockEntity.getData(THERMAL_MODE.get());
    }

    static void set(BlockEntity blockEntity, ThermalMode mode) {
        blockEntity.setData(THERMAL_MODE.get(), mode);
    }
}
