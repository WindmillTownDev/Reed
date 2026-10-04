package net.windmilltown.reed;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class ReedBootstrapTest {

    @Test
    void loadsServerProvider() {
        assertInstanceOf(ReedServerImpl.class, Reed.server());
    }
}
