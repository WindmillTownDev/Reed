package net.windmilltown.reed.debugblock;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Tick context captured at a debug block callback.
 *
 * @param gameTime world game time
 * @param phase current server tick phase
 * @param scheduledTick current scheduled tick, if any
 */
public record ServerTickContext(
    long gameTime,
    @NotNull ServerTickPhase phase,
    @NotNull Optional<ScheduledTickContext> scheduledTick
) {

    /**
     * Creates a tick context.
     */
    public ServerTickContext {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        if (scheduledTick == null) {
            throw new NullPointerException("scheduledTick");
        }
    }
}
