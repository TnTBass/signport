package tech.endorsed.signport.neoforge.permission;

import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.junit.jupiter.api.Test;
import tech.endorsed.signport.SignPort;
import tech.endorsed.signport.permission.SignPortPermissionPolicy;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class NeoForgeSignPortPermissionsTest {
    @Test
    void neoForgePermissionNodesMirrorCommonPolicy() {
        List<PermissionNode<Boolean>> nodes = NeoForgeSignPortPermissions.nodes();

        assertEquals(SignPortPermissionPolicy.defaultsByNode().size(), nodes.size());
        assertIterableEquals(SignPortPermissionPolicy.defaultsByNode().keySet(),
                nodes.stream().map(PermissionNode::getNodeName).toList());
        nodes.forEach(node -> {
            assertSame(PermissionTypes.BOOLEAN, node.getType());
            assertEquals(SignPort.MOD_ID, node.getNodeName().substring(0, node.getNodeName().indexOf('.')));
        });
    }

    @Test
    void registerNodesAddsEveryPolicyNodeToNeoForgeGatherEvent() {
        PermissionGatherEvent.Nodes event = new PermissionGatherEvent.Nodes();

        NeoForgeSignPortPermissions.registerNodes(event);

        assertEquals(nodeNames(NeoForgeSignPortPermissions.nodes()), nodeNames(event.getNodes()));
    }

    @Test
    void nodePathRejectsPermissionNodesOutsideSignPortNamespace() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> NeoForgeSignPortPermissions.nodePath("other.anchor.create"));

        assertEquals("Permission node 'other.anchor.create' must start with 'signport.'", exception.getMessage());
    }

    private static Set<String> nodeNames(Iterable<? extends PermissionNode<?>> nodes) {
        Set<String> names = new LinkedHashSet<>();
        for (PermissionNode<?> node : nodes) {
            names.add(node.getNodeName());
        }
        return names;
    }
}
