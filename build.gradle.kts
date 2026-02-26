import java.util.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.lombok") version "2.3.10"
    kotlin("plugin.compose") version "2.3.10"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    jacoco
}

group = "com.devoxx.genie"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    google()
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
            fileTree("build/instrumented/instrumentCode")
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
    forkEvery = 10
}

tasks.register("updateProperties") {
    group = "build"
    description = "Updates application.properties with the current project version"
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
        // Allow overriding IDE version via property: ./gradlew runIde -PideVersion=2025.1.1
        create("IC", providers.gradleProperty("ideVersion").orElse("2024.3"))
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.intellij.plugins.markdown")  // Required by markdown renderer
        composeUI()
        testFramework(TestFrameworkType.Platform)
    }
    
    // Note: Plugin bundles its own Kotlin runtime for Compose compatibility

    val lg4j_version = "1.11.0"
    val lg4j_beta_version = "1.11.0-beta19"
    val awsSdkVersion = "2.41.34"
    val retrofitVersion = "3.0.0"
    val sqliteVersion = "3.51.2.0"
    val dockerJavaVersion = "3.7.0"
    val jtokkitVersion = "1.1.0"
    val commonmarkVersion = "0.27.1"
    val jsoupVersion = "1.22.1"
    val nettyVersion = "4.2.10.Final"
    val composeVersion = "1.7.3"
    val markdownRendererVersion = "0.28.0"
    val logbackVersion = "1.5.32"
    val gitignoreReaderVersion = "1.14.1"
    val junitJupiterVersion = "6.1.0-M1"
    val junitPlatformVersion = "6.0.3"
    val lombokVersion = "1.18.42"
    val mockitoVersion = "5.21.0"
    val mockitoInlineVersion = "5.2.0"
    val assertjVersion = "3.27.7"
    val mockwebserverVersion = "5.3.2"
    val dotenvVersion = "5.2.2"
    val opentest4jVersion = "1.3.0"

    // Langchain4J dependencies
    implementation("dev.langchain4j:langchain4j:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-core:$lg4j_version")
    implementation("dev.langchain4j:langchain4j-http-client-jdk:$lg4j_version")
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
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:bedrock")
    implementation("software.amazon.awssdk:sts")
    implementation("software.amazon.awssdk:sso")
    implementation("software.amazon.awssdk:ssooidc")
    // Retrofit
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    // Docker
    implementation("com.github.docker-java:docker-java:$dockerJavaVersion")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaVersion")
    // Tokenizer, markdown, HTML, networking
    implementation("com.knuddels:jtokkit:$jtokkitVersion")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("io.netty:netty-all:$nettyVersion")
    // Compose Markdown Renderer
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:$markdownRendererVersion") {
        exclude(group = "org.intellij.markdown")
    }
    implementation("com.mikepenz:multiplatform-markdown-renderer-code-jvm:$markdownRendererVersion") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.intellij.markdown")
    }
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // GitIgnore Reader
    implementation("nl.basjes.gitignore:gitignore-reader:$gitignoreReaderVersion")
    // TDG
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    implementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")

    // Compose Desktop - bundled with plugin (required for runtime)
    implementation("org.jetbrains.compose.runtime:runtime-desktop:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation-desktop:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-desktop:$composeVersion")
    implementation("org.jetbrains.compose.components:components-animatedimage-desktop:$composeVersion")
    
    // Bundle Kotlin stdlib matching Compose requirements (must load before IDE's version)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
    
    // Skiko native libraries for all platforms (required for Compose Desktop)
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.8.18")
    compileOnly("org.projectlombok:lombok:$lombokVersion")

    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitPlatformVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-inline:$mockitoInlineVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockwebserverVersion")
    testImplementation("io.github.cdimascio:java-dotenv:$dotenvVersion")
    testImplementation("org.opentest4j:opentest4j:$opentest4jVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
    // Run plugin on different IntelliJ versions for testing
    // Usage: ./gradlew runIde -PideVersion=2024.3.5
    //        ./gradlew runIde -PideVersion=2025.1.1
    //        ./gradlew runIde -PideVersion=2025.2.2
    //        ./gradlew runIde -PideVersion=2025.3.3
    
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<JavaCompile> {
    }

    test {
        doFirst {
            val tmpDir = File("/tmp/test")
            tmpDir.deleteRecursively()
            tmpDir.mkdirs()
        }
        systemProperty("java.io.tmpdir", "/tmp/test")
        // Add these to help with platform tests
        systemProperty("idea.home.path", file("build/idea-sandbox"))

        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }

        maxHeapSize = "1g"

        // Configure JaCoCo agent to include plugin classes loaded from sandbox JARs
        extensions.configure<JacocoTaskExtension> {
            includes = listOf("com.devoxx.genie.*")
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }

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
