package net.windmilltown.reed.debugblock;

import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Access to debug blocks and their callbacks.
 *
 * <p>All methods must be called from the server thread.</p>
 */
public interface DebugBlockService {

    /**
     * Gets a debug block at a Bukkit block.
     *
     * @param block the block to inspect
     * @return the debug block, if the block is backed by a jigsaw block entity
     */
    @NotNull Optional<DebugBlock> getDebugBlock(@NotNull Block block);

    /**
     * Registers a synchronous listener.
     *
     * @param plugin owning plugin
     * @param listener listener
     * @return registration handle
     */
    @NotNull DebugBlockListenerRegistration registerListener(@NotNull Plugin plugin, @NotNull DebugBlockListener listener);
}
