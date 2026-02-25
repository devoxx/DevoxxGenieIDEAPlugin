package com.devoxx.genie.service.acp.model;

/**
 * Declares the capabilities that this ACP client supports.
 *
 * <p>Sent as part of the {@code initialize} handshake to inform the agent
 * which local operations (filesystem access, terminal) the client can handle.
 */
public class ClientCapabilities {
    public FileSystemCapability fs;
    public Boolean terminal;

    /**
     * Returns a capabilities object with all features enabled.
     *
     * @return capabilities with full filesystem and terminal support
     */
    public static ClientCapabilities full() {
        ClientCapabilities caps = new ClientCapabilities();
        caps.fs = new FileSystemCapability(true, true);
        caps.terminal = true;
        return caps;
    }

    /** Describes which filesystem operations the client supports. */
    public static class FileSystemCapability {
        public boolean readTextFile;
        public boolean writeTextFile;

        public FileSystemCapability(boolean readTextFile, boolean writeTextFile) {
            this.readTextFile = readTextFile;
            this.writeTextFile = writeTextFile;
        }
    }
}
