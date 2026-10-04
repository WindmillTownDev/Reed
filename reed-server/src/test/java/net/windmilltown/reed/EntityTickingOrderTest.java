package net.windmilltown.reed;

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

class EntityTickingOrderTest {

    @Test
    void chunksAssignAndRetainEntitySequences() throws IOException {
        final ClassNode entity = classNode("net/minecraft/world/entity/Entity");
        assertTrue(entity.fields.stream().anyMatch(field ->
            field.name.equals("reed$entityTickingSequence") && field.desc.equals("J")
        ));

        final List<AbstractInsnNode> addEntity = instructions(
            "ca/spottedleaf/moonrise/patches/chunk_system/level/entity/ChunkEntitySlices",
            "addEntity",
            "(Lnet/minecraft/world/entity/Entity;IZ)Z"
        );
        assertTrue(hasMethodCall(addEntity, "net/minecraft/world/entity/Entity", "reed$getEntityTickingSequence"));
        assertTrue(hasMethodCall(addEntity, "net/minecraft/world/entity/Entity", "reed$setEntityTickingSequence"));
    }

    @Test
    void persistenceAndStatusTransitionsUseLiveConfigurationAndSequenceSorting() throws IOException {
        final List<AbstractInsnNode> save = instructions(
            "ca/spottedleaf/moonrise/patches/chunk_system/level/entity/ChunkEntitySlices",
            "save",
            "()Lnet/minecraft/nbt/CompoundTag;"
        );
        assertConfigurationRead(save);
        assertTrue(hasMethodCall(save, "java/util/List", "sort"));

        final List<AbstractInsnNode> updateStatus = instructions(
            "ca/spottedleaf/moonrise/patches/chunk_system/level/entity/ChunkEntitySlices",
            "updateStatus",
            "(Lnet/minecraft/server/level/FullChunkStatus;Lca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup;)V"
        );
        assertConfigurationRead(updateStatus);
        assertTrue(hasMethodCall(updateStatus, "java/util/Arrays", "copyOf"));
        assertTrue(hasMethodCall(updateStatus, "java/util/Arrays", "sort"));
    }

    @Test
    void entityLookupOnlyRequestsSequenceRetentionForSectionMoves() throws IOException {
        final List<AbstractInsnNode> moveEntity = instructions(
            "ca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup",
            "moveEntity",
            "(Lnet/minecraft/world/entity/Entity;)Lca/spottedleaf/moonrise/patches/chunk_system/level/entity/ChunkEntitySlices;"
        );

        assertTrue(moveEntity.stream().anyMatch(instruction ->
            instruction instanceof MethodInsnNode method
                && method.owner.equals("ca/spottedleaf/moonrise/patches/chunk_system/level/entity/ChunkEntitySlices")
                && method.name.equals("addEntity")
                && method.desc.equals("(Lnet/minecraft/world/entity/Entity;IZ)Z")
        ));
    }

    private static void assertConfigurationRead(final List<AbstractInsnNode> instructions) {
        assertTrue(instructions.stream().anyMatch(instruction ->
            instruction instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.GETFIELD
                && field.owner.equals("net/windmilltown/reed/configuration/ReedConfiguration")
                && field.name.equals("paperFix")
        ));
        assertTrue(instructions.stream().anyMatch(instruction ->
            instruction instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.GETFIELD
                && field.name.equals("useVanillaEntityTickingOrder")
        ));
    }

    private static List<AbstractInsnNode> instructions(
        final String className,
        final String methodName,
        final String descriptor
    ) throws IOException {
        final MethodNode method = classNode(className).methods.stream()
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

    private static ClassNode classNode(final String className) throws IOException {
        final String resource = className + ".class";
        try (InputStream input = EntityTickingOrderTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing class resource " + resource);
            }
            final ClassNode classNode = new ClassNode();
            new ClassReader(input).accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return classNode;
        }
    }

    private static boolean hasMethodCall(final List<AbstractInsnNode> instructions, final String owner, final String name) {
        return instructions.stream()
            .anyMatch(instruction -> instruction instanceof MethodInsnNode method && method.owner.equals(owner) && method.name.equals(name));
    }
}
