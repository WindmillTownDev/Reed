package net.windmilltown.reed.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.configurate.ConfigurateException;

class ReedConfigurationsTest {

    @TempDir
    Path configDirectory;

    @Test
    void alwaysTickEntitiesShortCircuitsBeforeEntityAccess() throws IOException {
        final String className = "io/papermc/paper/entity/activation/ActivationRange.class";
        try (InputStream input = ReedConfigurationsTest.class.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(input);
            final ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            final MethodNode method = classNode.methods.stream()
                .filter(candidate -> candidate.name.equals("checkIfActive"))
                .findFirst()
                .orElseThrow();
            final List<AbstractInsnNode> instructions = new ArrayList<>();
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction.getOpcode() >= 0) {
                    instructions.add(instruction);
                }
            }

            final MethodInsnNode getConfiguration = (MethodInsnNode) instructions.get(0);
            assertEquals(Opcodes.INVOKESTATIC, getConfiguration.getOpcode());
            assertEquals("net/windmilltown/reed/configuration/ReedConfiguration", getConfiguration.owner);
            assertEquals("get", getConfiguration.name);

            final FieldInsnNode paperFix = (FieldInsnNode) instructions.get(1);
            assertEquals(Opcodes.GETFIELD, paperFix.getOpcode());
            assertEquals("paperFix", paperFix.name);

            final FieldInsnNode alwaysTickEntities = (FieldInsnNode) instructions.get(2);
            assertEquals(Opcodes.GETFIELD, alwaysTickEntities.getOpcode());
            assertEquals("alwaysTickEntities", alwaysTickEntities.name);

            assertEquals(Opcodes.IFEQ, instructions.get(3).getOpcode());
            assertEquals(Opcodes.ICONST_1, instructions.get(4).getOpcode());
            assertEquals(Opcodes.IRETURN, instructions.get(5).getOpcode());
        }
    }

    @Test
    void createsGlobalConfigurationWithDefaults() throws IOException {
        final ReedConfigurations configurations = new ReedConfigurations(this.configDirectory);
        final ReedConfiguration configuration = configurations.initialize();

        assertSame(configuration, ReedConfiguration.get());
        assertFalse(configuration.gameplay.legacyFallingBlockEntityTicking);
        assertFalse(configuration.paperFix.alwaysTickEntities);
        assertFalse(configuration.paperFix.useVanillaEntityCollision);
        assertFalse(configuration.paperFix.useVanillaEntityTickingOrder);
        assertTrue(configuration.features.tridentAutoDespawn);
        assertFalse(configuration.features.debugBlocks);

        final String serialized = Files.readString(configurations.configFile());
        assertTrue(serialized.contains("version: 1"));
        assertTrue(serialized.contains("legacy-falling-block-entity-ticking: false"));
        assertTrue(serialized.contains("paper-fix:"));
        assertTrue(serialized.contains("always-tick-entities: false"));
        assertTrue(serialized.contains("use-vanilla-entity-collision: false"));
        assertTrue(serialized.contains("use-vanilla-entity-ticking-order: false"));
        assertTrue(serialized.contains("trident-auto-despawn: true"));
    }

    @Test
    void reloadReplacesConfigurationOnlyAfterSuccessfulLoad() throws IOException {
        final ReedConfigurations configurations = new ReedConfigurations(this.configDirectory);
        final ReedConfiguration original = configurations.initialize();

        Files.writeString(configurations.configFile(), """
            version: 1
            gameplay:
              legacy-falling-block-entity-ticking: true
            paper-fix:
              use-vanilla-entity-ticking-order: true
            """);
        final ReedConfiguration reloaded = configurations.reload();

        assertTrue(reloaded.gameplay.legacyFallingBlockEntityTicking);
        assertTrue(reloaded.paperFix.useVanillaEntityTickingOrder);
        assertSame(reloaded, ReedConfiguration.get());

        Files.writeString(configurations.configFile(), "version: 2\n");
        assertThrows(ConfigurateException.class, configurations::reload);
        assertSame(reloaded, ReedConfiguration.get());
        assertNotSame(original, reloaded);
    }
}
