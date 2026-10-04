package net.windmilltown.reed.debugblock;

/**
 * Server phase in which a debug block callback occurred.
 */
public enum ServerTickPhase {
    OUTSIDE_WORLD_TICK,
    SCHEDULED_BLOCK_TICKS,
    SCHEDULED_FLUID_TICKS,
    BLOCK_EVENTS,
    ENTITY_TICKS,
    BLOCK_ENTITY_TICKS,
    NETWORK
}
