package com.devoxx.genie.service.generator.content.impl;

import com.devoxx.genie.service.generator.content.LanguageStyleGenerator;

/**
 * Generates Java-specific code style guidelines.
 */
public class JavaStyleGenerator implements LanguageStyleGenerator {
    
    @Override
    public void addCodeStyle(StringBuilder contentBuilder) {
        contentBuilder.append("- **Formatting:** Use IDE or checkstyle for formatting\n");
        contentBuilder.append("- **Naming:**\n");
        contentBuilder.append("  - Use camelCase for variables, methods, and fields\n");
        contentBuilder.append("  - Use PascalCase for classes and interfaces\n");
        contentBuilder.append("  - Use SCREAMING_SNAKE_CASE for constants\n");
        contentBuilder.append("- **Documentation:** Use JavaDoc for documentation\n");
        contentBuilder.append("- **Imports:** Organize imports and avoid wildcard imports\n");
        contentBuilder.append("- **Exception Handling:** Prefer specific exceptions and document throws\n\n");
    }
}
