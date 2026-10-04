package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

class PlayerNoClipFeatureTest {

    @Test
    void playerApiExposesNoClipPreference() throws NoSuchMethodException {
        assertNotNull(Player.class.getMethod("isNoClip"));
        assertNotNull(Player.class.getMethod("setNoClip", boolean.class));
    }

    @Test
    void activeNoClipControlsMovementPoseAndPistonReaction() throws IOException {
        final String playerClass = "net/minecraft/world/entity/player/Player.class";
        assertTrue(calls(instructions(playerClass, "tick"), "reed$updateNoPhysics"));
        assertTrue(calls(instructions(playerClass, "updatePlayerPose"), "reed$isNoClipActive"));

        final List<AbstractInsnNode> pistonReaction = instructions(playerClass, "getPistonPushReaction");
        assertTrue(calls(pistonReaction, "reed$isNoClipActive"));
        assertTrue(readsField(pistonReaction, "IGNORE_ENTITY"));
    }

    @Test
    void flightChangesRefreshNoPhysicsImmediately() throws IOException {
        assertTrue(calls(
            instructions("net/minecraft/server/network/ServerGamePacketListenerImpl.class", "handlePlayerAbilities"),
            "reed$updateNoPhysics"
        ));
        assertTrue(calls(
            instructions("org/bukkit/craftbukkit/entity/CraftPlayer.class", "setFlying"),
            "reed$updateNoPhysics"
        ));
    }

    private static List<AbstractInsnNode> instructions(final String className, final String methodName) throws IOException {
        try (InputStream input = PlayerNoClipFeatureTest.class.getClassLoader().getResourceAsStream(className)) {
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

    private static boolean calls(final List<AbstractInsnNode> instructions, final String methodName) {
        return instructions.stream()
            .filter(MethodInsnNode.class::isInstance)
            .map(MethodInsnNode.class::cast)
            .anyMatch(instruction -> instruction.name.equals(methodName));
    }

    private static boolean readsField(final List<AbstractInsnNode> instructions, final String fieldName) {
        return instructions.stream()
            .filter(FieldInsnNode.class::isInstance)
            .map(FieldInsnNode.class::cast)
            .anyMatch(instruction -> instruction.name.equals(fieldName));
    }
}
