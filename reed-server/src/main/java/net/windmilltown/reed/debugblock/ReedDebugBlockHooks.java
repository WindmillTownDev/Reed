package net.windmilltown.reed.debugblock;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.windmilltown.reed.Reed;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Internal bridge between Minecraft execution paths and the Reed API implementation.
 */
public final class ReedDebugBlockHooks {
    private static final ThreadLocal<TickState> TICK_STATE = ThreadLocal.withInitial(TickState::new);

    private ReedDebugBlockHooks() {
    }

    public static ServerTickPhase setTickPhase(final ServerTickPhase phase) {
        if (!net.windmilltown.reed.configuration.ReedConfiguration.get().features.debugBlocks) {
            return ServerTickPhase.OUTSIDE_WORLD_TICK;
        }
        final TickState state = TICK_STATE.get();
        final ServerTickPhase previous = state.phase;
        state.phase = phase;
        return previous;
    }

    public static void beginScheduledTick(final int priority, final long subTickOrder) {
        final TickState state = TICK_STATE.get();
        state.hasScheduledTick = true;
        state.priority = priority;
        state.subTickOrder = subTickOrder;
    }

    public static void endScheduledTick() {
        TICK_STATE.get().hasScheduledTick = false;
    }

    public static ServerTickContext tickContext(final ServerLevel level) {
        final TickState state = TICK_STATE.get();
        final Optional<ScheduledTickContext> scheduledTick = state.hasScheduledTick
            ? Optional.of(new ScheduledTickContext(state.priority, state.subTickOrder))
            : Optional.empty();
        return new ServerTickContext(level.getGameTime(), state.phase, scheduledTick);
    }

    public static void reportUpdateShape(
        final JigsawBlockEntity blockEntity,
        final ServerLevel level,
        final BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        service().reportUpdateShape(blockEntity, level, sourcePos, sourceBlock);
    }

    public static void reportNeighborChange(
        final JigsawBlockEntity blockEntity,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        service().reportNeighborChange(blockEntity, level, sourcePos, sourceBlock);
    }

    public static void reportPowerChange(
        final JigsawBlockEntity blockEntity,
        final boolean oldPowered,
        final boolean newPowered,
        final DebugBlockUpdateType cause,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        service().reportPowerChange(blockEntity, oldPowered, newPowered, cause, level, sourcePos, sourceBlock);
    }

    public static void reportPlaced(final @Nullable Player player, final JigsawBlockEntity blockEntity) {
        service().reportPlaced(player, blockEntity);
    }

    public static boolean reportUse(final Player player, final JigsawBlockEntity blockEntity) {
        return service().reportUse(player, blockEntity);
    }

    private static DebugBlockServiceImpl service() {
        return (DebugBlockServiceImpl) Reed.server().debugBlocks();
    }

    private static final class TickState {
        private ServerTickPhase phase = ServerTickPhase.OUTSIDE_WORLD_TICK;
        private boolean hasScheduledTick;
        private int priority;
        private long subTickOrder;
    }
}
