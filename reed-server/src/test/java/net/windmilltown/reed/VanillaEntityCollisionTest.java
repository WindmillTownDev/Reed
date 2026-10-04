package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class VanillaEntityCollisionTest {

    @Test
    void vanillaShapeCollisionClipsBothDirections() {
        final AABB moving = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        final VoxelShape positiveObstacle = Shapes.box(2.0, 0.0, 0.0, 3.0, 1.0, 1.0);
        final VoxelShape negativeObstacle = Shapes.box(-2.0, 0.0, 0.0, -1.0, 1.0, 1.0);

        assertEquals(1.0, Shapes.collideVanilla(Direction.Axis.X, moving, List.of(positiveObstacle), 5.0));
        assertEquals(-1.0, Shapes.collideVanilla(Direction.Axis.X, moving, List.of(negativeObstacle), -5.0));
    }

    @Test
    void entityCollisionSwitchRunsBeforePaperCollisionCode() throws IOException {
        final List<AbstractInsnNode> instructions = instructions(
            "net/minecraft/world/entity/Entity",
            "collide",
            "(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
        );

        assertConfigurationSwitch(instructions, "useVanillaEntityCollision");
        final int vanillaCall = methodIndex(instructions, "net/minecraft/world/entity/Entity", "collideVanilla");
        final int paperCollision = methodIndex(
            instructions,
            "ca/spottedleaf/moonrise/patches/collisions/CollisionUtil",
            "getEntityHardCollisions"
        );
        assertTrue(vanillaCall < paperCollision);
    }

    @Test
    void entityCandidatesSwitchBeforeMoonriseCollection() throws IOException {
        final List<AbstractInsnNode> instructions = instructions(
            "net/minecraft/world/level/EntityGetter",
            "getEntityCollisions",
            "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        );

        assertConfigurationSwitch(instructions, "useVanillaEntityCollision");
        final int vanillaInflate = methodIndex(instructions, "net/minecraft/world/phys/AABB", "inflate");
        final int moonriseEmptyCheck = methodIndex(
            instructions,
            "ca/spottedleaf/moonrise/patches/collisions/CollisionUtil",
            "isEmpty"
        );
        assertTrue(vanillaInflate < moonriseEmptyCheck);
    }

    @Test
    void vanillaShapePathUsesMojangAxisCollision() throws IOException {
        final List<AbstractInsnNode> instructions = instructions(
            "net/minecraft/world/phys/shapes/Shapes",
            "collideVanilla",
            "(Lnet/minecraft/core/Direction$Axis;Lnet/minecraft/world/phys/AABB;Ljava/lang/Iterable;D)D"
        );

        assertTrue(hasMethodCall(instructions, "net/minecraft/world/phys/shapes/VoxelShape", "collideX"));
        assertFalse(hasMethodCall(instructions, "net/minecraft/world/phys/shapes/VoxelShape", "collide"));
    }

    private static void assertConfigurationSwitch(final List<AbstractInsnNode> instructions, final String fieldName) {
        final MethodInsnNode getConfiguration = (MethodInsnNode)instructions.get(0);
        assertEquals(Opcodes.INVOKESTATIC, getConfiguration.getOpcode());
        assertEquals("net/windmilltown/reed/configuration/ReedConfiguration", getConfiguration.owner);
        assertEquals("get", getConfiguration.name);

        final FieldInsnNode section = (FieldInsnNode)instructions.get(1);
        assertEquals("paperFix", section.name);
        final FieldInsnNode setting = (FieldInsnNode)instructions.get(2);
        assertEquals(fieldName, setting.name);
        assertEquals(Opcodes.IFEQ, instructions.get(3).getOpcode());
    }

    private static List<AbstractInsnNode> instructions(
        final String className,
        final String methodName,
        final String descriptor
    ) throws IOException {
        final String resource = className + ".class";
        try (InputStream input = VanillaEntityCollisionTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing class resource " + resource);
            }
            final ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            final MethodNode method = classNode.methods.stream()
                .filter(candidate -> candidate.name.equals(methodName) && candidate.desc.equals(descriptor))
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

    private static int methodIndex(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof MethodInsnNode method && method.owner.equals(owner) && method.name.equals(name)) {
                return index;
            }
        }
        throw new AssertionError("Missing method call " + owner + "." + name);
    }

    private static boolean hasMethodCall(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        return instructions.stream()
            .anyMatch(instruction -> instruction instanceof MethodInsnNode method && method.owner.equals(owner) && method.name.equals(name));
    }
}
