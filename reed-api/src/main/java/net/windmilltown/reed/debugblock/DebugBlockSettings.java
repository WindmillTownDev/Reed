package net.windmilltown.reed.debugblock;

/**
 * Behavior settings for a debug block.
 *
 * @param enabled whether the block processes debug behavior
 * @param reportUpdateShape whether update-shape notifications are reported
 * @param reportNeighborChange whether neighbor-change notifications are reported
 * @param reportPowerChange whether power transitions are reported
 * @param respondToUpdateShape whether update-shape notifications recompute power
 * @param respondToNeighborChange whether neighbor-change notifications recompute power
 * @param quasiConnectivity whether power above the block is also sampled
 * @param analogOutputStrength comparator output strength; values greater than 15 are preserved
 */
public record DebugBlockSettings(
    boolean enabled,
    boolean reportUpdateShape,
    boolean reportNeighborChange,
    boolean reportPowerChange,
    boolean respondToUpdateShape,
    boolean respondToNeighborChange,
    boolean quasiConnectivity,
    int analogOutputStrength
) {

    public static final DebugBlockSettings DEFAULT = new DebugBlockSettings(false, false, false, true, false, true, false, 0);

    /**
     * Creates settings.
     */
    public DebugBlockSettings {
        if (analogOutputStrength < 0) {
            throw new IllegalArgumentException("analogOutputStrength must not be negative");
        }
    }
}
