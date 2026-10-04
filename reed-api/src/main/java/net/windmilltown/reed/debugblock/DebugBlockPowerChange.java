package net.windmilltown.reed.debugblock;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;

/**
 * A stored power-state transition.
 *
 * @param debugBlock target debug block
 * @param oldPowered previous power state
 * @param newPowered new power state
 * @param cause notification path that caused the transition
 * @param source actual source block when known
 * @param sourceType source block type reported by Minecraft
 * @param tickContext server tick context
 * @param sequence callback sequence within the world game tick
 */
public record DebugBlockPowerChange(
    @NotNull DebugBlock debugBlock,
    boolean oldPowered,
    boolean newPowered,
    @NotNull DebugBlockUpdateType cause,
    @NotNull Optional<Block> source,
    @NotNull BlockType sourceType,
    @NotNull ServerTickContext tickContext,
    long sequence
) {

    /**
     * Creates a power change.
     */
    public DebugBlockPowerChange {
        if (debugBlock == null) {
            throw new NullPointerException("debugBlock");
        }
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (sourceType == null) {
            throw new NullPointerException("sourceType");
        }
        if (tickContext == null) {
            throw new NullPointerException("tickContext");
        }
    }
}
