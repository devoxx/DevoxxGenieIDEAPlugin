import java.util.*

plugins {
    java
    id("org.jetbrains.intellij") version "1.17.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.devoxx.genie"
version = "0.9.3"

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
    val lg4j_version = "1.10.0"
    var lg4j_beta_version = "1.11.0-beta19"

    // Langchain4J dependencies
    implementation("dev.langchain4j:langchain4j:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-core:${lg4j_version}")
    implementation("dev.langchain4j:langchain4j-http-client-jdk:${lg4j_version}")
    implementation("dev.langchain4j:langchain4j-ollama:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-anthropic:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-bedrock:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-mistral-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-web-search-engine-tavily:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-azure-open-ai:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-chroma:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-mcp:$lg4j_beta_version")
    implementation("dev.langchain4j:langchain4j-reactor:$lg4j_beta_version")
    implementation(platform("software.amazon.awssdk:bom:2.41.23"))
    implementation("software.amazon.awssdk:bedrock")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:sso")
    implementation("software.amazon.awssdk:ssooidc")

    // Retrofit dependencies
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("org.xerial:sqlite-jdbc:3.51.1.0")

    // Docker dependencies
    implementation("com.github.docker-java:docker-java:3.7.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.7.0")

    // JTokkit dependencies
    implementation("com.knuddels:jtokkit:1.1.0")
    implementation("org.commonmark:commonmark:0.27.1")
    implementation("io.netty:netty-all:4.2.10.Final")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.27")

    // GitIgnore Reader
    implementation("nl.basjes.gitignore:gitignore-reader:1.14.1")

    // TDG : Add other TDG dependencies
    implementation("org.junit.jupiter:junit-jupiter-api:6.1.0-M1")
    implementation("org.junit.jupiter:junit-jupiter-engine:6.1.0-M1")
    implementation("org.junit.platform:junit-platform-launcher:6.0.2")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.0-M1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.2")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("io.github.cdimascio:java-dotenv:5.2.2")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.0-M1")
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
        untilBuild.set("253.*")
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

    runPluginVerifier {
        ideVersions.set(listOf(
            "IC-2023.3.8",
            "IC-2024.1.7",
            "IC-2024.2.6",
            "IC-2024.3.7",
            "IC-2025.1.7",
            "IC-2025.2.6.1"
        ))
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
