package com.devoxx.genie.service.generator.content.impl;

import com.devoxx.genie.service.generator.content.LanguageStyleGenerator;

/**
 * Generates Rust-specific code style guidelines.
 */
public class RustStyleGenerator implements LanguageStyleGenerator {
    
    @Override
    public void addCodeStyle(StringBuilder contentBuilder) {
        contentBuilder.append("- **Formatting:** `cargo fmt`\n");
        contentBuilder.append("- **Linting:** `cargo clippy`\n");
        contentBuilder.append("- **Documentation:** Use `///` for documentation comments\n");
        contentBuilder.append("- **Naming:** Follow Rust naming conventions:\n");
        contentBuilder.append("  - snake_case for variables and functions\n");
        contentBuilder.append("  - CamelCase for types and traits\n");
        contentBuilder.append("  - SCREAMING_SNAKE_CASE for constants\n\n");
    }
}
