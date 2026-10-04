package net.windmilltown.reed;

import net.windmilltown.reed.debugblock.DebugBlockService;
import net.windmilltown.reed.debugblock.DebugBlockServiceImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Server-side implementation of the Reed API entry point.
 */
public final class ReedServerImpl implements ReedServer {
    private final DebugBlockServiceImpl debugBlockService = new DebugBlockServiceImpl();

    public ReedServerImpl() {
    }

    @Override
    public @NotNull DebugBlockService debugBlocks() {
        return this.debugBlockService;
    }
}
