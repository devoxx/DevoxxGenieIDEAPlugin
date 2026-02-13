import java.util.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.lombok") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    jacoco
}

group = "com.devoxx.genie"
version = "0.9.11"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(
            fileTree("build/classes/java/main"),
            fileTree("build/classes/kotlin/main")
        )
    )

    sourceDirectories.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin"
        )
    )

    executionData.setFrom(
        files("build/jacoco/test.exec")
    )
}

// Configure test task for JaCoCo
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
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
    intellijPlatform {
        create("IC", "2024.3")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }

    val lg4j_version = "1.11.0"
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
    implementation(platform("software.amazon.awssdk:bom:2.41.28"))
    implementation("software.amazon.awssdk:bedrock")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:sso")
    implementation("software.amazon.awssdk:ssooidc")

    // Retrofit dependencies
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")

    // Docker dependencies
    implementation("com.github.docker-java:docker-java:3.7.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.7.0")

    // JTokkit dependencies
    implementation("com.knuddels:jtokkit:1.1.0")
    implementation("org.commonmark:commonmark:0.27.1")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("io.netty:netty-all:4.2.10.Final")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.29")

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
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    testImplementation("io.github.cdimascio:java-dotenv:5.2.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.0-M1")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "253.*"
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            create("IC", "2024.3.7")
            create("IC", "2025.1.7")
            create("IC", "2025.2.6.1")
        }
    }
}

tasks {
    withType<JavaCompile> {
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
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlinLombok {
    lombokConfigurationFile(file("lombok.config"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
