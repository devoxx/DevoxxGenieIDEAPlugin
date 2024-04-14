plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.devoxx.genie"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.langchain4j:langchain4j:0.29.1")
    implementation("dev.langchain4j:langchain4j-ollama:0.29.1")
    implementation("dev.langchain4j:langchain4j-local-ai:0.29.1")
    implementation("org.commonmark:commonmark:0.22.0")

    testImplementation("org.assertj:assertj-core:3.24.2")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.4")
    type.set("IC")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("241.*")
    }

    shadowJar {
        mergeServiceFiles()
        manifest {
            attributes(
                    "Implementation-Title" to "DevoxxGenie",
                    "Implementation-Version" to version,
            )
        }
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
