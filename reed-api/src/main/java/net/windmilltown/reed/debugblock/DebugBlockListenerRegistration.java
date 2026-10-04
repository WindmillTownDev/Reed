package net.windmilltown.reed.debugblock;

/**
 * A debug block listener registration.
 */
public interface DebugBlockListenerRegistration extends AutoCloseable {

    /**
     * Gets whether the listener is still registered.
     *
     * @return whether the registration is active
     */
    boolean isRegistered();

    /**
     * Unregisters the listener. Repeated calls have no effect.
     */
    @Override
    void close();
}
