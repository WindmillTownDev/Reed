package net.windmilltown.reed.debugblock;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Synchronous listener for debug block activity.
 *
 * <p>Callbacks run on the server thread in registration order. Implementations must not
 * retain a callback ordering assumption across separate server ticks.</p>
 */
public interface DebugBlockListener {

    /**
     * Called when a debug block receives an update-shape notification.
     *
     * @param update update details
     */
    default void onUpdateShape(@NotNull DebugBlockUpdate update) {
    }

    /**
     * Called when a debug block receives a neighbor-change notification.
     *
     * @param update update details
     */
    default void onNeighborChanged(@NotNull DebugBlockUpdate update) {
    }

    /**
     * Called after the stored power state and block orientation have changed.
     *
     * @param change power transition details
     */
    default void onPowerChange(@NotNull DebugBlockPowerChange change) {
    }

    /**
     * Called after a player placement has completed.
     *
     * @param player the placing player, or {@code null} for a non-player context
     * @param block the placed debug block
     */
    default void onPlaced(@Nullable Player player, @NotNull DebugBlock block) {
    }

    /**
     * Called when a player uses a debug block.
     *
     * @param player the player
     * @param block the debug block
     * @return whether the interaction was consumed
     */
    default @NotNull DebugBlockInteractionResult onUse(@NotNull Player player, @NotNull DebugBlock block) {
        return DebugBlockInteractionResult.PASS;
    }
}
