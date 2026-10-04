package net.windmilltown.reed.configuration;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Loads the single global Reed configuration file.
 */
public final class ReedConfigurations {

    public static final String FILE_NAME = "reed.yml";
    private static final Logger LOGGER = LogUtils.getClassLogger();
    private static final String HEADER = """
        This is the global configuration file for Reed.
        Options in this file apply to every world.
        Redstone and entity options can materially change gameplay,
        so validate changes against the server's behavior tests.
        """;

    private final Path configFile;

    public ReedConfigurations(final Path configDirectory) {
        this.configFile = Objects.requireNonNull(configDirectory, "configDirectory").resolve(FILE_NAME);
    }

    /**
     * Loads the configuration during server startup.
     *
     * @return the loaded configuration
     * @throws ConfigurateException if the configuration cannot be loaded
     */
    public ReedConfiguration initialize() throws ConfigurateException {
        final ReedConfiguration configuration = this.load();
        ReedConfiguration.set(configuration);
        return configuration;
    }

    /**
     * Transactionally reloads the configuration.
     *
     * <p>The active instance is replaced only after the whole file has loaded,
     * transformed, validated, and saved successfully.</p>
     *
     * @return the newly loaded configuration
     * @throws ConfigurateException if the configuration cannot be reloaded
     */
    public ReedConfiguration reload() throws ConfigurateException {
        final ReedConfiguration configuration = this.load();
        ReedConfiguration.set(configuration);
        return configuration;
    }

    public Path configFile() {
        return this.configFile;
    }

    private ReedConfiguration load() throws ConfigurateException {
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .headerMode(HeaderMode.PRESET)
            .defaultOptions(options -> options
                .header(HEADER)
                .mapFactory(MapFactories.sortedNatural()))
            .path(this.configFile)
            .build();

        final ConfigurationNode node;
        if (Files.notExists(this.configFile)) {
            node = CommentedConfigurationNode.root(loader.defaultOptions());
            node.node("version").set(ReedConfiguration.CURRENT_VERSION);
        } else {
            node = loader.load();
            this.verifyVersion(node);
        }

        this.applyTransformations(node);

        final ReedConfiguration configuration = node.require(ReedConfiguration.class);
        node.set(ReedConfiguration.class, configuration);
        loader.save(node);
        LOGGER.info("Loaded Reed configuration from {}", this.configFile);
        return configuration;
    }

    private void verifyVersion(final ConfigurationNode node) throws ConfigurateException {
        final ConfigurationNode versionNode = node.node("version");
        if (versionNode.virtual()) {
            LOGGER.warn("Reed configuration has no version; assuming version {}", ReedConfiguration.CURRENT_VERSION);
            versionNode.set(ReedConfiguration.CURRENT_VERSION);
            return;
        }

        final int version = versionNode.getInt();
        if (version > ReedConfiguration.CURRENT_VERSION) {
            throw new ConfigurateException(
                "Cannot load Reed configuration version " + version
                    + "; this server supports up to version " + ReedConfiguration.CURRENT_VERSION
            );
        }
    }

    private void applyTransformations(final ConfigurationNode node) throws ConfigurateException {
        final ConfigurationTransformation.VersionedBuilder builder = ConfigurationTransformation.versionedBuilder()
            .addVersion(1, ConfigurationTransformation.builder().build());
        // Add future versioned Reed configuration transformations after version 1.
        builder.build().apply(node);
    }
}
