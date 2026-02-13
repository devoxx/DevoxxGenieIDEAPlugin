package com.devoxx.genie.service.acp.model;

public class ClientCapabilities {
    public FileSystemCapability fs;
    public Boolean terminal;

    public static ClientCapabilities full() {
        ClientCapabilities caps = new ClientCapabilities();
        caps.fs = new FileSystemCapability(true, true);
        caps.terminal = true;
        return caps;
    }

    public static class FileSystemCapability {
        public boolean readTextFile;
        public boolean writeTextFile;

        public FileSystemCapability() {}

        public FileSystemCapability(boolean readTextFile, boolean writeTextFile) {
            this.readTextFile = readTextFile;
            this.writeTextFile = writeTextFile;
        }
    }
}
