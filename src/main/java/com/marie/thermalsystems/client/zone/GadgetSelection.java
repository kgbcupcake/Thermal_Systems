package com.marie.thermalsystems.client.zone;

import com.marie.thermalsystems.controller.ClimateController;
import com.marie.thermalsystems.item.GadgetMode;
import com.marie.thermalsystems.item.ZoneGadgetItem;
import com.marie.thermalsystems.network.ZoneInfo;
import com.marie.thermalsystems.network.ZonePayloads;
import com.marie.thermalsystems.registry.ThermalRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A gadget box as the client sees it, shared by the wireframe preview, the HUD readout, the gadget
 * menu and the zone editor so they all agree. While only corner one is set, the box runs to the
 * block under the crosshair ({@code preview} is then true). {@code problem} is a
 * {@code message.thermalsystems.zone.*} key for the first reason the server would reject the box,
 * or {@code null} when it looks fine - a hint only; the server re-checks everything.
 */
public record GadgetSelection(GadgetMode mode, BlockPos first, BlockPos second, boolean preview,
                              @Nullable UUID editing, long volume, @Nullable String problem,
                              @Nullable String overlapName) {

    /** The selection for {@code gadget}, or {@code null} when it has no corner one in a selecting mode. */
    @Nullable
    public static GadgetSelection of(ItemStack gadget, ZonePayloads.ZoneList zones) {
        GadgetMode mode = ZoneGadgetItem.mode(gadget);
        if (mode != GadgetMode.CREATE && mode != GadgetMode.EDIT) {
            return null;
        }
        BlockPos first = gadget.get(ThermalRegistries.CORNER_ONE.get());
        if (first == null) {
            return null;
        }
        BlockPos second = gadget.get(ThermalRegistries.CORNER_TWO.get());
        boolean preview = second == null;
        if (preview) {
            HitResult hit = Minecraft.getInstance().hitResult;
            second = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                    ? blockHit.getBlockPos()
                    : first;
        }
        return forBox(mode, first, second, preview, gadget.get(ThermalRegistries.EDIT_TARGET.get()), zones);
    }

    /** Checks an arbitrary box, such as the one being adjusted in the zone editor. */
    public static GadgetSelection forBox(GadgetMode mode, BlockPos first, BlockPos second, boolean preview,
                                         @Nullable UUID editing, ZonePayloads.ZoneList zones) {
        long volume = ClimateController.volume(first, second);
        String problem = null;
        String overlapName = null;
        Player player = Minecraft.getInstance().player;
        boolean limited = zones.maxVolume() > 0;
        if (limited && volume > zones.maxVolume()) {
            problem = "too_big";
        } else if (limited && player != null && ClimateController.distanceToBox(player.getX(), player.getY(), player.getZ(),
                first, second) > ClimateController.MAX_EDIT_DISTANCE) {
            problem = "too_far";
        } else {
            ZoneInfo overlap = firstOverlap(zones, first, second, editing);
            if (overlap != null) {
                problem = "overlap";
                overlapName = overlap.name();
            }
        }
        return new GadgetSelection(mode, first, second, preview, editing, volume, problem, overlapName);
    }

    public int sizeX() {
        return Math.abs(first.getX() - second.getX()) + 1;
    }

    public int sizeY() {
        return Math.abs(first.getY() - second.getY()) + 1;
    }

    public int sizeZ() {
        return Math.abs(first.getZ() - second.getZ()) + 1;
    }

    @Nullable
    private static ZoneInfo firstOverlap(ZonePayloads.ZoneList zones, BlockPos a, BlockPos b, @Nullable UUID exclude) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());
        for (ZoneInfo zone : zones.zones()) {
            if (!zone.hasBounds() || zone.id().equals(exclude)) {
                continue;
            }
            if (minX <= zone.max().getX() && maxX >= zone.min().getX()
                    && minY <= zone.max().getY() && maxY >= zone.min().getY()
                    && minZ <= zone.max().getZ() && maxZ >= zone.min().getZ()) {
                return zone;
            }
        }
        return null;
    }
}
