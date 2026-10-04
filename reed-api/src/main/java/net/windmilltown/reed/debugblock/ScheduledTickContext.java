package net.windmilltown.reed.debugblock;

/**
 * Metadata of the scheduled tick currently being executed.
 *
 * @param priority numeric scheduled-tick priority
 * @param subTickOrder stable order among scheduled ticks
 */
public record ScheduledTickContext(int priority, long subTickOrder) {
}
