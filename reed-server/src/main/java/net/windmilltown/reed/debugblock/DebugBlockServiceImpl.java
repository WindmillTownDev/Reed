package net.windmilltown.reed.debugblock;

import io.papermc.paper.util.MCUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Server implementation of the debug block service.
 */
public final class DebugBlockServiceImpl implements DebugBlockService {
    private final List<Registration> registrations = new ArrayList<>();
    private final IdentityHashMap<ServerLevel, SequenceState> sequences = new IdentityHashMap<>();

    @Override
    public @NotNull Optional<DebugBlock> getDebugBlock(final @NotNull Block block) {
        requireServerThread();
        Objects.requireNonNull(block, "block");
        if (!(block.getWorld() instanceof CraftWorld craftWorld)) {
            return Optional.empty();
        }
        final BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
        if (craftWorld.getHandle().getBlockEntity(pos) instanceof JigsawBlockEntity jigsawBlockEntity) {
            return Optional.of(jigsawBlockEntity.reed$getDebugBlock());
        }
        return Optional.empty();
    }

    @Override
    public @NotNull DebugBlockListenerRegistration registerListener(
        final @NotNull Plugin plugin,
        final @NotNull DebugBlockListener listener
    ) {
        requireServerThread();
        final Registration registration = new Registration(Objects.requireNonNull(plugin, "plugin"), Objects.requireNonNull(listener, "listener"));
        this.registrations.add(registration);
        return registration;
    }

    void reportUpdateShape(
        final JigsawBlockEntity blockEntity,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        final DebugBlockUpdate update = this.createUpdate(blockEntity, level, sourcePos, sourceBlock);
        this.dispatch(listener -> listener.onUpdateShape(update));
    }

    void reportNeighborChange(
        final JigsawBlockEntity blockEntity,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        final DebugBlockUpdate update = this.createUpdate(blockEntity, level, sourcePos, sourceBlock);
        this.dispatch(listener -> listener.onNeighborChanged(update));
    }

    void reportPowerChange(
        final JigsawBlockEntity blockEntity,
        final boolean oldPowered,
        final boolean newPowered,
        final DebugBlockUpdateType cause,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        final Source source = source(level, sourcePos, sourceBlock);
        final DebugBlockPowerChange change = new DebugBlockPowerChange(
            blockEntity.reed$getDebugBlock(),
            oldPowered,
            newPowered,
            cause,
            source.block(),
            source.material(),
            ReedDebugBlockHooks.tickContext(level),
            this.nextSequence(level)
        );
        this.dispatch(listener -> listener.onPowerChange(change));
    }

    void reportPlaced(final @Nullable Player player, final JigsawBlockEntity blockEntity) {
        this.dispatch(listener -> listener.onPlaced(player, blockEntity.reed$getDebugBlock()));
    }

    boolean reportUse(final Player player, final JigsawBlockEntity blockEntity) {
        for (Registration registration : List.copyOf(this.registrations)) {
            if (!registration.registered || !registration.plugin.isEnabled()) {
                continue;
            }
            try {
                if (registration.listener.onUse(player, blockEntity.reed$getDebugBlock()) == DebugBlockInteractionResult.SUCCESS) {
                    return true;
                }
            } catch (RuntimeException exception) {
                registration.plugin.getLogger().severe("Debug block listener failed in onUse: " + exception);
            }
        }
        return false;
    }

    private DebugBlockUpdate createUpdate(
        final JigsawBlockEntity blockEntity,
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        final Source source = source(level, sourcePos, sourceBlock);
        return new DebugBlockUpdate(
            blockEntity.reed$getDebugBlock(),
            source.block(),
            source.material(),
            ReedDebugBlockHooks.tickContext(level),
            this.nextSequence(level)
        );
    }

    private static Source source(
        final ServerLevel level,
        final @Nullable BlockPos sourcePos,
        final net.minecraft.world.level.block.Block sourceBlock
    ) {
        final Optional<Block> block = sourcePos == null ? Optional.empty() : Optional.of(CraftBlock.at(level, sourcePos));
        final BlockType blockType = CraftBlockType.minecraftToBukkitNew(sourceBlock);
        return new Source(block, blockType);
    }

    private long nextSequence(final ServerLevel level) {
        final long gameTime = level.getGameTime();
        final SequenceState state = this.sequences.computeIfAbsent(level, ignored -> new SequenceState(gameTime));
        if (state.gameTime != gameTime) {
            state.gameTime = gameTime;
            state.next = 0;
        }
        return state.next++;
    }

    private void dispatch(final ListenerCall call) {
        for (Registration registration : List.copyOf(this.registrations)) {
            if (!registration.registered || !registration.plugin.isEnabled()) {
                continue;
            }
            try {
                call.accept(registration.listener);
            } catch (RuntimeException exception) {
                registration.plugin.getLogger().severe("Debug block listener callback failed: " + exception);
            }
        }
    }

    private static void requireServerThread() {
        if (!MCUtil.isMainThread()) {
            throw new IllegalStateException("Debug block API must be used from the server thread");
        }
    }

    private final class Registration implements DebugBlockListenerRegistration {
        private final Plugin plugin;
        private final DebugBlockListener listener;
        private boolean registered = true;

        private Registration(final Plugin plugin, final DebugBlockListener listener) {
            this.plugin = plugin;
            this.listener = listener;
        }

        @Override
        public boolean isRegistered() {
            return this.registered;
        }

        @Override
        public void close() {
            requireServerThread();
            if (this.registered) {
                this.registered = false;
                DebugBlockServiceImpl.this.registrations.remove(this);
            }
        }
    }

    @FunctionalInterface
    private interface ListenerCall {
        void accept(DebugBlockListener listener);
    }

    private record Source(Optional<Block> block, BlockType material) {
    }

    private static final class SequenceState {
        private long gameTime;
        private long next;

        private SequenceState(final long gameTime) {
            this.gameTime = gameTime;
        }
    }
}
