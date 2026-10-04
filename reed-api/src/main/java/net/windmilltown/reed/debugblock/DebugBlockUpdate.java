package net.windmilltown.reed.debugblock;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.jetbrains.annotations.NotNull;

/**
 * A block notification received by a debug block.
 *
 * @param debugBlock target debug block
 * @param source actual source block when known
 * @param sourceType source block type reported by Minecraft
 * @param tickContext server tick context
 * @param sequence callback sequence within the world game tick
 */
public record DebugBlockUpdate(
    @NotNull DebugBlock debugBlock,
    @NotNull Optional<Block> source,
    @NotNull BlockType sourceType,
    @NotNull ServerTickContext tickContext,
    long sequence
) {

    /**
     * Creates an update.
     */
    public DebugBlockUpdate {
        if (debugBlock == null) {
            throw new NullPointerException("debugBlock");
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
