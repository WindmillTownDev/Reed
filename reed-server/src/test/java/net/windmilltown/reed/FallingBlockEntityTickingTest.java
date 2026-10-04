package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class FallingBlockEntityTickingTest {

    private static final String CLASS_NAME = "net/minecraft/world/entity/item/FallingBlockEntity";

    @Test
    void sourceChangeEventRemainsBeforeEntityAddition() throws IOException {
        final List<AbstractInsnNode> instructions = instructions("fall");
        final int event = methodIndex(
            instructions,
            "org/bukkit/craftbukkit/event/CraftEventFactory",
            "callEntityChangeBlockEvent"
        );
        final int configuration = methodIndex(
            instructions,
            "net/windmilltown/reed/configuration/ReedConfiguration",
            "get"
        );
        final int setBlock = methodIndex(instructions, "net/minecraft/world/level/Level", "setBlockAndUpdate");
        final int addEntity = methodIndex(instructions, "net/minecraft/world/level/Level", "addFreshEntity");

        assertEquals(1, methodCount(instructions, "org/bukkit/craftbukkit/event/CraftEventFactory", "callEntityChangeBlockEvent"));
        assertTrue(event < configuration);
        assertTrue(configuration < setBlock);
        assertTrue(setBlock < addEntity);
    }

    @Test
    void sourceRemovalRunsBeforeFallingEntityMovement() throws IOException {
        final List<AbstractInsnNode> instructions = instructions("tick");
        final int startPosition = methodIndex(instructions, CLASS_NAME, "getStartPos");
        final int removeBlock = methodIndex(instructions, "net/minecraft/world/level/Level", "removeBlock");
        final int timeUpdate = fieldIndex(instructions, CLASS_NAME, "time", Opcodes.PUTFIELD);
        final int gravity = methodIndex(instructions, CLASS_NAME, "applyGravity");
        final int move = methodIndex(instructions, CLASS_NAME, "move");

        assertTrue(startPosition < removeBlock);
        assertTrue(removeBlock < timeUpdate);
        assertTrue(timeUpdate < gravity);
        assertTrue(gravity < move);
    }

    private static List<AbstractInsnNode> instructions(final String methodName) throws IOException {
        final String resource = CLASS_NAME + ".class";
        try (InputStream input = FallingBlockEntityTickingTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing class resource " + resource);
            }
            final ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            final MethodNode method = classNode.methods.stream()
                .filter(candidate -> candidate.name.equals(methodName))
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

    private static long methodCount(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        return instructions.stream()
            .filter(instruction -> instruction instanceof MethodInsnNode method && method.owner.equals(owner) && method.name.equals(name))
            .count();
    }

    private static int fieldIndex(final List<AbstractInsnNode> instructions, final String owner, final String name, final int opcode) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof FieldInsnNode field
                && field.owner.equals(owner)
                && field.name.equals(name)
                && field.getOpcode() == opcode) {
                return index;
            }
        }
        throw new AssertionError("Missing field access " + owner + "." + name);
    }
}
