package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Trident;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class TridentAutoDespawnTest {

    @Test
    void apiExposesPositiveAutoDespawnState() throws NoSuchMethodException {
        assertEquals(boolean.class, Trident.class.getMethod("isAutoDespawn").getReturnType());
        assertEquals(void.class, Trident.class.getMethod("setAutoDespawn", boolean.class).getReturnType());
    }

    @Test
    void tickDespawnChecksOverrideBeforeVanillaState() throws IOException {
        final List<AbstractInsnNode> instructions = methodInstructions("tickDespawn", "()V");
        final MethodInsnNode stateCall = (MethodInsnNode) instructions.get(1);
        assertEquals(Opcodes.INVOKEVIRTUAL, stateCall.getOpcode());
        assertEquals("net/minecraft/world/entity/projectile/arrow/ThrownTrident", stateCall.owner);
        assertEquals("isAutoDespawn", stateCall.name);
        assertEquals(Opcodes.IFNE, instructions.get(2).getOpcode());
        assertEquals(Opcodes.RETURN, instructions.get(3).getOpcode());
    }

    @Test
    void stateUsesNamespacedMojangCustomDataAndConfigurationFallback() throws IOException {
        final List<AbstractInsnNode> getter = methodInstructions("isAutoDespawn", "()Z");
        assertTrue(hasField(getter, "net/minecraft/core/component/DataComponents", "CUSTOM_DATA"));
        assertTrue(hasString(getter, "reed"));
        assertTrue(hasString(getter, "auto_despawn"));
        assertTrue(hasField(getter, "net/windmilltown/reed/configuration/ReedConfiguration$Features", "tridentAutoDespawn"));

        final List<AbstractInsnNode> setter = methodInstructions("setAutoDespawn", "(Z)V");
        assertTrue(hasField(setter, "net/minecraft/core/component/DataComponents", "CUSTOM_DATA"));
        assertTrue(hasMethod(setter, "net/minecraft/world/item/component/CustomData", "update"));
        assertTrue(hasMethodNamed(setter, "setComponent"));
    }

    private static List<AbstractInsnNode> methodInstructions(final String name, final String descriptor) throws IOException {
        final String className = "net/minecraft/world/entity/projectile/arrow/ThrownTrident.class";
        try (InputStream input = TridentAutoDespawnTest.class.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(input);
            final ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            final MethodNode method = classNode.methods.stream()
                .filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor))
                .findFirst()
                .orElseThrow();
            final List<AbstractInsnNode> instructions = new ArrayList<>();
            for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction.getOpcode() >= 0) {
                    instructions.add(instruction);
                }
            }
            return instructions;
        }
    }

    private static boolean hasField(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        return instructions.stream()
            .filter(FieldInsnNode.class::isInstance)
            .map(FieldInsnNode.class::cast)
            .anyMatch(field -> field.owner.equals(owner) && field.name.equals(name));
    }

    private static boolean hasMethod(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        return instructions.stream()
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(method -> method.owner.equals(owner) && method.name.equals(name));
    }

    private static boolean hasMethodNamed(final List<AbstractInsnNode> instructions, final String name) {
        return instructions.stream()
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(method -> method.name.equals(name));
    }

    private static boolean hasString(final List<AbstractInsnNode> instructions, final String value) {
        return instructions.stream()
            .filter(LdcInsnNode.class::isInstance)
            .map(LdcInsnNode.class::cast)
            .anyMatch(instruction -> value.equals(instruction.cst));
    }
}
