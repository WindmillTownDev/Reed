package net.windmilltown.reed.debugblock;

/**
 * Result of a player using a debug block.
 */
public enum DebugBlockInteractionResult {
    /**
     * Continue dispatching and retain the block's default result.
     */
    PASS,

    /**
     * Consume the interaction and stop listener dispatch.
     */
    SUCCESS
}
