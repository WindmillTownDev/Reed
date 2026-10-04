package net.windmilltown.reed.debugblock;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * A debug block backed by a jigsaw block entity.
 *
 * <p>All methods must be called from the server thread.</p>
 */
public interface DebugBlock {

    /**
     * Gets the block represented by this object.
     *
     * @return the block
     */
    @NotNull Block getBlock();

    /**
     * Gets the current behavior settings.
     *
     * @return the settings
     */
    @NotNull DebugBlockSettings getSettings();

    /**
     * Replaces the current behavior settings.
     *
     * @param settings the new settings
     */
    void setSettings(@NotNull DebugBlockSettings settings);

    /**
     * Gets the last computed power state.
     *
     * @return whether the block is powered
     */
    boolean isPowered();

    /**
     * Gets the optional owner metadata.
     *
     * @return the owner
     */
    @NotNull Optional<UUID> getOwner();

    /**
     * Sets the owner metadata.
     *
     * @param owner the owner
     */
    void setOwner(@NotNull UUID owner);

    /**
     * Removes the owner metadata.
     */
    void clearOwner();

    /**
     * Gets the optional name metadata.
     *
     * @return the name
     */
    @NotNull Optional<String> getName();

    /**
     * Sets the name metadata.
     *
     * @param name the name
     */
    void setName(@NotNull String name);

    /**
     * Removes the name metadata.
     */
    void clearName();
}
