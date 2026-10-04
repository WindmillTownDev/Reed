package net.windmilltown.reed;

import java.util.ServiceLoader;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for APIs provided by Reed.
 */
public final class Reed {

    private Reed() {
    }

    /**
     * Gets the API implementation provided by the running Reed server.
     *
     * @return the Reed server API
     * @throws IllegalStateException if the API is used outside a Reed server
     */
    public static @NotNull ReedServer server() {
        return Holder.SERVER;
    }

    private static ReedServer loadServer() {
        return ServiceLoader.load(ReedServer.class, Reed.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No Reed server implementation is available"));
    }

    private static final class Holder {
        private static final ReedServer SERVER = loadServer();

        private Holder() {
        }
    }
}
