package net.windmilltown.reed.configuration;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * The global Reed configuration.
 */
@ConfigSerializable
public final class ReedConfiguration {

    public static final int CURRENT_VERSION = 1;
    private static volatile ReedConfiguration instance;

    @Setting("version")
    public int version = CURRENT_VERSION;

    @Setting("gameplay")
    public Gameplay gameplay = new Gameplay();

    @Setting("paper-fix")
    public PaperFix paperFix = new PaperFix();

    @Setting("features")
    public Features features = new Features();

    /**
     * Gets the currently loaded global configuration.
     *
     * @return the current configuration
     * @throws IllegalStateException if Reed configuration has not been initialized
     */
    public static ReedConfiguration get() {
        final ReedConfiguration configuration = instance;
        if (configuration == null) {
            throw new IllegalStateException("Reed configuration has not been initialized");
        }
        return configuration;
    }

    static void set(final ReedConfiguration configuration) {
        instance = configuration;
    }

    @ConfigSerializable
    public static final class Gameplay {

        @Setting("legacy-falling-block-entity-ticking")
        @Comment("Keeps a gravity block at its source until the falling block entity's first tick.")
        public boolean legacyFallingBlockEntityTicking = false;
    }

    @ConfigSerializable
    public static final class PaperFix {

        @Setting("always-tick-entities")
        @Comment("Bypasses Paper's Entity Activation Range for entities already eligible to tick, forcing their full tick path instead of inactiveTick.")
        public boolean alwaysTickEntities = false;

        @Setting("use-vanilla-entity-collision")
        @Comment("Uses Mojang's entity movement and collision resolution instead of Paper's optimized path.")
        public boolean useVanillaEntityCollision = false;

        @Setting("use-vanilla-entity-ticking-order")
        @Comment("Preserves stable per-chunk entity ticking order across chunk state changes and persistence.")
        public boolean useVanillaEntityTickingOrder = false;
    }

    @ConfigSerializable
    public static final class Features {

        @Setting("trident-auto-despawn")
        @Comment("Controls whether tridents without an explicit entity override naturally despawn.")
        public boolean tridentAutoDespawn = true;

        @Setting("debug-blocks")
        @Comment("Enables Jigsaw-backed debug blocks and their API callbacks.")
        public boolean debugBlocks = false;
    }
}
