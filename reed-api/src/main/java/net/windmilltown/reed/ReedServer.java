package net.windmilltown.reed;

import net.windmilltown.reed.debugblock.DebugBlockService;
import org.jetbrains.annotations.NotNull;

/**
 * Root of the API implemented by a running Reed server.
 */
public interface ReedServer {

    /**
     * Gets the debug block service.
     *
     * @return the debug block service
     */
    @NotNull DebugBlockService debugBlocks();
}
