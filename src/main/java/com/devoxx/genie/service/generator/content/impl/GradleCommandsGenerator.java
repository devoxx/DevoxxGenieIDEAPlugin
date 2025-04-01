package com.devoxx.genie.service.generator.content.impl;

import com.devoxx.genie.service.generator.content.BuildCommandsGenerator;

/**
 * Generates Gradle-specific build commands.
 */
public class GradleCommandsGenerator implements BuildCommandsGenerator {
    
    @Override
    public void addBuildCommands(StringBuilder contentBuilder) {
        contentBuilder.append("- **Build:** `./gradlew build`\n");
        contentBuilder.append("- **Test:** `./gradlew test`\n");
        contentBuilder.append("- **Single Test:** `./gradlew test --tests ClassName.methodName`\n");
        contentBuilder.append("- **Clean:** `./gradlew clean`\n");
        contentBuilder.append("- **Run:** `./gradlew run`\n\n");
    }
}
