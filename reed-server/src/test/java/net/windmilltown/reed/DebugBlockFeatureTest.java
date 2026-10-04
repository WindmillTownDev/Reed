package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.windmilltown.reed.debugblock.DebugBlockSettings;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class DebugBlockFeatureTest {

    @Test
    void settingsPreserveStrongAnalogOutputAndRejectNegativeValues() {
        final DebugBlockSettings settings = new DebugBlockSettings(true, false, false, true, false, true, false, 32);
        assertEquals(32, settings.analogOutputStrength());
        assertThrows(
            IllegalArgumentException.class,
            () -> new DebugBlockSettings(true, false, false, true, false, true, false, -1)
        );
    }

    @Test
    void neighborCallbackPrecedesResponseAndPowerCallbackFollowsStateWrite() throws IOException {
        final List<AbstractInsnNode> neighborChanged = instructions(
            "net/minecraft/world/level/block/JigsawBlock.class",
            "neighborChanged"
        );
        assertBefore(neighborChanged, "reportNeighborChange", "reed$updateState");

        final List<AbstractInsnNode> updateState = instructions(
            "net/minecraft/world/level/block/JigsawBlock.class",
            "reed$updateState"
        );
        assertBefore(updateState, "reed$setPowered", "setBlock");
        assertBefore(updateState, "setBlock", "reportPowerChange");
    }

    @Test
    void packetHandlingSetsAndRestoresNetworkPhase() throws IOException {
        final List<AbstractInsnNode> handle = instructions(
            "net/minecraft/network/PacketProcessor$ListenerAndPacket.class",
            "handle"
        );
        final List<Integer> phaseChanges = methodIndices(handle, "setTickPhase");
        final int packetHandle = methodIndex(handle, "handle");
        assertTrue(phaseChanges.size() >= 2);
        assertTrue(phaseChanges.getFirst() < packetHandle, "NETWORK must be set before the packet handler runs");
        assertTrue(packetHandle < phaseChanges.getLast(), "the previous phase must be restored after packet handling");
    }

    @Test
    void persistenceUsesNamespacedSnakeCaseKeys() throws IOException {
        final List<AbstractInsnNode> save = instructions(
            "net/minecraft/world/level/block/entity/JigsawBlockEntity$ReedDebugBlock.class",
            "save"
        );
        assertTrue(hasString(save, "report_neighbor_change"));
        assertTrue(hasString(save, "respond_to_neighbor_change"));
        assertTrue(hasString(save, "analog_output_strength"));
        assertTrue(hasString(save, "owner"));
        assertTrue(hasString(save, "name"));

        final List<AbstractInsnNode> entitySave = instructions(
            "net/minecraft/world/level/block/entity/JigsawBlockEntity.class",
            "saveAdditional"
        );
        assertTrue(hasString(entitySave, "reed"));
        assertTrue(hasString(entitySave, "debug_block"));
    }

    private static List<AbstractInsnNode> instructions(final String className, final String methodName) throws IOException {
        try (InputStream input = DebugBlockFeatureTest.class.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(input);
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

    private static void assertBefore(final List<AbstractInsnNode> instructions, final String firstName, final String secondName) {
        final int first = methodIndex(instructions, firstName);
        final int second = methodIndex(instructions, secondName);
        assertTrue(first >= 0, () -> "Missing method call: " + firstName);
        assertTrue(second >= 0, () -> "Missing method call: " + secondName);
        assertTrue(first < second, () -> firstName + " must precede " + secondName);
    }

    private static int methodIndex(final List<AbstractInsnNode> instructions, final String name) {
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof MethodInsnNode method && method.name.equals(name)) {
                return index;
            }
        }
        return -1;
    }

    private static List<Integer> methodIndices(final List<AbstractInsnNode> instructions, final String name) {
        final List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < instructions.size(); index++) {
            if (instructions.get(index) instanceof MethodInsnNode method && method.name.equals(name)) {
                indices.add(index);
            }
        }
        return indices;
    }

    private static boolean hasString(final List<AbstractInsnNode> instructions, final String value) {
        return instructions.stream()
            .filter(LdcInsnNode.class::isInstance)
            .map(LdcInsnNode.class::cast)
            .anyMatch(instruction -> value.equals(instruction.cst));
    }
}
