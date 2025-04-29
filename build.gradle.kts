import java.util.*

plugins {
    java
    id("org.jetbrains.intellij") version "1.17.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.devoxx.genie"
version = "0.6.0"

repositories {
    mavenCentral()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
}

tasks.register("updateProperties") {
    doLast {
        val projectVersion = version
        val propertiesFile = file("src/main/resources/application.properties")

        if (propertiesFile.exists()) {
            val properties = Properties()

            // Read existing properties while removing comments
            val filteredLines = propertiesFile.readLines()
                .filterNot { it.trim().startsWith("#") } // Remove comments

            properties.load(filteredLines.joinToString("\n").byteInputStream())

            // Keep only the version property
            properties.clear()
            properties.setProperty("version", projectVersion.toString())

            // Manually write the properties file without the timestamp
            propertiesFile.printWriter().use { writer ->
                properties.forEach { key, value ->
                    writer.println("$key=$value")
                }
            }
        } else {
            println("application.properties file not found!")
        }
    }
}

tasks.named("buildPlugin") {
    dependsOn("updateProperties")
}

dependencies {
    val lg4j_version = "1.0.0-beta3"

    // Add the dependencies for the core module
    implementation(project(":core"))

    // Langchain4J dependencies
    implementation("dev.langchain4j:langchain4j:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-ollama:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-anthropic:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-bedrock:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-mistral-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-azure-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-chroma:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-mcp:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-reactor:$lg4j_version")

    // Retrofit dependencies
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.xerial:sqlite-jdbc:3.48.0.0")

    // Docker dependencies
    implementation("com.github.docker-java:docker-java:3.4.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.4.0")

    // JTokkit dependencies
    implementation("com.knuddels:jtokkit:1.0.0")
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("io.netty:netty-all:4.1.100.Final")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.12")

    // GitIgnore Reader
    implementation("nl.basjes.gitignore:gitignore-reader:1.6.0")

    // TDG : Add other TDG dependencies
    implementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    implementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    implementation("org.junit.platform:junit-platform-launcher:1.11.3")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0-M2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("io.github.cdimascio:java-dotenv:5.2.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0-M2")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.4")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("251.*")
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

    test {
        systemProperty("java.io.tmpdir", "/tmp/test")
        // Add these to help with platform tests
        systemProperty("idea.home.path", file("build/idea-sandbox"))

        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }

        maxHeapSize = "1g"

        forkEvery = 1

        reports {
            junitXml.required.set(true)
            html.required.set(true)
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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
