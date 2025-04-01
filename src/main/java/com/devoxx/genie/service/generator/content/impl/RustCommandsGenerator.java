package com.devoxx.genie.service.generator.content.impl;

import com.devoxx.genie.service.generator.content.BuildCommandsGenerator;

/**
 * Generates Rust-specific build commands.
 */
public class RustCommandsGenerator implements BuildCommandsGenerator {
    
    @Override
    public void addBuildCommands(StringBuilder contentBuilder) {
        contentBuilder.append("- **Build:** `cargo build`\n");
        contentBuilder.append("- **Run:** `cargo run`\n");
        contentBuilder.append("- **Test:** `cargo test`\n");
        contentBuilder.append("- **Single Test:** `cargo test test_name`\n");
        contentBuilder.append("- **Release Build:** `cargo build --release`\n\n");
    }
}
