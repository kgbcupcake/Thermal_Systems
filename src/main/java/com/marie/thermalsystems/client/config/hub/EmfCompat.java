package com.marie.thermalsystems.client.config.hub;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Optional, reflection-only bridge to Entity Model Features' public {@code EMFAnimationApi}, so this
 * mod has no compile-time or hard runtime dependency on it.
 *
 * <p>EMF animation packs (e.g. Detailed Animations) override the player's head rotation with their own
 * formula but leave the hat layer on vanilla's, so the two separate whenever the head turns. Locking
 * the player to the vanilla model for the duration of a single preview draw keeps them aligned without
 * touching how the player looks anywhere else.
 */
final class EmfCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmfCompat.class);
    private static final String MOD_ID = "entity_model_features";
    private static final String API_CLASS = "traben.entity_model_features.EMFAnimationApi";

    private static boolean resolved;
    private static Method emfEntityOf;
    private static Method lock;
    private static Method unlock;

    private EmfCompat() {
    }

    /** Runs {@code draw} with {@code entity} forced to its vanilla model when EMF is present, else just runs it. */
    static void withVanillaModel(Entity entity, Runnable draw) {
        Object emfEntity = emfEntity(entity);
        if (emfEntity == null) {
            draw.run();
            return;
        }
        invoke(lock, emfEntity);
        try {
            draw.run();
        } finally {
            invoke(unlock, emfEntity);
        }
    }

    private static Object emfEntity(Entity entity) {
        if (!resolve()) {
            return null;
        }
        try {
            return emfEntityOf.invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return null;
        }
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            if (ModList.get().isLoaded(MOD_ID)) {
                try {
                    Class<?> api = Class.forName(API_CLASS);
                    emfEntityOf = api.getMethod("emfEntityOf", Entity.class);
                    Class<?> emfEntityType = emfEntityOf.getReturnType();
                    lock = api.getMethod("lockEntityToVanillaModel", emfEntityType);
                    unlock = api.getMethod("unlockEntityToVanillaModel", emfEntityType);
                } catch (ReflectiveOperationException | LinkageError e) {
                    disable(e);
                }
            }
        }
        return emfEntityOf != null;
    }

    private static void invoke(Method method, Object emfEntity) {
        try {
            method.invoke(null, emfEntity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
        }
    }

    private static void disable(Throwable cause) {
        LOGGER.warn("[MTS] Entity Model Features API unavailable; the config Home page preview will use EMF's model", cause);
        emfEntityOf = null;
    }
}
