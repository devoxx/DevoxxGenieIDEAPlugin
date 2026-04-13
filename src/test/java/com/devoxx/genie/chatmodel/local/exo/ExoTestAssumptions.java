package com.devoxx.genie.chatmodel.local.exo;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Test helper for Exo integration tests.
 *
 * <p>Several Exo tests use Mockito to stub HTTP calls but exercise multi-step flows that have
 * since drifted from the production code, so they only pass against a real Exo server running on
 * the default port. Use {@link #isExoServerRunning()} together with {@code Assumptions.assumeTrue}
 * to skip these tests on machines without a local Exo instance.
 */
public final class ExoTestAssumptions {

    private static final String EXO_HOST = "localhost";
    private static final int EXO_PORT = 52415;
    private static final int CONNECT_TIMEOUT_MS = 300;

    private ExoTestAssumptions() {
    }

    /**
     * @return {@code true} when an Exo server can be reached on {@code localhost:52415}.
     */
    public static boolean isExoServerRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(EXO_HOST, EXO_PORT), CONNECT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
