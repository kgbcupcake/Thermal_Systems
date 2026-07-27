package com.marie.thermalsystems.integration.enderio;

import com.marie.thermalsystems.ThermalSystemsMod;
import dev.marie.framework.ui.ForeignScreenDetector;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.component.Constraint;
import dev.marie.framework.ui.component.MarieComponent;
import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Bounds;
import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only companion to {@link EnderIOIntegration}: renders a
 * {@link HeatCoolToggleComponent} over Ender IO's Stirling Generator screen,
 * replacing {@code /thermal enderio setMode} as the primary control surface
 * for that generator (the command remains as a fallback - see
 * {@link EnderIOIntegration#onRegisterCommands}).
 *
 * <p>Only ever loaded on the physical client - see
 * {@link com.marie.thermalsystems.ThermalSystemsClient}, which calls
 * {@link #init(IEventBus)} from its {@code Dist.CLIENT}-only {@code @Mod}
 * constructor, guarded by the same {@code ModList.isLoaded(ENDERIO_MOD_ID)}
 * check every other Ender IO entry point in this mod uses.
 *
 * <p>Detects which generator a screen belongs to via
 * {@link ForeignScreenDetector#registerInterest}, matched purely by the
 * {@code enderio:stirling_generator} {@code MenuType}'s registry name - zero
 * compile-time dependency on Ender IO, same as {@link EnderIOIntegration}.
 * Ender IO's menu exposes no generic (non-Ender-IO-typed) way to read back
 * which block position it belongs to, so the position is instead recovered
 * by remembering the last block the player right-clicked
 * ({@link #onRightClickBlock}) and verifying, once the matching screen
 * opens, that the block currently at that position really is
 * {@code enderio:stirling_generator} - confirmed to share that registry name
 * with the block entity type and menu type by disassembling the Ender IO
 * 8.2.11-beta jar (blockstate/loot-table paths, {@code EIOMenus.STIRLING_GENERATOR}
 * registration), matching {@link EnderIOIntegration}'s own verification
 * approach for its capability wiring.
 *
 * <p>As soon as a generator is confirmed, {@link #onScreenOpened} sends an
 * {@link EnderIOModeRequestPayload} for its current {@link ThermalMode} and
 * creates a {@link HeatCoolToggleComponent} that starts in the component's
 * own "loading" state; {@link #onModeResponse} applies the reply once it
 * arrives, re-checking the tracked position still matches in case the
 * player switched screens in the meantime.
 */
public final class EnderIOClientIntegration {

    static final ResourceLocation STIRLING_GENERATOR_MENU =
            ResourceLocation.fromNamespaceAndPath(EnderIOIntegration.ENDERIO_MOD_ID, "stirling_generator");

    /** Persistence key for the toggle's {@link ComponentState} - stable across sessions/worlds. */
    private static final String TOGGLE_PERSISTENCE_ID = "thermalsystems.enderio_mode_toggle";

    /**
     * Offsets below are relative to the panel's own {@code getGuiLeft()}/{@code getGuiTop()}, read
     * off {@code StirlingGeneratorScreen.init()} by disassembling the Ender IO 8.2.11-beta jar
     * (same verification approach as {@link EnderIOIntegration}'s class javadoc) rather than eyeballed
     * from a screenshot - the panel itself is a fixed 176x166 layout (Ender IO never scales its
     * content to the container's rendered size), so these stay correct at any GUI scale/window size:
     * <ul>
     *   <li>{@code ProgressWidget.BottomUp} (the fuel/burn-progress "smoke" gauge) is added at
     *       {@code x=leftPos+80, y=topPos+52, w=14, h=14} - right edge at 94.
     *   <li>{@code IOConfigButton} (the gear, the lower of the two top-right buttons) is added at
     *       {@code x=leftPos+imageWidth-6-16, y=topPos+24, w=16, h=16} - since {@code imageWidth}
     *       is always 176 for this screen, that's x=154; bottom edge at 40.
     *   <li>{@code RedstoneControlPickerWidget} (the disable/redstone-mode circle above the gear)
     *       sits at the same x=154, y=topPos+6, w=16, h=16 - bottom edge 22, already clear of 40.
     *   <li>{@code ActivityWidget} (the hourglass) is added at x=154, y=topPos+64, w=16, h=16 - the
     *       toggle's x-range never reaches column 154, so it can never collide with this vertically
     *       either.
     * </ul>
     * That leaves an empty x:[94,154] * y:[40,~83] pocket - right of the fuel gauge, below the
     * disable/gear buttons, above where the player-inventory grid begins - matching the "Here" box
     * the toggle was asked to move into.
     */
    private static final int FUEL_GAUGE_RIGHT_OFFSET = 94;
    private static final int TOP_BUTTONS_BOTTOM_OFFSET = 40;

    /** Small breathing room below the gear button so the toggle doesn't touch it. */
    private static final int DEFAULT_Y_GAP = 4;

    private static final int DEFAULT_X_OFFSET = FUEL_GAUGE_RIGHT_OFFSET;
    private static final int DEFAULT_Y_OFFSET = TOP_BUTTONS_BOTTOM_OFFSET + DEFAULT_Y_GAP;

    private static BlockPos lastInteractedPos;
    private static Screen trackedScreen;
    private static HeatCoolToggleComponent toggle;

    /**
     * Drag gesture tracker for repositioning {@link #toggle}, live only while
     * {@link #repositionModeActive} is true. (Re)constructed alongside {@link #toggle} in
     * {@link #onScreenOpened} - not per-frame, since a drag gesture spans many frames - because
     * {@link DraggableResizable}'s constructor takes its target directly rather than a supplier,
     * and no {@link HeatCoolToggleComponent} exists yet at class-load time. {@link #toggleBounds}
     * still calls {@link DraggableResizable#setParentBounds} with fresh panel geometry every call,
     * in case the window is resized mid-drag.
     */
    private static DraggableResizable positionDrag;

    /**
     * Whether the player has toggled {@link EnderIOKeys#EDIT_TOGGLE_POSITION} on for the currently
     * tracked screen - lets {@link #toggle} be dragged instead of clicked to change HEAT/COOL.
     * Reset whenever the tracked screen closes so a fresh generator always opens in the normal
     * (non-repositioning) state.
     */
    private static boolean repositionModeActive;

    private EnderIOClientIntegration() {
    }

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, EnderIOClientIntegration::onRegisterPayloadHandlers);
        modEventBus.addListener(RegisterKeyMappingsEvent.class, EnderIOKeys::onRegisterKeyMappings);
        ForeignScreenDetector.registerInterest(STIRLING_GENERATOR_MENU, EnderIOClientIntegration::onScreenOpened);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, EnderIOClientIntegration::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, EnderIOClientIntegration::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, EnderIOClientIntegration::onRender);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, EnderIOClientIntegration::onMouseClick);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseDragged.Pre.class, EnderIOClientIntegration::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonReleased.Pre.class, EnderIOClientIntegration::onMouseReleased);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, EnderIOClientIntegration::onKeyPressed);
    }

    /**
     * Registers {@link EnderIOModeResponsePayload} (server-to-client only -
     * the client-to-server request leg, {@link EnderIOModeRequestPayload},
     * is registered separately by {@link EnderIOIntegration}, which is the
     * only side that ever needs to receive it).
     */
    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ThermalSystemsMod.MOD_ID).versioned("1");
        registrar.playToClient(EnderIOModeResponsePayload.TYPE, EnderIOModeResponsePayload.STREAM_CODEC,
                EnderIOClientIntegration::onModeResponse);
    }

    /**
     * Applies a {@link EnderIOModeResponsePayload} to {@link #toggle} - but
     * only if it's still tracking the same position the response is for.
     * The player could otherwise have opened a different generator's screen
     * (or closed it) between the request going out and this reply arriving.
     */
    private static void onModeResponse(EnderIOModeResponsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            HeatCoolToggleComponent current = toggle;
            if (current == null || !current.pos().equals(payload.pos())) {
                return;
            }
            ThermalMode mode;
            try {
                mode = ThermalMode.valueOf(payload.mode());
            } catch (IllegalArgumentException e) {
                return;
            }
            current.applyServerMode(mode);
        });
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            lastInteractedPos = event.getPos().immutable();
        }
    }

    private static void onScreenOpened(Screen screen) {
        BlockPos pos = lastInteractedPos;
        if (pos == null || !isStirlingGeneratorBlock(pos)) {
            return;
        }
        trackedScreen = screen;
        toggle = new HeatCoolToggleComponent(pos);
        positionDrag = new DraggableResizable(toggle,
                Constraint.fixed(HeatCoolToggleComponent.WIDTH, HeatCoolToggleComponent.HEIGHT),
                EnderIOClientIntegration::onDragCommitted);
        repositionModeActive = false;
        EnderIOModeRequestPayload.sendToServer(pos);
    }

    private static boolean isStirlingGeneratorBlock(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        return STIRLING_GENERATOR_MENU.equals(blockId);
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() == trackedScreen) {
            trackedScreen = null;
            toggle = null;
            repositionModeActive = false;
        }
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        if (toggle == null || event.getScreen() != trackedScreen) {
            return;
        }
        Bounds panelBounds = panelBounds(event.getScreen());
        if (panelBounds == null) {
            return;
        }
        Bounds bounds = toggleBounds(panelBounds, event.getMouseX(), event.getMouseY());
        RenderContext context = new GuiGraphicsRenderContext(
                event.getGuiGraphics(), Minecraft.getInstance(), Theme.DARK, event.getPartialTick());
        toggle.render(context, bounds);
        if (repositionModeActive) {
            context.drawDashedBorder(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    context.theme().color(ThemeKey.EDIT_BANNER_BACKGROUND));
        }
    }

    /** The vanilla panel's own screen-space rectangle, generically off {@code AbstractContainerScreen}. */
    @Nullable
    private static Bounds panelBounds(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return null;
        }
        return new Bounds(containerScreen.getGuiLeft(), containerScreen.getGuiTop(),
                containerScreen.getXSize(), containerScreen.getYSize());
    }

    /**
     * Resolves the toggle's current on-screen {@link Bounds}: the persisted
     * {@link ComponentState}'s (x, y) - relative to {@code panelBounds}'s own origin, so it stays
     * correctly anchored if the panel itself ever moves or the window resizes - if the player has
     * dragged it before, else {@link #DEFAULT_X_OFFSET}/{@link #DEFAULT_Y_OFFSET}'s computed
     * default. Recomputed from {@code panelBounds} fresh on every call (never cached), matching the
     * previous bottom-left-anchor behavior this replaces, so a saved position never goes stale if
     * the panel's own geometry changes.
     *
     * <p>While {@link #repositionModeActive} and a drag gesture is in progress, returns
     * {@link #positionDrag}'s live preview instead - {@code mx}/{@code my} are the current mouse
     * position from {@code ScreenEvent.Render.Post}, the only per-frame source of live mouse
     * coordinates available for a screen this integration doesn't own.
     */
    private static Bounds toggleBounds(Bounds panelBounds, int mx, int my) {
        positionDrag.setParentBounds(panelBounds);
        if (positionDrag.isDragging() || positionDrag.isResizing()) {
            Bounds preview = positionDrag.mouseDragged(mx, my);
            if (preview != null) {
                return preview;
            }
        }
        PersistenceProvider persistence = EnderIOUiPersistence.get();
        return persistence.load(TOGGLE_PERSISTENCE_ID)
                .map(state -> new Bounds(panelBounds.x() + state.x(), panelBounds.y() + state.y(),
                        HeatCoolToggleComponent.WIDTH, HeatCoolToggleComponent.HEIGHT))
                .orElseGet(() -> new Bounds(panelBounds.x() + DEFAULT_X_OFFSET, panelBounds.y() + DEFAULT_Y_OFFSET,
                        HeatCoolToggleComponent.WIDTH, HeatCoolToggleComponent.HEIGHT));
    }

    /**
     * Persists a completed drag as a {@link ComponentState} relative to the tracked screen's own
     * panel origin - not absolute screen coordinates - keyed by {@link #TOGGLE_PERSISTENCE_ID}, so
     * it stays correctly anchored if the panel itself is ever a different size or position (a
     * different GUI scale, a different generator's panel that isn't identical, etc.).
     */
    private static void onDragCommitted(MarieComponent target, Bounds committedBounds) {
        if (trackedScreen == null) {
            return;
        }
        Bounds panelBounds = panelBounds(trackedScreen);
        if (panelBounds == null) {
            return;
        }
        int relX = committedBounds.x() - panelBounds.x();
        int relY = committedBounds.y() - panelBounds.y();
        EnderIOUiPersistence.get().save(TOGGLE_PERSISTENCE_ID, new ComponentState(
                relX, relY, HeatCoolToggleComponent.WIDTH, HeatCoolToggleComponent.HEIGHT,
                false, false, false, 0));
    }

    private static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (toggle == null || event.getScreen() != trackedScreen) {
            return;
        }
        Bounds panelBounds = panelBounds(event.getScreen());
        if (panelBounds == null) {
            return;
        }
        int mx = (int) event.getMouseX();
        int my = (int) event.getMouseY();
        Bounds bounds = toggleBounds(panelBounds, mx, my);
        if (repositionModeActive) {
            if (positionDrag.mouseClicked(mx, my, bounds)) {
                event.setCanceled(true);
            }
            return;
        }
        if (toggle.mouseClicked(bounds, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    private static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (toggle == null || event.getScreen() != trackedScreen || !repositionModeActive) {
            return;
        }
        if (positionDrag.isDragging() || positionDrag.isResizing()) {
            event.setCanceled(true);
        }
    }

    private static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (toggle == null || event.getScreen() != trackedScreen || !repositionModeActive) {
            return;
        }
        if (positionDrag.isDragging() || positionDrag.isResizing()) {
            positionDrag.mouseReleased((int) event.getMouseX(), (int) event.getMouseY());
            event.setCanceled(true);
        }
    }

    /**
     * Toggles {@link #repositionModeActive} on {@link EnderIOKeys#EDIT_TOGGLE_POSITION} while the
     * tracked Stirling Generator screen is open - the same "press a key to enter/exit a marie-ui
     * edit mode" convention {@code NourishedKeys}/{@code EditModeController} use elsewhere, but
     * without replacing the screen the way {@link dev.marie.framework.ui.edit.EditModeController}
     * does: that would close Ender IO's own container menu, hiding the very panel geometry
     * (fuel gauge, buttons, inventory) the player needs to see to reposition the toggle relative
     * to it. Instead this flips a local flag and keeps rendering the real screen underneath.
     */
    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (toggle == null || event.getScreen() != trackedScreen) {
            return;
        }
        if (EnderIOKeys.EDIT_TOGGLE_POSITION.matches(event.getKeyCode(), event.getScanCode())) {
            repositionModeActive = !repositionModeActive;
            event.setCanceled(true);
        }
    }
}
